package com.example.ui.engine3d

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.*
import com.example.data.edss.PrioritizedMeshPath
import kotlin.math.*

/**
 * 3D Building structure with height and damage state
 */
data class Building3D(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val length: Float,
    val height: Float,
    val isDamaged: Boolean = false,
    val isCollapsed: Boolean = false,
    val roofColor: Color,
    val wallColor: Color
)

/**
 * Animated packet particle hopping along a 3D parabolic arc
 */
data class PacketArc3D(
    val id: String,
    val start: Point3D,
    val end: Point3D,
    val progress: Float, // 0..1
    val color: Color
)

class DigitalTwin3DEngine {

    // Precomputed terrain height map function across 0..2200m
    fun getElevation(x: Float, y: Float): Float {
        // Northern hills (y < 700m): high elevation up to 90m
        val hillFactor = ((700f - y).coerceAtLeast(0f) / 700f) * 90f
        // South-West river depression (river channel running diagonally)
        val riverDist = abs((x * 0.8f + y * 0.6f) - 1500f)
        val riverDip = if (riverDist < 250f) -((250f - riverDist) / 250f) * 25f else 0f
        return (hillFactor + riverDip + 15f).coerceAtLeast(2f)
    }

    val sampleBuildings = listOf(
        Building3D(
            id = "BLD-HSP1",
            name = "Apex Emergency Hospital",
            x = 1650f, y = 350f, width = 120f, length = 100f, height = 95f,
            roofColor = Color(0xFFEF4444), wallColor = Color(0xFF334155)
        ),
        Building3D(
            id = "BLD-SHL1",
            name = "St. Anne High School Safe Haven",
            x = 350f, y = 400f, width = 150f, length = 110f, height = 65f,
            roofColor = Color(0xFF10B981), wallColor = Color(0xFF1E293B)
        ),
        Building3D(
            id = "BLD-CIV1",
            name = "Civic Arena Relief Camp",
            x = 1850f, y = 650f, width = 160f, length = 140f, height = 75f,
            roofColor = Color(0xFF0284C7), wallColor = Color(0xFF1E293B)
        ),
        Building3D(
            id = "BLD-COL1",
            name = "Collapsed Commercial Block C",
            x = 750f, y = 480f, width = 110f, length = 90f, height = 30f,
            isCollapsed = true,
            roofColor = Color(0xFF991B1B), wallColor = Color(0xFF450A0A)
        ),
        Building3D(
            id = "BLD-RES1",
            name = "Riverside Residential Flats",
            x = 980f, y = 1220f, width = 90f, length = 80f, height = 80f,
            isDamaged = true,
            roofColor = Color(0xFFF59E0B), wallColor = Color(0xFF334155)
        )
    )

