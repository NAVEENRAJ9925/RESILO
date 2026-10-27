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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel

@Composable
fun CitizenEmergencyScreen(
    viewModel: ResilioViewModel
) {
    val reports by viewModel.reports.collectAsState()
    val instructions by viewModel.citizenInstructions.collectAsState()

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ReportCategory.SOS_TRAPPED) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ResilioNavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            CitizenMeshStatusBar()
        }

        item {
            Text(
                text = "ONE-TAP EMERGENCY REPORTING",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmergencyActionCard(
                    title = "SOS TRAPPED",
                    subtext = "Building / Roof",
                    icon = Icons.Default.Warning,
                    color = ResilioEmergencyRed,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_sos_trapped"),
                    onClick = {
                        selectedCategory = ReportCategory.SOS_TRAPPED
                        showReportDialog = true
                    }
                )
                EmergencyActionCard(
                    title = "MEDICAL SOS",
                    subtext = "Severe Injury",
                    icon = Icons.Default.LocalHospital,
                    color = ResilioEmergencyRed,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_medical_sos"),
                    onClick = {
                        selectedCategory = ReportCategory.MEDICAL_EMERGENCY
                        showReportDialog = true
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmergencyActionCard(
                    title = "FLOODED ROAD",
                    subtext = "Report Water Level",
                    icon = Icons.Default.WaterDrop,
                    color = ResilioMeshBlue,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_flooded_road"),
                    onClick = {
                        selectedCategory = ReportCategory.FLOODED_ROAD
                        showReportDialog = true
                    }
                )
                EmergencyActionCard(
                    title = "SHELTER / FOOD",
                    subtext = "Request Evacuation",
                    icon = Icons.Default.Home,
                    color = ResilioSuccessGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_shelter_food"),
                    onClick = {
                        selectedCategory = ReportCategory.SHELTER_REQUEST
                        showReportDialog = true
                    }
                )
            }
        }

        // Two-way Response Instructions Received from EDSS!
        item {
            Text(
                text = "EDSS RESPONSE INSTRUCTIONS (TWO-WAY RELAY)",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Instructions computed by EDSS & propagated back through D2D mesh without internet",
                color = ResilioTextMuted,
                fontSize = 10.sp
            )
        }

        if (instructions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkChatRead,
                            contentDescription = null,
                            tint = ResilioTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Response Instructions Yet",
                            color = ResilioTextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Submit an emergency report to receive automated triage, safe evacuation corridors, and rescue dispatch confirmation.",
                            color = ResilioTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(instructions) { instruction ->
                InstructionCard(instruction = instruction)
            }
        }

        // Active Emergency Reports Status
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "D2D PACKET DISPATCH & RELAY STATUS",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        items(reports) { report ->
            CitizenReportStatusCard(report = report)
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showReportDialog) {
        SubmitReportDialog(
            category = selectedCategory,
            onDismiss = { showReportDialog = false },
            onSubmit = { desc, name, injured, depth ->
                viewModel.submitReport(
                    category = selectedCategory,
                    description = desc,
                    victimName = name,
                    injuredCount = injured,
                    waterDepthCm = depth
                )
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun CitizenMeshStatusBar() {
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
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ResilioSuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "INFRASTRUCTURE-LESS D2D MESH",
                        color = ResilioSuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "NO CELLULAR / NO INTERNET",
                    color = ResilioTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MeshInfoItem(label = "Local Node ID", value = "NODE-USR-01")
                MeshInfoItem(label = "D2D Peers Nearby", value = "4 Active (<=260m)")
                MeshInfoItem(label = "Nearest ECV Gateway", value = "ECV-Alpha (~2 hops)")
                MeshInfoItem(label = "Protocol", value = "SCARP DTN (TTL 6)")
            }
        }
    }
}

@Composable
private fun MeshInfoItem(label: String, value: String) {
    Column {
        Text(text = label, color = ResilioTextMuted, fontSize = 9.sp)
        Text(text = value, color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmergencyActionCard(
    title: String,
    subtext: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = ResilioTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtext,
                color = ResilioTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun InstructionCard(instruction: CitizenInstruction) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, ResilioCyanPrimary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ResilioSuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = instruction.title,
                        color = ResilioCyanPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (instruction.etaMinutes > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ResilioCyanGlow)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ETA: ${instruction.etaMinutes} MIN",
                            color = ResilioCyanPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = instruction.instruction,
                color = ResilioTextPrimary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ResilioNavyBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = ResilioWarningAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SAFE ROUTE: ${instruction.safeRouteSummary}",
                    color = ResilioWarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = ResilioMeshBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SHELTER DESTINATION: ${instruction.recommendedShelterName}",
                    color = ResilioMeshBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CitizenReportStatusCard(report: EmergencyReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriorityBadge(priority = report.priority)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.category.displayName,
                        color = ResilioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                StatusBadge(status = report.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.description,
                color = ResilioTextSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hop progression indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = ResilioCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DTN Hop: ${report.hopCount}/${report.maxTtl}",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Path: ${report.relayPath.joinToString(" → ")}",
                        color = ResilioTextMuted,
                        fontSize = 10.sp
                    )
                }

                if (report.waterDepthCm > 0) {
                    Text(
                        text = "Water: ${report.waterDepthCm}cm",
                        color = ResilioMeshBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitReportDialog(
    category: ReportCategory,
    onDismiss: () -> Unit,
    onSubmit: (description: String, victimName: String, injuredCount: Int, waterDepthCm: Int) -> Unit
) {
    var description by remember {
        mutableStateOf(
            when (category) {
                ReportCategory.SOS_TRAPPED -> "Trapped on 2nd floor, water rising at staircase. Need rescue boat."
                ReportCategory.MEDICAL_EMERGENCY -> "Elderly person unconscious, severe head laceration."
                ReportCategory.FLOODED_ROAD -> "Submerged road, current too strong for wading."
                ReportCategory.SHELTER_REQUEST -> "Family of 4 requesting emergency shelter and drinking water."
                ReportCategory.COLLAPSED_STRUCTURE -> "Wall collapsed blocking alleyway exit."
            }
        )
    }
    var victimName by remember { mutableStateOf("Naveenraj & Residents") }
    var injuredCount by remember { mutableIntStateOf(if (category == ReportCategory.MEDICAL_EMERGENCY) 2 else 0) }
    var waterDepthCm by remember { mutableIntStateOf(if (category == ReportCategory.FLOODED_ROAD) 85 else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ResilioNavyCard,
        title = {
            Text(
                text = "SUBMIT ${category.displayName.uppercase()}",
                color = ResilioCyanPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "This report will propagate via D2D multi-hop mesh to the nearest Emergency Command Vehicle (ECV) and synchronize with the Digital Twin.",
                    color = ResilioTextSecondary,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = victimName,
                    onValueChange = { victimName = it },
                    label = { Text("Victim / Reporting Alias") },
                    modifier = Modifier.fillMaxWidth().testTag("input_victim_name")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Situation Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_description")
                )

                if (category == ReportCategory.MEDICAL_EMERGENCY || category == ReportCategory.SOS_TRAPPED) {
                    Text("Number of Injured / Trapped Persons: $injuredCount", color = ResilioTextPrimary, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5).forEach { count ->
                            FilterChip(
                                selected = injuredCount == count,
                                onClick = { injuredCount = count },
                                label = { Text("$count", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (category == ReportCategory.FLOODED_ROAD || category == ReportCategory.SOS_TRAPPED) {
                    Text("Estimated Water Depth: ${waterDepthCm}cm", color = ResilioTextPrimary, fontSize = 12.sp)
                    Slider(
                        value = waterDepthCm.toFloat(),
                        onValueChange = { waterDepthCm = it.toInt() },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = ResilioCyanPrimary,
                            activeTrackColor = ResilioCyanPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(description, victimName, injuredCount, waterDepthCm)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ResilioEmergencyRed),
                modifier = Modifier.testTag("btn_submit_report")
            ) {
                Text("Transmit via D2D Mesh", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ResilioTextSecondary)
            }
        }
    )
}
