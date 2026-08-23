package com.shrey4sh.rabbithole.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
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
    // alternate radii slightly so neighbors never sit at identical distance (avoids crowding)
    ring1.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / ring1.size - Math.PI / 2
        val r = min(width, height) * (if (i % 2 == 0) 0.30f else 0.38f)
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    ring2.forEachIndexed { i, n ->
        val angle = 2 * Math.PI * i / maxOf(ring2.size, 1) - Math.PI / 2 + 0.3
        val r = min(width, height) * 0.46f
        positions[n.id] = Offset(cx + (r * Math.cos(angle)).toFloat(), cy + (r * Math.sin(angle)).toFloat())
    }
    return positions
}

class GraphCanvasState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() { scale = 1f; offset = Offset.Zero }

    /** Fit the graph bounding box comfortably into the viewport (center button). */
    fun fitTo(positions: Map<String, Offset>, viewW: Float, viewH: Float) {
        if (positions.isEmpty()) return
        val minX = positions.values.minOf { it.x }; val maxX = positions.values.maxOf { it.x }
        val minY = positions.values.minOf { it.y }; val maxY = positions.values.maxOf { it.y }
        val bw = (maxX - minX).coerceAtLeast(1f); val bh = (maxY - minY).coerceAtLeast(1f)
        val padX = viewW * 0.18f; val padY = viewH * 0.22f
        val s = minOf((viewW - padX * 2) / bw, (viewH - padY * 2) / bh).coerceIn(0.4f, 1.6f)
        scale = s
        offset = Offset(
            viewW / 2f - (minX + bw / 2f) * s,
            viewH / 2f - (minY + bh / 2f) * s,
        )
    }

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
    onNodeLongPress: (Node) -> Unit = {},
    onTapEmpty: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    canvasState: GraphCanvasState = rememberGraphCanvasState(),
) {
    var canvasSize by remember { mutableStateOf(Offset(1000f, 1600f)) }
    val didFit = remember(nodes) { mutableStateOf(false) }
    var nodePositions by remember(nodes) {
        mutableStateOf(computeLayout(nodes, canvasSize.x, canvasSize.y))
    }

    val baseRadius = 26.dp.value
    // Material You derived colors (fall back to neutral values pre-S / in canvas scope)
    val m3 = MaterialTheme.colorScheme
    val edgeDefault = m3.outlineVariant.copy(alpha = 0.55f)
    val edgeSelected = m3.primary.copy(alpha = 0.55f)
    val edgeDimmed = m3.outlineVariant.copy(alpha = 0.18f)
    val rootColor = m3.primary
    val selectedColor = m3.primary
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

    /** Zoom-driven label budget: labels hide ONLY when genuinely crowded. */
    fun maxVisibleLabels(scale: Float): Int = when {
        scale < 0.55f -> maxOf(10, (nodes.size * 0.7f).toInt())
        scale < 0.8f -> maxOf(14, (nodes.size * 0.85f).toInt())
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
        .pointerInput(nodes, nodePositions) {
            awaitEachGesture {
                // --- unified gesture layer ---
                val down = awaitFirstDown(requireUnconsumed = false)
                val startNode = nodeAt(down.position)
                var isDragging = false          // true once movement starts
                var moved = false
                do {
                    val event = awaitPointerEvent()
                    val pointers = event.changes.filter { it.pressed }
                    when {
                        // ---- PINCH (2+ fingers): zoom around focal point + pan ----
                        pointers.size >= 2 -> {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculateCentroid(useCurrent = true) -
                                    event.calculateCentroid(useCurrent = false)
                            if (zoomChange != 1f) {
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val newScale = (canvasState.scale * zoomChange).coerceIn(0.4f, 3f)
                                // keep content under the fingers stationary while scaling
                                canvasState.offset = Offset(
                                    (canvasState.offset.x - centroid.x) * (newScale / canvasState.scale) + centroid.x,
                                    (canvasState.offset.y - centroid.y) * (newScale / canvasState.scale) + centroid.y)
                                canvasState.scale = newScale
                            }
                            canvasState.offset += panChange
                            moved = true
                            pointers.forEach { it.consume() }
                        }
                        // ---- ONE FINGER ----
                        pointers.size == 1 -> {
                            val change = pointers.first()
                            val delta = change.positionChange()
                            if (!moved && delta.getDistance() > 6f / canvasState.scale) {
                                moved = true
                                isDragging = startNode != null   // started on a node → drag node
                            }
                            if (moved && !change.isConsumed) {
                                if (isDragging && startNode != null) {
                                    // move just this node; edges + label follow automatically
                                    val gpDelta = Offset(delta.x / canvasState.scale,
                                                         delta.y / canvasState.scale)
                                    nodePositions = nodePositions.toMutableMap().apply {
                                        put(startNode.id, (nodePositions[startNode.id] ?: Offset.Zero) + gpDelta)
                                    }
                                } else {
                                    // empty space → pan whole graph
                                    canvasState.offset += delta
                                }
                                change.consume()
                            } else if (moved) {
                                change.consume()
                            }
                        }
                    }
                } while (event.changes.any { it.pressed })

                // ---- TAPS (no significant movement) ----
                if (!moved && startNode == null) {
                    onTapEmpty?.invoke()  // tap on empty space = deselect
                }
                if (!moved && startNode != null) {
                    onNodeTap(startNode)
                }
            }
        }
    ) {
        canvasSize = Offset(size.width, size.height)
        if (nodePositions.isEmpty()) return@Canvas

        // first layout: fit the whole graph comfortably on screen
        if (!didFit.value) {
            canvasState.fitTo(nodePositions, size.width, size.height)
            didFit.value = true
        }

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
            val prio = labelPriority(n)
            if (!(prio <= 2) && shown >= budget) continue

            val isSel = n.id == selectedId
            val isNeighborOfSel = selectedId != null &&
                    edges.any {
                        (it.sourceNodeId == selectedId && it.targetNodeId == n.id) ||
                        (it.targetNodeId == selectedId && it.sourceNodeId == n.id)
                    }
            // Priority 0-2 (root, selected, direct neighbors) ALWAYS get labels —
            // never dropped by budget or dimming. Only low-priority labels may hide.
            fun mustShow(): Boolean = prio <= 2

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

            // still no spot? progressively shorten and retry — must-show labels
            // get an extra shortened attempt so important nodes never go unlabeled
            if (best == null) {
                val base = n.title.replace(Regex("\\s*\\(.*?\\)$"), "")
                for (len in listOf(maxLen - 4, 8)) {
                    if (base.length <= len && best != null) break
                    labelText = shorten(base, len)
                    // re-evaluate all candidate positions with the shorter text
                    for (cand in candidates) {
                        val rect = rectFor(cand)
                        if (placed.any { it.rect.intersects(rect) }) continue
                        if (rect.right > screenW - 8f || rect.left < 8f ||
                            rect.top < 90f / s2 || rect.bottom > screenH - 120f / s2) continue
                        best = Placement(n.id, cand, rect)
                        break
                    }
                    if (best != null || base.length <= len) break
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
                            selectedId == null -> edgeDefault
                            related -> edgeSelected
                            else -> edgeDimmed
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
                val color = when {
                    isRoot -> rootColor
                    isSel -> selectedColor
                    else -> nodeColor(n.type.name)
                }
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
