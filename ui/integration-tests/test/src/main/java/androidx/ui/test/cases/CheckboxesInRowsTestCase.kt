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
import androidx.compose.composer
import androidx.compose.Composable
import androidx.compose.FrameManager
import androidx.compose.State
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.FlexRow
import androidx.ui.material.Checkbox
import androidx.ui.test.ComposeMaterialIntoActivity
import androidx.ui.test.ComposeTestCase

/**
 * Test case that puts the given amount of checkboxes into a column of rows and makes changes by
 * toggling the first checkbox.
 */
class CheckboxesInRowsTestCase(
    activity: Activity,
    private val amountOfCheckboxes: Int
) : ComposeTestCase(activity) {

    private val states = mutableListOf<State<Boolean>>()

    override fun runSetup() {
        compositionContext = ComposeMaterialIntoActivity(activity) {
            Column {
                repeat(amountOfCheckboxes) {
                    FlexRow {
                        inflexible {
                            Text(text = "Check Me!")
                        }
                        expanded(1f) {
                            Align(alignment = Alignment.CenterRight) {
                                CheckboxWithState()
                            }
                        }
                    }
                }
            }
        }!!
        FrameManager.nextFrame()

        view = activity.findViewById(android.R.id.content)

        measure()
        layout()
        drawSlow()
    }

    fun toggleState() {
        val state = states.first()
        state.value = !state.value
        FrameManager.nextFrame()
    }

    @Composable
    fun CheckboxWithState() {
        val state = +state { false }
        states.add(state)
        Checkbox(
            checked = state.value,
            onCheckedChange = { state.value = !state.value }
        )
    }
}
