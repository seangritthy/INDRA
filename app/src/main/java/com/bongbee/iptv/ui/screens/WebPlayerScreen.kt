package com.bongbee.iptv.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.util.AdBlocker
import com.bongbee.iptv.util.findActivity
import com.bongbee.iptv.viewmodel.IptvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlayerScreen(
    url: String,
    title: String,
    viewModel: IptvViewModel? = null,
    onClose: () -> Unit,
    isAiService: Boolean = false
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    
    val baseUri = remember(url) { Uri.parse(url) }
    val baseHost = remember(baseUri) { baseUri.host?.lowercase() ?: "" }

    // Modern Chrome User Agent to bypass bot detection
    val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36"

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, url) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                webView?.onResume()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                webView?.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    SideEffect {
        if (fullScreenView != null) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (fullScreenView != null) {
            fullScreenCallback?.onCustomViewHidden()
        } else if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            if (fullScreenView == null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (webView?.url != null) {
                                Text(
                                    Uri.parse(webView?.url).host ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                        }
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: url))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ElevatedSurface,
                        titleContentColor = TextPrimary
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (fullScreenView == null) padding else PaddingValues(0.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = desktopUA
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false
                                
                                // Block ads/trackers
                                if (AdBlocker.isAd(requestUrl)) return true
                                
                                return false
                            }

                            @SuppressLint("WebViewClientOnReceivedSslError")
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                handler?.proceed() // Ignore SSL errors for streaming sites
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                fullScreenView = view
                                fullScreenCallback = callback
                            }

                            override fun onHideCustomView() {
                                fullScreenView = null
                                fullScreenCallback = null
                            }
                        }

                        loadUrl(url)
                        webView = this
                    }
                },
                update = {
                    webView = it
                },
                modifier = Modifier.fillMaxSize()
            )

            if (fullScreenView != null) {
                AndroidView(
                    factory = { fullScreenView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading && fullScreenView == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = AccentCyan,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}
