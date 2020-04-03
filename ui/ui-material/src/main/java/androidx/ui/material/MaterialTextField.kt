/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.animation.FloatPropKey
import androidx.animation.TransitionSpec
import androidx.animation.TweenBuilder
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.stateFor
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Alignment
import androidx.ui.core.FirstBaseline
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.LastBaseline
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.drawBehind
import androidx.ui.core.offset
import androidx.ui.core.tag
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.foundation.ProvideContentColor
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.shape.corner.ZeroCornerSize
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Shape
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.material.ripple.ripple
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.lerp
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.max
import java.util.UUID

/**
 * Material Design implementation of the
 * [Material Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * If you want to observe the cursor location or selection range, use a FilledTextField override
 * with the [TextFieldValue] parameter instead
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by a theme.
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when text field is not in focus.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1].
 * @param onFocusChange the callback triggered when the text field gets or loses the focus.
 * If the boolean parameter value is `true`, it means the text field has a focus, and vice versa.
 * @param activeColor the color of the label and bottom indicator when the text field is in focus
 * @param inactiveColor the color of the input text or placeholder when the text field is in
 * focus, and the color of label and bottom indicator when the text field is not in focus
 * @param backgroundColor the background color of the text field's container. To a color provided
 * here there will be applied a transparency alpha defined by Material Design specifications
 * @param shape the shape of the text field's container
 */
@Composable
fun FilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    label: @Composable() () -> Unit,
    placeholder: @Composable() () -> Unit = emptyContent(),
    onFocusChange: (Boolean) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    backgroundColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomLeft = ZeroCornerSize, bottomRight = ZeroCornerSize)
) {
    var textFieldValue by state { TextFieldValue() }
    if (textFieldValue.text != value) {
        val newSelection = TextRange(
            textFieldValue.selection.start.coerceIn(0, value.length),
            textFieldValue.selection.end.coerceIn(0, value.length)
        )
        textFieldValue = TextFieldValue(text = value, selection = newSelection)
    }
    FilledTextFieldImpl(
        value = textFieldValue,
        onValueChange = {
            val previousValue = textFieldValue.text
            textFieldValue = it
            if (previousValue != it.text) {
                onValueChange(it.text)
            }
        },
        modifier = modifier,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        onFocusChange = onFocusChange,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        backgroundColor = backgroundColor,
        shape = shape
    )
}

/**
 * Material Design implementation of the
 * [Material Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text,
 * selection or cursor. An updated [TextFieldValue] comes as a parameter of the callback
 * If you only want to observe the text change, use a [FilledTextField] override with the String
 * parameter instead
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by a theme.
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when text field is not in focus.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1].
 * @param onFocusChange the callback triggered when the text field gets or loses the focus.
 * If the boolean parameter value is `true`, it means the text field has a focus, and vice versa.
 * @param activeColor the color of the label and bottom indicator when the text field is in focus
 * @param inactiveColor the color of the input text or placeholder when the text field is in
 * focus, and the color of label and bottom indicator when the text field is not in focus
 * @param backgroundColor the background color of the text field's container. To a color provided
 * here there will be applied a transparency alpha defined by Material Design specifications
 * @param shape the shape of the text field's container
 */
@Composable
fun FilledTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    label: @Composable() () -> Unit,
    placeholder: @Composable() () -> Unit = emptyContent(),
    onFocusChange: (Boolean) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    backgroundColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomLeft = ZeroCornerSize, bottomRight = ZeroCornerSize)
) {
    FilledTextFieldImpl(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        onFocusChange = onFocusChange,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        backgroundColor = backgroundColor,
        shape = shape
    )
}

/**
 * Implementation of the [FilledTextField]
 */
