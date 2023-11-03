/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.haptics

import android.os.Build
import androidx.core.haptics.device.HapticCompositionProfile
import androidx.core.haptics.device.HapticDeviceProfile
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.signal.PredefinedEffectSignal
import androidx.core.haptics.testing.AmplitudeVibrator
import androidx.core.haptics.testing.FullVibrator
import androidx.core.haptics.testing.PartialVibrator
import androidx.core.haptics.testing.PatternVibrator
import androidx.core.haptics.testing.PredefinedEffectsAndAmplitudeVibrator
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class GetHapticDeviceProfileTest {

    @Test
    fun getHapticDeviceProfile_noHardwareSupport_returnsEmptyProfile() {
        val hapticManager = requireNotNull(HapticManager.createForVibrator(PatternVibrator()))
        assertThat(hapticManager.deviceProfile).isEqualTo(HapticDeviceProfile())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun getHapticDeviceProfile_onlyAmplitudeControlSdk26AndAbove_returnsOnlyAmplitudeSupportTrue() {
        val hapticManager = requireNotNull(HapticManager.createForVibrator(AmplitudeVibrator()))
        val deviceProfile = hapticManager.deviceProfile

        assertThat(deviceProfile.isAmplitudeControlSupported).isTrue()
        assertThat(deviceProfile.hardwareOptimizedPredefinedEffects).isEmpty()
        assertThat(deviceProfile.compositionProfile).isEqualTo(HapticCompositionProfile())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun getHapticDeviceProfile_amplitudeAndPredefinedSupportSdk29AndAbove_returnsBothSupportTrue() {
        val hapticManager = requireNotNull(
            HapticManager.createForVibrator(PredefinedEffectsAndAmplitudeVibrator())
        )
        val deviceProfile = hapticManager.deviceProfile

        assertThat(deviceProfile.isAmplitudeControlSupported).isTrue()
        assertThat(deviceProfile.hardwareOptimizedPredefinedEffects)
            .containsExactlyElementsIn(PredefinedEffectSignal.getSdkAvailableEffects())
        assertThat(deviceProfile.compositionProfile).isEqualTo(HapticCompositionProfile())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getHapticDeviceProfile_compositionWithoutDurationSdk30AndAbove_returnsPrimitivesOnly() {
        val hapticManager = requireNotNull(
            HapticManager.createForVibrator(FullVibrator(fakePrimitiveDuration = null))
        )
        val compositionProfile = hapticManager.deviceProfile.compositionProfile

        assertThat(compositionProfile.supportedPrimitiveTypes)
            .containsExactlyElementsIn(PrimitiveAtom.getSdkAvailablePrimitiveTypes())

        assertThat(compositionProfile.isPrimitiveDurationReported).isFalse()
        PrimitiveAtom.ALL_PRIMITIVES.forEach { primitive ->
            assertWithMessage("Expected duration of $primitive to be zero")
                .that(compositionProfile.getPrimitiveDurationMillis(primitive.type))
                .isEqualTo(0L)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getHapticDeviceProfile_compositionWithPartialSupportSdk30AndAbove_returnsOnlySupported() {
        val hapticManager = requireNotNull(
            HapticManager.createForVibrator(
                PartialVibrator(
                    primitivesSupported = intArrayOf(PrimitiveAtom.CLICK, PrimitiveAtom.TICK),
                    primitivesDurations = mapOf(
                        PrimitiveAtom.CLICK to 20.milliseconds,
                        PrimitiveAtom.TICK to 10.milliseconds,
                    ),
                )
            )
        )
        val compositionProfile = hapticManager.deviceProfile.compositionProfile

        assertThat(compositionProfile.isPrimitiveDurationReported).isTrue()
        assertThat(compositionProfile.supportedPrimitiveTypes)
            .containsExactly(PrimitiveAtom.CLICK, PrimitiveAtom.TICK)
        assertThat(compositionProfile.getPrimitiveDurationMillis(PrimitiveAtom.CLICK))
            .isEqualTo(20L)
        assertThat(compositionProfile.getPrimitiveDurationMillis(PrimitiveAtom.TICK))
            .isEqualTo(10L)
        PrimitiveAtom.ALL_PRIMITIVES.map { it.type }.filter {
            it != PrimitiveAtom.CLICK && it != PrimitiveAtom.TICK
        }.forEach { primitive ->
            assertWithMessage("Expected duration of $primitive to be zero")
                .that(compositionProfile.getPrimitiveDurationMillis(primitive))
                .isEqualTo(0L)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getHapticDeviceProfile_compositionWithFullSupportSdk30AndAbove_returnsAllSdkPrimitives() {
        val sdkPrimitiveTypes = PrimitiveAtom.getSdkAvailablePrimitiveTypes()
        val missingPrimitiveTypes =
            PrimitiveAtom.ALL_PRIMITIVES.map { it.type }
                .filterNot { sdkPrimitiveTypes.contains(it) }

        val hapticManager = requireNotNull(HapticManager.createForVibrator(FullVibrator()))
        val compositionProfile = hapticManager.deviceProfile.compositionProfile

        assertThat(compositionProfile.isPrimitiveDurationReported).isTrue()
        assertThat(compositionProfile.supportedPrimitiveTypes)
            .containsExactlyElementsIn(sdkPrimitiveTypes)
        sdkPrimitiveTypes.forEach { primitive ->
            assertWithMessage("Expected duration of $primitive to be positive")
                .that(compositionProfile.getPrimitiveDurationMillis(primitive))
                .isGreaterThan(0L)
        }
        missingPrimitiveTypes.forEach { primitive ->
            assertWithMessage("Expected duration of $primitive to be zero")
                .that(compositionProfile.getPrimitiveDurationMillis(primitive))
                .isEqualTo(0L)
        }
    }
}
