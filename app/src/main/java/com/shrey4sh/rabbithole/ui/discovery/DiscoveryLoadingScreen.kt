package com.shrey4sh.rabbithole.ui.discovery

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shrey4sh.rabbithole.core.ui.TextSecondary
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp

private val STAGES = listOf(
    "Finding the main topic…",
    "Discovering related concepts…",
    "Connecting people and places…",
    "Finding unexpected connections…",
)

@Composable
fun DiscoveryLoadingScreen(topic: String) {
    var stage by remember { mutableIntStateOf(0) }
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")

    LaunchedEffect(Unit) {
        STAGES.indices.forEach {
            stage = it
            kotlinx.coroutines.delay(450)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(40.dp)) {
            Text("🐇", fontSize = androidx.compose.ui.unit.TextUnit.Unspecified.takeIf { false }
                ?: MaterialTheme.typography.displayLarge.fontSize,
                modifier = Modifier.alpha(pulse))
            Text("BUILDING YOUR RABBIT HOLE",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
                color = MaterialTheme.colorScheme.primary)
            Text(STAGES[stage], style = MaterialTheme.typography.bodyLarge, color = TextSecondary,
                textAlign = TextAlign.Center)
            Text("“$topic”", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
    }
}
