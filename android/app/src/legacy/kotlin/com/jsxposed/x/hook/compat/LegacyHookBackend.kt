package com.jsxposed.x.hook.compat

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.lang.reflect.Member

/**
 * legacy 通道后端（api100 / api101）：把统一的 [HookCallback] 适配到 `de.robv.android.xposed`。
 *
 * 该文件只参与 api100 / api101 的编译，api102 不会引用任何 legacy 类型。
 */
class LegacyHookBackend : HookBackend {

    override val name: String = "legacy(de.robv.android.xposed)"

    override fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val args = params.dropLast(1).toTypedArray() + adapt(callback)
        return Wrap(XposedHelpers.findAndHookMethod(className, classLoader, methodName, *args))
    }

    override fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val args = params.dropLast(1).toTypedArray() + adapt(callback)
        return Wrap(XposedHelpers.findAndHookMethod(clazz, methodName, *args))
    }

    override fun findAndHookConstructor(
        className: String,
        classLoader: ClassLoader?,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val args = params.dropLast(1).toTypedArray() + adapt(callback)
        return Wrap(XposedHelpers.findAndHookConstructor(className, classLoader, *args))
    }

    override fun findAndHookConstructor(
        clazz: Class<*>,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val args = params.dropLast(1).toTypedArray() + adapt(callback)
        return Wrap(XposedHelpers.findAndHookConstructor(clazz, *args))
    }

    override fun hookAllMethods(clazz: Class<*>, methodName: String, callback: HookCallback): Array<Unhook> =
        XposedBridge.hookAllMethods(clazz, methodName, adapt(callback))
            .map { Wrap(it) }
            .toTypedArray()

    override fun hookAllConstructors(clazz: Class<*>, callback: HookCallback): Array<Unhook> =
        XposedBridge.hookAllConstructors(clazz, adapt(callback))
            .map { Wrap(it) }
            .toTypedArray()

    override fun invokeOriginalMethod(member: Member, thisObject: Any?, args: Array<Any?>?): Any? =
        XposedBridge.invokeOriginalMethod(member, thisObject, args)

    override fun log(message: String) = XposedBridge.log(message)

    // ── 适配 ──

    private fun extractCallback(params: Array<out Any?>): HookCallback =
        params.lastOrNull() as? HookCallback
            ?: throw IllegalArgumentException("最后一个参数必须是 HookCallback")

    private fun adapt(callback: HookCallback): XC_MethodHook {
        if (callback is ReplaceCallback) {
            return object : XC_MethodReplacement(callback.priority) {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val hp = HookParam(param.method, param.thisObject, param.args)
                    return callback.replaceHookedMethod(hp)
                }
            }
        }

        return object : XC_MethodHook(callback.priority) {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val hp = HookParam(param.method, param.thisObject, param.args)
                callback.beforeHookedMethod(hp)
                if (hp.returnEarly) {
                    param.result = hp.result
                }
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val hp = HookParam(param.method, param.thisObject, param.args)
                hp.setOutcome(param.result, if (param.hasThrowable()) param.throwable else null)
                callback.afterHookedMethod(hp)

                val newThrowable = hp.throwable
                if (newThrowable != null) {
                    if (newThrowable !== param.throwable) param.setThrowable(newThrowable)
                } else {
                    param.result = hp.result
                }
            }
        }
    }

    private class Wrap(private val origin: XC_MethodHook.Unhook) : Unhook {
        override fun unhook() = origin.unhook()
    }
}
