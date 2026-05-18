package com.rhdevsx.adblockerlsp

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.FileOutputStream

class ConfigManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    private val altFile = File(context.filesDir, "config_alt.txt")

    @SuppressLint("SetWorldReadable")
    fun makeWorldReadable() {
        try {
            val dataDir = File(context.applicationInfo.dataDir)
            val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            val prefFile = File(context.applicationInfo.dataDir, "shared_prefs/config.xml")
            val filesDir = File(context.applicationInfo.dataDir, "files")
            
            dataDir.setExecutable(true, false)
            dataDir.setReadable(true, false)
            prefsDir.setExecutable(true, false)
            prefsDir.setReadable(true, false)
            prefFile.setExecutable(true, false)
            prefFile.setReadable(true, false)
            filesDir.setExecutable(true, false)
            filesDir.setReadable(true, false)
            altFile.setExecutable(true, false)
            altFile.setReadable(true, false)
            
            Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod -R 777 ${dataDir.absolutePath}")).waitFor()
        } catch (e: Exception) {}
    }

    private fun syncAltFile() {
        try {
            val sb = java.lang.StringBuilder()
            prefs.all.forEach { (key, value) ->
                sb.append("$key=$value\n")
            }
            FileOutputStream(altFile).use { it.write(sb.toString().toByteArray()) }
            makeWorldReadable()
        } catch (e: Exception) {}
    }

    fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        prefs.edit().putBoolean(packageName, isEnabled).commit()
        syncAltFile()
    }

    fun isAppEnabled(packageName: String): Boolean {
        return prefs.getBoolean(packageName, false)
    }

    fun setSchemeConfig(packageName: String, scheme1: Boolean, scheme2: Boolean, scheme3: Boolean) {
        prefs.edit()
            .putBoolean("${packageName}_scheme1", scheme1)
            .putBoolean("${packageName}_scheme2", scheme2)
            .putBoolean("${packageName}_scheme3", scheme3)
            .commit()
        syncAltFile()
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
