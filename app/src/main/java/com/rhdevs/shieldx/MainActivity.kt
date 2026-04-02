package com.rhdevs.shieldx

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainAppStructure(this)
            }
        }
    }
}

sealed class NavItem(var title: String, var icon: ImageVector, var route: String) {
    object Device : NavItem("Identitas", Icons.Default.Fingerprint, "device")
    object Location : NavItem("Lokasi", Icons.Default.Place, "location")
    object Security : NavItem("Fitur", Icons.Default.SettingsSuggest, "security")
}

@Composable
fun MainAppStructure(context: Context) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MainNavigationHost(navController = navController, context = context)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(NavItem.Device, NavItem.Location, NavItem.Security)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) { saveState = true }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun MainNavigationHost(navController: NavHostController, context: Context) {
    NavHost(navController, startDestination = NavItem.Device.route) {
        composable(NavItem.Device.route) { DeviceScreen(context) }
        composable(NavItem.Location.route) { LocationScreen(context) }
        composable(NavItem.Security.route) { SecurityScreen(context) }
    }
}

fun setFilePermissions(context: Context) {
    try {
        val prefName = "shield_config"
        val dataDir = context.applicationInfo.dataDir
        val prefsDir = File(dataDir, "shared_prefs")
        val prefsFile = File(prefsDir, "$prefName.xml")
        
        if (prefsDir.exists()) {
            prefsDir.setExecutable(true, false)
            prefsDir.setReadable(true, false)
        }
        
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false)
        }
    } catch (e: Exception) {}
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun DeviceScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    
    var imei by remember { mutableStateOf(prefs.getString("spoof_imei", "") ?: "") }
    var buildId by remember { mutableStateOf(prefs.getString("spoof_build", "") ?: "") }
    var release by remember { mutableStateOf(prefs.getString("spoof_release", "") ?: "") }
    var fingerprint by remember { mutableStateOf(prefs.getString("spoof_fingerprint", "") ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("Device Identity", "Manipulasi data mentah hardware")

        OutlinedTextField(value = imei, onValueChange = { imei = it }, label = { Text("IMEI") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = buildId, onValueChange = { buildId = it }, label = { Text("Build ID") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = release, onValueChange = { release = it }, label = { Text("Android Version") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = fingerprint, onValueChange = { fingerprint = it }, label = { Text("Fingerprint") }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                imei = "86" + (1000000000000..9999999999999).random().toString()
                release = listOf("13", "14", "15").random()
                buildId = "UP1A.231005.007"
                fingerprint = "google/husky/husky:${release}/${buildId}/10817346:user/release-keys"
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Generate Identitas Acak") }

        OutlinedButton(
            onClick = {
                prefs.edit().apply {
                    putString("spoof_imei", imei)
                    putString("spoof_build", buildId)
                    putString("spoof_release", release)
                    putString("spoof_fingerprint", fingerprint)
                    apply()
                }
                setFilePermissions(context)
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Simpan Konfigurasi") }
    }
}

@Composable
fun LocationScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("fake_gps_enabled", false)) }
    var lat by remember { mutableStateOf(prefs.getFloat("fake_gps_lat", -6.1754f).toString()) }
    var lon by remember { mutableStateOf(prefs.getFloat("fake_gps_lon", 106.8272f).toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        SectionTitle("Fake GPS Pro", "Kunci koordinat global")

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Aktifkan Lokasi Palsu", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Pilih Negara (Quick Preset):", style = MaterialTheme.typography.labelMedium)
        
        val presets = listOf(
            "Jakarta" to ("-6.1754" to "106.8272"),
            "Tokyo" to ("35.6895" to "139.6917"),
            "New York" to ("40.7128" to "-74.0060"),
            "London" to ("51.5074" to "-0.1278"),
            "Sydney" to ("-33.8688" to "151.2093")
        )

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            presets.forEach { (name, coords) ->
                SuggestionChip(
                    onClick = { 
                        lat = coords.first
                        lon = coords.second
                    },
                    label = { Text(name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text("Latitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = lon, onValueChange = { lon = it }, label = { Text("Longitude") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                prefs.edit().apply {
                    putBoolean("fake_gps_enabled", isEnabled)
                    putFloat("fake_gps_lat", lat.toFloatOrNull() ?: 0f)
                    putFloat("fake_gps_lon", lon.toFloatOrNull() ?: 0f)
                    apply()
                }
                setFilePermissions(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Terapkan Koordinat") }
    }
}

@Composable
fun SecurityScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    var adBlock by remember { mutableStateOf(prefs.getBoolean("adblock_enabled", false)) }
    var bypassSecure by remember { mutableStateOf(prefs.getBoolean("bypass_flag_secure", false)) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        SectionTitle("Global Security", "Bypass proteksi aplikasi")

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AdBlock Engine", fontWeight = FontWeight.Bold)
                    Text("Blokir koneksi DNS/URL iklan", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = adBlock, onCheckedChange = { 
                    adBlock = it 
                    prefs.edit().putBoolean("adblock_enabled", it).apply()
                    setFilePermissions(context)
                })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bypass Flag Secure", fontWeight = FontWeight.Bold)
                    Text("Izinkan Screenshot/Rekam Layar", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = bypassSecure, onCheckedChange = { 
                    bypassSecure = it 
                    prefs.edit().putBoolean("bypass_flag_secure", it).apply()
                    setFilePermissions(context)
                })
            }
        }
    }
}