    /**
     * Renders the complete 3D Digital Twin scene onto Compose DrawScope
     */
    fun renderScene(
        drawScope: DrawScope,
        camera: Camera3D,
        animTimeSec: Float,
        waterLevelRiseCm: Float,
        gateways: List<ECVGateway>,
        hazards: List<HazardZone>,
        resources: List<RescueResource>,
        reports: List<EmergencyReport>,
        nodes: List<D2DNode>,
        themeMode: VisualThemeMode,
        selectedNodeId: String?,
        showCoverageDomes: Boolean,
        showPacketArcs: Boolean,
        prioritizedPath: PrioritizedMeshPath? = null
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // 1. Collect renderable depth items for Painter's Algorithm
        val renderList = mutableListOf<Renderable3D>()

        // A. Terrain Mesh Grid (10x10 sectors)
        val gridStep = 220f
        val steps = (2200f / gridStep).toInt()
        for (i in 0 until steps) {
            for (j in 0 until steps) {
                val x1 = i * gridStep
                val y1 = j * gridStep
                val x2 = (i + 1) * gridStep
                val y2 = (j + 1) * gridStep

                val p00 = Point3D(x1, y1, getElevation(x1, y1))
                val p10 = Point3D(x2, y1, getElevation(x2, y1))
                val p11 = Point3D(x2, y2, getElevation(x2, y2))
                val p01 = Point3D(x1, y2, getElevation(x1, y2))

                val centerZ = (p00.z + p10.z + p11.z + p01.z) / 4f
                val centerPt = Point3D((x1 + x2) / 2f, (y1 + y2) / 2f, centerZ)
                val projCenter = camera.project(centerPt, width, height)

                if (projCenter.isVisible) {
                    renderList.add(
                        TerrainQuadRenderable(
                            depth = projCenter.depth,
                            p00 = p00, p10 = p10, p11 = p11, p01 = p01,
                            themeMode = themeMode
                        )
                    )
                }
            }
        }

        // B. Animated 3D Water Inundation Surface
        hazards.filter { it.type == HazardType.FLOOD }.forEach { flood ->
            val waterBaseZ = getElevation(flood.x, flood.y) + (flood.waterDepthCm + waterLevelRiseCm) / 100f
            val segments = 8
            val r = flood.radiusMeters
            for (seg in 0 until segments) {
                val angle1 = (seg.toFloat() / segments) * 2f * PI.toFloat()
                val angle2 = ((seg + 1).toFloat() / segments) * 2f * PI.toFloat()

                val waveOffset1 = sin(angle1 * 3f + animTimeSec * 2f) * 4f
                val waveOffset2 = sin(angle2 * 3f + animTimeSec * 2f) * 4f

                val pCenter = Point3D(flood.x, flood.y, waterBaseZ + sin(animTimeSec * 1.5f) * 3f)
                val p1 = Point3D(flood.x + cos(angle1) * r, flood.y + sin(angle1) * r, waterBaseZ + waveOffset1)
                val p2 = Point3D(flood.x + cos(angle2) * r, flood.y + sin(angle2) * r, waterBaseZ + waveOffset2)

                val projCenter = camera.project(pCenter, width, height)
                if (projCenter.isVisible) {
                    renderList.add(
                        WaterTriangleRenderable(
                            depth = projCenter.depth,
                            center = pCenter,
                            p1 = p1,
                            p2 = p2,
                            themeMode = themeMode
                        )
                    )
                }
            }
        }

        // C. 3D Buildings
        sampleBuildings.forEach { bld ->
            val groundZ = getElevation(bld.x, bld.y)
            val center = Point3D(bld.x + bld.width / 2f, bld.y + bld.length / 2f, groundZ + bld.height / 2f)
            val projCenter = camera.project(center, width, height)
            if (projCenter.isVisible) {
                renderList.add(
                    BuildingRenderable(
                        depth = projCenter.depth,
                        building = bld,
                        groundZ = groundZ,
                        themeMode = themeMode
                    )
                )
            }
        }

        // D. 3D ECV Gateways with Radar Domes & Sweeping Beams
        gateways.forEach { ecv ->
            val groundZ = getElevation(ecv.x, ecv.y)
            val ecvCenter = Point3D(ecv.x, ecv.y, groundZ)
            val proj = camera.project(ecvCenter, width, height)

            if (proj.isVisible) {
                renderList.add(
                    ECVGatewayRenderable(
                        depth = proj.depth,
                        gateway = ecv,
                        groundZ = groundZ,
                        animTimeSec = animTimeSec,
                        showDome = showCoverageDomes,
                        themeMode = themeMode
                    )
                )
            }
        }

        // E. 3D Distress Beacons & D2D Nodes
        val nodesToRender = nodes.filter { it.hasActiveSos || it.id == selectedNodeId }.ifEmpty { nodes.take(40) }
        nodesToRender.forEach { node ->
            val groundZ = getElevation(node.x, node.y)
            val pt = Point3D(node.x, node.y, groundZ)
            val proj = camera.project(pt, width, height)

            if (proj.isVisible) {
                renderList.add(
                    DeviceNodeRenderable(
                        depth = proj.depth,
                        node = node,
                        groundZ = groundZ,
                        isEmergency = node.hasActiveSos,
                        isSelected = node.id == selectedNodeId,
                        animTimeSec = animTimeSec,
                        themeMode = themeMode
                    )
                )
            }
        }

        // F. 3D Moving Rescue Units
        resources.forEach { res ->
            val groundZ = getElevation(res.x, res.y)
            val pt = Point3D(res.x, res.y, groundZ)
            val proj = camera.project(pt, width, height)

            if (proj.isVisible) {
                renderList.add(
                    RescueUnitRenderable(
                        depth = proj.depth,
                        resource = res,
                        groundZ = groundZ,
                        animTimeSec = animTimeSec,
                        themeMode = themeMode
                    )
                )
            }
        }

        // G. 3D Multi-Hop Packet Arc Streams (parabolic hops)
        if (showPacketArcs) {
            reports.filter { it.status == PacketStatus.HOPPING_RELAY || it.status == PacketStatus.DISPATCHED }.forEach { rpt ->
                val targetGateway = gateways.minByOrNull { hypot(it.x - rpt.x, it.y - rpt.y) }
                if (targetGateway != null) {
                    val pStart = Point3D(rpt.x, rpt.y, getElevation(rpt.x, rpt.y) + 5f)
                    val pEnd = Point3D(targetGateway.x, targetGateway.y, getElevation(targetGateway.x, targetGateway.y) + 20f)
                    val midZ = maxOf(pStart.z, pEnd.z) + 120f
                    val arcMid = Point3D((pStart.x + pEnd.x) / 2f, (pStart.y + pEnd.y) / 2f, midZ)

                    val projMid = camera.project(arcMid, width, height)
                    if (projMid.isVisible) {
                        renderList.add(
                            PacketArcRenderable(
                                depth = projMid.depth,
                                start = pStart,
                                end = pEnd,
                                peakHeight = midZ,
                                animPhase = (animTimeSec * 1.8f) % 1.0f,
                                themeMode = themeMode
                            )
                        )
                    }
                }
            }
        }

        // H. 3D Prioritized EDSS Mesh Routing Path
        if (prioritizedPath != null) {
            val nodeMap = nodes.associateBy { it.id }
            val gtwMap = gateways.associateBy { it.id }

            for (hop in prioritizedPath.hops) {
                val fromNode = nodeMap[hop.fromNodeId]
                val toPos = if (hop.toNodeId.startsWith("GTW-")) {
                    val gtwId = hop.toNodeId.removePrefix("GTW-")
                    gtwMap[gtwId]?.let { Point3D(it.x, it.y, getElevation(it.x, it.y) + 12f) }
                } else {
                    nodeMap[hop.toNodeId]?.let { Point3D(it.x, it.y, getElevation(it.x, it.y) + 8f) }
                }

                if (fromNode != null && toPos != null) {
                    val fromPos = Point3D(fromNode.x, fromNode.y, getElevation(fromNode.x, fromNode.y) + 8f)
                    val midPos = Point3D((fromPos.x + toPos.x) / 2f, (fromPos.y + toPos.y) / 2f, (fromPos.z + toPos.z) / 2f)
                    val projMid = camera.project(midPos, width, height)
                    if (projMid.isVisible) {
                        renderList.add(
                            PrioritizedRouteHopRenderable(
                                depth = projMid.depth,
                                start = fromPos,
                                end = toPos,
                                animPhase = (animTimeSec * 2.2f) % 1.0f
                            )
                        )
                    }
                }
            }
        }

        // 2. Sort by Depth (Farthest Z first, nearest Z last = Painter's Algorithm)
        renderList.sortByDescending { it.depth }

        // 3. Draw All Primitives
        renderList.forEach { renderable ->
            renderable.draw(drawScope, camera, width, height)
        }
    }
}

