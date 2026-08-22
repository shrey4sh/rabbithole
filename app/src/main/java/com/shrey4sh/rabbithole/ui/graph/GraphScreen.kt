package com.shrey4sh.rabbithole.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shrey4sh.rabbithole.core.ui.Accent
import com.shrey4sh.rabbithole.core.ui.Surface1
import com.shrey4sh.rabbithole.core.ui.nodeColor
import com.shrey4sh.rabbithole.domain.model.Node
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    hole: RabbitHole,
    onBack: () -> Unit,
    onTakeMeDeeper: (String) -> Unit = {},
    path: List<PathEntry> = emptyList(),
    onJumpBack: (String) -> Unit = {},
    depth: Int = 0,
    expanding: Boolean = false,
    onShare: (RabbitHole) -> Unit = {},
) {
    val state = rememberGraphCanvasState()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showPath by remember { mutableStateOf(false) }

    val selected = hole.nodes.find { it.id == selectedId }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            GraphCanvas(
                nodes = hole.nodes,
                edges = hole.edges,
                selectedId = selectedId,
                onNodeTap = { selectedId = if (selectedId == it.id) null else it.id },
                onNodeLongPress = { selectedId = it.id },
                modifier = Modifier.fillMaxSize(),
                canvasState = state,
            )
            if (expanding) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center) {
                    Text("🐇 going deeper…", style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 60.dp)) {
                // topic is primary; depth secondary
                val rootTitle = hole.nodes.firstOrNull()?.title ?: "Rabbit Hole"
                Text(rootTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (depth > 0) {
                    Text("Depth $depth",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent, modifier = Modifier.padding(top = 2.dp))
                }
            }
            IconButton(onClick = { showSearch = !showSearch },
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surface)) {
                Icon(Icons.Default.Search, "Search in graph")
            }
        }

        // search within graph
        AnimatedVisibility(visible = showSearch, modifier = Modifier.align(Alignment.TopCenter).padding(top = 68.dp)) {
            GraphNodeSearch(hole.nodes, onSelected = {
                selectedId = it.id
                showSearch = false
            })
        }

        // right floating controls: zoom in/out, center, reset
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FloatingActionButton(onClick = { state.zoomIn() }, modifier = Modifier.size(42.dp),
                containerColor = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Default.Add, "Zoom in", tint = MaterialTheme.colorScheme.onSurface)
            }
            FloatingActionButton(onClick = { state.zoomOut() }, modifier = Modifier.size(42.dp),
                containerColor = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Default.Remove, "Zoom out", tint = MaterialTheme.colorScheme.onSurface)
            }
            FloatingActionButton(onClick = { state.reset() }, modifier = Modifier.size(42.dp),
                containerColor = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Default.CenterFocusStrong, "Reset graph", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        // legend (bottom-left, compact)
        Legend(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 20.dp))

        // how-did-i-get-here floating button (top-right under search)
        if (path.size > 1) {
            FloatingActionButton(
                onClick = { showPath = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 68.dp, end = 14.dp)
                    .size(42.dp),
                containerColor = MaterialTheme.colorScheme.surface) {
                Text("🧭", style = MaterialTheme.typography.titleMedium)
            }
        }

        // exploration path overlay
        if (showPath) {
            PathOverlay(path, onJumpBack = { id ->
                onJumpBack(id); selectedId = null; showPath = false
            }, onDismiss = { showPath = false })
        }

        // rabbit hole "keep going?" bar
        if (selected != null && !expanding) {
            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.65f)).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Keep going?", color = Color(0xFFB8BECC), fontSize = 13.sp)
                androidx.compose.material3.FilledTonalButton(onClick = {
                    onTakeMeDeeper(selected.id)
                }) { Text("🐇 TAKE ME DEEPER", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }

        // node bottom sheet
        selected?.let { sel ->
            ModalBottomSheet(
                onDismissRequest = { selectedId = null },
                containerColor = Surface1,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            ) {
                NodeSheet(sel, hole, onTakeMeDeeper, onShare)
            }
        }
    }
}

@Composable
private fun GraphNodeSearch(nodes: List<Node>, onSelected: (Node) -> Unit) {
    var q by remember { mutableStateOf("") }
    val results = nodes.filter { it.title.contains(q, true) }.take(6)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TextField(
            value = q, onValueChange = { q = it },
            placeholder = { Text("Search in graph…") },
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        results.forEach { n ->
            Row(modifier = Modifier.fillMaxWidth()
                .clickable { onSelected(n) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(nodeColor(n.type.name)))
                Text(n.title, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}



@Composable
private fun NodeSheet(node: Node, hole: RabbitHole, onTakeMeDeeper: (String) -> Unit, onShare: (RabbitHole) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        // type chip + image circle
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape)
                .background(nodeColor(node.type.name).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center) {
                Text(node.title.take(1), color = nodeColor(node.type.name),
                    style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text(node.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(typeLabel(node.type.name.lowercase()) + " · ${node.type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(14.dp))
        if (node.description.isNotEmpty()) {
            Text(node.description, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp))
            Spacer(Modifier.height(16.dp))
        }

        Text("CONNECTED TO", style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        val connected = hole.edges.filter { it.sourceNodeId == node.id || it.targetNodeId == node.id }
        val relTexts = connected.mapNotNull { e ->
            val otherId = if (e.sourceNodeId == node.id) e.targetNodeId else e.sourceNodeId
            val rel = e.relationship.lowercase().replace('_', ' ')
            hole.nodes.find { it.id == otherId }?.let { "$rel → ${it.title}" }
        }
        relTexts.forEach { t ->
            Text("· $t", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 2.dp))
        }

        Spacer(Modifier.height(20.dp))

        // actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.Button(
                onClick = { onTakeMeDeeper(node.id) }, modifier = Modifier.weight(1f)) {
                Text("🐇 TAKE ME DEEPER") }
            androidx.compose.material3.OutlinedButton(
                onClick = {}, modifier = Modifier.weight(1f)) { Text("VIEW SOURCES") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.OutlinedButton(
                onClick = {}, modifier = Modifier.weight(1f)) { Text("SAVE") }
            androidx.compose.material3.OutlinedButton(
                onClick = { onShare(hole) }, modifier = Modifier.weight(1f)) { Text("SHARE") }
        }
    }
}

private fun typeLabel(s: String) = when (s) {
    "person" -> "Person"
    "place" -> "Place"
    "event" -> "Event"
    "technology" -> "Technology"
    else -> s.replaceFirstChar { it.uppercase() }
}

@Composable
fun Legend(modifier: Modifier = Modifier) {
    val types = listOf("PERSON" to "Person", "PLACE" to "Place", "EVENT" to "Event",
        "TECHNOLOGY" to "Tech", "GAME" to "Game", "MOVIE" to "Movie",
        "MUSIC" to "Music", "ORGANIZATION" to "Org", "CONCEPT" to "Concept")
    Column(modifier.padding(10.dp).clip(RoundedCornerShape(10.dp))
        .background(Color.Black.copy(alpha = 0.55f)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        types.forEach { (t, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(nodeColor(t)))
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFB8BECC))
            }
        }
    }
}
