package com.example.ui.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.edss.NodeTelemetry
import com.example.data.edss.PrioritizedMeshPath
import com.example.data.model.D2DNode
import com.example.data.model.ECVGateway
import kotlin.math.*

enum class TopologyColorMode {
    BATTERY_LEVEL,     // Emerald (>70%) to Amber to Red (<25%)
    HOP_DISTANCE,      // Color-coded by DTN TTL hops to ECV (1..6)
    SIGNAL_RSSI,       // Color-coded by link quality
    CONGESTION_LOAD    // Color-coded by relay traffic load rho
}

data class TopologyLink3D(
    val nodeA: D2DNode,
    val nodeB: D2DNode,
    val distanceMeters: Float,
    val linkQualityRssi: Int,
    val isPartofActiveRoute: Boolean
)

data class AnimatedPacketParticle(
    val fromPt: Point3D,
    val toPt: Point3D,
    val phase: Float, // 0..1
    val color: Color
)

class MeshTopology3DEngine {

    /**
     * Calculates 3D position for a node in the topological space.
     * Z-elevation reflects both terrain height and hierarchical topology tier:
     * - ECVs: Highest elevation command layer (Z = +140m)
     * - High-battery backbone relays: Tier 1 (Z = +80m)
     * - Edge citizen devices: Tier 2 (Z = +45m)
     * - SOS distress nodes: Highlighted with vertical beacon
     */
    fun getNode3DPosition(
        node: D2DNode,
        telemetry: NodeTelemetry?,
        groundZ: Float,
        isTieredElevation: Boolean
    ): Point3D {
        val tierZ = if (isTieredElevation) {
            when {
                node.hasActiveSos -> 110f
                (telemetry?.batteryPercent ?: 80) > 75 -> 75f
                (telemetry?.batteryPercent ?: 80) < 30 -> 35f
                else -> 55f
            }
        } else {
            30f
        }
        return Point3D(node.x, node.y, groundZ + tierZ)
    }

