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
import androidx.ui.layout.Alignment
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.material.Checkbox
import androidx.ui.material.RadioButton
import androidx.ui.material.RadioGroup
import androidx.ui.material.StyledText
import androidx.ui.material.Switch
import androidx.ui.material.Typography
import androidx.ui.material.parentCheckboxState
import androidx.ui.material.themeTextStyle
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.state
import com.google.r4a.unaryPlus

@Model
class CheckboxState(
    var color: Color? = null,
    var value: ToggleableState = Checked
) {
    fun toggle() {
        value = if (value == Checked) Unchecked else Checked
    }

    fun isChecked(): Boolean = value == Checked
}

@Composable
fun SelectionsControlsDemo() {

    val checkboxState = CheckboxState(value = Checked)
    val checkboxState2 = CheckboxState(value = Checked)
    val checkboxState3 = CheckboxState(value = Checked)

    val customColor = Color(0xffff0000.toInt())
    val customColor2 = Color(0xFFE91E63.toInt())
    val customColor3 = Color(0xFF607D8B.toInt())
    val customColor4 = Color(0xFFFF5722.toInt())
    val typography = +ambient(Typography)

    <Column mainAxisAlignment=MainAxisAlignment.Start>
        val radioOptions = listOf("Calls", "Missed", "Friends")
        val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
        <Container padding=EdgeInsets(10.dp) alignment=Alignment.Center>
            <RadioGroup
                options=radioOptions
                selectedOption
                onOptionSelected
                radioColor=customColor2 />
        </Container>
        <Padding padding=EdgeInsets(10.dp)>
            <RadioGroup> radioOptions.forEach { text ->
                <RadioGroupTextItem
                    selected=(text == selectedOption)
                    onSelected={ onOptionSelected(text) }
                    text=text
                    radioColor=customColor3
                    textStyle=typography.caption />
            }
            </RadioGroup>
        </Padding>
        <RadioGroup> radioOptions.drop(1).forEach { option ->
            val selected = option == selectedOption
            <RadioGroupItem selected onSelected={ onOptionSelected(option) }>
                <Padding padding=EdgeInsets(2.5.dp)>
                    <Row>
                        <RadioButton selected color=customColor4 />
                        <Container
                            color=(if (selected) customColor2 else customColor3)
                            width=100.dp
                            height=38.dp
                        >
                            <StyledText text=option.take(1) style=+themeTextStyle { h5 } />
                        </Container>
                    </Row>
                </Padding>
            </RadioGroupItem>
        }
        </RadioGroup>
        <Row mainAxisAlignment=MainAxisAlignment.SpaceAround>
            val parent = parentCheckboxState(
                checkboxState.value,
                checkboxState2.value,
                checkboxState3.value
            )
            <Checkbox value=checkboxState.value onToggle={ checkboxState.toggle() } />
            <Checkbox
                value=checkboxState2.value
                onToggle={ checkboxState2.toggle() }
                color=customColor2 />
            <Checkbox
                value=checkboxState3.value
                onToggle={ checkboxState3.toggle() }
                color=customColor3 />
            <Column>
                <Text text=TextSpan(text = "Parent", style = typography.h3) />
                <Checkbox
                    value=parent
                    color=customColor4
                    onToggle={
                        val s = if (parent == Checked) Unchecked
                        else Checked
                        checkboxState.value = s
                        checkboxState2.value = s
                        checkboxState3.value = s
                    } />
            </Column>
        </Row>
        <Row mainAxisAlignment=MainAxisAlignment.SpaceAround>
            val (c, onC) = +state { true }
            val (c2, onC2) = +state { false }
            val (c3, onC3) = +state { true }
            val (c4, onC4) = +state { false }
            <Switch checked=c onChecked=onC />
            <Switch checked=c2 onChecked=onC2 color=customColor2 />
            <Switch checked=c3 onChecked=onC3 color=customColor3 />
            <Switch checked=c4 onChecked=onC4 color=customColor4 />
        </Row>
        <Row mainAxisAlignment=MainAxisAlignment.SpaceAround>
            <RadioButton selected=true />
            <RadioButton selected=false />
            <RadioButton selected=true color=customColor />
            <RadioButton selected=false color=customColor />
        </Row>
    </Column>
}