/**
 * 3D Prioritized EDSS Route Hop with animated energy pulse
 */
class PrioritizedRouteHopRenderable(
    override val depth: Float,
    private val start: Point3D,
    private val end: Point3D,
    private val animPhase: Float
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val prStart = camera.project(start, width, height)
        val prEnd = camera.project(end, width, height)
        if (!prStart.isVisible && !prEnd.isVisible) return

        // Draw glowing neon routing line
        drawScope.drawLine(
            color = Color(0xCC00E5FF),
            start = prStart.screen,
            end = prEnd.screen,
            strokeWidth = 3f * prStart.scale
        )
        drawScope.drawLine(
            color = Color.White,
            start = prStart.screen,
            end = prEnd.screen,
            strokeWidth = 1.2f * prStart.scale
        )

        // Draw animated energy pulse traveling along the hop
        val pulseX = start.x + (end.x - start.x) * animPhase
        val pulseY = start.y + (end.y - start.y) * animPhase
        val pulseZ = start.z + (end.z - start.z) * animPhase
        val prPulse = camera.project(Point3D(pulseX, pulseY, pulseZ), width, height)

        drawScope.drawCircle(
            color = Color(0xFF00E5FF),
            radius = 5f * prPulse.scale,
            center = prPulse.screen
        )
        drawScope.drawCircle(
            color = Color.White,
            radius = 2.5f * prPulse.scale,
            center = prPulse.screen
        )
    }
}

