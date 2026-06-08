package com.bongbee.iptv.runtime

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PRootManager {
    private const val TAG = "PRootManager"

    suspend fun ensureProotInstalled(context: Context): File = withContext(Dispatchers.IO) {
        val binDir = Config.getBinDir(context)
        if (!binDir.exists()) binDir.mkdirs()
        val prootFile = Config.getProotBinary(context)

        if (prootFile.exists() && prootFile.canExecute()) return@withContext prootFile

        // Asset name matches what's downloaded in build.gradle.kts
        val assetName = "proot-static-aarch64"
        try {
            context.assets.open("bin/$assetName").use { input ->
                FileOutputStream(prootFile).use { output -> input.copyTo(output) }
            }
            prootFile.setExecutable(true, false)
            // Extra insurance for permissions
            try {
                Runtime.getRuntime().exec("chmod 755 ${prootFile.absolutePath}").waitFor()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to chmod binary, relying on setExecutable", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract proot binary: ${e.message}")
            throw Exception("[System] FATAL: Bundled proot binary missing from APK assets (bin/$assetName)")
        }
        prootFile
    }
}
