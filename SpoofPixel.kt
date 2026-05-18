package com.rhdevsx.adblockerlsp

import de.robv.android.xposed.XposedHelpers
import android.os.Build

object SpoofPixel {
    fun applySpoof() {
        XposedHelpers.setStaticObjectField(Build::class.java, "MANUFACTURER", "Google")
        XposedHelpers.setStaticObjectField(Build::class.java, "BRAND", "google")
        XposedHelpers.setStaticObjectField(Build::class.java, "MODEL", "Pixel 8 Pro")
        XposedHelpers.setStaticObjectField(Build::class.java, "DEVICE", "husky")
        XposedHelpers.setStaticObjectField(Build::class.java, "PRODUCT", "husky")
        XposedHelpers.setStaticObjectField(Build::class.java, "HARDWARE", "tensor")
    }
}
