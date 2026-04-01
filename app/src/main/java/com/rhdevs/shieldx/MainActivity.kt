package com.rhdevs.shieldx

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(this)
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_WORLD_READABLE)
    
    var adBlockState by remember { mutableStateOf(prefs.getBoolean("adblock_enabled", false)) }
    var imeiState by remember { mutableStateOf(prefs.getString("spoof_imei", "") ?: "") }
    var buildState by remember { mutableStateOf(prefs.getString("spoof_build", "") ?: "") }
    var releaseState by remember { mutableStateOf(prefs.getString("spoof_release", "") ?: "") }
    var fingerprintState by remember { mutableStateOf(prefs.getString("spoof_fingerprint", "") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "ShieldX Control Panel", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Aktifkan Blokir Iklan", modifier = Modifier.weight(1f))
            Switch(checked = adBlockState, onCheckedChange = { adBlockState = it })
        }

        OutlinedTextField(value = imeiState, onValueChange = { imeiState = it }, label = { Text("IMEI") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = buildState, onValueChange = { buildState = it }, label = { Text("Build ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = releaseState, onValueChange = { releaseState = it }, label = { Text("OS Release") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = fingerprintState, onValueChange = { fingerprintState = it }, label = { Text("OS Fingerprint") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            imeiState = "86" + (1000000000000..9999999999999).random().toString()
            releaseState = "14"
            buildState = "UP1A.231005.007"
            fingerprintState = "google/husky/husky:14/UP1A.231005.007/10817346:user/release-keys"
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Acak Data")
        }

        Button(onClick = {
            prefs.edit().apply {
                putBoolean("adblock_enabled", adBlockState)
                putString("spoof_imei", imeiState)
                putString("spoof_build", buildState)
                putString("spoof_release", releaseState)
                putString("spoof_fingerprint", fingerprintState)
                apply()
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Simpan Konfigurasi")
        }
    }
}
