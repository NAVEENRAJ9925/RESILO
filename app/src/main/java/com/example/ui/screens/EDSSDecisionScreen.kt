package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.edss.*
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel

@Composable
fun EDSSDecisionScreen(
    viewModel: ResilioViewModel
) {
    val reports by viewModel.reports.collectAsState()
    val edssDecisions by viewModel.edssDecisions.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val shelters by viewModel.shelters.collectAsState()
    val isResilio by viewModel.isResilioMode.collectAsState()
    val groundedMapsResult by viewModel.groundedMapsResult.collectAsState()

    val activePath by viewModel.activePrioritizedPath.collectAsState()
    val selectedPolicy by viewModel.selectedRoutingPolicy.collectAsState()

    var showBroadcastDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ResilioNavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            EDSSHeader(
                isResilio = isResilio,
                onBroadcastClick = { showBroadcastDialog = true }
            )
        }

        // Google Maps Grounding EDSS Facility Sync Strip
        item {
            Surface(
                color = ResilioNavyCard,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA4335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Google Maps Real-Time Grounding",
                                color = ResilioTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF4285F4).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "gemini-3.8-flash",
                                    color = Color(0xFF8AB4F8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "EDSS routes and citizen triage instructions are dynamically grounded with real-world Google Maps facilities (${groundedMapsResult?.places?.size ?: 0} facilities verified).",
                            color = ResilioTextSecondary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 5-Stage EDSS Pipeline Workflow Visualizer (Fig. 3)
        item {
            EDSSPipelineWorkflowCard()
        }

        // Telemetry-Driven Message Routing Path Optimizer
        item {
            EDSSMeshRoutingOptimizerCard(
                activePath = activePath,
                selectedPolicy = selectedPolicy,
                onSelectPolicy = { viewModel.setRoutingPolicy(it) },
                onSimulateNodeTelemetry = { nodeId, batt, rssi, hazard ->
                    viewModel.updateTelemetry(nodeId, batt, rssi, hazard)
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE EDSS DECISION QUEUE (${edssDecisions.size})",
                    color = ResilioCyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Decision Latency: 3-8 min",
                    color = ResilioSuccessGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (edssDecisions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Pending Decisions",
                            color = ResilioTextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "New citizen SOS messages arriving via D2D multi-hop will be triaged automatically.",
                            color = ResilioTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(edssDecisions) { decision ->
                val associatedReport = reports.firstOrNull { it.id == decision.reportId }
                EDSSDecisionCard(
                    decision = decision,
                    report = associatedReport,
                    onForceRecompute = { associatedReport?.let { viewModel.forceTriage(it) } }
                )
            }
        }

        // Available Resources Pool Status
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "EMERGENCY RESCUE RESOURCE INVENTORY",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        items(resources) { res ->
            RescueResourceStatusRow(resource = res)
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showBroadcastDialog) {
        EmergencyBroadcastDialog(
            shelters = shelters,
            onDismiss = { showBroadcastDialog = false },
            onSend = { title, message, shelter ->
                viewModel.sendBroadcastAlert(title, message, shelter)
                showBroadcastDialog = false
            }
        )
    }
}

@Composable
private fun EDSSHeader(
    isResilio: Boolean,
    onBroadcastClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EMERGENCY DECISION SUPPORT SYSTEM",
                        color = ResilioCyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isResilio) "Automated Triage & Digital-Twin Safe Routing" else "Manual Triage (SCARP Baseline)",
                        color = ResilioTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = onBroadcastClick,
                    colors = ButtonDefaults.buttonColors(containerColor = ResilioEmergencyRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_broadcast_mesh")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Broadcast", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EDSSPipelineWorkflowCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EDSS 5-STAGE WORKFLOW (IEEE FIGURE 3)",
                color = ResilioCyanPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            val stages = listOf(
                "1. Situation Analysis" to "Damage Assessment · Network Status · Human Reports",
                "2. Priority Classification" to "Criticality Scoring (0-100) · Triage Levels P1-P4",
                "3. Resource Allocation Engine" to "Match Rescue Boats, Ambulances, NDRF Teams",
                "4. Route Optimization" to "Dijkstra Safe Corridor · Avoid Flood Hazard · Shelters",
                "5. Coordination & Two-Way Relay" to "Relay Instructions down D2D Mesh to Victims"
            )

            stages.forEachIndexed { idx, (title, sub) ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(ResilioCyanGlow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${idx + 1}", color = ResilioCyanPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = title, color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = sub, color = ResilioTextMuted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EDSSDecisionCard(
    decision: EDSSDecision,
    report: EmergencyReport?,
    onForceRecompute: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(priority = decision.priorityTier)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = decision.victimName,
                        color = ResilioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Criticality Score Ring / Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ResilioEmergencyRedDim)
                        .border(1.dp, ResilioEmergencyRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Score: ${decision.priorityScore.toInt()}/100",
                        color = ResilioEmergencyRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            if (report != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = report.description,
                    color = ResilioTextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ResilioNavyBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Resource Recommendation
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = ResilioWarningAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ALLOCATED ASSET: ${decision.recommendedResource.label}",
                    color = ResilioWarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AltRoute,
                    contentDescription = null,
                    tint = ResilioCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = decision.safeRouteSummary,
                    color = ResilioTextPrimary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PinDrop,
                    contentDescription = null,
                    tint = ResilioSuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DESTINATION: ${decision.assignedShelterOrHospital}",
                    color = ResilioSuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Citizen Relay instruction preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ResilioNavyCard)
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "MESH RELAY INSTRUCTION (SENT TO CITIZEN):",
                        color = ResilioTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "\"${decision.citizenInstruction}\"",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
private fun RescueResourceStatusRow(resource: RescueResource) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (resource.type) {
                        ResourceType.AMBULANCE -> Icons.Default.LocalHospital
                        ResourceType.RESCUE_BOAT -> Icons.Default.Sailing
                        ResourceType.NDRF_SQUAD -> Icons.Default.Shield
                        ResourceType.FIRE_ENGINE -> Icons.Default.FireTruck
                        ResourceType.SUPPLY_TRUCK -> Icons.Default.LocalShipping
                    },
                    contentDescription = null,
                    tint = ResilioCyanPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = resource.name, color = ResilioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Speed: ${resource.speedKmh.toInt()} km/h · Safe Path Navigation", color = ResilioTextMuted, fontSize = 10.sp)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (resource.status) {
                            ResourceStatus.IDLE -> ResilioSuccessGreenDim
                            ResourceStatus.DISPATCHED -> ResilioWarningAmberDim
                            ResourceStatus.ON_SCENE -> ResilioEmergencyRedDim
                            ResourceStatus.RETURNING -> ResilioNavyCard
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = resource.status.label.substringBefore(" ("),
                    color = when (resource.status) {
                        ResourceStatus.IDLE -> ResilioSuccessGreen
                        ResourceStatus.DISPATCHED -> ResilioWarningAmber
                        ResourceStatus.ON_SCENE -> ResilioEmergencyRed
                        ResourceStatus.RETURNING -> ResilioTextSecondary
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmergencyBroadcastDialog(
    shelters: List<Shelter>,
    onDismiss: () -> Unit,
    onSend: (title: String, message: String, shelterName: String) -> Unit
) {
    var title by remember { mutableStateOf("Flash Flood & Dam Discharge Alert") }
    var message by remember { mutableStateOf("Dam sluice gates opening in 30 mins. Evacuate lower sectors immediately to high ground.") }
    var selectedShelter by remember { mutableStateOf(shelters.firstOrNull()?.name ?: "St. Anne High School") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ResilioNavyCard,
        title = {
            Text("Broadcast Over D2D Mesh", color = ResilioEmergencyRed, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This emergency directive will be propagated from all 4 ECV Gateways through multi-hop D2D mesh to all 260 citizen nodes.",
                    color = ResilioTextSecondary,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alert Title") },
                    modifier = Modifier.fillMaxWidth().testTag("input_broadcast_title")
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Broadcast Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_broadcast_msg")
                )
                Text("Designated Safe Evacuation Destination:", color = ResilioTextPrimary, fontSize = 11.sp)
                shelters.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedShelter = s.name }
                    ) {
                        RadioButton(
                            selected = selectedShelter == s.name,
                            onClick = { selectedShelter = s.name },
                            colors = RadioButtonDefaults.colors(selectedColor = ResilioCyanPrimary)
                        )
                        Text(text = s.name, color = ResilioTextPrimary, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, message, selectedShelter) },
                colors = ButtonDefaults.buttonColors(containerColor = ResilioEmergencyRed),
                modifier = Modifier.testTag("btn_confirm_broadcast")
            ) {
                Text("Transmit Broadcast", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ResilioTextSecondary)
            }
        }
    )
}

@Composable
fun EDSSMeshRoutingOptimizerCard(
    activePath: PrioritizedMeshPath?,
    selectedPolicy: RoutingPolicy,
    onSelectPolicy: (RoutingPolicy) -> Unit,
    onSimulateNodeTelemetry: (nodeId: String, battery: Int, rssi: Int, isHazard: Boolean) -> Unit
) {
    var showSimulator by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, ResilioCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        text = "TELEMETRIC ROUTING OPTIMIZER",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                TextButton(
                    onClick = { showSimulator = !showSimulator },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (showSimulator) Icons.Default.Tune else Icons.Default.Tune,
                        contentDescription = null,
                        tint = ResilioWarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showSimulator) "Hide Lab" else "Telemetry Lab",
                        color = ResilioWarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Analyzes device battery %, RSSI signal & flood hazard maps to route packets around bottlenecks and dying phones.",
                color = ResilioTextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Routing Policy Selector Chips
            Text(
                text = "ACTIVE EDSS ROUTING POLICY:",
                color = ResilioTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoutingPolicy.entries.take(3).forEach { policy ->
                    val isSel = selectedPolicy == policy
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelectPolicy(policy) },
                        label = {
                            Text(
                                text = when (policy) {
                                    RoutingPolicy.BALANCED_RESILIO -> "Adaptive"
                                    RoutingPolicy.CRITICAL_LOW_LATENCY -> "P1 Speed"
                                    RoutingPolicy.BATTERY_CONSERVATION -> "Battery Guard"
                                    RoutingPolicy.HAZARD_IMMUNE -> "Hazard Bypass"
                                    RoutingPolicy.NAIVE_SHORTEST_HOP -> "Naive SCARP"
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

            Spacer(modifier = Modifier.height(10.dp))

            // Active Path Telemetric Metrics
            if (activePath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PathMetricBox(
                        label = "End Delay",
                        value = "${activePath.totalEstimatedDelayMs.toInt()} ms",
                        color = ResilioCyanPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    PathMetricBox(
                        label = "Delivery PDR",
                        value = "${activePath.pathReliabilityPct.toInt()}%",
                        color = ResilioSuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PathMetricBox(
                        label = "Avg Battery",
                        value = "${activePath.averageBatteryLevelPct}%",
                        color = if (activePath.averageBatteryLevelPct > 40) ResilioSuccessGreen else ResilioWarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    PathMetricBox(
                        label = "Hazard Zone",
                        value = if (activePath.hazardAvoided) "Clean" else "In Danger",
                        color = if (activePath.hazardAvoided) ResilioSuccessGreen else ResilioEmergencyRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hop-by-Hop Breakdown
                Text(
                    text = "OPTIMIZED TELEMETRY HOPS (${activePath.hops.size} HOPS TO ${activePath.targetGatewayId}):",
                    color = ResilioCyanPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                activePath.hops.forEachIndexed { idx, hop ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ResilioNavyCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Hop ${idx + 1}: ${hop.fromNodeId} → ${hop.toNodeId}",
                                color = ResilioTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${hop.distanceMeters.toInt()}m",
                                color = ResilioTextMuted,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Signal RSSI
                            Text(
                                text = "${hop.linkRssiDbm} dBm",
                                color = if (hop.linkRssiDbm > -75) ResilioSuccessGreen else ResilioWarningAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Battery
                            Text(
                                text = "${hop.targetBattery}%",
                                color = if (hop.targetBattery > 35) ResilioCyanPrimary else ResilioEmergencyRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Interactive Telemetry Experimenter Lab
            AnimatedVisibility(visible = showSimulator) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF09141D))
                        .border(1.dp, ResilioWarningAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    val candidateNodeId = activePath?.hops?.firstOrNull()?.toNodeId ?: "N-142"

                    var simBatt by remember { mutableIntStateOf(18) }
                    var simRssi by remember { mutableIntStateOf(-89) }
                    var simHazard by remember { mutableStateOf(true) }

                    Text(
                        text = "LIVE TELEMETRY FAULT INJECTOR",
                        color = ResilioWarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Simulate degraded telemetry on relay node '$candidateNodeId' and observe the EDSS autonomously steer packets around it.",
                        color = ResilioTextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Relay Battery: $simBatt% (Low Battery Fault)", color = ResilioTextPrimary, fontSize = 10.sp)
                    Slider(
                        value = simBatt.toFloat(),
                        onValueChange = { simBatt = it.toInt() },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(thumbColor = ResilioWarningAmber, activeTrackColor = ResilioWarningAmber)
                    )

                    Text("Signal Strength: $simRssi dBm (Fading Channel)", color = ResilioTextPrimary, fontSize = 10.sp)
                    Slider(
                        value = simRssi.toFloat(),
                        onValueChange = { simRssi = it.toInt() },
                        valueRange = -95f..-45f,
                        colors = SliderDefaults.colors(thumbColor = ResilioCyanPrimary, activeTrackColor = ResilioCyanPrimary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Submerged in Flood Hazard", color = ResilioTextPrimary, fontSize = 11.sp)
                        Switch(
                            checked = simHazard,
                            onCheckedChange = { simHazard = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            onSimulateNodeTelemetry(candidateNodeId, simBatt, simRssi, simHazard)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ResilioWarningAmber),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_inject_telemetry")
                    ) {
                        Text("Apply Telemetry & Re-Route", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PathMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ResilioNavyCard)
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(6.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = ResilioTextMuted, fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}
