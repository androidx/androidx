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

import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.remember
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Box
import androidx.ui.foundation.Canvas
import androidx.ui.foundation.Text
import androidx.ui.foundation.selection.selectable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSize
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp

/**
 * Components for creating mutually exclusive set of [RadioButton]s.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again.
 *
 * Typical content for RadioGroup will be [RadioGroupScope.RadioGroupItem] and following usage:
 *
 * @sample androidx.ui.material.samples.CustomRadioGroupSample
 *
 * If you want a simplified version with [Column] of [RadioGroupScope.RadioGroupTextItem],
 * consider using version that accepts list of [String] options and doesn't require any children
 */
@Composable
fun RadioGroup(content: @Composable RadioGroupScope.() -> Unit) {
    val scope = remember { RadioGroupScope() }
    scope.content()
}

/**
 * Components for creating mutually exclusive set of [RadioButton]
 * as well as text label for this RadioButtons.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again. This component guarantees
 * that there will be only one or none selected RadioButton at a time.
 *
 * This component is ready to use without children being passed and
 * it places the options into a [Column] of [RadioGroupScope.RadioGroupTextItem].
 *
 * @sample androidx.ui.material.samples.DefaultRadioGroupSample
 *
 * @param options list of [String] to provide RadioButtons label
 * @param selectedOption label which represents selected RadioButton,
 * or `null` if nothing is selected
 * @param onSelectedChange callback to be invoked when RadioButton is clicked,
 * therefore the selection of this item is requested
 * @param modifier Modifier to be applied to the radio group layout
 * @param radioColor color for RadioButtons when selected.
 * @param textStyle parameters for text customization
 */
@Composable
fun RadioGroup(
    options: List<String>,
    selectedOption: String?,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    radioColor: Color = MaterialTheme.colors.secondary,
    textStyle: TextStyle? = null
) {
    RadioGroup {
        Column(modifier) {
            options.forEach { text ->
                RadioGroupTextItem(
                    selected = (text == selectedOption),
                    onSelect = { onSelectedChange(text) },
                    text = text,
                    radioColor = radioColor,
                    textStyle = textStyle
                )
            }
        }
    }
}

/**
 * Scope of [RadioGroup] to grant access to [RadioGroupItem] and others. This scope will be
 * provided automatically to the children of [RadioGroup].
 */
@Stable
class RadioGroupScope internal constructor() {

    /**
     * Basic item to be used inside [RadioGroup].
     *
     * This component provides basic radio item behavior such as
     * clicks and accessibility support.
     *
     * If you need simple item with [RadioButton] and [Text] wrapped in a [Row],
     * consider using [RadioGroupTextItem].
     *
     * @param selected whether or not this item is selected
     * @param onSelect callback to be invoked when your item is being clicked,
     * therefore the selection of this item is requested. Not invoked if item is already selected
     * @param modifier Modifier to be applied to this item
     */
    @Composable
    fun RadioGroupItem(
        selected: Boolean,
        onSelect: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier
                .semantics(mergeAllDescendants = true)
                .selectable(
                    selected = selected,
                    onClick = { if (!selected) onSelect() }
                ),
            children = content
        )
    }

    /**
     * Simple component to be used inside [RadioGroup] as a child.
     * Places [RadioButton] and [Text] inside the [Row].
     *
     * @param selected whether or not radio button in this item is selected
     * @param onSelect callback to be invoked when your item is being clicked, therefore the
     * selection of this item is requested. Not invoked if item is already selected
     * @param text to put as a label description of this item
     * @param modifier Modifier to be applied to this item
     * @param radioColor color for RadioButtons when selected
     * @param textStyle parameters for text customization
     */
    @Composable
    fun RadioGroupTextItem(
        selected: Boolean,
        onSelect: () -> Unit,
        text: String,
        modifier: Modifier = Modifier,
        radioColor: Color = MaterialTheme.colors.secondary,
        textStyle: TextStyle? = null
    ) {
        RadioGroupItem(selected = selected, onSelect = onSelect, modifier = modifier) {
            // TODO: remove this Box when Ripple becomes a modifier.
            Box {
                Row(Modifier.fillMaxWidth().padding(DefaultRadioItemPadding)) {
                    RadioButton(selected = selected, onSelect = onSelect, color = radioColor)
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body1.merge(textStyle),
                        modifier = Modifier.padding(start = DefaultRadioLabelOffset)
                    )
                }
            }
        }
    }
}

