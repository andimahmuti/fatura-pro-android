# Hardware, Bluetooth & Printing Utilities
-keep class android.bluetooth.** { *; }
-keep interface android.bluetooth.** { *; }
-keep class com.example.fatura.util.** { *; }

# Models & Data (Room, Receipts, Items, Settings and Serialization)
-keep class com.example.fatura.data.entity.** { *; }
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }

# Image Loading (Coil for Product Icons & Logos)
-keep class coil.** { *; }
-dontwarn coil.**

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# ML Kit & Camera
-dontwarn com.google.mlkit.**
-dontwarn androidx.camera.**
