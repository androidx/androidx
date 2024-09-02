/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NestedScrollingBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val nestedScrollingCaseFactory = { NestedScrollingTestCase() }

    @Test
    fun nested_scroll_propagation() {
        benchmarkRule.runBenchmarkFor(nestedScrollingCaseFactory) {
            runOnUiThread { doFramesUntilNoChangesPending() }

            benchmarkRule.measureRepeatedOnUiThread {
                getTestCase().toggleState()
                runWithTimingDisabled {
                    assertNoPendingChanges()
                    getTestCase().assertPostToggle()
                }
            }
        }
    }
}

class NestedScrollingTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private var collectedDeltasOuter = Offset.Zero
    private var collectedDeltasMiddle = Offset.Zero
    private var collectedVelocityOuter = Velocity.Zero
    private var collectedVelocityMiddle = Velocity.Zero

    private val outerConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                collectedDeltasOuter += available
                return super.onPreScroll(available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                collectedVelocityOuter += available
                return super.onPreFling(available)
            }
        }

    private val middleConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                collectedDeltasMiddle += available
                return super.onPreScroll(available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                collectedVelocityMiddle += available
                return super.onPreFling(available)
            }
        }

    private val dispatcher = NestedScrollDispatcher()
    private val noOpConnection = object : NestedScrollConnection {}
    private val delta = Offset(200f, 200f)
    private val velocity = Velocity(2000f, 200f)
    private var velocityResult = Velocity.Zero
    private val IntermediateConnection = object : NestedScrollConnection {}

    @Composable
    override fun MeasuredContent() {
        Box(modifier = Modifier.nestedScroll(outerConnection)) {
            Box(modifier = Modifier.nestedScroll(middleConnection)) {
                NestedBox(boxLevel = 20) {
                    Box(modifier = Modifier.nestedScroll(noOpConnection, dispatcher))
                }
            }
        }
    }

    @Composable
    private fun NestedBox(boxLevel: Int, leafContent: @Composable () -> Unit) {
        if (boxLevel == 0) {
            Box { leafContent() }
            return
        }

        Box(modifier = Modifier.nestedScroll(IntermediateConnection)) {
            NestedBox(boxLevel = boxLevel - 1, leafContent)
        }
    }

    override fun toggleState() {
        val scrollResult = dispatcher.dispatchPreScroll(delta, NestedScrollSource.UserInput)
        dispatcher.dispatchPostScroll(delta, scrollResult, NestedScrollSource.UserInput)

        runBlocking {
            velocityResult = dispatcher.dispatchPreFling(velocity)
            velocityResult = dispatcher.dispatchPostFling(velocity, velocityResult)
        }
    }

    fun assertPostToggle() {
        assertNotEquals(collectedDeltasOuter, Offset.Zero)
        assertNotEquals(collectedDeltasMiddle, Offset.Zero)
        assertNotEquals(collectedVelocityOuter, Velocity.Zero)
        assertNotEquals(collectedVelocityMiddle, Velocity.Zero)
    }
}
