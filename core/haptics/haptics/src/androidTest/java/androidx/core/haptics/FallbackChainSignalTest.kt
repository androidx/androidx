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

import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.lowTick
import androidx.core.haptics.signal.CompositionSignal.Companion.tick
import androidx.core.haptics.signal.FallbackChainSignal
import androidx.core.haptics.signal.FallbackChainSignal.Companion.fallbackChainOf
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.signal.ResolvableSignal
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf
import androidx.core.haptics.testing.PatternVibrator
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class FallbackChainSignalTest {

    private val deviceProfile = requireNotNull(
        HapticManager.createForVibrator(PatternVibrator())
    ).deviceProfile

    @Test
    fun resolve_emptyList_returnsNull() {
        assertThat(FallbackChainSignal(emptyList()).resolve(deviceProfile)).isNull()
    }

    @Test
    fun resolve_withNoSupportedSignal_returnsNull() {
        val fallbackChain = fallbackChainOf(
            compositionOf(click()), // requires primitive click support
            waveformOf(on(durationMillis = 20, amplitude = 0.5f)), // requires amplitude control
        )
        assertThat(fallbackChain.resolve(deviceProfile)).isNull()
    }

    @Test
    fun resolve_withSupportedSignals_returnsFirstSupportedSignal() {
        val fallbackChain = fallbackChainOf(
            compositionOf(click()), // requires primitive click support
            waveformOf(on(durationMillis = 20)), // First supported
            waveformOf(on(durationMillis = 10)), // Second supported
        )
        assertThat(fallbackChain.resolve(deviceProfile))
            .isEqualTo(waveformOf(on(durationMillis = 20)))
    }

    @Test
    fun resolve_withNoSupportedResolvableSignal_returnsNull() {
        val fallbackChain = fallbackChainOf(
            ResolvableSignal { null }, // Always resolves to a null signal, not supported
            waveformOf(on(durationMillis = 20, amplitude = 0.5f)), // requires amplitude control
            fallbackChainOf(
                compositionOf(lowTick()),
                compositionOf(tick(amplitudeScale = 0.4f)),
            ), // requires primitives tick and low tick support
        )
        assertThat(fallbackChain.resolve(deviceProfile)).isNull()
    }

    @Test
    fun resolve_withSupportedResolvableSignals_returnsFirstSupportedSignal() {
        val fallbackChain = fallbackChainOf(
            ResolvableSignal { null }, // Always resolves to a null signal, not supported
            fallbackChainOf(
                compositionOf(tick()), // requires primitive tick support
                predefinedClick(), // First supported
            ),
            waveformOf(on(durationMillis = 10)), // Second supported
        )
        assertThat(fallbackChain.resolve(deviceProfile)).isEqualTo(predefinedClick())
    }
}
