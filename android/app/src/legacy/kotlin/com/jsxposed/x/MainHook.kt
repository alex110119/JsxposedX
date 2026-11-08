package com.jsxposed.x

import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.HookEntry
import com.jsxposed.x.feature.hook.LoadPackageParamWrapper
import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.LegacyHookBackend
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_LoadPackage
import top.sacz.xphelper.XpHelper

/**
 * legacy 模块入口，只参与 api100 / api101 的编译。
 *
 * api102 使用 [com.jsxposed.x.NewApiHook]（libxposed API 102），不会再触碰本类。
 */
class MainHook : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TAG = "FINDBUGS"

        @Volatile
        private var backendInstalled = false

        fun ensureBackend() {
            if (backendInstalled) return
            synchronized(this) {
                if (backendInstalled) return
                HookBridge.install(LegacyHookBackend())
                backendInstalled = true
            }
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        ensureBackend()
        XpHelper.initZygote(startupParam)
        LogX.d(TAG, "========== MainHook.initZygote ==========")
        LogX.d(TAG, "  modulePath: ${startupParam.modulePath}")
        LogX.d(TAG, "  thread: ${Thread.currentThread().name}")
        LogX.d(TAG, "Xposed module loaded successfully")
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        ensureBackend()
        LogX.d(
            TAG,
            "MainHook.handleLoadPackage package=${lpparam.packageName} process=${lpparam.processName} classLoader=${lpparam.classLoader} thread=${Thread.currentThread().name}"
        )
        Thread.currentThread().contextClassLoader = lpparam.classLoader
        HookEntry.onPackageReady(LoadPackageParamWrapper(lpparam))
    }
}
