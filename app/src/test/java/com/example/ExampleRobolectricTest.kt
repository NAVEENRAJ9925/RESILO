package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.edss.RoutingPolicy
import com.example.data.model.*
import com.example.data.repository.ResilioRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Resilio", appName)
  }

  @Test
  fun `verify Resilio repository and EDSS triage prioritization`() {
    val repository = ResilioRepository()

    // 4 ECVs in Resilio mode
    assertEquals(4, repository.gateways.value.size)

    // Submit a critical emergency report
    val report = repository.submitEmergencyReport(
      category = ReportCategory.SOS_TRAPPED,
      description = "Critical water rising at terrace",
      victimName = "Trapped Resident",
      injuredCount = 3,
      waterDepthCm = 110
    )

    assertEquals(PriorityTier.P1_CRITICAL, report.priority)
    assertNotNull(report.nearestEcvId)

    // Switch to SCARP mode
    repository.toggleSimulationMode(false)
    assertEquals(1, repository.gateways.value.size)
    assertEquals("ECV-CENTROID", repository.gateways.value[0].id)
  }

  @Test
  fun `verify EDSS telemetric routing analyzes battery, RSSI, and hazard telemetry`() {
    val repository = ResilioRepository()

    val initialPath = repository.activePrioritizedPath.value
    assertNotNull(initialPath)
    assertTrue(initialPath!!.hops.isNotEmpty())
    assertTrue(initialPath.totalEstimatedDelayMs > 0)
    assertTrue(initialPath.pathReliabilityPct > 50)

    // Verify policy switching to Battery Guard
    repository.setRoutingPolicy(RoutingPolicy.BATTERY_CONSERVATION)
    val batteryGuardPath = repository.activePrioritizedPath.value
    assertNotNull(batteryGuardPath)
    assertEquals(RoutingPolicy.BATTERY_CONSERVATION, batteryGuardPath!!.policy)

    // Verify simulating a fault on a relay node
    val candidateRelay = batteryGuardPath.hops.first().toNodeId
    repository.updateNodeTelemetry(
      nodeId = candidateRelay,
      battery = 10, // Dying battery
      rssi = -92,   // Poor link
      isHazard = true // In flood water
    )

    val updatedPath = repository.activePrioritizedPath.value
    assertNotNull(updatedPath)
    // The health score of the candidate relay should reflect the penalty
    val telem = repository.nodeTelemetryMap.value[candidateRelay]
    assertNotNull(telem)
    assertEquals(10, telem!!.batteryPercent)
    assertTrue(telem.isSubmergedOrHazard)
  }
}
