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
import androidx.ui.core.Constraints
import androidx.ui.core.MeasureBox
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.times
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
import androidx.ui.material.Switch
import androidx.ui.material.Typography
import androidx.ui.material.parentCheckboxState
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.state
import com.google.r4a.unaryPlus

@Composable
fun FillGrid(horizontalGridCount: Int, @Children children: () -> Unit) {
    <MeasureBox> constraints ->
        val measurables = collect(children)
        val verticalGrid = (measurables.size + horizontalGridCount - 1) / horizontalGridCount
        val cellW = constraints.maxWidth / horizontalGridCount
        val cellH = constraints.maxHeight / verticalGrid
        val c = Constraints.tightConstraints(cellW, cellH)
        layout(constraints.maxWidth, constraints.maxHeight) {
            measurables
                .map { it.measure(c) }
                .forEachIndexed { index, placeable ->
                    val x = index % horizontalGridCount * cellW
                    val y = cellH * (index / horizontalGridCount)
                    placeable.place(x, y)
                }
        }
    </MeasureBox>
}

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

    val customColor = +memo {
        Color(0xffff0000.toInt())
    }
    val customColor2 = Color(0xFFE91E63.toInt())
    val customColor3 = Color(0xFF607D8B.toInt())
    val customColor4 = Color(0xFFFF5722.toInt())
    val typography = +ambient(Typography)

    <Column mainAxisAlignment=MainAxisAlignment.Start>
        val (selectedOption, onOptionSelected) = +state { 0 }
        val radioOptions = +memo {
            mapOf(
                0 to "Calls",
                1 to "Missed",
                2 to "Friends"
            )
        }
        val radioOptions2 = +memo {
            listOf(0, 1, 2)
        }
        <Container padding=EdgeInsets(10.dp) alignment=Alignment.Center>
            <RadioGroup
                options=radioOptions
                selectedOption
                onOptionSelected
                labelOffset=5.dp
                radioColor=customColor
                textStyle=typography.h3 />
        </Container>
        <Padding padding=EdgeInsets(10.dp)>
            <RadioGroup
                options=radioOptions2
                selectedOption
                onOptionSelected> key, isSelected ->
                <RadioTextItem
                    isSelected
                    text=radioOptions[key]!!
                    labelOffset=100.dp
                    radioColor=Color(0xffc28285.toInt())
                    textStyle=typography.h2 />
            </RadioGroup>
        </Padding>
        <RadioGroup
            options=radioOptions2
            selectedOption
            onOptionSelected> key, isSelected ->
            <Padding padding=EdgeInsets(2.5.dp)>
                <Container
                    color=(if (isSelected) Color(0xFF00ff0f.toInt()) else Color(0xffff002a.toInt()))
                    width=100.dp
                    height=25.dp
                >
                    val initials = radioOptions[key]!!.take(1)
                    <Text text=TextSpan(
                        style = TextStyle(fontSize = 40f),
                        text = initials
                    ) />
                </Container>
            </Padding>
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
