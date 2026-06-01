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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
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
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import kotlinx.coroutines.launch

@OptIn(PreviewSpatialApi::class)
class LowPowerGeospatialActivity : ComponentActivity() {
    companion object {
        private const val TAG = "LowPowerGeospatialActivity"
    }

    private var targetModeState by mutableStateOf(GeospatialMode.SPATIAL)

    private fun GeospatialMode.getTargetModeName(): String {
        return if (this == GeospatialMode.INERTIAL) "INERTIAL" else "SPATIAL"
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

    @OptIn(PreviewSpatialApi::class)
    @Composable
    private fun GeospatialDashboard(geospatial: Geospatial, arDevice: ArDevice) {
        val geospatialState by geospatial.state.collectAsState()
        val arDeviceState by arDevice.state.collectAsState()
        val localizationStatusResult =
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
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(GlimmerTheme.colors.surface)
                    .clickable { toggleGeospatialMode() }
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val geospatialTrackingMessage = geospatialState.getGeospatialStateMessage()
                val deviceTrackingMessage = arDeviceState.trackingState.getTrackingStateMessage()
                val translation = arDeviceState.devicePose.translation
                val rotation = arDeviceState.devicePose.rotation
                Text(
                    "Mode: ${targetModeState.getTargetModeName()} (Tap/Click to switch)",
                    color = GlimmerTheme.colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("Geospatial Tracking: $geospatialTrackingMessage", fontSize = 16.sp)
                Text("Device Tracking: $deviceTrackingMessage", fontSize = 16.sp)
                Text(
                    "Local XYZ: ${"%.3f".format(translation.x)}, ${"%.3f".format(translation.y)}, ${"%.3f".format(translation.z)}",
                    fontSize = 16.sp,
                )
                Text(getOrientationDescription(rotation), fontSize = 16.sp)
                val localizationResult = localizationStatusResult
                if (localizationResult !is CreateGeospatialPoseFromPoseSuccess) {
                    Text("Lat/Lon: N/A", fontSize = 16.sp)
                    Text("Alt: N/A", fontSize = 16.sp)
                    Text("Acc(H/V/Y): N/A", fontSize = 16.sp)
                    Text("EUS Quat: N/A", fontSize = 16.sp)
                    Text("RT delta XYZ: N/A", fontSize = 16.sp)
                    return@Column
                }
                val geospatialPose = localizationResult.pose
                val horizontalAccuracy = localizationResult.horizontalAccuracy
                val verticalAccuracy = localizationResult.verticalAccuracy
                val yawAccuracy = localizationResult.orientationYawAccuracy
                val eastUpSouthQuaternion = geospatialPose.eastUpSouthQuaternion
                Text(
                    "Lat/Lon: ${"%.6f".format(geospatialPose.latitude)}, ${"%.6f".format(geospatialPose.longitude)}",
                    fontSize = 16.sp,
                )
                Text("Alt: ${"%.1f".format(geospatialPose.altitude)} m", fontSize = 16.sp)
                Text(
                    "Acc(H/V/Y): ${"%.1f".format(horizontalAccuracy)}m / ${"%.1f".format(verticalAccuracy)}m / ${"%.1f".format(yawAccuracy)}°",
                    fontSize = 16.sp,
                )
                Text(
                    "EUS Quat: ${"%.2f".format(eastUpSouthQuaternion.x)}, ${"%.2f".format(eastUpSouthQuaternion.y)}, ${"%.2f".format(eastUpSouthQuaternion.z)}, ${"%.2f".format(eastUpSouthQuaternion.w)}",
                    fontSize = 16.sp,
                )
                val nonGeospatialResult =
                    remember(geospatialPose) {
                        geospatial.createPoseFromGeospatialPose(geospatialPose)
                    }
                val runtimeDeltaText =
                    if (nonGeospatialResult is CreatePoseFromGeospatialPoseSuccess) {
                        val originalTranslation = arDeviceState.devicePose.translation
                        val runtimeTranslation = nonGeospatialResult.pose.translation
                        "RT delta XYZ: ${"%.3f".format(runtimeTranslation.x - originalTranslation.x)}, ${"%.3f".format(runtimeTranslation.y - originalTranslation.y)}, ${"%.3f".format(runtimeTranslation.z - originalTranslation.z)}"
                    } else {
                        "RT delta XYZ: N/A"
                    }
                Text(runtimeDeltaText, fontSize = 16.sp)
            }
            Button(onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Exit",
                )
            }
        }
    }

    private fun TrackingState?.getTrackingStateMessage(): String {
        return when (this) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
            TrackingState.TRACKING_DEGRADED -> "DEGRADED"
            else -> "Unknown"
        }
    }

    private fun Geospatial.State?.getGeospatialStateMessage(): String {
        return when (this?.geospatialTrackingState) {
            Geospatial.GeospatialTrackingState.RUNNING -> "Running (Tracking)"
            Geospatial.GeospatialTrackingState.NOT_RUNNING -> "Not Running"
            Geospatial.GeospatialTrackingState.ERROR_INTERNAL -> "Internal Error"
            Geospatial.GeospatialTrackingState.ERROR_NOT_AUTHORIZED -> "Not Authorized"
            Geospatial.GeospatialTrackingState.ERROR_RESOURCE_EXHAUSTED -> "Resource Exhausted"
            Geospatial.GeospatialTrackingState.PAUSED -> "Paused"
            else -> "Checking..."
        }
    }
}
