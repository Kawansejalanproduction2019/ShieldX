package com.rhdevsx.adblockerlsp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.io.File

class ConfigManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    @SuppressLint("SetWorldReadable")
    fun makeWorldReadable() {
        val prefFile = File(context.applicationInfo.dataDir, "shared_prefs/config.xml")
        val dataDir = File(context.applicationInfo.dataDir)
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        
        if (dataDir.exists()) {
            dataDir.setExecutable(true, false)
            dataDir.setReadable(true, false)
        }
        if (prefsDir.exists()) {
            prefsDir.setExecutable(true, false)
            prefsDir.setReadable(true, false)
        }
        if (prefFile.exists()) {
            prefFile.setReadable(true, false)
        }
    }

    fun setGlobalMode(mode: String) {
        prefs.edit().putString("global_mode", mode).apply()
        makeWorldReadable()
    }

    fun getGlobalMode(): String {
        return prefs.getString("global_mode", "blacklist") ?: "blacklist"
    }

    fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        prefs.edit().putBoolean(packageName, isEnabled).apply()
        makeWorldReadable()
    }

    fun isAppEnabled(packageName: String): Boolean {
        return prefs.getBoolean(packageName, false)
    }

    fun setSchemeConfig(packageName: String, scheme1: Boolean, scheme2: Boolean, scheme3: Boolean) {
        prefs.edit()
            .putBoolean("${packageName}_scheme1", scheme1)
            .putBoolean("${packageName}_scheme2", scheme2)
            .putBoolean("${packageName}_scheme3", scheme3)
            .apply()
        makeWorldReadable()
    }

    fun getScheme1(packageName: String): Boolean {
        return prefs.getBoolean("${packageName}_scheme1", true)
    }

    fun getScheme2(packageName: String): Boolean {
        return prefs.getBoolean("${packageName}_scheme2", false)
    }

    fun getScheme3(packageName: String): Boolean {
        return prefs.getBoolean("${packageName}_scheme3", false)
    }
}
