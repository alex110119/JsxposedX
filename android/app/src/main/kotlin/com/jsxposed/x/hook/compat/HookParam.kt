package com.jsxposed.x.hook.compat

/**
 * Hook 回调参数的统一抽象。
 *
 * 语义与旧版 `XC_MethodHook.MethodHookParam` 对齐（**这一点很关键**）：
 * - [args] 是**同一个数组引用**，直接修改元素即可影响目标方法调用；
 * - 给 [result] 赋值等价于 `setResult()`：会清空异常并置位 [returnEarly]，
 *   因此在 before 阶段赋值即可短路原方法（JS 的 `param.setResult(...)` 依赖该行为）；
 * - 给 [throwable] 赋值等价于 `setThrowable()`：会清空结果并置位 [returnEarly]。
 */
class HookParam(
    val method: java.lang.reflect.Member,
    val thisObject: Any?,
    val args: Array<Any?>,
) {
    private var resultValue: Any? = null

    private var throwableValue: Throwable? = null

    var result: Any?
        get() = resultValue
        set(value) {
            resultValue = value
            throwableValue = null
            returnEarly = true
        }

    var throwable: Throwable?
        get() = throwableValue
        set(value) {
            throwableValue = value
            resultValue = null
            returnEarly = true
        }

    /** 已短路原方法（由 [result] / [throwable] 的 setter 置位）。 */
    var returnEarly: Boolean = false
        private set

    fun hasThrowable(): Boolean = throwableValue != null

    /**
     * 框架内部写回真实调用结果，不走 setter，避免误置 [returnEarly]。
     */
    internal fun setOutcome(result: Any?, throwable: Throwable?) {
        resultValue = result
        throwableValue = throwable
    }
}
