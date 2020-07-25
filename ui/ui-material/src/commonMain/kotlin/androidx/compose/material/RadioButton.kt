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

package androidx.compose.material

import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.Composable
import androidx.compose.Stable
import androidx.compose.remember
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.DpPropKey
import androidx.compose.animation.transition
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Text
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.RippleIndication
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Components for creating mutually exclusive set of [RadioButton]s.
 * Because of the nature of mutually exclusive set, when radio button is selected,
 * it can't be unselected by being pressed again.
 *
 * If you want a simplified version with [Column] of [RadioGroupScope.RadioGroupTextItem],
 * consider using version that accepts list of [String] options and doesn't require any children
 *
 * @deprecated RadioGroup provides no layout and no additional logic. It is recommended to create
 * such groups manually, to gain flexibility and full control over the state and the appearance
 *
 * See example of a simple radio group-like layout
 * @sample androidx.compose.material.samples.RadioGroupSample
 */
@Composable
@Suppress("DEPRECATION")
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
 * @param options list of [String] to provide RadioButtons label
 * @param selectedOption label which represents selected RadioButton,
 * or `null` if nothing is selected
 * @param onSelectedChange callback to be invoked when RadioButton is clicked,
 * therefore the selection of this item is requested
 * @param modifier Modifier to be applied to the radio group layout
 * @param radioColor color for RadioButtons when selected.
 * @param textStyle parameters for text customization
 *
 * @deprecated RadioGroup provides hardcoded layout and no flexibility. It is recommended to
 * create such layouts manually, to gain flexibility and full control over the state and the
 * appearance.
 *
 * See example of a simple radio group-like layout
 * @sample androidx.compose.material.samples.RadioGroupSample
 */
