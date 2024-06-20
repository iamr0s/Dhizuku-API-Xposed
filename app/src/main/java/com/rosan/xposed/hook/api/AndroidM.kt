package com.rosan.xposed.hook.api

import android.app.admin.DevicePolicyManager
import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.PackageInstaller
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuBinderWrapper
import com.rosan.xposed.Hook
import com.rosan.xposed.hook.DhizukuAPI
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.lang.reflect.Field

class AndroidM(lpparam: XC_LoadPackage.LoadPackageParam) : Hook(lpparam) {

    override fun beforeHook(): Boolean = true

    override fun hooking() {
        hookIsOwner()
        hookPackageNameGet()
        hookDeviceOwnerComponentName()
        hookPackageInstaller()
        hookOwnerPermission()
    }

    private fun hookIsOwner() {
        val isOwnerHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                super.beforeHookedMethod(param)
                if (param == null) return
                if (param.args.isEmpty()) return
                DhizukuAPI.whenDhizukuPermissionGranted {
                    param.args[0] = DhizukuAPI.serverPackageName
                }
            }
        }
        XposedHelpers.findAndHookMethod(
            DevicePolicyManager::class.java,
            "isDeviceOwnerApp",
            String::class.java,
            isOwnerHook
        )
        XposedHelpers.findAndHookMethod(
            DevicePolicyManager::class.java,
            "isProfileOwnerApp",
            String::class.java,
            isOwnerHook
        )
    }

    private fun hookPackageNameGet() {
        val packageNameHook = object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                super.afterHookedMethod(param)
                if (param == null) return
                val result = param.result as String?
                if (result != lpparam.packageName) return
                var hookIt = false
                kotlin.runCatching { throw Exception() }
                    .exceptionOrNull()?.stackTrace?.let {
                        for (stack in it) {
                            if (stack.className == Dhizuku::class.java.name) {
                                hookIt = false
                                break
                            } else if (stack.className == DevicePolicyManager::class.java.name) {
                                hookIt = true
                                break
                            }
                        }
                    }
                if (!hookIt) return
                DhizukuAPI.whenDhizukuPermissionGranted {
                    param.result = DhizukuAPI.serverPackageName
                }
            }
        }
        XposedHelpers.findAndHookMethod(
            getClass("android.app.ContextImpl"),
            "getPackageName",
            packageNameHook
        )
        XposedHelpers.findAndHookMethod(
            getClass("android.app.ContextImpl"),
            "getOpPackageName",
            packageNameHook
        )
    }

    private fun hookDeviceOwnerComponentName() {
        val devicePolicyManagerClazz = DevicePolicyManager::class.java
        devicePolicyManagerClazz.declaredMethods.forEach {
            XposedBridge.hookMethod(
                it,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        if (param == null) return
                        val service = param.thisObject as DevicePolicyManager
                        DhizukuAPI.whenDhizukuPermissionGranted {
                            proxyDevicePolicyManager(service)
                            param.args.forEachIndexed { index, any ->
                                if (any !is ComponentName) return@forEachIndexed
                                param.args[index] = DhizukuAPI.serverComponentName
                            }
                            if (it.name.contains("Delegated")) {
                                param.args[1] = DhizukuAPI.serverComponentName
                            }
                        }
                    }
                }
            )
        }
    }

    private fun hookPackageInstaller() {
        XposedHelpers.findAndHookMethod(
            getClass("android.app.ApplicationPackageManager"),
            "getPackageInstaller",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val installer = param?.result as PackageInstaller? ?: return
                    DhizukuAPI.whenDhizukuPermissionGranted {
                        proxyPackageInstaller(installer)
                    }
                }
            }
        )
        XposedHelpers.findAndHookConstructor(
            PackageInstaller.Session::class.java,
            IPackageInstallerSession::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    val session = param?.thisObject as PackageInstaller.Session? ?: return
                    DhizukuAPI.whenDhizukuPermissionGranted {
                        proxyPackageInstallerSession(session)
                    }
                }
            }
        )
    }

    private fun hookOwnerPermission() {
        XposedHelpers.findAndHookMethod(
            DevicePolicyManager::class.java,
            "isActivePasswordSufficient",
            object :
                XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    param?.result = true
                }
            })
    }

    private var packageInstallerSessionField: Field? = null

    private fun proxyPackageInstallerSession(packageInstallerSession: PackageInstaller.Session) {
        val field = packageInstallerSessionField
            ?: packageInstallerSession::class.java.getDeclaredField("mSession")
        packageInstallerSessionField = field
        field.isAccessible = true
        val session = field.get(packageInstallerSession) as IPackageInstallerSession
        val binder = session.asBinder()
        if (binder is DhizukuBinderWrapper) return
        val newBinder = Dhizuku.binderWrapper(binder)
        val newSession = IPackageInstallerSession.Stub.asInterface(newBinder)
        field.set(packageInstallerSession, newSession)
    }

    private var packageInstallerServiceField: Field? = null

    private var packageInstallerNameField: Field? = null

    private fun proxyPackageInstaller(packageInstaller: PackageInstaller) {
        (packageInstallerServiceField
            ?: packageInstaller::class.java.getDeclaredField("mInstaller")).let {
            packageInstallerServiceField = it
            it.isAccessible = true
            val installer = it.get(packageInstaller) as IPackageInstaller
            val binder = installer.asBinder()
            if (binder is DhizukuBinderWrapper) return@let
            val newBinder = Dhizuku.binderWrapper(binder)
            val newInstaller = IPackageInstaller.Stub.asInterface(newBinder)
            it.set(packageInstaller, newInstaller)
        }
        (packageInstallerNameField
            ?: packageInstaller::class.java.getDeclaredField("mInstallerPackageName")).let {
            packageInstallerNameField = it
            it.isAccessible = true
            val packageName = it.get(packageInstaller) as String?
            if (packageName != lpparam.packageName) return
            it.set(packageInstaller, DhizukuAPI.serverPackageName)
        }
    }

    private var devicePolicyManagerServiceField: Field? = null

    private fun proxyDevicePolicyManager(service: DevicePolicyManager) {
        val field =
            devicePolicyManagerServiceField ?: service::class.java.getDeclaredField("mService")
        devicePolicyManagerServiceField = field
        field.isAccessible = true
        val manager = field.get(service) as IDevicePolicyManager
        val binder = manager.asBinder()
        if (binder is DhizukuBinderWrapper) return
        val newBinder = Dhizuku.binderWrapper(binder)
        val newManager = IDevicePolicyManager.Stub.asInterface(newBinder)
        field.set(service, newManager)
    }
}
