/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("DEPRECATION")

package androidx.xr.arcore.projected.testapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseInternalError
import androidx.xr.arcore.CreateGeospatialPoseFromPoseNotTracking
import androidx.xr.arcore.CreateGeospatialPoseFromPoseResult
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.CreatePoseFromGeospatialPoseInternalError
import androidx.xr.arcore.CreatePoseFromGeospatialPoseNotTracking
import androidx.xr.arcore.CreatePoseFromGeospatialPoseResult
import androidx.xr.arcore.CreatePoseFromGeospatialPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.TrackingState
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.Text
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlinx.coroutines.launch

class LowPowerGeospatialActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LowPowerGeospatialActivity"
    }

    private var targetModeState by mutableStateOf(GeospatialMode.SPATIAL)

    private fun GeospatialMode.getTargetModeName(): String {
        return if (this == GeospatialMode.INERTIAL) "INERTIAL" else "SPATIAL"
    }

    private fun GeospatialMode.getTargetModeTitle(): String {
        return if (this == GeospatialMode.INERTIAL) {
            "LOW_POWER (INERTIAL)\n[Tap glasses to switch]"
        } else {
            "HIGH_ACCURACY (SPATIAL)\n[Tap glasses to switch]"
        }
    }

    private var sessionInstance by mutableStateOf<Session?>(null)
    private var geospatialInstance by mutableStateOf<Geospatial?>(null)
    private var arDeviceInstance by mutableStateOf<ArDevice?>(null)
    @OptIn(ExperimentalProjectedApi::class)
    private val requestPermissionLauncher:
        ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            tryCreateAndConfigureSession()
        }

    @OptIn(ExperimentalProjectedApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionsRequired =
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        val hasAllPermissions =
            permissionsRequired.all {
                ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            }
        if (hasAllPermissions) {
            tryCreateAndConfigureSession()
        } else {
            val params =
                ProjectedPermissionsRequestParams(
                    permissions = permissionsRequired,
                    rationale = "Location permissions are required in projected mode.",
                )
            requestPermissionLauncher.launch(listOf(params))
        }
        setContent {
            GlimmerTheme {
                val session = sessionInstance
                val geospatial = geospatialInstance
                val arDevice = arDeviceInstance
                if (session == null || geospatial == null || arDevice == null) {
                    return@GlimmerTheme Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(GlimmerTheme.colors.surface)
                                .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Initializing Projected Session...", fontWeight = FontWeight.Bold)
                    }
                }
                GeospatialDashboard(geospatial, arDevice)
            }
        }
    }

    private fun toggleGeospatialMode() {
        val newMode =
            if (targetModeState == GeospatialMode.SPATIAL) GeospatialMode.INERTIAL
            else GeospatialMode.SPATIAL
        targetModeState = newMode
        Log.i(TAG, "Switching geospatial mode to ${targetModeState.getTargetModeName()}")
        try {
            sessionInstance?.configure(
                Config.Builder()
                    .setGeospatial(newMode)
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure session", e)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            toggleGeospatialMode()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun tryCreateAndConfigureSession() {
        lifecycleScope.launch {
            try {
                when (
                    val result =
                        Session.create(
                            context = this@LowPowerGeospatialActivity,
                            lifecycleOwner = this@LowPowerGeospatialActivity,
                        )
                ) {
                    is SessionCreateSuccess -> {
                        val session = result.session
                        val config =
                            Config.Builder()
                                .setGeospatial(targetModeState)
                                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                                .build()
                        try {
                            session.configure(config)
                            sessionInstance = session
                            geospatialInstance = Geospatial.getInstance(session)
                            arDeviceInstance = ArDevice.getInstance(session)
                        } catch (e: Exception) {
                            Log.e(TAG, "Session configuration failed", e)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Session creation failed: $result")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating session", e)
            }
        }
    }

    @Composable
    private fun GeospatialDashboard(geospatial: Geospatial, arDevice: ArDevice) {
        val geospatialState by geospatial.state.collectAsState()
        val arDeviceState by arDevice.state.collectAsState()
        val geoPoseResult =
            remember(arDeviceState.devicePose, geospatialState.geospatialTrackingState) {
                if (
                    geospatialState.geospatialTrackingState ==
                        Geospatial.GeospatialTrackingState.RUNNING
                ) {
                    try {
                        geospatial.createGeospatialPoseFromPose(arDeviceState.devicePose)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating geospatial pose", e)
                        null
                    }
                } else {
                    null
                }
            }

        val deviceGeoPoseState = geospatialState.geospatialPose

        val localPoseResult =
            remember(deviceGeoPoseState, geospatialState.geospatialTrackingState) {
                if (
                    geospatialState.geospatialTrackingState ==
                        Geospatial.GeospatialTrackingState.RUNNING
                ) {
                    try {
                        geospatial.createPoseFromGeospatialPose(deviceGeoPoseState)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error creating pose from geospatial pose", e)
                        null
                    }
                } else {
                    null
                }
            }

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(GlimmerTheme.colors.surface)
                    .clickable { toggleGeospatialMode() }
                    .padding(top = 56.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                DashboardHeader(geospatialState, arDeviceState)
                LocalTrackingSection(arDeviceState)
                GeospatialTrackingSection(
                    geospatialState.geospatialPose,
                    geospatialState.horizontalAccuracy,
                    geospatialState.verticalAccuracy,
                    geospatialState.orientationYawAccuracy,
                )

                Spacer(modifier = Modifier.height(8.dp))
                HudTitleText("— CONVERSION VALUES —")

                val deviceLocalPose = arDeviceState.devicePose
                HudDataText(calculateLocalConversionText(localPoseResult, deviceLocalPose))
                HudDataText(calculateGeoConversionText(geoPoseResult, deviceGeoPoseState))
            }
            Button(onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Exit",
                )
            }
        }
    }

    @Composable
    private fun DashboardHeader(
        geospatialState: Geospatial.State?,
        arDeviceState: ArDevice.State?,
    ) {
        val geoStr = geospatialState.getGeospatialStateMessage()
        val devStr = arDeviceState?.trackingState.getTrackingStateMessage()
        HudModeTitleText(
            text = targetModeState.getTargetModeTitle(),
            modifier = Modifier.padding(end = 72.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        HudDataText(
            text = "Geo: $geoStr | Device: $devStr",
            modifier = Modifier.padding(end = 72.dp),
        )
    }

    @Composable
    private fun LocalTrackingSection(arDeviceState: ArDevice.State?) {
        val t = arDeviceState?.devicePose?.translation ?: return
        val rot = arDeviceState.devicePose.rotation
        HudDataText("Local XYZ: ${t.toFormattedString()}")
        HudDataText("Local Quat: ${rot.toFormattedString()}")
    }

    @Composable
    private fun GeospatialTrackingSection(
        dispGeoPose: GeospatialPose,
        horizontalAccuracy: Double,
        verticalAccuracy: Double,
        orientationYawAccuracy: Double,
    ) {
        HudDataText(dispGeoPose.toFormattedString())
        HudDataText(
            "Acc(H/V/Yaw): %.1fm / %.1fm / %.1f°"
                .format(horizontalAccuracy, verticalAccuracy, orientationYawAccuracy)
        )
        val eusStr = dispGeoPose.eastUpSouthQuaternion.toFormattedString()
        HudDataText("EUS Quat: $eusStr")
        val orientStr = getOrientationDescription(dispGeoPose.eastUpSouthQuaternion)
        HudDataText("Orientation: $orientStr")
    }

    @Composable
    private fun HudTitleText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = modifier,
        )
    }

    @Composable
    private fun HudModeTitleText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            color = GlimmerTheme.colors.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = modifier,
        )
    }

    @Composable
    private fun HudDataText(text: String, modifier: Modifier = Modifier) {
        Text(text = text, fontSize = 16.sp, lineHeight = 20.sp, modifier = modifier)
    }

    private fun Vector3.toFormattedString(): String = "%.3f, %.3f, %.3f".format(x, y, z)

    private fun Quaternion.toFormattedString(): String = "%.3f, %.3f, %.3f, %.3f".format(x, y, z, w)

    private fun GeospatialPose.toFormattedString(): String =
        "Lat/Lon: %.6f, %.6f | Alt: %.1fm".format(latitude, longitude, altitude)

    private fun calculateLocalConversionText(
        localPoseResult: CreatePoseFromGeospatialPoseResult?,
        deviceLocalPose: Pose,
    ): String {
        return when (localPoseResult) {
            is CreatePoseFromGeospatialPoseSuccess -> {
                val convPose = localPoseResult.pose
                val diffM =
                    getTranslationDiffMeters(convPose.translation, deviceLocalPose.translation)
                val poseStatus = if (diffM <= 0.05f) "✅" else "❌"

                val diffDeg = getAngleDiffDegrees(convPose.rotation, deviceLocalPose.rotation)
                val rotStatus = if (diffDeg <= 2.0f) "✅" else "❌"
                "Local: Pose $poseStatus(${"%.3f".format(diffM)}m) | Rot $rotStatus(${"%.1f".format(diffDeg)}°)"
            }
            is CreatePoseFromGeospatialPoseNotTracking -> "Local: ERROR (Not tracking)"
            is CreatePoseFromGeospatialPoseInternalError ->
                "Local: ERROR (${localPoseResult.error})"
            else -> "Local: N/A"
        }
    }

    private fun calculateGeoConversionText(
        geoPoseResult: CreateGeospatialPoseFromPoseResult?,
        deviceGeoPoseState: GeospatialPose?,
    ): String {
        return when (geoPoseResult) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                if (deviceGeoPoseState != null) {
                    val convGeo = geoPoseResult.pose
                    // Note on Microdegrees (µ°):
                    // 1 degree of latitude is approximately 111.3 kilometers on Earth.
                    // Therefore, 1 microdegree (1 degree / 1_000_000) is approximately 11.13
                    // centimeters (~4.4 inches).
                    // We use microdegrees because raw degree differences at sub-meter scale (e.g.,
                    // 0.0000005°)
                    // would be lost or unreadable in standard formatting. Microdegrees allow us to
                    // cleanly verify
                    // sub-centimeter round-trip conversion precision using simple floating-point
                    // values.
                    val diffLatMicro =
                        kotlin.math.abs(convGeo.latitude - deviceGeoPoseState.latitude) *
                            1_000_000.0
                    val diffLonMicro =
                        kotlin.math.abs(convGeo.longitude - deviceGeoPoseState.longitude) *
                            1_000_000.0
                    val latLonStatus =
                        if (diffLatMicro <= 10.0 && diffLonMicro <= 10.0) "✅" else "❌"

                    val diffAltM = kotlin.math.abs(convGeo.altitude - deviceGeoPoseState.altitude)
                    val altStatus = if (diffAltM <= 0.05) "✅" else "❌"

                    val diffDeg =
                        getAngleDiffDegrees(
                            convGeo.eastUpSouthQuaternion,
                            deviceGeoPoseState.eastUpSouthQuaternion,
                        )
                    val rotStatus = if (diffDeg <= 2.0f) "✅" else "❌"
                    "Geo LatLong: $latLonStatus(${"%.2f".format(diffLatMicro)}µ°, ${"%.2f".format(diffLonMicro)}µ°)\nGeo Alt/Rot: Alt $altStatus(${"%.2f".format(diffAltM)}m) | Rot $rotStatus(${"%.1f".format(diffDeg)}°)"
                } else {
                    "Geo LatLong: N/A (System GeoPose null)\nGeo Alt/Rot: N/A"
                }
            }
            is CreateGeospatialPoseFromPoseNotTracking ->
                "Geo LatLong: ERROR (Not tracking)\nGeo Alt/Rot: ERROR (Not tracking)"
            is CreateGeospatialPoseFromPoseInternalError ->
                "Geo LatLong: ERROR (${geoPoseResult.error})\nGeo Alt/Rot: ERROR (${geoPoseResult.error})"
            else -> "Geo LatLong: N/A (Not running)\nGeo Alt/Rot: N/A (Not running)"
        }
    }

    private fun getAngleDiffDegrees(q1: Quaternion, q2: Quaternion): Float {
        val dot = q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w
        val absDot = kotlin.math.abs(dot).coerceIn(0f, 1f)
        return (2.0 * kotlin.math.acos(absDot.toDouble()) * (180.0 / kotlin.math.PI)).toFloat()
    }

    private fun getTranslationDiffMeters(t1: Vector3, t2: Vector3): Float {
        val dx = t1.x - t2.x
        val dy = t1.y - t2.y
        val dz = t1.z - t2.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    @OptIn(ExperimentalInertialTrackingApi::class)
    private fun TrackingState?.getTrackingStateMessage(): String {
        return when (this) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
            TrackingState.TRACKING_DEGRADED -> "DEGRADED"
            else -> "UNKNOWN"
        }
    }

    private fun Geospatial.State?.getGeospatialStateMessage(): String {
        return when (this?.geospatialTrackingState) {
            Geospatial.GeospatialTrackingState.RUNNING -> "RUNNING"
            Geospatial.GeospatialTrackingState.NOT_RUNNING -> "NOT_RUNNING"
            Geospatial.GeospatialTrackingState.ERROR_INTERNAL -> "ERROR_INTERNAL"
            Geospatial.GeospatialTrackingState.ERROR_NOT_AUTHORIZED -> "ERROR_NOT_AUTHORIZED"
            Geospatial.GeospatialTrackingState.ERROR_RESOURCE_EXHAUSTED ->
                "ERROR_RESOURCE_EXHAUSTED"
            Geospatial.GeospatialTrackingState.PAUSED -> "PAUSED"
            else -> "UNKNOWN"
        }
    }
}
