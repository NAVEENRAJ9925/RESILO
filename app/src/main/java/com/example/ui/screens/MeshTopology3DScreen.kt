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
import com.example.data.edss.NodeTelemetry
import com.example.data.model.D2DNode
import com.example.ui.engine3d.Camera3D
import com.example.ui.engine3d.MeshTopology3DEngine
import com.example.ui.engine3d.TopologyColorMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel
import kotlin.math.hypot

@Composable
fun MeshTopology3DScreen(
    viewModel: ResilioViewModel
) {
    val engine = remember { MeshTopology3DEngine() }

    val nodes by viewModel.nodes.collectAsState()
    val gateways by viewModel.gateways.collectAsState()
    val telemetryMap by viewModel.nodeTelemetryMap.collectAsState()
    val activeRoute by viewModel.activePrioritizedPath.collectAsState()
    val isResilio by viewModel.isResilioMode.collectAsState()

    // 3D Camera Orbit & Perspective state
    var yaw by remember { mutableFloatStateOf(35f) }
    var pitch by remember { mutableFloatStateOf(48f) }
    var zoom by remember { mutableFloatStateOf(0.95f) }
    var targetX by remember { mutableFloatStateOf(1100f) }
    var targetY by remember { mutableFloatStateOf(1100f) }

    // Visual Topology toggles
    var colorMode by remember { mutableStateOf(TopologyColorMode.BATTERY_LEVEL) }
    var showRangeSpheres by remember { mutableStateOf(true) }
    var showPulses by remember { mutableStateOf(true) }
    var showGroundShadows by remember { mutableStateOf(true) }
    var isTieredElevation by remember { mutableStateOf(true) }
    var selectedNodeId by remember { mutableStateOf<String?>("N-112") }

    // 60fps clock for animated link traffic pulses
    val infiniteTransition = rememberInfiniteTransition(label = "TopologyClock")
    val animTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TopoSeconds"
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
            .background(Color(0xFF060D14))
    ) {
        // 1. 3D Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        yaw = (yaw + pan.x * 0.4f) % 360f
                        pitch = (pitch - pan.y * 0.3f).coerceIn(15f, 85f)
                        zoom = (zoom * gestureZoom).coerceIn(0.45f, 3.0f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        // Cycle to next active or neighbor node
                        selectedNodeId = if (selectedNodeId == "N-112") "N-142" else "N-112"
                    }
                }
        ) {
            engine.renderTopology(
                drawScope = this,
                camera = camera,
                animTimeSec = animTime,
                nodes = nodes,
                gateways = gateways,
                telemetryMap = telemetryMap,
                activeRoute = activeRoute,
                colorMode = colorMode,
                selectedNodeId = selectedNodeId,
                showRangeSpheres = showRangeSpheres,
                showSignalPulses = showPulses,
                showGroundShadows = showGroundShadows,
                isTieredElevation = isTieredElevation
            )
        }

        // 2. Top HUD Bar
        TopologyHUDHeader(
            colorMode = colorMode,
            onColorModeChange = { colorMode = it },
            nodeCount = nodes.size,
            isResilio = isResilio,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp)
        )

        // 3. Camera Perspective Presets (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            PresetPill(label = "Tiered 3D", onClick = { yaw = 35f; pitch = 48f; zoom = 0.95f; isTieredElevation = true })
            PresetPill(label = "Planar 2D", onClick = { yaw = 0f; pitch = 85f; zoom = 0.85f; isTieredElevation = false })
            PresetPill(label = "Horizon", onClick = { yaw = 120f; pitch = 22f; zoom = 1.3f; isTieredElevation = true })
        }

        // 4. Selected Device Live Telemetry Inspector (Bottom Left)
        val selectedNode = nodes.firstOrNull { it.id == selectedNodeId } ?: nodes.firstOrNull { it.hasActiveSos }
        if (selectedNode != null) {
            val telem = telemetryMap[selectedNode.id]
            PeerTelemetryInspectorCard(
                node = selectedNode,
                telemetry = telem,
                onDropBattery = {
                    viewModel.updateTelemetry(selectedNode.id, 12, telem?.rssiStrengthDbm ?: -80, telem?.isSubmergedOrHazard ?: false)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .widthIn(max = 290.dp)
            )
        }

        // 5. Topology Visualization Layers Control Strip (Bottom Right)
        TopologyControlsStrip(
            isTiered = isTieredElevation,
            onToggleTiered = { isTieredElevation = !isTieredElevation },
            showRangeSpheres = showRangeSpheres,
            onToggleRangeSpheres = { showRangeSpheres = !showRangeSpheres },
            showPulses = showPulses,
            onTogglePulses = { showPulses = !showPulses },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .widthIn(max = 220.dp)
        )
    }
}

