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
import androidx.core.haptics.signal.WaveformSignal
import androidx.core.haptics.signal.WaveformSignal.Companion.on
import androidx.core.haptics.testing.FakeVibratorSubject.Companion.assertThat
import androidx.core.haptics.testing.PatternVibrator
import androidx.core.haptics.testing.cancelRequest
import androidx.core.haptics.testing.playRequest
import androidx.core.haptics.testing.vibration
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class PlayAndCancelTest {
    private val fakeVibrator = PatternVibrator()
    private val hapticManager = requireNotNull(HapticManager.createForVibrator(fakeVibrator))

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun playThenCancel_api26AndAbove_sendsRequestToVibratorInOrder() {
        hapticManager.cancel()
        hapticManager.play(
            WaveformSignal.waveformOf(on(durationMillis = 10)),
            HapticAttributes(HapticAttributes.USAGE_TOUCH),
        )
        hapticManager.cancel()
        hapticManager.cancel()
        hapticManager.play(
            WaveformSignal.waveformOf(on(durationMillis = 20)),
            HapticAttributes(HapticAttributes.USAGE_TOUCH),
        )
        hapticManager.cancel()

        assertThat(fakeVibrator)
            .requestedExactly(
                cancelRequest(),
                playRequest(vibration(timings = longArrayOf(10), amplitudes = intArrayOf(-1))),
                cancelRequest(),
                cancelRequest(),
                playRequest(vibration(timings = longArrayOf(20), amplitudes = intArrayOf(-1))),
                cancelRequest(),
            )
            .inOrder()
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.N_MR1)
    @Test
    fun playThenCancel_belowApi26_sendsRequestToVibratorInOrder() {
        hapticManager.cancel()
        hapticManager.play(
            WaveformSignal.waveformOf(on(durationMillis = 10)),
            HapticAttributes(HapticAttributes.USAGE_TOUCH),
        )
        hapticManager.cancel()
        hapticManager.cancel()
        hapticManager.play(
            WaveformSignal.waveformOf(on(durationMillis = 20)),
            HapticAttributes(HapticAttributes.USAGE_TOUCH),
        )
        hapticManager.cancel()

        assertThat(fakeVibrator)
            .requestedExactly(
                cancelRequest(),
                playRequest(vibration(pattern = longArrayOf(0, 10))),
                cancelRequest(),
                cancelRequest(),
                playRequest(vibration(pattern = longArrayOf(0, 20))),
                cancelRequest(),
            )
            .inOrder()
    }
}
