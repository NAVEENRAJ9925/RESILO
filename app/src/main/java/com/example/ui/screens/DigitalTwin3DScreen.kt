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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.engine3d.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel
import kotlin.math.hypot

@Composable
fun DigitalTwin3DScreen(
    viewModel: ResilioViewModel
) {
    val engine = remember { DigitalTwin3DEngine() }

    val gateways by viewModel.gateways.collectAsState()
    val hazards by viewModel.hazards.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val nodes by viewModel.nodes.collectAsState()
    val isResilio by viewModel.isResilioMode.collectAsState()
    val activePath by viewModel.activePrioritizedPath.collectAsState()

    // Camera 3D state
    var yaw by remember { mutableFloatStateOf(45f) }
    var pitch by remember { mutableFloatStateOf(52f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var targetX by remember { mutableFloatStateOf(1100f) }
    var targetY by remember { mutableFloatStateOf(1100f) }

    // Simulation states
    var waterRiseCm by remember { mutableFloatStateOf(0f) }
    var themeMode by remember { mutableStateOf(VisualThemeMode.TACTICAL_3D) }
    var showDomes by remember { mutableStateOf(true) }
    var showArcs by remember { mutableStateOf(true) }
    var selectedNodeId by remember { mutableStateOf<String?>("N-112") }

    // 60fps continuous animation timer
    val infiniteTransition = rememberInfiniteTransition(label = "3DEngineClock")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "EngineSeconds"
    )

    val camera = remember(yaw, pitch, zoom, targetX, targetY) {
        Camera3D(
            yawDeg = yaw,
            pitchDeg = pitch,
            zoom = zoom,
            targetX = targetX,
            targetY = targetY
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (themeMode) {
                    VisualThemeMode.TACTICAL_3D -> ResilioNavyDark
                    VisualThemeMode.THERMAL_FLIR -> Color(0xFF070014)
                    VisualThemeMode.CYBER_TWIN -> Color(0xFF030B10)
                }
            )
    ) {
        // 1. High-Performance 3D Render Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        // Orbit Yaw & Pitch rotation via Drag
                        yaw = (yaw + pan.x * 0.4f) % 360f
                        pitch = (pitch - pan.y * 0.3f).coerceIn(20f, 85f)
                        zoom = (zoom * gestureZoom).coerceIn(0.45f, 2.8f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        // Toggle closest node selection
                        selectedNodeId = if (selectedNodeId == null) "N-112" else null
                    }
                }
        ) {
            engine.renderScene(
                drawScope = this,
                camera = camera,
                animTimeSec = animTime,
                waterLevelRiseCm = waterRiseCm,
                gateways = gateways,
                hazards = hazards,
                resources = resources,
                reports = reports,
                nodes = nodes,
                themeMode = themeMode,
                selectedNodeId = selectedNodeId,
                showCoverageDomes = showDomes,
                showPacketArcs = showArcs,
                prioritizedPath = activePath
            )
        }

        // 2. Top Tactical HUD Bar
        HUDHeaderBar(
            themeMode = themeMode,
            onThemeChange = { themeMode = it },
            isResilio = isResilio,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        )

        // 3. Camera Preset Pills
        CameraControlsPills(
            currentYaw = yaw,
            currentPitch = pitch,
            onSelectPreset = { pYaw, pPitch, pZoom ->
                yaw = pYaw
                pitch = pPitch
                zoom = pZoom
                targetX = 1100f
                targetY = 1100f
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 12.dp)
        )

        // 4. Live Device Telemetry Card (Bottom Left)
        val selectedNode = nodes.firstOrNull { it.id == selectedNodeId } ?: nodes.firstOrNull { it.hasActiveSos }
        if (selectedNode != null) {
            DeviceTelemetryCard(
                node = selectedNode,
                groundElevation = engine.getElevation(selectedNode.x, selectedNode.y),
                waterRiseCm = waterRiseCm,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            )
        }

        // 5. Interactive Simulation Drawer (Bottom Right)
        WaterRiseSimulationCard(
            waterRiseCm = waterRiseCm,
            onWaterRiseChange = { waterRiseCm = it },
            showDomes = showDomes,
            onToggleDomes = { showDomes = !showDomes },
            showArcs = showArcs,
            onToggleArcs = { showArcs = !showArcs },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .widthIn(max = 240.dp)
        )
    }
}

