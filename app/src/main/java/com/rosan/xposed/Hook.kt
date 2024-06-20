package com.rosan.xposed

import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

abstract class Hook(lpparam: XC_LoadPackage.LoadPackageParam) {
    protected val lpparam = lpparam

    companion object {
        fun isPackageName(lpparam: XC_LoadPackage.LoadPackageParam, packageName: String): Boolean {
            return lpparam.packageName.equals(packageName)
        }

        fun isProcessName(lpparam: XC_LoadPackage.LoadPackageParam, processName: String): Boolean {
            return lpparam.processName.equals(processName)
        }

        fun getClass(cls: Class<*>, classLoader: ClassLoader): Class<*>? {
            return XposedHelpers.findClassIfExists(cls.name, classLoader)
        }

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

    fun isPackageName(packageName: String): Boolean {
        return isPackageName(lpparam, packageName)
    }

    fun isProcessName(processName: String): Boolean {
        return isProcessName(lpparam, processName)
    }

    fun getClass(cls: Class<*>): Class<*>? {
        return getClass(cls, lpparam.classLoader)
    }

    fun getClass(className: String): Class<*>? {
        return getClass(className, lpparam.classLoader)
    }
}