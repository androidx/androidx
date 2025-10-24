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

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.CreatePoseFromGeospatialPoseSuccess
import androidx.xr.arcore.Earth
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureGooglePlayServicesLocationLibraryNotLinked
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
    private lateinit var earth: Earth
    private lateinit var textView: TextView
    private var initialGeospatialPose: GeospatialPose? = null
    private var vpsStatusMessage: String = "VPS status: checking..."
    private val sessionInitialized = CompletableDeferred<Unit>()
    private val TAG = "ProjectedTestAppActivity"
    val config: Config =
        Config(
            geospatial = Config.GeospatialMode.EARTH,
            deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        textView = TextView(this)
        textView.text = "\n\n\n\nWaiting for Geospatial Pose..."
        setContentView(textView)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(4000)
            tryCreateSession()
            lifecycleScope.launch {
                Log.i(TAG, "before sessionInitialized.await()")
                sessionInitialized.await()
                Log.i(TAG, "sessionInitialized.await()")
                earth = Earth.getInstance(session)
                // Check VPS availability
                checkVpsAvailability(37.422, -122.084) // Googleplex coordinates
                while (true) {
                    update()
                    delay(100)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i(TAG, "onRestart")
    }

    private fun update() {
        var newText = "\n\n\n"
        newText += getDevicePoseText()
        newText += getGeospatialPoseText()
        runOnUiThread { textView.text = newText }
    }

    private fun getDevicePoseText(): String {
        val pose = ArDevice.getInstance(session).state.value.devicePose
        return "\nDevicePose translation: ${pose.translation.x}, ${pose.translation.y}, ${pose.translation.z}" +
            "\nDevicePose rotation: ${pose.rotation.x}, ${pose.rotation.y}, ${pose.rotation.z}, ${pose.rotation.w}"
    }

    private fun getGeospatialPoseText(): String {
        when (val geospatialPoseResult = earth.createGeospatialPoseFromDevicePose()) {
            is CreateGeospatialPoseFromPoseSuccess -> {
                val currentGeospatialPose = geospatialPoseResult.pose
                val isCurrentPoseValid =
                    currentGeospatialPose.latitude != 0.0 && currentGeospatialPose.longitude != 0.0

                if (!isCurrentPoseValid) {
                    Log.w(TAG, "Skipping frame due to invalid currentGeospatialPose.")
                    return "\nWaiting for a valid Geospatial Pose..."
                }

                if (initialGeospatialPose == null) {
                    initialGeospatialPose = currentGeospatialPose
                }

                Log.i(TAG, "GeospatialPose from device pose: ${currentGeospatialPose}")

                checkVpsAvailability(
                    currentGeospatialPose.latitude,
                    currentGeospatialPose.longitude,
                )
                val comparisonMessage = testGeospatialConversions(currentGeospatialPose)

                var text = "\nGeospatialPose: ${currentGeospatialPose}"
                text += "\nVPS availability: $vpsStatusMessage"
                text += "\nComparison:\n$comparisonMessage"
                return text
            }
            else -> {
                Log.e(TAG, "Failed to get GeospatialPose from device pose: $geospatialPoseResult")
                return "\nError getting GeospatialPose: $geospatialPoseResult"
            }
        }
    }

    private fun checkVpsAvailability(latitude: Double, longitude: Double) {
        Log.i(TAG, "checkVpsAvailability latitude: $latitude, longitude: $longitude")
        lifecycleScope.launch {
            val vpsAvailabilityResult = Earth.checkVpsAvailability(session, latitude, longitude)
            vpsStatusMessage = getVpsMessage(vpsAvailabilityResult)
            Log.i(TAG, "VPS availability: $vpsStatusMessage ($vpsAvailabilityResult)")
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

        val initialNonGeoResult = earth.createPoseFromGeospatialPose(initialPose)
        val currentNonGeoResult = earth.createPoseFromGeospatialPose(currentGeospatialPose)

        if (
            initialNonGeoResult is CreatePoseFromGeospatialPoseSuccess &&
                currentNonGeoResult is CreatePoseFromGeospatialPoseSuccess
        ) {
            val initialNonGeoPose = initialNonGeoResult.pose
            val currentNonGeoPose = currentNonGeoResult.pose

            // Round trip the non-geo poses back to geospatial poses
            val initialGeoRoundtripResult = earth.createGeospatialPoseFromPose(initialNonGeoPose)
            val currentGeoRoundtripResult = earth.createGeospatialPoseFromPose(currentNonGeoPose)

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
                Log.i(TAG, "Conversion comparison:\n$message")
                return message
            } else {
                val error = "Failed to convert Pose to GeospatialPose for comparison"
                Log.e(TAG, error)
                return error
            }
        } else {
            val error = "Failed to convert GeospatialPose to Pose for comparison"
            Log.e(TAG, error)
            return error
        }
    }

    public fun tryCreateSession() {
        Log.i(TAG, "Session.create(this)")
        when (val result = Session.create(this)) {
            is SessionCreateSuccess -> {
                session = result.session
                try {
                    Log.i(TAG, "session.configure(config)")
                    when (val configResult = session.configure(config)) {
                        is SessionConfigureGooglePlayServicesLocationLibraryNotLinked -> {
                            Log.e(
                                TAG,
                                "Google Play Services Location Library is not linked, this should not happen.",
                            )
                        }

                        is SessionConfigureSuccess -> {
                            Log.i(TAG, "Session created successfully!!")
                            sessionInitialized.complete(Unit)
                        }

                        else -> {
                            Log.e(TAG, "Session creation error")
                        }
                    }
                } catch (e: UnsupportedOperationException) {
                    Log.e(TAG, "Session configuration not supported.")
                    this.finish()
                }
            }
            is SessionCreateApkRequired -> {
                Log.e(TAG, "Can't create session due to apk missing")
            }
            is SessionCreateUnsupportedDevice -> {
                Log.e(TAG, "Can't create session, unsupported device")
                finish()
            }
        }
    }
}
