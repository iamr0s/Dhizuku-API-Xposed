package com.rosan.xposed.util

import de.robv.android.xposed.XposedBridge

fun log(any: Any?) {
    if (any is Throwable) {
        XposedBridge.log(any)
    } else {
        XposedBridge.log(any.toString())
    }
}