/**
 * Base abstract renderable primitive with camera depth
 */
interface Renderable3D {
    val depth: Float
    fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float)
}

/**
 * 3D Terrain quadrilateral tile
 */
class TerrainQuadRenderable(
    override val depth: Float,
    private val p00: Point3D,
    private val p10: Point3D,
    private val p11: Point3D,
    private val p01: Point3D,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val pr00 = camera.project(p00, width, height)
        val pr10 = camera.project(p10, width, height)
        val pr11 = camera.project(p11, width, height)
        val pr01 = camera.project(p01, width, height)

        if (!pr00.isVisible && !pr10.isVisible && !pr11.isVisible && !pr01.isVisible) return

        val path = Path().apply {
            moveTo(pr00.screen.x, pr00.screen.y)
            lineTo(pr10.screen.x, pr10.screen.y)
            lineTo(pr11.screen.x, pr11.screen.y)
            lineTo(pr01.screen.x, pr01.screen.y)
            close()
        }

        val baseColor = when (themeMode) {
            VisualThemeMode.TACTICAL_3D -> {
                val avgZ = (p00.z + p10.z + p11.z + p01.z) / 4f
                if (avgZ > 45f) Color(0xFF1E2E38) else Color(0xFF0F1B24)
            }
            VisualThemeMode.THERMAL_FLIR -> Color(0xFF110726)
            VisualThemeMode.CYBER_TWIN -> Color(0xFF06141D)
        }

        val wireColor = when (themeMode) {
            VisualThemeMode.TACTICAL_3D -> Color(0x2238BDF8)
            VisualThemeMode.THERMAL_FLIR -> Color(0x33A855F7)
            VisualThemeMode.CYBER_TWIN -> Color(0x4400E5FF)
        }

        drawScope.drawPath(path, color = baseColor)
        drawScope.drawPath(path, color = wireColor, style = Stroke(width = 1f))
    }
}

/**
 * 3D Animated Water Surface Fragment
 */
class WaterTriangleRenderable(
    override val depth: Float,
    private val center: Point3D,
    private val p1: Point3D,
    private val p2: Point3D,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val prC = camera.project(center, width, height)
        val pr1 = camera.project(p1, width, height)
        val pr2 = camera.project(p2, width, height)

        val path = Path().apply {
            moveTo(prC.screen.x, prC.screen.y)
            lineTo(pr1.screen.x, pr1.screen.y)
            lineTo(pr2.screen.x, pr2.screen.y)
            close()
        }

        val waterColor = when (themeMode) {
            VisualThemeMode.TACTICAL_3D -> Color(0x660284C7)
            VisualThemeMode.THERMAL_FLIR -> Color(0x882563EB)
            VisualThemeMode.CYBER_TWIN -> Color(0x7700E5FF)
        }
        val edgeColor = Color(0xAA38BDF8)

        drawScope.drawPath(path, color = waterColor)
        drawScope.drawPath(path, color = edgeColor, style = Stroke(width = 1.2f))
    }
}

/**
 * 3D Extruded Building Prism
 */
