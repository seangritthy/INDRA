package com.bongbee.iptv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bongbee.iptv.R
import com.bongbee.iptv.ui.theme.*
import com.bongbee.iptv.viewmodel.IptvViewModel

data class AiService(
    val name: String,
    val url: String,
    val icon: ImageVector,
    val color: Color,
    val isLocalTerminal: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiHubScreen(
    viewModel: IptvViewModel,
    onServiceClick: (String, String, Boolean) -> Unit
) {
    val aiServices = listOf(
        AiService("Indra Terminal", "terminal", Icons.Default.Terminal, AccentCyan, true),
        AiService("Gemini", "https://gemini.google.com", Icons.Default.AutoAwesome, PrimaryPurple),
        AiService("DeepSeek", "https://chat.deepseek.com", Icons.Default.Cyclone, PrimaryBlue),
        AiService("ChatGPT", "https://chatgpt.com", Icons.AutoMirrored.Filled.Chat, Color(0xFF10A37F)),
        AiService("Grok", "https://grok.com", Icons.Default.Psychology, Color.White),
        AiService("Google Search", "https://www.google.com", Icons.Default.Search, Color(0xFF4285F4))
    )

    Box(modifier = Modifier.fillMaxSize().appBackground(viewModel)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text(
                text = stringResource(R.string.ai_services).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "NEURAL HUB",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(aiServices) { service ->
                    AiServiceCard(service) {
                        onServiceClick(service.url, service.name, service.isLocalTerminal)
                    }
                }
            }
        }
    }
}

@Composable
fun AiServiceCard(service: AiService, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ElevatedSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle glow in the corner
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(service.color.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(service.color.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, service.color.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = service.icon,
                        contentDescription = service.name,
                        tint = service.color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (service.isLocalTerminal) "Local System" else "Web Service",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
