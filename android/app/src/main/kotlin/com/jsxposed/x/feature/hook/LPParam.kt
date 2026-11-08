package com.jsxposed.x.feature.hook

import android.content.pm.ApplicationInfo

/**
 * 包加载参数的框架无关抽象。
 *
 * 具体实现按 flavor 拆分：
 * - api100 / api101：`LoadPackageParamWrapper`（包装 `XC_LoadPackage.LoadPackageParam`）
 * - api102：`Api102PackageReadyParamWrapper`（包装 `XposedModuleInterface.PackageReadyParam`）
 */
interface LPParam {
    val packageName: String
    val processName: String
    val classLoader: ClassLoader
    val appInfo: ApplicationInfo?
    val isFirstApplication: Boolean
}
