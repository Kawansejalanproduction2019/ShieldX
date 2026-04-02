package com.rhdevs.shieldx

import android.location.Location
import android.os.Build
import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.net.UnknownHostException
import java.net.URL

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    private val adHosts = listOf(
        "googleads", "doubleclick", "admob", "applovin", "unityads", "vungle",
        "inmobi", "chartboost", "adcolony", "amazon-adsystem", "appsflyer",
        "crashlytics", "scorecardresearch", "flurry", "mopub", "smaato",
        "startapp", "tiktok.com/api/ad", "facebook.com/audiencenetwork",
        "supersonicads", "tapjoy", "ironbeads", "admarvel", "adx", "bidswitch",
        "rubiconproject", "openx", "pubmatic", "criteo", "outbrain", "taboola",
        "googlesyndication", "safeframe", "adsafeprotected", "omtrdc",
        "imrworldwide", "lijit", "casalemedia", "mathtag", "advertising",
        "moatads", "exponential", "quantserve", "adtech", "tremorhub"
    )

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        val targetPackage = lpparam.packageName
        
        val isFakeGps = pref.getBoolean("fake_gps_enabled", false)
        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val isBypassSecure = pref.getBoolean("bypass_flag_secure", false)

        if (isFakeGps) {
            val lat = pref.getFloat("fake_gps_lat", -6.1754f).toDouble()
            val lon = pref.getFloat("fake_gps_lon", 106.8272f).toDouble()

            try {
                XposedHelpers.findAndHookMethod(
                    Location::class.java,
                    "getLatitude",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = lat
                        }
                    }
                )
                
                XposedHelpers.findAndHookMethod(
                    Location::class.java,
                    "getLongitude",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = lon
                        }
                    }
                )

                XposedHelpers.findAndHookMethod(
                    Location::class.java,
                    "isFromMockProvider",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            param.result = false
                        }
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    XposedHelpers.findAndHookMethod(
                        Location::class.java,
                        "isMock",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                param.result = false
                            }
                        }
                    )
                }
            } catch (e: Throwable) {
                XposedBridge.log("ShieldX GPS Error: " + e.message)
            }
        }

        if (targetPackage == "android" || targetPackage.contains("systemui")) return

        if (isAdblock) {
            try {
                XposedHelpers.findAndHookMethod(
                    "java.net.InetAddress",
                    lpparam.classLoader,
                    "getAllByName",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val host = param.args[0] as? String ?: return
                            val hostLower = host.lowercase()
                            for (ad in adHosts) {
                                if (hostLower.contains(ad)) {
                                    param.throwable = UnknownHostException("Blocked by ShieldX")
                                    return
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}

            try {
                XposedHelpers.findAndHookMethod(
                    "android.webkit.WebView",
                    lpparam.classLoader,
                    "loadUrl",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val url = param.args[0] as? String ?: return
                            val urlLower = url.lowercase()
                            for (ad in adHosts) {
                                if (urlLower.contains(ad)) {
                                    param.args[0] = "about:blank"
                                    return
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}

            try {
                XposedHelpers.findAndHookMethod(
                    "java.net.URL",
                    lpparam.classLoader,
                    "openConnection",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val urlObj = param.thisObject as URL
                            val host = urlObj.host.lowercase()
                            for (ad in adHosts) {
                                if (host.contains(ad)) {
                                    param.throwable = java.io.IOException("Connection Blocked by ShieldX")
                                    return
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        val imei = pref.getString("spoof_imei", "")
        if (imei != null && imei.isNotEmpty()) {
            try {
                XposedHelpers.findAndHookMethod(
                    "android.telephony.TelephonyManager", 
                    lpparam.classLoader, 
                    "getDeviceId", 
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.hasThrowable()) return
                            param.result = imei 
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        if (isBypassSecure) {
            try {
                XposedHelpers.findAndHookMethod(
                    Window::class.java, 
                    "setFlags", 
                    Int::class.javaPrimitiveType, 
                    Int::class.javaPrimitiveType, 
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            var flags = param.args[0] as Int
                            if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                                param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}
        }
    }
}
