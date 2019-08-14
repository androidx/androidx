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
import androidx.compose.composer
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Padding
import androidx.ui.layout.Table
import androidx.ui.layout.TableColumnWidth

@Sampled
@Composable
fun SimpleTable() {
    Padding(2.dp) {
        Table(columnCount = 8) {
            for (i in 0 until 8) {
                tableRow {
                    Padding(2.dp) {
                        SizedRectangle(color = Color.Magenta, height = 50.dp)
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun TableWithDifferentColumnWidths() {
    Padding(2.dp) {
        Table(
            columnCount = 5,
            columnWidth = { columnIndex ->
                when (columnIndex) {
                    0 -> TableColumnWidth.Inflexible.Wrap
                    1 -> TableColumnWidth.Flexible(flex = 1f)
                    2 -> TableColumnWidth.Flexible(flex = 3f)
                    3 -> TableColumnWidth.Inflexible.Fixed(width = 50.dp)
                    else -> TableColumnWidth.Inflexible.Fraction(fraction = 0.5f)
                }
            }
        ) {
            for (i in 0 until 8) {
                tableRow { j ->
                    Padding(2.dp) {
                        if (j == 0) {
                            SizedRectangle(color = Color.Magenta, width = 25.dp, height = 25.dp)
                        } else {
                            SizedRectangle(color = Color.Magenta, height = 25.dp)
                        }
                    }
                }
            }
        }
    }
}