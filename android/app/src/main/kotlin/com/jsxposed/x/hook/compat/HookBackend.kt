package com.jsxposed.x.hook.compat

import java.lang.reflect.Member

/**
 * Hook 能力后端。每个 flavor 提供一个实现：
 * - api100 / api101：`LegacyHookBackend`（de.robv.android.xposed）
 * - api102：`NewApi102Backend`（io.github.libxposed.api）
 *
 * 业务层只通过 [HookBridge] 访问，不直接依赖任何框架包名。
 */
interface HookBackend {

    /** 便于在日志里区分当前通道。 */
    val name: String

    fun findAndHookMethod(
        className: String,
        classLoader: ClassLoader?,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook

    fun findAndHookMethod(
        clazz: Class<*>,
        methodName: String,
        params: Array<out Any?>,
    ): Unhook

    fun findAndHookConstructor(
        className: String,
        classLoader: ClassLoader?,
        params: Array<out Any?>,
    ): Unhook

    fun findAndHookConstructor(
        clazz: Class<*>,
        params: Array<out Any?>,
    ): Unhook

    fun hookAllMethods(clazz: Class<*>, methodName: String, callback: HookCallback): Array<Unhook>

    fun hookAllConstructors(clazz: Class<*>, callback: HookCallback): Array<Unhook>

    fun invokeOriginalMethod(member: Member, thisObject: Any?, args: Array<Any?>?): Any?

    fun log(message: String)
}

/** 与旧版 `XC_MethodHook.Unhook` 等价。 */
fun interface Unhook {
    fun unhook()
}
