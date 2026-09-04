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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.util.packFloats
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests for [PitchLimits]. */
@OptIn(ExperimentalRotateToLookAtUserApi::class)
@RunWith(JUnit4::class)
class PitchLimitsTest {

    @Test
    fun pitchLimits_validRange_createsSuccessfully() {
        val limits = PitchLimits(minimumPitch = -30f, maximumPitch = 45f)
        assertThat(limits.minimumPitch).isEqualTo(-30f)
        assertThat(limits.maximumPitch).isEqualTo(45f)
    }

    @Test
    fun pitchLimits_fullRange_hasFullRange() {
        val limits = PitchLimits.FullRange
        assertThat(limits.minimumPitch).isEqualTo(-90f)
        assertThat(limits.maximumPitch).isEqualTo(90f)
    }

    @Test
    fun pitchLimits_minimumPitchBelowLimit_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = -90.1f, maximumPitch = 0f)
        }
    }

    @Test
    fun pitchLimits_maximumPitchAboveLimit_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = 0f, maximumPitch = 90.1f)
        }
    }

    @Test
    fun pitchLimits_minimumGreaterThanMaximum_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = 10f, maximumPitch = -10f)
        }
    }

    @Test
    fun pitchLimits_equalMinAndMax_createsSuccessfully() {
        val zeroLimits = PitchLimits(minimumPitch = 0f, maximumPitch = 0f)
        assertThat(zeroLimits.minimumPitch).isEqualTo(0f)
        assertThat(zeroLimits.maximumPitch).isEqualTo(0f)

        val minBoundary = PitchLimits(minimumPitch = -90f, maximumPitch = -90f)
        assertThat(minBoundary.minimumPitch).isEqualTo(-90f)
        assertThat(minBoundary.maximumPitch).isEqualTo(-90f)

        val maxBoundary = PitchLimits(minimumPitch = 90f, maximumPitch = 90f)
        assertThat(maxBoundary.minimumPitch).isEqualTo(90f)
        assertThat(maxBoundary.maximumPitch).isEqualTo(90f)
    }

    @Test
    fun pitchLimits_nan_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = Float.NaN, maximumPitch = 0f)
        }
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = 0f, maximumPitch = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = Float.NaN, maximumPitch = Float.NaN)
        }
    }

    @Test
    fun pitchLimits_infinity_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = Float.NEGATIVE_INFINITY, maximumPitch = 0f)
        }
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(minimumPitch = 0f, maximumPitch = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            PitchLimits(
                minimumPitch = Float.NEGATIVE_INFINITY,
                maximumPitch = Float.POSITIVE_INFINITY,
            )
        }
    }

    @Test
    fun pitchLimits_equalsAndHashCode_workCorrectly() {
        val limits1 = PitchLimits(-15f, 15f)
        val limits2 = PitchLimits(-15f, 15f)
        val limits3 = PitchLimits(-10f, 15f)

        assertThat(limits1).isEqualTo(limits2)
        assertThat(limits1.hashCode()).isEqualTo(limits2.hashCode())
        assertThat(limits1).isNotEqualTo(limits3)
    }

    @Test
    fun pitchLimits_toString_returnsExpectedFormat() {
        val limits = PitchLimits(-10f, 20f)
        assertThat(limits.toString())
            .isEqualTo("PitchLimits(minimumPitch=-10.0, maximumPitch=20.0)")
    }

    @Test
    fun pitchLimits_copy_updatesValuesCorrectly() {
        val limits = PitchLimits(-10f, 20f)

        val copiedDefault = limits.copy()
        assertThat(copiedDefault).isEqualTo(limits)
        assertThat(copiedDefault.minimumPitch).isEqualTo(-10f)
        assertThat(copiedDefault.maximumPitch).isEqualTo(20f)

        val copied1 = limits.copy(minimumPitch = -15f)
        assertThat(copied1.minimumPitch).isEqualTo(-15f)
        assertThat(copied1.maximumPitch).isEqualTo(20f)

        val copied2 = limits.copy(maximumPitch = 25f)
        assertThat(copied2.minimumPitch).isEqualTo(-10f)
        assertThat(copied2.maximumPitch).isEqualTo(25f)

        val copiedBoth = limits.copy(minimumPitch = -30f, maximumPitch = 40f)
        assertThat(copiedBoth.minimumPitch).isEqualTo(-30f)
        assertThat(copiedBoth.maximumPitch).isEqualTo(40f)
    }

    @Test
    fun pitchLimits_copy_invalidArguments_throwsIllegalArgumentException() {
        val limits = PitchLimits(-10f, 20f)

        // Below minimum limit
        assertFailsWith<IllegalArgumentException> { limits.copy(minimumPitch = -90.1f) }

        // Above maximum limit
        assertFailsWith<IllegalArgumentException> { limits.copy(maximumPitch = 90.1f) }

        // Minimum greater than existing maximum
        assertFailsWith<IllegalArgumentException> { limits.copy(minimumPitch = 25f) }

        // Existing minimum greater than new maximum
        assertFailsWith<IllegalArgumentException> { limits.copy(maximumPitch = -15f) }

        // NaN
        assertFailsWith<IllegalArgumentException> { limits.copy(minimumPitch = Float.NaN) }
        assertFailsWith<IllegalArgumentException> { limits.copy(maximumPitch = Float.NaN) }

        // Infinity
        assertFailsWith<IllegalArgumentException> {
            limits.copy(minimumPitch = Float.NEGATIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            limits.copy(maximumPitch = Float.POSITIVE_INFINITY)
        }
    }

    @Test
    fun pitchLimits_destructuring_worksCorrectly() {
        val (min, max) = PitchLimits(-15f, 30f)
        assertThat(min).isEqualTo(-15f)
        assertThat(max).isEqualTo(30f)
    }

    @Test
    fun pitchLimits_internalConstructor_storesAndReconstructsFromPackedLong() {
        val original = PitchLimits(minimumPitch = -45f, maximumPitch = 60f)
        val packed: Long = packFloats(-45f, 60f)

        // Verify reconstructing via the internal value class constructor PitchLimits(packedValue:
        // Long)
        val reconstructed = PitchLimits(packed)

        assertThat(reconstructed).isEqualTo(original)
        assertThat(reconstructed.minimumPitch).isEqualTo(-45f)
        assertThat(reconstructed.maximumPitch).isEqualTo(60f)

        // Verify with FullRange
        val fullRangePacked: Long = packFloats(-90f, 90f)
        val reconstructedFullRange = PitchLimits(fullRangePacked)
        assertThat(reconstructedFullRange).isEqualTo(PitchLimits.FullRange)
        assertThat(reconstructedFullRange.minimumPitch).isEqualTo(-90f)
        assertThat(reconstructedFullRange.maximumPitch).isEqualTo(90f)
    }
}
