# Xposed 模块混淆保护规则

# 保护主包下的所有类，防止 Xposed Hook 类被裁剪
-keep class com.jsxposed.x.** { *; }

# 保护 Xposed API 接口及其实现
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# libxposed API（api101 / api102）由框架在宿主进程注入，仅参与编译
-dontwarn io.github.libxposed.**
-keep class io.github.libxposed.** { *; }

# 保护 NativeProvider 相关类
-keep class com.jsxposed.x.NativeProvider { *; }

# 如果使用了 Kotlin 反射
-keep class kotlin.reflect.jvm.internal.** { *; }
-keepclassmembers class ** {
    public void *(**);
}

-dontwarn com.alibaba.fastjson2.JSON
-dontwarn com.alibaba.fastjson2.JSONArray
-dontwarn com.alibaba.fastjson2.JSONObject
-dontwarn com.alibaba.fastjson2.JSONReader$Feature
-dontwarn com.alibaba.fastjson2.TypeReference
