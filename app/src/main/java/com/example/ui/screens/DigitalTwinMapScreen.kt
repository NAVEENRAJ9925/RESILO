package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.maps.GroundedPlace
import com.example.data.maps.GroundedPlaceType
import com.example.ui.components.GoogleMapsGroundingHeaderBanner
import com.example.ui.components.GoogleMapsPlaceDetailModal
import com.example.ui.components.MetricGaugeCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MapLayerFilter
import com.example.ui.viewmodel.ResilioViewModel
import kotlin.math.hypot

enum class DigitalTwinViewMode {
    MESH_TOPOLOGY_3D,
    TERRAIN_3D,
    ORTHO_MAP_2D
}

@Composable
fun DigitalTwinMapScreen(
    viewModel: ResilioViewModel
) {
    val isResilio by viewModel.isResilioMode.collectAsState()
    val gateways by viewModel.gateways.collectAsState()
    val hazards by viewModel.hazards.collectAsState()
    val shelters by viewModel.shelters.collectAsState()
    val hospitals by viewModel.hospitals.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val metrics by viewModel.simulationMetrics.collectAsState()
    val mapLayers by viewModel.mapLayers.collectAsState()
    val selectedEntity by viewModel.selectedEntity.collectAsState()

    // Google Maps Grounding State (gemini-3.8-flash)
    val groundingResult by viewModel.groundedMapsResult.collectAsState()
    val isGroundingLoading by viewModel.isGroundingLoading.collectAsState()
    val selectedRegion by viewModel.selectedRegion.collectAsState()
    var activePlaceModal by remember { mutableStateOf<GroundedPlace?>(null) }

    var viewMode by remember { mutableStateOf(DigitalTwinViewMode.MESH_TOPOLOGY_3D) }
    var showHazardDialog by remember { mutableStateOf(false) }

    // Pan and Zoom states for 2.2km coordinate mapping
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseRadius"
    )

    Box(modifier = Modifier.fillMaxSize().background(ResilioNavyDark)) {
        when (viewMode) {
            DigitalTwinViewMode.MESH_TOPOLOGY_3D -> {
                MeshTopology3DScreen(viewModel = viewModel)
            }
            DigitalTwinViewMode.TERRAIN_3D -> {
                DigitalTwin3DScreen(viewModel = viewModel)
            }
            DigitalTwinViewMode.ORTHO_MAP_2D -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapControlsHeader(
                        currentMode = viewMode,
                        onSelectMode = { viewMode = it },
                        mapLayers = mapLayers,
                        onToggleLayer = { viewModel.toggleLayer(it) },
                        onResetView = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        onAddHazard = { showHazardDialog = true }
                    )

                    // Real-Time Google Maps Grounding Header Banner
                    GoogleMapsGroundingHeaderBanner(
                        groundingResult = groundingResult,
                        isLoading = isGroundingLoading,
                        selectedRegion = selectedRegion,
                        presetRegions = viewModel.presetRegions,
                        onSelectRegion = { viewModel.triggerMapsGrounding(it) },
                        onTriggerSync = { viewModel.triggerMapsGrounding(it) },
                        onViewPlaceDetails = { activePlaceModal = it },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

            // Main Digital Twin Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, ResilioNavyBorder)
                    .background(Color(0xFF09121A))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.7f, 3.5f)
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-400f * scale, 400f * scale),
                                y = (offset.y + pan.y).coerceIn(-400f * scale, 400f * scale)
                            )
                        }
                    }
                    .pointerInput(gateways, hazards, shelters, reports, groundingResult) {
                        detectTapGestures { tapOffset ->
                            groundingResult?.places?.forEach { gp ->
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val minDim = minOf(canvasWidth, canvasHeight)
                                val base = (gp.digitalTwinX / 2200f) * minDim + (canvasWidth - minDim) / 2f
                                val px = (base - canvasWidth / 2f) * scale + canvasWidth / 2f + offset.x
                                val baseY = (gp.digitalTwinY / 2200f) * minDim + (canvasHeight - minDim) / 2f
                                val py = (baseY - canvasHeight / 2f) * scale + canvasHeight / 2f + offset.y

                                if (hypot(px - tapOffset.x, py - tapOffset.y) < 36f * scale) {
                                    activePlaceModal = gp
                                    return@detectTapGestures
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val minDim = minOf(canvasWidth, canvasHeight)

                    // Map 0..2200 meters to canvas coordinates
                    fun toCanvasX(xMeters: Float): Float {
                        val base = (xMeters / 2200f) * minDim + (canvasWidth - minDim) / 2f
                        return (base - canvasWidth / 2f) * scale + canvasWidth / 2f + offset.x
                    }

                    fun toCanvasY(yMeters: Float): Float {
                        val base = (yMeters / 2200f) * minDim + (canvasHeight - minDim) / 2f
                        return (base - canvasHeight / 2f) * scale + canvasHeight / 2f + offset.y
                    }

                    fun toCanvasRadius(rMeters: Float): Float {
                        return (rMeters / 2200f) * minDim * scale
                    }

                    // 1. Draw Grid Lines (Every 500 meters)
                    val gridStep = 550f
                    for (gx in 0..4) {
                        val xm = gx * gridStep
                        val cx = toCanvasX(xm)
                        drawLine(
                            color = Color(0x1F38BDF8),
                            start = Offset(cx, toCanvasY(0f)),
                            end = Offset(cx, toCanvasY(2200f)),
                            strokeWidth = 1f
                        )
                    }
                    for (gy in 0..4) {
                        val ym = gy * gridStep
                        val cy = toCanvasY(ym)
                        drawLine(
                            color = Color(0x1F38BDF8),
                            start = Offset(toCanvasX(0f), cy),
                            end = Offset(toCanvasX(2200f), cy),
                            strokeWidth = 1f
                        )
                    }

                    // Outer Area Border (2.2km x 2.2km)
                    drawRect(
                        color = Color(0x4438BDF8),
                        topLeft = Offset(toCanvasX(0f), toCanvasY(0f)),
                        size = androidx.compose.ui.geometry.Size(
                            width = toCanvasX(2200f) - toCanvasX(0f),
                            height = toCanvasY(2200f) - toCanvasY(0f)
                        ),
                        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                    )

                    // 2. Draw Hazards
                    if (mapLayers.showHazards) {
                        hazards.forEach { h ->
                            val cx = toCanvasX(h.x)
                            val cy = toCanvasY(h.y)
                            val cr = toCanvasRadius(h.radiusMeters)

                            val hColor = when (h.type) {
                                HazardType.FLOOD -> Color(0x440284C7)
                                HazardType.COLLAPSED_BRIDGE -> Color(0x55E11D48)
                                HazardType.FIRE_SMOKE -> Color(0x55D97706)
                                HazardType.ROAD_BLOCKED -> Color(0x55DC2626)
                            }
                            val borderCol = when (h.type) {
                                HazardType.FLOOD -> Color(0xFF38BDF8)
                                else -> Color(0xFFF43F5E)
                            }

                            drawCircle(color = hColor, radius = cr, center = Offset(cx, cy))
                            drawCircle(
                                color = borderCol,
                                radius = cr,
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.8f)
                            )
                        }
                    }

                    // 3. Draw D2D Mesh Nodes (260 nodes)
                    if (mapLayers.showMeshLinks) {
                        // Draw subset of peer links
                        val nodesSample = nodes.take(70)
                        for (i in nodesSample.indices) {
                            for (j in (i + 1) until nodesSample.size) {
                                val n1 = nodesSample[i]
                                val n2 = nodesSample[j]
                                val d = hypot(n1.x - n2.x, n1.y - n2.y)
                                if (d < 260f) { // Within 260m D2D range
                                    drawLine(
                                        color = if (n1.isCovered && n2.isCovered) Color(0x2238BDF8) else Color(0x1164748B),
                                        start = Offset(toCanvasX(n1.x), toCanvasY(n1.y)),
                                        end = Offset(toCanvasX(n2.x), toCanvasY(n2.y)),
                                        strokeWidth = 1f
                                    )
                                }
                            }
                        }
                    }

                    // Draw Nodes themselves
                    nodes.forEach { node ->
                        val nx = toCanvasX(node.x)
                        val ny = toCanvasY(node.y)
                        val nColor = when {
                            node.hasActiveSos -> ResilioEmergencyRed
                            node.isCovered -> Color(0xFF00E676)
                            else -> Color(0xFF64748B) // Grey for uncovered as in Figure 5!
                        }
                        val nr = if (node.hasActiveSos) 5f * scale else 2.5f * scale
                        drawCircle(color = nColor, radius = nr, center = Offset(nx, ny))
                    }

                    // 4. Draw ECV Gateways with Coverage & Radar Pulse
                    if (mapLayers.showGateways) {
                        gateways.forEach { g ->
                            val gx = toCanvasX(g.x)
                            val gy = toCanvasY(g.y)
                            val gRadius = toCanvasRadius(g.coverageRadiusMeters)

                            // Coverage Circle
                            drawCircle(
                                color = Color(0x1A00E5FF),
                                radius = gRadius,
                                center = Offset(gx, gy)
                            )
                            drawCircle(
                                color = ResilioCyanPrimary.copy(alpha = 0.4f),
                                radius = gRadius,
                                center = Offset(gx, gy),
                                style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            )

                            // Animated Radar Pulse
                            drawCircle(
                                color = ResilioCyanPrimary.copy(alpha = (1f - radarPulse) * 0.35f),
                                radius = gRadius * radarPulse,
                                center = Offset(gx, gy),
                                style = Stroke(width = 2f)
                            )

                            // ECV Gateway Center Point (Red/Cyan Triangle or bold node)
                            drawCircle(
                                color = Color.Black,
                                radius = 9f * scale,
                                center = Offset(gx, gy)
                            )
                            drawCircle(
                                color = ResilioCyanPrimary,
                                radius = 7f * scale,
                                center = Offset(gx, gy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f * scale,
                                center = Offset(gx, gy)
                            )
                        }
                    }

                    // 5. Draw Shelters
                    if (mapLayers.showShelters) {
                        shelters.forEach { s ->
                            val sx = toCanvasX(s.x)
                            val sy = toCanvasY(s.y)
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 7f * scale,
                                center = Offset(sx, sy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3f * scale,
                                center = Offset(sx, sy)
                            )
                        }
                    }

                    // 6. Draw Hospitals
                    if (mapLayers.showHospitals) {
                        hospitals.forEach { h ->
                            val hx = toCanvasX(h.x)
                            val hy = toCanvasY(h.y)
                            drawCircle(
                                color = Color(0xFFEF4444),
                                radius = 7f * scale,
                                center = Offset(hx, hy)
                            )
                            // Plus shape
                            drawLine(
                                color = Color.White,
                                start = Offset(hx - 3f * scale, hy),
                                end = Offset(hx + 3f * scale, hy),
                                strokeWidth = 1.5f * scale
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(hx, hy - 3f * scale),
                                end = Offset(hx, hy + 3f * scale),
                                strokeWidth = 1.5f * scale
                            )
                        }
                    }

                    // 6b. Draw Grounded Google Maps Places (with Google Pin Badges & Halos)
                    groundingResult?.places?.forEach { gp ->
                        val gx = toCanvasX(gp.digitalTwinX)
                        val gy = toCanvasY(gp.digitalTwinY)

                        val pinColor = when (gp.type) {
                            GroundedPlaceType.EMERGENCY_HOSPITAL, GroundedPlaceType.TRAUMA_CENTER -> Color(0xFFEA4335)
                            GroundedPlaceType.RELIEF_SHELTER, GroundedPlaceType.COMMUNITY_HALL -> Color(0xFF34A853)
                            GroundedPlaceType.CRITICAL_BRIDGE -> Color(0xFFFBBC04)
                            GroundedPlaceType.POLICE_STATION -> Color(0xFF4285F4)
                        }

                        // Google Maps Grounded Radar Halo
                        drawCircle(
                            color = pinColor.copy(alpha = 0.22f),
                            radius = 16f * scale,
                            center = Offset(gx, gy)
                        )
                        // Google Pin Base Ring
                        drawCircle(
                            color = pinColor,
                            radius = 7.5f * scale,
                            center = Offset(gx, gy)
                        )
                        // Inner White Core
                        drawCircle(
                            color = Color.White,
                            radius = 3.2f * scale,
                            center = Offset(gx, gy)
                        )
                    }

                    // 7. Draw Rescue Resources & Heading
                    if (mapLayers.showRescueUnits) {
                        resources.forEach { res ->
                            val rx = toCanvasX(res.x)
                            val ry = toCanvasY(res.y)

                            // Trajectory line if dispatched
                            if (res.targetX != null && res.targetY != null) {
                                drawLine(
                                    color = ResilioWarningAmber.copy(alpha = 0.7f),
                                    start = Offset(rx, ry),
                                    end = Offset(toCanvasX(res.targetX!!), toCanvasY(res.targetY!!)),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                )
                            }

                            drawCircle(
                                color = ResilioWarningAmber,
                                radius = 6f * scale,
                                center = Offset(rx, ry)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 3f * scale,
                                center = Offset(rx, ry)
                            )
                        }
                    }

                    // 8. Draw Active Reports (Pulsing SOS)
                    reports.forEach { r ->
                        val rx = toCanvasX(r.x)
                        val ry = toCanvasY(r.y)
                        drawCircle(
                            color = ResilioEmergencyRed.copy(alpha = 0.3f),
                            radius = 14f * scale * radarPulse,
                            center = Offset(rx, ry)
                        )
                        drawCircle(
                            color = ResilioEmergencyRed,
                            radius = 6f * scale,
                            center = Offset(rx, ry)
                        )
                    }
                }

                // Map Legend Overlay
                MapLegend(
                    isResilio = isResilio,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )

                // Quick Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    FilledIconButton(
                        onClick = { scale = (scale * 1.25f).coerceAtMost(3.5f) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = ResilioNavyCard),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = ResilioCyanPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    FilledIconButton(
                        onClick = { scale = (scale / 1.25f).coerceAtLeast(0.7f) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = ResilioNavyCard),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = ResilioCyanPrimary)
                    }
                }
            }

            // Bottom Real-time Telemetry Bar
            DigitalTwinMetricsStrip(metrics = metrics, isResilio = isResilio)
        }
    }
}

        // Floating Google Maps Grounding HUD Pill (Visible in 3D modes)
        if (viewMode != DigitalTwinViewMode.ORTHO_MAP_2D) {
            Surface(
                color = ResilioNavySurface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4285F4)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .clickable {
                        viewModel.triggerMapsGrounding(selectedRegion)
                    }
                    .testTag("floating_maps_grounding_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isGroundingLoading) Color(0xFFFBBC04) else Color(0xFF34A853))
                    )
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Google Maps",
                        tint = Color(0xFFEA4335),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isGroundingLoading) "Grounding with Google Maps..." else "Google Maps: ${selectedRegion.name.substringBefore(" (")}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = Color(0xFF4285F4).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "gemini-3.8-flash",
                            color = Color(0xFF8AB4F8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Floating 3-Mode View Switcher Pill at bottom center
        FloatingTwinModeSwitcher(
            viewMode = viewMode,
            onSelectMode = { viewMode = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )
    }

    if (showHazardDialog) {
        AddHazardDialog(
            onDismiss = { showHazardDialog = false },
            onConfirm = { name, type, x, y, radius, depth ->
                viewModel.injectDisasterScenario(name, type, x, y, radius, depth)
                showHazardDialog = false
            }
        )
    }

    // Google Maps Place Detail Modal
    if (activePlaceModal != null) {
        GoogleMapsPlaceDetailModal(
            place = activePlaceModal!!,
            onDismiss = { activePlaceModal = null }
        )
    }
}

@Composable
private fun FloatingTwinModeSwitcher(
    viewMode: DigitalTwinViewMode,
    onSelectMode: (DigitalTwinViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ResilioNavySurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        modifier = modifier.border(1.2.dp, ResilioCyanPrimary, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModePill(
                label = "3D Topology",
                icon = Icons.Default.Hub,
                isSelected = viewMode == DigitalTwinViewMode.MESH_TOPOLOGY_3D,
                onClick = { onSelectMode(DigitalTwinViewMode.MESH_TOPOLOGY_3D) }
            )
            ModePill(
                label = "3D Terrain",
                icon = Icons.Default.ViewInAr,
                isSelected = viewMode == DigitalTwinViewMode.TERRAIN_3D,
                onClick = { onSelectMode(DigitalTwinViewMode.TERRAIN_3D) }
            )
            ModePill(
                label = "2D Map",
                icon = Icons.Default.Map,
                isSelected = viewMode == DigitalTwinViewMode.ORTHO_MAP_2D,
                onClick = { onSelectMode(DigitalTwinViewMode.ORTHO_MAP_2D) }
            )
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isSelected) ResilioCyanPrimary else Color.Transparent,
            contentColor = if (isSelected) Color.Black else ResilioTextSecondary
        ),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MapControlsHeader(
    currentMode: DigitalTwinViewMode,
    onSelectMode: (DigitalTwinViewMode) -> Unit,
    mapLayers: MapLayerFilter,
    onToggleLayer: ((MapLayerFilter) -> MapLayerFilter) -> Unit,
    onResetView: () -> Unit,
    onAddHazard: () -> Unit
) {
    Surface(
        color = ResilioNavySurface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onSelectMode(DigitalTwinViewMode.MESH_TOPOLOGY_3D) },
                    colors = ButtonDefaults.buttonColors(containerColor = ResilioCyanPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("btn_toggle_3d_engine")
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3D Topology", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilterChip(
                    selected = mapLayers.showHazards,
                    onClick = { onToggleLayer { it.copy(showHazards = !it.showHazards) } },
                    label = { Text("Hazards", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ResilioEmergencyRedDim,
                        selectedLabelColor = ResilioEmergencyRed
                    )
                )

                FilterChip(
                    selected = mapLayers.showShelters,
                    onClick = { onToggleLayer { it.copy(showShelters = !it.showShelters) } },
                    label = { Text("Shelters", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ResilioSuccessGreenDim,
                        selectedLabelColor = ResilioSuccessGreen
                    )
                )

                FilterChip(
                    selected = mapLayers.showRescueUnits,
                    onClick = { onToggleLayer { it.copy(showRescueUnits = !it.showRescueUnits) } },
                    label = { Text("Rescuers", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ResilioWarningAmberDim,
                        selectedLabelColor = ResilioWarningAmber
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAddHazard, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AddAlert, contentDescription = "Add Hazard", tint = ResilioEmergencyRed, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onResetView, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = "Reset Center", tint = ResilioCyanPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MapLegend(
    isResilio: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "TWIN TELEMETRY",
                color = ResilioCyanPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            LegendItem(color = ResilioCyanPrimary, label = if (isResilio) "ECV Gateways (4 Spread)" else "ECV Gateway (1 Centroid)")
            LegendItem(color = Color(0xFF00E676), label = "Covered Nodes (<=6 Hops)")
            LegendItem(color = Color(0xFF64748B), label = "Uncovered Nodes (>6 Hops)")
            LegendItem(color = Color(0xFF38BDF8), label = "Flood / Hazard Zone")
            LegendItem(color = ResilioWarningAmber, label = "Rescue Units En Route")
            LegendItem(color = ResilioEmergencyRed, label = "Active SOS Beacons")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = ResilioTextSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun DigitalTwinMetricsStrip(
    metrics: SimulationMetrics,
    isResilio: Boolean
) {
    Surface(
        color = ResilioNavySurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricGaugeCard(
                title = "Packet Delivery (PDR)",
                value = "${metrics.packetDeliveryRatio}",
                unit = "%",
                subtext = if (isResilio) "Resilio (+16.2% vs SCARP)" else "SCARP Baseline",
                highlightColor = if (isResilio) ResilioSuccessGreen else ResilioWarningAmber,
                modifier = Modifier.weight(1f)
            )
            MetricGaugeCard(
                title = "End-to-End Delay",
                value = "${metrics.endToEndDelayMs.toInt()}",
                unit = "ms",
                subtext = if (isResilio) "-88.9 ms reduction" else "Heavy contention",
                highlightColor = ResilioCyanPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricGaugeCard(
                title = "Coverage",
                value = "${metrics.communicationCoveragePct.toInt()}",
                unit = "%",
                subtext = "${metrics.coveredNodes}/${metrics.totalDeviceNodes} Nodes",
                highlightColor = ResilioMeshBlue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddHazardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, HazardType, Float, Float, Float, Int) -> Unit
) {
    var name by remember { mutableStateOf("Flash Inundation Sector C") }
    var selectedType by remember { mutableStateOf(HazardType.FLOOD) }
    var depthCm by remember { mutableIntStateOf(75) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ResilioNavyCard,
        title = {
            Text("Simulate Disaster Event", color = ResilioEmergencyRed, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Inject a real-time hazard into the Digital Twin to evaluate EDSS rerouting and obstacle avoidance.",
                    color = ResilioTextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Hazard Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ResilioTextPrimary,
                        unfocusedTextColor = ResilioTextPrimary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HazardType.entries.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label.substringBefore(" "), fontSize = 11.sp) }
                        )
                    }
                }
                Text("Water Depth: ${depthCm}cm", color = ResilioTextPrimary, fontSize = 12.sp)
                Slider(
                    value = depthCm.toFloat(),
                    onValueChange = { depthCm = it.toInt() },
                    valueRange = 10f..180f,
                    colors = SliderDefaults.colors(
                        thumbColor = ResilioCyanPrimary,
                        activeTrackColor = ResilioCyanPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(name, selectedType, 1150f, 1300f, 300f, depthCm)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ResilioEmergencyRed)
            ) {
                Text("Inject Hazard into Twin", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ResilioTextSecondary)
            }
        }
    )
}
