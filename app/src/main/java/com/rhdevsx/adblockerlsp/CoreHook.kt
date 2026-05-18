package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XSharedPreferences

class CoreHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val targetApp = lpparam.packageName
        
        if (targetApp == "com.rhdevsx.adblockerlsp" || targetApp == "android") return

        val prefs = XSharedPreferences("com.rhdevsx.adblockerlsp", "config")
        prefs.makeWorldReadable()
        prefs.reload()
        
        val isAppSelected = prefs.getBoolean(targetApp, false)

        XposedBridge.log("AdBlockerLSP Memasuki: $targetApp | Status UI: $isAppSelected")

        if (isAppSelected) {
            val scheme1 = prefs.getBoolean("${targetApp}_scheme1", true)
            val scheme2 = prefs.getBoolean("${targetApp}_scheme2", false)
            val scheme3 = prefs.getBoolean("${targetApp}_scheme3", false)

            XposedBridge.log("AdBlockerLSP Eksekusi Modul untuk $targetApp S1:$scheme1 S2:$scheme2 S3:$scheme3")

            if (scheme1) AdBlocker.applyDnsHook(lpparam)
            if (scheme2) AdBlocker.applyViewHook(lpparam)
            if (scheme3) SpoofPixel.applySpoof(lpparam)
        }
    }
}
