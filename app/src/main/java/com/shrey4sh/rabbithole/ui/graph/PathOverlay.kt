package com.shrey4sh.rabbithole.ui.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PathEntry(val nodeId: String, val title: String, val via: String?)

/**
 * "How did I get here?" — vertical timeline of the exploration path.
 * Tap any previous node to jump back to it.
 */
@Composable
fun PathOverlay(path: List<PathEntry>, onJumpBack: (String) -> Unit, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(16.dp)
        .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)).clickable { onDismiss() }
        .padding(22.dp)) {

        // dim whole overlay tap = dismiss; inner column stops propagation by consuming clicks
        Column {
            Text("HOW DID I GET HERE?", style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))

            path.forEachIndexed { i, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // timeline rail
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(11.dp).clip(CircleShape)
                            .background(if (i == path.lastIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline))
                        if (i < path.lastIndex) {
                            Box(Modifier.size(width = 2.dp, height = 26.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                        }
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f).clickable(enabled = i < path.lastIndex) {
                        onJumpBack(entry.nodeId)
                    }) {
                        Text(entry.title, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (i == path.lastIndex)
                                androidx.compose.ui.text.font.FontWeight.Bold
                            else androidx.compose.ui.text.font.FontWeight.Normal,
                            color = if (i < path.lastIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface)
                        entry.via?.let {
                            Text("via $it", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (i < path.lastIndex) {
                        Text("tap to jump back", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("Tap a previous node to jump back.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline)
        }
    }
}
