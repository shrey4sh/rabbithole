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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
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
 * Supports: pinch zoom, pan, node drag, tap to select, long-press select.
 * Labels are collision-aware and zoom-prioritized.
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
    var nodePositions by remember(nodes) {
        mutableStateOf(computeLayout(nodes, canvasSize.x, canvasSize.y))
    }

    val baseRadius = 26.dp.value
    // Light readable label colors (dark theme)
    val labelColorPrimary = android.graphics.Color.parseColor("#F2F0F5")
    val labelColorSecondary = android.graphics.Color.parseColor("#C8C4D0")
    val labelPaint = remember {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 15.dp.value
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            color = labelColorPrimary
            // very subtle shadow for separation only — never a substitute for light text
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.argb(120, 0, 0, 0))
        }
    }

    // ---- Label placement state -------------------------------------------------
    // Placed-label rects tracked across the frame so two visible labels NEVER overlap.
    class LabelRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun intersects(o: LabelRect): Boolean =
            left < o.right && o.left < right && top < o.bottom && o.bottom > top
        fun overlapArea(o: LabelRect): Float {
            val w = minOf(right, o.right) - maxOf(left, o.left)
            val h = minOf(bottom, o.bottom) - maxOf(top, o.top)
            return if (w > 0 && h > 0) w * h else 0f
        }
    }
    class Placement(val nodeId: String, val center: Offset, val rect: LabelRect)

    fun shorten(t: String, max: Int) = if (t.length > max) t.take(max - 1).trimEnd() + "…" else t

    /** Priority: lower = more important. Root > selected > direct neighbors > rest. */
    fun labelPriority(n: Node): Int = when {
        n.id == nodes.firstOrNull()?.id -> 0
        n.id == selectedId -> 1
        edges.any { (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                    (it.targetNodeId == selectedId && it.sourceNodeId == n.id) } -> 2
        else -> 3 + nodes.indexOf(n) % 8 // stable tie-break
    }

    /** Zoom-driven label budget: fewer labels when zoomed out, more when zoomed in. */
    fun maxVisibleLabels(scale: Float): Int = when {
        scale < 0.55f -> 7
        scale < 0.8f -> 11
        scale < 1.3f -> 16
        else -> nodes.size
    }

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
            val r = baseRadius * canvasState.scale * 1.8f
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

        // ---- Global label pass: place labels by priority so none ever overlap ----
        val s2 = canvasState.scale
        val placed = mutableListOf<Placement>()
        val screenW = size.width / s2.coerceAtLeast(0.001f)
        val screenH = size.height / s2.coerceAtLeast(0.001f)

        val drawOrder = nodes.sortedBy { labelPriority(it) }
        var shown = 0
        val budget = maxVisibleLabels(canvasState.scale)
        for (n in drawOrder) {
            val p = nodePositions[n.id] ?: continue
            if (shown >= budget) break

            val isSel = n.id == selectedId
            val dimmed = selectedId != null && !isSel &&
                    edges.none {
                        (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                        (it.targetNodeId == selectedId && it.sourceNodeId == n.id)
                    }
            if (dimmed && !isSel) continue // never label unrelated/dimmed nodes when something is selected

            // progressive shortening: try longer text first, shorten only under pressure
            val maxLen = when (labelPriority(n)) {
                0, 1 -> 22
                2 -> 18
                else -> 14
            }
            var labelText = shorten(n.title, maxLen)
            val rForLabel = if (n == nodes.firstOrNull()) nodeRadius * 1.15f
                            else if (isSel) nodeRadius * 1.25f else nodeRadius
            labelPaint.textSize = 15.dp.toPx() * canvasState.scale.coerceIn(0.7f, 1.6f)
            val th = labelPaint.fontSpacing

            fun rectFor(c: Offset): LabelRect {
                val tw = labelPaint.measureText(labelText)
                return LabelRect(c.x - tw/2 - 4f, c.y - th, c.x + tw/2 + 4f, c.y + 3f)
            }

            // candidate positions around the node
            val r = rForLabel
            val candidates = listOf(
                p + Offset(0f, r + th),                 // below
                p + Offset(0f, -(r + th * 0.35f)),      // above
                p + Offset(labelPaint.measureText(labelText)/2 + r + 4f, 0f),  // right
                p + Offset(-(labelPaint.measureText(labelText)/2 + r + 4f), 0f),// left
                p + Offset(-(r*0.8f), -(r + th)),       // upper-left
                p + Offset(r*0.8f, -(r + th)),          // upper-right
                p + Offset(-(r*0.8f), r + th),          // lower-left
                p + Offset(r*0.8f, r + th),             // lower-right
            )

            var best: Placement? = null
            var bestScore = Float.MAX_VALUE
            for (cand in candidates) {
                val rect = rectFor(cand)
                var score = 0f
                // hard rule: overlap with any placed label disqualifies
                if (placed.any { rect.intersects(it.rect) }) continue
                // penalty: covering another node circle
                for ((_, otherP) in nodePositions) {
                    if (otherP != p && otherP.x in rect.left..rect.right &&
                        otherP.y in rect.top..rect.bottom) score += 1000f
                }
                // penalty: crossing edges (sample edge midpoints)
                for (e in edges) {
                    val a = nodePositions[e.sourceNodeId] ?: continue
                    val b = nodePositions[e.targetNodeId] ?: continue
                    val mx = (a.x + b.x) / 2f; val my = (a.y + b.y) / 2f
                    if (mx in rect.left..rect.right && my in rect.top..rect.bottom) score += 60f
                }
                // penalty: off-screen / near boundaries
                if (rect.left < 8f || rect.right > screenW - 8f ||
                    rect.top < 90f/s2 || rect.bottom > screenH - 120f/s2) score += 400f
                // prefer below/above slightly
                val posIdx = candidates.indexOf(cand)
                score += posIdx * 5f
                if (score < bestScore) { bestScore = score; best = Placement(n.id, cand, rect) }
            }

            // still no spot? progressively shorten and retry once
            if (best == null && labelText.length > 8) {
                labelText = shorten(n.title.replace(Regex("\\s*\\(.*?\\)$"), ""), maxLen - 4)
                val c = p + Offset(0f, r + th)
                val rect = rectFor(c)
                if (placed.none { rect.intersects(it.rect) } &&
                    rect.right <= screenW - 8f && rect.left >= 8f) {
                    best = Placement(n.id, c, rect)
                }
            }

            if (best != null) {
                placed.add(best)
                shown++
            }
        }

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
                val isRoot = n.id == nodes.firstOrNull()?.id
                val isSel = n.id == selectedId
                val dimmed = selectedId != null && !isSel &&
                        edges.none {
                            (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                            (it.targetNodeId == selectedId && it.sourceNodeId == n.id)
                        }
                val color = nodeColor(n.type.name)
                val alpha = if (dimmed) 0.25f else 1f
                val r = when {
                    isSel -> nodeRadius * 1.25f
                    isRoot -> nodeRadius * 1.15f
                    else -> nodeRadius
                }

                // subtle root glow — small halo ring, not oversized
                if (isRoot) drawCircle(color.copy(alpha = 0.12f * alpha), r * 1.45f, p)

                if (isSel) drawCircle(color.copy(alpha = 0.18f), r * 1.8f, p)
                drawCircle(color.copy(alpha = 0.22f * alpha), r, p)
                drawCircle(color.copy(alpha = alpha), r, p,
                    style = Stroke((if (isRoot) 3.5f else 2.5f).dp.toPx()))
                drawCircle(color.copy(alpha = alpha), r * 0.45f, p)
            }

            // draw all accepted labels AFTER nodes so text sits on top cleanly
            drawIntoCanvas { cv ->
                placed.forEach { pl ->
                    val owner = nodes.firstOrNull { it.id == pl.nodeId } ?: return@forEach
                    val isSel = owner.id == selectedId
                    val isNeighbor = edges.any {
                        (it.sourceNodeId == selectedId && it.targetNodeId == owner.id) ||
                        (it.targetNodeId == selectedId && it.sourceNodeId == owner.id)
                    }
                    labelPaint.color = when {
                        isSel -> android.graphics.Color.WHITE
                        isNeighbor -> labelColorPrimary
                        else -> labelColorSecondary
                    }
                    val dimmedOwner = selectedId != null && !isSel && !isNeighbor
                    labelPaint.alpha = if (dimmedOwner) 100 else 255
                    cv.nativeCanvas.drawText(
                        shorten(owner.title, if (owner.id == nodes.firstOrNull()?.id || isSel) 22 else 18),
                        pl.center.x, pl.center.y, labelPaint)
                }
            }
        }
    }
}
