package com.jsxposed.x.feature.hook

/**
 * legacy 通道（api100 / api101）的 [LPParam] 实现。
 */
class LoadPackageParamWrapper(
    private val origin: de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
) : LPParam {
    override val packageName get() = origin.packageName
    override val processName get() = origin.processName
    override val classLoader get() = origin.classLoader
    override val appInfo get() = origin.appInfo
    override val isFirstApplication get() = origin.isFirstApplication
}
