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
import java.net.URL

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val isFakeGps = pref.getBoolean("fake_gps_enabled", false)
        val isBypassSecure = pref.getBoolean("bypass_flag_secure", false)

        /* STRATEGI 1 FAKE GPS TINGKAT DEWA */
        /* Kita manipulasi langsung dari objek Location, sehingga WhatsApp dan Maps pasti tertipu */
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
            } catch (e: Throwable) {
                XposedBridge.log("ShieldX GPS Error: " + e.message)
            }
        }

        /* STRATEGI 2 ANTI CRASH UNTUK DEVCHECK */
        /* Kita periksa apakah sistem melempar error, jika iya biarkan saja agar tidak crash */
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
            } catch (e: Throwable) {
                XposedBridge.log("ShieldX IMEI Error: " + e.message)
            }
        }

        /* STRATEGI 3 PEMBLOKIR IKLAN JALUR HTTP */
        /* Hancurkan koneksi saat aplikasi mencoba membuka tautan iklan */
        if (isAdblock) {
            try {
                XposedHelpers.findAndHookMethod(
                    "java.net.URL",
                    lpparam.classLoader,
                    "openConnection",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val urlObj = param.thisObject as URL
                            val host = urlObj.host.lowercase()
                            val adWords = listOf("ads", "admob", "doubleclick", "applovin", "unityads", "vungle")
                            
                            for (word in adWords) {
                                if (host.contains(word)) {
                                    param.throwable = java.io.IOException("Koneksi Iklan Diputus ShieldX")
                                    break
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                XposedBridge.log("ShieldX AdBlock Error: " + e.message)
            }
        }

        /* STRATEGI 4 BYPASS FLAG SECURE */
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
