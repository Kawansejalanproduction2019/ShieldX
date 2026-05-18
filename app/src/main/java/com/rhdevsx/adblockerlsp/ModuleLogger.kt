package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.XposedBridge

object ModuleLogger {
    fun logBlock(appName: String, action: String) {
        XposedBridge.log("AdBlockerLSP: [$appName] $action")
    }
}
