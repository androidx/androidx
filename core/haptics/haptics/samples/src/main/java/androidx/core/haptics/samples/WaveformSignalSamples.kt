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
import androidx.core.haptics.signal.WaveformSignal
import androidx.core.haptics.signal.WaveformSignal.Companion.off
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.repeatingWaveformOf
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf

/**
 * Sample showing how to create an on-off pattern.
 */
@Sampled
fun PatternWaveform() {
    waveformOf(
        on(durationMillis = 250),
        off(durationMillis = 350),
        on(durationMillis = 250),
    )
}

/**
 * Sample showing how to create an infinite haptic signal a repeating step waveform.
 */
@Sampled
fun PatternWaveformRepeat() {
    waveformOf(
        on(durationMillis = 100),
        off(durationMillis = 50),
        on(durationMillis = 100),
        off(durationMillis = 50),
    ).repeat()
}

/**
 * Sample showing how to create an amplitude step waveform.
 */
@Sampled
fun AmplitudeWaveform() {
    waveformOf(
        on(durationMillis = 10, amplitude = 0.2f),
        on(durationMillis = 20, amplitude = 0.4f),
        on(durationMillis = 30, amplitude = 0.8f),
        on(durationMillis = 40, amplitude = 1f),
        off(durationMillis = 50),
        on(durationMillis = 50),
    )
}

/**
 * Sample showing how to create an amplitude step waveform.
 */
@Sampled
fun RepeatingAmplitudeWaveform() {
    repeatingWaveformOf(
        on(durationMillis = 100),
        off(durationMillis = 50),
    )
}

/**
 * Sample showing how to create an infinite haptic signal as repeating step waveform.
 */
@Sampled
fun PatternThenRepeatExistingWaveform(waveformSignal: WaveformSignal) {
    waveformOf(
        on(durationMillis = 100),
        off(durationMillis = 50),
        on(durationMillis = 100),
        off(durationMillis = 500),
    ).thenRepeat(waveformSignal)
}

/**
 * Sample showing how to create an infinite haptic signal as repeating step waveform.
 */
@Sampled
fun PatternThenRepeatAmplitudeWaveform() {
    waveformOf(
        on(durationMillis = 100),
        off(durationMillis = 50),
        on(durationMillis = 100),
    ).thenRepeat(
        on(durationMillis = 500, amplitude = 0f),
        on(durationMillis = 100, amplitude = 0.2f),
        on(durationMillis = 200, amplitude = 0.4f),
        on(durationMillis = 300, amplitude = 0.8f),
        on(durationMillis = 400, amplitude = 1f),
    )
}
