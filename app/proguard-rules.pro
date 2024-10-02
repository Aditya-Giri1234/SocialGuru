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

-keepattributes SourceFile,LineNumberTable

# Keep all public classes, fields, and methods for your app package
-keep class com.aditya.socialguru.** { *; }

# Keep model classes for Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep ViewModels
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# RetroFit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep data binding classes
-keep class androidx.databinding.** { *; }

# Keep all Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

#Gson
-keep class com.google.gson.reflect.*{*;}
-keep class com.google.api.client.json.**{*;}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

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
-keep class kotlin.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

#For Generic
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-keep interface com.squareup.okhttp3.** { *; }
-dontwarn com.squareup.okhttp3.**

# Keep the markdown and html  class and its dependencies
-keep class io.noties.markwon.html.tag.StrikeHandler { *; }
-keep class io.noties.markwon.** { *; }  # Keep all classes in the markwon package
-keep class org.commonmark.** { *; }      # Keep all classes in the commonmark package
-dontwarn org.commonmark.ext.gfm.strikethrough.**


#This will generate mapping text file which tell us how our class name , member name map done.
-printmapping mapping.txt