class BuildingRenderable(
    override val depth: Float,
    private val building: Building3D,
    private val groundZ: Float,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val x = building.x
        val y = building.y
        val w = building.width
        val l = building.length
        val h = building.height

        // 4 Bottom vertices
        val b00 = camera.project(Point3D(x, y, groundZ), width, height)
        val b10 = camera.project(Point3D(x + w, y, groundZ), width, height)
        val b11 = camera.project(Point3D(x + w, y + l, groundZ), width, height)
        val b01 = camera.project(Point3D(x, y + l, groundZ), width, height)

        // 4 Top roof vertices
        val t00 = camera.project(Point3D(x, y, groundZ + h), width, height)
        val t10 = camera.project(Point3D(x + w, y, groundZ + h), width, height)
        val t11 = camera.project(Point3D(x + w, y + l, groundZ + h), width, height)
        val t01 = camera.project(Point3D(x, y + l, groundZ + h), width, height)

        // Draw Walls
        fun drawWall(p1: ProjectedPoint, p2: ProjectedPoint, p3: ProjectedPoint, p4: ProjectedPoint, color: Color) {
            val wallPath = Path().apply {
                moveTo(p1.screen.x, p1.screen.y)
                lineTo(p2.screen.x, p2.screen.y)
                lineTo(p3.screen.x, p3.screen.y)
                lineTo(p4.screen.x, p4.screen.y)
                close()
            }
            drawScope.drawPath(wallPath, color = color)
            drawScope.drawPath(wallPath, color = Color(0x55000000), style = Stroke(width = 1f))
        }

        drawWall(b00, b10, t10, t00, building.wallColor)
        drawWall(b10, b11, t11, t10, building.wallColor.copy(alpha = 0.85f))
        drawWall(b11, b01, t01, t11, building.wallColor)
        drawWall(b01, b00, t00, t01, building.wallColor.copy(alpha = 0.85f))

        // Draw Roof
        val roofPath = Path().apply {
            moveTo(t00.screen.x, t00.screen.y)
            lineTo(t10.screen.x, t10.screen.y)
            lineTo(t11.screen.x, t11.screen.y)
            lineTo(t01.screen.x, t01.screen.y)
            close()
        }
        drawScope.drawPath(roofPath, color = building.roofColor)
        drawScope.drawPath(roofPath, color = Color.White.copy(alpha = 0.6f), style = Stroke(width = 1.5f))
    }
}

/**
 * 3D Emergency Command Vehicle Gateway with Radar Dome & Sweeping Beam
 */
class ECVGatewayRenderable(
    override val depth: Float,
    private val gateway: ECVGateway,
    private val groundZ: Float,
    private val animTimeSec: Float,
    private val showDome: Boolean,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val ecvCenter = Point3D(gateway.x, gateway.y, groundZ + 8f)
        val proj = camera.project(ecvCenter, width, height)
        if (!proj.isVisible) return

        val s = proj.scale

        // Holographic Coverage Dome Sphere (lat/long wireframe rings)
        if (showDome) {
            val domeRadius = gateway.coverageRadiusMeters
            val domeSteps = 12
            val domePath = Path()
            for (step in 0..domeSteps) {
                val angle = (step.toFloat() / domeSteps) * 2f * PI.toFloat()
                val pt = Point3D(
                    gateway.x + cos(angle) * domeRadius,
                    gateway.y + sin(angle) * domeRadius,
                    groundZ
                )
                val pr = camera.project(pt, width, height)
                if (step == 0) domePath.moveTo(pr.screen.x, pr.screen.y) else domePath.lineTo(pr.screen.x, pr.screen.y)
            }
            drawScope.drawPath(
                domePath,
                color = Color(0x4400E5FF),
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
            )

            // Animated Sweeping Radar Beam (360 degrees)
            val sweepAngle = animTimeSec * 1.5f
            val beamEnd = Point3D(
                gateway.x + cos(sweepAngle) * domeRadius * 0.95f,
                gateway.y + sin(sweepAngle) * domeRadius * 0.95f,
                groundZ + 15f
            )
            val prBeam = camera.project(beamEnd, width, height)
            drawScope.drawLine(
                color = Color(0xAA00E5FF),
                start = proj.screen,
                end = prBeam.screen,
                strokeWidth = 2.5f
            )
        }

        // 3D ECV Vehicle Base
        drawScope.drawCircle(
            color = Color(0xFF0F172A),
            radius = 12f * s,
            center = proj.screen
        )
        drawScope.drawCircle(
            color = Color(0xFF00E5FF),
            radius = 8f * s,
            center = proj.screen
        )

        // Pulsing core
        val pulse = (sin(animTimeSec * 4f) + 1f) / 2f
        drawScope.drawCircle(
            color = Color.White.copy(alpha = 0.5f + pulse * 0.5f),
            radius = 4f * s,
            center = proj.screen
        )
    }
}

/**
 * 3D Device Node with Vertical Light Beacon
 */
