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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Mengabaikan peringatan library pengolah anotasi yang tidak dibutuhkan di runtime
-dontwarn javax.annotation.processing.**
-dontwarn javax.lang.model.**
-dontwarn com.google.auto.value.**

# Jika Anda menggunakan AutoValue atau library dari Google lainnya yang memicu error ini
-keepattributes Signature
-keepattributes *Annotation*