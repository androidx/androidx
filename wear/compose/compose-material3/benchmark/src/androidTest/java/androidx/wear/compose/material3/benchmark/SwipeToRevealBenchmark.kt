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

package androidx.wear.compose.material3.benchmark

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkDrawPerf
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkLayoutPerf
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealState
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberRevealState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SwipeToRevealBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val testCaseFactory = { SwipeToRevealTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(testCaseFactory)
    }

    @Test
    fun update_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(testCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun update_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(testCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun update_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(testCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun update_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(testCaseFactory, assertOneRecomposition = false)
    }

    @Test
    fun layout() {
        benchmarkRule.benchmarkLayoutPerf(testCaseFactory)
    }

    @Test
    fun draw() {
        benchmarkRule.benchmarkDrawPerf(testCaseFactory)
    }
}

internal class SwipeToRevealTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var revealState: RevealState
    private lateinit var coroutineScope: CoroutineScope

    @Composable
    override fun MeasuredContent() {
        coroutineScope = rememberCoroutineScope()
        revealState = rememberRevealState()

        SwipeToReveal(
            primaryAction = {
                PrimaryActionButton(
                    onClick = {},
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                    text = { Text("Delete") },
                )
            },
            onSwipePrimaryAction = {},
            secondaryAction = {
                SecondaryActionButton(
                    onClick = {},
                    icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "Options") },
                )
            },
            undoPrimaryAction = { UndoActionButton(onClick = {}, text = { Text("Undo Delete") }) },
            undoSecondaryAction = {
                UndoActionButton(onClick = {}, text = { Text("Undo Delete") })
            },
            revealState = revealState,
            revealDirection = Bidirectional,
            hasPartiallyRevealedState = false,
        ) {
            Button(
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Delete") { true },
                                CustomAccessibilityAction("Options") { true },
                            )
                    },
                onClick = {},
            ) {
                Text("This Button has two actions", modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    override fun ContentWrappers(content: @Composable (() -> Unit)) {
        MaterialTheme { content() }
    }

    override fun toggleState() {
        coroutineScope.launch {
            if (revealState.currentValue == RevealValue.Covered) {
                revealState.snapTo(RevealValue.RightRevealed)
            } else {
                revealState.snapTo(RevealValue.Covered)
            }
        }
    }
}
