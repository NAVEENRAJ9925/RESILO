package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.ui.components.MetricGaugeCard
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.MapLayerFilter
import com.example.ui.viewmodel.ResilioViewModel
import kotlin.math.hypot

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

    var is3DMode by remember { mutableStateOf(true) }
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
        if (is3DMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                DigitalTwin3DScreen(viewModel = viewModel)

                // Floating 2D Switch Button
                FloatingActionButton(
                    onClick = { is3DMode = false },
                    containerColor = ResilioNavySurface,
                    contentColor = ResilioCyanPrimary,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .height(38.dp)
                        .border(1.dp, ResilioCyanPrimary, RoundedCornerShape(12.dp))
                        .testTag("switch_to_2d_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch to 2D Ortho Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Layer filters and status bar with 3D switch
                MapControlsHeader(
                    is3DMode = is3DMode,
                    onToggle3D = { is3DMode = true },
                    mapLayers = mapLayers,
                    onToggleLayer = { viewModel.toggleLayer(it) },
                    onResetView = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onAddHazard = { showHazardDialog = true }
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
                    .pointerInput(gateways, hazards, shelters, reports) {
                        detectTapGestures { tapOffset ->
                            // Find nearest object
                            // Canvas transforms: coordinate (x, y) in meters [0, 2200] maps to canvas pixel
                            // For simplicity, selection can check nearby entities
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

    if (showHazardDialog) {
        AddHazardDialog(
            onDismiss = { showHazardDialog = false },
            onConfirm = { name, type, x, y, radius, depth ->
                viewModel.injectDisasterScenario(name, type, x, y, radius, depth)
                showHazardDialog = false
            }
        )
    }
}

@Composable
private fun MapControlsHeader(
    is3DMode: Boolean,
    onToggle3D: () -> Unit,
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
                    onClick = onToggle3D,
                    colors = ButtonDefaults.buttonColors(containerColor = ResilioCyanPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp).testTag("btn_toggle_3d_engine")
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3D Engine", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
