package com.example.data.maps

enum class GroundedPlaceType(val displayName: String) {
    EMERGENCY_HOSPITAL("Emergency Hospital"),
    TRAUMA_CENTER("Trauma Center"),
    RELIEF_SHELTER("Relief Shelter"),
    COMMUNITY_HALL("Community Safe Zone"),
    CRITICAL_BRIDGE("Bridge / Bottleneck"),
    POLICE_STATION("Command Post")
}

data class GroundedPlace(
    val id: String,
    val name: String,
    val type: GroundedPlaceType,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val mapsUri: String,
    val rating: Float? = null,
    val isOpenNow: Boolean = true,
    val digitalTwinX: Float = 0f, // Mapped to 0..2200m space
    val digitalTwinY: Float = 0f,
    val capacity: Int = 300,
    val icuBedsAvailable: Int = 10
)

data class MapsGroundingResult(
    val areaName: String,
    val centerLat: Double,
    val centerLng: Double,
    val summaryText: String,
    val places: List<GroundedPlace>,
    val groundingSources: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val isLiveGrounded: Boolean = true
)
