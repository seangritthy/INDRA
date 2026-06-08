package com.bongbee.iptv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Premium Futuristic Palette - DARK
val Background = Color(0xFF0B1020)
val Surface = Color(0xFF141B2D)
val ElevatedSurface = Color(0xFF1A2238)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val BorderColor = Color(0xFF2A344B)

// Premium Palette - LIGHT
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val ElevatedSurfaceLight = Color(0xFFF1F5F9)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val BorderColorLight = Color(0xFFE2E8F0)

// Accents (Consistent across themes)
val PrimaryPurple = Color(0xFF8B5CF6)
val PrimaryBlue = Color(0xFF3B82F6)
val AccentPink = Color(0xFFEC4899)
val AccentCyan = Color(0xFF22D3EE)

// Fire Palette
val FireRed = Color(0xFFFF4D00)
val FireOrange = Color(0xFFFF9500)
val FireYellow = Color(0xFFFFD500)

// Status Colors
val SuccessGreen = Color(0xFF22C55E)
val WarningOrange = Color(0xFFF59E0B)

// Gradients
val PremiumGradient = Brush.linearGradient(
    colors = listOf(PrimaryPurple, PrimaryBlue)
)

val NeonCyanGradient = Brush.linearGradient(
    colors = listOf(AccentCyan, PrimaryBlue)
)

val FireGradient = Brush.verticalGradient(
    colors = listOf(FireYellow, FireOrange, FireRed)
)
