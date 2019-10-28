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

package androidx.ui.test.cases

import android.app.Activity
import androidx.compose.FrameManager
import androidx.ui.foundation.ColoredRect
import androidx.ui.core.dp
import androidx.ui.core.setContent
import androidx.ui.graphics.Color
import androidx.ui.layout.Padding
import androidx.ui.layout.Table
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.ToggleableTestCase

/**
 * Test case that puts the given number of rectangles into a table layout and makes no changes.
 */
class TableRecompositionTestCase(
    activity: Activity,
    private val numberOfCells: Int
) : ComposeTestCase(activity), ToggleableTestCase {

    override fun setComposeContent(activity: Activity) = activity.setContent {
        MaterialTheme {
            Surface {
                Table(columns = numberOfCells) {
                    tableDecoration(overlay = false) { }
                    repeat(numberOfCells) {
                        tableRow {
                            Padding(2.dp) {
                                ColoredRect(color = Color.Black, width = 100.dp, height = 50.dp)
                            }
                        }
                    }
                }
            }
        }
    }!!

    override fun toggleState() {
        FrameManager.nextFrame()
    }
}
