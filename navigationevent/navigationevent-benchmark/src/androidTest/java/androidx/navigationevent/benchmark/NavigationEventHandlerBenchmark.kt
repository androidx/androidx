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

package androidx.navigationevent.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.recomposeUntilNoChangesPending
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo.None
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavigationEventHandlerBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun first_compose() {
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = NavigationEventDispatcher()
            }

        benchmarkRule.benchmarkFirstCompose {
            object : LayeredComposeTestCase() {
                @Composable
                override fun MeasuredContent() {
                    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                        val state = rememberNavigationEventState(None)
                        NavigationEventHandler(state)
                    }
                }
            }
        }
    }

    @Test
    fun event_progress() {
        val dispatcher = NavigationEventDispatcher()
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)

        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }

        benchmarkRule.run {
            runBenchmarkFor(
                givenTestCase =
                    LayeredCaseAdapter.of {
                        object : LayeredComposeTestCase() {
                            @Composable
                            override fun MeasuredContent() {
                                CompositionLocalProvider(
                                    LocalNavigationEventDispatcherOwner provides owner
                                ) {
                                    val state = rememberNavigationEventState(None)
                                    NavigationEventHandler(state)
                                }
                            }
                        }
                    }
            ) {
                measureRepeatedOnUiThread {
                    runWithMeasurementDisabled {
                        doFramesUntilNoChangesPending()
                        // Add the content to benchmark
                        getTestCase().addMeasuredContent()
                        doFramesUntilNoChangesPending()
                    }

                    input.backStarted(NavigationEvent())
                    recomposeUntilNoChangesPending()
                    input.backProgressed(NavigationEvent(progress = 0.5f))
                    recomposeUntilNoChangesPending()
                    input.backCompleted()
                    recomposeUntilNoChangesPending()

                    runWithMeasurementDisabled { disposeContent() }
                }
            }
        }
    }
}

private class LayeredCaseAdapter(private val innerCase: LayeredComposeTestCase) : ComposeTestCase {

    companion object {
        fun of(caseFactory: () -> LayeredComposeTestCase): () -> LayeredCaseAdapter = {
            LayeredCaseAdapter(caseFactory())
        }
    }

    var isComposed by mutableStateOf(false)

    @Composable
    override fun Content() {
        innerCase.ContentWrappers {
            if (isComposed) {
                innerCase.MeasuredContent()
            }
        }
    }

    fun addMeasuredContent() {
        assertTrue(!isComposed)
        isComposed = true
    }
}
