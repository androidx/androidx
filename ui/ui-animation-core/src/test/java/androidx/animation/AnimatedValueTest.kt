/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnimatedValueTest {
    private lateinit var clock: ManualAnimationClock

    @Before
    fun init() {
        clock = ManualAnimationClock(0L)
    }

    @Test
    fun testAnimation() {
        val animatedFloat = AnimatedFloat(0f, clock)
        animatedFloat.animateTo(1f)
        clock.clockTimeMillis += 100000L
        assertEquals(1f, animatedFloat.value, epsilon)
    }

    @Test
    fun testInitialValue() {
        val animatedFloat = AnimatedFloat(1.2f, clock)
        animatedFloat.animateTo(100f)
        assertEquals(1.2f, animatedFloat.value, epsilon)
    }

    @Test
    fun testTargetValue() {
        val animatedFloat = AnimatedFloat(0f, clock)
        animatedFloat.animateTo(2f)
        animatedFloat.animateTo(1.5f)
        assertEquals(1.5f, animatedFloat.targetValue, epsilon)
    }

    @Test
    fun testProgressionUp() {
        val animatedFloat = AnimatedFloat(0f, clock)

        animatedFloat.animateTo(1f)

        clock.clockTimeMillis = 0L
        val valueAtTimeZero = animatedFloat.value

        clock.clockTimeMillis = 1L
        val valueAtTimeOne = animatedFloat.value

        assertTrue(valueAtTimeOne > valueAtTimeZero)
    }

    @Test
    fun testProgressionDown() {
        val animatedFloat = AnimatedFloat(0f, clock)

        animatedFloat.animateTo(-1f)

        clock.clockTimeMillis = 0L
        val valueAtTimeZero = animatedFloat.value

        clock.clockTimeMillis = 1L
        val valueAtTimeOne = animatedFloat.value

        assertTrue(valueAtTimeOne < valueAtTimeZero)
    }

    @Test
    fun testEndCallback() {
        val animatedFloat = AnimatedFloat(0f, clock)

        var animationEndReason: AnimationEndReason? = null
        var animationEndValue: Float? = null

        animatedFloat.animateTo(1f) { reason, value ->
            animationEndReason = reason
            animationEndValue = value
        }

        clock.clockTimeMillis += 100000L

        assertNotNull(animationEndValue)
        assertNotNull(animationEndReason)

        assertEquals(1f, animationEndValue!!, epsilon)
        assertEquals(AnimationEndReason.TargetReached, animationEndReason)
    }
}