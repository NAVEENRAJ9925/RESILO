package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.edss.*
import com.example.data.maps.*
import com.example.data.repository.ResilioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisasterRegion(
    val id: String,
    val name: String,
    val regionLabel: String,
    val latitude: Double,
    val longitude: Double
)

enum class AppScreen(val title: String, val icon: String) {
    DIGITAL_TWIN("Digital Twin", "Map"),
    CITIZEN_SOS("Citizen SOS", "Emergency"),
    EDSS_COMMAND("EDSS Decision", "Psychology"),
    SIMULATION_EVAL("Mesh Benchmarks", "Analytics"),
    OFFLINE_TOOLKIT("Offline Toolkit", "Build")
}

data class MapLayerFilter(
    val showHazards: Boolean = true,
    val showShelters: Boolean = true,
    val showHospitals: Boolean = true,
    val showRescueUnits: Boolean = true,
    val showMeshLinks: Boolean = true,
    val showGateways: Boolean = true
)

class ResilioViewModel(
    private val repository: ResilioRepository = ResilioRepository()
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.DIGITAL_TWIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _mapLayers = MutableStateFlow(MapLayerFilter())
    val mapLayers: StateFlow<MapLayerFilter> = _mapLayers.asStateFlow()

    private val _selectedEntity = MutableStateFlow<Any?>(null)
    val selectedEntity: StateFlow<Any?> = _selectedEntity.asStateFlow()

    val isResilioMode: StateFlow<Boolean> = repository.isResilioMode
    val reports: StateFlow<List<EmergencyReport>> = repository.reports
    val gateways: StateFlow<List<ECVGateway>> = repository.gateways
    val hazards: StateFlow<List<HazardZone>> = repository.hazards
    val shelters: StateFlow<List<Shelter>> = repository.shelters
    val hospitals: StateFlow<List<Hospital>> = repository.hospitals
    val resources: StateFlow<List<RescueResource>> = repository.resources
    val nodes: StateFlow<List<D2DNode>> = repository.nodes
    val edssDecisions: StateFlow<List<EDSSDecision>> = repository.edssDecisions
    val citizenInstructions: StateFlow<List<CitizenInstruction>> = repository.citizenInstructions
    val simulationMetrics: StateFlow<SimulationMetrics> = repository.simulationMetrics

    // Google Maps Real-Time Grounding State (gemini-3.8-flash with googleMaps tool)
    val groundedMapsResult: StateFlow<MapsGroundingResult?> = repository.groundedMapsResult
    val isGroundingLoading: StateFlow<Boolean> = repository.isGroundingLoading
    val selectedGroundedPlace: StateFlow<GroundedPlace?> = repository.selectedGroundedPlace

    // Preset Disaster Regions for quick live grounding in Tamil Nadu and India
    val presetRegions = listOf(
        DisasterRegion(
            id = "cbe",
            name = "Coimbatore Basin (Tamil Nadu)",
            regionLabel = "Noyyal River Corridor • 10.9974°N, 76.9589°E",
            latitude = 10.9974,
            longitude = 76.9589
        ),
        DisasterRegion(
            id = "chn",
            name = "Chennai Adyar Basin (Tamil Nadu)",
            regionLabel = "Coastal Flood Plain • 13.0827°N, 80.2707°E",
            latitude = 13.0827,
            longitude = 80.2707
        ),
        DisasterRegion(
            id = "wyn",
            name = "Wayanad Valley (Kerala)",
            regionLabel = "High-Elevation Landslide Zone • 11.6854°N, 76.1320°E",
            latitude = 11.6854,
            longitude = 76.1320
        ),
        DisasterRegion(
            id = "mum",
            name = "Mumbai Mithi River (Maharashtra)",
            regionLabel = "Urban Inundation Zone • 19.0760°N, 72.8777°E",
            latitude = 19.0760,
            longitude = 72.8777
        )
    )

    private val _selectedRegion = MutableStateFlow(presetRegions[0])
    val selectedRegion: StateFlow<DisasterRegion> = _selectedRegion.asStateFlow()

    // EDSS Telemetric Mesh Routing State
    val nodeTelemetryMap: StateFlow<Map<String, NodeTelemetry>> = repository.nodeTelemetryMap
    val activePrioritizedPath: StateFlow<PrioritizedMeshPath?> = repository.activePrioritizedPath
    val selectedRoutingPolicy: StateFlow<RoutingPolicy> = repository.selectedRoutingPolicy

    fun setRoutingPolicy(policy: RoutingPolicy) {
        repository.setRoutingPolicy(policy)
    }

    fun computeRouteForNode(nodeId: String, policy: RoutingPolicy) {
        repository.computePath(nodeId, policy)
    }

    fun updateTelemetry(nodeId: String, battery: Int, rssi: Int, isHazard: Boolean) {
        repository.updateNodeTelemetry(nodeId, battery, rssi, isHazard)
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectEntity(entity: Any?) {
        _selectedEntity.value = entity
    }

    fun toggleSimulationMode(isResilio: Boolean) {
        repository.toggleSimulationMode(isResilio)
    }

    fun toggleLayer(layer: (MapLayerFilter) -> MapLayerFilter) {
        _mapLayers.value = layer(_mapLayers.value)
    }

    fun submitReport(
        category: ReportCategory,
        description: String,
        victimName: String,
        injuredCount: Int,
        waterDepthCm: Int
    ) {
        repository.submitEmergencyReport(
            category = category,
            description = description,
            victimName = victimName,
            injuredCount = injuredCount,
            waterDepthCm = waterDepthCm
        )
    }

    fun sendBroadcastAlert(title: String, alertText: String, shelterName: String) {
        repository.broadcastEmergencyAlert(title, alertText, shelterName)
    }

    fun injectDisasterScenario(name: String, type: HazardType, x: Float, y: Float, radius: Float, depth: Int) {
        repository.addHazardZone(name, type, x, y, radius, depth)
    }

    fun forceTriage(report: EmergencyReport) {
        repository.processEDSSForReport(report)
    }

    fun triggerMapsGrounding(region: DisasterRegion? = null) {
        val targetRegion = region ?: _selectedRegion.value
        _selectedRegion.value = targetRegion
        repository.syncGoogleMapsGrounding(
            areaName = targetRegion.name,
            centerLat = targetRegion.latitude,
            centerLng = targetRegion.longitude
        )
    }

    fun selectGroundedPlace(place: GroundedPlace?) {
        repository.selectGroundedPlace(place)
    }
}
