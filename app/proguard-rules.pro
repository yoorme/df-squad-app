# Keep kotlinx-serialization serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.yoorme.squadsignup.** { kotlinx.serialization.KSerializer serializer(...); }
# kotlinx.serialization 官方 R8 full mode 规则：保住 Serializable 类的 Companion/INSTANCE
# 与序列化器，避免将来改用反射式 serializer<T>() 时踩坑
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Retrofit
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ===== 极光推送 =====
# jcore 以 aar 分发，自带 consumer rules（keep cn.jpush.** / cn.jiguang.**）会被自动应用；
# jpush 是 jar、携带不了 consumer rules，这里补齐同名规则，
# 避免将来单独升级 jpush jar 时其回调/服务被裁掉。
-dontwarn cn.jpush.**
-keep class cn.jpush.** { *; }
-dontwarn cn.jiguang.**
-keep class cn.jiguang.** { *; }
# 极光通过清单声明 + 反射回调以下组件（清单组件 AGP 已保底，这里再加一道显式保险）
-keep class com.yoorme.squadsignup.notify.PushMessageReceiver { *; }
-keep class com.yoorme.squadsignup.notify.PushService { *; }
