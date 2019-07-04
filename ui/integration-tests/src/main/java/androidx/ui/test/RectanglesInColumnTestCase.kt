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

package androidx.ui.test

import androidx.compose.composer
import android.app.Activity
import androidx.compose.CompositionContext
import androidx.compose.FrameManager
import androidx.compose.Model
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Column

@Model
class RectanglesInColumnTestCaseColorModel(var color: Color)

/**
 * Test case that puts the given amount of rectangles into a column layout and makes changes by
 * modifying the color used in the model.
 */
class RectanglesInColumnTestCase(
    private val activity: Activity,
    private val amountOfRectangles: Int
) {

    private val model = RectanglesInColumnTestCaseColorModel(Color.Black)
    lateinit var compositionContext: CompositionContext

    fun runSetup() {
        compositionContext = ComposeMaterialIntoActivity(activity) {
            Column {
                repeat(amountOfRectangles) { i ->
                    if (i == 0) {
                        ColoredRect(color = model.color, width = 100.dp, height = 50.dp)
                    } else {
                        ColoredRect(color = Color.Green, width = 100.dp, height = 50.dp)
                    }
                }
            }
        }!!
        FrameManager.nextFrame()
    }

    fun toggleState() {
        if (model.color == Color.Purple) {
            model.color = Color.Blue
        } else {
            model.color = Color.Purple
        }
        FrameManager.nextFrame()
    }
}