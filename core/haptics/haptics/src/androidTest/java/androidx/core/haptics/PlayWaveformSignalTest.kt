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
import androidx.core.haptics.signal.WaveformSignal.Companion.off
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.signal.WaveformSignal.Companion.repeatingWaveformOf
import androidx.core.haptics.signal.WaveformSignal.Companion.waveformOf
import androidx.core.haptics.testing.AmplitudeVibrator
import androidx.core.haptics.testing.FakeVibratorSubject.Companion.assertThat
import androidx.core.haptics.testing.PatternVibrator
import androidx.core.haptics.testing.vibration
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(JUnit4::class)
@SmallTest
class PlayWaveformSignalSdk26AndAboveTest {
    private val fakeVibrator = AmplitudeVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @Test
    fun play_withOneShot_vibratesWithOneShotEffect() {
        hapticManager.play(waveformOf(on(durationMillis = 10)))
        hapticManager.play(waveformOf(on(durationMillis = 20, amplitude = 0.2f)))
        assertThat(fakeVibrator).vibratedExactly(
            vibration(timings = longArrayOf(10), amplitudes = intArrayOf(-1)),
            vibration(timings = longArrayOf(20), amplitudes = intArrayOf(51)),
        ).inOrder()
    }

    @Test
    fun play_withAmplitudes_vibratesWithAmplitudes() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10, amplitude = 0.2f),
                on(durationMillis = 20, amplitude = 0.8f),
                on(durationMillis = 30, amplitude = 0f),
                on(durationMillis = 40, amplitude = 1f),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                timings = longArrayOf(10, 20, 30, 40),
                amplitudes = intArrayOf(51, 204, 0, 255),
            )
        )
    }

    @Test
    fun play_withOnOffPattern_vibratesWithAmplitudes() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10),
                off(durationMillis = 20),
                on(durationMillis = 30),
                off(durationMillis = 40),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                timings = longArrayOf(10, 20, 30, 40),
                amplitudes = intArrayOf(-1, 0, -1, 0),
            )
        )
    }

    @Test
    fun play_withRepeatingAmplitudes_vibratesWithRepeatIndex() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10, amplitude = 0.2f),
                on(durationMillis = 20, amplitude = 0.4f),
            ).thenRepeat(
                on(durationMillis = 30, amplitude = 0.6f),
                on(durationMillis = 40, amplitude = 0.8f),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                timings = longArrayOf(10, 20, 30, 40),
                amplitudes = intArrayOf(51, 102, 153, 204),
                repeat = 2,
            )
        )
    }
}

@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
@RunWith(JUnit4::class)
@SmallTest
class PlayWaveformSignalBelowSdk26Test {
    private val fakeVibrator = PatternVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @Test
    fun play_withOneShot_vibratesWithPatternForDefaultAndMaxAmplitudes() {
        hapticManager.play(waveformOf(on(durationMillis = 10)))
        hapticManager.play(waveformOf(on(durationMillis = 20, amplitude = 1f)))
        hapticManager.play(waveformOf(on(durationMillis = 30, amplitude = 0.2f)))
        assertThat(fakeVibrator).vibratedExactly(
            vibration(pattern = longArrayOf(0, 10)),
            vibration(pattern = longArrayOf(0, 20)),
            // Ignores last request with non-default amplitude
        ).inOrder()
    }

    @Test
    fun play_withAmplitudes_doesNotVibrate() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10, amplitude = 0.2f),
                on(durationMillis = 20, amplitude = 0.8f),
                on(durationMillis = 30, amplitude = 0f),
                on(durationMillis = 40, amplitude = 1f),
            )
        )
        assertThat(fakeVibrator).neverVibrated()
    }

    @Test
    fun play_withOnOffPattern_vibratesWithFallbackPattern() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10),
                off(durationMillis = 20),
                on(durationMillis = 30),
                on(durationMillis = 40),
                off(durationMillis = 50),
                off(durationMillis = 60),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            // OFF(0ms), ON(10ms), OFF(20ms), ON(30+40ms), OFF(50+60ms)
            vibration(pattern = longArrayOf(0, 10, 20, 70, 110))
        )
    }

    @Test
    fun play_withOnOffMaxAmplitudePattern_vibratesWithFallbackPattern() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10),
                on(durationMillis = 20, amplitude = 1f),
                off(durationMillis = 30),
                on(durationMillis = 40, amplitude = 0f),
                on(durationMillis = 50),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            // OFF(0ms), ON(10+20ms), OFF(30+40ms), ON(50ms)
            vibration(pattern = longArrayOf(0, 30, 70, 50))
        )
    }

    @Test
    fun play_withRepeatingPattern_vibratesWithRepeatIndex() {
        hapticManager.play(
            repeatingWaveformOf(
                off(durationMillis = 10),
                on(durationMillis = 20),
                off(durationMillis = 30),
                off(durationMillis = 40),
                on(durationMillis = 50),
                on(durationMillis = 60),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                // OFF(10ms), ON(20ms), OFF(30+40ms), ON(50+60ms)
                pattern = longArrayOf(10, 20, 70, 110),
                repeat = 0,
            )
        )
    }

    @Test
    fun play_withInitialAndRepeatingPattern_doesNotMergeInitialWithRepeatingPattern() {
        hapticManager.play(
            waveformOf(
                on(durationMillis = 10),
                off(durationMillis = 20),
                off(durationMillis = 30),
                on(durationMillis = 40),
            ).thenRepeat(
                on(durationMillis = 50),
                on(durationMillis = 60),
                off(durationMillis = 70),
                off(durationMillis = 80),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                // Does not merge consecutive ON steps 40 and 50 because of repeat index.
                // OFF(0ms), ON(10ms), OFF(20+30ms), ON(40ms), OFF(+0ms), ON(50+60ms), OFF(70+80ms)
                pattern = longArrayOf(0, 10, 50, 40, 0, 110, 150),
                repeat = 4,
            )
        )
    }
}

@RunWith(JUnit4::class)
@SmallTest
class PlayWaveformSignalAllSdksTest {
    private val fakeVibrator = AmplitudeVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @Test
    fun play_withZeroDurationSignal_doesNotVibrate() {
        hapticManager.play(waveformOf(on(durationMillis = 0)))
        hapticManager.play(waveformOf(on(durationMillis = 0, amplitude = 0.2f)))
        hapticManager.play(
            waveformOf(
                on(durationMillis = 0, amplitude = 0.2f),
                on(durationMillis = 0, amplitude = 0.8f),
                on(durationMillis = 0, amplitude = 0f),
                on(durationMillis = 0, amplitude = 1f),
                off(durationMillis = 0),
            )
        )
        assertThat(fakeVibrator).neverVibrated()
    }

    @Test
    fun waveformOf_withNoAtom_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            waveformOf()
        }
    }

    @Test
    fun on_withAmplitudeLargerThanOne_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            on(durationMillis = 0, amplitude = 1.1f)
        }
    }

    @Test
    fun on_withNegativeAmplitude_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            on(durationMillis = 0, amplitude = -0.5f)
        }
    }

    @Test
    fun on_withNegativeDuration_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            on(durationMillis = -10)
        }
    }

    @Test
    fun off_withNegativeDuration_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            off(durationMillis = -10)
        }
    }
}
