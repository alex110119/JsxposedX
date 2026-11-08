package com.jsxposed.x.hook.compat

/**
 * 与旧版 `XC_MethodHook` 等价的方法回调基类。
 *
 * 方法名保持 `beforeHookedMethod` / `afterHookedMethod`，让业务代码迁移时只需替换类型。
 * 执行顺序：before → 原方法（除非 [HookParam.returnEarly]）→ after → 返回 [HookParam.result]。
 */
abstract class HookCallback {

    /** Hook 优先级，数值越大越先执行；默认与旧版一致。 */
    open val priority: Int = PRIORITY_DEFAULT

    open fun beforeHookedMethod(param: HookParam) {}

    open fun afterHookedMethod(param: HookParam) {}

    companion object {
        const val PRIORITY_DEFAULT: Int = 50
        const val PRIORITY_HIGHEST: Int = 10000
        const val PRIORITY_LOWEST: Int = -10000
    }
}

/**
 * 与旧版 `XC_MethodReplacement` 等价：完全替换原方法实现，不执行原逻辑。
 */
abstract class ReplaceCallback : HookCallback() {

    abstract fun replaceHookedMethod(param: HookParam): Any?
}
