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

package androidx.core.haptics.samples

import androidx.annotation.Sampled
import androidx.core.haptics.signal.CompositionSignal
import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.slowRise
import androidx.core.haptics.signal.FallbackChainSignal.Companion.fallbackChainOf
import androidx.core.haptics.signal.WaveformSignal.Companion.off
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf

/**
 * Sample showing how to create a haptic fallback chain with haptic signals.
 */
@Sampled
fun HapticFallbackChainOfSignals() {
    fallbackChainOf(
        compositionOf(
            slowRise(amplitudeScale = 0.7f),
            CompositionSignal.off(durationMillis = 50),
            click(),
        ),
        waveformOf(
            // 600ms ramp up with 50% increments
            on(durationMillis = 100, amplitude = 0.1f),
            on(durationMillis = 100, amplitude = 0.15f),
            on(durationMillis = 100, amplitude = 0.22f),
            on(durationMillis = 100, amplitude = 0.34f),
            on(durationMillis = 100, amplitude = 0.51f),
            on(durationMillis = 100, amplitude = 0.76f),
            // 50ms off
            off(durationMillis = 50),
            // 20ms at max amplitude
            on(durationMillis = 20, amplitude = 1f),
        ),
        waveformOf(
            // Shorter 400ms vibration at fixed default amplitude.
            on(durationMillis = 400),
            off(durationMillis = 50),
            on(durationMillis = 20),
        ),
    )
}
