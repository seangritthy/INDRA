package com.bongbee.iptv.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bongbee.iptv.runtime.DebianRuntime
import com.bongbee.iptv.ui.theme.*
import java.io.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val debianRuntime = remember { DebianRuntime(context) }
    val installStatus by debianRuntime.installStatus.collectAsState()
    val logs by debianRuntime.logs.collectAsState()
    
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    
    val termInterface = remember {
        TerminalInterface(context, debianRuntime) { output ->
            webViewInstance?.post {
                try {
                    val base64 = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
                    webViewInstance?.evaluateJavascript("if(window.writeToTerm) { window.writeToTerm('$base64'); }", null)
                } catch (e: Exception) {
                    Log.e("TerminalScreen", "JS write failed", e)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!debianRuntime.isInstalled()) {
            debianRuntime.install()
        }
    }

    LaunchedEffect(installStatus) {
        if (installStatus is DebianRuntime.InstallStatus.Ready || debianRuntime.isInstalled()) {
            if (!isRunning) {
                termInterface.startShell()
                isRunning = true
            }
        }
    }

    LaunchedEffect(logs) {
        if (logs.isNotEmpty() && !isRunning) {
            val lastLog = logs.last()
            termInterface.sendOutputToView("\r\n[System] $lastLog")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            termInterface.close()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Indra Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
            },
            actions = {
                IconButton(onClick = { 
                    webViewInstance?.evaluateJavascript("if(window.term && window.term.hasSelection()) { Android.copyToClipboard(window.term.getSelection()); window.term.clearSelection(); }", null)
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selection", tint = TextPrimary)
                }
                IconButton(onClick = { 
                    isRunning = false
                    termInterface.close()
                    debianRuntime.reinstall() 
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = AccentCyan)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = ElevatedSurface)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        isFocusable = true
                        isFocusableInTouchMode = true
                        
                        addJavascriptInterface(termInterface, "Android")
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                requestFocus()
                            }
                        }

                        val html = getTerminalHtml()
                        loadDataWithBaseURL("https://localhost", html, "text/html", "UTF-8", null)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { webViewInstance = it }
            )
        }

        TerminalToolbar(webViewInstance)
    }
}

@Composable
fun TerminalToolbar(webView: WebView?) {
    Surface(
        color = ElevatedSurface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val keys = listOf("CTRL", "TAB", "ESC", "↑", "↓", "←", "→")
            keys.forEach { key ->
                Surface(
                    onClick = {
                        val code = when (key) {
                            "CTRL" -> "\\x03"
                            "TAB" -> "\\t"
                            "ESC" -> "\\x1b"
                            "↑" -> "\\x1b[A"
                            "↓" -> "\\x1b[B"
                            "←" -> "\\x1b[D"
                            "→" -> "\\x1b[C"
                            else -> ""
                        }
                        webView?.evaluateJavascript("if(window.Android) Android.sendInput('$code')", null)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(key, color = TextPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun getTerminalHtml() = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/xterm@5.3.0/css/xterm.css" />
        <script src="https://cdn.jsdelivr.net/npm/xterm@5.3.0/lib/xterm.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/xterm-addon-fit@0.8.0/lib/xterm-addon-fit.js"></script>
        <style>
            body { margin: 0; background: #000; overflow: hidden; width: 100vw; height: 100vh; }
            #terminal { width: 100vw; height: 100vh; padding: 10px; box-sizing: border-box; }
            .xterm .xterm-viewport { background-color: #000 !important; }
        </style>
    </head>
    <body>
        <div id="terminal"></div>
        <script>
            (function() {
                var term;
                var fitAddon;
                function init() {
                    if (typeof Terminal === 'undefined' || typeof FitAddon === 'undefined') {
                        setTimeout(init, 100);
                        return;
                    }
                    term = new Terminal({
                        cursorBlink: true,
                        theme: { 
                            background: '#000000', 
                            foreground: '#22D3EE', 
                            cursor: '#8B5CF6',
                            selection: 'rgba(139, 92, 246, 0.3)'
                        },
                        convertEol: true,
                        fontSize: 14,
                        fontFamily: 'monospace'
                    });
                    fitAddon = new FitAddon.FitAddon();
                    term.loadAddon(fitAddon);
                    window.term = term;
                    term.open(document.getElementById('terminal'));
                    fitAddon.fit();
                    
                    window.writeToTerm = function(b64) {
                        term.write(atob(b64));
                        term.scrollToBottom();
                    };

                    term.onData(data => {
                        if (window.Android) Android.sendInput(data);
                    });

                    Android.onTerminalReady();
                    
                    document.body.onclick = function() {
                        if (!term.hasSelection()) {
                            term.focus();
                            if (window.Android) Android.showKeyboard();
                        }
                    };
                    
                    window.onresize = () => {
                        fitAddon.fit();
                        term.scrollToBottom();
                    };
                }
                init();
            })();
        </script>
    </body>
    </html>
""".trimIndent()

class TerminalInterface(
    private val context: Context,
    private val runtime: DebianRuntime,
    private val onOutput: (String) -> Unit
) {
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var shellJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isReady = false
    private val pendingOutput = mutableListOf<String>()

    fun startShell() {
        if (!runtime.isInstalled()) return
        
        shellJob?.cancel()
        shellJob = scope.launch {
            try {
                val pb = ProcessBuilder(runtime.getLaunchCommand())
                pb.redirectErrorStream(true)
                
                val p = pb.start()
                process = p
                outputStream = p.outputStream
                
                val inputStream = p.inputStream
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (isActive) {
                    try {
                        bytesRead = withContext(Dispatchers.IO) { inputStream.read(buffer) }
                        if (bytesRead == -1) break
                        val text = String(buffer, 0, bytesRead)
                        synchronized(this@TerminalInterface) {
                            if (isReady) {
                                onOutput(text)
                            } else {
                                pendingOutput.add(text)
                            }
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                onOutput("Runtime Error: ${e.message}\n")
            }
        }
    }

    fun sendOutputToView(text: String) {
        synchronized(this) {
            if (isReady) {
                onOutput(text)
            } else {
                pendingOutput.add(text)
            }
        }
    }

    @JavascriptInterface
    fun onTerminalReady() {
        synchronized(this) {
            isReady = true
            pendingOutput.forEach { onOutput(it) }
            pendingOutput.clear()
        }
    }

    @JavascriptInterface
    fun sendInput(data: String) {
        scope.launch {
            try {
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                Log.e("TerminalInterface", "Input error", e)
            }
        }
    }

    @JavascriptInterface
    fun showKeyboard() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(null, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            Log.e("TerminalInterface", "Keyboard failed", e)
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        scope.launch(Dispatchers.Main) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Terminal Selection", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("TerminalInterface", "Copy failed", e)
            }
        }
    }

    fun close() {
        shellJob?.cancel()
        process?.destroy()
        outputStream = null
        process = null
    }
}
