# ==============================================
# Xposed 模块 混淆保护规则（完整版 - API101 + LSPosed 2.0）
# ==============================================

# 保护主包所有类，防止被混淆/裁剪
-keep class com.jsxposed.x.** { *; }

# 保护传统 Xposed API
-keep class de.robv.android.xposed.** { *; }
-dontwarn de.robv.android.xposed.**

# 保护新版 LibXposed API 101（必须）
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**

# 保护 NativeProvider
-keep class com.jsxposed.x.NativeProvider { *; }

# 保护 XpHelper 工具（你要求额外添加的）
-keep class top.sacz.xphelper.** { *; }
-dontwarn top.sacz.xphelper.**

# Kotlin 反射保护
-keep class kotlin.reflect.jvm.internal.** { *; }
-keepclassmembers class ** {
    public void *(**);
}

# fastjson2 忽略警告
-dontwarn com.alibaba.fastjson2.JSON
-dontwarn com.alibaba.fastjson2.JSONArray
-dontwarn com.alibaba.fastjson2.JSONObject
-dontwarn com.alibaba.fastjson2.JSONReader$Feature
-dontwarn com.alibaba.fastjson2.TypeReference

# 保护 META-INF/xposed 配置文件不被删除
-adaptresourcefilenames **/META-INF/xposed/*
