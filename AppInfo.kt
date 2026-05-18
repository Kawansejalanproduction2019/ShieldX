package com.rhdevsx.adblockerlsp

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    var isEnabled: Boolean,
    var scheme1: Boolean,
    var scheme2: Boolean,
    var scheme3: Boolean
)