class DeviceNodeRenderable(
    override val depth: Float,
    private val node: D2DNode,
    private val groundZ: Float,
    private val isEmergency: Boolean,
    private val isSelected: Boolean,
    private val animTimeSec: Float,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val basePt = Point3D(node.x, node.y, groundZ)
        val projBase = camera.project(basePt, width, height)
        if (!projBase.isVisible) return

        val s = projBase.scale

        if (isEmergency) {
            // 3D Vertical Light Pillar shooting upwards 250 meters!
            val topPt = Point3D(node.x, node.y, groundZ + 250f)
            val projTop = camera.project(topPt, width, height)

            drawScope.drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color(0x00FF3366), Color(0xCCFF3366)),
                    startY = projTop.screen.y,
                    endY = projBase.screen.y
                ),
                start = projBase.screen,
                end = projTop.screen,
                strokeWidth = 3f * s
            )

            // Ascending pulsing beacon rings along the pillar
            val ringPhase = (animTimeSec * 1.5f) % 1.0f
            val ringZ = groundZ + 250f * ringPhase
            val prRing = camera.project(Point3D(node.x, node.y, ringZ), width, height)

            drawScope.drawCircle(
                color = Color(0xFFFF3366).copy(alpha = (1f - ringPhase) * 0.8f),
                radius = 12f * ringPhase * s,
                center = prRing.screen,
                style = Stroke(width = 2f)
            )

            // Red Emergency beacon base
            drawScope.drawCircle(
                color = Color(0xFFFF3366),
                radius = 7f * s,
                center = projBase.screen
            )
        } else {
            // Normal green/cyan mesh node
            val nodeColor = if (node.isCovered) Color(0xFF00E676) else Color(0xFF64748B)
            drawScope.drawCircle(
                color = nodeColor,
                radius = 3.5f * s,
                center = projBase.screen
            )
        }

        if (isSelected) {
            drawScope.drawCircle(
                color = Color(0xFF00E5FF),
                radius = 14f * s,
                center = projBase.screen,
                style = Stroke(width = 2f)
            )
        }
    }
}

/**
 * 3D Moving Rescue Unit with heading indicator
 */
class RescueUnitRenderable(
    override val depth: Float,
    private val resource: RescueResource,
    private val groundZ: Float,
    private val animTimeSec: Float,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        val pt = Point3D(resource.x, resource.y, groundZ + 5f)
        val proj = camera.project(pt, width, height)
        if (!proj.isVisible) return

        val s = proj.scale

        // Emergency flashing lightbar (Amber & Cyan)
        val flashState = ((animTimeSec * 6f).toInt() % 2 == 0)
        val barColor = if (flashState) Color(0xFFFFB300) else Color(0xFF00E5FF)

        drawScope.drawCircle(
            color = Color.Black,
            radius = 9f * s,
            center = proj.screen
        )
        drawScope.drawCircle(
            color = barColor,
            radius = 6f * s,
            center = proj.screen
        )
    }
}

/**
 * 3D Parabolic Packet Arc Stream
 */
class PacketArcRenderable(
    override val depth: Float,
    private val start: Point3D,
    private val end: Point3D,
    private val peakHeight: Float,
    private val animPhase: Float,
    private val themeMode: VisualThemeMode
) : Renderable3D {
    override fun draw(drawScope: DrawScope, camera: Camera3D, width: Float, height: Float) {
        // Draw parabolic curve using 8 segments
        val segments = 8
        val path = Path()

        for (i in 0..segments) {
            val t = i.toFloat() / segments
            // Parabolic interpolation: Z = (1 - 4(t - 0.5)^2) * peak
            val px = start.x + (end.x - start.x) * t
            val py = start.y + (end.y - start.y) * t
            val pz = start.z + (end.z - start.z) * t + (1f - 4f * (t - 0.5f).pow(2)) * (peakHeight - start.z)

            val pr = camera.project(Point3D(px, py, pz), width, height)
            if (i == 0) path.moveTo(pr.screen.x, pr.screen.y) else path.lineTo(pr.screen.x, pr.screen.y)
        }

        drawScope.drawPath(
            path,
            color = Color(0x5500E5FF),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        )

        // Animated leaping packet particle
        val t = animPhase
        val px = start.x + (end.x - start.x) * t
        val py = start.y + (end.y - start.y) * t
        val pz = start.z + (end.z - start.z) * t + (1f - 4f * (t - 0.5f).pow(2)) * (peakHeight - start.z)
        val prParticle = camera.project(Point3D(px, py, pz), width, height)

        drawScope.drawCircle(
            color = Color.White,
            radius = 4f * prParticle.scale,
            center = prParticle.screen
        )
        drawScope.drawCircle(
            color = Color(0xFF00E5FF),
            radius = 7f * prParticle.scale,
            center = prParticle.screen,
            style = Stroke(width = 1.5f)
        )
    }
}
