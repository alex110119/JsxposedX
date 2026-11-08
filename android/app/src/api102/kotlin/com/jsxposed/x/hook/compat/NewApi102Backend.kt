package com.jsxposed.x.hook.compat

import android.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * libxposed API 102 后端。
 *
 * API 102 只提供 `hook(Executable).intercept(Chain)`，没有 `XposedHelpers` 那一套；
 * 这里负责把 [HookCallback] 的 before/after 语义还原到拦截器链上：
 *
 * ```
 * before 回调（可改 args / setResult 短路）
 *   → chain.proceed(args)（可被跳过）
 *   → after 回调（可改 result / throwable）
 *   → 返回最终 result
 * ```
 */
class NewApi102Backend(private val module: XposedInterface) : HookBackend {

    override val name: String = "libxposed(api102)"

    override fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val loader = classLoader ?: fallbackClassLoader()
        val types = resolveTypes(params, loader)
        val clazz = ReflectX.findClass(className, loader)
        return hookExecutable(ReflectX.findMethodExact(clazz, methodName, types), callback)
    }

    override fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val types = resolveTypes(params, clazz.classLoader ?: fallbackClassLoader())
        return hookExecutable(ReflectX.findMethodExact(clazz, methodName, types), callback)
    }

    override fun findAndHookConstructor(
        className: String,
        classLoader: ClassLoader?,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val loader = classLoader ?: fallbackClassLoader()
        val types = resolveTypes(params, loader)
        val clazz = ReflectX.findClass(className, loader)
        return hookExecutable(ReflectX.findConstructorExact(clazz, types), callback)
    }

    override fun findAndHookConstructor(
        clazz: Class<*>,
        params: Array<out Any?>,
    ): Unhook {
        val callback = extractCallback(params)
        val types = resolveTypes(params, clazz.classLoader ?: fallbackClassLoader())
        return hookExecutable(ReflectX.findConstructorExact(clazz, types), callback)
    }

    override fun hookAllMethods(clazz: Class<*>, methodName: String, callback: HookCallback): Array<Unhook> =
        clazz.declaredMethods
            .filter { it.name == methodName }
            .map { hookExecutable(it, callback) }
            .toTypedArray()

    override fun hookAllConstructors(clazz: Class<*>, callback: HookCallback): Array<Unhook> =
        clazz.declaredConstructors
            .map { hookExecutable(it, callback) }
            .toTypedArray()

    override fun invokeOriginalMethod(member: Member, thisObject: Any?, args: Array<Any?>?): Any? {
        val realArgs: Array<Any?> = args ?: emptyArray()
        return when (member) {
            is Method -> module.getInvoker(member)
                .setType(XposedInterface.Invoker.Type.ORIGIN)
                .invoke(thisObject, *realArgs)

            is Constructor<*> -> {
                @Suppress("UNCHECKED_CAST")
                val ctor = member as Constructor<Any>
                module.getInvoker(ctor)
                    .setType(XposedInterface.Invoker.Type.ORIGIN)
                    .invoke(null, *realArgs)
            }

            else -> throw IllegalArgumentException("不支持的成员类型: ${member.javaClass.name}")
        }
    }

    override fun log(message: String) {
        // 与旧版 XposedBridge.log 保持完全一致的 tag，模块内的 logcat 控制台解析依赖它
        Log.i(LOG_TAG, message)
    }

    // ── 内部实现 ──

    private fun hookExecutable(executable: Executable, callback: HookCallback): Unhook {
        val handle = module.hook(executable)
            .setPriority(callback.priority)
            .intercept { chain -> dispatch(chain, callback) }
        return Handle(handle)
    }

    private fun dispatch(chain: XposedInterface.Chain, callback: HookCallback): Any? {
        val args: Array<Any?> = chain.args.toTypedArray()
        val param = HookParam(chain.executable, chain.thisObject, args)

        if (callback is ReplaceCallback) {
            return callback.replaceHookedMethod(param)
        }

        callback.beforeHookedMethod(param)

        if (param.returnEarly) {
            param.throwable?.let { throw it }
            return param.result
        }

        val result: Any?
        try {
            result = chain.proceed(param.args)
        } catch (t: Throwable) {
            param.setOutcome(null, t)
            callback.afterHookedMethod(param)
            val replaced = param.throwable
            if (replaced != null && replaced !== t) throw replaced
            throw t
        }

        param.setOutcome(result, null)
        callback.afterHookedMethod(param)
        param.throwable?.let { throw it }
        return param.result
    }

    private fun extractCallback(params: Array<out Any?>): HookCallback =
        params.lastOrNull() as? HookCallback
            ?: throw IllegalArgumentException("最后一个参数必须是 HookCallback")

    private fun resolveTypes(params: Array<out Any?>, classLoader: ClassLoader): Array<Class<*>> =
        params.dropLast(1).map { raw ->
            when (raw) {
                is Class<*> -> raw
                is String -> ReflectX.findClass(raw, classLoader)
                else -> throw IllegalArgumentException("无法解析的参数类型: $raw")
            }
        }.toTypedArray()

    private fun fallbackClassLoader(): ClassLoader =
        Thread.currentThread().contextClassLoader
            ?: NewApi102Backend::class.java.classLoader
            ?: ClassLoader.getSystemClassLoader()

    private class Handle(private val origin: XposedInterface.HookHandle) : Unhook {
        override fun unhook() = origin.unhook()
    }

    private companion object {
        const val LOG_TAG = "Xposed"
    }
}
