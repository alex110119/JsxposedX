package com.jsxposed.x

import android.annotation.SuppressLint
import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.ModuleInterfaceParamWrapper
import com.jsxposed.x.feature.hook.lpparamProcessName
import de.robv.android.xposed.IXposedHookZygoteInit
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import top.sacz.xphelper.XpHelper

class NewApiHook : XposedModule() {

    private lateinit var mainHook: MainHook

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        super.onModuleLoaded(param)

        instance = this
        mainHook = MainHook()
        lpparamProcessName = param.processName

        runCatching {
            // API 101: getModuleApplicationInfo() 直接可用（继承自 XposedInterfaceWrapper）
            val modulePath = getModuleApplicationInfo().sourceDir
            if (!modulePath.isNullOrBlank()) {
                val startupParam = createStartupParam(modulePath)
                XpHelper.initZygote(startupParam)
            } else {
                LogX.w("NewApiHook", "module path is empty, skip XpHelper.initZygote")
            }
        }.onFailure {
            LogX.e("NewApiHook", "init failed: ${it.message}")
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        super.onPackageLoaded(param)
        mainHook.handleNewApiPackageLoaded(ModuleInterfaceParamWrapper(param))
    }

    companion object {
        @Volatile
        var instance: NewApiHook? = null
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
