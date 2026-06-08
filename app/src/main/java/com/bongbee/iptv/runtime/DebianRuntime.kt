package com.bongbee.iptv.runtime

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*

class DebianRuntime(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _installStatus = MutableStateFlow<InstallStatus>(InstallStatus.Idle)
    val installStatus = _installStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val rootDir = Config.getDebianDir(context)
    private val rootfsDir = Config.getRootfsDir(context)
    private val binDir = Config.getBinDir(context)
    private val prootBinary = Config.getProotBinary(context)

    sealed class InstallStatus {
        object Idle : InstallStatus()
        data class Extracting(val progress: Int, val item: String) : InstallStatus()
        object Ready : InstallStatus()
        data class Error(val message: String) : InstallStatus()
    }

    private fun addLog(message: String) {
        _logs.value = _logs.value + message
        Log.d("IndraTerminal", message)
    }

    fun isInstalled(): Boolean {
        return prootBinary.exists() && prootBinary.canExecute()
    }

    fun install() {
        scope.launch {
            try {
                _logs.value = emptyList()
                addLog("Initialising Indra Terminal (Offline Mode)...")
                
                // 1. Initialize Engine from Assets
                _installStatus.value = InstallStatus.Extracting(0, "Engine")
                PRootManager.ensureProotInstalled(context)
                
                addLog("Engine successfully initialized from bundled assets.")

                // 2. Setup Filesystem
                if (!binDir.exists()) binDir.mkdirs()
                if (!rootDir.exists()) rootDir.mkdirs()
                if (!rootfsDir.exists()) rootfsDir.mkdirs()
                
                setupDns()
                
                addLog("Indra Terminal is ready.")
                _installStatus.value = InstallStatus.Ready
            } catch (e: Exception) {
                val errorMsg = "FATAL: ${e.message}"
                addLog(errorMsg)
                _installStatus.value = InstallStatus.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun setupDns() {
        try {
            val etc = File(rootfsDir, "etc")
            if (!etc.exists()) etc.mkdirs()
            val resolvConf = File(etc, "resolv.conf")
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
        } catch (e: Exception) {
            Log.e("IndraTerminal", "DNS error", e)
        }
    }

    fun getLaunchCommand(): List<String> {
        val hasBash = File(rootfsDir, "bin/bash").exists()
        
        return if (hasBash) {
            listOf(
                prootBinary.absolutePath,
                "--link2symlink",
                "-0",
                "-r", rootfsDir.absolutePath,
                "-b", "/dev",
                "-b", "/proc",
                "-b", "/sys",
                "-b", "/data",
                "-b", "/mnt",
                "-b", "/storage/emulated/0:/sdcard",
                "-w", "/root",
                "/usr/bin/env", "-i",
                "HOME=/root",
                "TERM=xterm-256color",
                "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "LANG=en_US.UTF-8",
                "/bin/bash", "--login"
            )
        } else {
            listOf(
                "/system/bin/sh",
                "-c",
                "export PATH=\$PATH:${binDir.absolutePath}; export HOME=${rootDir.absolutePath}; cd \$HOME; echo 'Indra Terminal Ready. Assets initialized.'; ${prootBinary.absolutePath} --version; /system/bin/sh -i"
            )
        }
    }

    fun reinstall() {
        rootDir.deleteRecursively()
        binDir.deleteRecursively()
        install()
    }
}
