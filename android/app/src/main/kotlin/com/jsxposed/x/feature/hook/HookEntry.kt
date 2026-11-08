package com.jsxposed.x.feature.hook

import com.jsxposed.x.core.constants.AppConstant
import com.jsxposed.x.core.utils.log.LogX
import com.jsxposed.x.feature.jsxposed.hook.HookJsXposed
import java.util.Collections

/**
 * 与框架无关的包加载调度入口。
 *
 * legacy 入口（`MainHook`，api100/api101）与新 API 入口（`NewApiHook`，api102）
 * 都只负责把参数包装成 [LPParam]，然后交给这里，避免业务逻辑绑定某一代 API。
 */
object HookEntry {

    private const val TAG = "FINDBUGS"

    private val hookedProcesses = Collections.synchronizedSet(mutableSetOf<String>())

    fun onPackageReady(lpparam: LPParam) {
        val start = System.currentTimeMillis()
        val processKey = "${lpparam.packageName}|${lpparam.processName}"
        LogX.d(
            TAG,
            "HookEntry.onPackageReady start package=${lpparam.packageName} process=${lpparam.processName} backend=${com.jsxposed.x.hook.compat.HookBridge.backendName()} thread=${Thread.currentThread().name}"
        )

        if (!hookedProcesses.add(processKey)) {
            LogX.w(TAG, "HookEntry skip duplicate package=${lpparam.packageName} process=${lpparam.processName}")
            return
        }

        if (lpparam.packageName == AppConstant.APP_PACKAGE_NAME) {
            LogX.d(TAG, "Main app loaded")
        }

        try {
            HookProcess(lpparam).register()
            LogX.d(TAG, "HookEntry after HookProcess package=${lpparam.packageName} process=${lpparam.processName}")

            HookJsXposed.setup(lpparam)
            LogX.d(TAG, "HookEntry after HookJsXposed package=${lpparam.packageName} process=${lpparam.processName}")
        } catch (t: Throwable) {
            LogX.e(TAG, "HookEntry failed package=${lpparam.packageName} process=${lpparam.processName}", android.util.Log.getStackTraceString(t))
        }

        LogX.d(
            TAG,
            "HookEntry.onPackageReady finish package=${lpparam.packageName} process=${lpparam.processName} cost=${System.currentTimeMillis() - start}ms"
        )
    }
}
