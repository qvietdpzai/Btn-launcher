# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\tools\adt-bundle-windows-x86_64-20131030\sdk/tools/proguard/proguard-android.txt
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

# We use Reflection on the builder to avoid creating too many objects
 -keep class net.objecthunter.exp4j.ExpressionBuilder**
 -keepclassmembers class net.objecthunter.exp4j.ExpressionBuilder** {
    *;
 }
# Option screens
 -keep class net.kdt.pojavlaunch.prefs.screens** {*;}

# Gson model classes - keep all fields for serialization/deserialization
-keep class net.kdt.pojavlaunch.modloaders.modpacks.models.** { *; }
-keep class net.kdt.pojavlaunch.modloaders.BTAUtils$** { *; }
-keep class net.kdt.pojavlaunch.modloaders.FabriclikeUtils$FabricVersion { *; }
-keep class net.kdt.pojavlaunch.authenticator.model.** { *; }
-keep class net.kdt.pojavlaunch.JAssets { *; }
-keep class net.kdt.pojavlaunch.JVersionList$Version { *; }
-keep class net.kdt.pojavlaunch.customcontrols.ControlData { *; }
-keep class net.kdt.pojavlaunch.customcontrols.ControlDrawerData { *; }
-keep class net.kdt.pojavlaunch.customcontrols.CustomControls { *; }
-keep class net.kdt.pojavlaunch.instances.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Gson: prevent R8 from merging/collapsing classes used by Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation class * extends com.google.gson.TypeAdapter
-keepclassmembers class * {
    <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-dontwarn com.google.gson.**


