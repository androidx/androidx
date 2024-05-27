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
import androidx.core.haptics.signal.CompositionSignal
import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.lowTick
import androidx.core.haptics.signal.CompositionSignal.Companion.tick
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedTick
import androidx.core.haptics.signal.WaveformSignal.Companion.off
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class HapticDeviceProfileTest {

    @Test
    fun empty_hasNoHardwareSupport() {
        val deviceProfile = HapticDeviceProfile()
        assertThat(deviceProfile.isAmplitudeControlSupported).isFalse()
        assertThat(deviceProfile.hardwareOptimizedPredefinedEffects).isEmpty()
        assertThat(deviceProfile.compositionProfile).isEqualTo(HapticCompositionProfile())
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun isAmplitudeControlSupported_sdk26AndAbove_returnsAmplitudeControlHint() {
        assertThat(HapticDeviceProfile(true).isAmplitudeControlSupported).isTrue()
        assertThat(HapticDeviceProfile(false).isAmplitudeControlSupported).isFalse()
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun isAmplitudeControlSupported_belowSdk26_returnsAlwaysFalse() {
        assertThat(HapticDeviceProfile(true).isAmplitudeControlSupported).isFalse()
        assertThat(HapticDeviceProfile(false).isAmplitudeControlSupported).isFalse()
    }

    @Test
    fun hardwareOptimizedPredefinedEffects_withEmptyHint_returnsEmpty() {
        val profile = HapticDeviceProfile(hardwareOptimizedPredefinedEffectsHint = emptySet())
        assertThat(profile.hardwareOptimizedPredefinedEffects).isEmpty()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun hardwareOptimizedPredefinedEffects_withHintSdk29AndAbove_returnsOptimizedEffectsFromHint() {
        val profile =
            HapticDeviceProfile(
                hardwareOptimizedPredefinedEffectsHint = setOf(predefinedTick(), predefinedClick()),
            )
        assertThat(profile.hardwareOptimizedPredefinedEffects)
            .containsExactly(predefinedTick(), predefinedClick())
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun hardwareOptimizedPredefinedEffects_withHintBelowSdk29_returnsAlwaysEmpty() {
        val profile =
            HapticDeviceProfile(
                hardwareOptimizedPredefinedEffectsHint = setOf(predefinedTick(), predefinedClick()),
            )
        assertThat(profile.hardwareOptimizedPredefinedEffects).isEmpty()
    }

    @Test
    fun isSupported_predefinedEffectWithAnyDeviceProfile_returnsAlwaysTrue() {
        assertThat(HapticDeviceProfile().supports(predefinedClick())).isTrue()
        assertThat(
                HapticDeviceProfile(
                        amplitudeControlSupportHint = true,
                        hardwareOptimizedPredefinedEffectsHint = setOf(predefinedClick()),
                    )
                    .supports(predefinedClick())
            )
            .isTrue()
    }

    @Test
    fun isSupported_waveformWithoutAmplitudeControl_returnsTrueForPatternAndFalseForAmplitudes() {
        val deviceProfile = HapticDeviceProfile()
        val patternWaveform = waveformOf(off(durationMillis = 10), on(durationMillis = 20))
        val amplitudeWaveform = waveformOf(on(durationMillis = 10, amplitude = 0.5f))

        assertThat(deviceProfile.supports(patternWaveform)).isTrue()
        assertThat(deviceProfile.supports(amplitudeWaveform)).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun isSupported_waveformWithAmplitudeControlSdk26AndAbove_returnsAlwaysTrue() {
        val deviceProfile = HapticDeviceProfile(amplitudeControlSupportHint = true)
        val patternWaveform = waveformOf(off(durationMillis = 10), on(durationMillis = 20))
        val amplitudeWaveform = waveformOf(on(durationMillis = 10, amplitude = 0.5f))

        assertThat(deviceProfile.supports(patternWaveform)).isTrue()
        assertThat(deviceProfile.supports(amplitudeWaveform)).isTrue()
    }

    @Test
    fun isSupported_compositionWithoutPrimitives_returnsAlwaysFalse() {
        val deviceProfile = HapticDeviceProfile()
        assertThat(deviceProfile.supports(compositionOf(click()))).isFalse()
        assertThat(deviceProfile.supports(compositionOf(tick()))).isFalse()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun isSupported_compositionWithPrimitivesSdk30AndAbove_returnsTrueForSupportedPrimitives() {
        val deviceProfile =
            HapticDeviceProfile(
                compositionProfile =
                    HapticCompositionProfile(
                        supportedPrimitiveTypesHint =
                            setOf(PrimitiveAtom.CLICK, PrimitiveAtom.TICK),
                    )
            )
        val supportedComposition =
            compositionOf(
                click(amplitudeScale = 0.7f),
                CompositionSignal.off(durationMillis = 50),
                tick(),
            )
        val unsupportedComposition =
            compositionOf(
                click(amplitudeScale = 0.7f),
                CompositionSignal.off(durationMillis = 50),
                lowTick(),
            )

        assertThat(deviceProfile.supports(supportedComposition)).isTrue()
        assertThat(deviceProfile.supports(unsupportedComposition)).isFalse()
    }
}

@RunWith(JUnit4::class)
@SmallTest
class HapticCompositionProfileTest {

    @Test
    fun empty_hasNoHardwareSupport() {
        val profile = HapticCompositionProfile()
        assertThat(profile.isPrimitiveDurationReported).isFalse()
        assertThat(profile.supportedPrimitiveTypes).isEmpty()
        PrimitiveAtom.ALL_PRIMITIVES.forEach { primitive ->
            assertWithMessage("Expected reported duration of $primitive to be zero")
                .that(profile.getPrimitiveDurationMillis(primitive.type))
                .isEqualTo(0L)
        }
    }

    @Test
    fun supportedPrimitiveTypes_withEmptyHint_returnsEmpty() {
        val profile = HapticCompositionProfile(supportedPrimitiveTypesHint = emptySet())
        assertThat(profile.supportedPrimitiveTypes).isEmpty()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R, maxSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun supportedPrimitiveTypes_withHintSdk30_returnsOnlyHintsFromSdk30() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint =
                    setOf(
                        PrimitiveAtom.TICK,
                        PrimitiveAtom.LOW_TICK,
                        PrimitiveAtom.CLICK,
                    ),
            )
        assertThat(profile.supportedPrimitiveTypes)
            .containsExactly(PrimitiveAtom.CLICK, PrimitiveAtom.TICK)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun supportedPrimitiveTypes_withHintSdk31AndAbove_returnsMatchingHints() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint =
                    setOf(
                        PrimitiveAtom.TICK,
                        PrimitiveAtom.LOW_TICK,
                    ),
                primitiveDurationMillisMapHint = null,
            )
        assertThat(profile.supportedPrimitiveTypes)
            .containsExactly(PrimitiveAtom.LOW_TICK, PrimitiveAtom.TICK)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun supportedPrimitiveTypes_withHintBelowSdk30_returnsEmpty() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint =
                    setOf(
                        PrimitiveAtom.TICK,
                        PrimitiveAtom.LOW_TICK,
                    ),
            )
        assertThat(profile.supportedPrimitiveTypes).isEmpty()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getPrimitiveDurationMillis_withNullDurationsSdk30AndAbove_returnsDurationReportedFalse() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint =
                    setOf(
                        PrimitiveAtom.TICK,
                        PrimitiveAtom.CLICK,
                    ),
                primitiveDurationMillisMapHint = null,
            )
        assertThat(profile.isPrimitiveDurationReported).isFalse()
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.TICK)).isEqualTo(0L)
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.CLICK)).isEqualTo(0L)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getPrimitiveDurationMillis_withDurationsSdk30AndAbove_returnsHintDurations() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint =
                    setOf(
                        PrimitiveAtom.TICK,
                        PrimitiveAtom.CLICK,
                    ),
                primitiveDurationMillisMapHint =
                    mapOf(
                        PrimitiveAtom.TICK to 10L,
                        PrimitiveAtom.CLICK to 20L,
                    ),
            )
        assertThat(profile.isPrimitiveDurationReported).isTrue()
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.TICK)).isEqualTo(10L)
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.CLICK)).isEqualTo(20L)
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.THUD))
            .isEqualTo(0L) // Thud not supported
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun getPrimitiveDurationMillis_withUnsupportedPrimitiveDurationSdk30AndAbove_ignoresDuration() {
        val profile =
            HapticCompositionProfile(
                supportedPrimitiveTypesHint = setOf(PrimitiveAtom.TICK),
                primitiveDurationMillisMapHint =
                    mapOf(
                        PrimitiveAtom.TICK to 10L,
                        PrimitiveAtom.CLICK to 20L,
                    ),
            )
        assertThat(profile.isPrimitiveDurationReported).isTrue()
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.TICK)).isEqualTo(10L)
        assertThat(profile.getPrimitiveDurationMillis(PrimitiveAtom.CLICK))
            .isEqualTo(0L) // Click not supported
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun constructor_withMissingSupportedPrimitiveDurationSdk30AndAbove_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            HapticCompositionProfile(
                supportedPrimitiveTypesHint = setOf(PrimitiveAtom.TICK, PrimitiveAtom.CLICK),
                primitiveDurationMillisMapHint = mapOf(PrimitiveAtom.CLICK to 10L),
            )
        }
    }
}
