package com.rosan.xposed.hook;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;
import com.rosan.xposed.Hook;
import com.rosan.xposed.hook.api.AndroidM;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DhizukuAPI extends Hook {
    private static ComponentName serverComponentName;
    private static boolean requesting;

    public DhizukuAPI(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    public static ComponentName getServerComponentName() {
        return serverComponentName;
    }

    public static String getServerPackageName() {
        return serverComponentName.getPackageName();
    }

    private static boolean isPermissionGranted() {
        try {
            return Dhizuku.isPermissionGranted();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public interface Action<T> {
        T run();
    }

    public static <T> T whenDhizukuPermissionGranted(Action<T> action) {
        if (isPermissionGranted()) return action.run();
        if (!requesting) {
            synchronized (DhizukuAPI.class) {
                if (!requesting) {
                    requesting = true;
                    new Thread(() -> {
                        try {
                            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                                @Override
                                public void onRequestPermission(int grantResult) {
                                    requesting = false;
                                }
                            });
                        } catch (Throwable ignored) {
                            requesting = false;
                        }
                    }).start();
                }
            }
        }
        return null;
    }

    @Override
    public void hooking() {
        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param == null || param.args.length == 0) return;
                        Context context = (Context) param.args[0];
                        if (!Dhizuku.init(context)) return;
                        serverComponentName = Dhizuku.getOwnerComponent();
                        new AndroidM(lpparam).start();
                    }
                });
    }
}
