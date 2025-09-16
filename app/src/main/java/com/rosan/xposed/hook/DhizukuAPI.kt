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
                val ctx = param?.args?.get(0) as? Context ?: return

                // ✅ 初始化 Dhizuku
                if (!Dhizuku.init(ctx)) {
                    XposedBridge.log("[DhizukuAPI] Dhizuku.init() failed")
                    return
                }

                serverComponentName = Dhizuku.getOwnerComponent()
                XposedBridge.log("[DhizukuAPI] Dhizuku initialized, server: $serverComponentName")

                // ✅ 主动触发权限请求（延迟2秒，确保应用在前台）
                thread {
                    Thread.sleep(2000)
                    Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                        override fun onRequestPermission(grantResult: Int) {
                            XposedBridge.log("[DhizukuAPI] Permission result: $grantResult")
                        }
                    })
                }

                // ✅ 启动 AndroidM hook
                AndroidM(lpparam).start()
            }
        })
}

}
