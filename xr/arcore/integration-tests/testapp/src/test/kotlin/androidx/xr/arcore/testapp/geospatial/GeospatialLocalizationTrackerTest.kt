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

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GeospatialLocalizationTrackerTest {

    private val tracker = GeospatialLocalizationTracker()

    @Test
    fun init_zeroRequiredConvergedSamples_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeospatialLocalizationTracker(requiredConvergedSamples = 0)
        }
    }

    @Test
    fun init_negativeRequiredConvergedSamples_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            GeospatialLocalizationTracker(requiredConvergedSamples = -1)
        }
    }

    @Test
    fun init_requiredConvergedSamplesOne_localizesAfterSingleSample() {
        val singleSampleTracker = GeospatialLocalizationTracker(requiredConvergedSamples = 1)

        assertThat(
                singleSampleTracker.onAccuracySample(
                    CONVERGED_HORIZONTAL_METERS,
                    CONVERGED_YAW_DEGREES,
                )
            )
            .isEqualTo(LocalizationState.LOCALIZED)
    }

    @Test
    fun state_initially_isNotTracking() {
        assertThat(tracker.state).isEqualTo(LocalizationState.NOT_TRACKING)
    }

    @Test
    fun thresholds_default_are10MetersAnd15Degrees() {
        assertThat(tracker.horizontalAccuracyThresholdMeters).isEqualTo(HORIZONTAL_THRESHOLD_METERS)
        assertThat(tracker.yawAccuracyThresholdDegrees).isEqualTo(YAW_THRESHOLD_DEGREES)
    }

    @Test
    fun isWithinThresholds_withinThresholds_returnsTrue() {
        assertThat(tracker.isWithinThresholds(CONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES))
            .isTrue()
    }

    @Test
    fun isWithinThresholds_atThresholds_returnsTrue() {
        assertThat(tracker.isWithinThresholds(HORIZONTAL_THRESHOLD_METERS, YAW_THRESHOLD_DEGREES))
            .isTrue()
    }

    @Test
    fun isWithinThresholds_horizontalAccuracyAboveThreshold_returnsFalse() {
        assertThat(
                tracker.isWithinThresholds(
                    HORIZONTAL_THRESHOLD_METERS + EXCESS,
                    YAW_THRESHOLD_DEGREES,
                )
            )
            .isFalse()
    }

    @Test
    fun isWithinThresholds_yawAccuracyAboveThreshold_returnsFalse() {
        assertThat(
                tracker.isWithinThresholds(
                    HORIZONTAL_THRESHOLD_METERS,
                    YAW_THRESHOLD_DEGREES + EXCESS,
                )
            )
            .isFalse()
    }

    @Test
    fun isWithinThresholds_zeroHorizontalAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(0.0, CONVERGED_YAW_DEGREES)).isFalse()
    }

    @Test
    fun isWithinThresholds_zeroYawAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(CONVERGED_HORIZONTAL_METERS, 0.0)).isFalse()
    }

    @Test
    fun isWithinThresholds_negativeHorizontalAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(-1.0, CONVERGED_YAW_DEGREES)).isFalse()
    }

    @Test
    fun isWithinThresholds_negativeYawAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(CONVERGED_HORIZONTAL_METERS, -1.0)).isFalse()
    }

    @Test
    fun isWithinThresholds_nanHorizontalAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(Double.NaN, CONVERGED_YAW_DEGREES)).isFalse()
    }

    @Test
    fun isWithinThresholds_nanYawAccuracy_returnsFalse() {
        assertThat(tracker.isWithinThresholds(CONVERGED_HORIZONTAL_METERS, Double.NaN)).isFalse()
    }

    @Test
    fun isWithinThresholds_doesNotChangeState() {
        tracker.isWithinThresholds(CONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES)

        assertThat(tracker.state).isEqualTo(LocalizationState.NOT_TRACKING)
    }

    @Test
    fun onAccuracySample_fromNotTracking_localizesAfterRequiredConvergedSamples() {
        assertLocalizesAfterRequiredConvergedSamples()
    }

    @Test
    fun onAccuracySample_fromLocalizing_localizesAfterRequiredConvergedSamples() {
        check(
            tracker.onAccuracySample(UNCONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES) ==
                LocalizationState.LOCALIZING
        )

        assertLocalizesAfterRequiredConvergedSamples()
    }

    @Test
    fun onAccuracySample_atThresholds_localizes() {
        repeat(REQUIRED_CONVERGED_SAMPLES) {
            tracker.onAccuracySample(HORIZONTAL_THRESHOLD_METERS, YAW_THRESHOLD_DEGREES)
        }

        assertThat(tracker.state).isEqualTo(LocalizationState.LOCALIZED)
    }

    @Test
    fun onAccuracySample_horizontalAccuracyAboveThreshold_staysLocalizing() {
        repeat(REQUIRED_CONVERGED_SAMPLES * 2) {
            tracker.onAccuracySample(HORIZONTAL_THRESHOLD_METERS + EXCESS, CONVERGED_YAW_DEGREES)
        }

        assertThat(tracker.state).isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_yawAccuracyAboveThreshold_staysLocalizing() {
        repeat(REQUIRED_CONVERGED_SAMPLES * 2) {
            tracker.onAccuracySample(CONVERGED_HORIZONTAL_METERS, YAW_THRESHOLD_DEGREES + EXCESS)
        }

        assertThat(tracker.state).isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_zeroAccuracies_staysLocalizing() {
        repeat(REQUIRED_CONVERGED_SAMPLES * 2) { tracker.onAccuracySample(0.0, 0.0) }

        assertThat(tracker.state).isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_nanAccuracies_staysLocalizing() {
        repeat(REQUIRED_CONVERGED_SAMPLES * 2) { tracker.onAccuracySample(Double.NaN, Double.NaN) }

        assertThat(tracker.state).isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_streakInterruptedBySampleAboveThresholds_restartsConvergence() {
        repeat(REQUIRED_CONVERGED_SAMPLES - 1) { onConvergedAccuracySample() }
        check(
            tracker.onAccuracySample(UNCONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES) ==
                LocalizationState.LOCALIZING
        )

        assertLocalizesAfterRequiredConvergedSamples()
    }

    @Test
    fun onAccuracySample_localizedAndBetweenThresholdsAndHysteresisLimits_staysLocalized() {
        localize()

        repeat(REQUIRED_CONVERGED_SAMPLES * 2) {
            assertThat(tracker.onAccuracySample(DEGRADED_HORIZONTAL_METERS, DEGRADED_YAW_DEGREES))
                .isEqualTo(LocalizationState.LOCALIZED)
        }
    }

    @Test
    fun onAccuracySample_localizedAndAtHysteresisLimits_staysLocalized() {
        localize()

        assertThat(
                tracker.onAccuracySample(
                    HORIZONTAL_HYSTERESIS_LIMIT_METERS,
                    YAW_HYSTERESIS_LIMIT_DEGREES,
                )
            )
            .isEqualTo(LocalizationState.LOCALIZED)
    }

    @Test
    fun onAccuracySample_localizedAndHorizontalAccuracyAboveHysteresisLimit_revertsToLocalizing() {
        localize()

        assertThat(
                tracker.onAccuracySample(
                    HORIZONTAL_HYSTERESIS_LIMIT_METERS + EXCESS,
                    CONVERGED_YAW_DEGREES,
                )
            )
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_localizedAndYawAccuracyAboveHysteresisLimit_revertsToLocalizing() {
        localize()

        assertThat(
                tracker.onAccuracySample(
                    CONVERGED_HORIZONTAL_METERS,
                    YAW_HYSTERESIS_LIMIT_DEGREES + EXCESS,
                )
            )
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_localizedAndZeroHorizontalAccuracy_revertsToLocalizing() {
        localize()

        assertThat(tracker.onAccuracySample(0.0, CONVERGED_YAW_DEGREES))
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_localizedAndZeroYawAccuracy_revertsToLocalizing() {
        localize()

        assertThat(tracker.onAccuracySample(CONVERGED_HORIZONTAL_METERS, 0.0))
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_localizedAndNanHorizontalAccuracy_revertsToLocalizing() {
        localize()

        assertThat(tracker.onAccuracySample(Double.NaN, CONVERGED_YAW_DEGREES))
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_localizedAndNanYawAccuracy_revertsToLocalizing() {
        localize()

        assertThat(tracker.onAccuracySample(CONVERGED_HORIZONTAL_METERS, Double.NaN))
            .isEqualTo(LocalizationState.LOCALIZING)
    }

    @Test
    fun onAccuracySample_afterRevertingToLocalizing_requiresConvergedSamplesAgain() {
        localize()
        tracker.onAccuracySample(UNCONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES)

        assertLocalizesAfterRequiredConvergedSamples()
    }

    @Test
    fun onAccuracySample_afterRevertingToLocalizing_appliesThresholdsInsteadOfHysteresisLimits() {
        localize()
        tracker.onAccuracySample(UNCONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES)

        repeat(REQUIRED_CONVERGED_SAMPLES * 2) {
            assertThat(tracker.onAccuracySample(DEGRADED_HORIZONTAL_METERS, DEGRADED_YAW_DEGREES))
                .isEqualTo(LocalizationState.LOCALIZING)
        }
    }

    @Test
    fun onNotTracking_whenLocalized_resetsToNotTracking() {
        localize()

        assertThat(tracker.onNotTracking()).isEqualTo(LocalizationState.NOT_TRACKING)
        assertThat(tracker.state).isEqualTo(LocalizationState.NOT_TRACKING)
    }

    @Test
    fun onNotTracking_whenLocalized_requiresConvergedSamplesAgain() {
        localize()
        tracker.onNotTracking()

        assertLocalizesAfterRequiredConvergedSamples()
    }

    @Test
    fun onNotTracking_whenLocalizing_clearsConvergedSamples() {
        repeat(REQUIRED_CONVERGED_SAMPLES - 1) { onConvergedAccuracySample() }
        check(tracker.onNotTracking() == LocalizationState.NOT_TRACKING)

        assertLocalizesAfterRequiredConvergedSamples()
    }

    /** Passes accuracies that are within the thresholds to [tracker]. */
    private fun onConvergedAccuracySample(): LocalizationState =
        tracker.onAccuracySample(CONVERGED_HORIZONTAL_METERS, CONVERGED_YAW_DEGREES)

    private fun localize() {
        repeat(REQUIRED_CONVERGED_SAMPLES) { onConvergedAccuracySample() }
        check(tracker.state == LocalizationState.LOCALIZED)
    }

    /**
     * Asserts that [tracker] only localizes once it receives [REQUIRED_CONVERGED_SAMPLES]
     * consecutive samples within the thresholds.
     */
    private fun assertLocalizesAfterRequiredConvergedSamples() {
        repeat(REQUIRED_CONVERGED_SAMPLES - 1) {
            assertThat(onConvergedAccuracySample()).isEqualTo(LocalizationState.LOCALIZING)
        }
        assertThat(onConvergedAccuracySample()).isEqualTo(LocalizationState.LOCALIZED)
    }

    private companion object {
        // The default thresholds, and the default hysteresis limits (the thresholds plus their
        // hysteresis margins) beyond which localization is lost.
        const val HORIZONTAL_THRESHOLD_METERS = 10.0
        const val YAW_THRESHOLD_DEGREES = 15.0
        const val HORIZONTAL_HYSTERESIS_LIMIT_METERS = 15.0
        const val YAW_HYSTERESIS_LIMIT_DEGREES = 25.0
        const val REQUIRED_CONVERGED_SAMPLES = 3

        // Accuracies within the thresholds.
        const val CONVERGED_HORIZONTAL_METERS = 2.0
        const val CONVERGED_YAW_DEGREES = 5.0

        // Accuracies between the thresholds and the hysteresis limits.
        const val DEGRADED_HORIZONTAL_METERS = 12.0
        const val DEGRADED_YAW_DEGREES = 20.0

        // A horizontal accuracy beyond both its threshold and its hysteresis limit.
        const val UNCONVERGED_HORIZONTAL_METERS = 30.0

        // How far accuracies exceed a threshold or hysteresis limit by.
        const val EXCESS = 0.1
    }
}
