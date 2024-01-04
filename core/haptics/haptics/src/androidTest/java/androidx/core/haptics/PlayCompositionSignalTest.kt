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
import androidx.core.haptics.signal.CompositionSignal.Companion.click
import androidx.core.haptics.signal.CompositionSignal.Companion.compositionOf
import androidx.core.haptics.signal.CompositionSignal.Companion.lowTick
import androidx.core.haptics.signal.CompositionSignal.Companion.off
import androidx.core.haptics.signal.CompositionSignal.Companion.quickFall
import androidx.core.haptics.signal.CompositionSignal.Companion.quickRise
import androidx.core.haptics.signal.CompositionSignal.Companion.slowRise
import androidx.core.haptics.signal.CompositionSignal.Companion.spin
import androidx.core.haptics.signal.CompositionSignal.Companion.thud
import androidx.core.haptics.signal.CompositionSignal.Companion.tick
import androidx.core.haptics.signal.CompositionSignal.PrimitiveAtom
import androidx.core.haptics.testing.CompositionPrimitive
import androidx.core.haptics.testing.FakeVibratorSubject.Companion.assertThat
import androidx.core.haptics.testing.FullVibrator
import androidx.core.haptics.testing.vibration
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(Parameterized::class)
@SmallTest
class PlayCompositionSignalSdk30AndAboveTest(
    private val primitive: PrimitiveAtom,
) {
    private val fakeVibrator = FullVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @Test
    fun play_vibratesWithSupportedPrimitives() {
        hapticManager.play(
            compositionOf(
                primitive,
                off(durationMillis = 50),
                primitive.withAmplitudeScale(0.5f),
                off(durationMillis = 100),
                primitive.withAmplitudeScale(0.8f),
                off(durationMillis = 200),
            )
        )
        assertThat(fakeVibrator).vibratedExactly(
            vibration(
                CompositionPrimitive(primitive),
                CompositionPrimitive(primitive, scale = 0.5f, delay = 50.milliseconds),
                CompositionPrimitive(primitive, scale = 0.8f, delay = 100.milliseconds),
                // Skips trailing 200ms delay from vibrate call
            )
        )
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "primitive:{0}")
        fun data(): Collection<Any> {
            val primitives = mutableListOf(
                tick(),
                click(),
                slowRise(),
                quickRise(),
                quickFall(),
            )
            if (Build.VERSION.SDK_INT >= 31) {
                primitives.apply {
                    add(lowTick())
                    add(spin())
                    add(thud())
                }
            }
            return primitives
        }
    }
}

@SdkSuppress(maxSdkVersion = Build.VERSION_CODES.Q)
@RunWith(Parameterized::class)
@SmallTest
class PlayCompositionSignalBelowSdk30Test(
    private val primitive: PrimitiveAtom,
) {
    private val fakeVibrator = FullVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @Test
    fun play_doesNotVibrate() {
        hapticManager.play(compositionOf(primitive))
        assertThat(fakeVibrator).neverVibrated()
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "primitive:{0}")
        fun data(): Collection<Any> = mutableListOf(
            tick(),
            click(),
            slowRise(),
            quickRise(),
            quickFall(),
            lowTick(),
            spin(),
            thud(),
        )
    }
}

@RunWith(JUnit4::class)
@SmallTest
class PlayCompositionSignalPartialPrimitiveSdkSupportTest {
    private val fakeVibrator = FullVibrator()
    private val hapticManager = HapticManager.createForVibrator(fakeVibrator)

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R, maxSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun play_api30AndPrimitiveFromApi31AndAbove_doesNotVibrate() {
        hapticManager.play(compositionOf(lowTick()))
        hapticManager.play(compositionOf(thud()))
        hapticManager.play(compositionOf(spin()))
        // Mix supported/unsupported primitives
        hapticManager.play(compositionOf(tick(), lowTick()))
        assertThat(fakeVibrator).neverVibrated()
    }
}

@RunWith(JUnit4::class)
@SmallTest
class PlayCompositionSignalAllSdksTest {

    @Test
    fun compositionOf_withNoAtom_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            compositionOf()
        }
    }

    @Test
    fun off_withNegativeDuration_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            off(durationMillis = -10)
        }
    }

    @Test
    fun withAmplitudeScale_withAmplitudeLargerThanOne_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            click().withAmplitudeScale(2f)
        }
    }

    @Test
    fun withAmplitudeScale_withNegativeAmplitude_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            click().withAmplitudeScale(-1f)
        }
    }
}
