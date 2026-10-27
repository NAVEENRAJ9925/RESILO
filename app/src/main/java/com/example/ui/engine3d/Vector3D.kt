package com.example.ui.engine3d

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Point3D(x * scale, y * scale, z * scale)
}

data class Camera3D(
    var yawDeg: Float = 45f,          // Orbit rotation around Z (0..360)
    var pitchDeg: Float = 55f,        // Tilt angle from top (20..85)
    var zoom: Float = 1.0f,           // Zoom factor (0.4..3.0)
    var targetX: Float = 1100f,       // Center of 2.2km area
    var targetY: Float = 1100f,
    var targetZ: Float = 0f,
    var fovDistance: Float = 2200f
) {
    /**
     * Projects a 3D world coordinate (in 0..2200m space, with Z in meters) to 2D screen coordinates.
     * Returns Pair(Offset, DepthZ).
     */
    fun project(
        point: Point3D,
        screenWidth: Float,
        screenHeight: Float
    ): ProjectedPoint {
        val yawRad = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitchRad = Math.toRadians(pitchDeg.toDouble()).toFloat()

        // 1. Translate relative to target
        val dx = point.x - targetX
        val dy = point.y - targetY
        val dz = point.z - targetZ

        // 2. Rotate Yaw (around Z axis)
        val cosY = cos(yawRad)
        val sinY = sin(yawRad)
        val xYaw = dx * cosY - dy * sinY
        val yYaw = dx * sinY + dy * cosY
        val zYaw = dz

        // 3. Rotate Pitch (tilt around X axis)
        val cosP = cos(pitchRad)
        val sinP = sin(pitchRad)
        val yPitch = yYaw * cosP - zYaw * sinP
        val zPitch = yYaw * sinP + zYaw * cosP

        // 4. Perspective Projection
        val distanceToCamera = fovDistance + zPitch
        val safeDistance = distanceToCamera.coerceAtLeast(100f)
        val scaleFactor = (fovDistance / safeDistance) * zoom * (minOf(screenWidth, screenHeight) / 1400f)

        val screenX = screenWidth / 2f + xYaw * scaleFactor
        val screenY = screenHeight / 2f - yPitch * scaleFactor

        return ProjectedPoint(
            screen = Offset(screenX, screenY),
            depth = zPitch,
            scale = scaleFactor,
            isVisible = distanceToCamera > 50f
        )
    }
}

data class ProjectedPoint(
    val screen: Offset,
    val depth: Float,
    val scale: Float,
    val isVisible: Boolean
)

enum class VisualThemeMode {
    TACTICAL_3D,       // Realistic tactical daylight disaster map
    THERMAL_FLIR,      // High-contrast infrared night thermal
    CYBER_TWIN         // Holographic wireframe digital twin
}
