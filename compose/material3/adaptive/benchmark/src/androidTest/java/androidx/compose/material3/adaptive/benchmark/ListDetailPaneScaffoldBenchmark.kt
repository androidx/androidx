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
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
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
class ListDetailPaneScaffoldBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun singlePane_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel {
            ListDetailPaneScaffoldTestCase().apply {
                currentScaffoldDirective = singlePaneDirective
            }
        }
    }

    @Test
    fun dualPane_firstPixel() {
        benchmarkRule.benchmarkToFirstPixel {
            ListDetailPaneScaffoldTestCase().apply { currentScaffoldDirective = dualPaneDirective }
        }
    }

    @Test
    fun singlePane_firstCompose() {
        benchmarkRule.benchmarkFirstCompose {
            ListDetailPaneScaffoldTestCase().apply {
                currentScaffoldDirective = singlePaneDirective
            }
        }
    }

    @Test
    fun dualPane_firstCompose() {
        benchmarkRule.benchmarkFirstCompose {
            ListDetailPaneScaffoldTestCase().apply { currentScaffoldDirective = dualPaneDirective }
        }
    }

    @Test
    fun singlePane_navigateToDetail() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : ListDetailPaneScaffoldTestCase() {
                        override fun toggleState() {
                            val newPane =
                                if (currentDestination.pane == ListDetailPaneScaffoldRole.List) {
                                    ListDetailPaneScaffoldRole.Detail
                                } else {
                                    ListDetailPaneScaffoldRole.List
                                }
                            currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                        }
                    }
                    .apply { currentScaffoldDirective = singlePaneDirective }
            },
            // For skipping state transitions
            assertOneRecomposition = false
        )
    }

    @Test
    fun dualPane_navigateToExtra() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : ListDetailPaneScaffoldTestCase() {
                        override fun toggleState() {
                            val newPane =
                                if (currentDestination.pane == ListDetailPaneScaffoldRole.List) {
                                    ListDetailPaneScaffoldRole.Extra
                                } else {
                                    ListDetailPaneScaffoldRole.List
                                }
                            currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                        }
                    }
                    .apply { currentScaffoldDirective = dualPaneDirective }
            },
            // For skipping state transitions
            assertOneRecomposition = false
        )
    }

    @Test
    fun singlePane_navigateToDetail_animated() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : ListDetailPaneScaffoldTestCase(animated = true) {
                        override fun toggleState() {
                            val newPane =
                                if (currentDestination.pane == ListDetailPaneScaffoldRole.List) {
                                    ListDetailPaneScaffoldRole.Detail
                                } else {
                                    ListDetailPaneScaffoldRole.List
                                }
                            currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                        }
                    }
                    .apply { currentScaffoldDirective = singlePaneDirective }
            },
            // For skipping animations
            assertOneRecomposition = false
        )
    }

    @Test
    fun dualPane_navigateToExtra_animated() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : ListDetailPaneScaffoldTestCase(animated = true) {
                        override fun toggleState() {
                            val newPane =
                                if (currentDestination.pane == ListDetailPaneScaffoldRole.List) {
                                    ListDetailPaneScaffoldRole.Extra
                                } else {
                                    ListDetailPaneScaffoldRole.List
                                }
                            currentDestination = ThreePaneScaffoldDestinationItem(newPane, 0)
                        }
                    }
                    .apply { currentScaffoldDirective = dualPaneDirective }
            },
            // For skipping animations
            assertOneRecomposition = false
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal open class ListDetailPaneScaffoldTestCase(animated: Boolean = false) :
    ThreePaneScaffoldTestCase(animated) {
    override var currentDestination by
        mutableStateOf(ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, 0))

    @Composable
    override fun MeasuredContent() {
        ListDetailPaneScaffold(
            directive = currentScaffoldDirective,
            value =
                calculateThreePaneScaffoldValue(
                    maxHorizontalPartitions = currentScaffoldDirective.maxHorizontalPartitions,
                    adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
                    currentDestination = currentDestination
                ),
            listPane = { TestPane(Color.Red) },
            detailPane = { TestPane(Color.Yellow) },
            extraPane = { TestPane(Color.Blue) }
        )
    }
}
