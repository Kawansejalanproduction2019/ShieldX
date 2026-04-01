package com.rhdevs.shieldx

import android.location.Location
import android.location.LocationManager
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

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    private fun log(msg: String) {
        XposedBridge.log("ShieldX-Debug [${System.currentTimeMillis()}]: $msg")
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        // Log aplikasi yang sedang ditarget
        log("Injected into: ${lpparam.packageName}")

        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val isFakeGps = pref.getBoolean("fake_gps_enabled", false)
        val isBypassSecure = pref.getBoolean("bypass_flag_secure", false)

        if (lpparam.packageName == "android" || lpparam.packageName.contains("systemui")) return

        // --- 1. FAKE GPS REPAIR ---
        if (isFakeGps) {
            val lat = pref.getFloat("fake_gps_lat", -6.1754f).toDouble()
            val lon = pref.getFloat("fake_gps_lon", 106.8272f).toDouble()
            
            try {
                // Hook multiple methods untuk memastikan lokasi benar-benar terkunci
                val locationMethods = listOf("getLastKnownLocation", "getLastLocation")
                locationMethods.forEach { methodName ->
                    XposedHelpers.findAndHookMethod(
                        LocationManager::class.java, methodName, String::class.java, 
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                val provider = param.args[0] as? String ?: LocationManager.GPS_PROVIDER
                                param.result = createFakeLocation(provider, lat, lon)
                                log("Spoofed location for $methodName to $lat, $lon")
                            }
                        }
                    )
                }
            } catch (e: Throwable) { log("Fake GPS Error: ${e.message}") }
        }

        // --- 2. IDENTITY SPOOFING (IMEI/SERIAL) - ANTI CRASH ---
        val imei = pref.getString("spoof_imei", "")
        if (!imei.isNullOrEmpty()) {
            try {
                // Hook TelephonyManager dengan proteksi
                XposedHelpers.findAndHookMethod("android.telephony.TelephonyManager", lpparam.classLoader, "getDeviceId", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) { param.result = imei }
                })
                // Hook untuk Serial Number
                XposedHelpers.setStaticObjectField(Build::class.java, "SERIAL", "RHDEVS-${System.currentTimeMillis()/1000}")
            } catch (e: Throwable) { log("Identity Spoof Error (Safe Skip): ${e.message}") }
        }

        // --- 3. ADBLOCK DNS HIJACK ---
        if (isAdblock) {
            val adKeywords = listOf("ads", "metrics", "analytics", "doubleclick", "applovin", "vungle")
            try {
                XposedHelpers.findAndHookMethod("java.net.InetAddress", lpparam.classLoader, "getAllByName", String::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val host = param.args[0] as? String ?: return
                        if (adKeywords.any { host.contains(it) }) {
                            log("Blocked AD Domain: $host")
                            param.throwable = UnknownHostException("ShieldX AdBlock")
                        }
                    }
                })
            } catch (e: Throwable) { log("AdBlock Error: ${e.message}") }
        }

        // --- 4. BYPASS FLAG_SECURE ---
        if (isBypassSecure) {
            try {
                XposedHelpers.findAndHookMethod(Window::class.java, "setFlags", Int::class.java, Int::class.java, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        var flags = param.args[0] as Int
                        if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                            param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            log("Bypassed FLAG_SECURE in ${lpparam.packageName}")
                        }
                    }
                })
            } catch (e: Throwable) {}
        }
    }

    private fun createFakeLocation(provider: String, lat: Double, lon: Double): Location {
        return Location(provider).apply {
            latitude = lat
            longitude = lon
            altitude = 30.0
            accuracy = 5.0f
            time = System.currentTimeMillis()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
            }
        }
    }
}
