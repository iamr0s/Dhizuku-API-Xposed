package com.rosan.xposed;

import com.rosan.xposed.hook.DhizukuAPI;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage {
    public HookInit() {
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null) return;
        if (!lpparam.isFirstApplication) return;
        if (lpparam.classLoader == null) return;
        new DhizukuAPI(lpparam).start();
    }
}
