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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.CombinedModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import kotlin.random.Random

@Composable
fun HierarchicalFocusDemo() {
    val numRows = 3
    val numColumns = 4

    val style = TextStyle(color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)

    var selectedRow by remember { mutableIntStateOf(0) }
    val selectedColumn = remember { Array(numRows) { mutableIntStateOf(0) } }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                    .hierarchicalFocusGroup(active = selectedRow == rowIx),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { selectedRow = rowIx }) { Text("Sel") }
                if (rowIx == numRows - 1) {
                    Text(
                        "... No focus here ...",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    repeat(numColumns + 1) { colIx ->
                        Box(
                            Modifier.thenIf(
                                    colIx < numColumns,
                                    // Last column wants nothing to do with focus
                                    Modifier.hierarchicalFocusGroup(
                                        active = selectedColumn[rowIx].intValue == colIx
                                    ),
                                )
                                .weight(1f)
                                .clickable { selectedColumn[rowIx].intValue = colIx }
                                .thenIf(
                                    selectedColumn[rowIx].intValue == colIx,
                                    Modifier.border(BorderStroke(2.dp, Color.Red)),
                                )
                        ) {
                            if (colIx < numColumns) {
                                var focused by remember { mutableStateOf(false) }
                                BasicText(
                                    "$colIx",
                                    style = style,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .requestFocusOnHierarchyActive()
                                            .onFocusChanged { focused = it.isFocused }
                                            .focusable()
                                            .thenIf(focused, Modifier.background(Color.Gray)),
                                )
                            } else {
                                BasicText("No", style = style, modifier = Modifier)
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = {
                val r = Random
                selectedRow = r.nextInt(numRows)
                repeat(numRows) { selectedColumn[it].intValue = r.nextInt(numColumns + 1) }
            },
            Modifier.size(40.dp),
        ) {
            Text("Shuffle", style = style)
        }
    }
}

private fun Modifier.thenIf(condition: Boolean, other: Modifier) =
    if (condition) CombinedModifier(this, other) else this
