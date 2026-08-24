# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Preserve AutofillService and components from obfuscation
-keep class com.example.autofill.VaultAutofillService { *; }
-keep class com.example.autofill.** { *; }

# Preserve JNI native method signatures
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve Room entities and TypeConverters
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Preserve BouncyCastle cryptographic engines
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Strip verbose/debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

