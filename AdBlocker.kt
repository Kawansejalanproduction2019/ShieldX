package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XC_MethodReplacement

object AdBlocker {
    fun applyDnsHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "java.net.InetAddress",
            lpparam.classLoader,
            "getAllByName",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val host = param.args[0] as String
                    val adHosts = listOf(
                        "googleads.g.doubleclick.net", 
                        "applovin.com", 
                        "graph.facebook.com"
                    )
                    
                    if (adHosts.contains(host)) {
                        ModuleLogger.logBlock(lpparam.packageName, "Blokir DNS: $host")
                        param.throwable = java.net.UnknownHostException("Blocked")
                    }
                }
            }
        )
    }

    fun applyViewHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.google.android.gms.ads.AdView",
                lpparam.classLoader,
                "loadAd",
                "com.google.android.gms.ads.AdRequest",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        ModuleLogger.logBlock(lpparam.packageName, "Blokir AdView loadAd")
                        return null
                    }
                }
            )
        } catch (e: Throwable) {
        }
    }
}