@Composable
private fun HUDHeaderBar(
    themeMode: VisualThemeMode,
    onThemeChange: (VisualThemeMode) -> Unit,
    isResilio: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ResilioNavySurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ResilioCyanPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "3D DIGITAL TWIN · TELEMETRY ENGINE",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isResilio) "4 ECV Gateways · Real-Time Terrain & Waves" else "SCARP 1-Gateway Baseline",
                        color = ResilioTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Visual Theme Mode Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                VisualThemeMode.entries.forEach { mode ->
                    val isSel = themeMode == mode
                    FilterChip(
                        selected = isSel,
                        onClick = { onThemeChange(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    VisualThemeMode.TACTICAL_3D -> "Tactical"
                                    VisualThemeMode.THERMAL_FLIR -> "FLIR"
                                    VisualThemeMode.CYBER_TWIN -> "Holo"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ResilioCyanGlow,
                            selectedLabelColor = ResilioCyanPrimary
                        ),
                        modifier = Modifier.testTag("filter_theme_${mode.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraControlsPills(
    currentYaw: Float,
    currentPitch: Float,
    onSelectPreset: (Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.End
    ) {
        CameraPillButton(label = "Isometric 3D", onClick = { onSelectPreset(45f, 55f, 1.0f) })
        CameraPillButton(label = "Top Ortho", onClick = { onSelectPreset(0f, 85f, 0.9f) })
        CameraPillButton(label = "Low Horizon", onClick = { onSelectPreset(135f, 28f, 1.35f) })
    }
}

@Composable
private fun CameraPillButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ResilioNavySurface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(8.dp))
            .height(28.dp)
    ) {
        Text(text = label, color = ResilioCyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DeviceTelemetryCard(
    node: D2DNode,
    groundElevation: Float,
    waterRiseCm: Float,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, if (node.hasActiveSos) ResilioEmergencyRed else ResilioNavyBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRY: ${node.id}",
                    color = if (node.hasActiveSos) ResilioEmergencyRed else ResilioCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                if (node.hasActiveSos) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ResilioEmergencyRedDim)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("ACTIVE SOS", color = ResilioEmergencyRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = ResilioNavyBorder)

            TelemetryRow(label = "Position (X, Y, Z)", value = "[${node.x.toInt()}m, ${node.y.toInt()}m, +${groundElevation.toInt()}m]")
            TelemetryRow(label = "Battery Reserve", value = "${node.batteryPercent}% Li-Po")
            TelemetryRow(label = "Hop Distance to Gateway", value = "${node.hopDistance} Hops (${node.nearestGatewayId ?: "ECV-Alpha"})")
            TelemetryRow(label = "Signal RSSI (BLE/Wi-Fi)", value = "-${(65 + node.hopDistance * 5)} dBm")
            TelemetryRow(label = "Water Level at Sensor", value = "+${(waterRiseCm).toInt()} cm")
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = ResilioTextMuted, fontSize = 9.sp)
        Text(text = value, color = ResilioTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WaterRiseSimulationCard(
    waterRiseCm: Float,
    onWaterRiseChange: (Float) -> Unit,
    showDomes: Boolean,
    onToggleDomes: () -> Unit,
    showArcs: Boolean,
    onToggleArcs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "WATER RISE SIMULATOR: +${waterRiseCm.toInt()}cm",
                color = ResilioMeshBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = waterRiseCm,
                onValueChange = onWaterRiseChange,
                valueRange = 0f..150f,
                colors = SliderDefaults.colors(
                    thumbColor = ResilioMeshBlue,
                    activeTrackColor = ResilioMeshBlue
                ),
                modifier = Modifier.height(24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Radar Domes", color = ResilioTextSecondary, fontSize = 9.sp)
                Switch(
                    checked = showDomes,
                    onCheckedChange = { onToggleDomes() },
                    modifier = Modifier.height(20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("3D Packet Arcs", color = ResilioTextSecondary, fontSize = 9.sp)
                Switch(
                    checked = showArcs,
                    onCheckedChange = { onToggleArcs() },
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}
