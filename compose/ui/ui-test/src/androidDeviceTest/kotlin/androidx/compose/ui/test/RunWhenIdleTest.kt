/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RunWhenIdleTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun triggeredAnimation_withRunWhenIdle() = runComposeUiTest {
        var size by mutableStateOf(64.dp)

        var animationIsDone = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.testTag("foo")
                            .animateContentSize { _, _ -> animationIsDone = true }
                            .size(size)
                            .background(Color.Red)
                )
            }
        }

        mainClock.autoAdvance = false

        val timeSeries = mutableListOf<IntSize>()
        size = 32.dp

        while (!animationIsDone) {
            mainClock.advanceTimeByFrame()
            runWhenIdle { captureMotionTestValues(timeSeries) }
        }
        assertThat(timeSeries)
            .containsExactly(
                IntSize(64, 64),
                IntSize(64, 64),
                IntSize(63, 63),
                IntSize(60, 60),
                IntSize(56, 56),
                IntSize(52, 52),
                IntSize(49, 49),
                IntSize(46, 46),
                IntSize(43, 43),
                IntSize(41, 41),
                IntSize(39, 39),
                IntSize(37, 37),
                IntSize(36, 36),
                IntSize(35, 35),
                IntSize(35, 35),
                IntSize(34, 34),
                IntSize(34, 34),
                IntSize(33, 33),
                IntSize(32, 32),
            )
            .inOrder()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun triggeredAnimation_withAwaitAndRunWhenIdle() = runComposeUiTest {
        var size by mutableStateOf(64.dp)

        var animationIsDone = false
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.testTag("foo")
                            .animateContentSize { _, _ -> animationIsDone = true }
                            .size(size)
                            .background(Color.Red)
                )
            }
        }

        mainClock.autoAdvance = false

        val timeSeries = mutableListOf<IntSize>()
        size = 32.dp

        while (!animationIsDone) {
            mainClock.advanceTimeByFrame()
            awaitAndRunWhenIdle { captureMotionTestValues(timeSeries) }
        }
        assertThat(timeSeries)
            .containsExactly(
                IntSize(64, 64),
                IntSize(64, 64),
                IntSize(63, 63),
                IntSize(60, 60),
                IntSize(56, 56),
                IntSize(52, 52),
                IntSize(49, 49),
                IntSize(46, 46),
                IntSize(43, 43),
                IntSize(41, 41),
                IntSize(39, 39),
                IntSize(37, 37),
                IntSize(36, 36),
                IntSize(35, 35),
                IntSize(35, 35),
                IntSize(34, 34),
                IntSize(34, 34),
                IntSize(33, 33),
                IntSize(32, 32),
            )
            .inOrder()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun triggeredAnimation_withHasPendingWork() = runComposeUiTest {
        var size by mutableStateOf(64.dp)

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier =
                        Modifier.testTag("foo")
                            .animateContentSize { _, _ -> }
                            .size(size)
                            .background(Color.Red)
                )
            }
        }

        mainClock.autoAdvance = false
        val timeSeries = mutableListOf<IntSize>()
        // Triggering the animation
        size = 32.dp

        while (hasPendingWork()) {
            mainClock.advanceTimeByFrame()
            waitForIdle()

            captureMotionTestValues(timeSeries)
        }
        assertThat(timeSeries)
            .containsExactly(
                IntSize(64, 64),
                IntSize(64, 64),
                IntSize(63, 63),
                IntSize(60, 60),
                IntSize(56, 56),
                IntSize(52, 52),
                IntSize(49, 49),
                IntSize(46, 46),
                IntSize(43, 43),
                IntSize(41, 41),
                IntSize(39, 39),
                IntSize(37, 37),
                IntSize(36, 36),
                IntSize(35, 35),
                IntSize(35, 35),
                IntSize(34, 34),
                IntSize(34, 34),
                IntSize(33, 33),
                IntSize(32, 32),
            )
            .inOrder()
    }

    /**
     * Illustrative implementation of a "sample the property values of the current frame" method.
     *
     * Motion tests do exactly that, just with more syntactic sugar 🍬.
     */
    @OptIn(ExperimentalTestApi::class)
    private fun ComposeUiTest.captureMotionTestValues(fooSizeTimeSeries: MutableList<IntSize>) {
        // Capture a value and add to the time series.
        fooSizeTimeSeries.add(onNodeWithTag("foo").fetchSemanticsNode().size)

        repeat(10) {
            // simulation of capturing multiple properties.
            // For making the point, this just repeatedly captures the same property.
            onNodeWithTag("foo").fetchSemanticsNode().size
        }
    }
}
