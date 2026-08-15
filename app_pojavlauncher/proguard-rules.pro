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

# Keep the main application package and all its subpackages
-keep class net.kdt.pojavlaunch.** { *; }
-keep class com.kdt.** { *; }
-keep class net.objecthunter.exp4j.ExpressionBuilder** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn com.google.gson.**

# JNI bindings: native code (libglfw.so) resolves these classes and methods by
# name (GetStaticMethodID), so R8 must not strip or rename them.
# Without this, opening the custom controls editor crashes with
# NoSuchMethodError: GLFW.receiveGrabState(Z)V
-keep class git.artdeell.dnbootstrap.glfw.** { *; }


