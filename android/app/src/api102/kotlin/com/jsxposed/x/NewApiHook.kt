package com.jsxposed.x

import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.Api102PackageReadyParamWrapper
import com.jsxposed.x.feature.hook.HookEntry
import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.NewApi102Backend
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface

/**
 * libxposed API 102 模块入口。
 *
 * API 102 禁止访问 `de.robv.android.xposed`（模块 dex 里的该前缀会被框架重写成随机包名，
 * 且 102 通道不会注入 legacy 桥接类），因此这里**不能**再引用 `MainHook`、
 * `IXposedHookZygoteInit` 或 XpHelper，全部改为走 [NewApi102Backend]。
 */
class NewApiHook : XposedModule() {

    init {
        instance = this
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        instance = this
        processName = param.processName

        HookBridge.install(NewApi102Backend(this))

        runCatching {
            LogX.d(
                TAG,
                "onModuleLoaded process=${param.processName} module=${getModuleApplicationInfo().sourceDir} backend=${HookBridge.backendName()}"
            )
        }.onFailure {
            LogX.e(TAG, "onModuleLoaded log failed: ${it.message}")
        }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(param)
        HookEntry.onPackageReady(Api102PackageReadyParamWrapper(param, processName))
    }

    companion object {
        private const val TAG = "FINDBUGS"

        @Volatile
        var instance: NewApiHook? = null
            private set

        @Volatile
        private var processName: String = ""

        fun usePreferencesSnapshotTransport(): Boolean = false
    }
}
