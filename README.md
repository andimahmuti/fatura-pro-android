# 🧾 FATURA Pro — Android POS & Invoicing System

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform">
  <img src="https://img.shields.io/badge/Framework-Jetpack%20Compose-blue.svg" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  <img src="https://img.shields.io/badge/Architecture-Clean%20%26%20Modular-orange.svg" alt="Architecture">
</p>

**FATURA Pro** is a professional, lightning-fast, and open-source Android Point of Sale (POS) and Invoicing application built with modern Android technologies (Kotlin, Jetpack Compose, Room Database). Designed for seamless retail management, receipt generation, and thermal Bluetooth printing.

---

## ✨ Key Features

* **🧾 Complete Invoicing & Sales:** Manage cart items, calculates totals, taxes, and transactions instantly.
* **🖨️ ESC/POS Bluetooth Printing:** Full support for thermal Bluetooth receipt printers with logo printing capabilities.
* **📊 Reports & History:** Comprehensive tracking of daily turnover, sales history, and saved receipts.
* **📱 Adaptive Screen Orientation:**
  * **Smartphones:** Strictly locked to **Portrait Mode** for optimal single-handed POS usability.
  * **Tablets ($\ge$ 600dp):** Supports both **Portrait & Landscape Modes** for expanded dashboard management.
* **⚡ Ultra-Optimized & Slim APKs:** Minified with R8 full-mode optimization and stripped resources for lightning-fast startup and tiny file size (~5-7 MB per ABI split).
* **🌐 Multi-Architecture Support:** Built for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, and `universal` devices.

---

## 📥 Download & Installation

You can download the ready-to-use APK optimized for your device architecture directly from the [Releases](../../releases) page:
* `FATURAPro-arm64-v8a.apk` (Modern 64-bit phones & tablets)
* `FATURAPro-armeabi-v7a.apk` (Older 32-bit devices)
* `FATURAPro-universal.apk` (All-in-one APK)

---

## 🛠️ Tech Stack

* **Language:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
* **Local Database:** [Room Database](https://developer.android.com/training/data-storage/room) (SQLite)
* **Concurrency:** Kotlin Coroutines & Flow
* **Min SDK:** Android 7.0 (API 24)
* **Target SDK:** Android 35 (API 35)

---

## 📜 License

This project is open-source and available under the [MIT License](LICENSE).
