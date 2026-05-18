package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XSharedPreferences
import java.io.File

class CoreHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val targetApp = lpparam.packageName
        
        if (targetApp == "com.rhdevsx.adblockerlsp" || targetApp == "android") return

        val prefs = XSharedPreferences("com.rhdevsx.adblockerlsp", "config")
        prefs.makeWorldReadable()
        prefs.reload()
        
        var isAppSelected = prefs.getBoolean(targetApp, false)
        var scheme1 = prefs.getBoolean("${targetApp}_scheme1", true)
        var scheme2 = prefs.getBoolean("${targetApp}_scheme2", false)
        var scheme3 = prefs.getBoolean("${targetApp}_scheme3", false)

        val prefCanRead = prefs.file.canRead()
        var fallbackUsed = false

        if (!isAppSelected) {
            try {
                val altFile = File("/data/data/com.rhdevsx.adblockerlsp/files/config_alt.txt")
                if (altFile.exists() && altFile.canRead()) {
                    val lines = altFile.readLines()
                    for (line in lines) {
                        if (line.startsWith("$targetApp=")) {
                            isAppSelected = line.split("=")[1].toBoolean()
                            fallbackUsed = true
                        } else if (line.startsWith("${targetApp}_scheme1=")) {
                            scheme1 = line.split("=")[1].toBoolean()
                        } else if (line.startsWith("${targetApp}_scheme2=")) {
                            scheme2 = line.split("=")[1].toBoolean()
                        } else if (line.startsWith("${targetApp}_scheme3=")) {
                            scheme3 = line.split("=")[1].toBoolean()
                        }
                    }
                }
            } catch (e: Exception) {
                XposedBridge.log("AdBlockerLSP Fallback Error: ${e.message}")
            }
        }

        XposedBridge.log("AdBlockerLSP Memasuki: $targetApp | Status UI: $isAppSelected | PrefRead: $prefCanRead | Fallback: $fallbackUsed")

        if (isAppSelected) {
            XposedBridge.log("AdBlockerLSP Eksekusi Modul untuk $targetApp S1:$scheme1 S2:$scheme2 S3:$scheme3")

            if (scheme1) AdBlocker.applyDnsHook(lpparam)
            if (scheme2) AdBlocker.applyViewHook(lpparam)
            if (scheme3) SpoofPixel.applySpoof(lpparam)
        }
    }
}
