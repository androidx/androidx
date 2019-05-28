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

import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.baseui.selection.ToggleableState.Checked
import androidx.ui.baseui.selection.ToggleableState.Unchecked
import androidx.ui.core.Text
import androidx.ui.core.dp
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
import androidx.ui.material.parentCheckboxState
import androidx.ui.material.surface.Surface
import androidx.ui.material.themeTextStyle
import androidx.ui.graphics.Color
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus

@Model
class CheckboxState(var value: ToggleableState) {
    fun toggle() {
        value = if (value == Checked) Unchecked else Checked
    }
}

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
        onOptionSelected = onOptionSelected,
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
                    onSelected = { onOptionSelected(text) }) {
                    Padding(padding = 10.dp) {
                        Column {
                            RadioButton(selected = selected)
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
        val state = +memo { CheckboxState(Checked) }
        val state2 = +memo { CheckboxState(Checked) }
        val state3 = +memo { CheckboxState(Checked) }
        fun calcParentState() = parentCheckboxState(state.value, state2.value, state3.value)
        val onParentClick = {
            val s = if (calcParentState() == Checked) {
                Unchecked
            } else {
                Checked
            }
            state.value = s
            state2.value = s
            state3.value = s
        }
        Row {
            Checkbox(value = calcParentState(), onClick = onParentClick)
            Text(text = "This is parent", style = +themeTextStyle { body1 })
        }
        Padding(left = 10.dp) {
            Column(crossAxisAlignment = CrossAxisAlignment.Start) {
                Checkbox(
                    value = state.value,
                    color = customColor,
                    onClick = { state.toggle() })
                Checkbox(
                    value = state2.value,
                    onClick = { state2.toggle() },
                    color = customColor2
                )
                Checkbox(
                    value = state3.value,
                    onClick = { state3.toggle() },
                    color = customColor3
                )
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
        Switch(checked = checked, onClick = { onChecked(!checked) })
        Switch(checked = checked2, onClick = { onChecked2(!checked2) }, color = customColor)
        Switch(checked = checked3, onClick = { onChecked3(!checked3) }, color = customColor2)
        Switch(checked = checked4, onClick = { onChecked4(!checked4) }, color = customColor3)
    }
}

@Composable
fun RadioButtonDemo() {
    Row(
        mainAxisAlignment = MainAxisAlignment.SpaceAround,
        mainAxisSize = MainAxisSize.Min
    ) {
        RadioButton(selected = true)
        RadioButton(selected = false)
        RadioButton(selected = true, color = customColor)
        RadioButton(selected = false, color = customColor)
    }
}