@Composable
private fun FilledTextFieldImpl(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    label: @Composable() () -> Unit,
    placeholder: @Composable() () -> Unit,
    // TODO (b/152968058) finalise this API
    onFocusChange: (Boolean) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    backgroundColor: Color,
    shape: Shape
) {
    val focusIdentifier = remember { UUID.randomUUID().toString() }
    var shouldFocus by state { false }
    var focused by state { false }
    val inputState = stateFor(value.text, focused) {
        when {
            focused -> InputPhase.Focused
            value.text.isEmpty() -> InputPhase.UnfocusedEmpty
            else -> InputPhase.UnfocusedNotEmpty
        }
    }

    val decoratedPlaceholder = @Composable {
        if (inputState.value == InputPhase.Focused && value.text.isEmpty()) {
            Decoration(
                contentColor = inactiveColor,
                typography = MaterialTheme.typography.subtitle1,
                emphasis = EmphasisAmbient.current.medium,
                children = placeholder
            )
        }
    }
    val decoratedTextField = @Composable { tagModifier: Modifier ->
        Decoration(
            contentColor = inactiveColor,
            typography = MaterialTheme.typography.subtitle1,
            emphasis = EmphasisAmbient.current.high
        ) {
            TextField(
                value = value,
                modifier = tagModifier,
                textStyle = textStyle,
                onValueChange = onValueChange,
                onFocus = {
                    focused = true
                    onFocusChange(true)
                },
                onBlur = {
                    focused = false
                    onFocusChange(false)
                },
                focusIdentifier = focusIdentifier
            )
        }
    }

    val textFieldModifier = Modifier.preferredSizeIn(
        minWidth = TextFieldMinWidth,
        minHeight = TextFieldMinHeight
    ) + modifier

    Surface(
        modifier = textFieldModifier,
        shape = shape,
        color = backgroundColor.copy(alpha = ContainerAlpha)
    ) {
        Clickable(onClick = { shouldFocus = true }, modifier = Modifier.ripple(false)) {
            if (shouldFocus) {
                FocusManagerAmbient.current.requestFocusById(focusIdentifier)
                shouldFocus = false
            }

            val emphasisLevels = EmphasisAmbient.current
            val emphasizedActiveColor = emphasisLevels.high.emphasize(activeColor)
            val labelInactiveColor = emphasisLevels.medium.emphasize(inactiveColor)
            val indicatorInactiveColor = inactiveColor.copy(alpha = IndicatorInactiveAlpha)

            TextFieldTransitionScope.transition(
                inputState = inputState.value,
                activeColor = emphasizedActiveColor,
                labelInactiveColor = labelInactiveColor,
                indicatorInactiveColor = indicatorInactiveColor
            ) { labelProgress, labelColor, indicatorWidth, indicatorColor ->
                // TODO(soboleva): figure out how this will play with the textStyle provided in label slot
                val labelAnimatedStyle = lerp(
                    MaterialTheme.typography.subtitle1,
                    MaterialTheme.typography.caption,
                    labelProgress
                )
                val decoratedLabel = @Composable {
                    Decoration(
                        contentColor = labelColor,
                        typography = labelAnimatedStyle,
                        children = label
                    )
                }
                val paddingAndIndicator = Modifier
                    .drawIndicatorLine(indicatorWidth, indicatorColor)
                    .padding(start = TextHorizontalPadding, end = TextHorizontalPadding)
                TextFieldLayout(
                    animationProgress = labelProgress,
                    modifier = paddingAndIndicator,
                    placeholder = decoratedPlaceholder,
                    label = decoratedLabel,
                    textField = decoratedTextField
                )
            }
        }
    }
}

/**
 * Sets content color, typography and emphasis for [children] composable
 */
@Composable
private fun Decoration(
    contentColor: Color,
    typography: TextStyle,
    emphasis: Emphasis? = null,
    children: @Composable() () -> Unit
) {
    ProvideTextStyle(typography) {
        ProvideContentColor(contentColor) {
            if (emphasis != null) {
                ProvideEmphasis(emphasis, children)
            } else {
                children()
            }
        }
    }
}

/**
 * Layout of the text field, label and placeholder
 */
