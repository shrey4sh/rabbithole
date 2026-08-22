package com.shrey4sh.rabbithole.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- RabbitHole palette (dark-first, per spec) ----
val Bg = Color(0xFF08090D)
val Surface1 = Color(0xFF111318)
val Surface2 = Color(0xFF171920)
val Violet = Color(0xFF8B7CFF)
val Indigo = Color(0xFF6C5CE7)
val Cyan = Color(0xFF4FD1E8)
val TextPrimary = Color(0xFFF2F3F7)
val TextSecondary = Color(0xFF9AA0AE)
val Line = Color(0xFF1E222D)

// node-type accents (subtle, restrained)
val NodeColors = mapOf(
    "PERSON" to Color(0xFF8B7CFF),
    "PLACE" to Color(0xFF4FD1E8),
    "EVENT" to Color(0xFFF2A65A),
    "TECHNOLOGY" to Color(0xFF6EE7B7),
    "BOOK" to Color(0xFFF2D06B),
    "MOVIE" to Color(0xFFFF8FA3),
    "GAME" to Color(0xFF9D8DFF),
    "MUSIC" to Color(0xFF7FD1AE),
    "ORGANIZATION" to Color(0xFF7FB3F5),
    "CONCEPT" to Color(0xFFC0A9FF),
)

fun nodeColor(type: String): Color = NodeColors[type.uppercase()] ?: Violet

private val DarkScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Line,
    outlineVariant = Line,
    error = Color(0xFFF27E7E),
)

@Composable
fun RabbitHoleTheme(content: @Composable () -> Unit) {
    // dark-first: always dark (light theme comes in Settings later)
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
