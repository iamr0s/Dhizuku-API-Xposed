package com.rosan.xposed

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class Hook(protected val lpparam: XC_LoadPackage.LoadPackageParam) {

    companion object {
        fun getClass(className: String, classLoader: ClassLoader): Class<*>? {
            return XposedHelpers.findClassIfExists(className, classLoader)
        }
    }

    open fun start() {
        if (beforeHook()) {
            hooking()
            afterHook()
        }
    }

    open fun beforeHook(): Boolean {
        return true
    }

    abstract fun hooking()

    open fun afterHook() {
    }

    fun getClass(className: String): Class<*>? {
        return getClass(className, lpparam.classLoader)
    }
}