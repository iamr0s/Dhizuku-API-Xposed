# Add project specific ProGuard rules here.

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-optimizationpasses 7
-dontpreverify

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep @androidx.annotation.Keep class * {*;}
-keep class * {
    @androidx.annotation.Keep <fields>;
}
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keep public class * extends android.os.IInterface
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
