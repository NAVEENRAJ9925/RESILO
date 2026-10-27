package com.example.data.edss

import com.example.data.model.PriorityTier

enum class RoutingPolicy(val displayName: String, val description: String) {
    BALANCED_RESILIO("Resilio Adaptive (Telemetric)", "Optimizes for delivery ratio, battery preservation & hazard avoidance"),
    CRITICAL_LOW_LATENCY("P1 Priority Low-Latency", "Shortest delay path using strongest RSSI links for immediate life safety"),
    BATTERY_CONSERVATION("Energy Guard (<25% Avoidance)", "Reroutes packets around low-battery devices to prevent node shutdown"),
    HAZARD_IMMUNE("Hazard Zone Bypass", "Strictly avoids relays within active flood waters or structural debris"),
    NAIVE_SHORTEST_HOP("Naive Shortest Hop (SCARP)", "Blind shortest Euclidean hops without telemetry awareness")
}

data class NodeTelemetry(
    val nodeId: String,
    var batteryPercent: Int, // 0..100
    var rssiStrengthDbm: Int, // -40 (excellent) to -95 (poor)
    var isSubmergedOrHazard: Boolean,
    var currentQueuePackets: Int, // current buffer load
    var relayLoadRatio: Float = 0.1f // rho = L/22
) {
    // Computes overall routing suitability score (0 = unusable, 100 = optimal relay)
    val healthScore: Int
        get() {
            var score = batteryPercent * 0.40f // Battery weight 40%
            val rssiNorm = ((rssiStrengthDbm + 100).coerceIn(0, 60) / 60f) * 35f // RSSI weight 35%
            score += rssiNorm
            if (isSubmergedOrHazard) score -= 30f // Hazard penalty
            score -= (currentQueuePackets.coerceAtMost(10) * 2.5f) // Congestion penalty
            return score.toInt().coerceIn(5, 99)
        }
}

data class EvaluatedHop(
    val fromNodeId: String,
    val toNodeId: String,
    val distanceMeters: Float,
    val linkRssiDbm: Int,
    val deliveryProb: Float,
    val delayMs: Float,
    val targetBattery: Int
)

data class PrioritizedMeshPath(
    val pathId: String,
    val sourceNodeId: String,
    val targetGatewayId: String,
    val policy: RoutingPolicy,
    val priorityTier: PriorityTier,
    val hops: List<EvaluatedHop>,
    val nodeIds: List<String>,
    val totalEstimatedDelayMs: Float,
    val pathReliabilityPct: Float,
    val averageBatteryLevelPct: Int,
    val hazardAvoided: Boolean,
    val bottleneckCongestionIndex: Float
)
