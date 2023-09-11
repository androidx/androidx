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
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PositionIndicatorBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()
    private val defaultPositionIndicatorCaseFactory = { PositionIndicatorBenchmarkTestCase() }

    @Test
    fun changeFraction() {
        benchmarkRule.changePositionBenchmark {
            PositionIndicatorBenchmarkTestCase(targetFraction = 0.5f)
        }
    }

    @Test
    fun changeFractionAndSizeFraction_hide() {
        benchmarkRule.changePositionBenchmark {
            PositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.Hide
            )
        }
    }

    @Test
    fun changeFractionAndSizeFraction_autoHide() {
        benchmarkRule.changePositionBenchmark {
            PositionIndicatorBenchmarkTestCase(
                targetFraction = 0.5f,
                targetSizeFraction = 0.5f,
                targetVisibility = PositionIndicatorVisibility.AutoHide
            )
        }
    }

    @Test
    fun firstPixel() {
        benchmarkRule.benchmarkToFirstPixel(defaultPositionIndicatorCaseFactory)
    }
}

internal class PositionIndicatorBenchmarkTestCase(
    val targetFraction: Float? = null,
    val targetSizeFraction: Float? = null,
    val targetVisibility: PositionIndicatorVisibility? = null
) : LayeredComposeTestCase() {
    private lateinit var positionFraction: MutableState<Float>
    private lateinit var sizeFraction: MutableState<Float>
    private lateinit var visibility: MutableState<PositionIndicatorVisibility>

    fun onChange() {
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
                visibility = { visibility.value })
        }

        PositionIndicator(
            state = state,
            indicatorHeight = 50.dp,
            indicatorWidth = 4.dp,
            paddingHorizontal = 5.dp
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        MaterialTheme {
            content()
        }
    }
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
        disposeContent()
        measureRepeated {
            runWithTimingDisabled {
                setupContent()
                assertNoPendingChanges()
            }
            getTestCase().onChange()
            doFrame()
            runWithTimingDisabled {
                disposeContent()
            }
        }
    }
}
