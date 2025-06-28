package com.example.dowithtime.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern color palette
val PrimaryBlue = Color(0xFF6366F1)
val PrimaryBlueDark = Color(0xFF4F46E5)
val SecondaryPurple = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFEC4899)
val AccentOrange = Color(0xFFF97316)
val AccentGreen = Color(0xFF10B981)

// Background colors
val BackgroundLight = Color(0xFFFAFAFA)
val BackgroundDark = Color(0xFF0F0F23)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF1A1A2E)

// Text colors
val TextPrimaryLight = Color(0xFF1F2937)
val TextSecondaryLight = Color(0xFF6B7280)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFFD1D5DB)

// Status colors
val SuccessGreen = Color(0xFF10B981)
val WarningYellow = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)

// Gradients
val PrimaryGradient = Brush.linearGradient(
    colors = listOf(PrimaryBlue, SecondaryPurple)
)

val AccentGradient = Brush.linearGradient(
    colors = listOf(AccentPink, AccentOrange)
)

val SuccessGradient = Brush.linearGradient(
    colors = listOf(SuccessGreen, Color(0xFF059669))
)

val TimerGradient = Brush.linearGradient(
    colors = listOf(PrimaryBlue, AccentPink)
)

// Legacy colors for compatibility
val Purple80 = PrimaryBlue
val PurpleGrey80 = SecondaryPurple
val Pink80 = AccentPink

val Purple40 = PrimaryBlueDark
val PurpleGrey40 = Color(0xFF7C3AED)
val Pink40 = Color(0xFFBE185D)