@Composable
private fun TextFieldLayout(
    animationProgress: Float,
    modifier: Modifier,
    placeholder: @Composable() () -> Unit,
    label: @Composable() () -> Unit,
    textField: @Composable() (Modifier) -> Unit
) {
    Layout(
        children = {
            Box(modifier = Modifier.tag(PlaceholderTag), children = placeholder)
            Box(modifier = Modifier.tag(LabelTag), children = label)
            textField(Modifier.tag(TextFieldTag))
        },
        modifier = modifier
    ) { measurables, constraints, _ ->
        val placeholderPlaceable =
            measurables.find { it.tag == PlaceholderTag }?.measure(constraints)

        val baseLineOffset = FirstBaselineOffset.toIntPx()

        val labelConstraints = constraints
            .offset(vertical = -LastBaselineOffset.toIntPx())
            .copy(minWidth = IntPx.Zero, minHeight = IntPx.Zero)
        val labelPlaceable = measurables.first { it.tag == LabelTag }.measure(labelConstraints)
        val labelBaseline = labelPlaceable[LastBaseline] ?: labelPlaceable.height
        val labelEndPosition = (baseLineOffset - labelBaseline).coerceAtLeast(IntPx.Zero)
        val effectiveLabelBaseline = max(labelBaseline, baseLineOffset)

        val textfieldPlaceable = measurables
            .first { it.tag == TextFieldTag }
            .measure(labelConstraints.offset(vertical = -effectiveLabelBaseline))
        val textfieldFirstBaseline = requireNotNull(textfieldPlaceable[FirstBaseline]) {
            "No text first baseline."
        }
        val textfieldLastBaseline = requireNotNull(textfieldPlaceable[LastBaseline]) {
            "No text last baseline."
        }
        val textfieldPositionY = effectiveLabelBaseline + baseLineOffset - textfieldFirstBaseline

        val width = max(textfieldPlaceable.width, constraints.minWidth)
        val height = max(
            textfieldPositionY + textfieldLastBaseline + LastBaselineOffset.toIntPx(),
            constraints.minHeight
        )

        layout(width, height) {
            // Text field and label are placed with respect to the baseline offsets.
            // But if label is empty, then the text field should be centered vertically.
            if (labelPlaceable.width != IntPx.Zero) {
                placeLabelAndTextfield(
                    width,
                    height,
                    textfieldPlaceable,
                    labelPlaceable,
                    labelEndPosition,
                    textfieldPositionY,
                    animationProgress
                )
            } else {
                placeTextfield(width, height, textfieldPlaceable)
            }
            placeholderPlaceable?.place(IntPx.Zero, textfieldPositionY)
        }
    }
}

/**
 * A draw modifier that draws a bottom indicator line
 */
@Composable
private fun Modifier.drawIndicatorLine(lineWidth: Dp, color: Color): Modifier {
    val paint = remember { Paint() }
    return drawBehind {
        val strokeWidth = lineWidth.value * density
        paint.strokeWidth = strokeWidth
        paint.color = color
        val y = size.height.value - strokeWidth / 2
        drawLine(
            Offset(0f, y),
            Offset(size.width.value, y),
            paint
        )
    }
}

/**
 * Places a text field and a label with respect to the baseline offsets
 */
private fun Placeable.PlacementScope.placeLabelAndTextfield(
    width: IntPx,
    height: IntPx,
    textfieldPlaceable: Placeable,
    labelPlaceable: Placeable,
    labelEndPosition: IntPx,
    textPosition: IntPx,
    animationProgress: Float
) {
    val labelCenterPosition = Alignment.CenterStart.align(
        IntPxSize(
            width - labelPlaceable.width,
            height - labelPlaceable.height
        )
    )
    val labelDistance = labelCenterPosition.y - labelEndPosition
    val labelPositionY =
        labelCenterPosition.y - labelDistance * animationProgress
    labelPlaceable.place(IntPx.Zero, labelPositionY)

    textfieldPlaceable.place(IntPx.Zero, textPosition)
}

/**
 * Places a text field center vertically
 */
private fun Placeable.PlacementScope.placeTextfield(
    width: IntPx,
    height: IntPx,
    textPlaceable: Placeable
) {
    val textCenterPosition = Alignment.CenterStart.align(
        IntPxSize(
            width - textPlaceable.width,
            height - textPlaceable.height
        )
    )
    textPlaceable.place(IntPx.Zero, textCenterPosition.y)
}

private object TextFieldTransitionScope {
    private val LabelColorProp = ColorPropKey()
    private val LabelProgressProp = FloatPropKey()
    private val IndicatorColorProp = ColorPropKey()
    private val IndicatorWidthProp = DpPropKey()

