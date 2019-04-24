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
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.Color
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.composer
import com.google.r4a.state
import com.google.r4a.unaryPlus

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

    <Padding padding>
        <Column crossAxisAlignment=CrossAxisAlignment.Start>
            <Text text="Checkbox" style=headerStyle />
            <Padding padding>
                <CheckboxDemo />
            </Padding>
            <Text text="Switch" style=headerStyle />
            <Padding padding>
                <SwitchDemo />
            </Padding>
            <Text text="RadioButton" style=headerStyle />
            <Padding padding>
                <RadioButtonDemo />
            </Padding>
            <Text text="Radio group :: Default usage" style=headerStyle />
            <Padding padding>
                <DefaultRadioGroup />
            </Padding>
            <Text text="Radio group :: Custom usage" style=headerStyle />
            <Padding padding>
                <CustomRadioGroup />
            </Padding>
        </Column>
    </Padding>
}

@Composable
fun DefaultRadioGroup() {
    val radioOptions = listOf("Calls", "Missed", "Friends")
    val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
    <RadioGroup
        options=radioOptions
        selectedOption
        onOptionSelected
        radioColor=customColor2 />
}

@Composable
fun CustomRadioGroup() {
    val radioOptions = listOf("Disagree", "Neutral", "Agree")
    val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
    val textStyle = +themeTextStyle { subtitle1 }

    <RadioGroup>
        <Row mainAxisSize=MainAxisSize.Min> radioOptions.forEach { text ->
            val selected = text == selectedOption
            <RadioGroupItem
                selected
                onSelected={ onOptionSelected(text) }>
                <Padding padding=10.dp>
                    <Column>
                        <RadioButton selected />
                        <Text text=text style=textStyle />
                    </Column>
                </Padding>
            </RadioGroupItem>
        }
        </Row>
    </RadioGroup>
}

@Composable
fun CheckboxDemo() {
    <Column crossAxisAlignment=CrossAxisAlignment.Start>
        val state = CheckboxState(Checked)
        val state2 = CheckboxState(Checked)
        val state3 = CheckboxState(Checked)
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
        <Row>
            <Checkbox value=calcParentState() onToggle=onParentClick />
            <Text text="This is parent" style=+themeTextStyle { body1 } />
        </Row>
        <Padding left = 10.dp>
            <Column crossAxisAlignment=CrossAxisAlignment.Start>
                <Checkbox
                    value=state.value
                    color=customColor
                    onToggle={ state.toggle() } />
                <Checkbox
                    value=state2.value
                    onToggle={ state2.toggle() }
                    color=customColor2 />
                <Checkbox
                    value=state3.value
                    onToggle={ state3.toggle() }
                    color=customColor3 />
            </Column>
        </Padding>
    </Column>
}

@Composable
fun SwitchDemo() {
    <Row
        mainAxisAlignment=MainAxisAlignment.SpaceAround
        mainAxisSize=MainAxisSize.Min>
        val (checked, onChecked) = +state { false }
        val (checked2, onChecked2) = +state { false }
        val (checked3, onChecked3) = +state { true }
        val (checked4, onChecked4) = +state { true }
        <Switch checked onChecked />
        <Switch checked=checked2 onChecked=onChecked2 color=customColor />
        <Switch checked=checked3 onChecked=onChecked3 color=customColor2 />
        <Switch checked=checked4 onChecked=onChecked4 color=customColor3 />
    </Row>
}

@Composable
fun RadioButtonDemo() {
    <Row
        mainAxisAlignment=MainAxisAlignment.SpaceAround
        mainAxisSize=MainAxisSize.Min>
        <RadioButton selected=true />
        <RadioButton selected=false />
        <RadioButton selected=true color=customColor />
        <RadioButton selected=false color=customColor />
    </Row>
}