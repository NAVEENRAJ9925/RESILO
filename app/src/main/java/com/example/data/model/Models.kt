package com.example.data.model

enum class ReportCategory(val displayName: String, val iconName: String) {
    SOS_TRAPPED("SOS Trapped", "Warning"),
    MEDICAL_EMERGENCY("Medical Emergency", "LocalHospital"),
    FLOODED_ROAD("Flooded Road", "WaterDrop"),
    SHELTER_REQUEST("Shelter / Supplies", "Home"),
    COLLAPSED_STRUCTURE("Building Collapse", "DomainDisabled")
}

enum class PriorityTier(val label: String, val weight: Float) {
    P1_CRITICAL("P1 - Critical (Immediate Danger)", 1.0f),
    P2_HIGH("P2 - High (Urgent Medical/Rising Water)", 0.75f),
    P3_MODERATE("P3 - Moderate (Trapped/Shelter Need)", 0.50f),
    P4_LOW("P4 - Low (Informational/Check-in)", 0.25f)
}

enum class PacketStatus(val label: String) {
    QUEUED_D2D("Queued in D2D Buffer"),
    HOPPING_RELAY("Hopping via Mesh Relays"),
    DELIVERED_ECV("Delivered to Gateway ECV"),
    TRIAGED_EDSS("Triaged by EDSS"),
    DISPATCHED("Rescue Resource Dispatched"),
    RESOLVED("Assisted / Resolved")
}

enum class HazardType(val label: String) {
    FLOOD("Flood Water"),
    COLLAPSED_BRIDGE("Collapsed Bridge / Debris"),
    FIRE_SMOKE("Fire / Toxic Smoke"),
    ROAD_BLOCKED("Road Impassable")
}

enum class Passability(val label: String) {
    IMPASSABLE("Impassable (All Vehicles)"),
    BOAT_ONLY("Watercraft / Boat Only"),
    HIGH_CLEARANCE("High-Clearance Trucks Only"),
    CLEAR("Passable")
}

enum class ResourceType(val label: String) {
    AMBULANCE("Rapid Ambulance"),
    RESCUE_BOAT("Inflatable Rescue Boat"),
    NDRF_SQUAD("Disaster Rescue Squad (NDRF)"),
    FIRE_ENGINE("Fire Rescue Unit"),
    SUPPLY_TRUCK("Emergency Food & Water Truck")
}

enum class ResourceStatus(val label: String) {
    IDLE("Stationed & Ready"),
    DISPATCHED("En Route (Safe Navigation)"),
    ON_SCENE("On Scene Conducting Triage"),
    RETURNING("Returning to Command Base")
}

data class EmergencyReport(
    val id: String,
    val category: ReportCategory,
    val priority: PriorityTier,
    val victimName: String,
    val description: String,
    val x: Float, // coordinate in meters (0 to 2200)
    val y: Float, // coordinate in meters (0 to 2200)
    val timestamp: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 78,
    val waterDepthCm: Int = 0,
    val injuredCount: Int = 1,
    val status: PacketStatus = PacketStatus.QUEUED_D2D,
    val hopCount: Int = 0,
    val maxTtl: Int = 6,
    val relayPath: List<String> = emptyList(),
    val assignedResourceId: String? = null,
    val edssResponse: String? = null,
    val nearestEcvId: String? = null
)

data class ECVGateway(
    val id: String,
    val name: String,
    val callsign: String,
    val x: Float,
    val y: Float,
    val coverageRadiusMeters: Float = 480f,
    val packetCount: Int = 0,
    val relayLoad: Float = 0.15f,
    val batteryPercent: Int = 94,
    val isOnline: Boolean = true
)

data class HazardZone(
    val id: String,
    val name: String,
    val type: HazardType,
    val x: Float,
    val y: Float,
    val radiusMeters: Float,
    val waterDepthCm: Int,
    val passability: Passability
)

data class Shelter(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val capacity: Int,
    val occupied: Int,
    val foodDays: Int,
    val waterLiters: Int,
    val medicalSupport: Boolean = true
) {
    val occupancyPercent: Int get() = if (capacity > 0) ((occupied.toFloat() / capacity) * 100).toInt() else 0
    val isFull: Boolean get() = occupied >= capacity
}

data class Hospital(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val icuBedsAvailable: Int,
    val totalBeds: Int,
    val traumaReady: Boolean = true,
    val emergencyGeneratorOnline: Boolean = true
)

data class RescueResource(
    val id: String,
    val name: String,
    val type: ResourceType,
    var x: Float,
    var y: Float,
    var targetX: Float? = null,
    var targetY: Float? = null,
    val status: ResourceStatus = ResourceStatus.IDLE,
    val assignedReportId: String? = null,
    val speedKmh: Float = 16f,
    val etaMinutes: Int = 0
)

data class D2DNode(
    val id: String,
    val x: Float,
    val y: Float,
    val isCovered: Boolean,
    val hopDistance: Int,
    val nearestGatewayId: String?,
    val hasActiveSos: Boolean = false,
    val batteryPercent: Int = 85
)

data class EDSSDecision(
    val id: String,
    val reportId: String,
    val victimName: String,
    val priorityScore: Float, // 0..100
    val priorityTier: PriorityTier,
    val recommendedResource: ResourceType,
    val assignedResourceId: String,
    val safeRouteSummary: String,
    val assignedShelterOrHospital: String,
    val citizenInstruction: String,
    val dispatchDirective: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SimulationMetrics(
    val mode: String, // "Resilio (4 ECVs Multi-Gateway)" or "SCARP Baseline (1 Centroid ECV)"
    val packetDeliveryRatio: Float, // e.g. 84.6% vs 68.4%
    val endToEndDelayMs: Float,     // e.g. 164.9 ms vs 253.8 ms
    val emergencyResponseTimeMin: Float, // e.g. 23.9 min vs 59.9 min
    val communicationCoveragePct: Float, // e.g. 98.0% vs 85.0%
    val totalDeviceNodes: Int = 260,
    val coveredNodes: Int = 255,
    val activePacketsInFlight: Int = 0,
    val averageHopCount: Float = 2.4f
)

data class CitizenInstruction(
    val id: String,
    val title: String,
    val instruction: String,
    val safeRouteSummary: String,
    val recommendedShelterName: String,
    val assignedTeamCallsign: String,
    val etaMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)
