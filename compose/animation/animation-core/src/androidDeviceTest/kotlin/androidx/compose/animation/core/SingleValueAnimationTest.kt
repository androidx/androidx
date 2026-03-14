/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class SingleValueAnimationTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun animate1DTest() {
        fun <T> myTween(): TweenSpec<T> =
            TweenSpec(easing = FastOutSlowInEasing, durationMillis = 100)

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                val animationValue by animateDpAsState(if (enabled) 50.dp else 250.dp, myTween())
                // TODO: Properly test this with a deterministic clock when the test framework is
                // ready
                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(0, 100)
                                val fraction = FastOutSlowInEasing.transform(playTime / 100f)
                                expected = lerp(250f, 50f, fraction)
                            }
                        } while (frameTime - startTime <= 100_000_000L)
                        // Animation is finished at this point
                        expected = 50f
                    }
                    assertEquals(expected.dp, animationValue)
                } else {
                    assertEquals(250.dp, animationValue)
                }
            }
        }
        assertEquals(250f, expected)
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(50f, expected)
    }

    @Test
    fun animate1DOnCoroutineTest() {
        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                // Animate from 250f to 50f when enable flips to true
                val animationValue by
                    animateFloatAsState(
                        if (enabled) 50f else 250f,
                        tween(200, easing = FastOutLinearInEasing),
                    )
                // TODO: Properly test this with a deterministic clock when the test framework is
                // ready
                if (enabled) {
                    LaunchedEffect(Unit) {
                        assertEquals(250f, animationValue)
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(0, 200)
                                val fraction = FastOutLinearInEasing.transform(playTime / 200f)
                                expected = lerp(250f, 50f, fraction)
                            }
                        } while (frameTime - startTime <= 200_000_000L)
                        expected = 50f
                    }
                }
                assertEquals(expected, animationValue)
            }
        }
        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(50f, expected)
    }

    @Test
    fun animate2DTest() {

        val startVal = AnimationVector(120f, 56f)
        val endVal = AnimationVector(0f, 77f)
        var expected by mutableStateOf(startVal)

        fun <V> tween(): TweenSpec<V> = TweenSpec(easing = LinearEasing, durationMillis = 100)

        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val sizeValue by
                    animateSizeAsState(
                        if (enabled) Size.VectorConverter.convertFromVector(endVal)
                        else Size.VectorConverter.convertFromVector(startVal),
                        tween(),
                    )

                val pxPositionValue by
                    animateOffsetAsState(
                        if (enabled) Offset.VectorConverter.convertFromVector(endVal)
                        else Offset.VectorConverter.convertFromVector(startVal),
                        tween(),
                    )

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(0, 100)
                                expected =
                                    AnimationVector(
                                        lerp(startVal.v1, endVal.v1, playTime / 100f),
                                        lerp(startVal.v2, endVal.v2, playTime / 100f),
                                    )
                            }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = endVal
                    }
                }

                assertEquals(Size.VectorConverter.convertFromVector(expected), sizeValue)
                assertEquals(Offset.VectorConverter.convertFromVector(expected), pxPositionValue)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(endVal, expected)
    }

    @Test
    fun animate4DRectTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        fun <V> tween(): TweenSpec<V> =
            TweenSpec(easing = LinearOutSlowInEasing, durationMillis = 100)

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(startVal)
        rule.setContent {
            Box {
                val pxBoundsValue by
                    animateRectAsState(
                        if (enabled) Rect.VectorConverter.convertFromVector(endVal)
                        else Rect.VectorConverter.convertFromVector(startVal),
                        tween(),
                    )

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(0, 100)

                                val fraction = LinearOutSlowInEasing.transform(playTime / 100f)
                                expected =
                                    AnimationVector(
                                        lerp(startVal.v1, endVal.v1, fraction),
                                        lerp(startVal.v2, endVal.v2, fraction),
                                        lerp(startVal.v3, endVal.v3, fraction),
                                        lerp(startVal.v4, endVal.v4, fraction),
                                    )
                            }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = endVal
                    }
                }

                // Check this every frame
                assertEquals(Rect.VectorConverter.convertFromVector(expected), pxBoundsValue)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(endVal, expected)
    }

    @Test
    fun animateColorTest() {
        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(Color.Black)
        rule.setContent {
            Box {
                val value by
                    animateColorAsState(
                        if (enabled) Color.Cyan else Color.Black,
                        TweenSpec(durationMillis = 100, easing = FastOutLinearInEasing),
                    )
                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(0, 100)
                                val fraction = FastOutLinearInEasing.transform(playTime / 100f)
                                expected = lerp(Color.Black, Color.Cyan, fraction)
                            }
                        } while (frameTime - startTime <= 100_000_000L)
                        expected = Color.Cyan
                    }
                }
                // Check every frame
                assertEquals(expected.red, value.red, 1 / 255f)
                assertEquals(expected.green, value.green, 1 / 255f)
                assertEquals(expected.blue, value.blue, 1 / 255f)
                assertEquals(expected.alpha, value.alpha, 1 / 255f)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
        assertEquals(Color.Cyan, expected)
    }

    @Test
    fun frameByFrameInterruptionTest() {
        var enabled by mutableStateOf(false)
        var currentValue by mutableStateOf(Offset(-300f, -300f))
        rule.setContent {
            Box {
                var destination: Offset by remember { mutableStateOf(Offset(600f, 600f)) }
                val offsetValue = animateOffsetAsState(if (enabled) destination else Offset(0f, 0f))
                if (enabled) {
                    LaunchedEffect(enabled) {
                        var startTime = -1L
                        while (true) {
                            val current = withFrameMillis {
                                if (startTime < 0) startTime = it
                                // Fuzzy test by fine adjusting the target on every frame, and
                                // verify there's a reasonable amount of test. This is to make sure
                                // the animation does not stay "frozen" when there's continuous
                                // target changes.
                                if (destination.x >= 600) {
                                    destination = Offset(599f, 599f)
                                } else {
                                    destination = Offset(601f, 601f)
                                }
                                it
                            }
                            currentValue = offsetValue.value
                            if (current - startTime > 1000) {
                                break
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            enabled = true
            assertEquals(Offset(-300f, -300f), currentValue)
        }
        rule.waitUntil(1300) { currentValue.x > 300f && currentValue.y > 300f }
    }

    @Test
    fun visibilityThresholdTest() {

        val specForFloat = FloatSpringSpec(visibilityThreshold = 0.01f)
        val specForOffset = FloatSpringSpec(visibilityThreshold = 0.5f)

        val animationSpecForOffset = spring<Offset>(visibilityThreshold = Offset(0.5f, 0.5f))

        var expectedFloat by mutableStateOf(0f)
        var expectedOffset by mutableStateOf(Offset(0f, 0f))
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val offsetValue by
                    animateOffsetAsState(
                        if (enabled) Offset(100f, 100f) else Offset(0f, 0f),
                        animationSpecForOffset,
                    )

                val floatValue by animateFloatAsState(if (enabled) 100f else 0f, specForFloat)

                val durationForFloat = specForFloat.getDurationNanos(0f, 100f, 0f)
                val durationForOffset = specForOffset.getDurationNanos(0f, 100f, 0f)

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime = frameTime - startTime
                                expectedFloat =
                                    if (playTime < durationForFloat) {
                                        specForFloat.getValueFromNanos(playTime, 0f, 100f, 0f)
                                    } else {
                                        100f
                                    }

                                expectedOffset =
                                    if (playTime < durationForOffset) {
                                        val offset =
                                            specForOffset.getValueFromNanos(playTime, 0f, 100f, 0f)
                                        Offset(offset, offset)
                                    } else {
                                        Offset(100f, 100f)
                                    }
                            }
                        } while (frameTime - startTime <= durationForFloat)
                        expectedFloat = 100f
                    }
                }

                assertEquals(expectedOffset, offsetValue)
                assertEquals(expectedFloat, floatValue)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun defaultVisibilityThresholdTest() {

        val specForFloat =
            FloatSpringSpec(visibilityThreshold = Dp.Companion.VisibilityThreshold.value)
        val specForOffset =
            FloatSpringSpec(visibilityThreshold = Offset.Companion.VisibilityThreshold.x)

        var expectedFloat by mutableStateOf(0f)
        var expectedOffset by mutableStateOf(Offset(0f, 0f))
        var enabled by mutableStateOf(false)
        rule.setContent {
            Box {
                val offsetValue by
                    animateOffsetAsState(if (enabled) Offset(100f, 100f) else Offset(0f, 0f))

                val floatValue by animateFloatAsState(if (enabled) 100f else 0f)

                val durationForFloat = specForFloat.getDurationNanos(0f, 100f, 0f)
                val durationForOffset = specForOffset.getDurationNanos(0f, 100f, 0f)

                if (enabled) {
                    LaunchedEffect(Unit) {
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime = frameTime - startTime
                                expectedFloat =
                                    if (playTime < durationForFloat) {
                                        specForFloat.getValueFromNanos(playTime, 0f, 100f, 0f)
                                    } else {
                                        100f
                                    }

                                expectedOffset =
                                    if (playTime < durationForOffset) {
                                        val offset =
                                            specForOffset.getValueFromNanos(playTime, 0f, 100f, 0f)
                                        Offset(offset, offset)
                                    } else {
                                        Offset(100f, 100f)
                                    }
                            }
                        } while (frameTime - startTime <= durationForFloat)
                        expectedFloat = 100f
                    }
                }

                // The expected values and actual values should have a delta no larger than
                // the visibility threshold
                assertEquals(
                    expectedOffset.x,
                    offsetValue.x,
                    Dp.Companion.VisibilityThreshold.value,
                )
                assertEquals(
                    expectedOffset.y,
                    offsetValue.y,
                    Dp.Companion.VisibilityThreshold.value,
                )
                assertEquals(expectedFloat, floatValue, Offset.Companion.VisibilityThreshold.x)
            }
        }

        rule.runOnIdle { enabled = true }
        rule.waitForIdle()
    }

    @Test
    fun customSpringSpecVisibilityThresholdTest() {
        rule.mainClock.autoAdvance = false
        val threshold = 0.1f
        // Use a very low stiffness to make the animation slow and easy to track
        val customSpec =
            spring<Float>(stiffness = Spring.StiffnessVeryLow, visibilityThreshold = threshold)
        var enabled by mutableStateOf(false)
        var latestValue = 0f
        var isFinished = false
        rule.setContent {
            val floatValue by
                animateFloatAsState(
                    if (enabled) 1f else 0f,
                    customSpec,
                    finishedListener = { isFinished = true },
                )
            latestValue = floatValue
        }

        rule.runOnIdle { enabled = true }

        // Advance the clock frame by frame and record values
        val values = mutableListOf<Float>()
        while (!isFinished) {
            values.add(latestValue)
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        values.add(latestValue)

        assertTrue("Animation should be finished", isFinished)
        // Ensure it reached 1f
        assertEquals(1f, values.last())

        // Target value is 1f. Threshold is 0.1f.
        // Once the value is within threshold of target (i.e. > 0.9f), it should snap to 1f.
        // So no value should be in the range (0.9, 1.0)
        for (v in values) {
            if (v != 1f) {
                assertTrue(
                    "Value $v should not be in the range (0.9, 1.0) given threshold $threshold",
                    v <= 1f - threshold,
                )
            }
        }

        // Ensure we actually animated and didn't just snap immediately from 0 to 1
        assertTrue("Should have multiple values", values.size > 2)
    }

    @Test
    fun customSpringSpecLargeVisibilityThresholdTest() {
        rule.mainClock.autoAdvance = false
        val threshold = 5f
        val startValue = 200f
        val targetValue = 1000f
        // Use a very low stiffness to make the animation slow and easy to track
        val customSpec =
            spring<Float>(stiffness = Spring.StiffnessVeryLow, visibilityThreshold = threshold)
        var target by mutableStateOf(startValue)
        var latestValue = startValue
        var isFinished = false
        rule.setContent {
            val floatValue by
                animateFloatAsState(target, customSpec, finishedListener = { isFinished = true })
            latestValue = floatValue
        }

        rule.runOnIdle { target = targetValue }

        // Advance the clock frame by frame and record values
        val values = mutableListOf<Float>()
        while (!isFinished) {
            values.add(latestValue)
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            // Safety break to avoid infinite loop
            if (values.size > 2000) break
        }
        values.add(latestValue)

        assertTrue("Animation should be finished", isFinished)
        // Ensure it reached 1000f
        assertEquals(targetValue, values.last())

        // Target value is 1000f. Threshold is 5f.
        // Once the value is within threshold of target (i.e. > 995f), it should snap to 1000f.
        // So no value should be in the range (995, 1000)
        for (v in values) {
            if (v != targetValue) {
                assertTrue(
                    "Error: Value = $v, when values in the range (995, 1000) should be snapped to 1000 given threshold $threshold",
                    v <= targetValue - threshold,
                )
            }
        }

        // Ensure we actually animated
        assertTrue("Should have multiple values", values.size > 2)
    }

    @Test
    fun updateAnimationSpecTest() {
        var duration by mutableStateOf(100)
        var firstRun by mutableStateOf(true)
        fun <T> myTween(): TweenSpec<T> =
            TweenSpec(easing = FastOutSlowInEasing, durationMillis = duration)

        var enabled by mutableStateOf(false)
        var expected by mutableStateOf(250f)
        rule.setContent {
            Box {
                val animationValue by animateDpAsState(if (enabled) 50.dp else 250.dp, myTween())
                assertEquals(expected.dp, animationValue)
                if (!firstRun) {
                    LaunchedEffect(enabled) {
                        if (enabled) {
                            assertEquals(100, duration)
                        } else {
                            assertEquals(200, duration)
                        }
                        val startTime = withFrameNanos { it }
                        var frameTime = startTime
                        do {
                            withFrameNanos {
                                frameTime = it
                                val playTime =
                                    ((frameTime - startTime) / 1_000_000L).coerceIn(
                                        0,
                                        duration.toLong(),
                                    )
                                val fraction =
                                    FastOutSlowInEasing.transform(playTime / duration.toFloat())
                                expected =
                                    if (enabled) {
                                        lerp(250f, 50f, fraction)
                                    } else {
                                        lerp(50f, 250f, fraction)
                                    }
                            }
                        } while (frameTime - startTime <= duration * 1_000_000L)
                        expected = if (enabled) 50f else 250f
                    }
                }
            }
        }
        rule.runOnIdle {
            enabled = true
            firstRun = false
        }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(50f, expected)

        rule.runOnIdle {
            enabled = false
            duration = 200
        }
        rule.waitForIdle()
        // Animation is finished at this point
        assertEquals(250f, expected)
    }
}
