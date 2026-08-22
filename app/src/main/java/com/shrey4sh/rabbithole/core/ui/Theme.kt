package com.shrey4sh.rabbithole.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ---- RabbitHole palette (dark-first, per spec) ----
val Bg = Color(0xFF08090D)
val Surface1 = Color(0xFF111318)
val Surface2 = Color(0xFF171920)
val Accent = Color(0xFF8B7CFF)   // electric violet/indigo
val Accent2 = Color(0xFF53D8F0)  // cyan
val TextPrimary = Color(0xFFF0F4FA)
val TextSecondary = Color(0xFFB8BECC)

private val RabbitHoleDark = darkColorScheme(
    primary = Accent,
    secondary = Accent2,
    background = Bg,
    surface = Surface1,
    surfaceVariant = Surface2,
    onPrimary = Color(0xFF0A0B12),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Color(0xFF6E7484),
    error = Color(0xFFFF6B7A),
)

private val RabbitHoleLight = lightColorScheme(
    primary = Color(0xFF5B4BC4),
    secondary = Color(0xFF00697A),
)

/** Node type → accent color (used by graph + legend + sheets). */
fun nodeColor(type: String): Color = when (type.uppercase()) {
    "PERSON" -> Color(0xFFC792EA)
    "PLACE" -> Color(0xFF53D8F0)
    "EVENT" -> Color(0xFFF5A97F)
    "TECHNOLOGY", "TECH" -> Color(0xFF7FE3A4)
    "GAME" -> Color(0xFF89AAFF)
    "MOVIE" -> Color(0xFFF28CA8)
    "MUSIC" -> Color(0xFF8FD6BD)
    "ORGANIZATION", "ORG" -> Color(0xFF74A8F5)
    "BOOK" -> Color(0xFFE5C07B)
    else -> Color(0xFFA99BF5) // CONCEPT
}

/**
 * Material You: on Android 12+ the dark scheme derives from the wallpaper
 * unless overridden. Falls back to the RabbitHole palette below API 31.
 */
@Composable
fun RabbitHoleTheme(
    dynamic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> RabbitHoleDark
        else -> RabbitHoleLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
