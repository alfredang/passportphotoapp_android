package com.tertiaryinfotech.passportphoto.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Central design tokens so the app feels consistent (mirrors the iOS Theme). */
object Brand {
    val Accent = Color(0xFF007AFF)          // system-blue-ish brand
    val Background = Color(0xFFF2F2F7)      // grouped background
    val Surface = Color(0xFFFFFFFF)
    val PrimaryText = Color(0xFF1C1C1E)
    val SecondaryText = Color(0xFF6C6C70)

    val CardShape = RoundedCornerShape(16.dp)
    val ButtonShape = RoundedCornerShape(14.dp)
}

private val LightColors = lightColorScheme(
    primary = Brand.Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF00325C),
    secondary = Brand.Accent,
    background = Brand.Background,
    onBackground = Brand.PrimaryText,
    surface = Brand.Surface,
    onSurface = Brand.PrimaryText,
    surfaceVariant = Color(0xFFEFEFF4),
    onSurfaceVariant = Brand.SecondaryText,
)

@Composable
fun PassportPhotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
