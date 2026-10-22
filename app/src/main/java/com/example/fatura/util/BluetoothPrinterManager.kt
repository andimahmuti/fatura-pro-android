package com.example.fatura.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothPrinterManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    companion object {
        private var bluetoothSocket: BluetoothSocket? = null
        private var outputStream: OutputStream? = null
        private var inputStream: java.io.InputStream? = null
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private var lastConnectedAddress: String? = null
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceAddress: String, onResult: (Boolean) -> Unit) {
        if (deviceAddress.isBlank() || !BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            onResult(false)
            return
        }
        lastConnectedAddress = deviceAddress
        if (isConnected()) {
            onResult(true)
            return
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device == null) {
                onResult(false)
                return
            }

            Thread {
                val success = syncConnect(deviceAddress)
                onResult(success)
            }.start()
        } catch (e: Throwable) {
            onResult(false)
        }
    }

    fun disconnect() {
        Log.d("BluetoothPrinter", "Manual disconnect requested")
        closeConnection()
    }

    private fun closeConnection() {
        try {
            outputStream?.flush()
            outputStream?.close()
            inputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            Log.e("BluetoothPrinter", "Error closing connection", e)
        } finally {
            outputStream = null
            inputStream = null
            bluetoothSocket = null
        }
    }

    /**
     * Attempts to get battery level using ESC/POS command DLE EOT 4.
     */
    fun getBatteryLevel(onResult: (Int?) -> Unit) {
        if (!isConnected()) {
            onResult(null)
            return
        }
        
        Thread {
            try {
                while (inputStream?.available() ?: 0 > 0) {
                    inputStream?.read()
                }
                outputStream?.write(byteArrayOf(0x10, 0x04, 0x04))
                outputStream?.flush()
                
                var attempts = 0
                while ((inputStream?.available() ?: 0) == 0 && attempts < 10) {
                    Thread.sleep(50)
                    attempts++
                }
                
                if ((inputStream?.available() ?: 0) > 0) {
                    val response = inputStream?.read() ?: -1
                    val level = when {
                        response == -1 -> null
                        (response and 0x0C) == 0x00 -> 100
                        (response and 0x0C) == 0x04 -> 70
                        (response and 0x0C) == 0x08 -> 40
                        (response and 0x0C) == 0x0C -> 10
                        else -> null
                    }
                    onResult(level)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                onResult(null)
            }
        }.start()
    }

    fun printWithLogo(text: String, logoPath: String?, savedAddress: String? = null, onResult: (Boolean) -> Unit = {}) {
        Thread {
            try {
                // Ensure connected before anything
                val addressToUse = savedAddress ?: lastConnectedAddress
                if (!isConnected() && addressToUse != null) {
                    syncConnect(addressToUse)
                }

                if (isConnected()) {
                    // 1. Send Center Alignment Command
                    outputStream?.write(byteArrayOf(0x1B, 0x61, 0x01)) 
                    outputStream?.flush()

                    // 2. Logo Printing
                    if (text.contains("[LOGO]") && logoPath != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(logoPath)
                        if (bitmap != null) {
                            // Scale for 58mm printer (384 dots width)
                            val targetWidth = 384
                            val targetHeight = (bitmap.height.toFloat() * targetWidth / bitmap.width).toInt()
                            
                            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                            val bytes = decodeBitmap(scaled)
                            outputStream?.write(bytes)
                            outputStream?.write("\n".toByteArray())
                            outputStream?.flush()
                        }
                    }
                    
                    // 3. Clean text and print
                    val cleanText = text.replace("[LOGO]\n", "").replace("[LOGO]", "")
                    // Ensure text alignment is reset to Left for items if needed, 
                    // but the builder usually sends its own alignment.
                    outputStream?.write(cleanText.toByteArray())
                    outputStream?.flush()
                    Thread.sleep(200)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("BluetoothPrinter", "PrintWithLogo failed", e)
                onResult(false)
            }
        }.start()
    }

    /**
     * Prints a bitmap (Logo) using ESC/POS commands.
     */
    fun printBitmap(bitmap: android.graphics.Bitmap) {
        try {
            val bytes = decodeBitmap(bitmap)
            outputStream?.write(bytes)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e("BluetoothPrinter", "Error printing bitmap", e)
        }
    }

    private fun decodeBitmap(bmp: android.graphics.Bitmap): ByteArray {
        val width = bmp.width
        val height = bmp.height
        val bw = (width + 7) / 8
        val data = ByteArray(height * bw + 8)
        
        data[0] = 0x1D
        data[1] = 0x76
        data[2] = 0x30
        data[3] = 0x00
        data[4] = (bw % 256).toByte()
        data[5] = (bw / 256).toByte()
        data[6] = (height % 256).toByte()
        data[7] = (height / 256).toByte()

        var k = 8
        for (i in 0 until height) {
            for (j in 0 until bw) {
                var temp = 0
                for (b in 0 until 8) {
                    val x = j * 8 + b
                    if (x < width) {
                        val pixel = bmp.getPixel(x, i)
                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val b_val = pixel and 0xff
                        val gray = (r * 0.299 + g * 0.587 + b_val * 0.114).toInt()
                        if (gray < 128) {
                            temp = temp or (1 shl (7 - b))
                        }
                    }
                }
                data[k++] = temp.toByte()
            }
        }
        return data
    }

    fun print(data: ByteArray, savedAddress: String? = null, onResult: (Boolean) -> Unit = {}) {
        val addressToUse = savedAddress ?: lastConnectedAddress
        Thread {
            synchronized(this) {
                if (!isConnected() && addressToUse != null) syncConnect(addressToUse)
                if (!isConnected()) {
                    onResult(false)
                    return@synchronized
                }
                try {
                    outputStream?.write(data)
                    outputStream?.flush()
                    Thread.sleep(200)
                    onResult(true)
                } catch (e: IOException) {
                    if (addressToUse != null) {
                        closeConnection()
                        if (syncConnect(addressToUse)) {
                            try {
                                outputStream?.write(data)
                                outputStream?.flush()
                                onResult(true)
                                return@synchronized
                            } catch (_: Exception) {}
                        }
                    }
                    onResult(false)
                }
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun syncConnect(address: String): Boolean {
        synchronized(BluetoothPrinterManager::class.java) {
            if (isConnected() && lastConnectedAddress == address) {
                return true
            }
            if (address.isBlank() || !BluetoothAdapter.checkBluetoothAddress(address)) {
                return false
            }
            return try {
                closeConnection()
                val adapter = bluetoothAdapter ?: return false
                if (!adapter.isEnabled) return false
                val device = adapter.getRemoteDevice(address) ?: return false
                bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                try {
                    bluetoothSocket?.connect()
                } catch (e: IOException) {
                    bluetoothSocket = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        .invoke(device, 1) as BluetoothSocket
                    bluetoothSocket?.connect()
                }
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
                lastConnectedAddress = address
                true
            } catch (e: Throwable) {
                closeConnection()
                false
            }
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket != null && bluetoothSocket!!.isConnected && outputStream != null
    }

    /**
     * Aggressive auto-reconnect logic.
     */
    fun ensureConnection(savedAddress: String?, onResult: (Boolean) -> Unit) {
        if (isConnected()) {
            onResult(true)
            return
        }
        if (savedAddress != null) {
            connect(savedAddress, onResult)
        } else {
            onResult(false)
        }
    }
}
