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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.VpsAvailabilityResult
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Modes representing the previous separate test cases unified into one. */
enum class TestMode(val displayName: String, val shortName: String) {
    HIGH_ACCURACY_GEO(
        displayName = "High Accuracy Geospatial (6DoF + Spatial Geo)",
        shortName = "High-Acc Geo",
    ),
    LOW_POWER_GEO(
        displayName = "Low Power Geospatial (6DoF + Inertial Geo)",
        shortName = "Low-Power Geo",
    ),
    DEVICE_6DOF(
        displayName = "Device 6DoF Spatial (Geo Disabled)",
        shortName = "6DoF Spatial",
    ),
    DEVICE_3DOF(
        displayName = "Device 3DoF Inertial (Geo Disabled)",
        shortName = "3DoF Inertial",
    ),
    GEO_ONLY(
        displayName = "Geospatial Only (Device Tracking Disabled)",
        shortName = "Geo Only",
    ),
    ALL_DISABLED(
        displayName = "All Tracking Disabled",
        shortName = "All Disabled",
    ),
}

/** Results of verifying round-trip conversions between local Pose and GeospatialPose. */
data class LocalRoundTripResult(
    val originalPose: Pose,
    val convertedGeoPose: GeospatialPose,
    val roundTripPose: Pose,
    val translationDiffMeters: Float,
    val rotationDiffDegrees: Float,
)

data class GeoRoundTripResult(
    val originalGeoPose: GeospatialPose,
    val convertedPose: Pose,
    val roundTripGeoPose: GeospatialPose,
    val latDiffDegrees: Double,
    val lonDiffDegrees: Double,
    val altitudeDiffMeters: Double,
    val rotationDiffDegrees: Float,
)

data class ConversionResult(
    val localRoundTrip: LocalRoundTripResult? = null,
    val geoRoundTrip: GeoRoundTripResult? = null,
    val message: String? = null,
)

internal class TrackingViewModel : ViewModel() {

    // --- High-level Configuration & Status ---
    private val _currentMode = MutableStateFlow(TestMode.HIGH_ACCURACY_GEO)
    private val _message = MutableStateFlow("Projected device is disconnected.")
    private val _isProjectedDeviceConnected = MutableStateFlow(false)

    // --- Device Tracking Telemetry ---
    private val _deviceTrackingState = MutableStateFlow(TrackingState.STOPPED)
    private val _devicePose = MutableStateFlow(Pose())

    // --- Geospatial Telemetry ---
    private val _geospatialTrackingState =
        MutableStateFlow(Geospatial.GeospatialTrackingState.NOT_RUNNING)
    private val _geospatialPose = MutableStateFlow(GeospatialPose())
    private val _horizontalAccuracy = MutableStateFlow(0.0)
    private val _verticalAccuracy = MutableStateFlow(0.0)
    private val _orientationYawAccuracy = MutableStateFlow(0.0)
    private val _vpsAvailability = MutableStateFlow<VpsAvailabilityResult?>(null)
    private val _conversionResult = MutableStateFlow<ConversionResult?>(null)

    // --- Public Read-Only State Flows ---
    internal val currentMode = _currentMode.asStateFlow()
    internal val message = _message.asStateFlow()
    internal val isProjectedDeviceConnected = _isProjectedDeviceConnected.asStateFlow()

    internal val deviceTrackingState = _deviceTrackingState.asStateFlow()
    internal val devicePose = _devicePose.asStateFlow()

    internal val geospatialTrackingState = _geospatialTrackingState.asStateFlow()
    internal val geospatialPose = _geospatialPose.asStateFlow()
    internal val horizontalAccuracy = _horizontalAccuracy.asStateFlow()
    internal val verticalAccuracy = _verticalAccuracy.asStateFlow()
    internal val orientationYawAccuracy = _orientationYawAccuracy.asStateFlow()
    internal val vpsAvailability = _vpsAvailability.asStateFlow()
    internal val conversionResult = _conversionResult.asStateFlow()

    // --- State Update Methods ---
    internal fun setCurrentMode(mode: TestMode) {
        viewModelScope.launch { _currentMode.emit(mode) }
    }

    internal fun setMessage(msg: String) {
        viewModelScope.launch { _message.emit(msg) }
    }

    internal fun setProjectedDeviceConnected(connected: Boolean) {
        viewModelScope.launch { _isProjectedDeviceConnected.emit(connected) }
    }

    internal fun setArDeviceState(deviceTrackingState: TrackingState, devicePose: Pose) {
        viewModelScope.launch {
            _deviceTrackingState.emit(deviceTrackingState)
            _devicePose.emit(devicePose)
        }
    }

    internal fun setGeospatialState(
        geospatialTrackingState: Geospatial.GeospatialTrackingState,
        geospatialPose: GeospatialPose,
        horizontalAccuracy: Double,
        verticalAccuracy: Double,
        orientationYawAccuracy: Double,
    ) {
        viewModelScope.launch {
            _geospatialTrackingState.emit(geospatialTrackingState)
            _geospatialPose.emit(geospatialPose)
            _horizontalAccuracy.emit(horizontalAccuracy)
            _verticalAccuracy.emit(verticalAccuracy)
            _orientationYawAccuracy.emit(orientationYawAccuracy)
        }
    }

    internal fun setVpsAvailability(status: VpsAvailabilityResult?) {
        viewModelScope.launch { _vpsAvailability.emit(status) }
    }

    internal fun setConversionResult(result: ConversionResult?) {
        viewModelScope.launch { _conversionResult.emit(result) }
    }
}
