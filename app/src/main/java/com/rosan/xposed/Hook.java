package com.rosan.xposed;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class Hook {
    protected final XC_LoadPackage.LoadPackageParam lpparam;

    protected Hook(XC_LoadPackage.LoadPackageParam lpparam) {
        this.lpparam = lpparam;
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists(className, classLoader);
    }

    public void start() {
        if (beforeHook()) {
            hooking();
            afterHook();
        }
    }

    public boolean beforeHook() {
        return true;
    }

    public abstract void hooking();

    public void afterHook() {
    }

    public Class<?> findClass(String className) {
        return findClass(className, lpparam.classLoader);
    }
}
