package com.jsxposed.x

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.hook.ModuleInterfaceParamWrapper
import com.jsxposed.x.feature.hook.lpparamProcessName
import de.robv.android.xposed.IXposedHookZygoteInit
import io.github.libxposed.api.XposedModule
import top.sacz.xphelper.XpHelper

class NewApiHook : XposedModule() {

    private lateinit var mainHook: MainHook

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)

        instance = this
        mainHook = MainHook()
        lpparamProcessName = param.processName

        runCatching {
            val modulePath = resolveModulePathCompat()
            if (modulePath.isNotBlank()) {
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
    override fun onPackageLoaded(param: PackageLoadedParam) {
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
