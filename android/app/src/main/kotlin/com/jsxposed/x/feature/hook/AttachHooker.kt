package com.jsxposed.x.feature.hook

import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.HookCallback
import com.jsxposed.x.hook.compat.HookParam
import android.content.Context
import com.jsxposed.x.core.utils.log.LogX
import java.util.Collections


object AttachHooker {

    private const val TAG = "FINDBUGS"
    private val hooks = mutableListOf<HookImpl>()
    private val attachCallbacks = mutableListOf<Pair<String, (Context) -> Unit>>()
    private val installedHooks = Collections.synchronizedSet(mutableSetOf<String>())

    fun register(hook: HookImpl) {
        hooks.add(hook)
        LogX.d(TAG, "AttachHooker.register hook=${hook.javaClass.name} total=${hooks.size}")
    }

    fun registerAfterAttach(name: String, callback: (Context) -> Unit) {
        attachCallbacks.add(name to callback)
        LogX.d(TAG, "AttachHooker.registerAfterAttach name=$name total=${attachCallbacks.size}")
    }

    fun setupAttachHook(lpparam: LPParam) {
        try {
            val installKey = "${lpparam.packageName}|${lpparam.processName}"
            if (!installedHooks.add(installKey)) {
                LogX.d(TAG, "AttachHooker.setupAttachHook skip duplicate package=${lpparam.packageName} process=${lpparam.processName}")
                return
            }

            LogX.d(TAG, "AttachHooker.setupAttachHook package=${lpparam.packageName} process=${lpparam.processName} hooks=${hooks.size} callbacks=${attachCallbacks.size} classLoader=${lpparam.classLoader}")
            HookBridge.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                "attach",
                Context::class.java,
                object : HookCallback() {
                    override fun afterHookedMethod(param: HookParam) {
                        val context = param.args[0] as? Context ?: return
                        val start = System.currentTimeMillis()
                        LogX.d(TAG, "AttachHooker.afterHookedMethod enter package=${lpparam.packageName} process=${lpparam.processName} thread=${Thread.currentThread().name} hooks=${hooks.size} callbacks=${attachCallbacks.size} context=${context.javaClass.name}")
                        hooks.forEachIndexed { index, hook ->
                            try {
                                val hookStart = System.currentTimeMillis()
                                LogX.d(TAG, "AttachHooker.onAttach start index=$index hook=${hook.javaClass.name} package=${lpparam.packageName} process=${lpparam.processName}")
                                hook.onAttach(context)
                                LogX.d(TAG, "AttachHooker.onAttach finish index=$index hook=${hook.javaClass.name} cost=${System.currentTimeMillis() - hookStart}ms")
                            } catch (e: Throwable) {
                                LogX.e(TAG, "AttachHooker.onAttach failed hook=${hook.javaClass.name}", android.util.Log.getStackTraceString(e))
                            }
                        }
                        attachCallbacks.forEachIndexed { index, callbackEntry ->
                            val (name, callback) = callbackEntry
                            try {
                                val callbackStart = System.currentTimeMillis()
                                LogX.d(TAG, "AttachHooker.callback start index=$index name=$name package=${lpparam.packageName} process=${lpparam.processName}")
                                callback(context)
                                LogX.d(TAG, "AttachHooker.callback finish index=$index name=$name cost=${System.currentTimeMillis() - callbackStart}ms")
                            } catch (e: Throwable) {
                                LogX.e(TAG, "AttachHooker.callback failed name=$name", android.util.Log.getStackTraceString(e))
                            }
                        }
                        LogX.d(TAG, "AttachHooker.afterHookedMethod exit package=${lpparam.packageName} process=${lpparam.processName} cost=${System.currentTimeMillis() - start}ms")
                    }
                }
            )
        } catch (e: Throwable) {
            LogX.e(TAG, "AttachHooker.setupAttachHook failed", android.util.Log.getStackTraceString(e))
        }
    }
}
