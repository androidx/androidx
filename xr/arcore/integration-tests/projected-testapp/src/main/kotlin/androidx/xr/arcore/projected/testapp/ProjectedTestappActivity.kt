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
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Log
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureLibraryNotLinked
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.math.GeospatialPose
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Test app which tests projected perception API surface. */
class ProjectedTestAppActivity : ComponentActivity() {
    private lateinit var session: Session
    private lateinit var geospatial: Geospatial
    private lateinit var textView: TextView
    private var initialGeospatialPose: GeospatialPose? = null
    private var vpsStatusMessage: String = "VPS status: checking..."
    private val sessionInitialized = CompletableDeferred<Unit>()
    private var exceptionMessage: String? = null
    private val TAG = "ProjectedTestAppActivity"
    private val configs =
        listOf(
            "Geospatial On, 3DoF On" to
                Config(
                    geospatial = GeospatialMode.VPS_AND_GPS,
                    deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                ),
            "Geospatial Off, 3DoF On" to
                Config(
                    geospatial = GeospatialMode.DISABLED,
                    deviceTracking = DeviceTrackingMode.LAST_KNOWN,
                ),
            "Geospatial Off, 3DoF Off" to
                Config(
                    geospatial = GeospatialMode.DISABLED,
                    deviceTracking = DeviceTrackingMode.DISABLED,
                ),
            "Geospatial On, 3DoF Off" to
                Config(
                    geospatial = GeospatialMode.VPS_AND_GPS,
                    deviceTracking = DeviceTrackingMode.DISABLED,
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
                    Log.info("$permission is granted")
                } else {
                    Log.warn("$permission is not granted")
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
        Log.info { "onCreate" }
        textView = TextView(this)
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
                Log.info { "before sessionInitialized.await()" }
                sessionInitialized.await()
                Log.info { "sessionInitialized.await()" }
                geospatial = Geospatial.getInstance(session)
                // Check VPS availability
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
        Log.info { "onPause" }
    }

    override fun onStop() {
        super.onStop()
        Log.info { "onStop" }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.info { "onDestroy" }
    }

    override fun onRestart() {
        super.onRestart()
        Log.info { "onRestart" }
    }

    private fun update() {
        var newText = "\n\n\nCurrent config: ${configs[currentConfigIndex].first}\n"

        if (exceptionMessage != null) {
            newText += "Exception: $exceptionMessage"
            runOnUiThread { textView.text = newText }
            return
        }

        val geoOn = currentConfig.geospatial == GeospatialMode.VPS_AND_GPS
        val trackingOn = currentConfig.deviceTracking == DeviceTrackingMode.LAST_KNOWN

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
        val pose = ArDevice.getInstance(session).state.value.devicePose
        return "\nDevicePose translation: ${pose.translation.x}, ${pose.translation.y}, ${pose.translation.z}" +
            "\nDevicePose rotation: ${pose.rotation.x}, ${pose.rotation.y}, ${pose.rotation.z}, ${pose.rotation.w}"
    }

    private fun getGeospatialPoseText(): String {
        val devicePose = ArDevice.getInstance(session).state.value.devicePose
        val geospatialState = Geospatial.getInstance(session).state.value
        when (val geospatialPoseResult = geospatial.createGeospatialPoseFromPose(devicePose)) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                val currentGeospatialPose = geospatialPoseResult.pose
                val isCurrentPoseValid =
                    currentGeospatialPose.latitude != 0.0 && currentGeospatialPose.longitude != 0.0

                if (!isCurrentPoseValid) {
                    Log.warn { "Skipping frame due to invalid currentGeospatialPose." }
                    return "\nWaiting for a valid Geospatial Pose..."
                }

                if (initialGeospatialPose == null) {
                    initialGeospatialPose = currentGeospatialPose
                }

                Log.info { "GeospatialPose from device pose: ${currentGeospatialPose}" }

                checkVpsAvailability(
                    currentGeospatialPose.latitude,
                    currentGeospatialPose.longitude,
                )
                val comparisonMessage = testGeospatialConversions(currentGeospatialPose)

                var text = "\nGeospatial State: ${getGeospatialStateMessage(geospatialState)}"
                text += "\nGeospatialPose: ${currentGeospatialPose}"
                text += "\nVPS availability: $vpsStatusMessage"
                text += "\nComparison:\n$comparisonMessage"
                return text
            }
            else -> {
                Log.error { "Failed to get GeospatialPose from device pose: $geospatialPoseResult" }
                return "\nError getting GeospatialPose: $geospatialPoseResult"
            }
        }
    }

    private fun checkVpsAvailability(latitude: Double, longitude: Double) {
        Log.info { "checkVpsAvailability latitude: $latitude, longitude: $longitude" }
        lifecycleScope.launch {
            val vpsAvailabilityResult = geospatial.checkVpsAvailability(latitude, longitude)
            vpsStatusMessage = getVpsMessage(vpsAvailabilityResult)
            Log.info { "VPS availability: $vpsStatusMessage ($vpsAvailabilityResult)" }
        }
    }

    private fun getGeospatialStateMessage(geospatialState: Geospatial.State?): String {
        return when (geospatialState) {
            Geospatial.State.RUNNING -> "Running"
            Geospatial.State.NOT_RUNNING -> "Not Running"
            Geospatial.State.ERROR_INTERNAL -> "Internal Error"
            Geospatial.State.ERROR_NOT_AUTHORIZED -> "Not Authorized"
            Geospatial.State.ERROR_RESOURCE_EXHAUSTED -> "Resource Exhausted"
            Geospatial.State.PAUSED -> "Paused"
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
                Lat diff: $latDiff
                Lon diff: $lonDiff
                Alt diff: $altDiff
                ---
                X diff: $xDiff
                Y diff: $yDiff
                Z diff: $zDiff
            """
                        .trimIndent()
                Log.info { "Conversion comparison:\n$message" }
                return message
            } else {
                val error = "Failed to convert Pose to GeospatialPose for comparison"
                Log.error { error }
                return error
            }
        } else {
            val error = "Failed to convert GeospatialPose to Pose for comparison"
            Log.error { error }
            return error
        }
    }

    public fun tryCreateSession() {
        Log.info { "Session.create(this)" }
        when (val result = Session.create(this)) {
            is SessionCreateSuccess -> {
                session = result.session
                try {
                    Log.info { "session.configure(currentConfig)" }
                    when (val configResult = session.configure(currentConfig)) {
                        is SessionConfigureLibraryNotLinked -> {
                            Log.error { "Library \"${configResult.libraryName}\" not linked." }
                        }
                        is SessionConfigureSuccess -> {
                            Log.info { "Session created successfully!!" }
                        }
                        else -> {
                            Log.error { "Session creation error" }
                        }
                    }
                } catch (e: UnsupportedOperationException) {
                    Log.error(e) { "Session configuration not supported." }
                    exceptionMessage = e.message
                } finally {
                    sessionInitialized.complete(Unit)
                }
            }
            is SessionCreateApkRequired -> {
                Log.error { "Can't create session due to apk missing" }
            }
            is SessionCreateUnsupportedDevice -> {
                Log.error { "Can't create session, unsupported device" }
                finish()
            }
            else -> {
                Log.error { "Unexpected ${result::class.simpleName}" }
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER) {
            return super.onKeyUp(keyCode, event)
        }
        currentConfigIndex = (currentConfigIndex + 1) % configs.size
        val newConfigName = configs[currentConfigIndex].first
        Log.info { "Switching to config: $newConfigName" }
        exceptionMessage = null
        lifecycleScope.launch {
            sessionInitialized.await()
            Log.info { "Reconfiguring session with config: $newConfigName" }
            try {
                when (val configResult = session.configure(currentConfig)) {
                    is SessionConfigureSuccess -> {
                        Log.info { "Session reconfigured successfully!" }
                        // Reset initial pose when config changes for correct diffs
                        initialGeospatialPose = null
                    }
                    is SessionConfigureLibraryNotLinked -> {
                        Log.error { "Library \"${configResult.libraryName}\" not linked." }
                    }
                    else -> {
                        Log.error { "Session reconfigure error: $configResult" }
                    }
                }
            } catch (e: UnsupportedOperationException) {
                Log.error(e) { "Configuration failed: " }
                exceptionMessage = e.message
            }
        }
        return true
    }
}
