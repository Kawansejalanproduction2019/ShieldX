package com.rhdevs.shieldx

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        // Membaca data yang disimpan dari UI multi-layar
        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val imei = pref.getString("spoof_imei", "")
        val buildId = pref.getString("spoof_build", "")
        val releaseVer = pref.getString("spoof_release", "")
        val fingerprint = pref.getString("spoof_fingerprint", "")
        
        // Data BARU
        val isFakeGps = pref.getBoolean("fake_gps_enabled", false)
        val lat = pref.getFloat("fake_gps_lat", -6.1754f).toDouble()
        val lon = pref.getFloat("fake_gps_lon", 106.8272f).toDouble()
        val isBypassSecure = pref.getBoolean("bypass_flag_secure", false)

        if (lpparam.packageName == "android" || lpparam.packageName.contains("systemui")) return

        // --- HOOK 1: Device Spoofing (Existing) ---
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

        // --- HOOK 2: AdBlock (Existing) ---
        if (isAdblock) {
            try {
                XposedHelpers.findAndHookMethod("com.google.android.gms.ads.AdView", lpparam.classLoader, "loadAd", "com.google.android.gms.ads.AdRequest", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as View
                        view.visibility = View.GONE
                        param.result = null
                    }
                })
            } catch (e: Throwable) {}
        }

        // --- HOOK 3: BARU - Fake GPS (Location Manager) ---
        if (isFakeGps) {
            try {
                // Hooking android.location.LocationManager.getLastKnownLocation
                XposedHelpers.findAndHookMethod(
                    LocationManager::class.java,
                    "getLastKnownLocation",
                    String::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val provider = param.args[0] as String
                            // Membuat lokasi palsu seolah-olah asli
                            val mockLocation = Location(provider)
                            mockLocation.latitude = lat
                            mockLocation.longitude = lon
                            mockLocation.altitude = 10.0
                            mockLocation.accuracy = 1.0f
                            mockLocation.time = System.currentTimeMillis()
                            mockLocation.elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
                            param.result = mockLocation
                        }
                    }
                )
            } catch (e: Throwable) {}
        }

        // --- HOOK 4: BARU - Bypass FLAG_SECURE (Screenshot) ---
        if (isBypassSecure) {
            try {
                // Hooking android.view.Window.setFlags
                XposedHelpers.findAndHookMethod(
                    Window::class.java,
                    "setFlags",
                    Int::class.javaPrimitiveType, // flags
                    Int::class.javaPrimitiveType, // mask
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            var flags = param.args[0] as Int
                            val mask = param.args[1] as Int
                            
                            // Jika aplikasi target mencoba memasang FLAG_SECURE
                            if (flags and WindowManager.LayoutParams.FLAG_SECURE != 0) {
                                // Hancurkan bendera secure-nya sebelum sistem menerimanya
                                flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                                param.args[0] = flags // Timpa argumen
                            }
                        }
                    }
                )
            } catch (e: Throwable) {}
        }
    }
}
