package com.rosan.xposed.hook

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import com.rosan.xposed.Hook
import com.rosan.xposed.hook.api.AndroidM
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlin.concurrent.thread

class DhizukuAPI(lpparam: XC_LoadPackage.LoadPackageParam) : Hook(lpparam) {
    private lateinit var context: Context

    companion object {
        lateinit var serverComponentName: ComponentName
            private set

        val serverPackageName: String
            get() = serverComponentName.packageName

        private var requesting = false
        fun <T> whenDhizukuPermissionGranted(action: () -> T): T? {
            if (Dhizuku.isPermissionGranted()) return action.invoke()
            if (!requesting) synchronized(this) {
                requesting = true
                thread {
                    Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                        override fun onRequestPermission(grantResult: Int) {
                            requesting = false
                        }
                    })
                }
            }
            return null
        }
    }

    override fun hooking() {
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    context = param?.args?.get(0) as Context
                    if (!Dhizuku.init(context)) return
                    serverComponentName = Dhizuku.getOwnerComponent()
                    AndroidM(lpparam).start()
                }
            })
    }
}