/**
 * Component to represent two states, selected and not selected.
 *
 * RadioButton is usually used as a child of [RadioGroupScope.RadioGroupItem], and these items
 * and coupled together to [RadioGroup] to represent a multiple exclusion set of options
 * the user can choose from.
 *
 * @sample androidx.ui.material.samples.RadioButtonSample
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param onSelect callback to be invoked when RadioButton is being clicked,
 * therefore the selection of this item is requested. Not invoked if item is already selected
 * @param modifier Modifier to be applied to radio button
 * @param color color of the RadioButton
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelect: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.secondary
) {
    Stack(
        modifier.selectable(
            selected = selected,
            onClick = { if (!selected) onSelect?.invoke() },
            indication = RippleIndication(bounded = false)
        )
    ) {
        val unselectedColor =
            MaterialTheme.colors.onSurface.copy(alpha = UnselectedOpacity)
        val definition = remember(color, unselectedColor) {
            generateTransitionDefinition(color, unselectedColor)
        }
        Transition(definition = definition, toState = selected) { state ->
            val radioStroke = Stroke(RadioStrokeWidth.value * DensityAmbient.current.density)
            Canvas(Modifier.padding(RadioButtonPadding).preferredSize(RadioButtonSize)) {
                drawRadio(
                    state[ColorProp],
                    state[OuterRadiusProp].toPx(),
                    state[InnerRadiusProp].toPx(),
                    state[GapProp].toPx(),
                    radioStroke
                )
            }
        }
    }
}

private fun DrawScope.drawRadio(
    color: Color,
    outerPx: Float,
    innerPx: Float,
    gapWidth: Float,
    radioStroke: Stroke
) {
    // TODO(malkov): currently Radio gravity is always CENTER but we need to be flexible
    val centerW = center.dx
    val centerH = center.dy

    val center = Offset(centerW, centerH)
    if (gapWidth == 0.0f) {
        val strokeWidth = outerPx - innerPx
        drawCircle(color, outerPx - strokeWidth / 2, center, style = Stroke(strokeWidth))
    } else {
        drawCircle(color, outerPx - radioStroke.width / 2, center, style = radioStroke)
        val circleRadius = outerPx - radioStroke.width - gapWidth
        val innerCircleStrokeWidth = circleRadius - innerPx

        drawCircle(
            color, circleRadius - innerCircleStrokeWidth / 2, center,
            style = Stroke(innerCircleStrokeWidth)
        )
    }
}

private val OuterRadiusProp = DpPropKey()
private val InnerRadiusProp = DpPropKey()
private val GapProp = DpPropKey()
private val ColorProp = ColorPropKey()

private const val RadiusClosureDuration = 150
private const val PulseDuration = 100
private const val GapDuration = 150
private const val TotalDuration = RadiusClosureDuration + PulseDuration + GapDuration

private fun generateTransitionDefinition(
    selectedColor: Color,
    unselectedColor: Color
) = transitionDefinition {
    state(false) {
        this[OuterRadiusProp] = RadioRadius
        this[InnerRadiusProp] = InitialInner
        this[GapProp] = 0.dp
        this[ColorProp] = unselectedColor
    }
    state(true) {
        this[OuterRadiusProp] = RadioRadius
        this[InnerRadiusProp] = 0.dp
        this[GapProp] = DefaultGap
        this[ColorProp] = selectedColor
    }
    transition(fromState = false, toState = true) {
        ColorProp using snap()
        OuterRadiusProp using keyframes {
            val smallerOuter = RadioRadius - OuterOffsetDuringAnimation + RadioStrokeWidth / 2
            duration = TotalDuration
            RadioRadius at 0
            smallerOuter at RadiusClosureDuration
            smallerOuter + PulseDelta at RadiusClosureDuration + PulseDuration / 2
            smallerOuter at RadiusClosureDuration + PulseDuration
            RadioRadius at TotalDuration
        }
        InnerRadiusProp using tween {
            duration = RadiusClosureDuration
        }
        GapProp using tween {
            delay = RadiusClosureDuration + PulseDuration
            duration = GapDuration
        }
    }
    transition(fromState = true, toState = false) {
        ColorProp using snap()
        OuterRadiusProp using keyframes {
            val smallerOuter = RadioRadius - OuterOffsetDuringAnimation + RadioStrokeWidth / 2
            duration = TotalDuration
            RadioRadius at 0
            smallerOuter at GapDuration
            smallerOuter + PulseDelta at GapDuration + PulseDuration / 2
            smallerOuter at GapDuration + PulseDuration
            RadioRadius at TotalDuration
        }
        GapProp using tween {
            duration = GapDuration
        }
        InnerRadiusProp using tween {
            delay = GapDuration + PulseDuration
            duration = RadiusClosureDuration
        }
    }
}

private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioStrokeWidth = 2.dp
// TODO(malkov): random numbers for now to produce radio as in material comp.
private val DefaultGap = 3.dp
private const val UnselectedOpacity = 0.6f

// for animations
private val OuterOffsetDuringAnimation = 2.dp
private val PulseDelta = 0.5.dp
private val InitialInner = RadioRadius - RadioStrokeWidth

private val DefaultRadioLabelOffset = 20.dp
private val DefaultRadioItemPadding = 10.dp
