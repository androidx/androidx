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

package androidx.compose.foundation.layout.benchmark

import androidx.benchmark.BlackHole
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.RectRulers
import androidx.compose.ui.layout.innermostOf
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule

@LargeTest
class WindowInsetsBenchmark {

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun readImeInset() =
        benchmarkRule.benchmarkFirstCompose {
            object : LayeredComposeTestCase() {
                @Composable
                override fun MeasuredContent() {
                    WindowInsets.ime
                }
            }
        }

    @Test
    fun applyImeInset() =
        benchmarkRule.benchmarkFirstCompose {
            object : LayeredComposeTestCase() {
                @Composable
                override fun MeasuredContent() {
                    Box(Modifier.windowInsetsPadding(WindowInsets.ime))
                }
            }
        }

    @Test
    fun provideRulers() =
        benchmarkRule.benchmarkFirstMeasure {
            object : LayeredComposeTestCase() {
                val rulers = RectRulers()

                @Composable
                override fun MeasuredContent() {
                    Layout { _, _ ->
                        layout(
                            100,
                            100,
                            rulers = {
                                rulers.top provides 0.5f
                                rulers.left provides 0.5f
                                rulers.right provides 0.5f
                                rulers.bottom provides 0.5f
                            },
                        ) {}
                    }
                }
            }
        }

    @Test
    fun readMergedRulers() =
        benchmarkRule.benchmarkToFirstPixel {
            object : LayeredComposeTestCase() {
                val rulers1 = RectRulers()
                val rulers2 = RectRulers()
                val innerMost = RectRulers.innermostOf(rulers1, rulers2)

                @Composable
                override fun MeasuredContent() {
                    Layout { _, _ ->
                        layout(
                            100,
                            100,
                            rulers = {
                                rulers1.top provides 0.5f
                                rulers1.left provides 0.5f
                                rulers2.right provides 0.5f
                                rulers2.bottom provides 0.5f
                            },
                        ) {
                            BlackHole.consume(
                                Rect(
                                    innerMost.left.current(-1f),
                                    innerMost.top.current(-1f),
                                    innerMost.right.current(-1f),
                                    innerMost.bottom.current(-1f),
                                )
                            )
                        }
                    }
                }
            }
        }

    @Test fun initRulers() = benchmarkRule.measureRepeated { BlackHole.consume(RectRulers()) }
}
