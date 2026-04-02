package com.jsxposed.x.feature.hook

import android.content.pm.ApplicationInfo
import io.github.libxposed.api.XposedModuleInterface

interface LPParam {
    val packageName: String
    val processName: String
    val classLoader: ClassLoader
    val appInfo: ApplicationInfo?
    val isFirstApplication: Boolean
}

class LoadPackageParamWrapper(
    private val origin: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
) : LPParam {
    override val packageName get() = origin.packageName
    override val processName get() = origin.processName
    override val classLoader get() = origin.classLoader
    override val appInfo get() = origin.appInfo
    override val isFirstApplication get() = origin.isFirstApplication
}

var lpparamProcessName: String = ""

class ModuleInterfaceParamWrapper(
    private val origin: XposedModuleInterface.PackageLoadedParam
) : LPParam {
    override val packageName get() = origin.packageName
    override val processName get() = lpparamProcessName
    // API 101: classLoader → defaultClassLoader
    override val classLoader get() = origin.defaultClassLoader
    override val appInfo get() = origin.applicationInfo
    override val isFirstApplication get() = origin.isFirstPackage
}
