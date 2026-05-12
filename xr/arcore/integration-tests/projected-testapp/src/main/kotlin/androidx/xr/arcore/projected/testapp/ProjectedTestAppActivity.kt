/*
 * Copyright 2025 The Android Open Source Project
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
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.CreatePoseFromGeospatialPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.GeospatialState
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.VpsAvailabilityAvailable
import androidx.xr.arcore.VpsAvailabilityErrorInternal
import androidx.xr.arcore.VpsAvailabilityNetworkError
import androidx.xr.arcore.VpsAvailabilityNotAuthorized
import androidx.xr.arcore.VpsAvailabilityResourceExhausted
import androidx.xr.arcore.VpsAvailabilityResult
import androidx.xr.arcore.VpsAvailabilityUnavailable
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureLibraryNotLinked
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.GeospatialPose
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test app which tests projected perception API surface. */
@OptIn(PreviewSpatialApi::class)
class ProjectedTestAppActivity : ComponentActivity() {
    private lateinit var session: Session
    private lateinit var geospatial: Geospatial
    private lateinit var textView: TextView
    private var initialGeospatialPose: GeospatialPose? = null
    private var vpsStatusMessage: String = "VPS status: checking..."
    private val sessionInitialized = CompletableDeferred<Unit>()
    private var exceptionMessage: String? = null
    private val configs =
        listOf(
            "Geospatial On, 6DoF On" to
                Config(
                    geospatial = GeospatialMode.SPATIAL,
                    deviceTracking = DeviceTrackingMode.SPATIAL,
                ),
            "Geospatial Off, 6DoF On" to
                Config(
                    geospatial = GeospatialMode.DISABLED,
                    deviceTracking = DeviceTrackingMode.SPATIAL,
                ),
            "Geospatial Off, 3DoF On" to
                Config(
                    geospatial = GeospatialMode.DISABLED,
                    deviceTracking = DeviceTrackingMode.INERTIAL,
                ),
            "Geospatial Off, Device Tracking Off" to
                Config(
                    geospatial = GeospatialMode.DISABLED,
                    deviceTracking = DeviceTrackingMode.DISABLED,
                ),
            "Geospatial On, Device Tracking Off" to
                Config(
                    geospatial = GeospatialMode.SPATIAL,
                    deviceTracking = DeviceTrackingMode.DISABLED,
                ),
            "Geospatial Low Power" to
                Config(
                    geospatial = GeospatialMode.INERTIAL,
                    deviceTracking = DeviceTrackingMode.SPATIAL,
                ),
        )
    private var currentConfigIndex = 0
    private val currentConfig: Config
        get() = configs[currentConfigIndex].second

