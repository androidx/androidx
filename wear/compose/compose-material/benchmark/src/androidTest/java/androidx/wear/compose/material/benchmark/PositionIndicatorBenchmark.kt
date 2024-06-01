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

package androidx.wear.compose.material.benchmark

import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.setupContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorDefaults
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SimplePositionIndicatorBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()
    private val defaultPositionIndicatorCaseFactory = {
        SimplePositionIndicatorBenchmarkTestCase(animate = false)
    }

    @Test
    fun changeFraction() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(targetFraction = 0.5f, animate = false)
        }
    }

    @Test
    fun changeFractionAndSizeFraction_hide() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.Hide,
                animate = false
            )
        }
    }

    @Test
    fun changeFractionAndSizeFraction_autoHide() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.AutoHide,
                animate = false
            )
        }
    }

    @Test
    fun changeFraction_withAnimation() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(targetFraction = 0.5f, animate = true)
        }
    }

    @Test
    fun changeFractionAndSizeFraction_hide_withAnimation() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.Hide,
                animate = true
            )
        }
    }

    @Test
    fun changeFractionAndSizeFraction_autoHide_withAnimation() {
        benchmarkRule.changePositionBenchmark {
            SimplePositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.AutoHide,
                animate = true
            )
        }
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(defaultPositionIndicatorCaseFactory)
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class PositionIndicatorWithScalingLazyColumnBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun positionIndicator_withScalingLazyColumn_withScroll() {
        benchmarkRule.changePositionBenchmark {
            PositionIndicatorWithScalingLazyColumnBenchmarkTestCase(true)
        }
    }

    @Test
    fun positionIndicator_withScalingLazyColumn_noScroll() {
        benchmarkRule.changePositionBenchmark {
            PositionIndicatorWithScalingLazyColumnBenchmarkTestCase(false)
        }
    }
}

internal class SimplePositionIndicatorBenchmarkTestCase(
    val targetFraction: Float? = null,
    val targetSizeFraction: Float? = null,
    val targetVisibility: PositionIndicatorVisibility? = null,
    val animate: Boolean
) : PositionIndicatorBenchmarkTestCase() {
    private lateinit var positionFraction: MutableState<Float>
    private lateinit var sizeFraction: MutableState<Float>
    private lateinit var visibility: MutableState<PositionIndicatorVisibility>

    override fun onChange() {
        runBlocking {
            targetFraction?.let { positionFraction.value = targetFraction }
            targetSizeFraction?.let { sizeFraction.value = targetSizeFraction }
            targetVisibility?.let { visibility.value = targetVisibility }
        }
    }

    @Composable
    override fun MeasuredContent() {
        positionFraction = remember { mutableFloatStateOf(0f) }
        sizeFraction = remember { mutableFloatStateOf(.1f) }
        visibility = remember { mutableStateOf(PositionIndicatorVisibility.Show) }

        val state = remember {
            CustomPositionIndicatorState(
                _positionFraction = { positionFraction.value },
                sizeFraction = { sizeFraction.value },
                visibility = { visibility.value }
            )
        }

        PositionIndicator(
            state = state,
            indicatorHeight = 50.dp,
            indicatorWidth = 4.dp,
            paddingHorizontal = 5.dp,
            fadeInAnimationSpec =
                if (animate) PositionIndicatorDefaults.visibilityAnimationSpec else snap(),
            fadeOutAnimationSpec =
                if (animate) PositionIndicatorDefaults.visibilityAnimationSpec else snap(),
            positionAnimationSpec =
                if (animate) PositionIndicatorDefaults.positionAnimationSpec else snap()
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

internal class PositionIndicatorWithScalingLazyColumnBenchmarkTestCase(
    private val withScroll: Boolean = false
) : PositionIndicatorBenchmarkTestCase() {
    private lateinit var slcState: ScalingLazyListState

    override fun onChange() {
        runBlocking { if (withScroll) slcState.scrollToItem(2) }
    }

    @Composable
    override fun MeasuredContent() {
        slcState = rememberScalingLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLazyColumn(
                state = slcState,
            ) {
                items(50) {
                    // By changing the size we can also change the size of the PositionIndicator,
                    // which will allow us to better measure all parts of PositionIndicator math.
                    val height = if (it % 2 == 0) 20.dp else 40.dp
                    Box(modifier = Modifier.fillMaxWidth().height(height))
                }
            }
            PositionIndicator(
                scalingLazyListState = slcState,
                fadeInAnimationSpec = snap(),
                fadeOutAnimationSpec = snap(),
                positionAnimationSpec = snap()
            )
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme { content() }
    }
}

internal abstract class PositionIndicatorBenchmarkTestCase : LayeredComposeTestCase() {
    abstract fun onChange()
}

private class CustomPositionIndicatorState(
    private val _positionFraction: () -> Float,
    private val sizeFraction: () -> Float,
    private val visibility: () -> PositionIndicatorVisibility
) : PositionIndicatorState {
    override val positionFraction: Float
        get() = _positionFraction()

    override fun sizeFraction(scrollableContainerSizePx: Float): Float = sizeFraction()

    override fun visibility(scrollableContainerSizePx: Float): PositionIndicatorVisibility =
        visibility()
}

internal fun ComposeBenchmarkRule.changePositionBenchmark(
    caseFactory: () -> PositionIndicatorBenchmarkTestCase
) {
    runBenchmarkFor(caseFactory) {
        runOnUiThread { disposeContent() }
        measureRepeatedOnUiThread {
            runWithTimingDisabled {
                setupContent()
                assertNoPendingChanges()
            }
            getTestCase().onChange()
            doFrame()
            runWithTimingDisabled { disposeContent() }
        }
    }
}
