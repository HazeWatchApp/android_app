# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

#App specific configs
-keep class me.ebernie.mapi.model.** { *; }

#workaround Issue 78377
-keep class !android.support.v7.internal.view.menu.MenuBuilder
-keep class !android.support.v7.internal.view.menu.SubMenuBuilder

# Remove log
# Issue http://sourceforge.net/p/proguard/bugs/534/
-assumenosideeffects class android.util.Log {
    public static int i(...);
    public static int w(...);
    public static int v(...);
    public static int d(...);
    public static int e(...);
}

-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }

-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**

-keep class com.github.mikephil.charting.** { *; }
