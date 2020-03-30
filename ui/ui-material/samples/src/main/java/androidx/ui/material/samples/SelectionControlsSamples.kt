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

package androidx.ui.material.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.padding
import androidx.ui.material.Checkbox
import androidx.ui.material.RadioButton
import androidx.ui.material.RadioGroup
import androidx.ui.material.Switch
import androidx.ui.material.TriStateCheckbox
import androidx.ui.unit.dp

@Sampled
@Composable
fun TriStateCheckboxSample() {
    Column {
        // define dependent checkboxes states
        val (state, onStateChange) = state { true }
        val (state2, onStateChange2) = state { true }

        // TriStateCheckbox state reflects state of dependent checkboxes
        val parentState = remember(state, state2) {
            if (state && state2) ToggleableState.On
            else if (!state && !state2) ToggleableState.Off
            else ToggleableState.Indeterminate
        }
        // click on TriStateCheckbox can set state for dependent checkboxes
        val onParentClick = {
            val s = parentState != ToggleableState.On
            onStateChange(s)
            onStateChange2(s)
        }

        TriStateCheckbox(state = parentState, onClick = onParentClick, color = Color.Black)
        Column(Modifier.padding(10.dp, 0.dp, 0.dp, 0.dp)) {
            Checkbox(state, onStateChange)
            Checkbox(state2, onStateChange2)
        }
    }
}

@Sampled
@Composable
fun CheckboxSample() {
    val checkedState = state { true }
    Checkbox(
        checked = checkedState.value,
        onCheckedChange = { checkedState.value = it }
    )
}

@Sampled
@Composable
fun SwitchSample() {
    val checkedState = state { true }
    Switch(
        checked = checkedState.value,
        onCheckedChange = { checkedState.value = it }
    )
}

@Sampled
@Composable
fun RadioButtonSample() {
    // we have two radio buttons and only one can be selected
    // let's emulate binary choice here
    var enterTheMatrix by state { true }
    Row {
        RadioButton(
            selected = enterTheMatrix,
            onSelect = { enterTheMatrix = true },
            color = Color.Red
        )
        RadioButton(
            selected = !enterTheMatrix,
            onSelect = { enterTheMatrix = false },
            color = Color.Blue
        )
    }
}

@Sampled
@Composable
fun DefaultRadioGroupSample() {
    val radioOptions = listOf("Calls", "Missed", "Friends")
    val (selectedOption, onOptionSelected) = state { radioOptions[0] }
    RadioGroup(
        options = radioOptions,
        selectedOption = selectedOption,
        onSelectedChange = onOptionSelected
    )
}

@Sampled
@Composable
fun CustomRadioGroupSample() {
    val radioOptions = listOf("Disagree", "Neutral", "Agree")
    val (selectedOption, onOptionSelected) = state { radioOptions[0] }

    RadioGroup {
        Row {
            radioOptions.forEach { text ->
                val selected = text == selectedOption
                RadioGroupItem(
                    selected = selected,
                    onSelect = { onOptionSelected(text) }) {
                    // TODO: remove Box when Ripple becomes a modifier
                    Box {
                        Column(modifier = Modifier.padding(10.dp)) {
                            RadioButton(
                                selected = selected,
                                onSelect = { onOptionSelected(text) })
                            Text(text = text)
                        }
                    }
                }
            }
        }
    }
}