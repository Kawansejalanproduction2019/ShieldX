package com.rhdevsx.adblockerlsp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnManageApps).setOnClickListener {
            startActivity(Intent(this, ManageAppsActivity::class.java))
        }

        findViewById<Button>(R.id.btnManageTemplates).setOnClickListener {
            Toast.makeText(this, "Fitur Template dalam pengembangan", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Mengunduh dan kompilasi filter URL...", Toast.LENGTH_LONG).show()
            FilterManager.downloadAndCompileFilters(this) { success, message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
