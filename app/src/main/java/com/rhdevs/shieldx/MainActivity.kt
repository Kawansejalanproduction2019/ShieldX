package com.rhdevs.shieldx

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

// Model untuk Item Navigasi
sealed class NavItem(var title: String, var icon: ImageVector, var route: String) {
    object Device : NavItem("Device", Icons.Default.PhoneAndroid, "device")
    object Location : NavItem("Location", Icons.Default.Map, "location")
    object Security : NavItem("Security", Icons.Default.Security, "security")
}

@Composable
fun MainAppStructure(context: Context) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
        composable(NavItem.Device.route) { DeviceSpooferScreen(context) }
        composable(NavItem.Location.route) { LocationSpooferScreen(context) }
        composable(NavItem.Security.route) { SecurityBypassScreen(context) }
    }
}

// Fungsi pembantu izin sistem file (Android 15)
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

// Komponen Umum untuk Judul Layar
@Composable
fun ScreenHeader(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

// --- LAYAR 1: DEVICE SPOOFER (Pindah dari Main sebelumnya) ---
@Composable
fun DeviceSpooferScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    
    var adBlockState by remember { mutableStateOf(prefs.getBoolean("adblock_enabled", false)) }
    var imeiState by remember { mutableStateOf(prefs.getString("spoof_imei", "") ?: "") }
    var buildState by remember { mutableStateOf(prefs.getString("spoof_build", "") ?: "") }
    var releaseState by remember { mutableStateOf(prefs.getString("spoof_release", "") ?: "") }
    var fingerprintState by remember { mutableStateOf(prefs.getString("spoof_fingerprint", "") ?: "") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader("Device Spoofer", "Manipulasi identitas sistem perangkat")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Aktifkan AdBlock", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(checked = adBlockState, onCheckedChange = { adBlockState = it })
        }
        
        OutlinedTextField(value = imeiState, onValueChange = { imeiState = it }, label = { Text("IMEI") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = buildState, onValueChange = { buildState = it }, label = { Text("Build ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = releaseState, onValueChange = { releaseState = it }, label = { Text("OS Release") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = fingerprintState, onValueChange = { fingerprintState = it }, label = { Text("OS Fingerprint") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                imeiState = "86" + (1000000000000..9999999999999).random().toString()
                releaseState = listOf("13", "14", "15").random()
                buildState = "UP1A.231005.007"
                fingerprintState = "google/husky/husky:${releaseState}/${buildState}/10817346:user/release-keys"
            }, modifier = Modifier.weight(1f)) { Text("Acak Data") }
            
            Button(onClick = {
                prefs.edit().apply {
                    putBoolean("adblock_enabled", adBlockState)
                    putString("spoof_imei", imeiState)
                    putString("spoof_build", buildState)
                    putString("spoof_release", releaseState)
                    putString("spoof_fingerprint", fingerprintState)
                    apply()
                }
                setFilePermissions(context)
            }, modifier = Modifier.weight(1f)) { Text("Simpan") }
        }
    }
}

// --- LAYAR 2: LOCATION SPOOFER (Fake GPS) ---
@Composable
fun LocationSpooferScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    
    var fakeGpsState by remember { mutableStateOf(prefs.getBoolean("fake_gps_enabled", false)) }
    var latState by remember { mutableStateOf(prefs.getFloat("fake_gps_lat", -6.1754f).toString()) }
    var lonState by remember { mutableStateOf(prefs.getFloat("fake_gps_lon", 106.8272f).toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        ScreenHeader("Location Spoofer", "Manipulasi GPS di tingkat Framework")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Aktifkan Fake GPS", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Switch(checked = fakeGpsState, onCheckedChange = { fakeGpsState = it })
        }

        OutlinedTextField(
            value = latState, 
            onValueChange = { latState = it }, 
            label = { Text("Latitude") }, 
            modifier = Modifier.fillMaxWidth(), 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = lonState, 
            onValueChange = { lonState = it }, 
            label = { Text("Longitude") }, 
            modifier = Modifier.fillMaxWidth(), 
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                latState = "-6.1754" // Monas
                lonState = "106.8272"
            }, modifier = Modifier.weight(1f)) { Text("Preset Jakarta") }

            Button(onClick = {
                prefs.edit().apply {
                    putBoolean("fake_gps_enabled", fakeGpsState)
                    putFloat("fake_gps_lat", latState.toFloatOrNull() ?: -6.1754f)
                    putFloat("fake_gps_lon", lonState.toFloatOrNull() ?: 106.8272f)
                    apply()
                }
                setFilePermissions(context)
            }, modifier = Modifier.weight(1f)) { Text("Simpan Lokasi") }
        }
    }
}

// --- LAYAR 3: SECURITY BYPASS (Bypass Flag Secure) ---
@Composable
fun SecurityBypassScreen(context: Context) {
    val prefs = context.getSharedPreferences("shield_config", Context.MODE_PRIVATE)
    
    var bypassFlagSecureState by remember { mutableStateOf(prefs.getBoolean("bypass_flag_secure", false)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Security Bypass", "Hancurkan batasan keamanan aplikasi")

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Bypass FLAG_SECURE", fontWeight = FontWeight.SemiBold)
                    Text(text = "Izinkan screenshot/rekam layar di aplikasi m-banking/secure", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = bypassFlagSecureState, onCheckedChange = { newState ->
                    bypassFlagSecureState = newState
                    prefs.edit().putBoolean("bypass_flag_secure", newState).apply()
                    setFilePermissions(context)
                })
            }
        }
    }
}
