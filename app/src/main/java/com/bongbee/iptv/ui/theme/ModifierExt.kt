package com.bongbee.iptv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.bongbee.iptv.viewmodel.IptvViewModel

fun Modifier.appBackground(viewModel: IptvViewModel): Modifier = composed {
    val colors = when (viewModel.backgroundSkin) {
        "Gradient" -> listOf(Background, Color(0xFF1E293B))
        "Modern" -> listOf(Color(0xFF0F172A), Color(0xFF020617))
        "Space" -> listOf(Color(0xFF020617), Color(0xFF1E1B4B))
        "Cyber" -> listOf(Color(0xFF0B0F1A), Color(0xFF1A1B26))
        else -> listOf(Background, Color(0xFF0F172A))
    }
    this.background(Brush.verticalGradient(colors))
}

fun Modifier.glassMorphism(): Modifier = composed {
    this.background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.01f)
            )
        )
    )
}

fun Modifier.neonBorder(color: Color): Modifier = composed {
    this.background(
        Brush.linearGradient(
            colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
        )
    )
}
