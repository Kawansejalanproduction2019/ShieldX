package com.rhdevsx.adblockerlsp

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ManageAppsActivity : Activity() {

    private lateinit var configManager: ConfigManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var spinnerMode: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_apps)

        configManager = ConfigManager(this)
        recyclerView = findViewById(R.id.recyclerView)
        spinnerMode = findViewById(R.id.spinnerMode)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppListAdapter(emptyList(), configManager)
        recyclerView.adapter = adapter

        setupSpinner()
        loadApps()
    }

    private fun setupSpinner() {
        val modes = arrayOf("Daftar Hitam", "Daftar Putih")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        spinnerMode.adapter = spinnerAdapter

        val currentMode = configManager.getGlobalMode()
        if (currentMode == "whitelist") {
            spinnerMode.setSelection(1)
        } else {
            spinnerMode.setSelection(0)
        }

        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMode = if (position == 1) "whitelist" else "blacklist"
                configManager.setGlobalMode(selectedMode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appList = mutableListOf<AppInfo>()

            for (appInfo in packages) {
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 || appInfo.packageName == "com.google.android.gms") {
                    val packageName = appInfo.packageName
                    val name = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    
                    val isEnabled = configManager.isAppEnabled(packageName)
                    val scheme1 = configManager.getScheme1(packageName)
                    val scheme2 = configManager.getScheme2(packageName)
                    val scheme3 = configManager.getScheme3(packageName)

                    appList.add(AppInfo(name, packageName, icon, isEnabled, scheme1, scheme2, scheme3))
                }
            }

            appList.sortBy { it.name.lowercase() }

            runOnUiThread {
                adapter.updateData(appList)
            }
        }.start()
    }
}
