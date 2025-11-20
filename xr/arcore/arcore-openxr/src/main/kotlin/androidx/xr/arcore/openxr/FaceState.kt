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

package androidx.xr.arcore.openxr

import androidx.xr.runtime.TrackingState

internal data class FaceState(
    val trackingState: TrackingState = TrackingState.PAUSED,
    val isValid: Boolean = false,
    val parameters: FloatArray = FloatArray(OpenXrFace.XR_FACE_PARAMETER_COUNT_ANDROID),
    val regionConfidences: FloatArray =
        FloatArray(OpenXrFace.XR_FACE_REGION_CONFIDENCE_COUNT_ANDROID),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FaceState

        if (isValid != other.isValid) return false
        if (trackingState != other.trackingState) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (!regionConfidences.contentEquals(other.regionConfidences)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isValid.hashCode()
        result = 31 * result + trackingState.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + regionConfidences.contentHashCode()
        return result
    }
}

internal fun TrackingState.Companion.fromOpenXrFaceTrackingState(
    trackingState: Int
): TrackingState =
    when (trackingState) {
        0 -> TrackingState.PAUSED // XR_TRACKING_STATE_PAUSED_ANDROID
        1 -> TrackingState.STOPPED // XR_TRACKING_STATE_STOPPED_ANDROID
        2 -> TrackingState.TRACKING // XR_TRACKING_STATE_TRACKING_ANDROID
        else -> {
            throw IllegalArgumentException("Invalid tracking state.")
        }
    }