    /**
     * Renders the complete 3D Mesh Network Topology onto Compose DrawScope
     */
    fun renderTopology(
        drawScope: DrawScope,
        camera: Camera3D,
        animTimeSec: Float,
        nodes: List<D2DNode>,
        gateways: List<ECVGateway>,
        telemetryMap: Map<String, NodeTelemetry>,
        activeRoute: PrioritizedMeshPath?,
        colorMode: TopologyColorMode,
        selectedNodeId: String?,
        showRangeSpheres: Boolean,
        showSignalPulses: Boolean,
        showGroundShadows: Boolean,
        isTieredElevation: Boolean,
        d2dRangeMeters: Float = 260f
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        val renderList = mutableListOf<Renderable3D>()
        val nodePosMap = mutableMapOf<String, Point3D>()
        val nodeLookup = nodes.associateBy { it.id }

        // 1. Calculate 3D positions for all nodes
        nodes.forEach { node ->
            val telem = telemetryMap[node.id]
            val groundZ = 10f // base ground datum
            val pt = getNode3DPosition(node, telem, groundZ, isTieredElevation)
            nodePosMap[node.id] = pt
        }

        // 2. Gateway 3D positions
        val gtwPosMap = mutableMapOf<String, Point3D>()
        gateways.forEach { gtw ->
            val pt = Point3D(gtw.x, gtw.y, 140f)
            gtwPosMap[gtw.id] = pt
        }

        // 3. Ground Plane Coordinate Grid & Shadows
        val groundSteps = 10
        val stepSize = 220f
        for (i in 0..groundSteps) {
            val xm = i * stepSize
            renderList.add(
                GridLineRenderable(
                    start = Point3D(xm, 0f, 0f),
                    end = Point3D(xm, 2200f, 0f),
                    color = Color(0x1800E5FF)
                )
            )
            val ym = i * stepSize
            renderList.add(
                GridLineRenderable(
                    start = Point3D(0f, ym, 0f),
                    end = Point3D(2200f, ym, 0f),
                    color = Color(0x1800E5FF)
                )
            )
        }

        // 4. Ground Elevation Shadows & Connecting Stems for Nodes
        if (showGroundShadows) {
            nodes.take(50).forEach { node ->
                val nodePos = nodePosMap[node.id] ?: return@forEach
                val groundPt = Point3D(node.x, node.y, 0f)
                val proj = camera.project(nodePos, width, height)
                if (proj.isVisible) {
                    renderList.add(
                        NodeStemRenderable(
                            depth = proj.depth,
                            nodePos = nodePos,
                            groundPos = groundPt,
                            hasSos = node.hasActiveSos
                        )
                    )
                }
            }
        }

        // 5. 3D Inter-Device Mesh Topology Links
        val activeRouteNodePairs = mutableSetOf<Pair<String, String>>()
        activeRoute?.hops?.forEach { hop ->
            activeRouteNodePairs.add(hop.fromNodeId to hop.toNodeId)
            activeRouteNodePairs.add(hop.toNodeId to hop.fromNodeId)
        }

        // Compute neighbor links (within 260m)
        val sampleNodes = nodes.take(65)
        for (i in sampleNodes.indices) {
            val n1 = sampleNodes[i]
            val p1 = nodePosMap[n1.id] ?: continue

            for (j in (i + 1) until sampleNodes.size) {
                val n2 = sampleNodes[j]
                val p2 = nodePosMap[n2.id] ?: continue

                val dist = hypot(n1.x - n2.x, n1.y - n2.y)
                if (dist <= d2dRangeMeters) {
                    val isRoute = (n1.id to n2.id) in activeRouteNodePairs
                    val mid = Point3D((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f, (p1.z + p2.z) / 2f)
                    val projMid = camera.project(mid, width, height)

                    if (projMid.isVisible) {
                        renderList.add(
                            MeshLink3DRenderable(
                                depth = projMid.depth,
                                start = p1,
                                end = p2,
                                isRouteHop = isRoute,
                                rssiDbm = telemetryMap[n2.id]?.rssiStrengthDbm ?: -65,
                                animTimeSec = animTimeSec,
                                showPulses = showSignalPulses
                            )
                        )
                    }
                }
            }

            // Links to gateways within range
            gateways.forEach { gtw ->
                val dist = hypot(n1.x - gtw.x, n1.y - gtw.y)
                if (dist <= d2dRangeMeters + 60f) {
                    val pGtw = gtwPosMap[gtw.id] ?: return@forEach
                    val isRoute = (n1.id to "GTW-${gtw.id}") in activeRouteNodePairs
                    val mid = Point3D((p1.x + pGtw.x) / 2f, (p1.y + pGtw.y) / 2f, (p1.z + pGtw.z) / 2f)
                    val projMid = camera.project(mid, width, height)

                    if (projMid.isVisible) {
                        renderList.add(
                            MeshLink3DRenderable(
                                depth = projMid.depth,
                                start = p1,
                                end = pGtw,
                                isRouteHop = isRoute,
                                rssiDbm = -52,
                                animTimeSec = animTimeSec,
                                showPulses = true
                            )
                        )
                    }
                }
            }
        }

        // 6. 3D Gateways
        gateways.forEach { gtw ->
            val pGtw = gtwPosMap[gtw.id] ?: return@forEach
            val proj = camera.project(pGtw, width, height)
            if (proj.isVisible) {
                renderList.add(
                    GatewayTopologyRenderable(
                        depth = proj.depth,
                        gateway = gtw,
                        pos = pGtw,
                        animTimeSec = animTimeSec
                    )
                )
            }
        }

        // 7. 3D Peer Nodes with Telemetry Rings
        nodes.forEach { node ->
            val pt = nodePosMap[node.id] ?: return@forEach
            val proj = camera.project(pt, width, height)
            if (proj.isVisible) {
                val telem = telemetryMap[node.id]
                renderList.add(
                    PeerNode3DRenderable(
                        depth = proj.depth,
                        node = node,
                        pos = pt,
                        telemetry = telem,
                        colorMode = colorMode,
                        isSelected = node.id == selectedNodeId,
                        showRangeSphere = showRangeSpheres && (node.id == selectedNodeId || node.hasActiveSos),
                        animTimeSec = animTimeSec
                    )
                )
            }
        }

        // 8. Depth Sort (Painter's Algorithm)
        renderList.sortByDescending { it.depth }

        // 9. Draw all primitives
        renderList.forEach { it.draw(drawScope, camera, width, height) }
    }
}

/**
 * 3D Ground coordinate grid line
 */
class GridLineRenderable(
    private val start: Point3D,
    private val end: Point3D,
    private val color: Color
) : Renderable3D {
    override val depth: Float = (start.z + end.z) / 2f

    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val prStart = camera.project(start, width, height)
        val prEnd = camera.project(end, width, height)
        if (prStart.isVisible || prEnd.isVisible) {
            drawScope.drawLine(
                color = color,
                start = prStart.screen,
                end = prEnd.screen,
                strokeWidth = 1f
            )
        }
    }
}