    private val permissionsRequired =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    @OptIn(ExperimentalProjectedApi::class)
    private val requestPermissionLauncher:
        ActivityResultLauncher<List<ProjectedPermissionsRequestParams>> =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            if (
                results[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                    results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                onPermissionGranted()
            }
            var permissionDeniedText = ""
            for (permission in permissionsRequired) {
                if (results[permission] == true) {
                    Log.i("JetpackXR", "$permission is granted")
                } else {
                    Log.w("JetpackXR", "$permission is not granted")
                    permissionDeniedText += "Please grant $permission permission.\n"
                }
            }
            if (permissionDeniedText.isNotEmpty()) {
                runOnUiThread {
                    textView.text = "\n\n\n Cannot start Session.\n$permissionDeniedText"
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("JetpackXR", "onCreate")
        textView = TextView(this)
        textView.setBackgroundColor(android.graphics.Color.BLACK)
        textView.setTextColor(android.graphics.Color.WHITE)
        textView.text = "\n\n\n\nWaiting for Geospatial Pose..."
        setContentView(textView)
        if (!hasPermissions()) {
            requestPermissions()
        } else {
            onPermissionGranted()
        }
    }

    private fun onPermissionGranted() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(4000) // TODO: b/436981970 - the onResume 2x is happening again with this change.
            tryCreateSession()
            lifecycleScope.launch {
                Log.i("JetpackXR", "before sessionInitialized.await()")
                sessionInitialized.await()
                Log.i("JetpackXR", "sessionInitialized.await()")
                geospatial = Geospatial.getInstance(session)
                checkVpsAvailability(37.422, -122.084) // Googleplex coordinates
                while (true) {
                    update()
                    delay(100)
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissionsRequired) {
            if (
                ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalProjectedApi::class)
    private fun requestPermissions() {
        val params =
            ProjectedPermissionsRequestParams(
                permissions = permissionsRequired,
                rationale = "Location permission is required to determine your geospatial pose.",
            )
        requestPermissionLauncher.launch(listOf(params))
    }

    override fun onPause() {
        super.onPause()
        Log.i("JetpackXR", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("JetpackXR", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("JetpackXR", "onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("JetpackXR", "onRestart")
    }

    private fun update() {
        var newText = "\n\n\nCurrent config: ${configs[currentConfigIndex].first}\n"

        if (exceptionMessage != null) {
            newText += "Exception: $exceptionMessage"
            runOnUiThread { textView.text = newText }
            return
        }

        val geoOn =
            currentConfig.geospatial == GeospatialMode.SPATIAL ||
                currentConfig.geospatial == GeospatialMode.INERTIAL
        val trackingOn = currentConfig.deviceTracking != DeviceTrackingMode.DISABLED

        if (geoOn && trackingOn) {
            newText += getDevicePoseText()
            newText += getGeospatialPoseText()
        } else if (!geoOn && trackingOn) {
            newText += getDevicePoseText()
        } else if (!geoOn && !trackingOn) {
            newText += "All tracking is disabled."
        }
        runOnUiThread { textView.text = newText }
    }

    private fun getDevicePoseText(): String {
        val state = ArDevice.getInstance(session).state.value
        val pose = state.devicePose
        val t = pose.translation
        val r = pose.rotation
        return "\nTracking State: ${getTrackingStateMessage(state.trackingState)}" +
            "\nDevicePose translation: ${t.x.fmt()}, ${t.y.fmt()}, ${t.z.fmt()}" +
            "\nDevicePose rotation: ${r.x.fmt()}, ${r.y.fmt()}, ${r.z.fmt()}, ${r.w.fmt()}"
    }

    private fun getGeospatialPoseText(): String {
        val devicePose = ArDevice.getInstance(session).state.value.devicePose
        val geospatialState = Geospatial.getInstance(session).state.value
        if (geospatialState != GeospatialState.RUNNING) {
            return "\nGeospatial State: ${getGeospatialStateMessage(geospatialState)} (Waiting for Earth...)"
        }

        when (val geospatialPoseResult = geospatial.createGeospatialPoseFromPose(devicePose)) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                val geoPose = geospatialPoseResult.pose
                val lat = geoPose.latitude
                val lon = geoPose.longitude
                val alt = geoPose.altitude
                val eus = geoPose.eastUpSouthQuaternion
                if (lat == 0.0 && lon == 0.0) {
                    Log.w("JetpackXR", "Skipping frame due to invalid currentGeospatialPose.")
                    return "\nWaiting for a valid Geospatial Pose..."
                }

                if (initialGeospatialPose == null) {
                    initialGeospatialPose = geoPose
                }

                Log.i("JetpackXR", "GeospatialPose from device pose: $geoPose")

                checkVpsAvailability(lat, lon)
                val comparisonMessage = testGeospatialConversions(geoPose)

                var text =
                    "\nGeospatial GeospatialState: ${getGeospatialStateMessage(geospatialState)}"
                text += "\nGeospatialPose: Lat/Lon: ${lat.fmt(6)}, ${lon.fmt(6)}, Alt: ${alt.fmt()}"
                text += "\nEUS Quat: ${eus.x.fmt()}, ${eus.y.fmt()}, ${eus.z.fmt()}, ${eus.w.fmt()}"
                text += "\nVPS availability: $vpsStatusMessage"
                text += "\nComparison:\n$comparisonMessage"
                return text
            }
            else -> {
                Log.e(
                    "JetpackXR",
                    "Failed to get GeospatialPose from device pose: $geospatialPoseResult",
                )
                return "\nError getting GeospatialPose: $geospatialPoseResult"
            }
        }
    }

    private fun checkVpsAvailability(latitude: Double, longitude: Double) {
        Log.i("JetpackXR", "checkVpsAvailability latitude: $latitude, longitude: $longitude")
        lifecycleScope.launch {
            val vpsAvailabilityResult = geospatial.checkVpsAvailability(latitude, longitude)
            vpsStatusMessage = getVpsMessage(vpsAvailabilityResult)
            Log.i("JetpackXR", "VPS availability: ${vpsStatusMessage} ($vpsAvailabilityResult)")
        }
    }

    private fun getTrackingStateMessage(trackingState: TrackingState?): String {
        return when (trackingState) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
            TrackingState.TRACKING_DEGRADED -> "TRACKING_DEGRADED"
            else -> "TrackingState(unknown)"
        }
    }

    private fun getGeospatialStateMessage(geospatialState: GeospatialState?): String {
        return when (geospatialState) {
            GeospatialState.RUNNING -> "Running"
            GeospatialState.NOT_RUNNING -> "Not Running"
            GeospatialState.ERROR_INTERNAL -> "Internal Error"
            GeospatialState.ERROR_NOT_AUTHORIZED -> "Not Authorized"
            GeospatialState.ERROR_RESOURCE_EXHAUSTED -> "Resource Exhausted"
            GeospatialState.PAUSED -> "Paused"
            else -> "Checking..."
        }
    }

    private fun getVpsMessage(vpsAvailabilityResult: VpsAvailabilityResult?): String {
        return when (vpsAvailabilityResult) {
            is VpsAvailabilityAvailable -> "VPS is available."
            is VpsAvailabilityErrorInternal ->
                "VPS availability check failed with an internal error."
            is VpsAvailabilityNetworkError ->
                "VPS availability check failed due to a network error."
            is VpsAvailabilityNotAuthorized ->
                "VPS availability check failed due to an authorization error."
            is VpsAvailabilityResourceExhausted ->
                "VPS availability check failed due to resource exhaustion."
            is VpsAvailabilityUnavailable -> "VPS is unavailable."
            null -> "VPS status: checking..."
            else -> "VPS status: unknown."
        }
    }

    private fun testGeospatialConversions(currentGeospatialPose: GeospatialPose): String {
        val initialPose = initialGeospatialPose ?: return "Initial pose not set"

        val initialNonGeoResult = geospatial.createPoseFromGeospatialPose(initialPose)
        val currentNonGeoResult = geospatial.createPoseFromGeospatialPose(currentGeospatialPose)

        if (
            initialNonGeoResult is CreatePoseFromGeospatialPoseSuccess &&
                currentNonGeoResult is CreatePoseFromGeospatialPoseSuccess
        ) {
            val initialNonGeoPose = initialNonGeoResult.pose
            val currentNonGeoPose = currentNonGeoResult.pose

            // Round trip the non-geo poses back to geospatial poses
            val initialGeoRoundtripResult =
                geospatial.createGeospatialPoseFromPose(initialNonGeoPose)
            val currentGeoRoundtripResult =
                geospatial.createGeospatialPoseFromPose(currentNonGeoPose)

            if (
                initialGeoRoundtripResult is CreateGeospatialPoseFromPoseSuccess &&
                    currentGeoRoundtripResult is CreateGeospatialPoseFromPoseSuccess
            ) {
                val initialRoundtripGeoPose = initialGeoRoundtripResult.pose
                val currentRoundtripGeoPose = currentGeoRoundtripResult.pose

                // Compare lat/lon/alt from the round-tripped data
                val latDiff = currentRoundtripGeoPose.latitude - initialRoundtripGeoPose.latitude
                val lonDiff = currentRoundtripGeoPose.longitude - initialRoundtripGeoPose.longitude
                val altDiff = currentRoundtripGeoPose.altitude - initialRoundtripGeoPose.altitude

                // Compare non-geo x/y/z
                val xDiff = currentNonGeoPose.translation.x - initialNonGeoPose.translation.x
                val yDiff = currentNonGeoPose.translation.y - initialNonGeoPose.translation.y
                val zDiff = currentNonGeoPose.translation.z - initialNonGeoPose.translation.z

                val message =
                    """
                ΔLat/Lon: ${latDiff.fmt(6)}, ${lonDiff.fmt(6)}
                ΔAlt: ${altDiff.fmt()}
                ---
                ΔXYZ: ${xDiff.fmt()}, ${yDiff.fmt()}, ${zDiff.fmt()}
            """
                        .trimIndent()
                Log.i("JetpackXR", "Conversion comparison:\n$message")
                return message
            } else {
                val error = "Failed to convert Pose to GeospatialPose for comparison"
                Log.e("JetpackXR", error)
                return error
            }
        } else {
            val error = "Failed to convert GeospatialPose to Pose for comparison"
            Log.e("JetpackXR", error)
            return error
        }
    }

    public fun tryCreateSession() {
        Log.i("JetpackXR", "Session.create(this)")
        when (val result = Session.create(context = this)) {
            is SessionCreateSuccess -> {
                session = result.session
                try {
                    Log.i("JetpackXR", "session.configure(currentConfig)")
                    when (val configResult = session.configure(currentConfig)) {
                        is SessionConfigureLibraryNotLinked -> {
                            Log.e(
                                "JetpackXR",
                                "Library \"${configResult.libraryName}\" not linked.",
                            )
                        }

                        is SessionConfigureSuccess -> {
                            Log.i("JetpackXR", "Session created successfully!!")
                        }

                        else -> {
                            Log.e("JetpackXR", "Session creation error")
                        }
                    }
                } catch (e: UnsupportedOperationException) {
                    Log.e("JetpackXR", "Session configuration not supported.", e)
                    exceptionMessage = e.message
                } finally {
                    sessionInitialized.complete(Unit)
                }
            }

            is SessionCreateApkRequired -> {
                Log.e("JetpackXR", "Can't create session due to apk missing")
            }

            is SessionCreateUnsupportedDevice -> {
                Log.e("JetpackXR", "Can't create session, unsupported device")
                finish()
            }

            else -> {
                Log.e("JetpackXR", "Unexpected ${result::class.simpleName}")
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
            return super.onKeyUp(keyCode, event)
        }
        currentConfigIndex = (currentConfigIndex + 1) % configs.size
        val newConfigName = configs[currentConfigIndex].first
        Log.i("JetpackXR", "Switching to config: $newConfigName")
        exceptionMessage = null
        lifecycleScope.launch {
            sessionInitialized.await()
            Log.i("JetpackXR", "Reconfiguring session with config: $newConfigName")
            try {
                when (val configResult = session.configure(currentConfig)) {
                    is SessionConfigureSuccess -> {
                        Log.i("JetpackXR", "Session reconfigured successfully!")
                        // Reset initial pose when config changes for correct diffs
                        initialGeospatialPose = null
                    }

                    is SessionConfigureLibraryNotLinked -> {
                        Log.e("JetpackXR", "Library \"${configResult.libraryName}\" not linked.")
                    }

                    else -> {
                        Log.e("JetpackXR", "Session reconfigure error: $configResult")
                    }
                }
            } catch (e: UnsupportedOperationException) {
                Log.e("JetpackXR", "Configuration failed: ", e)
                exceptionMessage = e.message
            }
        }
        return true
    }

    private fun Float.fmt(digits: Int = 3) = String.format(Locale.US, "%.${digits}f", this)

    private fun Double.fmt(digits: Int = 3) = String.format(Locale.US, "%.${digits}f", this)
}
