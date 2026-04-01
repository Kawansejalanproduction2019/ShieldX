package com.rhdevs.shieldx

import android.os.Build
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class CoreHook : IXposedHookLoadPackage {

    private val pref = XSharedPreferences("com.rhdevs.shieldx", "shield_config")

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        pref.reload()
        
        val isAdblock = pref.getBoolean("adblock_enabled", false)
        val imei = pref.getString("spoof_imei", "")
        val buildId = pref.getString("spoof_build", "")
        val releaseVer = pref.getString("spoof_release", "")
        val fingerprint = pref.getString("spoof_fingerprint", "")

        if (lpparam.packageName == "android" || lpparam.packageName.contains("systemui")) return

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
    }
}
