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
import androidx.ui.foundation.Border
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.drawBorders
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutAspectRatio
import androidx.ui.layout.Padding
import androidx.ui.layout.Table
import androidx.ui.unit.dp

@Sampled
@Composable
fun TableWithBorders() {
    Padding(10.dp) {
        Table(columns = 8) {
            drawBorders(
                defaultBorder = Border(color = Color.Red, size = 2.dp)
            ) {
                outer()
                vertical(column = 2, rows = 0 until 8)
                vertical(column = 4, rows = 0 until 8)
                vertical(column = 6, rows = 0 until 8)
                horizontal(row = 2, columns = 0 until 8)
                horizontal(row = 4, columns = 0 until 8)
                horizontal(row = 6, columns = 0 until 8)
            }
            repeat(8) {
                tableRow {
                    repeat(8) {
                        Padding(2.dp) {
                            ColoredRect(color = Color.Magenta, modifier = LayoutAspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}
