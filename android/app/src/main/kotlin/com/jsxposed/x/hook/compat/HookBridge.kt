package com.jsxposed.x.hook.compat

import com.jsxposed.x.core.utils.log.LogX

/**
 * 业务层唯一的 Hook 入口，签名与旧版 `XposedHelpers` / `XposedBridge` 保持一致，
 * 内部按 flavor 委派给 [HookBackend]。
 */
object HookBridge {

    private const val TAG = "HookBridge"

    @Volatile
    private var backendRef: HookBackend? = null

    fun install(backend: HookBackend) {
        backendRef = backend
        LogX.i(TAG, "HookBackend installed: ${backend.name}")
    }

    fun isInstalled(): Boolean = backendRef != null

    fun backendName(): String = backendRef?.name ?: "<none>"

    private fun backend(): HookBackend = backendRef
        ?: throw IllegalStateException("HookBackend 尚未安装，模块入口未初始化")

    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        vararg params: Any?,
    ): Unhook = backend().findAndHookMethod(className, classLoader, methodName, params)

    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        vararg params: Any?,
    ): Unhook = backend().findAndHookMethod(clazz, methodName, params)

    fun findAndHookConstructor(
        className: String,
        classLoader: ClassLoader?,
        vararg params: Any?,
    ): Unhook = backend().findAndHookConstructor(className, classLoader, params)

    fun findAndHookConstructor(
        clazz: Class<*>,
        vararg params: Any?,
    ): Unhook = backend().findAndHookConstructor(clazz, params)

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: HookCallback): Array<Unhook> =
        backend().hookAllMethods(clazz, methodName, callback)

    fun hookAllConstructors(clazz: Class<*>, callback: HookCallback): Array<Unhook> =
        backend().hookAllConstructors(clazz, callback)

    fun invokeOriginalMethod(member: java.lang.reflect.Member, thisObject: Any?, args: Array<Any?>?): Any? =
        backend().invokeOriginalMethod(member, thisObject, args)

    fun log(message: String) = backend().log(message)
}
