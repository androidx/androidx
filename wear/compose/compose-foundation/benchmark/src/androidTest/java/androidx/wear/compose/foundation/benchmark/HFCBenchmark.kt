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

package androidx.wear.compose.foundation.benchmark

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.ui.Alignment
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Benchmark for Wear Compose HierarchicalFocusCoordinator. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HfcBenchmark {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    private val caseFactoryFactory = { useHfc: Boolean -> { caseFactory(useHfc) } }

    @Test fun first_compose_hfc() = benchmarkRule.benchmarkFirstCompose(caseFactoryFactory(true))

    @Test fun first_compose_base() = benchmarkRule.benchmarkFirstCompose(caseFactoryFactory(false))
}

internal class caseFactory(val useHfc: Boolean) : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        val numRows = 3
        val numColumns = 5

        var selectedRow by remember { mutableStateOf(0) }
        var selectedColumn = remember { Array(numRows) { mutableStateOf(0) } }

        val focusRequesters = remember { Array(numRows * numColumns) { FocusRequester() } }

        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            repeat(numRows) { rowIx ->
                Row(
                    Modifier.fillMaxWidth()
                        .then(
                            if (rowIx == selectedRow) {
                                Modifier.border(BorderStroke(2.dp, Color.Green))
                            } else {
                                Modifier
                            }
                        )
                        .thenIf(
                            useHfc,
                            Modifier.hierarchicalFocusGroup(active = selectedRow == rowIx),
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        "Sel",
                        Modifier.background(Color.Gray, CircleShape).clickable {
                            selectedRow = rowIx
                        },
                    )
                    repeat(numColumns) { colIx ->
                        var focused by remember { mutableStateOf(false) }
                        val focusRequester = focusRequesters[colIx + numColumns * rowIx]
                        BasicText(
                            "$colIx",
                            style =
                                TextStyle(
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center,
                                ),
                            modifier =
                                Modifier.thenIf(
                                        useHfc,
                                        Modifier.hierarchicalFocusGroup(
                                                active = selectedColumn[rowIx].value == colIx
                                            )
                                            .requestFocusOnHierarchyActive(),
                                    )
                                    .weight(1f)
                                    .clickable { selectedColumn[rowIx].value = colIx }
                                    .onFocusChanged { focused = it.isFocused }
                                    .focusRequester(focusRequester)
                                    .focusable()
                                    .thenIf(
                                        selectedColumn[rowIx].value == colIx,
                                        Modifier.border(BorderStroke(2.dp, Color.Red)),
                                    )
                                    .thenIf(focused, Modifier.background(Color.Gray)),
                        )
                    }
                }
            }
        }
        if (!useHfc) {
            LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }
        }
    }
}

private fun Modifier.thenIf(condition: Boolean, other: Modifier) =
    if (condition) CombinedModifier(this, other) else this