    @Composable
    fun transition(
        inputState: InputPhase,
        activeColor: Color,
        labelInactiveColor: Color,
        indicatorInactiveColor: Color,
        children: @Composable() (
            labelProgress: Float,
            labelColor: Color,
            indicatorWidth: Dp,
            indicatorColor: Color
        ) -> Unit
    ) {
        val definition = remember(activeColor, labelInactiveColor, indicatorInactiveColor) {
            generateLabelTransitionDefinition(
                activeColor,
                labelInactiveColor,
                indicatorInactiveColor
            )
        }
        Transition(definition = definition, toState = inputState) { state ->
            children(
                state[LabelProgressProp],
                state[LabelColorProp],
                state[IndicatorWidthProp],
                state[IndicatorColorProp]
            )
        }
    }

    private fun generateLabelTransitionDefinition(
        activeColor: Color,
        labelInactiveColor: Color,
        indicatorInactiveColor: Color
    ) = transitionDefinition {
        state(InputPhase.Focused) {
            this[LabelColorProp] = activeColor
            this[IndicatorColorProp] = activeColor
            this[LabelProgressProp] = 1f
            this[IndicatorWidthProp] = IndicatorFocusedWidth
        }
        state(InputPhase.UnfocusedEmpty) {
            this[LabelColorProp] = labelInactiveColor
            this[IndicatorColorProp] = indicatorInactiveColor
            this[LabelProgressProp] = 0f
            this[IndicatorWidthProp] = IndicatorUnfocusedWidth
        }
        state(InputPhase.UnfocusedNotEmpty) {
            this[LabelColorProp] = labelInactiveColor
            this[IndicatorColorProp] = indicatorInactiveColor
            this[LabelProgressProp] = 1f
            this[IndicatorWidthProp] = 1.dp
        }

        transition(fromState = InputPhase.Focused, toState = InputPhase.UnfocusedEmpty) {
            labelTransition()
            indicatorTransition()
        }
        transition(fromState = InputPhase.Focused, toState = InputPhase.UnfocusedNotEmpty) {
            indicatorTransition()
        }
        transition(fromState = InputPhase.UnfocusedNotEmpty, toState = InputPhase.Focused) {
            indicatorTransition()
        }
        transition(fromState = InputPhase.UnfocusedEmpty, toState = InputPhase.Focused) {
            labelTransition()
            indicatorTransition()
        }
        // below states are needed to support case when a single state is used to control multiple
        // text fields.
        transition(fromState = InputPhase.UnfocusedNotEmpty, toState = InputPhase.UnfocusedEmpty) {
            labelTransition()
        }
        transition(fromState = InputPhase.UnfocusedEmpty, toState = InputPhase.UnfocusedNotEmpty) {
            labelTransition()
        }
    }

    private fun TransitionSpec<InputPhase>.indicatorTransition() {
        IndicatorColorProp using tweenAnimation()
        IndicatorWidthProp using tweenAnimation()
    }
    private fun TransitionSpec<InputPhase>.labelTransition() {
        LabelColorProp using tweenAnimation()
        LabelProgressProp using tweenAnimation()
    }
    private fun <T> tweenAnimation() = TweenBuilder<T>().apply { duration = AnimationDuration }
}

/**
 * An internal state used to animate a label and an indicator.
 */
private enum class InputPhase {
    // Text field is focused
    Focused,
    // Text field is not focused and input text is empty
    UnfocusedEmpty,
    // Text field is not focused but input text is not empty
    UnfocusedNotEmpty
}

private const val TextFieldTag = "TextField"
private const val PlaceholderTag = "Hint"
private const val LabelTag = "Label"

private const val AnimationDuration = 150
private val IndicatorUnfocusedWidth = 1.dp
private val IndicatorFocusedWidth = 2.dp
private const val IndicatorInactiveAlpha = 0.42f
private const val ContainerAlpha = 0.12f
private val TextFieldMinHeight = 56.dp
private val TextFieldMinWidth = 280.dp
private val FirstBaselineOffset = 20.dp
private val TextHorizontalPadding = 16.dp
private val LastBaselineOffset = 16.dp