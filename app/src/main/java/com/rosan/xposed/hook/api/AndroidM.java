package com.rosan.xposed.hook.api;

import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.pm.IPackageInstaller;
import android.content.pm.IPackageInstallerSession;
import android.content.pm.PackageInstaller;
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
        hookPackageInstaller();
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

    private void hookPackageInstaller() {
        XposedHelpers.findAndHookMethod(
                findClass("android.app.ApplicationPackageManager"),
                "getPackageInstaller",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param == null || !(param.getResult() instanceof PackageInstaller)) return;
                        PackageInstaller installer = (PackageInstaller) param.getResult();
                        DhizukuAPI.whenDhizukuPermissionGranted(() -> {
                            proxyPackageInstaller(installer);
                            return null;
                        });
                    }
                });

        XposedHelpers.findAndHookConstructor(
                PackageInstaller.Session.class,
                IPackageInstallerSession.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        if (param == null || !(param.thisObject instanceof PackageInstaller.Session)) return;
                        PackageInstaller.Session session = (PackageInstaller.Session) param.thisObject;
                        DhizukuAPI.whenDhizukuPermissionGranted(() -> {
                            proxyPackageInstallerSession(session);
                            return null;
                        });
                    }
                });
    }

    private void hookOwnerPermission() {
        XposedHelpers.findAndHookMethod(
                DevicePolicyManager.class,
                "isActivePasswordSufficient",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (param != null) param.setResult(true);
                    }
                });
    }

    private Field packageInstallerSessionField;

    private void proxyPackageInstallerSession(PackageInstaller.Session packageInstallerSession) {
        try {
            Field field = packageInstallerSessionField;
            if (field == null) {
                field = packageInstallerSession.getClass().getDeclaredField("mSession");
                packageInstallerSessionField = field;
            }
            field.setAccessible(true);
            IPackageInstallerSession session = (IPackageInstallerSession) field.get(packageInstallerSession);
            IBinder binder = session.asBinder();
            if (binder instanceof DhizukuBinderWrapper) return;
            field.set(packageInstallerSession,
                    IPackageInstallerSession.Stub.asInterface(Dhizuku.binderWrapper(binder)));
        } catch (Throwable ignored) {
        }
    }

    private Field packageInstallerServiceField;
    private Field packageInstallerNameField;

    private void proxyPackageInstaller(PackageInstaller packageInstaller) {
        try {
            Field serviceField = packageInstallerServiceField;
            if (serviceField == null) {
                serviceField = packageInstaller.getClass().getDeclaredField("mInstaller");
                packageInstallerServiceField = serviceField;
            }
            serviceField.setAccessible(true);
            IPackageInstaller installer = (IPackageInstaller) serviceField.get(packageInstaller);
            IBinder binder = installer.asBinder();
            if (!(binder instanceof DhizukuBinderWrapper)) {
                serviceField.set(packageInstaller,
                        IPackageInstaller.Stub.asInterface(Dhizuku.binderWrapper(binder)));
            }

            Field nameField = packageInstallerNameField;
            if (nameField == null) {
                nameField = packageInstaller.getClass().getDeclaredField("mInstallerPackageName");
                packageInstallerNameField = nameField;
            }
            nameField.setAccessible(true);
            if (!lpparam.packageName.equals(nameField.get(packageInstaller))) return;
            nameField.set(packageInstaller, DhizukuAPI.getServerPackageName());
        } catch (Throwable ignored) {
        }
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
