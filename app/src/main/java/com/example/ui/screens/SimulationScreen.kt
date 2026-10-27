package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.SimulationMetrics
import com.example.ui.components.MetricGaugeCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ResilioViewModel

@Composable
fun SimulationScreen(
    viewModel: ResilioViewModel
) {
    val isResilio by viewModel.isResilioMode.collectAsState()
    val metrics by viewModel.simulationMetrics.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ResilioNavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SimulationBenchmarkHeader(
                isResilio = isResilio,
                onToggle = { viewModel.toggleSimulationMode(it) }
            )
        }

        // Live Key Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = "Packet Delivery Ratio",
                    value = "${metrics.packetDeliveryRatio}",
                    unit = "%",
                    subtext = if (isResilio) "84.6% ± 4.7 (Resilio)" else "68.4% ± 5.1 (SCARP)",
                    highlightColor = if (isResilio) ResilioSuccessGreen else ResilioWarningAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = "End-to-End Delay",
                    value = "${metrics.endToEndDelayMs.toInt()}",
                    unit = "ms",
                    subtext = if (isResilio) "164.9 ms ± 34.9" else "253.8 ms ± 44.3",
                    highlightColor = ResilioCyanPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricGaugeCard(
                    title = "Emergency Response Time",
                    value = "${metrics.emergencyResponseTimeMin}",
                    unit = "min",
                    subtext = if (isResilio) "23.9 min ± 0.5 (60% faster)" else "59.9 min ± 0.8 (Manual)",
                    highlightColor = if (isResilio) ResilioSuccessGreen else ResilioEmergencyRed,
                    modifier = Modifier.weight(1f)
                )
                MetricGaugeCard(
                    title = "Communication Coverage",
                    value = "${metrics.communicationCoveragePct.toInt()}",
                    unit = "%",
                    subtext = if (isResilio) "98.0% ± 4.3" else "85.0% ± 4.7",
                    highlightColor = ResilioMeshBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Visual Comparison Bars (Figure 4)
        item {
            PerformanceComparisonChartCard()
        }

        // Table I Parameters from Paper
        item {
            SimulationParametersTableCard()
        }

        // Topology Architecture Insight (Figure 5)
        item {
            TopologyArchitectureCard(isResilio = isResilio)
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SimulationBenchmarkHeader(
    isResilio: Boolean,
    onToggle: (Boolean) -> Unit
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONTE CARLO SIMULATION BENCHMARK",
                        color = ResilioCyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Reproducing IEEE Paper Evaluation (n = 60 Monte Carlo trials)",
                        color = ResilioTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isResilio,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ResilioCyanPrimary,
                        checkedTrackColor = ResilioNavyCard,
                        uncheckedThumbColor = ResilioWarningAmber,
                        uncheckedTrackColor = ResilioNavyCard
                    ),
                    modifier = Modifier.testTag("switch_benchmark_mode")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = ResilioNavyBorder)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isResilio) {
                    "ACTIVE: Resilio Multi-Gateway (4 ECVs spread across quadrants) + Automated EDSS Triage & Digital-Twin Safe Route selection."
                } else {
                    "ACTIVE: SCARP Baseline (1 Centroid ECV) with manual triage and naive routing. Bottleneck occurs at central gateway."
                },
                color = if (isResilio) ResilioSuccessGreen else ResilioWarningAmber,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun PerformanceComparisonChartCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "EVALUATION METRICS COMPARISON (FIG. 4)",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Packet Delivery Ratio (PDR)
            MetricBarRow(
                metricName = "(a) Packet Delivery Ratio (PDR)",
                scarpVal = 68.4f,
                resilioVal = 84.6f,
                unit = "%",
                isHigherBetter = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Average End-to-End Delay
            MetricBarRow(
                metricName = "(b) Average End-to-End Delay",
                scarpVal = 253.8f,
                resilioVal = 164.9f,
                unit = "ms",
                isHigherBetter = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Emergency Response Time
            MetricBarRow(
                metricName = "(c) Emergency Response Time",
                scarpVal = 59.9f,
                resilioVal = 23.9f,
                unit = "min",
                isHigherBetter = false
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Communication Coverage
            MetricBarRow(
                metricName = "(d) Communication Coverage",
                scarpVal = 85.0f,
                resilioVal = 98.0f,
                unit = "%",
                isHigherBetter = true
            )
        }
    }
}

