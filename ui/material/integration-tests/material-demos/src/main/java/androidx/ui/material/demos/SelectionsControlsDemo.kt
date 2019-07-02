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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.foundation.selection.ToggleableState
import androidx.ui.foundation.selection.ToggleableState.Checked
import androidx.ui.foundation.selection.ToggleableState.Unchecked
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.CrossAxisAlignment
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.MainAxisSize
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.material.Checkbox
import androidx.ui.material.RadioButton
import androidx.ui.material.RadioGroup
import androidx.ui.material.Switch
import androidx.ui.material.surface.Surface
import androidx.ui.material.themeTextStyle
import androidx.ui.material.TriStateCheckbox

private val customColor = Color(0xFFFF5722.toInt())
private val customColor2 = Color(0xFFE91E63.toInt())
private val customColor3 = Color(0xFF607D8B.toInt())

@Composable
fun SelectionsControlsDemo() {

    val headerStyle = +themeTextStyle { h6 }
    val padding = EdgeInsets(10.dp)

    Surface {
        Padding(padding = padding) {
            Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                Text(text = "Checkbox", style = headerStyle)
                Padding(padding = padding) {
                    CheckboxDemo()
                }
                Text(text = "Switch", style = headerStyle)
                Padding(padding = padding) {
                    SwitchDemo()
                }
                Text(text = "RadioButton", style = headerStyle)
                Padding(padding = padding) {
                    RadioButtonDemo()
                }
                Text(text = "Radio group :: Default usage", style = headerStyle)
                Padding(padding = padding) {
                    DefaultRadioGroup()
                }
                Text(text = "Radio group :: Custom usage", style = headerStyle)
                Padding(padding = padding) {
                    CustomRadioGroup()
                }
            }
        }
    }
}

@Composable
fun DefaultRadioGroup() {
    val radioOptions = listOf("Calls", "Missed", "Friends")
    val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
    RadioGroup(
        options = radioOptions,
        selectedOption = selectedOption,
        onSelectedChange = onOptionSelected,
        radioColor = customColor2
    )
}

@Composable
fun CustomRadioGroup() {
    val radioOptions = listOf("Disagree", "Neutral", "Agree")
    val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
    val textStyle = +themeTextStyle { subtitle1 }

    RadioGroup {
        Row(mainAxisSize = MainAxisSize.Min) {
            radioOptions.forEach { text ->
                val selected = text == selectedOption
                RadioGroupItem(
                    selected = selected,
                    onSelect = { onOptionSelected(text) }) {
                    Padding(padding = 10.dp) {
                        Column {
                            RadioButton(
                                selected = selected,
                                onSelect = { onOptionSelected(text) })
                            Text(text = text, style = textStyle)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckboxDemo() {
    Column(crossAxisAlignment = CrossAxisAlignment.Start) {
        val (state, onStateChange) = +state { true }
        val (state2, onStateChange2) = +state { true }
        val (state3, onStateChange3) = +state { true }
        val parentState = +memo(state, state2, state3) {
            if (state && state2 && state3) ToggleableState.Checked
            else if (!state && !state2 && !state3) ToggleableState.Unchecked
            else ToggleableState.Indeterminate
        }
        val onParentClick = {
            val s = parentState != Checked
            onStateChange(s)
            onStateChange2(s)
            onStateChange3(s)
        }
        Row {
            TriStateCheckbox(value = parentState, onClick = onParentClick)
            Text(text = "This is parent TriStateCheckbox", style = +themeTextStyle { body1 })
        }
        Padding(left = 10.dp) {
            Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                Checkbox(state, onStateChange, customColor)
                Checkbox(state2, onStateChange2, customColor2)
                Checkbox(state3, onStateChange3, customColor3)
            }
        }
    }
}

@Composable
fun SwitchDemo() {
    Row(
        mainAxisAlignment = MainAxisAlignment.SpaceAround,
        mainAxisSize = MainAxisSize.Min
    ) {
        val (checked, onChecked) = +state { false }
        val (checked2, onChecked2) = +state { false }
        val (checked3, onChecked3) = +state { true }
        val (checked4, onChecked4) = +state { true }
        Switch(checked = checked, onCheckedChange = onChecked)
        Switch(checked = checked2, onCheckedChange = onChecked2, color = customColor)
        Switch(checked = checked3, onCheckedChange = onChecked3, color = customColor2)
        Switch(checked = checked4, onCheckedChange = onChecked4, color = customColor3)
    }
}

@Composable
fun RadioButtonDemo() {
    Row(
        mainAxisAlignment = MainAxisAlignment.SpaceAround,
        mainAxisSize = MainAxisSize.Min
    ) {
        RadioButton(selected = true, onSelect = null)
        RadioButton(selected = false, onSelect = null)
        RadioButton(selected = true, color = customColor, onSelect = null)
        RadioButton(selected = false, color = customColor, onSelect = null)
    }
}