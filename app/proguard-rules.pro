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

# Keep all public classes, fields, and methods for your app package
-keep class com.aditya.socialguru.** { *; }

# Keep model classes for Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep ViewModels
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Retrofit models
-keep class com.squareup.retrofit2.** { *; }

# Keep data binding classes
-keep class androidx.databinding.** { *; }

# Keep all Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Gson serialized classes
-keep class com.google.gson.** { *; }
-keepattributes Signature

# Keep Glide classes
-keep public class com.bumptech.glide.** { *; }
-keep public class com.bumptech.glide.module.AppGlideModule { *; }
-keep public class * implements com.bumptech.glide.module.GlideModule { *; }

# Keep Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }

# Keep DataStore preferences
-keep class androidx.datastore.** { *; }

# Keep emoji library classes
-keep class com.vanniktech.emoji.** { *; }

# Keep classes annotated with @Keep
-keep @androidx.annotation.Keep class * { *; }

# For Media3
-keep class androidx.media3.** { *; }

# Keep for Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**

