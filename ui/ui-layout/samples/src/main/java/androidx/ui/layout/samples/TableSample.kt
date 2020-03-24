/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.foundation.Box
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Table
import androidx.ui.layout.TableColumnWidth
import androidx.ui.unit.dp

@Sampled
@Composable
fun SimpleTable() {
    Table(columns = 8) {
        for (i in 0 until 8) {
            tableRow {
                for (j in 0 until 8) {
                    Box(
                        LayoutPadding(2.dp) + LayoutAspectRatio(1f),
                        backgroundColor = Color.Magenta
                    )
                }
            }
        }
    }
}

@Sampled
@Composable
fun TableWithDecorations() {
    Table(columns = 8) {
        tableDecoration(overlay = false) {
            Box(backgroundColor = Color.Green)
        }
        tableDecoration(overlay = false) {
            Box(shape = CircleShape, backgroundColor = Color.Red)
        }
        for (i in 0 until 8) {
            tableRow {
                for (j in 0 until 8) {
                    Box(
                        LayoutPadding(2.dp) + LayoutAspectRatio(1f),
                        backgroundColor = Color.Magenta
                    )
                }
            }
        }
    }
}

@Sampled
@Composable
fun TableWithDifferentColumnWidths() {
    val padding = LayoutPadding(2.dp)
    Table(
        columns = 5,
        columnWidth = { columnIndex ->
            when (columnIndex) {
                0 -> TableColumnWidth.Wrap
                1 -> TableColumnWidth.Flex(flex = 1f)
                2 -> TableColumnWidth.Flex(flex = 3f)
                3 -> TableColumnWidth.Fixed(width = 50.dp)
                else -> TableColumnWidth.Fraction(fraction = 0.5f)
            }
        }
    ) {
        for (i in 0 until 8) {
            tableRow {
                Box(padding + LayoutSize(25.dp, 25.dp), backgroundColor = Color.Magenta)
                for (j in 1 until 5) {
                    Box(padding + LayoutHeight(25.dp), backgroundColor = Color.Magenta)
                }
            }
        }
    }
}