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
import androidx.core.haptics.signal.PredefinedEffectSignal
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedDoubleClick
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedHeavyClick
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedTick
import androidx.core.haptics.testing.FakeVibratorSubject.Companion.assertThat
import androidx.core.haptics.testing.PredefinedEffectsAndAmplitudeVibrator
import androidx.core.haptics.testing.vibration
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SmallTest
class PlayPredefinedEffectSignalTest(
    private val effect: PredefinedEffectSignal,
    private val expectedFallbackPattern: LongArray,
) {
    private val fakeVibrator = PredefinedEffectsAndAmplitudeVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun play_api29AndAbove_vibratesWithPredefinedEffect() {
        hapticManager.play(effect)
        assertThat(fakeVibrator).vibratedExactly(vibration(effect))
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun play_belowApi29_vibratesWithFallbackPattern() {
        hapticManager.play(effect)
        assertThat(fakeVibrator).vibratedExactly(vibration(expectedFallbackPattern))
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "effect:{0}, expectedFallbackPattern:{1}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(predefinedTick(), longArrayOf(0, 10)),
            arrayOf(predefinedClick(), longArrayOf(0, 20)),
            arrayOf(predefinedHeavyClick(), longArrayOf(0, 30)),
            arrayOf(predefinedDoubleClick(), longArrayOf(0, 30, 100, 30)),
        )
    }
}