@Composable
private fun MetricBarRow(
    metricName: String,
    scarpVal: Float,
    resilioVal: Float,
    unit: String,
    isHigherBetter: Boolean
) {
    val maxRef = maxOf(scarpVal, resilioVal) * 1.15f
    val scarpFraction = (scarpVal / maxRef).coerceIn(0.1f, 1f)
    val resilioFraction = (resilioVal / maxRef).coerceIn(0.1f, 1f)

    Column {
        Text(text = metricName, color = ResilioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))

        // Resilio Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Resilio", color = ResilioCyanPrimary, fontSize = 10.sp, modifier = Modifier.width(46.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ResilioNavySurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(resilioFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isHigherBetter) ResilioSuccessGreen else ResilioCyanPrimary)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$resilioVal $unit",
                color = ResilioTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // SCARP Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "SCARP", color = ResilioWarningAmber, fontSize = 10.sp, modifier = Modifier.width(46.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ResilioNavySurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(scarpFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ResilioWarningAmber)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$scarpVal $unit",
                color = ResilioTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.width(64.dp)
            )
        }
    }
}

@Composable
private fun SimulationParametersTableCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavyCard),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TABLE I: SIMULATION PARAMETERS (IEEE PAPER)",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            val params = listOf(
                Triple("Area Size", "2.2 km × 2.2 km", "2.2 km × 2.2 km"),
                Triple("Device Nodes", "260 (uniform random)", "260 (uniform random)"),
                Triple("D2D Range", "260 m", "260 m"),
                Triple("Monte Carlo Trials", "60 trials", "60 trials"),
                Triple("Hop Limit (DTN TTL)", "6 hops", "6 hops"),
                Triple("Gateways (ECVs)", "1 (centroid)", "4 (farthest-point)"),
                Triple("Decision Latency", "U(18, 32) min", "U(3, 8) min"),
                Triple("Dispatch Speed", "12 km/h", "16 km/h"),
                Triple("Distance Factor", "1.0 (naive)", "0.70 (30% safe reduction)"),
                Triple("Relay Capacity", "22 paths (ρ = L/22)", "22 paths (ρ = L/22)")
            )

            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ResilioNavySurface)
                    .padding(6.dp)
            ) {
                Text("Parameter", color = ResilioTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                Text("SCARP", color = ResilioWarningAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Resilio", color = ResilioCyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
            }

            params.forEachIndexed { i, (p, s, r) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (i % 2 == 0) Color.Transparent else ResilioNavySurface.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(p, color = ResilioTextPrimary, fontSize = 10.sp, modifier = Modifier.weight(1.3f))
                    Text(s, color = ResilioTextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text(r, color = ResilioCyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.1f))
                }
            }
        }
    }
}

@Composable
private fun TopologyArchitectureCard(isResilio: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ResilioNavySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ResilioNavyBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TOPOLOGY COVERAGE INSIGHT (FIG. 5)",
                color = ResilioCyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isResilio) {
                    "Under the four-gateway Resilio configuration, peripheral clusters are absorbed into coverage (98% vs 85%). The multi-gateway architecture eliminates single points of failure, shortens average communication paths, and reduces queuing delay."
                } else {
                    "Under the single-gateway SCARP configuration, uncovered nodes cluster in regions farthest from the sole Emergency Command Vehicle (ECV). Hop limit TTL of 6 prevents peripheral nodes from reaching the centroid gateway."
                },
                color = ResilioTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}
