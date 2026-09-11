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

package androidx.xr.arcore.projected.testapp.tracking

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.VpsAvailabilityAvailable
import androidx.xr.arcore.VpsAvailabilityErrorInternal
import androidx.xr.arcore.VpsAvailabilityNetworkError
import androidx.xr.arcore.VpsAvailabilityNotAuthorized
import androidx.xr.arcore.VpsAvailabilityResourceExhausted
import androidx.xr.arcore.VpsAvailabilityResult
import androidx.xr.arcore.VpsAvailabilityUnavailable
import androidx.xr.arcore.projected.testapp.getOrientationDescription
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalProjectedApi::class)
class TrackingHostActivity : ComponentActivity() {

    private val viewModel: TrackingViewModel by viewModels()
    private lateinit var trackingController: TrackingController
    private var projectedActivity: TrackingProjectedActivity? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine || coarse) {
                if (
                    viewModel.isProjectedDeviceConnected.value &&
                        !trackingController.isSessionActive
                ) {
                    initializeSession()
                }
            } else {
                viewModel.setMessage(
                    "Location permissions denied. Geospatial features unavailable."
                )
            }
        }

    private val lifecycleCallbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is TrackingProjectedActivity) {
                    projectedActivity = activity
                    activity.viewModel = this@TrackingHostActivity.viewModel
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity == projectedActivity) {
                    projectedActivity = null
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        trackingController =
            TrackingController(viewModel = viewModel, coroutineScope = lifecycleScope)

        monitorProjectedConnection()

        val permissions =
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            permissionLauncher.launch(permissions)
        }

        setContent { HostView(viewModel, trackingController) }
    }

    private fun initializeSession() {
        val sessionContext =
            try {
                ProjectedContext.createProjectedDeviceContext(this)
            } catch (e: Throwable) {
                Log.w(TAG, "Projected device context not available", e)
                viewModel.setMessage("Projected device is disconnected.")
                return
            }
        lifecycleScope.launch {
            try {
                trackingController.onCreate(sessionContext, this@TrackingHostActivity)
            } catch (e: Throwable) {
                Log.e(TAG, "Error initializing session", e)
                viewModel.setMessage("Session creation error: ${e.message}")
            }
        }
    }

    private fun launchProjectedActivity() {
        try {
            val projectedContext = ProjectedContext.createProjectedDeviceContext(this)
            val intent =
                Intent(this, TrackingProjectedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val options =
                ProjectedContext.createProjectedActivityOptions(projectedContext).toBundle()
            startActivity(intent, options)
        } catch (e: Throwable) {
            Log.w(TAG, "Error launching TrackingProjectedActivity: ${e.message}")
        }
    }

    private fun monitorProjectedConnection() {
        lifecycleScope.launch {
            try {
                ProjectedContext.isProjectedDeviceConnected(
                        this@TrackingHostActivity,
                        Dispatchers.Default,
                    )
                    .collectLatest { connected ->
                        viewModel.setProjectedDeviceConnected(connected)
                        if (connected) {
                            if (projectedActivity == null) {
                                launchProjectedActivity()
                            }
                            val permissions =
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            val hasPermissions = permissions.all {
                                ContextCompat.checkSelfPermission(this@TrackingHostActivity, it) ==
                                    PackageManager.PERMISSION_GRANTED
                            }
                            if (hasPermissions && !trackingController.isSessionActive) {
                                initializeSession()
                            }
                        } else {
                            viewModel.setMessage("Projected device is disconnected.")
                            projectedActivity?.finish()
                            projectedActivity = null
                            trackingController.onDestroy()
                        }
                    }
            } catch (e: Throwable) {
                Log.w(TAG, "Error monitoring connection", e)
                viewModel.setProjectedDeviceConnected(false)
                viewModel.setMessage("Projected device is disconnected.")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingController.onDestroy()
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        projectedActivity?.finish()
    }

    @Composable
    private fun HostView(viewModel: TrackingViewModel, controller: TrackingController) {
        val currentMode by viewModel.currentMode.collectAsState()
        val isConnected by viewModel.isProjectedDeviceConnected.collectAsState()
        val message by viewModel.message.collectAsState()

        val deviceTrackingState by viewModel.deviceTrackingState.collectAsState()
        val devicePose by viewModel.devicePose.collectAsState()

        val geoState by viewModel.geospatialTrackingState.collectAsState()
        val geoPose by viewModel.geospatialPose.collectAsState()
        val hAcc by viewModel.horizontalAccuracy.collectAsState()
        val vAcc by viewModel.verticalAccuracy.collectAsState()
        val yawAcc by viewModel.orientationYawAccuracy.collectAsState()
        val vpsStatus by viewModel.vpsAvailability.collectAsState()
        val conversionResult by viewModel.conversionResult.collectAsState()

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(
                    modifier =
                        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    // Header
                    Text(
                        text = "XR Tracking & Geospatial Test Suite",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Connection and Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Active Mode: ${currentMode.shortName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                Text(
                                    text =
                                        if (isConnected) "Projected: Connected ✅"
                                        else "Projected: Disconnected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color =
                                        if (isConnected) Color(0xFF2E7D32)
                                        else MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status: $message",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Mode Selection Section
                    Text(
                        text = "Select Test Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // Mode buttons (6 modes)
                    ModeButtonRow(
                        mode1 = TestMode.HIGH_ACCURACY_GEO,
                        mode2 = TestMode.LOW_POWER_GEO,
                        currentMode = currentMode,
                        onSelect = { controller.switchMode(it) },
                    )
                    ModeButtonRow(
                        mode1 = TestMode.DEVICE_6DOF,
                        mode2 = TestMode.DEVICE_3DOF,
                        currentMode = currentMode,
                        onSelect = { controller.switchMode(it) },
                    )
                    ModeButtonRow(
                        mode1 = TestMode.GEO_ONLY,
                        mode2 = TestMode.ALL_DISABLED,
                        currentMode = currentMode,
                        onSelect = { controller.switchMode(it) },
                    )

                    // Actions Section
                    Text(
                        text = "Actions & Diagnostics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { controller.checkVpsAvailability() },
                        ) {
                            Text("Check VPS")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { controller.runConversionCheck() },
                        ) {
                            Text("Run Conversions")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Complete Telemetry Information
                    Text(
                        text = "Live Telemetry Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    // Section 1: Device Tracking
                    TelemetryCard(title = "Device Tracking (ArDevice)") {
                        Text(
                            "Tracking State: $deviceTrackingState",
                            fontWeight = FontWeight.Medium,
                        )
                        val t = devicePose.translation
                        val r = devicePose.rotation
                        Text(
                            "Position:  X:${"%.4f".format(Locale.US, t.x)}  Y:${"%.4f".format(Locale.US, t.y)}  Z:${"%.4f".format(Locale.US, t.z)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        Text(
                            "Rotation:  X:${"%.4f".format(Locale.US, r.x)}  Y:${"%.4f".format(Locale.US, r.y)}  Z:${"%.4f".format(Locale.US, r.z)}  W:${"%.4f".format(Locale.US, r.w)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }

                    // Section 2: Geospatial Tracking
                    TelemetryCard(title = "Geospatial Tracking") {
                        Text("Geospatial State: $geoState", fontWeight = FontWeight.Medium)
                        Text(
                            "Lat: ${"%.6f".format(Locale.US, geoPose.latitude)}°, Lon: ${"%.6f".format(Locale.US, geoPose.longitude)}°, Alt: ${"%.2f".format(Locale.US, geoPose.altitude)}m",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        Text(
                            "Accuracy: H: ${"%.1f".format(Locale.US, hAcc)}m | V: ${"%.1f".format(Locale.US, vAcc)}m | Yaw: ${"%.1f".format(Locale.US, yawAcc)}°",
                            fontSize = 13.sp,
                        )
                        val eus = geoPose.eastUpSouthQuaternion
                        Text(
                            "EUS Quat: X:${"%.4f".format(Locale.US, eus.x)} Y:${"%.4f".format(Locale.US, eus.y)} Z:${"%.4f".format(Locale.US, eus.z)} W:${"%.4f".format(Locale.US, eus.w)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        Text(
                            getOrientationDescription(eus),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                        Text("VPS: ${getVpsAvailabilityDescription(vpsStatus)}", fontSize = 13.sp)
                    }

                    // Section 3: Pose Conversion Verification
                    TelemetryCard(title = "Pose <-> Geospatial Round-Trip Diffs") {
                        ConversionComparisonDisplay(conversionResult)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    @Composable
    private fun ModeButtonRow(
        mode1: TestMode,
        mode2: TestMode,
        currentMode: TestMode,
        onSelect: (TestMode) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onSelect(mode1) },
                colors =
                    if (currentMode == mode1)
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    else
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
            ) {
                Text(mode1.shortName, fontSize = 13.sp)
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onSelect(mode2) },
                colors =
                    if (currentMode == mode2)
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    else
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
            ) {
                Text(mode2.shortName, fontSize = 13.sp)
            }
        }
    }

    @Composable
    private fun TelemetryCard(
        title: String,
        highlight: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                content()
            }
        }
    }

    private fun getVpsAvailabilityDescription(vpsResult: VpsAvailabilityResult?): String {
        return when (vpsResult) {
            null -> "Not checked"
            is VpsAvailabilityAvailable -> "Available"
            is VpsAvailabilityUnavailable -> "Unavailable"
            is VpsAvailabilityNetworkError -> "Error: Network error"
            is VpsAvailabilityNotAuthorized -> "Error: Not authorized"
            is VpsAvailabilityResourceExhausted -> "Error: Resource exhausted"
            is VpsAvailabilityErrorInternal -> "Error: Internal error"
            else -> "Unknown (${vpsResult::class.simpleName})"
        }
    }

    @Composable
    private fun ConversionComparisonDisplay(result: ConversionResult?) {
        if (result == null) {
            Text("No conversion run yet", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            return
        }
        if (result.message != null) {
            Text(result.message, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            return
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            result.localRoundTrip?.let { l ->
                val posPass = l.translationDiffMeters <= 0.05f
                val rotPass = l.rotationDiffDegrees <= 2.0f
                val posStatus = if (posPass) "✅" else "❌"
                val rotStatus = if (rotPass) "✅" else "❌"
                Text(
                    "Local Roundtrip:\n" +
                        "  Pos $posStatus (${"%.4f".format(Locale.US, l.translationDiffMeters)}m) | " +
                        "Rot $rotStatus (${"%.2f".format(Locale.US, l.rotationDiffDegrees)}°)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
                ?: Text(
                    "Local Roundtrip: Not available",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )

            result.geoRoundTrip?.let { g ->
                val diffLatMicro = g.latDiffDegrees * 1_000_000.0
                val diffLonMicro = g.lonDiffDegrees * 1_000_000.0
                val latLonPass = diffLatMicro <= 10.0 && diffLonMicro <= 10.0
                val altPass = g.altitudeDiffMeters <= 0.05
                val rotPass = g.rotationDiffDegrees <= 2.0f
                val latLonStatus = if (latLonPass) "✅" else "❌"
                val altStatus = if (altPass) "✅" else "❌"
                val rotStatus = if (rotPass) "✅" else "❌"
                Text(
                    "Geo Roundtrip:\n" +
                        "  Lat/Lon $latLonStatus (${"%.2f".format(Locale.US, diffLatMicro)}µ°, ${"%.2f".format(Locale.US, diffLonMicro)}µ°)\n" +
                        "  Alt $altStatus (${"%.3f".format(Locale.US, g.altitudeDiffMeters)}m) | " +
                        "Rot $rotStatus (${"%.1f".format(Locale.US, g.rotationDiffDegrees)}°)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
            }
                ?: Text(
                    "Geo Roundtrip: Waiting for valid non-zero GeospatialPose",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
        }
    }

    companion object {
        private const val TAG = "TrackingHostActivity"
    }
}
