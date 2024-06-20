package com.rosan.xposed

import androidx.annotation.Keep
import com.rosan.xposed.hook.DhizukuAPI
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class HookInit @Keep constructor() : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam == null) return
        if (!lpparam.isFirstApplication) return
        if (lpparam.classLoader == null) return
        DhizukuAPI(lpparam).start()
    }
}