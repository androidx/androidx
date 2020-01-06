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

package androidx.ui.integration.test.foundation

import androidx.compose.Composable
import androidx.compose.Model
import androidx.ui.core.dp
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.test.ComposeTestCase
import androidx.ui.integration.test.ToggleableTestCase

@Model
private class RectanglesInColumnTestCaseColorModel(var color: Color)

/**
 * Test case that puts the given amount of rectangles into a column layout and makes changes by
 * modifying the color used in the model.
 *
 * Note: Rectangle are created in for loop that reference a single model. Currently it will happen
 * that the whole loop has to be re-run when model changes.
 */
class RectsInColumnSharedModelTestCase(
    private val amountOfRectangles: Int
) : ComposeTestCase, ToggleableTestCase {

    private val model = RectanglesInColumnTestCaseColorModel(Color.Black)

    @Composable
    override fun emitContent() {
        MaterialTheme {
            Column {
                repeat(amountOfRectangles) { i ->
                    if (i == 0) {
                        ColoredRect(color = model.color, width = 100.dp, height = 50.dp)
                    } else {
                        ColoredRect(color = Color.Green, width = 100.dp, height = 50.dp)
                    }
                }
            }
        }
    }

    override fun toggleState() {
        if (model.color == Color.Magenta) {
            model.color = Color.Blue
        } else {
            model.color = Color.Magenta
        }
    }
}