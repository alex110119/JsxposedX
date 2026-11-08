package com.jsxposed.x.feature.hook.quick_functions

import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.HookCallback
import com.jsxposed.x.hook.compat.HookParam
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import com.jsxposed.x.core.utils.ApkUtils
import com.jsxposed.x.feature.hook.HookImpl
import com.jsxposed.x.feature.hook.LPParam


class HookInjectTip(private val lpparam: LPParam) : HookImpl {

    lateinit var context: Context

    override fun onAttach(context: Context) {
        this.context = context
        create()
    }

    override fun create() {
        val className = ApkUtils.getResolveInfo(context, lpparam.packageName)?.name ?: return

        HookBridge.findAndHookMethod(
            className,
            lpparam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : HookCallback() {
                override fun afterHookedMethod(param: HookParam) {
                    super.afterHookedMethod(param)
                    Toast.makeText(
                        context,
                        "JsxposedX Hook: ${lpparam.packageName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}