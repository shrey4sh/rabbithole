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

// ---- Neutral dark surfaces (background stays near-black on all devices) ----
val Bg = Color(0xFF08090D)
val Surface1 = Color(0xFF111318)
val Surface2 = Color(0xFF171920)
val TextPrimary = Color(0xFFF0F4FA)
val TextSecondary = Color(0xFFB8BECC)

/**
 * Fallback schemes for pre-Android-12 devices: neutral slate/blue Material 3,
 * deliberately NOT purple. On Android 12+ dynamicDark/LightColorScheme wins.
 */
private val FallbackDark = darkColorScheme(
    primary = Color(0xFF9CB8E8),          // desaturated blue
    onPrimary = Color(0xFF0E1A2E),
    primaryContainer = Color(0xFF27436B),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF8FB3C7),        // blue-gray
    secondaryContainer = Color(0xFF2A4854),
    tertiary = Color(0xFFA8C0A8),         // sage
    background = Bg,
    surface = Surface1,
    surfaceVariant = Surface2,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Color(0xFF6E7484),
    outlineVariant = Color(0xFF23252E),
    error = Color(0xFFFF6B7A),
)

private val FallbackLight = lightColorScheme(
    primary = Color(0xFF3D5A91),          // slate blue
    secondary = Color(0xFF41627A),
    tertiary = Color(0xFF50694F),
)

/** App theme mode persisted from Settings > Appearance. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Material You: accent colors derive from the user's wallpaper on Android 12+.
 * Backgrounds stay neutral/near-black; dynamic color drives interactive elements only.
 */
@Composable
fun RabbitHoleTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) FallbackDark else FallbackLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * Node type → category tone. Distinct hues so types stay distinguishable, drawn from
 * Material-tonal-style desaturated families (no neon, no purple brand).
 */
fun nodeColor(type: String): Color = when (type.uppercase()) {
    "PERSON" -> Color(0xFF8FB8E8)      // sky blue
    "PLACE" -> Color(0xFF6FD0C7)       // teal
    "EVENT" -> Color(0xFFF0A868)       // amber
    "TECHNOLOGY", "TECH" -> Color(0xFF7FD89A) // green
    "GAME" -> Color(0xFFE88BA8)        // rose
    "MOVIE" -> Color(0xFFE8A0B8)       // soft pink
    "MUSIC" -> Color(0xFF9AD0B0)       // mint
    "ORGANIZATION", "ORG" -> Color(0xFF88A8E0) // periwinkle blue
    "BOOK" -> Color(0xFFE0C078)        // sand
    else -> Color(0xFF90B0D8)          // CONCEPT: muted steel blue
}
