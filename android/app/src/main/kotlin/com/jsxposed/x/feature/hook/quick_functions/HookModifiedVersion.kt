package com.jsxposed.x.feature.hook.quick_functions

import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.HookCallback
import com.jsxposed.x.hook.compat.HookParam
import com.jsxposed.x.hook.compat.ReflectX
import android.content.Context
import android.content.pm.PackageInfo
import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.HookImpl
import com.jsxposed.x.feature.hook.LPParam


class HookModifiedVersion(private val lpparam: LPParam) : HookImpl {

    private var context: Context? = null

    // 伪造版本信息配�?
    private val fakeVersionName = "9.9.9" // 伪造的版本名称
    private val fakeVersionCode = 999999 // 伪造的版本
    private val targetPackageName = lpparam.packageName // 使用lpparam获取目标包名

    override fun onAttach(context: Context) {
        this.context = context
        create()
    }

    override fun create() {

        try {
            val packageManagerClass = ReflectX.findClass(
                "android.app.ApplicationPackageManager",
                context?.classLoader
            )

            hookGetPackageInfoVariants(packageManagerClass)
            hookAdditionalVersionCheckMethods()
        } catch (e: Throwable) {
            LogX.d("HookUpdate", "Hook initialization failed: ${e.message}")
        }
    }

    private fun hookGetPackageInfoVariants(packageManagerClass: Class<*>) {
        HookBridge.findAndHookMethod(
            packageManagerClass,
            "getPackageInfo",
            String::class.java,
            Int::class.javaPrimitiveType,
            object : HookCallback() {
                override fun beforeHookedMethod(param: HookParam) {
                    handlePackageInfoHook(param)
                }
            }
        )

        try {
            val flagsClass = Class.forName(
                "android.content.pm.PackageManager\$PackageInfoFlags",
                false,
                context?.classLoader
            )

            HookBridge.findAndHookMethod(
                packageManagerClass,
                "getPackageInfo",
                String::class.java,
                flagsClass,
                object : HookCallback() {
                    override fun beforeHookedMethod(param: HookParam) {
                        handlePackageInfoHook(param)
                    }
                }
            )
        } catch (e: ClassNotFoundException) {
            LogX.d("HookUpdate", "PackageInfoFlags not available (pre-Android 12)")
        }

        // 变体3: getPackageInfo(VersionedPackage, int)
        try {
            val versionedPackageClass = Class.forName(
                "android.content.pm.VersionedPackage",
                false,
                context?.classLoader
            )

            HookBridge.findAndHookMethod(
                packageManagerClass,
                "getPackageInfo",
                versionedPackageClass,
                Int::class.javaPrimitiveType,
                object : HookCallback() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val versionedPackage = param.args[0]
                        val pkgName =
                            ReflectX.callMethod(versionedPackage, "getPackageName") as String

                        if (targetPackageName == pkgName) {
                            param.result = createFakePackageInfo(pkgName)

                        }
                    }
                }
            )
        } catch (e: ClassNotFoundException) {
            LogX.d("HookUpdate", "VersionedPackage not available (pre-Android 8)")
        }
    }

    private fun handlePackageInfoHook(param: HookParam) {
        val pkgName = param.args[0] as? String ?: return

        if (targetPackageName == pkgName) {
            param.result = createFakePackageInfo(pkgName)
        }
    }

    private fun hookAdditionalVersionCheckMethods() {
        try {
            HookBridge.findAndHookMethod(
                "android.app.ApplicationPackageManager",
                context?.classLoader,
                "getPackageInfoAsUser",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                object : HookCallback() {
                    override fun beforeHookedMethod(param: HookParam) {
                        val pkgName = param.args[0] as? String ?: return
                        if (targetPackageName == pkgName) {
                            param.result = createFakePackageInfo(pkgName)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            LogX.d("HookUpdate", "Failed to hook getPackageInfoAsUser: ${e.message}")
        }
    }

    private fun createFakePackageInfo(packageName: String): PackageInfo {
        return PackageInfo().apply {
            this.packageName = packageName
            versionName = fakeVersionName
            versionCode = fakeVersionCode
            // 设置其他必要字段
            applicationInfo = context?.packageManager?.getApplicationInfo(packageName, 0)
        }
    }
}