@Deprecated(
    "RadioGroup provides hardcoded layout and no flexibility. It is recommended to create such " +
            "layouts manually, to gain flexibility and full control over the state and the " +
            "appearance",
    replaceWith = ReplaceWith(
        "Column(modifier) {\n" +
                "options.forEach { text ->\n" +
                "Row(Modifier\n" +
                ".fillMaxWidth()\n" +
                ".selectable(\n" +
                "selected = (text == selectedOption),\n" +
                "onClick = { onSelectedChange(text) }\n" +
                ")\n" +
                ".padding(horizontal = 16.dp)\n" +
                ") {\n" +
                "RadioButton(\n" +
                "selected = (text == selectedOption),\n" +
                "onClick = { onSelectedChange(text) },\n" +
                "selectedColor = radioColor\n" +
                ")\n" +
                "Text(\n" +
                "text = text,\n" +
                "style = MaterialTheme.typography.body1.merge(textStyle),\n" +
                "modifier = Modifier.padding(start = 16.dp)\n" +
                ")\n" +
                "}\n" +
                "}" +
                "}",
        "androidx.compose.material.RadioButton",
        "androidx.compose.foundation.Text",
        "androidx.compose.foundation.layout.Row",
        "androidx.compose.foundation.layout.Column",
        "androidx.compose.foundation.layout.fillMaxWidth",
        "androidx.ui.core.Modifier",
        "androidx.compose.ui.unit.dp",
        "androidx.compose.foundation.selection.selectable"
    )
)
@Composable
fun RadioGroup(
    options: List<String>,
    selectedOption: String?,
    onSelectedChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    radioColor: Color = MaterialTheme.colors.secondary,
    textStyle: TextStyle? = null
) {
    Column(modifier) {
        options.forEach { text ->
            Row(Modifier
                .fillMaxWidth()
                .selectable(
                    selected = (text == selectedOption),
                    onClick = { onSelectedChange(text) }
                )
                .padding(horizontal = 16.dp)
            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = { onSelectedChange(text) },
                    selectedColor = radioColor
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.body1.merge(textStyle),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

/**
 * Scope of [RadioGroup] to grant access to [RadioGroupItem] and others. This scope will be
 * provided automatically to the children of [RadioGroup].
 *
 * @deprecated This Scope has been deprecated alongside RadioGroup. Use [Row] and [Column] with
 * [RadioButton]s and [Text]s instead
 */
@Deprecated(
    "This Scope has been deprecated alongside RadioGroup. Use Row and Column with " +
            "RadioButtons instead"
)
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
     *
     * @deprecated RadioGroupItem provides no flexibility or real value. Use [Box] with [Modifier
     * .selectable] instead
     */
    @Deprecated(
        "RadioGroupItem provides no flexibility or real value. Use Box with Modifier" +
                ".selectable instead",
        replaceWith = ReplaceWith(
            "Box(\n" +
                    "modifier = modifier.selectable(\n" +
                    "selected = selected,\n" +
                    "onClick = { if (!selected) onSelect() }\n" +
                    "),\n" +
                    "children = content\n" +
                    ")",
            "androidx.compose.foundation.selection.selectable",
            "androidx.compose.foundation.Box"
        )
    )
    @Composable
    fun RadioGroupItem(
        selected: Boolean,
        onSelect: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier.selectable(
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
     *
     * @deprecated RadioGroupItem provides no flexibility for ui or state hoisting.
     * Use [Box] with [Modifier.selectable] and [Row] or [Column] to place together [RadioButton]
     * and [Text] as you like
     */
    @Deprecated(
        "RadioGroupItem provides no flexibility or real value. Use Box with Modifier" +
                ".selectable and Row or Column to place together RadioButton and Text as you like",
        replaceWith = ReplaceWith(
            "Box(\n" +
                    "modifier = modifier.selectable(\n" +
                    "selected = selected,\n" +
                    "onClick = { if (!selected) onSelect() }\n" +
                    "),\n" +
                    "children = {\n" +
                    "Box {\n" +
                    "Row(Modifier.fillMaxWidth().padding(16.dp)) {\n" +
                    "RadioButton(selected = selected, onClick = onSelect, selectedColor =" +
                    " radioColor)\n" +
                    "Text(\n" +
                    "text = text,\n" +
                    "style = MaterialTheme.typography.body1.merge(textStyle),\n" +
                    "modifier = Modifier.padding(start = 16.dp)\n" +
                    ")\n" +
                    "}\n" +
                    "}\n" +
                    "}\n" +
                    ")",
            "androidx.compose.foundation.selection.selectable",
            "androidx.compose.foundation.Box"
        )
    )
    @Composable
    fun RadioGroupTextItem(
        selected: Boolean,
        onSelect: () -> Unit,
        text: String,
        modifier: Modifier = Modifier,
        radioColor: Color = MaterialTheme.colors.secondary,
        textStyle: TextStyle? = null
    ) {
        Box(
            modifier = modifier.selectable(
                selected = selected,
                onClick = { if (!selected) onSelect() }
            ),
            children = {
                // TODO: remove this Box when Ripple becomes a modifier.
                Box {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        RadioButton(
                            selected = selected,
                            onClick = onSelect,
                            selectedColor = radioColor
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.body1.merge(textStyle),
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        )
    }
}

/**
 * Component to represent two states, selected and not selected.
 *
 * @sample androidx.compose.material.samples.RadioButtonSample
 *
 * [RadioButton]s can be combined together with [Text] in the desired layout (e.g. [Column] or
 * [Row]) to achieve radio group-like behaviour, where the entire layout is selectable:
 *
 * @sample androidx.compose.material.samples.RadioGroupSample
 *
 * @param selected boolean state for this button: either it is selected or not
 * @param onClick callback to be invoked when the RadioButton is being clicked
 * @param modifier Modifier to be applied to the radio button
 * @param enabled Controls the enabled state of the [RadioButton]. When `false`, this button will
 * not be selectable and appears in the disabled ui state
 * @param selectedColor color of the RadioButton when selected
 * @param unselectedColor color of the RadioButton when not selected
 * @param disabledColor color of the RadioButton when disabled
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = MaterialTheme.colors.secondary,
    unselectedColor: Color = RadioButtonConstants.defaultUnselectedColor,
    disabledColor: Color = RadioButtonConstants.defaultDisabledColor
) {
    val definition = remember(selectedColor, unselectedColor) {
        generateTransitionDefinition(selectedColor, unselectedColor)
    }
    val state = transition(definition = definition, toState = selected)
    Canvas(
        modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                indication = RippleIndication(bounded = false, radius = RadioButtonRippleRadius)
            )
            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .size(RadioButtonSize)
    ) {
        val color = if (enabled) state[ColorProp] else disabledColor
        drawRadio(color, state[DotRadiusProp])
    }
}

/**
 * Constants used in [RadioButton].
 */
object RadioButtonConstants {

    /**
     * Default color that will be used for [RadioButton] when disabled
     */
    @Composable
    val defaultDisabledColor: Color
        get() {
            return EmphasisAmbient.current.disabled.applyEmphasis(
                MaterialTheme.colors.onSurface
            )
        }

    /**
     * Default color that will be used for [RadioButton] when unselected
     */
    @Composable
    val defaultUnselectedColor: Color
        get() {
            return MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        }
}

private fun DrawScope.drawRadio(color: Color, dotRadius: Dp) {
    val strokeWidth = RadioStrokeWidth.toPx()
    drawCircle(color, RadioRadius.toPx() - strokeWidth / 2, style = Stroke(strokeWidth))
    if (dotRadius > 0.dp) {
        drawCircle(color, dotRadius.toPx() - strokeWidth / 2, style = Fill)
    }
}

private val DotRadiusProp = DpPropKey()
private val ColorProp = ColorPropKey()
private const val RadioAnimationDuration = 100

private fun generateTransitionDefinition(selectedColor: Color, unselectedColor: Color) =
    transitionDefinition<Boolean> {
        state(false) {
            this[DotRadiusProp] = 0.dp
            this[ColorProp] = unselectedColor
        }
        state(true) {
            this[DotRadiusProp] = RadioButtonDotSize / 2
            this[ColorProp] = selectedColor
        }
        transition {
            ColorProp using tween(
                durationMillis = RadioAnimationDuration
            )
            DotRadiusProp using tween(
                durationMillis = RadioAnimationDuration
            )
        }
    }

private val RadioButtonRippleRadius = 24.dp
private val RadioButtonPadding = 2.dp
private val RadioButtonSize = 20.dp
private val RadioRadius = RadioButtonSize / 2
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
