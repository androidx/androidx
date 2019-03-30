/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.material

import androidx.ui.baseui.selection.MutuallyExclusiveSetItem
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.MeasureBox
import androidx.ui.core.PxSize
import androidx.ui.core.Text
import androidx.ui.core.adapter.Draw
import androidx.ui.core.dp
import androidx.ui.core.max
import androidx.ui.core.min
import androidx.ui.engine.geometry.Offset
import androidx.ui.layout.Column
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.MainAxisSize
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.unaryPlus

// TODO(malkov): need to support case when no radio buttons selected
/**
 * Components for creating mutually exclusive set of [RadioButton]s.
 * Because of the nature of mutually exclusive set, when radio button is checked,
 * it can't be unchecked by being pressed again. This component guarantees
 * that there always will be only one selected item at a time.
 *
 * The selection identified by unique key, parametrized as [K] and onOptionSelected
 * callback invoked when new child was selected
 *
 * Typical children for RadioGroup will be [RadioGroup.Item] and following usage:
 *
 *     <RadioGroup ...> key, isSelected ->
 *         <RadioGroupItem text=key.toString() isSelected>
 *     </RadioGroup>
 *
 * If you want a simplest ready-to-use version, consider using
 *     <RadioGroup .../>
 * without children
 *
 * @param options id to value container to provide data for every RadioButton option
 * @param selectedOption id of type [K] which represent selected RadioButton option
 * @param onOptionSelected callback to be invoked when RadioButton item is selected
 * @param children components to draw ui horizontally after RadioButton based on value V
 */
@Composable
fun <K> RadioGroup(
    options: List<K>,
    selectedOption: K,
    onOptionSelected: (key: K) -> Unit,
    @Children children: RadioGroupScope.(key: K, isSelected: Boolean) -> Unit
) {
    val scope = +memo { RadioGroupScope() }
    <Column mainAxisSize=MainAxisSize.Min> options.forEach { key ->
        val selected = selectedOption == key
        <MutuallyExclusiveSetItem
            key=key
            selected
            onSelected={ onOptionSelected(it) }>
            <children p1=scope key=key isSelected=selected />
        </MutuallyExclusiveSetItem>
    }
    </Column>
}

/**
 * Components for creating mutually exclusive set of [RadioButton]
 * as well as text label for this RadioButtons.
 * Because of the nature of mutually exclusive set, when radio button is checked,
 * it can't be unchecked by being pressed again. This component guarantees
 * that there always will be only one selected RadioButton at a time
 *
 * This component is ready to use without children being passed and it uses
 * RadioGroupItem as a child implementation
 *
 * The selection identified by unique key, parametrized as [K] and onSelectedChanged
 * callback invoked when new RadioButton was selected
 *
 * @param options id to text container to provide RadioButtons id and label
 * @param selectedOption id of type K which represent selected RadioButton
 * @param onOptionSelected callback to be invoked when RadioButton is selected
 * @param radioStyle parameters for RadioButtons customization
 * @param textStyle parameters for text customization
 */
@Composable
fun <K> RadioGroup(
    options: Map<K, String>,
    selectedOption: K,
    onOptionSelected: (K) -> Unit,
    labelOffset: Dp = DefaultRadioLabelOffset,
    radioColor: Color? = null,
    textStyle: TextStyle? = null
) {
    val keys = +memo(options) { options.keys.toList() }
    <RadioGroup options=keys selectedOption onOptionSelected> key, isSelected ->
        val text = options[key]!!
        <Padding padding=EdgeInsets(DefaultRadioItemPadding)>
            <RadioTextItem text isSelected labelOffset radioColor textStyle />
        </Padding>
    </RadioGroup>
}

class RadioGroupScope internal constructor() {
    /**
     * Simple component to be used inside [RadioGroup] as a child
     * Consists of radio group and text that follows this button
     *
     * Defaults used: for text, [MaterialTypography.body1] will be used
     *
     * @param isSelected whether or not radio button in this item should be selected
     * @param text to put as a visual description of this item
     * @param radioStyle parameters for RadioButtons customization
     * @param textStyle parameters for text customization
     */
    @Composable
    fun RadioTextItem(
        isSelected: Boolean,
        text: String,
        labelOffset: Dp = DefaultRadioLabelOffset,
        radioColor: Color? = null,
        textStyle: TextStyle? = null
    ) {
        val typography = +ambient(Typography)
        <Row mainAxisSize=MainAxisSize.Max mainAxisAlignment=MainAxisAlignment.Start>
            <RadioButton selected=isSelected color=radioColor />
            <Padding padding=EdgeInsets(left = labelOffset)>
                <Text text=TextSpan(style = typography.body1.merge(textStyle), text = text) />
            </Padding>
        </Row>
    }
}

/**
 * Component to represent two states, selected and unchecked.
 *
 * RadioButtons are usually coupled together to [RadioGroup] to represent
 * multiply-exclusion set of options for user to choose from
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param color optional color. [MaterialColor.primary] is used by default
 */
@Composable
fun RadioButton(
    selected: Boolean,
    color: Color? = null
) {
    <MeasureBox> constraints ->
        collect {
            val colors = +ambient(Colors)
            <DrawRadioButton selected color=(color ?: colors.primary) />
        }
        val size = RadioRadius.toIntPx() * 2
        val w = max(constraints.minWidth, min(constraints.maxWidth, size))
        val h = max(constraints.minHeight, min(constraints.maxHeight, size))
        layout(w, h) {
            // no children to place
        }
    </MeasureBox>
}

@Composable
private fun DrawRadioButton(selected: Boolean, color: Color) {
    <Draw> canvas, parentSize ->
        drawRadio(canvas, parentSize, selected, color)
    </Draw>
}

private fun DensityReceiver.drawRadio(
    canvas: Canvas,
    parentSize: PxSize,
    selected: Boolean,
    color: Color
) {
    val p = Paint()
    p.isAntiAlias = true
    p.color = if (selected) color else UnselectedRadioColor
    p.strokeWidth = RadioStrokeWidth.toPx().value
    p.style = PaintingStyle.stroke

    // TODO(malkov): currently Radio gravity is always CENTER but we need to be flexible
    val centerW = parentSize.width.value / 2
    val centerH = parentSize.height.value / 2
    val center = Offset(centerW, centerH)

    canvas.drawCircle(center, (RadioRadius - strokeWidth / 2).toPx().value, p)

    if (selected) {
        p.style = PaintingStyle.fill
        p.strokeWidth = 0f
        canvas.drawCircle(center, InnerCircleSize.toPx().value, p)
    }
}

// TODO(malkov): see how it goes and maybe move it to styles or cross-widget defaults
private val UnselectedRadioColor = Color(0xFF7D7D7D.toInt())

// TODO(malkov): random numbers for now to produce radio as in material comp.
private val InnerCircleSize = 4.75.dp
private val RadioStrokeWidth = 2.dp
private val RadioRadius = 10.dp

private val DefaultRadioItemPadding = 10.dp
private val DefaultRadioLabelOffset = 20.dp