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

package androidx.ui.material

import androidx.animation.ManualAnimationClock
import androidx.compose.FrameManager.framed
import androidx.test.filters.MediumTest
import androidx.ui.core.TestTag
import androidx.ui.layout.DpConstraints
import androidx.ui.test.assertValueEquals
import androidx.ui.test.createComposeRule
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SliderTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun sliderPosition_defaultConstructor() {
        framed {
            val position = SliderPosition(animatedClock = ManualAnimationClock(0))
            assertTrue(position.startValue == 0f)
            assertTrue(position.endValue == 1f)
            assertTrue(position.value == 0f)
            assertTrue(position.tickFractions.isEmpty())
        }
    }

    @Test
    fun sliderPosition_valueCoercion() {
        framed {
            val position = SliderPosition(animatedClock = ManualAnimationClock(0))
            assertTrue(position.value == 0f)
            assertTrue(position.endValue == 1f)
            position.value = 2f
            assertTrue(position.value == 1f)
            assertTrue(position.startValue == 0f)
            position.value = -10f
            assertTrue(position.value == 0f)
        }
    }

    @Test
    fun sliderPosition_reversedRange() {
        val pos = SliderPosition(
            initial = 0f,
            valueRange = 10f..0f,
            animatedClock = ManualAnimationClock(0)
        )
        assertTrue(pos.startValue == 10f)
        assertTrue(pos.endValue == 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun sliderPosition_stepsThrowWhenLessThanZero() {
        SliderPosition(steps = -1, animatedClock = ManualAnimationClock(0))
    }

    @Test
    fun slider_semantics() {
        val tag = "slider"
        val position = SliderPosition(animatedClock = ManualAnimationClock(0))

        composeTestRule
            .setMaterialContent {
                TestTag(tag = tag) {
                    Slider(position)
                }
            }

        findByTag(tag)
            .assertValueEquals("0.0")

        composeTestRule.runOnUiThread {
            position.value = 0.5f
        }

        findByTag(tag)
            .assertValueEquals("0.5")
    }

    @Test
    fun slider_sizes() {
        val position = SliderPosition(animatedClock = ManualAnimationClock(0))

        composeTestRule
            .setMaterialContentAndCollectSizes(
                parentConstraints = DpConstraints(maxWidth = 100.dp, maxHeight = 100.dp)
            ) { Slider(position) }
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(100.dp)
    }
}