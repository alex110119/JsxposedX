package com.jsxposed.x

import android.annotation.SuppressLint
import com.jsxposed.x.feature.hook.HookEntry
import com.jsxposed.x.feature.hook.ModuleInterfaceParamWrapper
import com.jsxposed.x.feature.hook.lpparamProcessName
import de.robv.android.xposed.IXposedHookZygoteInit
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import top.sacz.xphelper.XpHelper

/**
 * libxposed API 100 模块入口（旧构造器形态）。
 *
 * 同样走 legacy 后端，包加载流程统一委派给 [HookEntry]。
 */
class NewApiHook(base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam) :
    XposedModule(base, param) {

    init {
        instance = this
        MainHook.ensureBackend()
        lpparamProcessName = param.processName
        try {
            val suparam = createStartupParam(this.applicationInfo.sourceDir)
            XpHelper.initZygote(suparam)
        } catch (_: Exception) {
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)
        HookEntry.onPackageReady(ModuleInterfaceParamWrapper(param))
    }

    companion object {
        @Volatile
        var instance: NewApiHook? = null

        fun usePreferencesSnapshotTransport(): Boolean = true
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
