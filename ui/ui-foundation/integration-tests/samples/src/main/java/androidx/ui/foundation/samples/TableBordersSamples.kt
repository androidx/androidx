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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.drawBorders
import androidx.ui.foundation.shape.border.Border
import androidx.ui.graphics.Color
import androidx.ui.layout.Padding
import androidx.ui.layout.Table

@Sampled
@Composable
fun SimpleTableWithBorders() {
    Padding(10.dp) {
        Table(columnCount = 8) {
            drawBorders(
                defaultBorder = Border(color = Color.Red, width = 2.dp)
            ) {
                outer()
                horizontal(row = 1)
                vertical(column = 1, rows = 1 until rowCount)
                vertical(column = columnCount - 1, rows = 1 until rowCount)
            }
            repeat(8) {
                tableRow {
                    Padding(2.dp) {
                        ColoredRect(color = Color.Magenta, height = 50.dp)
                    }
                }
            }
        }
    }
}
