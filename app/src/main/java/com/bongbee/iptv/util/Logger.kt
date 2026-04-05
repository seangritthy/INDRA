package com.bongbee.iptv.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

object Logger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun i(message: String) {
        val time = dateFormat.format(Date())
        val formatted = "[$time] INFO: $message"
        addLog(formatted)
        android.util.Log.i("DebianPro", message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val formatted = "[$time] ERROR: $message ${throwable?.message ?: ""}"
        addLog(formatted)
        android.util.Log.e("DebianPro", message, throwable)
    }

    private fun addLog(line: String) {
        val current = _logs.value.toMutableList()
        current.add(line)
        if (current.size > 500) current.removeAt(0)
        _logs.value = current
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
