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

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.CreatePoseFromGeospatialPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureLibraryNotLinked
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalInertialTrackingApi::class)
internal class TrackingController(
    private val viewModel: TrackingViewModel,
    private val coroutineScope: CoroutineScope,
) {
    private var session: Session? = null
    private var deviceTrackingJob: Job? = null
    private var geospatialTrackingJob: Job? = null

    internal val isSessionActive: Boolean
        get() = session != null

    internal suspend fun onCreate(context: Context, lifecycleOwner: LifecycleOwner) {
        val message: String
        try {
            when (val result = Session.create(context = context, lifecycleOwner = lifecycleOwner)) {
                is SessionCreateSuccess -> {
                    session = result.session
                    message = "Session created successfully."
                    viewModel.setMessage(message)
                    configureSession(viewModel.currentMode.value)
                }
                is SessionCreateApkRequired -> {
                    message = "Session creation failed: ${result.requiredApk} is required."
                    viewModel.setMessage(message)
                }
                is SessionCreateUnsupportedDevice -> {
                    message = "Session creation failed: Device is not supported."
                    viewModel.setMessage(message)
                }
                is SessionCreateUnknownError -> {
                    message = "Session creation failed: ${result.errorMessage}"
                    viewModel.setMessage(message)
                }
                else -> {
                    message = "Session creation failed: ${result::class.simpleName}"
                    viewModel.setMessage(message)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Exception creating session", e)
            viewModel.setMessage("Session creation error: ${e.message}")
        }
    }

    internal fun onDestroy() {
        stopAllTrackingJobs()
        session = null
    }

    private fun stopAllTrackingJobs() {
        deviceTrackingJob?.cancel()
        deviceTrackingJob = null
        geospatialTrackingJob?.cancel()
        geospatialTrackingJob = null
    }

    internal fun switchMode(mode: TestMode) {
        viewModel.setCurrentMode(mode)
        viewModel.setConversionResult(null)
        viewModel.setVpsAvailability(null)

        val currentSession = session
        if (currentSession == null) {
            viewModel.setMessage("Projected device disconnected. Mode will apply once connected.")
            return
        }
        stopAllTrackingJobs()
        configureSession(mode)
    }

    private fun configureSession(mode: TestMode) {
        val currentSession = session ?: return
        val configBuilder = Config.Builder()

        when (mode) {
            TestMode.HIGH_ACCURACY_GEO -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setGeospatial(GeospatialMode.SPATIAL)
            }
            TestMode.LOW_POWER_GEO -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setGeospatial(GeospatialMode.INERTIAL)
            }
            TestMode.DEVICE_6DOF -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setGeospatial(GeospatialMode.DISABLED)
            }
            TestMode.DEVICE_3DOF -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.INERTIAL)
                    .setGeospatial(GeospatialMode.DISABLED)
            }
            TestMode.GEO_ONLY -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.DISABLED)
                    .setGeospatial(GeospatialMode.SPATIAL)
            }
            TestMode.ALL_DISABLED -> {
                configBuilder
                    .setDeviceTracking(DeviceTrackingMode.DISABLED)
                    .setGeospatial(GeospatialMode.DISABLED)
            }
        }

        val config = configBuilder.build()
        try {
            when (val configResult = currentSession.configure(config)) {
                is SessionConfigureSuccess -> {
                    viewModel.setMessage("Configured: ${mode.displayName}")
                    startTrackingForMode(currentSession, mode)
                }
                is SessionConfigureLibraryNotLinked -> {
                    viewModel.setMessage(
                        "Configure error: Library \"${configResult.libraryName}\" not linked."
                    )
                }
                else -> {
                    viewModel.setMessage(
                        "Session configuration failed: ${configResult::class.simpleName}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception configuring session for mode $mode", e)
            viewModel.setMessage("Configure Exception: ${e.message}")
        }
    }

    private fun startTrackingForMode(currentSession: Session, mode: TestMode) {
        val hasDeviceTracking =
            mode in
                listOf(
                    TestMode.HIGH_ACCURACY_GEO,
                    TestMode.LOW_POWER_GEO,
                    TestMode.DEVICE_6DOF,
                    TestMode.DEVICE_3DOF,
                )
        val hasGeospatial =
            mode in
                listOf(
                    TestMode.HIGH_ACCURACY_GEO,
                    TestMode.LOW_POWER_GEO,
                    TestMode.GEO_ONLY,
                )

        if (hasDeviceTracking) {
            try {
                val arDevice = ArDevice.getInstance(currentSession)
                deviceTrackingJob = coroutineScope.launch {
                    arDevice.state.collect { state ->
                        viewModel.setArDeviceState(state.trackingState, state.devicePose)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring ArDevice for mode $mode", e)
                viewModel.setArDeviceState(TrackingState.STOPPED, Pose())
            }
        } else {
            viewModel.setArDeviceState(TrackingState.STOPPED, Pose())
        }

        if (hasGeospatial) {
            try {
                val geospatial = Geospatial.getInstance(currentSession)
                geospatialTrackingJob = coroutineScope.launch {
                    geospatial.state.collect { state ->
                        viewModel.setGeospatialState(
                            state.geospatialTrackingState,
                            state.geospatialPose,
                            state.horizontalAccuracy,
                            state.verticalAccuracy,
                            state.orientationYawAccuracy,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring Geospatial for mode $mode", e)
                viewModel.setGeospatialState(
                    Geospatial.GeospatialTrackingState.NOT_RUNNING,
                    GeospatialPose(),
                    0.0,
                    0.0,
                    0.0,
                )
            }
        } else {
            viewModel.setGeospatialState(
                Geospatial.GeospatialTrackingState.NOT_RUNNING,
                GeospatialPose(),
                0.0,
                0.0,
                0.0,
            )
        }
    }

    internal fun checkVpsAvailability() {
        val currentSession = session
        if (currentSession == null) {
            viewModel.setMessage("Projected device disconnected.")
            viewModel.setVpsAvailability(null)
            return
        }
        coroutineScope.launch {
            try {
                val geospatial = Geospatial.getInstance(currentSession)
                val currentPose = geospatial.state.value.geospatialPose
                val (lat, lon) =
                    if (currentPose.latitude != 0.0 || currentPose.longitude != 0.0) {
                        currentPose.latitude to currentPose.longitude
                    } else {
                        // Googleplex fallback coordinates if current pose not yet acquired
                        37.422 to -122.084
                    }
                val result = geospatial.checkVpsAvailability(lat, lon)
                viewModel.setVpsAvailability(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking VPS availability", e)
                viewModel.setMessage("VPS check exception: ${e.message}")
            }
        }
    }

    internal fun runConversionCheck() {
        val currentSession = session
        if (currentSession == null) {
            viewModel.setConversionResult(
                ConversionResult(message = "Unavailable (Projected device disconnected)")
            )
            return
        }
        coroutineScope.launch {
            try {
                val currentMode = viewModel.currentMode.value
                val hasDeviceTracking =
                    currentMode in
                        listOf(
                            TestMode.HIGH_ACCURACY_GEO,
                            TestMode.LOW_POWER_GEO,
                            TestMode.DEVICE_6DOF,
                            TestMode.DEVICE_3DOF,
                        )
                val hasGeospatial =
                    currentMode in
                        listOf(
                            TestMode.HIGH_ACCURACY_GEO,
                            TestMode.LOW_POWER_GEO,
                            TestMode.GEO_ONLY,
                        )
                if (!hasDeviceTracking || !hasGeospatial) {
                    viewModel.setConversionResult(
                        ConversionResult(
                            message =
                                "Both Device Tracking and Geospatial must be enabled to run pose conversions."
                        )
                    )
                    return@launch
                }

                val arDevice = ArDevice.getInstance(currentSession)
                val geospatial = Geospatial.getInstance(currentSession)
                val deviceLocalPose = arDevice.state.value.devicePose
                val currentGeoPose = geospatial.state.value.geospatialPose

                // Round-trip 1: Pose -> GeospatialPose -> Pose
                val geoFromPoseResult = geospatial.createGeospatialPoseFromPose(deviceLocalPose)
                val localRoundTrip =
                    if (geoFromPoseResult is CreateGeospatialPoseFromPoseSuccess) {
                        val convertedGeoPose = geoFromPoseResult.pose
                        val poseFromGeoResult =
                            geospatial.createPoseFromGeospatialPose(convertedGeoPose)
                        if (poseFromGeoResult is CreatePoseFromGeospatialPoseSuccess) {
                            val roundTripPose = poseFromGeoResult.pose
                            val diffMeters =
                                getTranslationDiffMeters(
                                    deviceLocalPose.translation,
                                    roundTripPose.translation,
                                )
                            val diffDegrees =
                                getAngleDiffDegrees(
                                    deviceLocalPose.rotation,
                                    roundTripPose.rotation,
                                )
                            LocalRoundTripResult(
                                originalPose = deviceLocalPose,
                                convertedGeoPose = convertedGeoPose,
                                roundTripPose = roundTripPose,
                                translationDiffMeters = diffMeters,
                                rotationDiffDegrees = diffDegrees,
                            )
                        } else null
                    } else null

                // Round-trip 2: GeospatialPose -> Pose -> GeospatialPose
                val geoRoundTrip =
                    if (currentGeoPose.latitude != 0.0 || currentGeoPose.longitude != 0.0) {
                        val poseFromGeoResult =
                            geospatial.createPoseFromGeospatialPose(currentGeoPose)
                        if (poseFromGeoResult is CreatePoseFromGeospatialPoseSuccess) {
                            val convertedPose = poseFromGeoResult.pose
                            val geoFromPoseResult2 =
                                geospatial.createGeospatialPoseFromPose(convertedPose)
                            if (geoFromPoseResult2 is CreateGeospatialPoseFromPoseSuccess) {
                                val roundTripGeo = geoFromPoseResult2.pose
                                val diffLat = abs(roundTripGeo.latitude - currentGeoPose.latitude)
                                val diffLon = abs(roundTripGeo.longitude - currentGeoPose.longitude)
                                val diffAltM = abs(roundTripGeo.altitude - currentGeoPose.altitude)
                                val diffRotDeg =
                                    getAngleDiffDegrees(
                                        roundTripGeo.eastUpSouthQuaternion,
                                        currentGeoPose.eastUpSouthQuaternion,
                                    )
                                GeoRoundTripResult(
                                    originalGeoPose = currentGeoPose,
                                    convertedPose = convertedPose,
                                    roundTripGeoPose = roundTripGeo,
                                    latDiffDegrees = diffLat,
                                    lonDiffDegrees = diffLon,
                                    altitudeDiffMeters = diffAltM,
                                    rotationDiffDegrees = diffRotDeg,
                                )
                            } else null
                        } else null
                    } else null

                viewModel.setConversionResult(
                    ConversionResult(
                        localRoundTrip = localRoundTrip,
                        geoRoundTrip = geoRoundTrip,
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception running conversions", e)
                viewModel.setConversionResult(
                    ConversionResult(message = "Conversion error: ${e.message}")
                )
            }
        }
    }

    private fun getAngleDiffDegrees(q1: Quaternion, q2: Quaternion): Float {
        val dot = q1.x * q2.x + q1.y * q2.y + q1.z * q2.z + q1.w * q2.w
        val absDot = abs(dot).coerceIn(0f, 1f)
        return (2.0 * acos(absDot.toDouble()) * (180.0 / Math.PI)).toFloat()
    }

    private fun getTranslationDiffMeters(t1: Vector3, t2: Vector3): Float {
        val dx = t1.x - t2.x
        val dy = t1.y - t2.y
        val dz = t1.z - t2.z
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    companion object {
        private const val TAG = "TrackingController"
    }
}
