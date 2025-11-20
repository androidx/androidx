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

import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import androidx.core.haptics.signal.PredefinedEffectSignal.Companion.predefinedClick
import androidx.core.haptics.testing.AttributedVibrationSubject.Companion.assertThat
import androidx.core.haptics.testing.FakeVibratorSubject.Companion.assertThat
import androidx.core.haptics.testing.FullVibrator
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class PlayHapticAttributesTest {
    private val fakeVibrator = FullVibrator()
    private val hapticManager = requireNotNull(HapticManager.createForVibrator(fakeVibrator))

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun play_api33AndAbove_vibratesWithEffectAndVibrationAttributes() {
        hapticManager.play(predefinedClick(), HapticAttributes(HapticAttributes.USAGE_TOUCH))
        assertThat(fakeVibrator).hasVibrationCount(1)

        val vibration = fakeVibrator.vibrations().first()
        assertThat(vibration)
            .hasVibrationEffectThat()
            .isEqualTo(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        assertThat(vibration)
            .hasVibrationAttributesThat()
            .isEqualTo(
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build()
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q, maxSdkVersion = Build.VERSION_CODES.S)
    @Test
    fun play_api29To32_vibratesWithEffectAndAudioAttributes() {
        hapticManager.play(predefinedClick(), HapticAttributes(HapticAttributes.USAGE_TOUCH))
        assertThat(fakeVibrator).hasVibrationCount(1)

        val vibration = fakeVibrator.vibrations().first()
        assertThat(vibration)
            .hasVibrationEffectThat()
            .isEqualTo(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        assertThat(vibration)
            .hasAudioAttributesThat()
            .isEqualTo(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun play_api21To28_vibratesWithPatternAndAudioAttributes() {
        hapticManager.play(predefinedClick(), HapticAttributes(HapticAttributes.USAGE_TOUCH))
        assertThat(fakeVibrator).hasVibrationCount(1)

        val vibration = fakeVibrator.vibrations().first()
        assertThat(vibration).hasPatternVibration()
        assertThat(vibration)
            .hasAudioAttributesThat()
            .isEqualTo(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
    }
}
