package com.jsxposed.x.feature.hook.quick_functions

import com.jsxposed.x.hook.compat.HookBridge
import com.jsxposed.x.hook.compat.HookCallback
import com.jsxposed.x.hook.compat.HookParam
import android.content.Context
import com.jsxposed.x.feature.hook.HookImpl
import com.jsxposed.x.feature.hook.LPParam



class HookVpn(private val lpparam: LPParam) : HookImpl {
    lateinit var context: Context
    override fun onAttach(context: Context) {
        this.context = context
        create()
    }

    override fun create() {
        HookBridge.findAndHookMethod(
            "java.net.NetworkInterface",
            lpparam.classLoader,
            "getName",
            object : HookCallback() {
                override fun beforeHookedMethod(param: HookParam) {
                    super.beforeHookedMethod(param)
                    param.result = "hello"
                }
            }
        )
    }

}
