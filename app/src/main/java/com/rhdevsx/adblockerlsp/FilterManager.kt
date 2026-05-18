package com.rhdevsx.adblockerlsp

import android.content.Context
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object FilterManager {
    
    fun downloadAndCompileFilters(context: Context, callback: (Boolean, String) -> Unit) {
        Thread {
            val urls = listOf(
                "https://easylist.to/easylist/easylist.txt",
                "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                "https://raw.githubusercontent.com/ABPindo/indonesianadblockrules/master/subscriptions/abpindo.txt"
            )

            val compiledHosts = HashSet<String>()
            val abpPattern = Pattern.compile("^\\|\\|([a-zA-Z0-9.-]+)\\^?$")

            try {
                for (urlString in urls) {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    if (connection.responseCode == 200) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val currentLine = line!!.trim()
                            
                            if (currentLine.startsWith("||")) {
                                val matcher = abpPattern.matcher(currentLine)
                                if (matcher.find()) {
                                    matcher.group(1)?.let { compiledHosts.add(it) }
                                }
                            } else if (!currentLine.startsWith("!") && !currentLine.startsWith("[") && !currentLine.contains("#") && currentLine.contains(".")) {
                                if (!currentLine.contains("/") && !currentLine.contains("=")) {
                                    compiledHosts.add(currentLine)
                                }
                            }
                        }
                        reader.close()
                    }
                    connection.disconnect()
                }

                val filterFile = File(context.applicationInfo.dataDir, "shared_prefs/compiled_hosts.txt")
                if (!filterFile.parentFile.exists()) {
                    filterFile.parentFile.mkdirs()
                }
                
                filterFile.writeText(compiledHosts.joinToString("\n"))
                
                filterFile.setReadable(true, false)
                filterFile.parentFile.setExecutable(true, false)
                filterFile.parentFile.setReadable(true, false)
                File(context.applicationInfo.dataDir).setExecutable(true, false)
                File(context.applicationInfo.dataDir).setReadable(true, false)

                callback(true, "Berhasil mengkompilasi ${compiledHosts.size} domain iklan.")
            } catch (e: Exception) {
                callback(false, "Gagal: ${e.message}")
            }
        }.start()
    }
}
