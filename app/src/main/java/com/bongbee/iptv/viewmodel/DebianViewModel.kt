package com.bongbee.iptv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bongbee.iptv.runtime.DebianRuntime
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DebianViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime = DebianRuntime(application)
    
    val installStatus: StateFlow<DebianRuntime.InstallStatus> = runtime.installStatus
    val logs: StateFlow<List<String>> = runtime.logs

    fun isInstalled() = runtime.isInstalled()

    fun install() {
        runtime.install()
    }

    fun reinstall() {
        runtime.reinstall()
    }

    fun getLaunchCommand() = runtime.getLaunchCommand()
}
