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
import androidx.ui.animation.Transition
import androidx.ui.baseui.selection.MutuallyExclusiveSetItem
import androidx.ui.core.DensityReceiver
import androidx.ui.core.Dp
import androidx.ui.core.Layout
import androidx.ui.core.PxSize
import androidx.ui.core.Text
import androidx.ui.core.adapter.Draw
import androidx.ui.core.dp
import androidx.ui.core.max
import androidx.ui.core.min
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.shift
import androidx.ui.engine.geometry.shrink
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
import androidx.ui.painting.TextStyle
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer
import com.google.r4a.memo
import com.google.r4a.unaryPlus

/**
 * Components for creating mutually exclusive set of [RadioButton]s.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again.
 *
 * Typical children for RadioGroup will be [RadioGroupScope.RadioGroupItem] and following usage:
 *
 *     <RadioGroup> options.forEach { item ->
 *         <RadioGroupTextItem text=item.toString() selected=... onSelected={...}/>
 *      }
 *     </RadioGroup>
 *
 * If you want a simplest ready-to-use version, consider using
 *     <RadioGroup .../>
 * without children
 */
@Composable
fun RadioGroup(
    @Children children: RadioGroupScope.() -> Unit
) {
    val scope = +memo { RadioGroupScope() }
    <Column mainAxisSize=MainAxisSize.Min>
        <children p1=scope />
    </Column>
}

/**
 * Components for creating mutually exclusive set of [RadioButton]
 * as well as text label for this RadioButtons.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again. This component guarantees
 * that there will be only one or none selected RadioButton at a time
 *
 * This component is ready to use without children being passed and it uses
 * [RadioGroupScope.RadioGroupTextItem] as a child implementation
 *
 * @param options list of [String] to provide RadioButtons label
 * @param selectedOption label which represents selected RadioButton,
 * or [null] if nothing is selected
 * @param onOptionSelected callback to be invoked when RadioButton is selected
 * @param radioColor color for RadioButtons when selected
 * @param textStyle parameters for text customization
 */
@Composable
fun RadioGroup(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    radioColor: Color? = null,
    textStyle: TextStyle? = null
) {
    <RadioGroup> options.forEach { text ->
        <RadioGroupTextItem
            selected=(text == selectedOption)
            onSelected={ onOptionSelected(text) }
            text
            radioColor
            textStyle />
    }
    </RadioGroup>
}

class RadioGroupScope internal constructor() {

    /**
     * Basic item to be used inside [RadioGroup].
     *
     * This component provides basic radio item behavior such as
     * clicks and accessibility support.
     *
     * If you need ready-to-use item with RadioButton and Text inside,
     * consider using [RadioGroupTextItem].
     *
     * @param selected whether or not this item is selected
     * @param onSelected callback to be invoked when your item is selected,
     * does nothing if item is already selected
     */
    @Composable
    fun RadioGroupItem(
        selected: Boolean,
        onSelected: () -> Unit,
        @Children children: () -> Unit
    ) {
        <MutuallyExclusiveSetItem
            selected
            onSelected={ if (!selected) onSelected() }>
            <children />
        </MutuallyExclusiveSetItem>
    }

    /**
     * Simple component to be used inside [RadioGroup] as a child
     * Consists of [RadioButton] and [Text] that follows this button
     *
     * Defaults used:
     * * for text, [MaterialTypography.body1] will be used
     * * for selected radio button, [MaterialColors.primary] will be used
     *
     * @param selected whether or not radio button in this item is selected
     * @param onSelected callback to be invoked when your item is selected
     * does nothing if item is already selected
     * @param text to put as a label description of this item
     * @param radioColor color for RadioButtons when selected
     * @param textStyle parameters for text customization
     */
    @Composable
    fun RadioGroupTextItem(
        selected: Boolean,
        onSelected: () -> Unit,
        text: String,
        radioColor: Color? = null,
        textStyle: TextStyle? = null
    ) {
        <RadioGroupItem selected onSelected>
            val padding =
                EdgeInsets(top = DefaultRadioItemPadding, bottom = DefaultRadioItemPadding)
            <Padding padding>
                <Row mainAxisSize=MainAxisSize.Max mainAxisAlignment=MainAxisAlignment.Start>
                    <RadioButton selected color=radioColor />
                    <Padding padding=EdgeInsets(left = DefaultRadioLabelOffset)>
                        <StyledText text style=+themeTextStyle { body1.merge(textStyle) } />
                    </Padding>
                </Row>
            </Padding>
        </RadioGroupItem>
    }
}

/**
 * Component to represent two states, selected and not selected.
 *
 * RadioButtons are usually coupled together to [RadioGroup] to represent
 * multiply-exclusion set of options for user to choose from
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param color optional color. [MaterialColors.primary] is used by default
 */
@Composable
fun RadioButton(
    selected: Boolean,
    color: Color? = null
) {
    <Layout layoutBlock={ _, constraints ->
        val size = RadioRadius.toIntPx() * 2
        val w = max(constraints.minWidth, min(constraints.maxWidth, size))
        val h = max(constraints.minHeight, min(constraints.maxHeight, size))
        layout(w, h) {
            // no children to place
        }
    }>
        val activeColor = +color.orFromTheme { primary }
        val definition = +memo(activeColor) {
            generateTransitionDefinition(activeColor)
        }
        <Transition definition toState=selected> state ->
            <DrawRadioButton
                color=state[ColorProp]
                outerRadius=state[OuterRadiusProp]
                innerRadius=state[InnerRadiusProp]
                gap=state[GapProp] />
        </Transition>
    </Layout>
}

@Composable
private fun DrawRadioButton(color: Color, outerRadius: Dp, innerRadius: Dp, gap: Dp) {
    <Draw> canvas, parentSize ->
        drawRadio(canvas, parentSize, color, outerRadius, innerRadius, gap)
    </Draw>
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

private fun generateTransitionDefinition(activeColor: Color) = transitionDefinition {
    state(false) {
        this[OuterRadiusProp] = RadioRadius
        this[InnerRadiusProp] = InitialInner
        this[GapProp] = 0.dp
        this[ColorProp] = UnselectedRadioColor
    }
    state(true) {
        this[OuterRadiusProp] = RadioRadius
        this[InnerRadiusProp] = 0.dp
        this[GapProp] = DefaultGap
        this[ColorProp] = activeColor
    }
    transition(fromState = false, toState = true) {
        ColorProp using tween {
            duration = 0
        }
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
            delay = (RadiusClosureDuration + PulseDuration).toLong()
            duration = GapDuration
        }
    }
    transition(fromState = true, toState = false) {
        ColorProp using tween {
            duration = 0
        }
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
            delay = (GapDuration + PulseDuration).toLong()
            duration = RadiusClosureDuration
        }
    }
}

// TODO(malkov): see how it goes and maybe move it to styles or cross-widget defaults
private val UnselectedRadioColor = Color(0xFF7D7D7D.toInt())

// TODO(malkov): random numbers for now to produce radio as in material comp.
private val RadioRadius = 10.dp
private val RadioStrokeWidth = 2.dp
private val DefaultGap = 3.dp

// for animations
private val OuterOffsetDuringAnimation = 2.dp
private val PulseDelta = 0.5.dp
private val InitialInner = RadioRadius - RadioStrokeWidth

private val DefaultRadioLabelOffset = 20.dp
private val DefaultRadioItemPadding = 10.dp
