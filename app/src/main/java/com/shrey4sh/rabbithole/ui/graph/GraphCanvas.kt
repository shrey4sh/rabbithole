package com.shrey4sh.rabbithole.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.shrey4sh.rabbithole.core.ui.nodeColor
import com.shrey4sh.rabbithole.domain.model.Edge
import com.shrey4sh.rabbithole.domain.model.Node
import kotlin.math.min

/** Simple ring layout computed once per graph. */
fun computeLayout(
    nodes: List<Node>,
    width: Float,
    height: Float,
): Map<String, Offset> {
    if (nodes.isEmpty()) return emptyMap()
    val cx = width / 2; val cy = height / 2
    val root = nodes.first()
    val others = nodes.drop(1)
    val positions = mutableMapOf(root.id to Offset(cx, cy))

    val ring1 = others.take(min(8, others.size))
    val ring2 = others.drop(ring1.size)
    ring1.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / ring1.size - Math.PI / 2
        val r = min(width, height) * 0.26f
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    ring2.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / maxOf(ring2.size, 1) - Math.PI / 2 + 0.3
        val r = min(width, height) * 0.4f
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    return positions
}

class GraphCanvasState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() { scale = 1f; offset = Offset.Zero }
    fun zoomIn() { scale = (scale * 1.25f).coerceAtMost(3f) }
    fun zoomOut() { scale = (scale / 1.25f).coerceAtLeast(0.35f) }
}

@Composable
fun rememberGraphCanvasState(): GraphCanvasState = remember { GraphCanvasState() }

/**
 * Interactive graph canvas.
 * Supports: pinch zoom, two-finger pan, single-finger pan, node drag,
 * tap to select, long-press select.
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
    var canvasSize by remember { mutableStateOf(Offset(1000f, 1600f)) }
    var draggingNodeId by remember { mutableStateOf<String?>(null) }
    var nodePositions by remember(nodes) { mutableStateOf(computeLayout(nodes, canvasSize.x, canvasSize.y)) }

    val baseRadius = 26.dp.value

    fun nodeAt(screenPos: Offset): Node? {
        val gp = Offset((screenPos.x - canvasState.offset.x) / canvasState.scale,
                        (screenPos.y - canvasState.offset.y) / canvasState.scale)
        return nodes.minByOrNull { n ->
            val p = nodePositions[n.id] ?: return@minByOrNull Float.MAX_VALUE
            val dx = p.x - gp.x; val dy = p.y - gp.y
            dx * dx + dy * dy
        }?.takeIf { n ->
            val p = nodePositions[n.id] ?: return@takeIf false
            val dx = p.x - gp.x; val dy = p.y - gp.y
            val r = baseRadius * canvasState.scale * 1.6f
            dx * dx + dy * dy < r * r
        }
    }

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(nodes) {
            detectTapGestures(
                onTap = { pos -> nodeAt(pos)?.let(onNodeTap) },
                onLongPress = { pos -> nodeAt(pos)?.let(onNodeLongPress) },
                onDoubleTap = { pos -> nodeAt(pos)?.let(onNodeTap) },
            )
        }
        .pointerInput(nodes, canvasSize) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val newScale = (canvasState.scale * zoom).coerceIn(0.35f, 3f)
                canvasState.offset = Offset(
                    (canvasState.offset.x - centroid.x) * (newScale / canvasState.scale) + centroid.x,
                    (canvasState.offset.y - centroid.y) * (newScale / canvasState.scale) + centroid.y)
                canvasState.scale = newScale
                canvasState.offset = Offset(canvasState.offset.x + pan.x, canvasState.offset.y + pan.y)
            }
        }
        .pointerInput(nodes, nodePositions) {
            detectDragGestures(
                onDragStart = { pos -> draggingNodeId = nodeAt(pos)?.id },
                onDrag = { change, _ ->
                    val id = draggingNodeId ?: return@detectDragGestures
                    change.consume()
                    val gp = Offset((change.position.x - canvasState.offset.x) / canvasState.scale,
                                    (change.position.y - canvasState.offset.y) / canvasState.scale)
                    nodePositions = nodePositions.toMutableMap().apply { put(id, gp) }
                },
                onDragEnd = { draggingNodeId = null },
            )
        }
    ) {
        canvasSize = Offset(size.width, size.height)
        if (nodePositions.isEmpty()) return@Canvas

        val s = canvasState.scale
        val ox = canvasState.offset.x
        val oy = canvasState.offset.y
        val nodeRadius = baseRadius * s.coerceIn(0.5f, 2.5f)

        withTransform({ scale(s, s, pivot = Offset.Zero); translate(ox, oy) }) {

            edges.forEach { e ->
                val a = nodePositions[e.sourceNodeId]
                val b = nodePositions[e.targetNodeId]
                if (a != null && b != null) {
                    val related = selectedId == null ||
                            e.sourceNodeId == selectedId || e.targetNodeId == selectedId
                    drawLine(
                        color = when {
                            selectedId == null -> Color(0xFF2A2F3D)
                            related -> Color(0xFF4A5170)
                            else -> Color(0xFF171B26)
                        },
                        start = a, end = b,
                        strokeWidth = 1.5f.dp.toPx(), cap = StrokeCap.Round)
                }
            }

            nodes.forEach { n ->
                val p = nodePositions[n.id] ?: return@forEach
                val isSel = n.id == selectedId
                val dimmed = selectedId != null && !isSel &&
                        edges.none {
                            (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                            (it.targetNodeId == selectedId && it.sourceNodeId == n.id)
                        }
                val color = nodeColor(n.type.name)
                val alpha = if (dimmed) 0.25f else 1f
                val r = if (isSel) nodeRadius * 1.25f else nodeRadius

                if (isSel) drawCircle(color.copy(alpha = 0.18f), r * 1.8f, p)
                drawCircle(color.copy(alpha = 0.22f * alpha), r, p)
                drawCircle(color.copy(alpha = alpha), r, p, style = Stroke(2.5f.dp.toPx()))
                drawCircle(color.copy(alpha = alpha), r * 0.45f, p)
            }
        }
    }
}
