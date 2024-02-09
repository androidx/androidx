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

package androidx.compose.material3.adaptive.benchmark

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculateSupportingPaneScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.graphics.Color
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class SupportingPaneScaffoldBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun singlePane_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel {
            SupportingPaneScaffoldTestCase().apply {
                currentScaffoldDirective = singlePaneDirective
            }
        }
    }

    @Test
    fun dualPane_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel {
            SupportingPaneScaffoldTestCase().apply {
                currentScaffoldDirective = dualPaneDirective
            }
        }
    }

    @Test
    fun singlePane_firstCompose() {
        benchmarkRule.benchmarkFirstCompose {
            SupportingPaneScaffoldTestCase().apply {
                currentScaffoldDirective = singlePaneDirective
            }
        }
    }

    @Test
    fun dualPane_firstCompose() {
        benchmarkRule.benchmarkFirstCompose {
            SupportingPaneScaffoldTestCase().apply {
                currentScaffoldDirective = dualPaneDirective
            }
        }
    }

    @Test
    fun singlePane_navigateBetweenMainAndSupporting() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase() {
                    override fun toggleState() {
                        val newPane =
                            if (currentDestination.pane == SupportingPaneScaffoldRole.Main) {
                                SupportingPaneScaffoldRole.Supporting
                            } else {
                                SupportingPaneScaffoldRole.Main
                            }
                        currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                    }
                }.apply {
                    currentScaffoldDirective = singlePaneDirective
                }
            }
        )
    }

    @Test
    fun dualPane_navigateToExtra() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase() {
                    override fun toggleState() {
                        val newPane =
                            if (currentDestination.pane == SupportingPaneScaffoldRole.Main) {
                                SupportingPaneScaffoldRole.Extra
                            } else {
                                SupportingPaneScaffoldRole.Main
                            }
                        currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                    }
                }.apply {
                    currentScaffoldDirective = dualPaneDirective
                }
            }
        )
    }

    @Test
    fun singlePane_navigateToSupporting_animated() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase(animated = true) {
                    override fun toggleState() {
                        val newPane =
                            if (currentDestination.pane == SupportingPaneScaffoldRole.Main) {
                                SupportingPaneScaffoldRole.Supporting
                            } else {
                                SupportingPaneScaffoldRole.Main
                            }
                        currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                    }
                }.apply {
                    currentScaffoldDirective = singlePaneDirective
                }
            },
            // For skipping animations
            assertOneRecomposition = false
        )
    }

    @Test
    fun dualPane_navigateToExtra_animated() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase(animated = true) {
                    override fun toggleState() {
                        val newPane =
                            if (currentDestination.pane == SupportingPaneScaffoldRole.Main) {
                                SupportingPaneScaffoldRole.Extra
                            } else {
                                SupportingPaneScaffoldRole.Main
                            }
                        currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                    }
                }.apply {
                    currentScaffoldDirective = dualPaneDirective
                }
            },
            // For skipping animations
            assertOneRecomposition = false
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal open class SupportingPaneScaffoldTestCase(
    animated: Boolean = false
) : ThreePaneScaffoldTestCase(animated) {
    override var currentDestination by mutableStateOf(
        ThreePaneScaffoldDestinationItem(SupportingPaneScaffoldRole.Main, 0)
    )

    @Composable
    override fun MeasuredContent() {
        SupportingPaneScaffold(
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = currentScaffoldDirective,
                currentDestination = currentDestination
            ),
            supportingPane = { TestPane(Color.Red) },
            extraPane = { TestPane(Color.Blue) }
        ) {
            TestPane(Color.Yellow)
        }
    }
}
