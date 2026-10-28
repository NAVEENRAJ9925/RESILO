package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.maps.GroundedPlace
import com.example.data.maps.GroundedPlaceType
import com.example.data.maps.MapsGroundingResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.DisasterRegion

@Composable
fun GoogleMapsGroundingHeaderBanner(
    groundingResult: MapsGroundingResult?,
    isLoading: Boolean,
    selectedRegion: DisasterRegion,
    presetRegions: List<DisasterRegion>,
    onSelectRegion: (DisasterRegion) -> Unit,
    onTriggerSync: (DisasterRegion) -> Unit,
    onViewPlaceDetails: (GroundedPlace) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        color = ResilioNavyCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF4285F4).copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Top Row: Brand & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Google Maps Pin Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA4335)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Google Maps Pin",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Google Maps Grounding",
                                color = ResilioTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF4285F4).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF4285F4))
                            ) {
                                Text(
                                    text = "gemini-3.8-flash",
                                    color = Color(0xFF8AB4F8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isLoading) "Querying live places via googleMaps tool..." else selectedRegion.regionLabel,
                            color = ResilioTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sync Action Button
                FilledTonalButton(
                    onClick = { onTriggerSync(selectedRegion) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("btn_sync_google_maps")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grounding...", fontSize = 11.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Maps", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Disaster Region Selector Chips
            Text(
                text = "OPERATIONAL DISASTER SECTORS",
                color = ResilioTextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetRegions.forEach { region ->
                    val isSelected = region.id == selectedRegion.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectRegion(region)
                            onTriggerSync(region)
                        },
                        label = {
                            Text(
                                text = region.name.substringBefore(" ("),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1F3A60),
                            selectedLabelColor = Color(0xFF8AB4F8),
                            containerColor = ResilioNavySurface,
                            labelColor = ResilioTextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color(0xFF4285F4) else ResilioNavyBorder
                        ),
                        modifier = Modifier.testTag("chip_region_${region.id}")
                    )
                }
            }

            // Grounded Places Horizontal Bar
            if (groundingResult != null && groundingResult.places.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "GROUNDED GOOGLE MAPS FACILITIES (${groundingResult.places.size})",
                        color = Color(0xFF34A853),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isExpanded) "Hide Details ▲" else "View All ▼",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Places Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groundingResult.places.forEach { place ->
                        GroundedPlaceMiniChip(
                            place = place,
                            onClick = { onViewPlaceDetails(place) }
                        )
                    }
                }
            }

            // Expanded Grounding Summary & Attributions
            AnimatedVisibility(visible = isExpanded && groundingResult != null) {
                groundingResult?.let { res ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ResilioNavyDark.copy(alpha = 0.7f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = res.summaryText,
                            color = ResilioTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Divider(color = ResilioNavyBorder, thickness = 0.8.dp)

                        Text(
                            text = "Google Maps Sources & CIDs:",
                            color = ResilioTextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        res.groundingSources.take(3).forEach { source ->
                            Text(
                                text = "• $source",
                                color = Color(0xFF8AB4F8),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroundedPlaceMiniChip(
    place: GroundedPlace,
    onClick: () -> Unit
) {
    val (typeColor, typeIcon) = when (place.type) {
        GroundedPlaceType.EMERGENCY_HOSPITAL, GroundedPlaceType.TRAUMA_CENTER ->
            Color(0xFFEA4335) to Icons.Default.LocalHospital
        GroundedPlaceType.RELIEF_SHELTER, GroundedPlaceType.COMMUNITY_HALL ->
            Color(0xFF34A853) to Icons.Default.Home
        GroundedPlaceType.CRITICAL_BRIDGE ->
            Color(0xFFFBBC04) to Icons.Default.Warning
        GroundedPlaceType.POLICE_STATION ->
            Color(0xFF4285F4) to Icons.Default.Security
    }

    Surface(
        color = ResilioNavySurface,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("chip_place_${place.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(12.dp)
                )
            }

            Column {
                Text(
                    text = place.name.substringBefore(" Super").substringBefore(" Safe"),
                    color = ResilioTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (place.rating != null) {
                        Text(
                            text = "★ ${place.rating}",
                            color = Color(0xFFFBBC04),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "(${place.digitalTwinX.toInt()}m, ${place.digitalTwinY.toInt()}m)",
                        color = ResilioTextTertiary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleMapsPlaceDetailModal(
    place: GroundedPlace,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val (typeColor, typeIcon, typeLabel) = when (place.type) {
        GroundedPlaceType.EMERGENCY_HOSPITAL, GroundedPlaceType.TRAUMA_CENTER ->
            Triple(Color(0xFFEA4335), Icons.Default.LocalHospital, "Hospital / Trauma Facility")
        GroundedPlaceType.RELIEF_SHELTER, GroundedPlaceType.COMMUNITY_HALL ->
            Triple(Color(0xFF34A853), Icons.Default.Home, "Evacuation Shelter / Camp")
        GroundedPlaceType.CRITICAL_BRIDGE ->
            Triple(Color(0xFFFBBC04), Icons.Default.Warning, "Arterial Bridge / Chokepoint")
        GroundedPlaceType.POLICE_STATION ->
            Triple(Color(0xFF4285F4), Icons.Default.Security, "Emergency Command Post")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ResilioNavyCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = place.name,
                        color = ResilioTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = typeLabel,
                        color = typeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Address Box
                Surface(
                    color = ResilioNavySurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFFEA4335),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = place.address,
                            color = ResilioTextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Metadata Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating
                    Surface(
                        color = ResilioNavySurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Google Rating", color = ResilioTextTertiary, fontSize = 10.sp)
                            Text(
                                text = if (place.rating != null) "★ ${place.rating} / 5.0" else "Verified",
                                color = Color(0xFFFBBC04),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Operational Capacity
                    Surface(
                        color = ResilioNavySurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                if (place.type == GroundedPlaceType.EMERGENCY_HOSPITAL) "ICU Beds" else "Capacity",
                                color = ResilioTextTertiary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (place.type == GroundedPlaceType.EMERGENCY_HOSPITAL) "${place.icuBedsAvailable} Available" else "${place.capacity} Persons",
                                color = ResilioCyanPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Digital Twin Coordinate Transform
                Surface(
                    color = ResilioNavyDark,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ResilioNavyBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "REAL-TIME DIGITAL TWIN MAPPING",
                            color = ResilioTextTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Global WGS-84:", color = ResilioTextSecondary, fontSize = 11.sp)
                            Text("${place.latitude}°N, ${place.longitude}°E", color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Twin Grid Coord:", color = ResilioTextSecondary, fontSize = 11.sp)
                            Text("X: ${place.digitalTwinX.toInt()}m, Y: ${place.digitalTwinY.toInt()}m", color = ResilioCyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openInGoogleMaps(context, place.mapsUri, place.name, place.latitude, place.longitude)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                modifier = Modifier.testTag("btn_open_google_maps")
            ) {
                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open in Google Maps", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ResilioTextSecondary)
            }
        }
    )
}

private fun openInGoogleMaps(
    context: Context,
    mapsUri: String,
    name: String,
    lat: Double,
    lng: Double
) {
    try {
        val uri = if (mapsUri.isNotBlank()) {
            Uri.parse(mapsUri)
        } else {
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(name)})")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Navigating to: $name ($lat, $lng)", Toast.LENGTH_SHORT).show()
    }
}
