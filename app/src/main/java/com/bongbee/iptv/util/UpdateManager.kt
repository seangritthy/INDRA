package com.bongbee.iptv.util

import android.content.*
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bongbee.iptv.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/seangritthy/INDRA/releases"
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK = "last_check_time"
    private const val CHECK_INTERVAL = 6 * 60 * 60 * 1000 // 6 hours

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    data class UpdateUIState(
        val version: String,
        val url: String,
        val isMandatory: Boolean
    )
    
    private val _updateUIState = MutableStateFlow<UpdateUIState?>(null)
    val updateUIState = _updateUIState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    fun dismissUpdateDialog() {
        _updateUIState.value = null
        _isDownloading.value = false
    }

    suspend fun checkUpdate(
        context: Context, 
        force: Boolean = false, 
        onUpdateAvailable: ((isForce: Boolean, url: String, version: String) -> Unit)? = null
    ) {
        if (!force) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
            if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL) return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URL("$GITHUB_RELEASES_URL?nocache=${System.currentTimeMillis()}")
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "INDRA-App")
                conn.connectTimeout = 10000
                
                if (conn.responseCode == 200) {
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val releases = JSONArray(response)
                    if (releases.length() == 0) return@withContext

                    var downloadUrl = ""
                    var tagName = ""
                    var changelog = ""
                    var found = false

                    for (i in 0 until releases.length()) {
                        val rel = releases.getJSONObject(i)
                        val assets = rel.getJSONArray("assets")
                        for (j in 0 until assets.length()) {
                            val asset = assets.getJSONObject(j)
                            if (asset.getString("name").endsWith(".apk", true)) {
                                downloadUrl = asset.getString("browser_download_url")
                                tagName = rel.getString("tag_name")
                                changelog = rel.optString("body", "")
                                found = true; break
                            }
                        }
                        if (found) break
                    }

                    if (!found) return@withContext

                    val displayVersion = if (tagName.lowercase().startsWith("v")) tagName else "v$tagName"
                    val latestClean = tagName.replace("v", "", true).trim()
                    val currentClean = BuildConfig.VERSION_NAME.replace("v", "", ignoreCase = true).trim()

                    if (isNewerVersion(latestClean, currentClean) || changelog.contains("[TEST]", true)) {
                        val isMandatory = changelog.contains("[FORCE]", true)
                        withContext(Dispatchers.Main) {
                            _updateUIState.value = UpdateUIState(displayVersion, downloadUrl, isMandatory)
                            onUpdateAvailable?.invoke(isMandatory, downloadUrl, displayVersion)
                            // Auto-download and install immediately — no dialog needed
                            startDownload(context, downloadUrl, displayVersion)
                        }
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "Check failed", e) }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        if (latest == current) return false
        return try {
            val l = latest.split(".").map { it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L }
            val c = current.split(".").map { it.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L }
            for (i in 0 until maxOf(l.size, c.size)) {
                val lv = l.getOrNull(i) ?: 0L
                val cv = c.getOrNull(i) ?: 0L
                if (lv > cv) return true
                if (lv < cv) return false
            }
            false
        } catch (e: Exception) { true }
    }

    fun startDownload(context: Context, downloadUrl: String, version: String) {
        val fileName = "INDRA_${version.replace(".", "_")}.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        _isDownloading.value = true
        _downloadProgress.value = 0f

        managerScope.launch(Dispatchers.IO) {
            try {
                var currentUrl = downloadUrl
                var connection: HttpURLConnection
                var redirectCount = 0

                // Manually follow redirects (GitHub uses multiple)
                while (true) {
                    connection = URL(currentUrl).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.setRequestProperty("User-Agent", "INDRA-App")
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000

                    val responseCode = connection.responseCode
                    if (responseCode in listOf(301, 302, 303, 307, 308)) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (newUrl == null || redirectCount++ > 5) throw Exception("Too many redirects")
                        currentUrl = newUrl
                    } else {
                        break
                    }
                }

                if (connection.responseCode != 200) {
                    throw Exception("HTTP ${connection.responseCode}")
                }

                val fileSize = connection.contentLengthLong
                
                connection.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalDownloaded = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalDownloaded += bytesRead
                            if (fileSize > 0) {
                                _downloadProgress.value = totalDownloaded.toFloat() / fileSize.toFloat()
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    _downloadProgress.value = 1f
                    _isDownloading.value = false
                    installApk(context, destination)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isDownloading.value = false
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Install fail", e)
        }
    }
}