/**
 * Vertical stem connecting 3D elevated node to its ground coordinate shadow
 */
class NodeStemRenderable(
    override val depth: Float,
    private val nodePos: Point3D,
    private val groundPos: Point3D,
    private val hasSos: Boolean
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val prTop = camera.project(nodePos, width, height)
        val prBottom = camera.project(groundPos, width, height)
        if (!prTop.isVisible && !prBottom.isVisible) return

        val stemColor = if (hasSos) Color(0x55FF3366) else Color(0x3338BDF8)

        drawScope.drawLine(
            color = stemColor,
            start = prTop.screen,
            end = prBottom.screen,
            strokeWidth = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Ground shadow dot
        drawScope.drawCircle(
            color = Color(0x66000000),
            radius = 3.5f * prBottom.scale,
            center = prBottom.screen
        )
    }
}

/**
 * 3D Animated Mesh Link with directional telemetry pulses
 */
class MeshLink3DRenderable(
    override val depth: Float,
    private val start: Point3D,
    private val end: Point3D,
    private val isRouteHop: Boolean,
    private val rssiDbm: Int,
    private val animTimeSec: Float,
    private val showPulses: Boolean
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val prStart = camera.project(start, width, height)
        val prEnd = camera.project(end, width, height)
        if (!prStart.isVisible && !prEnd.isVisible) return

        val s = prStart.scale

        if (isRouteHop) {
            // Highlighted active route: vibrant golden/cyan glowing conduit
            drawScope.drawLine(
                color = Color(0x6600E5FF),
                start = prStart.screen,
                end = prEnd.screen,
                strokeWidth = 5f * s
            )
            drawScope.drawLine(
                color = Color(0xFF00E5FF),
                start = prStart.screen,
                end = prEnd.screen,
                strokeWidth = 2.2f * s
            )
            drawScope.drawLine(
                color = Color.White,
                start = prStart.screen,
                end = prEnd.screen,
                strokeWidth = 1.0f * s
            )
        } else {
            // General peer link colored by RSSI
            val linkColor = when {
                rssiDbm > -70 -> Color(0x3338BDF8) // Strong
                rssiDbm > -85 -> Color(0x2238BDF8) // Medium
                else -> Color(0x22FFAA00)          // Weak/fading
            }
            drawScope.drawLine(
                color = linkColor,
                start = prStart.screen,
                end = prEnd.screen,
                strokeWidth = 1.2f * s
            )
        }

        // Directional pulse particle traveling along link
        if (showPulses || isRouteHop) {
            val speed = if (isRouteHop) 2.5f else 1.2f
            val phase = (animTimeSec * speed + (start.x.hashCode() % 100) / 100f) % 1.0f
            val pulseX = start.x + (end.x - start.x) * phase
            val pulseY = start.y + (end.y - start.y) * phase
            val pulseZ = start.z + (end.z - start.z) * phase
            val prPulse = camera.project(Point3D(pulseX, pulseY, pulseZ), width, height)

            drawScope.drawCircle(
                color = if (isRouteHop) Color(0xFF00E5FF) else Color(0xFF38BDF8),
                radius = (if (isRouteHop) 4.5f else 2.5f) * prPulse.scale,
                center = prPulse.screen
            )
        }
    }
}

/**
 * 3D Peer Node with battery indicator ring, RSSI orbital halo, and distress beacon
 */
