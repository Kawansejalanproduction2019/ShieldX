package com.rhdevs.shieldx

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.net.UnknownHostException

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    // Daftar hitam server iklan (Template AdBlockList)
    private val adDomains = listOf(
        "googleads", "admob", "doubleclick", "applovin", "unityads",
        "vungle", "inmobi", "chartboost", "adcolony", "amazon-adsystem",
        "supersonicads", "appsflyer", "crashlytics", "scorecardresearch",
        "flurry", "mopub", "smaato", "startapp", "tiktok.com/api/ad"
    )

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val imei = pref.getString("spoof_imei", "")
        val buildId = pref.getString("spoof_build", "")
        val releaseVer = pref.getString("spoof_release", "")
        val fingerprint = pref.getString("spoof_fingerprint", "")
        
        val isFakeGps = pref.getBoolean("fake_gps_enabled", false)
        val lat = pref.getFloat("fake_gps_lat", -6.1754f).toDouble()
        val lon = pref.getFloat("fake_gps_lon", 106.8272f).toDouble()
        val isBypassSecure = pref.getBoolean("bypass_flag_secure", false)

        if (lpparam.packageName == "android" || lpparam.packageName.contains("systemui")) return

        // --- HOOK 1: Device Spoofing ---
        if (!buildId.isNullOrEmpty()) {
            XposedHelpers.setStaticObjectField(Build::class.java, "ID", buildId)
            XposedHelpers.setStaticObjectField(Build::class.java, "DISPLAY", buildId)
        }
        if (!releaseVer.isNullOrEmpty()) {
            XposedHelpers.setStaticObjectField(Build.VERSION::class.java, "RELEASE", releaseVer)
        }
        if (!fingerprint.isNullOrEmpty()) {
            XposedHelpers.setStaticObjectField(Build::class.java, "FINGERPRINT", fingerprint)
        }
        if (!imei.isNullOrEmpty()) {
            try {
                XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getDeviceId", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { param.result = imei }
                })
            } catch (e: Throwable) {}
        }

        // --- HOOK 2: MESIN PEMBLOKIR IKLAN TINGKAT JARINGAN (DNS HIJACK) ---
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
                            
                            // Pindai apakah tautan mengandung keyword server iklan
                            for (domain in adDomains) {
                                if (host.contains(domain, ignoreCase = true)) {
                                    // HANCURKAN TAUTAN: Paksa error DNS secara instan
                                    param.throwable = UnknownHostException("Koneksi iklan diputus oleh ShieldX")
                                    return
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}

            // Tambahan: Blokir pemuatan URL iklan di WebView
            try {
                XposedHelpers.findAndHookMethod(
                    "android.webkit.WebView",
                    lpparam.classLoader,
                    "loadUrl",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val url = param.args[0] as? String ?: return
                            for (domain in adDomains) {
                                if (url.contains(domain, ignoreCase = true)) {
                                    // Kosongkan URL agar WebView memuat halaman kosong
                                    param.args[0] = "about:blank"
                                    return
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        // --- HOOK 3: Fake GPS ---
        if (isFakeGps) {
            try {
                XposedHelpers.findAndHookMethod(LocationManager::class.java, "getLastKnownLocation", String::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val provider = param.args[0] as String
                        val mockLocation = Location(provider).apply {
                            latitude = lat
                            longitude = lon
                            altitude = 10.0
                            accuracy = 1.0f
                            time = System.currentTimeMillis()
                            elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                        }
                        param.result = mockLocation
                    }
                })
            } catch (e: Throwable) {}
        }

        // --- HOOK 4: Bypass FLAG_SECURE ---
        if (isBypassSecure) {
            try {
                XposedHelpers.findAndHookMethod(Window::class.java, "setFlags", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        var flags = param.args[0] as Int
                        if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                            flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            param.args[0] = flags
                        }
                    }
                })
            } catch (e: Throwable) {}
        }
    }
}
