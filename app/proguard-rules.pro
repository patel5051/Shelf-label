# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entity classes
-keep class com.retailshelflabel.data.db.** { *; }

# Keep SUNMI SDK interfaces (stubs — adjust if real SDK is added)
-keep class com.sunmi.** { *; }
-keep interface com.sunmi.** { *; }
-keep class woyou.aidlservice.** { *; }

# Keep ZXing classes used for barcode generation
-keep class com.google.zxing.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
