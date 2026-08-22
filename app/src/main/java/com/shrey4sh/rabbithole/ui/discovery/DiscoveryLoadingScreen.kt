package com.shrey4sh.rabbithole.ui.discovery

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
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

private val STAGES = listOf(
    "Finding the main topic…",
    "Discovering related concepts…",
    "Connecting people and places…",
    "Finding unexpected connections…",
)

@Composable
fun DiscoveryLoadingScreen(topic: String) {
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        STAGES.indices.forEach {
            stage = it
            kotlinx.coroutines.delay(700)
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = tween(900), label = "a")
    // loop the pulse
    val a2 by androidx.compose.animation.core.rememberInfiniteTransition(label = "p2").animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = tween(900, delayMillis = 900), label = "a2")

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(40.dp)) {
            Text("🐇", style = MaterialTheme.typography.displayLarge.copy(
                textAlign = TextAlign.Center), modifier = Modifier.alpha(if (stage % 2 == 0) alpha else a2))
            Text("BUILDING YOUR RABBIT HOLE",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.androidx.compose.ui.unit.sp),
                color = MaterialTheme.colorScheme.primary)
            Text(STAGES[stage], style = MaterialTheme.typography.bodyLarge, color = TextSecondary,
                textAlign = TextAlign.Center)
            Text("“$topic”", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
    }
}
