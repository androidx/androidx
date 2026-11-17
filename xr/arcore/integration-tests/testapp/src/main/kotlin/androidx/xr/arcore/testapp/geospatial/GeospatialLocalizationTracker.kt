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

package androidx.xr.arcore.testapp.geospatial

/** How well Geospatial is localized, as determined by [GeospatialLocalizationTracker]. */
internal enum class LocalizationState {
    /** Geospatial is not running, or the geospatial pose of the device isn't available. */
    NOT_TRACKING,

    /** Geospatial is running, but its accuracy hasn't converged yet. */
    LOCALIZING,

    /** Geospatial accuracy has converged, so anchors can be created and restored. */
    LOCALIZED,
}

/**
 * Determines whether the Geospatial accuracy has converged enough to create and restore anchors.
 *
 * Geospatial localization starts out coarse and converges once the Visual Positioning System (VPS)
 * recognizes the surroundings. Anchors that are created or restored before then can be placed
 * meters away from where they are expected, often outside of the field of view.
 *
 * The tracker becomes [LocalizationState.LOCALIZED] once the horizontal and yaw accuracies have
 * been within their thresholds for [requiredConvergedSamples] consecutive samples. Once localized,
 * it only reverts to [LocalizationState.LOCALIZING] when an accuracy degrades beyond its threshold
 * plus a hysteresis margin, so that small fluctuations don't toggle the state.
 *
 * @property horizontalAccuracyThresholdMeters the maximum horizontal accuracy, in meters, at which
 *   anchors can be created and restored
 * @property yawAccuracyThresholdDegrees the maximum orientation yaw accuracy, in degrees, at which
 *   anchors can be created and restored
 */
internal class GeospatialLocalizationTracker(
    val horizontalAccuracyThresholdMeters: Double = DEFAULT_HORIZONTAL_ACCURACY_THRESHOLD_METERS,
    val yawAccuracyThresholdDegrees: Double = DEFAULT_YAW_ACCURACY_THRESHOLD_DEGREES,
    private val horizontalAccuracyHysteresisMeters: Double =
        DEFAULT_HORIZONTAL_ACCURACY_HYSTERESIS_METERS,
    private val yawAccuracyHysteresisDegrees: Double = DEFAULT_YAW_ACCURACY_HYSTERESIS_DEGREES,
    private val requiredConvergedSamples: Int = DEFAULT_REQUIRED_CONVERGED_SAMPLES,
) {
    init {
        require(requiredConvergedSamples >= 1) { "requiredConvergedSamples must be at least 1." }
    }

    /** The current [LocalizationState]. */
    var state: LocalizationState = LocalizationState.NOT_TRACKING
        private set

    private var convergedSamples = 0

    /**
     * Returns whether [horizontalAccuracyMeters] and [yawAccuracyDegrees] are within the thresholds
     * at which anchors can be created and restored.
     */
    fun isWithinThresholds(horizontalAccuracyMeters: Double, yawAccuracyDegrees: Double): Boolean =
        isWithinLimits(
            horizontalAccuracyMeters,
            yawAccuracyDegrees,
            horizontalAccuracyThresholdMeters,
            yawAccuracyThresholdDegrees,
        )

    /**
     * Updates [state] with the latest accuracies of the geospatial pose of the device.
     *
     * @return the updated [state]
     */
    fun onAccuracySample(
        horizontalAccuracyMeters: Double,
        yawAccuracyDegrees: Double,
    ): LocalizationState {
        if (state == LocalizationState.LOCALIZED) {
            val isWithinHysteresis =
                isWithinLimits(
                    horizontalAccuracyMeters,
                    yawAccuracyDegrees,
                    horizontalAccuracyThresholdMeters + horizontalAccuracyHysteresisMeters,
                    yawAccuracyThresholdDegrees + yawAccuracyHysteresisDegrees,
                )
            if (isWithinHysteresis) {
                return state
            }
            convergedSamples = 0
            state = LocalizationState.LOCALIZING
            return state
        }

        convergedSamples =
            if (isWithinThresholds(horizontalAccuracyMeters, yawAccuracyDegrees)) {
                convergedSamples + 1
            } else {
                0
            }
        state =
            if (convergedSamples >= requiredConvergedSamples) {
                LocalizationState.LOCALIZED
            } else {
                LocalizationState.LOCALIZING
            }
        return state
    }

    /**
     * Resets [state] to [LocalizationState.NOT_TRACKING], e.g. when Geospatial isn't running.
     *
     * @return the updated [state]
     */
    fun onNotTracking(): LocalizationState {
        convergedSamples = 0
        state = LocalizationState.NOT_TRACKING
        return state
    }

    // The runtime reports an accuracy of zero when no estimate is available, so only positive
    // accuracies are considered valid. The comparisons also reject NaN.
    private fun isWithinLimits(
        horizontalAccuracyMeters: Double,
        yawAccuracyDegrees: Double,
        horizontalLimitMeters: Double,
        yawLimitDegrees: Double,
    ): Boolean =
        horizontalAccuracyMeters > 0.0 &&
            horizontalAccuracyMeters <= horizontalLimitMeters &&
            yawAccuracyDegrees > 0.0 &&
            yawAccuracyDegrees <= yawLimitDegrees

    internal companion object {
        // Match the accuracy thresholds used by the ARCore Geospatial sample.
        const val DEFAULT_HORIZONTAL_ACCURACY_THRESHOLD_METERS = 10.0
        const val DEFAULT_YAW_ACCURACY_THRESHOLD_DEGREES = 15.0

        // Once localized, how far the accuracies can degrade beyond their thresholds before
        // localization is considered lost.
        const val DEFAULT_HORIZONTAL_ACCURACY_HYSTERESIS_METERS = 5.0
        const val DEFAULT_YAW_ACCURACY_HYSTERESIS_DEGREES = 10.0

        // How many consecutive samples have to be within the thresholds to localize.
        const val DEFAULT_REQUIRED_CONVERGED_SAMPLES = 3
    }
}