@Composable
private fun TopologyHUDHeader(
    colorMode: TopologyColorMode,
    onColorModeChange: (TopologyColorMode) -> Unit,
    nodeCount: Int,
    isResilio: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ResilioNavySurface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp,
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "3D MESH TOPOLOGY DIGITAL TWIN",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "$nodeCount PEER NODES · 260m D2D",
                    color = ResilioSuccessGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Color Encoding Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TopologyColorMode.entries.forEach { mode ->
                    val isSel = colorMode == mode
                    FilterChip(
                        selected = isSel,
                        onClick = { onColorModeChange(mode) },
                        label = {
                            Text(
                                text = when (mode) {
                                    TopologyColorMode.BATTERY_LEVEL -> "Battery"
                                    TopologyColorMode.HOP_DISTANCE -> "DTN Hops"
                                    TopologyColorMode.SIGNAL_RSSI -> "RSSI"
                                    TopologyColorMode.CONGESTION_LOAD -> "Load ρ"
                                },
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ResilioCyanGlow,
                            selectedLabelColor = ResilioCyanPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetPill(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = ResilioNavySurface.copy(alpha = 0.9f)),
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
private fun PeerTelemetryInspectorCard(
    node: D2DNode,
    telemetry: NodeTelemetry?,
    onDropBattery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface.copy(alpha = 0.94f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, if (node.hasActiveSos) ResilioEmergencyRed else ResilioCyanPrimary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NODE TELEMETRY: ${node.id}",
                    color = if (node.hasActiveSos) ResilioEmergencyRed else ResilioCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${telemetry?.healthScore ?: 85}/100 Score",
                    color = ResilioSuccessGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = ResilioNavyBorder)

            TopologyInfoRow("Battery Level", "${telemetry?.batteryPercent ?: node.batteryPercent}% (${if ((telemetry?.batteryPercent ?: 80) < 25) "CRITICAL" else "HEALTHY"})")
            TopologyInfoRow("Signal Quality", "${telemetry?.rssiStrengthDbm ?: -65} dBm (BLE/Wi-Fi Aware)")
            TopologyInfoRow("DTN Queue Buffer", "${telemetry?.currentQueuePackets ?: 2} packets pending")
            TopologyInfoRow("Relay Load Ratio (ρ)", "ρ = ${String.format("%.2f", telemetry?.relayLoadRatio ?: 0.12f)} (Cap: 22)")
            TopologyInfoRow("Gateway Link", "${node.nearestGatewayId ?: "ECV-Alpha"} (~${node.hopDistance} hops)")

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onDropBattery,
                colors = ButtonDefaults.buttonColors(containerColor = ResilioWarningAmber),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth().height(26.dp).testTag("btn_simulate_battery_drain")
            ) {
                Text("Simulate Battery Drain (12%)", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TopologyInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = ResilioTextMuted, fontSize = 9.sp)
        Text(text = value, color = ResilioTextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TopologyControlsStrip(
    isTiered: Boolean,
    onToggleTiered: () -> Unit,
    showRangeSpheres: Boolean,
    onToggleRangeSpheres: () -> Unit,
    showPulses: Boolean,
    onTogglePulses: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("3D TOPOLOGY LAYERS", color = ResilioCyanPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tier Elevation", color = ResilioTextSecondary, fontSize = 9.sp)
                Switch(checked = isTiered, onCheckedChange = { onToggleTiered() }, modifier = Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("260m Range Spheres", color = ResilioTextSecondary, fontSize = 9.sp)
                Switch(checked = showRangeSpheres, onCheckedChange = { onToggleRangeSpheres() }, modifier = Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Signal Packet Pulses", color = ResilioTextSecondary, fontSize = 9.sp)
                Switch(checked = showPulses, onCheckedChange = { onTogglePulses() }, modifier = Modifier.height(20.dp))
            }
        }
    }
}
