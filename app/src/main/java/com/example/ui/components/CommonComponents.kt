package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PacketStatus
import com.example.data.model.PriorityTier
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen

@Composable
fun ResilioTopAppBar(
    currentScreen: AppScreen,
    isResilioMode: Boolean,
    onToggleMode: (Boolean) -> Unit,
    activeIncidentsCount: Int
) {
    Surface(
        color = ResilioNavySurface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(ResilioCyanPrimary, ResilioNavyCard)))
                            .border(1.5.dp, ResilioCyanPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Radar Mesh",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RESILIO",
                                color = ResilioCyanPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isResilioMode) ResilioSuccessGreenDim else ResilioWarningAmberDim)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isResilioMode) "4-ECV TWIN" else "SCARP 1-ECV",
                                    color = if (isResilioMode) ResilioSuccessGreen else ResilioWarningAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "D2D Mesh · Digital Twin · EDSS",
                            color = ResilioTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Mode Switch button
                FilterChip(
                    selected = isResilioMode,
                    onClick = { onToggleMode(!isResilioMode) },
                    label = {
                        Text(
                            text = if (isResilioMode) "Resilio" else "SCARP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isResilioMode) Icons.Default.Hub else Icons.Default.AltRoute,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ResilioCyanGlow,
                        selectedLabelColor = ResilioCyanPrimary
                    ),
                    modifier = Modifier.testTag("mode_toggle_chip")
                )
            }
        }
    }
}

@Composable
fun ResilioBottomNavigation(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        containerColor = ResilioNavySurface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        AppScreen.entries.forEach { screen ->
            val isSelected = screen == currentScreen
            val icon = when (screen) {
                AppScreen.DIGITAL_TWIN -> if (isSelected) Icons.Filled.Map else Icons.Outlined.Map
                AppScreen.CITIZEN_SOS -> if (isSelected) Icons.Filled.Emergency else Icons.Outlined.Emergency
                AppScreen.EDSS_COMMAND -> if (isSelected) Icons.Filled.Psychology else Icons.Outlined.Psychology
                AppScreen.SIMULATION_EVAL -> if (isSelected) Icons.Filled.BarChart else Icons.Outlined.BarChart
                AppScreen.OFFLINE_TOOLKIT -> if (isSelected) Icons.Filled.Build else Icons.Outlined.Build
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ResilioCyanPrimary,
                    selectedTextColor = ResilioCyanPrimary,
                    unselectedIconColor = ResilioTextSecondary,
                    unselectedTextColor = ResilioTextSecondary,
                    indicatorColor = ResilioNavyCard
                ),
                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: PriorityTier) {
    val (color, bgColor) = when (priority) {
        PriorityTier.P1_CRITICAL -> ResilioEmergencyRed to ResilioEmergencyRedDim
        PriorityTier.P2_HIGH -> ResilioWarningAmber to ResilioWarningAmberDim
        PriorityTier.P3_MODERATE -> ResilioMeshBlue to Color(0x3338BDF8)
        PriorityTier.P4_LOW -> ResilioTextSecondary to Color(0x2294A3B8)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.label.substringBefore(" ("),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(status: PacketStatus) {
    val (color, bgColor) = when (status) {
        PacketStatus.QUEUED_D2D -> ResilioTextSecondary to Color(0x2294A3B8)
        PacketStatus.HOPPING_RELAY -> ResilioWarningAmber to ResilioWarningAmberDim
        PacketStatus.DELIVERED_ECV -> ResilioMeshBlue to Color(0x3338BDF8)
        PacketStatus.TRIAGED_EDSS -> ResilioGatewayPurple to Color(0x33A855F7)
        PacketStatus.DISPATCHED -> ResilioCyanPrimary to ResilioCyanGlow
        PacketStatus.RESOLVED -> ResilioSuccessGreen to ResilioSuccessGreenDim
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MetricGaugeCard(
    title: String,
    value: String,
    unit: String,
    subtext: String,
    highlightColor: Color = ResilioCyanPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = ResilioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = highlightColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        color = ResilioTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = ResilioTextMuted,
                fontSize = 10.sp
            )
        }
    }
}
