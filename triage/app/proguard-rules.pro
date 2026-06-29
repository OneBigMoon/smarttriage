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

### 实体类 start
-keep class com.wcjk.triage.bean.** {*;}
### 实体类 end

### 百度语音合成 start
-keep class com.baidu.tts.**{*;}
-keep class com.baidu.speechsynthesizer.**{*;}
### 百度语音合成 end

### 科大讯飞语音合成 start
-keep class com.iflytek.**{*;}
### 科大讯飞语音合成 end

### buterknife start
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}

-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
### buttknife end

### eventbus start
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
### eventbus end

#### rxjava rxandroid start
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}
-dontnote rx.internal.util.PlatformDependent
#### rxjava rxandroid end

### retrofit start
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**
-dontwarn javax.annotation.**
### retrofit end

### gilde start
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
#### glide end

### okhttp start
-dontwarn okhttp3.**
-keep class okhttp3.**{*;}
### okhttp end

### okio start
-dontwarn okio.**
-keep class okio.**{*;}

-dontwarn io.socket.client.*
-keep class io.socket.client.** { *; }

-dontwarn io.socket.emitter.*
-keep class io.socket.emitter.** { *; }

-dontwarn io.socket.engineio.*
-keep class io.socket.engineio.** { *; }
### okio end

###
-keep class com.github.mjdev.** {*;}
####
### rxpermission start
-keep class com.tbruyelle.rxpermissions.** {*;}
-keep class com.tbruyelle.rxpermissions2.** {*;}
-keep class io.reactivex.** {*;}
### rxpermission end
### 本地 start
-keepattributes EnclosingMethod
-keep class de.mindpipe.android.logging.** {*;}

### 卓策 ZCAPI start
-keep class com.zcapi { *; }
-dontwarn com.zcapi
### 卓策 ZCAPI end
-keep class org.apache.log4j.** {*;}
-dontwarn org.apache.log4j.**
-keep class org.slf4j.** {*;}
-keep class org.slf4j.impl.** {*;}

-keep class com.baidu.speech.** {*;}
-keep class com.baidu.speechsynthesizer.**{*;}
-keep class com.baidu.tts.** {*;}
-keep class com.google.zxing.** {*;}
### 本地 end

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.converter.gson.**{ *;}
-keep class retrofit2.adapter.rxjava2.**{ *;}
-keep class io.socket.**{ *;}
-keep class io.reactivex.**{ *;}
-keep class com.alibaba.fastjson.**{ *;}
-keep class retrofit2.adapter.rxjava.** {*;}
-keep class retrofit2.converter.gson.** {*;}
-keep class org.reactivestreams.**{*;}
-keep class com.google.gson.**{*;}

-keepattributes Signature
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.**{*; }

-dontoptimize