class PeerNode3DRenderable(
    override val depth: Float,
    private val node: D2DNode,
    private val pos: Point3D,
    private val telemetry: NodeTelemetry?,
    private val colorMode: TopologyColorMode,
    private val isSelected: Boolean,
    private val showRangeSphere: Boolean,
    private val animTimeSec: Float
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val proj = camera.project(pos, width, height)
        if (!proj.isVisible) return

        val s = proj.scale
        val batt = telemetry?.batteryPercent ?: node.batteryPercent
        val rssi = telemetry?.rssiStrengthDbm ?: -65

        // Determine node color based on active color mode
        val nodeColor = when (colorMode) {
            TopologyColorMode.BATTERY_LEVEL -> {
                when {
                    batt > 70 -> Color(0xFF00E676) // Healthy
                    batt > 30 -> Color(0xFFFFB300) // Medium
                    else -> Color(0xFFFF3366)      // Critical Low
                }
            }
            TopologyColorMode.HOP_DISTANCE -> {
                when (node.hopDistance) {
                    1 -> Color(0xFF00E5FF)
                    2 -> Color(0xFF38BDF8)
                    3 -> Color(0xFFA855F7)
                    4 -> Color(0xFFF59E0B)
                    else -> Color(0xFF64748B)
                }
            }
            TopologyColorMode.SIGNAL_RSSI -> {
                when {
                    rssi > -65 -> Color(0xFF00E676)
                    rssi > -80 -> Color(0xFF38BDF8)
                    else -> Color(0xFFFF3366)
                }
            }
            TopologyColorMode.CONGESTION_LOAD -> {
                val rho = telemetry?.relayLoadRatio ?: 0.1f
                if (rho > 0.6f) Color(0xFFFF3366) else if (rho > 0.3f) Color(0xFFFFB300) else Color(0xFF00E676)
            }
        }

        // 3D D2D Wireless Range Sphere (260m)
        if (showRangeSphere) {
            val rangeSteps = 16
            val r = 260f
            val spherePath = Path()
            for (i in 0..rangeSteps) {
                val a = (i.toFloat() / rangeSteps) * 2f * PI.toFloat()
                val pt = Point3D(pos.x + cos(a) * r, pos.y + sin(a) * r, pos.z)
                val pr = camera.project(pt, width, height)
                if (i == 0) spherePath.moveTo(pr.screen.x, pr.screen.y) else spherePath.lineTo(pr.screen.x, pr.screen.y)
            }
            drawScope.drawPath(
                spherePath,
                color = Color(0x3300E5FF),
                style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            )
        }

        // Distress SOS vertical beam
        if (node.hasActiveSos) {
            val beamTop = Point3D(pos.x, pos.y, pos.z + 180f)
            val prTop = camera.project(beamTop, width, height)
            drawScope.drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color(0x00FF3366), Color(0xEEFF3366)),
                    startY = prTop.screen.y,
                    endY = proj.screen.y
                ),
                start = proj.screen,
                end = prTop.screen,
                strokeWidth = 3f * s
            )

            // Ascending beacon rings
            val phase = (animTimeSec * 2f) % 1.0f
            drawScope.drawCircle(
                color = Color(0xFFFF3366).copy(alpha = (1f - phase) * 0.8f),
                radius = (10f + phase * 20f) * s,
                center = proj.screen,
                style = Stroke(width = 1.5f)
            )
        }

        // Concentric Orbital Battery Ring around Node
        val battSweep = (batt / 100f) * 360f
        val ringRadius = 9f * s
        drawScope.drawArc(
            color = nodeColor,
            startAngle = -90f,
            sweepAngle = battSweep,
            useCenter = false,
            topLeft = Offset(proj.screen.x - ringRadius, proj.screen.y - ringRadius),
            size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
            style = Stroke(width = 2f * s)
        )

        // Node Central Sphere
        drawScope.drawCircle(
            color = Color(0xFF0F172A),
            radius = 5.5f * s,
            center = proj.screen
        )
        drawScope.drawCircle(
            color = if (node.hasActiveSos) Color(0xFFFF3366) else nodeColor,
            radius = 4f * s,
            center = proj.screen
        )

        // Selection crosshair / reticle
        if (isSelected) {
            val selR = 15f * s
            drawScope.drawCircle(
                color = Color(0xFF00E5FF),
                radius = selR,
                center = proj.screen,
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            )
        }
    }
}

/**
 * 3D Gateway Command Vehicle representation in Topology Space
 */
class GatewayTopologyRenderable(
    override val depth: Float,
    private val gateway: ECVGateway,
    private val pos: Point3D,
    private val animTimeSec: Float
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val proj = camera.project(pos, width, height)
        if (!proj.isVisible) return

        val s = proj.scale

        // Pulsing command ring
        val pulse = (sin(animTimeSec * 3f) + 1f) / 2f
        drawScope.drawCircle(
            color = Color(0x3300E5FF),
            radius = (16f + pulse * 10f) * s,
            center = proj.screen,
            style = Stroke(width = 1.5f)
        )

        // Diamond gateway symbol
        val path = Path().apply {
            moveTo(proj.screen.x, proj.screen.y - 12f * s)
            lineTo(proj.screen.x + 12f * s, proj.screen.y)
            lineTo(proj.screen.x, proj.screen.y + 12f * s)
            lineTo(proj.screen.x - 12f * s, proj.screen.y)
            close()
        }
        drawScope.drawPath(path, color = Color(0xFF0F172A))
        drawScope.drawPath(path, color = Color(0xFF00E5FF), style = Stroke(width = 2f * s))

        drawScope.drawCircle(
            color = Color.White,
            radius = 3.5f * s,
            center = proj.screen
        )
    }
}
