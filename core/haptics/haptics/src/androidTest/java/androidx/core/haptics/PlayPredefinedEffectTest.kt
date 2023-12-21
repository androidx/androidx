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
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.haptics.signal.PredefinedEffect
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedDoubleClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedHeavyClick
import androidx.core.haptics.signal.PredefinedEffect.Companion.PredefinedTick
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(Parameterized::class)
@SmallTest
class PlayPredefinedEffectTest(
    private val effect: PredefinedEffect,
    private val expectedFallbackPattern: LongArray,
) {
    // Vibrator has package-protected constructor and cannot be extended by a FakeVibrator
    // TODO(b/275084444): replace with a testable interface to allow all SDK levels
    private val vibrator = mock(Vibrator::class.java)
    private val hapticManager = HapticManager.createForVibrator(vibrator)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun perform_api29AndAbove() {
        hapticManager.play(effect)
        verify(vibrator).vibrate(eq(VibrationEffect.createPredefined(effect.effectId)))
    }

    @Suppress("DEPRECATION") // Verifying deprecated APIs are triggered by this test
    @SdkSuppress(
        minSdkVersion = 28, // TODO(b/275084444): remove this once we introduce fake vibrator
        maxSdkVersion = Build.VERSION_CODES.P
    )
    @Test
    fun perform_belowApi29() {
        hapticManager.play(effect)
        verify(vibrator).vibrate(eq(expectedFallbackPattern), eq(-1))
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "effect:{0}, expectedFallbackPattern:{1}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(PredefinedTick, longArrayOf(0, 10)),
            arrayOf(PredefinedClick, longArrayOf(0, 20)),
            arrayOf(PredefinedHeavyClick, longArrayOf(0, 30)),
            arrayOf(PredefinedDoubleClick, longArrayOf(0, 30, 100, 30)),
        )
    }
}
