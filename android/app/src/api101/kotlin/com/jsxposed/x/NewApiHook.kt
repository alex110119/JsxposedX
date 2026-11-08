package com.jsxposed.x

import android.annotation.SuppressLint
import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.Api101PackageReadyParamWrapper
import com.jsxposed.x.feature.hook.HookEntry
import de.robv.android.xposed.IXposedHookZygoteInit
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import top.sacz.xphelper.XpHelper

/**
 * libxposed API 101 模块入口。
 *
 * API 101 仍允许访问 legacy 包，因此继续使用 legacy 后端（XpHelper 也依赖它），
 * 但包加载流程统一走 [HookEntry]，与 api102 保持同一条业务链路。
 */
class NewApiHook : XposedModule() {

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        instance = this
        processName = param.processName

        MainHook.ensureBackend()

        runCatching {
            val startupParam = createStartupParam(getModuleApplicationInfo().sourceDir)
            XpHelper.initZygote(startupParam)
        }.onFailure {
            LogX.e(TAG, "onModuleLoaded init failed: ${it.message}")
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        super.onPackageReady(param)
        HookEntry.onPackageReady(Api101PackageReadyParamWrapper(param, processName))
    }

    companion object {
        private const val TAG = "FINDBUGS"

        @Volatile
        var instance: NewApiHook? = null

        @Volatile
        private var processName: String = ""

        fun usePreferencesSnapshotTransport(): Boolean = false
    }

    private fun createStartupParam(modulePath: String): IXposedHookZygoteInit.StartupParam {
        val clazz = IXposedHookZygoteInit.StartupParam::class.java
        val constructor = clazz.getDeclaredConstructor()
        constructor.isAccessible = true
        val instance = constructor.newInstance()
        val fieldModulePath = clazz.getDeclaredField("modulePath")
        fieldModulePath.isAccessible = true
        fieldModulePath.set(instance, modulePath)
        return instance
    }
}
