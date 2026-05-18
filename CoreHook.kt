package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XSharedPreferences

class CoreHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val prefs = XSharedPreferences("com.rhdevsx.adblockerlsp", "config")
        prefs.makeWorldReadable()
        
        val targetApp = lpparam.packageName
        val isEnabled = prefs.getBoolean(targetApp, false)

        if (isEnabled) {
            val scheme1 = prefs.getBoolean("${targetApp}_scheme1", true)
            val scheme2 = prefs.getBoolean("${targetApp}_scheme2", false)
            val scheme3 = prefs.getBoolean("${targetApp}_scheme3", false)

            ModuleLogger.logBlock(targetApp, "Modul aktif untuk aplikasi ini")

            if (scheme1) AdBlocker.applyDnsHook(lpparam)
            if (scheme2) AdBlocker.applyViewHook(lpparam)
            if (scheme3) SpoofPixel.applySpoof(lpparam)
        }
    }
}
