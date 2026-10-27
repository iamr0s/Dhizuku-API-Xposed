package com.rosan.xposed.hook.api;

import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.os.IBinder;

import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuBinderWrapper;
import com.rosan.xposed.Hook;
import com.rosan.xposed.hook.DhizukuAPI;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class AndroidM extends Hook {
    public AndroidM(XC_LoadPackage.LoadPackageParam lpparam) {
        super(lpparam);
    }

    @Override
    public boolean beforeHook() {
        return true;
    }

    @Override
    public void hooking() {
        hookIsOwner();
        hookPackageNameGet();
        hookDeviceOwnerComponentName();
        hookOwnerPermission();
    }

    private void hookIsOwner() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param == null || param.args.length == 0) return;
                DhizukuAPI.whenDhizukuPermissionGranted(() -> {
                    param.args[0] = DhizukuAPI.getServerPackageName();
                    return null;
                });
            }
        };
        XposedHelpers.findAndHookMethod(
                DevicePolicyManager.class,
                "isDeviceOwnerApp",
                String.class,
                hook);
        XposedHelpers.findAndHookMethod(
                DevicePolicyManager.class,
                "isProfileOwnerApp",
                String.class,
                hook);
    }

    private void hookPackageNameGet() {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param == null) return;
                if (!lpparam.packageName.equals(param.getResult())) return;
                boolean hookIt = false;
                for (StackTraceElement stack : new Exception().getStackTrace()) {
                    if (stack.getClassName().equals(Dhizuku.class.getName())) {
                        hookIt = false;
                        break;
                    } else if (stack.getClassName().equals(DevicePolicyManager.class.getName())) {
                        hookIt = true;
                        break;
                    }
                }
                if (!hookIt) return;
                DhizukuAPI.whenDhizukuPermissionGranted(() -> {
                    param.setResult(DhizukuAPI.getServerPackageName());
                    return null;
                });
            }
        };
        Class<?> contextImpl = findClass("android.app.ContextImpl");
        XposedHelpers.findAndHookMethod(contextImpl, "getPackageName", hook);
        XposedHelpers.findAndHookMethod(contextImpl, "getOpPackageName", hook);
    }

    private void hookDeviceOwnerComponentName() {
        for (final Method method : DevicePolicyManager.class.getDeclaredMethods()) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    if (param == null) return;
                    DevicePolicyManager service = (DevicePolicyManager) param.thisObject;
                    DhizukuAPI.whenDhizukuPermissionGranted(() -> {
                        proxyDevicePolicyManager(service);
                        for (int i = 0; i < param.args.length; i++) {
                            if (param.args[i] instanceof ComponentName) {
                                param.args[i] = DhizukuAPI.getServerComponentName();
                            }
                        }
                        if (method.getName().contains("Delegated") && param.args.length > 1) {
                            param.args[1] = DhizukuAPI.getServerPackageName();
                        }
                        return null;
                    });
                }
            });
        }
    }

    private void hookOwnerPermission() {
        XposedHelpers.findAndHookMethod(
                DevicePolicyManager.class,
                "isActivePasswordSufficient",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param != null) param.setResult(true);
                    }
                });
    }

    private Field devicePolicyManagerServiceField;

    private void proxyDevicePolicyManager(DevicePolicyManager service) {
        try {
            Field field = devicePolicyManagerServiceField;
            if (field == null) {
                field = service.getClass().getDeclaredField("mService");
                devicePolicyManagerServiceField = field;
            }
            field.setAccessible(true);
            IDevicePolicyManager manager = (IDevicePolicyManager) field.get(service);
            IBinder binder = manager.asBinder();
            if (binder instanceof DhizukuBinderWrapper) return;
            field.set(service, IDevicePolicyManager.Stub.asInterface(Dhizuku.binderWrapper(binder)));
        } catch (Throwable ignored) {
        }
    }
}
