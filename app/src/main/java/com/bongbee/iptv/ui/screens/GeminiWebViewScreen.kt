package com.bongbee.iptv.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bongbee.iptv.viewmodel.IptvViewModel

@Composable
fun GeminiWebViewScreen(
    viewModel: IptvViewModel,
    onClose: () -> Unit
) {
    val geminiUrl = "https://gemini.google.com"

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshWebView(geminiUrl)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { 
            lifecycleOwner.lifecycle.removeObserver(observer) 
        }
    }

    WebPlayerScreen(
        url = geminiUrl,
        title = "Gemini AI",
        viewModel = viewModel,
        onClose = { onClose() },
        isAiService = true
    )
}
