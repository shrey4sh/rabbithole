package com.shrey4sh.rabbithole.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.shrey4sh.rabbithole.core.ui.nodeColor
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/** Simple force-directed layout computed once per graph. */
private fun computeLayout(
    nodes: List<Node>,
    edges: List<Edge>,
    width: Float,
    height: Float,
): Map<String, Offset> {
    if (nodes.isEmpty()) return emptyMap()
    val cx = width / 2; val cy = height / 2

    // root at center, others on 1-2 rings
    val root = nodes.first()
    val others = nodes.drop(1)
    val positions = mutableMapOf(root.id to Offset(cx, cy))

    val ring1 = others.take(min(8, others.size))
    val ring2 = others.drop(ring1.size)

    ring1.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / ring1.size - Math.PI / 2
        val r = min(width, height) * 0.28f
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    ring2.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / maxOf(ring2.size, 1) - Math.PI / 2 + 0.3
        val r = min(width, height) * 0.42f
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    return positions
}

private fun dist(a: Offset, b: Offset): Float = sqrt((a.x - b.x).pow2() + (a.y - b.y).pow2())
private fun Float.pow2() = this * this

/**
 * Interactive force-graph canvas.
 * Supports: pinch zoom, two/single-finger pan, tap select, drag node.
 */
@Composable
fun GraphCanvas(
    nodes: List<Node>,
    edges: List<Edge>,
    selectedId: String?,
    onNodeTap: (Node) -> Unit,
    onNodeLongPress: (Node) -> Unit,
    modifier: Modifier = Modifier,
    canvasState: GraphCanvasState = rememberGraphCanvasState(),
) {
    var size by remember { mutableStateOf(Offset(1000f, 1600f)) }
    val positions = computeLayout(nodes, edges, size.x, size.y)

    var scale by canvasState.scale
    var offset by canvasState.offset
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var nodePositions by remember(nodes) { mutableStateOf(positions) }

    val nodeRadius = 26f * scale.coerceIn(0.5f, 2.5f)

    fun nodeAt(pos: Offset): Node? {
        // convert screen pos to graph coords
        val gp = (pos - offset) / scale
        return nodes.minByOrNull { n ->
            val p = nodePositions[n.id] ?: return@minByOrNull Float.MAX_VALUE
            dist(p, gp)
        }?.takeIf { n ->
            val p = nodePositions[n.id] ?: return@takeIf false
            dist(p, gp) < nodeRadius * 1.6f
        }
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(nodes, edges) {
            detectTapGestures(
                onTap = { pos -> nodeAt(pos)?.let(onNodeTap) },
                onLongPress = { pos -> nodeAt(pos)?.let(onNodeLongPress) },
                onDoubleTap = { pos -> nodeAt(pos)?.let(onNodeTap) },
            )
        }
        .pointerInput(nodes) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val newScale = (scale * zoom).coerceIn(0.35f, 3f)
                // zoom around centroid
                offset = (offset - centroid) * (newScale / scale) + centroid
                scale = newScale
                offset += pan
            }
        }
        .pointerInput(nodes, nodePositions) {
            detectDragGestures(
                onDragStart = { pos ->
                    draggingNodeId = nodeAt(pos)?.id
                },
                onDrag = { change, _ ->
                    val id = draggingNodeId ?: return@detectDragGestures
                    change.consume()
                    val gp = (change.position - offset) / scale
                    nodePositions = nodePositions.toMutableMap().apply { put(id, gp) }
                },
                onDragEnd = { draggingNodeId = null },
            )
        }
    ) {
        size = Offset(this.size.width.toFloat(), this.size.height.toFloat())
        if (nodePositions.isEmpty()) return@Canvas
        nodePositions = nodePositions.ifEmpty { positions }

        // transform: scale + translate
        withTransform({ scale(scale, scale, pivot = Offset.Zero); translate(offset.x, offset.y) }) {

            // edges
            edges.forEach { e ->
                val a = nodePositions[e.sourceNodeId]
                val b = nodePositions[e.targetNodeId]
                if (a != null && b != null) {
                    val related = selectedId == null || e.sourceNodeId == selectedId || e.targetNodeId == selectedId
                    drawLine(
                        color = if (selectedId == null) Color(0xFF2A2F3D)
                        else if (related) Color(0xFF4A5170) else Color(0xFF171B26),
                        start = a, end = b,
                        strokeWidth = 1.5f.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // nodes
            nodes.forEach { n ->
                val p = nodePositions[n.id] ?: return@forEach
                val isSel = n.id == selectedId
                val dimmed = selectedId != null && !isSel &&
                        edges.none { (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                                (it.targetNodeId == selectedId && it.sourceNodeId == n.id) }
                val color = nodeColor(n.type.name)
                val alpha = if (dimmed) 0.25f else 1f
                val r = if (isSel) nodeRadius * 1.25f else nodeRadius

                // subtle glow for selected
                if (isSel) {
                    drawCircle(color = color.copy(alpha = 0.18f), radius = r * 1.8f, center = p)
                }
                // fill
                drawCircle(color = color.copy(alpha = 0.22f * alpha), radius = r, center = p)
                drawCircle(color = color.copy(alpha = alpha), radius = r, center = p, style = Stroke(width = 2.5f.dp.toPx()))
                // core dot
                drawCircle(color = color.copy(alpha = alpha), radius = r * 0.45f, center = p)
            }
        }
    }
}

// helpers
private operator fun Offset.div(s: Float) = Offset(x / s, y / s)
private operator fun Offset.times(s: Float) = Offset(x * s, y * s)

@Composable
fun rememberGraphCanvasState(): GraphCanvasState = remember { GraphCanvasState() }

class GraphCanvasState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() { scale = 1f; offset = Offset.Zero }
    fun zoomIn() { scale = (scale * 1.25f).coerceAtMost(3f) }
    fun zoomOut() { scale = (scale / 1.25f).coerceAtLeast(0.35f) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.withTransform(
    block: androidx.compose.ui.graphics.drawscope.DrawTransform.() -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) = androidx.compose.ui.graphics.drawscope.withTransform(block, content)

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectDragGestures(
    onDragStart: (Offset) -> Unit,
    onDrag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    androidx.compose.foundation.gestures.detectDragGestures(
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragEnd,
    )
}
