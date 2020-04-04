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

package androidx.ui.integration.test.material

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.wrapContentSize
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Surface
import androidx.ui.test.ComposeTestCase

/**
 * Test case that puts the given amount of checkboxes into a column of rows and makes changes by
 * toggling the first checkbox.
 */
class CheckboxesInRowsTestCase(
    private val amountOfCheckboxes: Int
) : ComposeTestCase, ToggleableTestCase {

    private val states = mutableListOf<MutableState<Boolean>>()

    @Composable
    override fun emitContent() {
        MaterialTheme {
            Surface {
                Column {
                    repeat(amountOfCheckboxes) {
                        Row {
                            Text(text = "Check Me!")
                            CheckboxWithState(
                                Modifier.weight(1f).wrapContentSize(Alignment.CenterEnd)
                            )
                        }
                    }
                }
            }
        }
    }

    override fun toggleState() {
        val state = states.first()
        state.value = !state.value
    }

    @Composable
    fun CheckboxWithState(modifier: Modifier = Modifier) {
        val state = state { false }
        states.add(state)
        Checkbox(
            checked = state.value,
            onCheckedChange = { state.value = !state.value },
            modifier = modifier
        )
    }
}
