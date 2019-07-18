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

package androidx.ui.layout.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.composer
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.layout.Table
import androidx.ui.layout.TableColumnWidth
import androidx.ui.layout.samples.SizedRectangle

class TableActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Table(
                columnWidth = { columnIndex ->
                    when (columnIndex) {
                        0 -> TableColumnWidth.Wrap
                        1 -> TableColumnWidth.Flex(flex = 1f)
                        2 -> TableColumnWidth.Flex(flex = 2f)
                        3 -> TableColumnWidth.Fixed(width = 50.dp)
                        4 -> TableColumnWidth.Fixed(width = 50.dp)
                        else -> TableColumnWidth.Fraction(fraction = 0.25f)
                    }
                }
            ) {
                tableRow {
                    SizedRectangle(Color.Red, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Green, height = 25.dp)
                    SizedRectangle(Color.Blue, height = 25.dp)
                    SizedRectangle(Color.Cyan, height = 25.dp)
                    SizedRectangle(Color.Magenta, height = 25.dp)
                    SizedRectangle(Color.Yellow, height = 25.dp)
                }
                tableRow {
                    SizedRectangle(Color.Green, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Blue, height = 25.dp)
                    SizedRectangle(Color.Cyan, height = 25.dp)
                    SizedRectangle(Color.Magenta, height = 25.dp)
                    SizedRectangle(Color.Yellow, height = 25.dp)
                    SizedRectangle(Color.Red, height = 25.dp)
                }
                tableRow {
                    SizedRectangle(Color.Blue, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Cyan, height = 25.dp)
                    SizedRectangle(Color.Magenta, height = 25.dp)
                    SizedRectangle(Color.Yellow, height = 25.dp)
                    SizedRectangle(Color.Red, height = 25.dp)
                    SizedRectangle(Color.Green, height = 25.dp)
                }
                tableRow {
                    SizedRectangle(Color.Cyan, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Magenta, height = 25.dp)
                    SizedRectangle(Color.Yellow, height = 25.dp)
                    SizedRectangle(Color.Red, height = 25.dp)
                    SizedRectangle(Color.Green, height = 25.dp)
                    SizedRectangle(Color.Blue, height = 25.dp)
                }
                tableRow {
                    SizedRectangle(Color.Magenta, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Yellow, height = 25.dp)
                    SizedRectangle(Color.Red, height = 25.dp)
                    SizedRectangle(Color.Green, height = 25.dp)
                    SizedRectangle(Color.Blue, height = 25.dp)
                    SizedRectangle(Color.Cyan, height = 25.dp)
                }
                tableRow {
                    SizedRectangle(Color.Yellow, height = 25.dp, width = 25.dp)
                    SizedRectangle(Color.Red, height = 25.dp)
                    SizedRectangle(Color.Green, height = 25.dp)
                    SizedRectangle(Color.Blue, height = 25.dp)
                    SizedRectangle(Color.Cyan, height = 25.dp)
                    SizedRectangle(Color.Magenta, height = 25.dp)
                }
            }
        }
    }
}