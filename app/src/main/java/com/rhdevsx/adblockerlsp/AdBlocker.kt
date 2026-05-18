package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XC_MethodReplacement
import java.io.File

object AdBlocker {
    
    private val blockedHosts = HashSet<String>()
    private var isLoaded = false

    private fun loadFilters() {
        if (isLoaded) return
        try {
            val filterFile = File("/data/data/com.rhdevsx.adblockerlsp/shared_prefs/compiled_hosts.txt")
            if (filterFile.exists()) {
                filterFile.useLines { lines ->
                    lines.forEach { blockedHosts.add(it.trim()) }
                }
            } else {
                blockedHosts.addAll(listOf("googleads.g.doubleclick.net", "applovin.com", "graph.facebook.com"))
            }
            isLoaded = true
        } catch (e: Throwable) {
        }
    }

    fun applyDnsHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        loadFilters()
        
        XposedHelpers.findAndHookMethod(
            "java.net.InetAddress",
            lpparam.classLoader,
            "getAllByName",
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val host = param.args[0] as String? ?: return
                    
                    if (blockedHosts.contains(host)) {
                        ModuleLogger.logBlock(lpparam.packageName, "Blokir Filter DNS: $host")
                        param.throwable = java.net.UnknownHostException("Blocked by AdBlockerLSP")
                    } else {
                        val parts = host.split(".")
                        if (parts.size >= 3) {
                            val rootDomain = parts[parts.size - 2] + "." + parts[parts.size - 1]
                            if (blockedHosts.contains(rootDomain)) {
                                ModuleLogger.logBlock(lpparam.packageName, "Blokir Filter Akar DNS: $host")
                                param.throwable = java.net.UnknownHostException("Blocked by AdBlockerLSP")
                            }
                        }
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
