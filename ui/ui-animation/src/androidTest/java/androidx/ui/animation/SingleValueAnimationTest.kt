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

package androidx.ui.animation

import androidx.animation.AnimationVector
import androidx.animation.AnimationVector2D
import androidx.animation.AnimationVector4D
import androidx.animation.FastOutLinearInEasing
import androidx.animation.FastOutSlowInEasing
import androidx.animation.LinearEasing
import androidx.animation.LinearOutSlowInEasing
import androidx.animation.TweenBuilder
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.setValue
import androidx.test.filters.MediumTest
import androidx.ui.foundation.Box
import androidx.ui.geometry.Size
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.waitForIdle
import androidx.ui.unit.Bounds
import androidx.ui.unit.Dp
import androidx.ui.unit.Position
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.util.lerp

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@MediumTest
class SingleValueAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animate1DTest() {
        val startVal = 20f
        val endVal = 250f

        var floatValue = startVal
        var dpValue = startVal.dp
        var pxValue = startVal

        val animConfig: TweenBuilder<*>.() -> Unit = {
            easing = FastOutSlowInEasing
            duration = 100
        }
        val children: @Composable() (Boolean) -> Unit = { enabled ->
            floatValue = animate(
                if (enabled) endVal else startVal,
                TweenBuilder<Float>().apply(animConfig)
            )

            dpValue = animate(
                if (enabled) endVal.dp else startVal.dp,
                TweenBuilder<Dp>().apply(animConfig)
            )

            pxValue = animate(
                if (enabled) endVal else startVal,
                TweenBuilder<Float>().apply(animConfig)
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val value = lerp(
                    startVal.toFloat(), endVal.toFloat(),
                    FastOutSlowInEasing.invoke(i / 100f)
                )
                assertEquals(value, floatValue)
                assertEquals(value.dp, dpValue)
                assertEquals(value, pxValue)
                composeTestRule.clockTestRule.advanceClock(50)
                waitForIdle()
            }
        }

        animateTest(children, verify)
    }

    @Test
    fun animate2DTest() {

        val startVal = AnimationVector(120f, 56f)
        val endVal = AnimationVector(0f, 77f)

        var vectorValue = startVal
        var positionValue = PositionToVectorConverter.convertFromVector(startVal)
        var sizeValue = SizeToVectorConverter.convertFromVector(startVal)
        var pxPositionValue = PxPositionToVectorConverter.convertFromVector(startVal)
        var pxSizeValue = PxSizeToVectorConverter.convertFromVector(startVal)

        val animConfig: TweenBuilder<*>.() -> Unit = {
            easing = LinearEasing
            duration = 100
        }
        val children: @Composable() (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) endVal else startVal,
                TweenBuilder<AnimationVector2D>().apply(animConfig)
            )

            positionValue = animate(
                if (enabled)
                    PositionToVectorConverter.convertFromVector(endVal)
                else
                    PositionToVectorConverter.convertFromVector(startVal),
                TweenBuilder<Position>().apply(animConfig)
            )

            sizeValue = animate(
                if (enabled)
                    SizeToVectorConverter.convertFromVector(endVal)
                else
                    SizeToVectorConverter.convertFromVector(startVal),
                TweenBuilder<Size>().apply(animConfig)
            )

            pxPositionValue = animate(
                if (enabled)
                    PxPositionToVectorConverter.convertFromVector(endVal)
                else
                    PxPositionToVectorConverter.convertFromVector(startVal),
                TweenBuilder<PxPosition>().apply(animConfig)
            )

            pxSizeValue = animate(
                if (enabled)
                    PxSizeToVectorConverter.convertFromVector(endVal)
                else
                    PxSizeToVectorConverter.convertFromVector(startVal),
                TweenBuilder<PxSize>().apply(animConfig)
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val expect = AnimationVector(
                    lerp(startVal.v1, endVal.v1, i / 100f),
                    lerp(startVal.v2, endVal.v2, i / 100f)
                )

                assertEquals(expect, vectorValue)
                assertEquals(SizeToVectorConverter.convertFromVector(expect), sizeValue)
                assertEquals(PositionToVectorConverter.convertFromVector(expect), positionValue)
                assertEquals(PxSizeToVectorConverter.convertFromVector(expect), pxSizeValue)
                assertEquals(PxPositionToVectorConverter.convertFromVector(expect), pxPositionValue)
                composeTestRule.clockTestRule.advanceClock(50)
                waitForIdle()
            }
        }

        animateTest(children, verify)
    }

    @Test
    fun animate4DTest() {
        val startVal = AnimationVector(30f, -76f, 280f, 35f)
        val endVal = AnimationVector(-42f, 89f, 77f, 100f)

        var vectorValue = startVal
        var boundsValue = BoundsToVectorConverter.convertFromVector(startVal)
        var pxBoundsValue = PxBoundsToVectorConverter.convertFromVector(startVal)

        val animConfig: TweenBuilder<*>.() -> Unit = {
            easing = LinearOutSlowInEasing
            duration = 100
        }
        val children: @Composable() (Boolean) -> Unit = { enabled ->
            vectorValue = animate(
                if (enabled) endVal else startVal,
                TweenBuilder<AnimationVector4D>().apply(animConfig)
            )

            boundsValue = animate(
                if (enabled)
                    BoundsToVectorConverter.convertFromVector(endVal)
                else
                    BoundsToVectorConverter.convertFromVector(startVal),
                TweenBuilder<Bounds>().apply(animConfig)
            )

            pxBoundsValue = animate(
                if (enabled)
                    PxBoundsToVectorConverter.convertFromVector(endVal)
                else
                    PxBoundsToVectorConverter.convertFromVector(startVal),
                TweenBuilder<PxBounds>().apply(animConfig)
            )
        }

        val verify: () -> Unit = {
            for (i in 0..100 step 50) {
                val fraction = LinearOutSlowInEasing.invoke(i / 100f)
                val expect = AnimationVector(
                    lerp(startVal.v1, endVal.v1, fraction),
                    lerp(startVal.v2, endVal.v2, fraction),
                    lerp(startVal.v3, endVal.v3, fraction),
                    lerp(startVal.v4, endVal.v4, fraction)
                )

                assertEquals(expect, vectorValue)
                assertEquals(BoundsToVectorConverter.convertFromVector(expect), boundsValue)
                assertEquals(PxBoundsToVectorConverter.convertFromVector(expect), pxBoundsValue)
                composeTestRule.clockTestRule.advanceClock(50)
                waitForIdle()
            }
        }

        animateTest(children, verify)
    }

    @Test
    fun animateColorTest() {
        var value = Color.Black
        val children: @Composable() (Boolean) -> Unit = { enabled ->
            value = animate(if (enabled) Color.Cyan else Color.Black, TweenBuilder<Color>().apply {
                duration = 100
                easing = FastOutLinearInEasing
            })
        }

        val verify: () -> Unit = {

            for (i in 0..100 step 50) {
                val fraction = FastOutLinearInEasing.invoke(i / 100f)
                val expected = lerp(Color.Black, Color.Cyan, fraction)
                assertEquals(expected, value)
                composeTestRule.clockTestRule.advanceClock(50)
                waitForIdle()
            }
        }

        animateTest(children, verify)
    }

    private fun animateTest(children: @Composable() (Boolean) -> Unit, verify: () -> Unit) {

        composeTestRule.clockTestRule.pauseClock()
        var enabled by mutableStateOf(false)
        composeTestRule.setContent {
            Box {
                children(enabled)
            }
        }
        runOnIdleCompose { enabled = true }
        waitForIdle()

        verify()
    }
}
