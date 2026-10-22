import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.fatura"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("fatura-release.jks")
            storePassword = "fatura2026"
            keyAlias = "fatura_key"
            keyPassword = "fatura2026"
        }
    }

    defaultConfig {
        applicationId = "com.albania.fatura"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "1.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        resourceConfigurations += listOf("sq") // Ultra compression

        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")) // 64-bit & 32-bit Universal
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = true // Super Ultra Kompresim
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true // Super Ultra Kompresim
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    
    // ML Kit & Camera
    implementation(libs.barcode.scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.work.runtime.ktx)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

abstract class CopyApkTask : DefaultTask() {
    @get:Internal
    abstract val buildDir: DirectoryProperty

    @get:Internal
    abstract val desktopDir: Property<String>

    @TaskAction
    fun copyApk() {
        val desktop = File(desktopDir.get())
        if (!desktop.exists()) desktop.mkdirs()

        val buildDirFile = buildDir.get().asFile
        val apkFolder = File(buildDirFile, "outputs/apk/release")

        if (apkFolder.exists()) {
            val apkFiles = apkFolder.listFiles { _, name -> name.endsWith(".apk") } ?: emptyArray()
            val zipFile = File(desktop, "Fatura_APKs.zip")
            FileOutputStream(zipFile).use { fos ->
                ZipOutputStream(BufferedOutputStream(fos)).use { zos ->
                    apkFiles.forEach { file ->
                        val entryName = when {
                            file.name.contains("arm64-v8a") -> "Fatura_arm64-v8a_64bit.apk"
                            file.name.contains("armeabi-v7a") -> "Fatura_armeabi-v7a_32bit.apk"
                            file.name.contains("x86_64") -> "Fatura_x86_64.apk"
                            file.name.contains("x86") -> "Fatura_x86.apk"
                            file.name.contains("universal") -> "Fatura_Universal.apk"
                            else -> file.name
                        }
                        val entry = ZipEntry(entryName)
                        zos.putNextEntry(entry)
                        file.inputStream().use { input -> input.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            println("✅ Te gjitha APK-te sipas arkitekturave u paketuan ne Fatura_APKs.zip: ${zipFile.absolutePath}")
        } else {
            println("⚠️ Nuk u gjet asnje APK i ndertuar.")
        }
    }
}

val copyApkTask = tasks.register<CopyApkTask>("copyApkToDesktop") {
    buildDir.set(layout.buildDirectory)
    desktopDir.set("C:/Users/ANDI/Desktop")
}

tasks.matching { it.name.startsWith("assemble") }.configureEach {
    finalizedBy(copyApkTask)
}
