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

package androidx.compose.ui.benchmark

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.ComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.doFramesUntilNoChangesPending
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalMediaQueryApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.mediaQuery
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.filters.LargeTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Benchmark for evaluating the startup and initial composition performance of the MediaQuery API.
 *
 * It uses the following parameters:
 * - [integrationEnabled]: Whether [ComposeUiFlags.isMediaQueryIntegrationEnabled] is true.
 * - [useMediaQuery]: Whether the Composable tree actually executes a `mediaQuery` lookup.
 */
@OptIn(ExperimentalMediaQueryApi::class, ExperimentalComposeUiApi::class)
@LargeTest
@RunWith(Parameterized::class)
class MediaQueryBenchmark(
    private val integrationEnabled: Boolean,
    private val useMediaQuery: Boolean,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "integrationEnabled={0},useMediaQuery={1}")
        fun parameters() =
            listOf(
                // Baseline: ComposeView creation with the feature flag disabled.
                arrayOf(false, false),
                // Regression check: ComposeView creation with the flag enabled but no media
                // queries. Checks that enabling the flag adds no overhead (verifying that scope
                // initialization and listener registrations are lazily deferred).
                arrayOf(true, false),
                // Integration cost: ComposeView creation with the flag enabled and executing a
                // media query.
                arrayOf(true, true),
            )
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private var originalIsMediaQueryIntegrationEnabled = false

    @Before
    fun setup() {
        originalIsMediaQueryIntegrationEnabled = ComposeUiFlags.isMediaQueryIntegrationEnabled
        ComposeUiFlags.isMediaQueryIntegrationEnabled = integrationEnabled
    }

    @After
    fun tearDown() {
        ComposeUiFlags.isMediaQueryIntegrationEnabled = originalIsMediaQueryIntegrationEnabled
    }

    @Test
    fun initialComposition() {
        with(benchmarkRule) {
            runBenchmarkFor({ ContainingViewTestCase() }) {
                // Wait for the host FrameLayout to be fully laid out and attached before measuring.
                runOnUiThread { doFramesUntilNoChangesPending() }

                measureRepeatedOnUiThread {
                    val containingView = getTestCase().containingView

                    // Create a fresh ComposeView. This initializes AndroidComposeView
                    // and registers/unregisters its listeners when attached/detached.
                    val composeView =
                        ComposeView(containingView.context).also {
                            it.setContent {
                                Box(Modifier.fillMaxSize()) {
                                    if (useMediaQuery) {
                                        val matches = mediaQuery { windowWidth > 200.dp }
                                        if (matches) {
                                            Box(Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }

                    // Add view: triggers constructor and onAttachedToWindow() listener
                    // registration.
                    containingView.addView(composeView)

                    recompose()

                    // Remove view: triggers onDetachedFromWindow() listener unregistration.
                    // Run measurement disabled to avoid counting layout detachment time.
                    runWithMeasurementDisabled { containingView.removeAllViews() }
                }
            }
        }
    }

    /**
     * Test case providing a stable, window-attached FrameLayout parent to host the newly
     * instantiated ComposeViews during the benchmark runs.
     */
    private class ContainingViewTestCase : ComposeTestCase {
        lateinit var containingView: FrameLayout

        @Composable
        override fun Content() {
            AndroidView(factory = { context -> FrameLayout(context).also { containingView = it } })
        }
    }
}
