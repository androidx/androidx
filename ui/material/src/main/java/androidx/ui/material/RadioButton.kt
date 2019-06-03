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

import androidx.animation.ColorPropKey
import androidx.animation.DpPropKey
import androidx.animation.transitionDefinition
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.Transition
import androidx.ui.baseui.selection.MutuallyExclusiveSetItem
import androidx.ui.baseui.selection.Toggleable
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.PxSize
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.shift
import androidx.ui.engine.geometry.shrink
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.MainAxisSize
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.layout.Wrap
import androidx.ui.material.ripple.Ripple
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.PaintingStyle
import androidx.ui.painting.TextStyle

/**
 * Components for creating mutually exclusive set of [RadioButton]s.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again.
 *
 * Typical children for RadioGroup will be [RadioGroupScope.RadioGroupItem] and following usage:
 *
 *     RadioGroup {
 *         Column { options.forEach { item ->
 *             RadioGroupTextItem(text = item.toString(), selected = ..., onSelected = { ... })
 *         }
 *         }
 *     }
 *
 * If you want a simplified version with [Column] of [RadioGroupScope.RadioGroupTextItem],
 * consider using version that accepts list of [String] options and doesn't require any children:
 *
 *     RadioGroup(
 *         options = listOfOption,
 *         selectedOption = ...,
 *         onOptionSelected = { ... })
 */
@Composable
fun RadioGroup(@Children children: @Composable RadioGroupScope.() -> Unit) {
    val scope = +memo { RadioGroupScope() }
    children(p1 = scope)
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
 * @param options list of [String] to provide RadioButtons label
 * @param selectedOption label which represents selected RadioButton,
 * or [null] if nothing is selected
 * @param onSelectedChange callback to be invoked when RadioButton is clicked,
 * therefore the selection of this item is requested
 * @param radioColor color for RadioButtons when selected.
 * [MaterialColors.secondary] is used by default
 * @param textStyle parameters for text customization
 */
@Composable
fun RadioGroup(
    options: List<String>,
    selectedOption: String?,
    onSelectedChange: (String) -> Unit,
    radioColor: Color = +themeColor { secondary },
    textStyle: TextStyle? = null
) {
    RadioGroup {
        Column(mainAxisSize = MainAxisSize.Min) {
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
     */
    @Composable
    fun RadioGroupItem(
        selected: Boolean,
        onSelect: () -> Unit,
        @Children children: @Composable() () -> Unit
    ) {
        Container {
            Ripple(bounded = true) {
                MutuallyExclusiveSetItem(
                    selected = selected,
                    onClick = { if (!selected) onSelect() }) {
                    children()
                }
            }
        }
    }

    /**
     * Simple component to be used inside [RadioGroup] as a child.
     * Places [RadioButton] and [Text] inside the [Row].
     *
     * Defaults used:
     * * for text, [MaterialTypography.body1] will be used
     * * for selected radio button, [MaterialColors.primary] will be used
     *
     * @param selected whether or not radio button in this item is selected
     * @param onSelect callback to be invoked when your item is being clicked,
     * therefore the selection of this item is requested. Not invoked if item is already selected
     * @param text to put as a label description of this item
     * @param radioColor color for RadioButtons when selected.
     * [MaterialColors.secondary] is used by default
     * @param textStyle parameters for text customization
     */
    @Composable
    fun RadioGroupTextItem(
        selected: Boolean,
        onSelect: () -> Unit,
        text: String,
        radioColor: Color = +themeColor { secondary },
        textStyle: TextStyle? = null
    ) {
        RadioGroupItem(selected = selected, onSelect = onSelect) {
            Padding(padding = DefaultRadioItemPadding) {
                Row(mainAxisSize = MainAxisSize.Max, mainAxisAlignment = MainAxisAlignment.Start) {
                    RadioButton(selected = selected, onSelect = onSelect, color = radioColor)
                    Padding(left = DefaultRadioLabelOffset) {
                        Text(text = text, style = +themeTextStyle { body1.merge(textStyle) })
                    }
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
 * @param selected boolean state for this button: either it is selected or not
 * @param onSelect callback to be invoked when RadioButton is being clicked,
 * therefore the selection of this item is requested. Not invoked if item is already selected
 * @param color optional color. [MaterialColors.secondary] is used by default
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelect: (() -> Unit)?,
    color: Color = +themeColor { secondary }
) {
    Wrap {
        Ripple(bounded = false) {
            Toggleable(
                value = if (selected) ToggleableState.Checked else ToggleableState.Unchecked,
                onToggle = onSelect
            ) {
                Padding(padding = RadioButtonPadding) {
                    Container(width = RadioButtonSize, height = RadioButtonSize) {
                        val unselectedColor =
                            (+themeColor { onSurface }).copy(alpha = UnselectedOpacity)
                        val definition = +memo(color, unselectedColor) {
                            generateTransitionDefinition(color, unselectedColor)
                        }
                        Transition(definition = definition, toState = selected) { state ->
                            DrawRadioButton(
                                color = state[ColorProp],
                                outerRadius = state[OuterRadiusProp],
                                innerRadius = state[InnerRadiusProp],
                                gap = state[GapProp]
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawRadioButton(color: Color, outerRadius: Dp, innerRadius: Dp, gap: Dp) {
    Draw { canvas, parentSize ->
        drawRadio(canvas, parentSize, color, outerRadius, innerRadius, gap)
    }
}

private fun DensityReceiver.drawRadio(
    canvas: Canvas,
    parentSize: PxSize,
    color: Color,
    outerRadius: Dp,
    innerRadius: Dp,
    gap: Dp
) {
    val p = Paint()
    p.isAntiAlias = true
    p.color = color
    p.style = PaintingStyle.fill

    // TODO(malkov): currently Radio gravity is always CENTER but we need to be flexible
    val centerW = parentSize.width.value / 2
    val centerH = parentSize.height.value / 2
    val outerPx = outerRadius.toPx().value
    val innerPx = innerRadius.toPx().value

    val circleOffset = Offset(centerW - outerPx, centerH - outerPx)
    val outer = RRect(
        0f,
        0f,
        outerPx * 2,
        outerPx * 2,
        Radius.circular(outerPx)
    ).shift(circleOffset)

    if (gap == 0.dp) {
        val inner = outer.shrink(outerPx - innerPx)
        canvas.drawDRRect(outer, inner, p)
    } else {
        val inner = outer.shrink(RadioStrokeWidth.toPx().value)
        canvas.drawDRRect(outer, inner, p)
        val radioOuter = inner.shrink(gap.toPx().value)
        val radioInner = outer.shrink(outerPx - innerPx)
        canvas.drawDRRect(radioOuter, radioInner, p)
    }
}

private val OuterRadiusProp = DpPropKey()
private val InnerRadiusProp = DpPropKey()
private val GapProp = DpPropKey()
private val ColorProp = ColorPropKey()

private val RadiusClosureDuration = 150
private val PulseDuration = 100
private val GapDuration = 150
private val TotalDuration = RadiusClosureDuration + PulseDuration + GapDuration

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
private val UnselectedOpacity = 0.6f

// for animations
private val OuterOffsetDuringAnimation = 2.dp
private val PulseDelta = 0.5.dp
private val InitialInner = RadioRadius - RadioStrokeWidth

private val DefaultRadioLabelOffset = 20.dp
private val DefaultRadioItemPadding = 10.dp
