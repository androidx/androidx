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

package androidx.constraintlayout.compose.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Skip
import androidx.constraintlayout.compose.Span
import androidx.constraintlayout.compose.atMost

@Sampled
@Composable
fun Row_sample() {
    ConstraintLayout(
        constraintSet =
            ConstraintSet {
                val (a, b, c, d, e) = createRefsFor(0, 1, 2, 3, 4)
                val row =
                    createRow(
                        a,
                        b,
                        c,
                        d,
                        e,
                        spacing = 10.dp,
                        weights = floatArrayOf(3f, 3f, 2f, 2f, 1f),
                    )
                constrain(row) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }

                constrain(a, b, c, d, e) { width = Dimension.fillToConstraints }
            },
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(5) {
            Text(text = "item$it", modifier = Modifier.layoutId(it).background(Color.LightGray))
        }
    }
}

@Sampled
@Composable
fun Column_sample() {
    ConstraintLayout(
        constraintSet =
            ConstraintSet {
                val (a, b, c, d, e) = createRefsFor(0, 1, 2, 3, 4)
                val column =
                    createColumn(
                        a,
                        b,
                        c,
                        d,
                        e,
                        spacing = 10.dp,
                        weights = floatArrayOf(3f, 3f, 2f, 2f, 1f),
                    )
                constrain(column) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }

                constrain(a, b, c, d, e) { height = Dimension.fillToConstraints }
            },
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(5) {
            Text(text = "item$it", modifier = Modifier.layoutId(it).background(Color.LightGray))
        }
    }
}

@Sampled
@Composable
fun Grid_calculator_sample() {
    // For most of the keys we can just use the displayed text as the ID.
    val ids =
        arrayOf(
            // Text box will span all 4 columns and the first 2 of rows
            "textBox",
            "C",
            "+/-",
            "%",
            "/",
            "7",
            "8",
            "9",
            "*",
            "4",
            "5",
            "6",
            "-",
            "1",
            "2",
            "3",
            "+",
            // The '0' will span two columns, note that it's on the 24th position in the grid
            "0",
            ".",
            "="
        )
    ConstraintLayout(
        constraintSet =
            ConstraintSet {
                val idRefs = Array(ids.size) { createRefFor(ids[it]) }

                val g1 =
                    createGrid(
                        elements = idRefs,
                        rows = 7,
                        columns = 4,
                        verticalSpacing = 10.dp,
                        horizontalSpacing = 10.dp,
                        spans =
                            arrayOf(
                                // textBox
                                Span(position = 0, rows = 2, columns = 4),
                                // '0' key
                                Span(position = 24, rows = 1, columns = 2)
                            )
                    )

                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }

                constrain(*idRefs) {
                    // Make all the layouts fill up their space, you may still use coercing methods
                    // such as `atMost(Dp)` or `atMostWrapContent()` to further limit their size.
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
            },
        modifier = Modifier.fillMaxSize()
    ) {
        ids.forEach { id ->
            when (id) {
                "textBox" -> {
                    Box(
                        modifier =
                            Modifier.background(Color.Gray)
                                // As usual, IDs should only be assigned on top-level children
                                .layoutId(id),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(text = "100", fontSize = 80.sp)
                    }
                }
                else -> {
                    Button(onClick = {}, Modifier.layoutId(id)) {
                        Text(text = id, fontSize = 30.sp)
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun Grid_navigationPad_sample() {
    val keys =
        arrayOf("Insert", "Home", "Page Up", "Delete", "End", "Page Down", "↑", "←", "↓", "→")
    ConstraintLayout(
        constraintSet =
            ConstraintSet {
                val keyRefs = Array(keys.size) { createRefFor(keys[it]) }

                val g1 =
                    createGrid(
                        elements = keyRefs,
                        rows = 5,
                        columns = 3,
                        verticalSpacing = 8.dp,
                        horizontalSpacing = 8.dp,
                        skips =
                            arrayOf(
                                // These positions follow the expected Grid cells indexing
                                // Arranged horizontally by default:
                                //   - 0 is top-left
                                //   - 14 is bottom-right (5 rows x 3 columns - 1)
                                Skip(position = 6, rows = 1, columns = 3),
                                Skip(position = 9, rows = 1, columns = 1),
                                Skip(position = 11, rows = 1, columns = 1)
                            )
                    )
                constrain(g1) {
                    width = Dimension.matchParent
                    height = Dimension.matchParent
                }

                constrain(*keyRefs) {
                    width = Dimension.fillToConstraints.atMost(100.dp)
                    height = Dimension.fillToConstraints.atMost(100.dp)
                }
            },
        modifier = Modifier.fillMaxSize()
    ) {
        keys.forEachIndexed { index, key ->
            Box(
                modifier = Modifier.layoutId(key).background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    textAlign = TextAlign.Center,
                    // Make fontSize bigger for the arrow keys
                    fontSize = if (index >= 6) 24.sp else TextUnit.Unspecified
                )
            }
        }
    }
}
