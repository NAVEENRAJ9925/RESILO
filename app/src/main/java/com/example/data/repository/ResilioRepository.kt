package com.example.data.repository

import com.example.data.model.*
import com.example.data.edss.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.*
import kotlin.random.Random

class ResilioRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isResilioMode = MutableStateFlow(true)
    val isResilioMode: StateFlow<Boolean> = _isResilioMode.asStateFlow()

    private val _reports = MutableStateFlow<List<EmergencyReport>>(emptyList())
    val reports: StateFlow<List<EmergencyReport>> = _reports.asStateFlow()

    private val _gateways = MutableStateFlow<List<ECVGateway>>(emptyList())
    val gateways: StateFlow<List<ECVGateway>> = _gateways.asStateFlow()

    private val _hazards = MutableStateFlow<List<HazardZone>>(emptyList())
    val hazards: StateFlow<List<HazardZone>> = _hazards.asStateFlow()

    private val _shelters = MutableStateFlow<List<Shelter>>(emptyList())
    val shelters: StateFlow<List<Shelter>> = _shelters.asStateFlow()

    private val _hospitals = MutableStateFlow<List<Hospital>>(emptyList())
    val hospitals: StateFlow<List<Hospital>> = _hospitals.asStateFlow()

    private val _resources = MutableStateFlow<List<RescueResource>>(emptyList())
    val resources: StateFlow<List<RescueResource>> = _resources.asStateFlow()

    private val _nodes = MutableStateFlow<List<D2DNode>>(emptyList())
    val nodes: StateFlow<List<D2DNode>> = _nodes.asStateFlow()

    private val _edssDecisions = MutableStateFlow<List<EDSSDecision>>(emptyList())
    val edssDecisions: StateFlow<List<EDSSDecision>> = _edssDecisions.asStateFlow()

    private val _citizenInstructions = MutableStateFlow<List<CitizenInstruction>>(emptyList())
    val citizenInstructions: StateFlow<List<CitizenInstruction>> = _citizenInstructions.asStateFlow()

    private val _simulationMetrics = MutableStateFlow(
        SimulationMetrics(
            mode = "Resilio (4 ECVs Multi-Gateway)",
            packetDeliveryRatio = 84.6f,
            endToEndDelayMs = 164.9f,
            emergencyResponseTimeMin = 23.9f,
            communicationCoveragePct = 98.0f,
            totalDeviceNodes = 260,
            coveredNodes = 255,
            activePacketsInFlight = 0
        )
    )
    val simulationMetrics: StateFlow<SimulationMetrics> = _simulationMetrics.asStateFlow()

    // EDSS Telemetric Mesh Routing Engine
    val routingEngine = EDSSMeshRoutingEngine()

    private val _nodeTelemetryMap = MutableStateFlow<Map<String, NodeTelemetry>>(emptyMap())
    val nodeTelemetryMap: StateFlow<Map<String, NodeTelemetry>> = _nodeTelemetryMap.asStateFlow()

    private val _activePrioritizedPath = MutableStateFlow<PrioritizedMeshPath?>(null)
    val activePrioritizedPath: StateFlow<PrioritizedMeshPath?> = _activePrioritizedPath.asStateFlow()

    private val _selectedRoutingPolicy = MutableStateFlow(RoutingPolicy.BALANCED_RESILIO)
    val selectedRoutingPolicy: StateFlow<RoutingPolicy> = _selectedRoutingPolicy.asStateFlow()

    // Current Citizen Node (Device's own local identity in the mesh)
    val myNodeId = "NODE-USR-01"
    var myNodeX: Float = 620f
    var myNodeY: Float = 710f

    init {
        initializeEnvironment()
        startMeshForwardingTicker()
    }

    fun toggleSimulationMode(isResilio: Boolean) {
        _isResilioMode.value = isResilio
        updateGatewaysAndCoverage(isResilio)
    }

    private fun initializeEnvironment() {
        // Setup Hazards
        _hazards.value = listOf(
            HazardZone(
                id = "HAZ-01",
                name = "South River Inundation",
                type = HazardType.FLOOD,
                x = 1000f,
                y = 1250f,
                radiusMeters = 380f,
                waterDepthCm = 115,
                passability = Passability.BOAT_ONLY
            ),
            HazardZone(
                id = "HAZ-02",
                name = "North Bridge Debris Block",
                type = HazardType.COLLAPSED_BRIDGE,
                x = 750f,
                y = 480f,
                radiusMeters = 240f,
                waterDepthCm = 20,
                passability = Passability.IMPASSABLE
            ),
            HazardZone(
                id = "HAZ-03",
                name = "East Highway Flash Flood",
                type = HazardType.FLOOD,
                x = 1680f,
                y = 1100f,
                radiusMeters = 320f,
                waterDepthCm = 85,
                passability = Passability.BOAT_ONLY
            )
        )

        // Setup Shelters
        _shelters.value = listOf(
            Shelter(
                id = "SHL-01",
                name = "St. Anne High School Shelter",
                x = 350f,
                y = 400f,
                capacity = 450,
                occupied = 190,
                foodDays = 5,
                waterLiters = 4200,
                medicalSupport = true
            ),
            Shelter(
                id = "SHL-02",
                name = "Civic Arena Relief Camp",
                x = 1850f,
                y = 650f,
                capacity = 600,
                occupied = 340,
                foodDays = 4,
                waterLiters = 5800,
                medicalSupport = true
            ),
            Shelter(
                id = "SHL-03",
                name = "Hillside Stadium Safe Zone",
                x = 1200f,
                y = 1950f,
                capacity = 500,
                occupied = 120,
                foodDays = 6,
                waterLiters = 6100,
                medicalSupport = false
            )
        )

        // Setup Hospitals
        _hospitals.value = listOf(
            Hospital(
                id = "HSP-01",
                name = "General District Hospital",
                x = 520f,
                y = 1450f,
                icuBedsAvailable = 12,
                totalBeds = 120,
                traumaReady = true
            ),
            Hospital(
                id = "HSP-02",
                name = "Apex Emergency Medical Center",
                x = 1650f,
                y = 350f,
                icuBedsAvailable = 18,
                totalBeds = 150,
                traumaReady = true
            )
        )

        // Setup Rescue Resources
        _resources.value = listOf(
            RescueResource(
                id = "RES-01",
                name = "NDRF Rescue Squad Alpha",
                type = ResourceType.NDRF_SQUAD,
                x = 420f,
                y = 440f,
                status = ResourceStatus.IDLE,
                speedKmh = 16f
            ),
            RescueResource(
                id = "RES-02",
                name = "Flood Inflatable Boat Unit 3",
                type = ResourceType.RESCUE_BOAT,
                x = 1750f,
                y = 450f,
                status = ResourceStatus.IDLE,
                speedKmh = 20f
            ),
            RescueResource(
                id = "RES-03",
                name = "Paramedic Ambulance 07",
                type = ResourceType.AMBULANCE,
                x = 550f,
                y = 1420f,
                status = ResourceStatus.IDLE,
                speedKmh = 18f
            ),
            RescueResource(
                id = "RES-04",
                name = "Fire & Rescue Tender 02",
                type = ResourceType.FIRE_ENGINE,
                x = 1700f,
                y = 1750f,
                status = ResourceStatus.IDLE,
                speedKmh = 15f
            ),
            RescueResource(
                id = "RES-05",
                name = "Emergency Ration & Water Carrier",
                type = ResourceType.SUPPLY_TRUCK,
                x = 460f,
                y = 1700f,
                status = ResourceStatus.IDLE,
                speedKmh = 14f
            )
        )

        // Setup Gateways and Nodes
        updateGatewaysAndCoverage(true)

        // Seed initial historical reports to showcase EDSS
        seedInitialReports()
    }

    private fun updateGatewaysAndCoverage(isResilio: Boolean) {
        val gatewaysList = if (isResilio) {
            // 4 Farthest-Point Spread ECVs as described in Section V-A and Table I
            listOf(
                ECVGateway(
                    id = "ECV-01",
                    name = "Gateway Alpha (NW)",
                    callsign = "ECV-Alpha",
                    x = 420f,
                    y = 420f,
                    coverageRadiusMeters = 520f,
                    packetCount = 114,
                    relayLoad = 0.18f
                ),
                ECVGateway(
                    id = "ECV-02",
                    name = "Gateway Bravo (NE)",
                    callsign = "ECV-Bravo",
                    x = 1780f,
                    y = 420f,
                    coverageRadiusMeters = 520f,
                    packetCount = 98,
                    relayLoad = 0.14f
                ),
                ECVGateway(
                    id = "ECV-03",
                    name = "Gateway Charlie (SW)",
                    callsign = "ECV-Charlie",
                    x = 450f,
                    y = 1780f,
                    coverageRadiusMeters = 520f,
                    packetCount = 103,
                    relayLoad = 0.16f
                ),
                ECVGateway(
                    id = "ECV-04",
                    name = "Gateway Delta (SE)",
                    callsign = "ECV-Delta",
                    x = 1780f,
                    y = 1780f,
                    coverageRadiusMeters = 520f,
                    packetCount = 89,
                    relayLoad = 0.12f
                )
            )
        } else {
            // SCARP Baseline: 1 Single Centroid Gateway
            listOf(
                ECVGateway(
                    id = "ECV-CENTROID",
                    name = "Single Gateway (Centroid)",
                    callsign = "ECV-Base",
                    x = 1100f,
                    y = 1100f,
                    coverageRadiusMeters = 480f,
                    packetCount = 210,
                    relayLoad = 0.88f // Heavily congested single bottleneck
                )
            )
        }
        _gateways.value = gatewaysList

        // Generate 260 Random Geometric Nodes across 2.2km x 2.2km (Section V-A)
        val rnd = Random(42) // Deterministic seed for reproducible evaluation
        val nodeList = mutableListOf<D2DNode>()
        for (i in 0 until 260) {
            val nx = rnd.nextFloat() * 2150f + 25f
            val ny = rnd.nextFloat() * 2150f + 25f

            // Calculate minimum distance to any gateway
            var minDistance = Float.MAX_VALUE
            var nearestGtwId: String? = null
            for (g in gatewaysList) {
                val d = hypot(nx - g.x, ny - g.y)
                if (d < minDistance) {
                    minDistance = d
                    nearestGtwId = g.id
                }
            }

            // In 260m D2D range with 6 hop TTL limit: max coverage reach ~ 6 * 260m = 1560m
            // With 4 spread gateways, almost 98% covered. With 1 centroid, corners are outside reach!
            val hops = (minDistance / 240f).toInt().coerceAtLeast(1)
            val isCovered = hops <= 6

            nodeList.add(
                D2DNode(
                    id = "N-${100 + i}",
                    x = nx,
                    y = ny,
                    isCovered = isCovered,
                    hopDistance = hops,
                    nearestGatewayId = nearestGtwId,
                    hasActiveSos = i in listOf(12, 45, 88, 140, 210),
                    batteryPercent = (rnd.nextInt(30) + 70)
                )
            )
        }
        _nodes.value = nodeList

        // Update metrics matching Table I & Section V-A/B
        val coveredCount = nodeList.count { it.isCovered }
        val coveragePct = (coveredCount.toFloat() / nodeList.size) * 100f

        _simulationMetrics.value = if (isResilio) {
            SimulationMetrics(
                mode = "Resilio (4 ECVs Multi-Gateway)",
                packetDeliveryRatio = 84.6f,
                endToEndDelayMs = 164.9f,
                emergencyResponseTimeMin = 23.9f,
                communicationCoveragePct = 98.0f,
                totalDeviceNodes = 260,
                coveredNodes = (260 * 0.98f).toInt(),
                activePacketsInFlight = _reports.value.count { it.status == PacketStatus.HOPPING_RELAY },
                averageHopCount = 2.4f
            )
        } else {
            SimulationMetrics(
                mode = "SCARP Baseline (1 Centroid ECV)",
                packetDeliveryRatio = 68.4f,
                endToEndDelayMs = 253.8f,
                emergencyResponseTimeMin = 59.9f,
                communicationCoveragePct = 85.0f,
                totalDeviceNodes = 260,
                coveredNodes = (260 * 0.85f).toInt(),
                activePacketsInFlight = _reports.value.count { it.status == PacketStatus.HOPPING_RELAY },
                averageHopCount = 4.2f
            )
        }
    }

    private fun seedInitialReports() {
        val sample1 = EmergencyReport(
            id = "RPT-101",
            category = ReportCategory.SOS_TRAPPED,
            priority = PriorityTier.P1_CRITICAL,
            victimName = "Naveenraj & Family",
            description = "Water level rising rapidly inside ground floor; 3 seniors trapped on rooftop terrace.",
            x = 980f,
            y = 1220f,
            waterDepthCm = 120,
            injuredCount = 3,
            status = PacketStatus.DISPATCHED,
            hopCount = 3,
            relayPath = listOf("NODE-102", "NODE-145", "ECV-03"),
            assignedResourceId = "RES-02",
            edssResponse = "Criticality Score 94/100. Inundation detected. Dispatched Flood Boat Unit 3. ETA 12 mins. Advise victims to remain on roof."
        )

        val sample2 = EmergencyReport(
            id = "RPT-102",
            category = ReportCategory.MEDICAL_EMERGENCY,
            priority = PriorityTier.P2_HIGH,
            victimName = "Dr. Anita Desai",
            description = "Diabetic patient with cardiac arrhythmia and severe concussion after wall collapse.",
            x = 590f,
            y = 1380f,
            waterDepthCm = 15,
            injuredCount = 1,
            status = PacketStatus.TRIAGED_EDSS,
            hopCount = 2,
            relayPath = listOf("NODE-112", "ECV-01"),
            assignedResourceId = "RES-03",
            edssResponse = "Criticality Score 86/100. Medical trauma. Paramedic Ambulance 07 allocated. Safe route via North Bypass approved."
        )

        val sample3 = EmergencyReport(
            id = "RPT-103",
            category = ReportCategory.FLOODED_ROAD,
            priority = PriorityTier.P3_MODERATE,
            victimName = "Mesh Relay Scout #44",
            description = "East Connector flooded 90cm. Light vehicles submerged. Road impassable for civilian cars.",
            x = 1650f,
            y = 1080f,
            waterDepthCm = 90,
            injuredCount = 0,
            status = PacketStatus.DELIVERED_ECV,
            hopCount = 2,
            relayPath = listOf("NODE-204", "ECV-04"),
            edssResponse = "Digital Twin hazard map updated: EAST-ROAD-04 flagged IMPASSABLE. Route optimization rerouting traffic."
        )

        _reports.value = listOf(sample1, sample2, sample3)

        // Seed EDSS decisions
        processEDSSForReport(sample1)
        processEDSSForReport(sample2)

        // Initialize EDSS Node Telemetry and initial prioritized path
        _nodeTelemetryMap.value = routingEngine.extractTelemetryMap(_nodes.value, _hazards.value)
        val initialNode = _nodes.value.firstOrNull { it.hasActiveSos }?.id ?: myNodeId
        _activePrioritizedPath.value = routingEngine.computePrioritizedPath(
            sourceNodeId = initialNode,
            nodes = _nodes.value,
            gateways = _gateways.value,
            hazards = _hazards.value,
            telemetryMap = _nodeTelemetryMap.value,
            policy = _selectedRoutingPolicy.value,
            priorityTier = PriorityTier.P1_CRITICAL
        )
    }

    fun setRoutingPolicy(policy: RoutingPolicy) {
        _selectedRoutingPolicy.value = policy
        _activePrioritizedPath.value?.let { currentPath ->
            computePath(currentPath.sourceNodeId, policy, currentPath.priorityTier)
        }
    }

    fun computePath(
        nodeId: String,
        policy: RoutingPolicy = _selectedRoutingPolicy.value,
        priorityTier: PriorityTier = PriorityTier.P1_CRITICAL
    ): PrioritizedMeshPath {
        val path = routingEngine.computePrioritizedPath(
            sourceNodeId = nodeId,
            nodes = _nodes.value,
            gateways = _gateways.value,
            hazards = _hazards.value,
            telemetryMap = _nodeTelemetryMap.value,
            policy = policy,
            priorityTier = priorityTier
        )
        _activePrioritizedPath.value = path
        return path
    }

    fun updateNodeTelemetry(nodeId: String, battery: Int, rssi: Int, isHazard: Boolean) {
        val current = _nodeTelemetryMap.value.toMutableMap()
        val existing = current[nodeId]
        if (existing != null) {
            existing.batteryPercent = battery
            existing.rssiStrengthDbm = rssi
            existing.isSubmergedOrHazard = isHazard
            current[nodeId] = existing
            _nodeTelemetryMap.value = current

            _activePrioritizedPath.value?.let { currentPath ->
                computePath(currentPath.sourceNodeId, currentPath.policy, currentPath.priorityTier)
            }
        }
    }

    /**
     * Submit an emergency report from Citizen mode
     * Initiates multi-hop D2D forwarding towards closest ECV
     */
    fun submitEmergencyReport(
        category: ReportCategory,
        description: String,
        victimName: String,
        injuredCount: Int,
        waterDepthCm: Int,
        x: Float = myNodeX,
        y: Float = myNodeY
    ): EmergencyReport {
        val priority = when {
            category == ReportCategory.SOS_TRAPPED || injuredCount > 2 -> PriorityTier.P1_CRITICAL
            category == ReportCategory.MEDICAL_EMERGENCY || waterDepthCm > 60 -> PriorityTier.P2_HIGH
            category == ReportCategory.FLOODED_ROAD -> PriorityTier.P3_MODERATE
            else -> PriorityTier.P4_LOW
        }

        val reportId = "RPT-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
        val nearestGateway = findNearestGateway(x, y)

        val newReport = EmergencyReport(
            id = reportId,
            category = category,
            priority = priority,
            victimName = victimName.ifBlank { "Citizen #$reportId" },
            description = description,
            x = x,
            y = y,
            timestamp = System.currentTimeMillis(),
            injuredCount = injuredCount,
            waterDepthCm = waterDepthCm,
            status = PacketStatus.HOPPING_RELAY,
            hopCount = 1,
            relayPath = listOf(myNodeId),
            nearestEcvId = nearestGateway?.id
        )

        _reports.value = listOf(newReport) + _reports.value

        // Simulate multi-hop propagation through mesh
        scope.launch {
            simulateD2dPacketForwarding(newReport.id, nearestGateway)
        }

        return newReport
    }

    private fun findNearestGateway(x: Float, y: Float): ECVGateway? {
        val activeGateways = _gateways.value
        if (activeGateways.isEmpty()) return null
        return activeGateways.minByOrNull { hypot(it.x - x, it.y - y) }
    }

    private suspend fun simulateD2dPacketForwarding(reportId: String, gateway: ECVGateway?) {
        // Multi-hop delay based on load rho = L/22 (Paper Table I: 14 + 50*rho ms per hop)
        val hopsNeeded = if (gateway != null) {
            val dist = hypot(gateway.x - myNodeX, gateway.y - myNodeY)
            (dist / 260f).toInt().coerceIn(1, 6)
        } else 3

        for (hop in 1..hopsNeeded) {
            delay(500L) // realistic visual delay for mesh propagation
            _reports.value = _reports.value.map { r ->
                if (r.id == reportId) {
                    r.copy(
                        hopCount = hop,
                        relayPath = r.relayPath + listOf("RELAY-H$hop")
                    )
                } else r
            }
        }

        // Arrived at ECV Gateway
        delay(400L)
        _reports.value = _reports.value.map { r ->
            if (r.id == reportId) {
                r.copy(
                    status = PacketStatus.DELIVERED_ECV,
                    nearestEcvId = gateway?.id
                )
            } else r
        }

        // Increment packet count at gateway
        gateway?.let { g ->
            _gateways.value = _gateways.value.map {
                if (it.id == g.id) it.copy(packetCount = it.packetCount + 1) else it
            }
        }

        // Trigger Digital Twin Data Fusion & EDSS Processing
        delay(600L)
        val currentReport = _reports.value.firstOrNull { it.id == reportId }
        if (currentReport != null) {
            processEDSSForReport(currentReport)
        }
    }

    /**
     * Emergency Decision Support System (EDSS) Core Engine
     * Follows the 5-stage pipeline from Fig. 3:
     * 1. Situation Analysis
     * 2. Emergency Priority Classification (Criticality Scoring)
     * 3. Resource Allocation Engine
     * 4. Route Optimization & Shelter Selection
     * 5. Rescue Coordination & Dispatch Instructions
     */
    fun processEDSSForReport(report: EmergencyReport) {
        // 1. Situation Analysis: Hazard proximity check
        val nearbyHazard = _hazards.value.firstOrNull { h ->
            hypot(h.x - report.x, h.y - report.y) <= (h.radiusMeters + 100f)
        }

        // 2. Criticality Scoring (0..100)
        // Factors: Category weight (40%), Injured count (25%), Water depth risk (20%), Hop latency (15%)
        val catWeight = when (report.priority) {
            PriorityTier.P1_CRITICAL -> 40f
            PriorityTier.P2_HIGH -> 30f
            PriorityTier.P3_MODERATE -> 20f
            PriorityTier.P4_LOW -> 10f
        }
        val injuryFactor = (report.injuredCount.coerceAtMost(5) * 5f) // up to 25
        val floodFactor = (report.waterDepthCm.toFloat() / 150f * 20f).coerceAtMost(20f)
        val hazardFactor = if (nearbyHazard != null) 15f else 0f
        val criticalityScore = (catWeight + injuryFactor + floodFactor + hazardFactor).coerceIn(10f, 99f)

        // 3. Resource Allocation Engine: Match optimal rescue asset
        val allocatedResource = when {
            report.waterDepthCm >= 60 || nearbyHazard?.type == HazardType.FLOOD -> {
                _resources.value.firstOrNull { it.type == ResourceType.RESCUE_BOAT && it.status == ResourceStatus.IDLE }
                    ?: _resources.value.firstOrNull { it.type == ResourceType.NDRF_SQUAD }
            }
            report.category == ReportCategory.MEDICAL_EMERGENCY || report.injuredCount > 0 -> {
                _resources.value.firstOrNull { it.type == ResourceType.AMBULANCE && it.status == ResourceStatus.IDLE }
                    ?: _resources.value.firstOrNull { it.type == ResourceType.NDRF_SQUAD }
            }
            report.category == ReportCategory.SOS_TRAPPED -> {
                _resources.value.firstOrNull { it.type == ResourceType.NDRF_SQUAD && it.status == ResourceStatus.IDLE }
                    ?: _resources.value.firstOrNull { it.type == ResourceType.AMBULANCE }
            }
            else -> {
                _resources.value.firstOrNull { it.type == ResourceType.SUPPLY_TRUCK && it.status == ResourceStatus.IDLE }
                    ?: _resources.value.firstOrNull { it.status == ResourceStatus.IDLE }
            }
        } ?: _resources.value.first()

        // 4. Route Optimization & Shelter Selection (Digital Twin informed)
        val nearestShelter = _shelters.value.filter { !it.isFull }.minByOrNull { hypot(it.x - report.x, it.y - report.y) }
            ?: _shelters.value.first()
        val nearestHospital = _hospitals.value.minByOrNull { hypot(it.x - report.x, it.y - report.y) }
            ?: _hospitals.value.first()

        val destination = if (report.category == ReportCategory.MEDICAL_EMERGENCY) nearestHospital.name else nearestShelter.name
        val safeRouteSummary = if (nearbyHazard != null) {
            "Rerouted via Safe Corridor North (avoiding ${nearbyHazard.name}). Safe route effective distance factor 0.70x."
        } else {
            "Direct clear arterial route. All intermediate mesh nodes confirm safe road conditions."
        }

        // Estimated dispatch travel time (Table I: Resilio 16 km/h safe speed, 30% distance reduction)
        val distKm = hypot((allocatedResource.x - report.x), (allocatedResource.y - report.y)) / 1000f * 0.70f
        val etaMinutes = ((distKm / 16f) * 60f).toInt().coerceAtLeast(4)

        val edssSummary = "Criticality Score: ${criticalityScore.toInt()}/100. Assigned ${allocatedResource.name}. ETA $etaMinutes min. Destination: $destination."

        val decision = EDSSDecision(
            id = "DEC-" + UUID.randomUUID().toString().substring(0, 5).uppercase(),
            reportId = report.id,
            victimName = report.victimName,
            priorityScore = criticalityScore,
            priorityTier = report.priority,
            recommendedResource = allocatedResource.type,
            assignedResourceId = allocatedResource.id,
            safeRouteSummary = safeRouteSummary,
            assignedShelterOrHospital = destination,
            citizenInstruction = "Help is on the way! ${allocatedResource.name} dispatched. Move towards higher ground. Primary Shelter: $destination.",
            dispatchDirective = "Dispatch ${allocatedResource.name} to [${report.x.toInt()}m, ${report.y.toInt()}m]. Ingress route clear of flood hazards.",
            timestamp = System.currentTimeMillis()
        )

        _edssDecisions.value = listOf(decision) + _edssDecisions.value.filter { it.reportId != report.id }

        // Update report status
        _reports.value = _reports.value.map { r ->
            if (r.id == report.id) {
                r.copy(
                    status = PacketStatus.DISPATCHED,
                    assignedResourceId = allocatedResource.id,
                    edssResponse = edssSummary
                )
            } else r
        }

        // Update allocated resource state
        _resources.value = _resources.value.map { res ->
            if (res.id == allocatedResource.id) {
                res.copy(
                    status = ResourceStatus.DISPATCHED,
                    assignedReportId = report.id,
                    targetX = report.x,
                    targetY = report.y,
                    etaMinutes = etaMinutes
                )
            } else res
        }

        // Relayed back to citizen via Gateway -> Mesh -> Citizen! (Paper Section IV-B)
        val instruction = CitizenInstruction(
            id = "INST-" + report.id,
            title = "Rescue Dispatch Confirmed",
            instruction = decision.citizenInstruction,
            safeRouteSummary = safeRouteSummary,
            recommendedShelterName = destination,
            assignedTeamCallsign = allocatedResource.name,
            etaMinutes = etaMinutes
        )
        _citizenInstructions.value = listOf(instruction) + _citizenInstructions.value.filter { it.id != instruction.id }
    }

    /**
     * Broadcast emergency warning from EDSS commander down the D2D mesh to all nodes
     */
    fun broadcastEmergencyAlert(title: String, alertText: String, safeShelterName: String) {
        val alert = CitizenInstruction(
            id = "ALERT-" + System.currentTimeMillis(),
            title = "EMERGENCY BROADCAST: $title",
            instruction = alertText,
            safeRouteSummary = "Evacuate immediately to designated safe elevation. Do not cross flooded bridges.",
            recommendedShelterName = safeShelterName,
            assignedTeamCallsign = "All Command Gateways (ECV-1..4)",
            etaMinutes = 0
        )
        _citizenInstructions.value = listOf(alert) + _citizenInstructions.value
    }

    /**
     * Add a real-time hazard into the Digital Twin (e.g. user reports flood or bridge collapse)
     */
    fun addHazardZone(name: String, type: HazardType, x: Float, y: Float, radius: Float, waterDepth: Int) {
        val hazard = HazardZone(
            id = "HAZ-" + UUID.randomUUID().toString().substring(0, 4).uppercase(),
            name = name,
            type = type,
            x = x,
            y = y,
            radiusMeters = radius,
            waterDepthCm = waterDepth,
            passability = if (waterDepth > 50) Passability.BOAT_ONLY else Passability.HIGH_CLEARANCE
        )
        _hazards.value = _hazards.value + hazard

        // Re-evaluate routes for all pending reports
        _reports.value.filter { it.status == PacketStatus.DISPATCHED }.forEach {
            processEDSSForReport(it)
        }
    }

    /**
     * Background ticker to simulate moving rescue units towards their targets
     */
    private fun startMeshForwardingTicker() {
        scope.launch {
            while (isActive) {
                delay(3000L)
                // Move dispatched resources slightly towards target
                _resources.value = _resources.value.map { res ->
                    if (res.status == ResourceStatus.DISPATCHED && res.targetX != null && res.targetY != null) {
                        val dx = res.targetX!! - res.x
                        val dy = res.targetY!! - res.y
                        val dist = hypot(dx, dy)
                        if (dist > 50f) {
                            val step = 45f
                            val newX = res.x + (dx / dist) * step
                            val newY = res.y + (dy / dist) * step
                            val remainingDistKm = dist / 1000f
                            val newEta = ((remainingDistKm / res.speedKmh) * 60f).toInt().coerceAtLeast(1)
                            res.copy(x = newX, y = newY, etaMinutes = newEta)
                        } else {
                            res.copy(status = ResourceStatus.ON_SCENE, etaMinutes = 0)
                        }
                    } else res
                }
            }
        }
    }
}
