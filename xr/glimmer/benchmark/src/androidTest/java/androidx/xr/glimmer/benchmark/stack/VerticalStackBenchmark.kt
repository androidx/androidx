/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.benchmark.stack

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.assertNoPendingChanges
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.stack.StackState
import androidx.xr.glimmer.stack.VerticalStack
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class VerticalStackBenchmark {

    @get:Rule(0) val benchmarkRule = ComposeBenchmarkRule()

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose { VerticalStackTestCase() }
    }

    @Test
    fun first_pixel() {
        benchmarkRule.benchmarkToFirstPixel { VerticalStackTestCase() }
    }

    @Test
    fun scroll_firstFrame() {
        with(benchmarkRule) {
            runBenchmarkFor({ VerticalStackTestCase() }) {
                runOnUiThread { doFramesUntilNoChangesPending() }

                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        getTestCase().resetScroll()
                        doFramesUntilNoChangesPending()
                        getTestCase().beforeScroll()
                        assertNoPendingChanges()
                    }

                    getTestCase().scroll()
                    doFrame()

                    runWithMeasurementDisabled { getTestCase().afterScroll() }
                }
            }
        }
    }
}

private class VerticalStackTestCase : LayeredComposeTestCase() {

    private val stackState = StackState()
    private var stackHeightPx = 0f

    @Composable
    override fun MeasuredContent() {
        VerticalStack(state = stackState, modifier = Modifier.height(StackHeight)) {
            items(10) { index ->
                Box(modifier = Modifier.focusable().itemDecoration(RectangleShape)) {
                    Text(
                        "Item-$index",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        with(LocalDensity.current) { stackHeightPx = StackHeight.toPx() }
        GlimmerTheme { content() }
    }

    fun resetScroll() {
        runBlocking { stackState.scrollToItem(0) }
    }

    fun beforeScroll() {
        assertEquals(0, stackState.topItem)
    }

    fun scroll() {
        stackState.dispatchRawDelta(stackHeightPx)
    }

    fun afterScroll() {
        assertEquals(1, stackState.topItem)
    }
}

private val StackHeight = 300.dp
