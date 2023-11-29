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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.AnimatedPane
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.SupportingPaneScaffold
import androidx.compose.material3.adaptive.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.calculateSupportingPaneScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    fun singlePane_navigateToSupporting() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase() {
                    override fun toggleState() {
                        currentDestination = SupportingPaneScaffoldRole.Supporting
                    }
                }.apply {
                    currentScaffoldDirective = singlePaneDirective
                }
            },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun dualPane_navigateToExtra() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout(
            {
                object : SupportingPaneScaffoldTestCase() {
                    override fun toggleState() {
                        currentDestination = SupportingPaneScaffoldRole.Extra
                    }
                }.apply {
                    currentScaffoldDirective = dualPaneDirective
                }
            },
            assertOneRecomposition = false,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal open class SupportingPaneScaffoldTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    var currentScaffoldDirective by mutableStateOf(singlePaneDirective)
    var currentDestination by mutableStateOf(SupportingPaneScaffoldRole.Main)

    @Composable
    override fun MeasuredContent() {
        SupportingPaneScaffold(
            scaffoldState = calculateSupportingPaneScaffoldState(
                scaffoldDirective = currentScaffoldDirective,
                currentPaneDestination = currentDestination
            ),
            supportingPane = {
                AnimatedPane(
                    modifier = Modifier.testTag(tag = "SupportingPane")
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                }
            },
            extraPane = {
                AnimatedPane(
                    modifier = Modifier.testTag(tag = "ExtraPane")
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Blue))
                }
            }
        ) {
            AnimatedPane(
                modifier = Modifier.testTag(tag = "MainPane")
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Yellow))
            }
        }
    }

    override fun toggleState() {}
}
