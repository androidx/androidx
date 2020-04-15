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
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.unit.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.test.ComposeTestCase
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.layout.preferredSize

/**
 * Test case that puts the given amount of rectangles into a column layout and makes changes by
 * modifying the color used in the model.
 *
 * Note: Each rectangle has its own model so changes should always affect only the first one.
 */
class RectsInColumnTestCase(
    private val amountOfRectangles: Int
) : ComposeTestCase, ToggleableTestCase {

    private val states = mutableListOf<MutableState<Color>>()

    @Composable
    override fun emitContent() {
        MaterialTheme {
            Surface {
                Column {
                    repeat(amountOfRectangles) {
                        ColoredRectWithModel()
                    }
                }
            }
        }
    }

    override fun toggleState() {
        val state = states.first()
        if (state.value == Color.Magenta) {
            state.value = Color.Blue
        } else {
            state.value = Color.Magenta
        }
    }

    @Composable
    fun ColoredRectWithModel() {
        val state = state { Color.Black }
        states.add(state)
        Box(Modifier.preferredSize(100.dp, 50.dp).drawBackground(state.value))
    }
}