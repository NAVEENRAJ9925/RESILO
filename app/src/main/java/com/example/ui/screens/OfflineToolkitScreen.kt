package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel

@Composable
fun OfflineToolkitScreen(
    viewModel: ResilioViewModel
) {
    val shelters by viewModel.shelters.collectAsState()
    val gateways by viewModel.gateways.collectAsState()

    var isStrobeActive by remember { mutableStateOf(false) }

    // Pulsing strobe animation
    val infiniteTransition = rememberInfiniteTransition(label = "Strobe")
    val strobeFlash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "FlashIntensity"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ResilioNavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ToolkitHeader()
        }

        // 1. Emergency Optical SOS Strobe Beacon
        item {
            OpticalStrobeCard(
                isActive = isStrobeActive,
                flashValue = if (isStrobeActive) strobeFlash else 0f,
                onToggle = { isStrobeActive = !isStrobeActive }
            )
        }

        // 2. Offline Compass & Wayfinder to nearest safe haven
        item {
            OfflineCompassCard(
                closestGateway = gateways.firstOrNull()?.name ?: "ECV-Alpha",
                closestShelter = shelters.firstOrNull()?.name ?: "St. Anne High School"
            )
        }

        // 3. Offline Survival & First Aid Manual
        item {
            Text(
                text = "OFFLINE SURVIVAL & FIRST-AID PROTOCOLS",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        item {
            SurvivalProtocolCard(
                title = "Flash Flood & Inundation Trapped",
                icon = Icons.Default.WaterDrop,
                steps = listOf(
                    "Never attempt to walk or drive through flood waters exceeding 15cm (currents can sweep individuals away).",
                    "Ascend to highest accessible floor or roof terrace. Carry battery bank and bright signaling cloth.",
                    "Turn off main electrical breaker if water reaches foundation.",
                    "Resilio D2D packets will hop via neighborhood devices to notify Emergency Command Vehicles."
                )
            )
        }

        item {
            SurvivalProtocolCard(
                title = "Building Collapse & Debris Extraction",
                icon = Icons.Default.Warning,
                steps = listOf(
                    "Cover mouth and nose with fabric to filter toxic dust and particulate matter.",
                    "Tap rhythmically on pipes or structural walls (3 distinct taps) so rescue squads can localize sound.",
                    "Conserve vocal energy; avoid shouting unless hearing rescue dogs or search squads.",
                    "Do not ignite matches or lighters due to potential gas line leaks."
                )
            )
        }

        item {
            SurvivalProtocolCard(
                title = "Severe Bleeding & Shock Triage",
                icon = Icons.Default.LocalHospital,
                steps = listOf(
                    "Apply firm, direct continuous pressure on wounds using sterile cloth or clean clothing.",
                    "Elevate injured limbs above heart level if no fracture is suspected.",
                    "Keep patient warm to prevent hypothermia; cover with blankets or plastic tarps.",
                    "Note down symptoms and approximate blood loss in Resilio Medical SOS form."
                )
            )
        }

        // 4. Local DTN Bundle Store Inspector
        item {
            DTNBundleCacheCard()
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ToolkitHeader() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "OFFLINE EMERGENCY TOOLKIT",
                color = ResilioCyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Operational completely without cellular towers or internet connection. Fully bundled on-device.",
                color = ResilioTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun OpticalStrobeCard(
    isActive: Boolean,
    flashValue: Float,
    onToggle: () -> Unit
) {
    val strobeColor = if (isActive && flashValue > 0.5f) ResilioEmergencyRed else ResilioNavyCard

    Card(
        colors = CardDefaults.cardColors(containerColor = strobeColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, if (isActive) ResilioEmergencyRed else ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (isActive) Color.White else ResilioEmergencyRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OPTICAL SOS RESCUE BEACON",
                            color = if (isActive) Color.White else ResilioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive) "Pulsing High-Intensity Strobe Active" else "Screen Strobe for Aerial / Ground Search",
                            color = if (isActive) Color.White.copy(alpha = 0.8f) else ResilioTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color.White else ResilioEmergencyRed,
                        contentColor = if (isActive) ResilioEmergencyRed else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_strobe_toggle")
                ) {
                    Text(if (isActive) "STOP STROBE" else "ACTIVATE SOS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun OfflineCompassCard(
    closestGateway: String,
    closestShelter: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "OFFLINE BEARING & WAYFINDER",
                color = ResilioCyanPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Compass Dial Graphic
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ResilioNavyCard)
                        .border(1.5.dp, ResilioCyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Compass",
                        tint = ResilioCyanPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NearMe, contentDescription = null, tint = ResilioSuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Safe Shelter: $closestShelter", color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Bearing: 340° NNW · 620m (Safe Elevation)", color = ResilioTextSecondary, fontSize = 10.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, contentDescription = null, tint = ResilioCyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Command Gateway: $closestGateway", color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Bearing: 045° NE · 480m · D2D Relay Active", color = ResilioTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurvivalProtocolCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    steps: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(10.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = ResilioCyanPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, color = ResilioTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ResilioTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.forEachIndexed { i, step ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${i + 1}.",
                                color = ResilioCyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(18.dp)
                            )
                            Text(text = step, color = ResilioTextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DTNBundleCacheCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
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
                Text(
                    text = "LOCAL DTN STORE-CARRY-FORWARD BUFFER",
                    color = ResilioCyanPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(text = "4 Bundles Cached", color = ResilioSuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When new D2D peer devices come within 260m wireless range, opportunistic bundle exchanges forward queued reports automatically.",
                color = ResilioTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
