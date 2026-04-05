package com.bongbee.iptv.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bongbee.iptv.BuildConfig
import com.bongbee.iptv.R
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.util.UpdateManager
import com.bongbee.iptv.viewmodel.IptvViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: IptvViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.settings), 
                        style = MaterialTheme.typography.headlineSmall, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close), tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Premium background glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentCyan.copy(alpha = 0.05f), Color.Transparent),
                            radius = 1000f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Personalization Section
                PremiumSettingsSection(
                    title = stringResource(R.string.personalization), 
                    subtitle = stringResource(R.string.theme_desc),
                    icon = Icons.Default.AutoAwesome
                ) {
                    PremiumToggleItem(
                        title = stringResource(R.string.dark_mode),
                        subtitle = stringResource(R.string.dark_mode_desc),
                        icon = if (viewModel.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        checked = viewModel.isDarkMode,
                        onCheckedChange = { viewModel.toggleTheme() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // General Section
                PremiumSettingsSection(
                    title = stringResource(R.string.general), 
                    subtitle = stringResource(R.string.language_desc),
                    icon = Icons.Default.SettingsSuggest
                ) {
                    PremiumClickItem(
                        title = stringResource(R.string.app_language),
                        subtitle = if (viewModel.appLanguage == "km") "Khmer (ភាសាខ្មែរ)" else "English",
                        icon = Icons.Default.Translate,
                        onClick = {
                            val nextLang = if (viewModel.appLanguage == "en") "km" else "en"
                            viewModel.setLanguage(nextLang)
                        }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.05f))
                    
                    PremiumToggleItem(
                        title = stringResource(R.string.auto_next),
                        subtitle = stringResource(R.string.auto_next_desc),
                        icon = Icons.Default.FastForward,
                        checked = viewModel.autoNext,
                        onCheckedChange = { viewModel.toggleAutoNext() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Updates Section
                PremiumSettingsSection(
                    title = stringResource(R.string.updates), 
                    subtitle = stringResource(R.string.check_updates_desc),
                    icon = Icons.Default.SystemUpdate
                ) {
                    PremiumClickItem(
                        title = stringResource(R.string.check_updates),
                        subtitle = stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
                        icon = Icons.Default.NewReleases,
                        onClick = {
                            if (!isCheckingUpdate) {
                                scope.launch {
                                    isCheckingUpdate = true
                                    UpdateManager.checkUpdate(context, force = true)
                                    isCheckingUpdate = false
                                }
                            }
                        }
                    )
                    if (isCheckingUpdate) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = AccentCyan,
                            trackColor = Color.Transparent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support Section
                PremiumDonationCard()

                Spacer(modifier = Modifier.height(48.dp))
                
                // Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "INDRA IPTV PREMIUM v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.community_love),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun PremiumSettingsSection(
    title: String, 
    subtitle: String,
    icon: ImageVector, 
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Surface(
                color = AccentCyan.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = AccentCyan, 
                    modifier = Modifier.padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = AccentCyan,
                    letterSpacing = 2.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
        Surface(
            color = ElevatedSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun PremiumToggleItem(
    title: String, 
    subtitle: String, 
    icon: ImageVector,
    checked: Boolean, 
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = if (checked) AccentCyan else TextSecondary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = AccentCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedSurface,
                uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun PremiumClickItem(
    title: String, 
    subtitle: String, 
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumDonationCard() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ElevatedSurface,
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(AccentCyan.copy(alpha = 0.2f), Color.Transparent)))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(AccentCyan.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.donation_title).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            val qrContent = "00020101021129450016abaakhppxxx@abaa01090064477360208ABA Bank40600006abaP2P011211A0B0680A3E020900644773603095002247920404Dual5204000053031165802KH5911RITHY SEANG6010Phnom Penh63048250"
            val qrBitmap = remember(qrContent) { generateQrCode(qrContent, 400) }
            
            if (qrBitmap != null) {
                val imageBitmap = remember(qrBitmap) { qrBitmap.asImageBitmap() }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(8.dp)
                        .shadow(12.dp, RoundedCornerShape(20.dp))
                        .combinedClickable(
                            onClick = {
                                try {
                                    val abaUri = Uri.parse("abamobile://")
                                    val intent = Intent(Intent.ACTION_VIEW, abaUri)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.aba.mobile.android"))
                                        context.startActivity(playStoreIntent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, "ABA Bank app not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onLongClick = {
                                shareBitmap(context, qrBitmap)
                            }
                        )
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(160.dp)
                            .padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = stringResource(R.string.aba_bank),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AccentCyan
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                stringResource(R.string.donation_desc),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

private fun shareBitmap(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "donation_qr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share QR: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun generateQrCode(text: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
