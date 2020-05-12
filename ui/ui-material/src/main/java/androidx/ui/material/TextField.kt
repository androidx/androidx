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
import androidx.compose.Providers
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.stateFor
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.Ref
import androidx.ui.core.drawBehind
import androidx.ui.core.offset
import androidx.ui.core.tag
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.TextFieldValue
import androidx.ui.foundation.clickable
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.shape.corner.ZeroCornerSize
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.VisualTransformation
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.semantics.Semantics
import androidx.ui.text.FirstBaseline
import androidx.ui.text.LastBaseline
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.lerp
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max

/**
 * Material Design implementation of the
 * [Material Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 * A simple example looks like:
 *
 * @sample androidx.ui.material.samples.SimpleFilledTextFieldSample
 *
 * You may provide a placeholder:
 *
 * @sample androidx.ui.material.samples.FilledTextFieldWithPlaceholder
 *
 * You can also provide leading and trailing icons:
 *
 * @sample androidx.ui.material.samples.FilledTextFieldWithIcons
 *
 * To handle the error input state, use [isErrorValue] parameter:
 *
 * @sample androidx.ui.material.samples.FilledTextFieldWithErrorState
 *
 * Additionally, you may provide additional message at the bottom:
 *
 * @sample androidx.ui.material.samples.TextFieldWithHelperMessage
 *
 * Password text field example:
 *
 * @sample androidx.ui.material.samples.PasswordFilledTextField
 *
 * Hiding a software keyboard on IME action performed:
 *
 * @sample androidx.ui.material.samples.TextFieldWithHideKeyboardOnImeAction
 *
 * If apart from input text change you also want to observe the cursor location or selection range,
 * use a FilledTextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * If the boolean parameter value is `true`, it means the text field has a focus, and vice versa
 * @param isErrorValue indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param keyboardType the keyboard type to be used with the text field.
 * Note that the input type is not guaranteed. For example, an IME may send a non-ASCII character
 * even if you set the keyboard type to [KeyboardType.Ascii]
 * @param imeAction the IME action honored by the IME. The 'enter' key on the soft keyboard input
 * will show a corresponding icon. For example, search icon may be shown if [ImeAction.Search] is
 * selected. When a user taps on that 'enter' key, the [onImeActionPerformed] callback is called
 * with the specified [ImeAction]
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [imeAction] field. The callback also exposes a [SoftwareKeyboardController] instance as a
 * parameter that can be used to request to hide the software keyboard
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param onFocusChange a callback to be invoked when the text field gets or loses the focus
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of the input text or placeholder when the text field is in
 * focus, and the color of label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 * @param backgroundColor the background color of the text field's container. To the color provided
 * here there will be applied a transparency alpha defined by Material Design specifications
 * @param shape the shape of the text field's container
 */
@Composable
fun FilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onFocusChange: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error,
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
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChange = onFocusChange,
        onTextInputStarted = onTextInputStarted,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = backgroundColor,
        shape = shape
    )
}

/**
 * Material Design implementation of the
 * [Material Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 *
 * See example usage:
 * @sample androidx.ui.material.samples.FilledTextFieldSample
 *
 * If you only want to observe an input text change, use a FilledTextField overload with the
 * [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text,
 * selection or cursor. An updated [TextFieldValue] comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * If the boolean parameter value is `true`, it means the text field has a focus, and vice versa
 * @param isErrorValue indicates if the text field's current value is in error state. If set to
 * true, the label, bottom indicator and trailing icon will be displayed in [errorColor] color
 * @param keyboardType the keyboard type to be used with the text field.
 * Note that the input type is not guaranteed. For example, an IME may send a non-ASCII character
 * even if you set the keyboard type to [KeyboardType.Ascii]
 * @param imeAction the IME action honored by the IME. The 'enter' key on the soft keyboard input
 * will show a corresponding icon. For example, search icon may be shown if [ImeAction.Search] is
 * selected. When a user taps on that 'enter' key, the [onImeActionPerformed] callback is called
 * with the specified [ImeAction]
 * @param onImeActionPerformed is triggered when the input service performs an [ImeAction].
 * Note that the emitted IME action may be different from what you specified through the
 * [imeAction] field. The callback also exposes a [SoftwareKeyboardController] instance as a
 * parameter that can be used to request to hide the software keyboard
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
 * @param onFocusChange a callback to be invoked when the text field gets or loses the focus
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of the input text or placeholder when the text field is in
 * focus, and the color of label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 * @param backgroundColor the background color of the text field's container. To the color provided
 * here there will be applied a transparency alpha defined by Material Design specifications
 * @param shape the shape of the text field's container
 */
@Composable
fun FilledTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = currentTextStyle(),
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isErrorValue: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Unspecified,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit = { _, _ -> },
    onFocusChange: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error,
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
        leading = leadingIcon,
        trailing = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChange = onFocusChange,
        onTextInputStarted = onTextInputStarted,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
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
    label: @Composable () -> Unit,
    placeholder: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    isErrorValue: Boolean,
    visualTransformation: VisualTransformation,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeActionPerformed: (ImeAction, SoftwareKeyboardController?) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onTextInputStarted: (SoftwareKeyboardController) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    errorColor: Color,
    backgroundColor: Color,
    shape: Shape
) {
    val focusModifier = FocusModifier()
    val keyboardController: Ref<SoftwareKeyboardController> = remember { Ref() }

    val inputState = stateFor(value.text, focusModifier.focusState) {
        when {
            focusModifier.focusState == FocusState.Focused -> InputPhase.Focused
            value.text.isEmpty() -> InputPhase.UnfocusedEmpty
            else -> InputPhase.UnfocusedNotEmpty
        }
    }

    val decoratedPlaceholder: @Composable (() -> Unit)? =
        if (placeholder != null && inputState.value == InputPhase.Focused && value.text.isEmpty()) {
            {
                Decoration(
                    contentColor = inactiveColor,
                    typography = MaterialTheme.typography.subtitle1,
                    emphasis = EmphasisAmbient.current.medium,
                    children = placeholder
                )
            }
        } else null

    val decoratedTextField = @Composable { tagModifier: Modifier ->
        Decoration(
            contentColor = inactiveColor,
            typography = MaterialTheme.typography.subtitle1,
            emphasis = EmphasisAmbient.current.high
        ) {
            TextField(
                value = value,
                modifier = tagModifier + focusModifier,
                textStyle = textStyle,
                onValueChange = onValueChange,
                onFocusChange = onFocusChange,
                cursorColor = if (isErrorValue) errorColor else activeColor,
                visualTransformation = visualTransformation,
                keyboardType = keyboardType,
                imeAction = imeAction,
                onImeActionPerformed = {
                    onImeActionPerformed(it, keyboardController.value)
                },
                onTextInputStarted = {
                    keyboardController.value = it
                    onTextInputStarted(it)
                }
            )
        }
    }

    val textFieldModifier = Modifier.preferredSizeIn(
        minWidth = TextFieldMinWidth,
        minHeight = TextFieldMinHeight
    ) + modifier

    Semantics(container = true, mergeAllDescendants = true) {
        Surface(
            modifier = textFieldModifier,
            shape = shape,
            color = backgroundColor.applyAlpha(alpha = ContainerAlpha)
        ) {
            val emphasisLevels = EmphasisAmbient.current
            val emphasizedActiveColor = emphasisLevels.high.applyEmphasis(activeColor)
            val labelInactiveColor = emphasisLevels.medium.applyEmphasis(inactiveColor)
            val indicatorInactiveColor =
                inactiveColor.applyAlpha(alpha = IndicatorInactiveAlpha)

            TextFieldTransitionScope.transition(
                inputState = inputState.value,
                activeColor = emphasizedActiveColor,
                labelInactiveColor = labelInactiveColor,
                indicatorInactiveColor = indicatorInactiveColor
            ) { labelProgress, animatedLabelColor, indicatorWidth, indicatorColor ->
                // TODO(soboleva): figure out how this will play with the textStyle provided in label slot
                val labelAnimatedStyle = lerp(
                    MaterialTheme.typography.subtitle1,
                    MaterialTheme.typography.caption,
                    labelProgress
                )

                val leadingColor = inactiveColor.applyAlpha(alpha = TrailingLeadingAlpha)
                val trailingColor = if (isErrorValue) errorColor else leadingColor

                // text field with label and placeholder
                val labelColor = if (isErrorValue) errorColor else animatedLabelColor
                val decoratedLabel = @Composable {
                    Decoration(
                        contentColor = labelColor,
                        typography = labelAnimatedStyle,
                        children = label
                    )
                }

                // places leading icon, text field with label, trailing icon
                IconsTextFieldLayout(
                    modifier = Modifier
                        .clickable(indication = RippleIndication(bounded = false)) {
                            focusModifier.requestFocus()
                            keyboardController.value?.showSoftwareKeyboard()
                        }
                        .drawIndicatorLine(
                            lineWidth = indicatorWidth,
                            color = if (isErrorValue) errorColor else indicatorColor
                        ),
                    textField = {
                        TextFieldLayout(
                            animationProgress = labelProgress,
                            modifier = Modifier
                                .padding(
                                    start = HorizontalTextFieldPadding,
                                    end = HorizontalTextFieldPadding
                                ),
                            placeholder = decoratedPlaceholder,
                            label = decoratedLabel,
                            textField = decoratedTextField
                        )
                    },
                    leading = leading,
                    trailing = trailing,
                    leadingColor = leadingColor,
                    trailingColor = trailingColor
                )
            }
        }
    }
}

/**
 * Set alpha if the color is not translucent
 */
private fun Color.applyAlpha(alpha: Float): Color {
    return if (this.alpha != 1f) this else this.copy(alpha = alpha)
}

/**
 * Set content color, typography and emphasis for [children] composable
 */
@Composable
private fun Decoration(
    contentColor: Color,
    typography: TextStyle? = null,
    emphasis: Emphasis? = null,
    children: @Composable () -> Unit
) {
    val colorAndEmphasis = @Composable {
        Providers(ContentColorAmbient provides contentColor) {
            if (emphasis != null) ProvideEmphasis(emphasis, children) else children()
        }
    }
    if (typography != null) ProvideTextStyle(typography, colorAndEmphasis) else colorAndEmphasis()
}

/**
 * Layout of the text field, label and placeholder
 */
@Composable
private fun TextFieldLayout(
    animationProgress: Float,
    modifier: Modifier,
    placeholder: @Composable (() -> Unit)?,
    label: @Composable () -> Unit,
    textField: @Composable (Modifier) -> Unit
) {
    Layout(
        children = {
            if (placeholder != null) {
                Box(modifier = Modifier.tag(PlaceholderTag), children = placeholder)
            }
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

        val textFieldConstraints = constraints
            .offset(vertical = -LastBaselineOffset.toIntPx() - effectiveLabelBaseline)
            .copy(minHeight = IntPx.Zero)
        val textfieldPlaceable = measurables
            .first { it.tag == TextFieldTag }
            .measure(textFieldConstraints)
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
                    placeholderPlaceable,
                    labelEndPosition,
                    textfieldPositionY,
                    animationProgress
                )
            } else {
                placeTextfield(width, height, textfieldPlaceable, placeholderPlaceable)
            }
        }
    }
}

/**
 * Layout of the leading and trailing icons and the text field.
 * It differs from the Row as it does not lose the minHeight constraint which is needed to
 * correctly place the text field and label.
 * Should be revisited if b/154202249 is fixed so that Row could be used instead
 */
@Composable
private fun IconsTextFieldLayout(
    modifier: Modifier = Modifier,
    textField: @Composable () -> Unit,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color
) {
    Layout(
        children = {
            if (leading != null) {
                Box(Modifier.tag("leading").iconPadding(start = HorizontalIconPadding)) {
                    Decoration(contentColor = leadingColor, children = leading)
                }
            }
            if (trailing != null) {
                Box(Modifier.tag("trailing").iconPadding(end = HorizontalIconPadding)) {
                    Decoration(contentColor = trailingColor, children = trailing)
                }
            }
            textField()
        },
        modifier = modifier
    ) { measurables, incomingConstraints, _ ->
        val constraints = incomingConstraints.copy(minWidth = IntPx.Zero, minHeight = IntPx.Zero)
        var occupiedSpace = 0.ipx

        val leadingPlaceable = measurables.find { it.tag == "leading" }?.measure(constraints)
        occupiedSpace += leadingPlaceable?.width ?: 0.ipx

        val trailingPlaceable = measurables.find { it.tag == "trailing" }
            ?.measure(constraints.offset(horizontal = -occupiedSpace))
        occupiedSpace += trailingPlaceable?.width ?: 0.ipx

        val textFieldPlaceable = measurables.first {
            it.tag != "leading" && it.tag != "trailing"
        }.measure(incomingConstraints.offset(horizontal = -occupiedSpace))
        occupiedSpace += textFieldPlaceable.width

        val width = max(occupiedSpace, incomingConstraints.minWidth)
        val height = max(
            listOf(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable
            ).maxBy { it?.height ?: 0.ipx }?.height ?: 0.ipx,
            incomingConstraints.minHeight
        )
        layout(width, height) {
            leadingPlaceable?.place(
                0.ipx,
                Alignment.CenterVertically.align(height - leadingPlaceable.height)
            )
            textFieldPlaceable.place(
                leadingPlaceable?.width ?: 0.ipx,
                Alignment.CenterVertically.align(height - textFieldPlaceable.height)
            )
            trailingPlaceable?.place(
                width - trailingPlaceable.width,
                Alignment.CenterVertically.align(height - trailingPlaceable.height)
            )
        }
    }
}

private fun Modifier.iconPadding(start: Dp = 0.dp, end: Dp = 0.dp) =
    this + object : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
            layoutDirection: LayoutDirection
        ): MeasureScope.MeasureResult {
            val horizontal = start.toIntPx() + end.toIntPx()
            val placeable = measurable.measure(constraints.offset(-horizontal))
            val width = if (placeable.nonZero) {
                (placeable.width + horizontal).coerceIn(constraints.minWidth, constraints.maxWidth)
            } else {
                0.ipx
            }
            return layout(width, placeable.height) {
                placeable.place(start.toIntPx(), 0.ipx)
            }
        }
    }

private val Placeable.nonZero: Boolean get() = this.width != 0.ipx || this.height != 0.ipx

/**
 * A draw modifier that draws a bottom indicator line
 */
private fun Modifier.drawIndicatorLine(lineWidth: Dp, color: Color): Modifier {
    return drawBehind {
        val strokeWidth = lineWidth.value * density
        val y = size.height - strokeWidth / 2
        drawLine(
            color,
            Offset(0f, y),
            Offset(size.width, y),
            stroke = Stroke(width = strokeWidth)
        )
    }
}

/**
 * Places the provided text field, placeholder and label with respect to the baseline offsets
 */
private fun Placeable.PlacementScope.placeLabelAndTextfield(
    width: IntPx,
    height: IntPx,
    textfieldPlaceable: Placeable,
    labelPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
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
    placeholderPlaceable?.place(IntPx.Zero, textPosition)
}

/**
 * Places the provided text field and placeholder center vertically
 */
private fun Placeable.PlacementScope.placeTextfield(
    width: IntPx,
    height: IntPx,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?
) {
    val textCenterPosition = Alignment.CenterStart.align(
        IntPxSize(
            width - textPlaceable.width,
            height - textPlaceable.height
        )
    )
    textPlaceable.place(IntPx.Zero, textCenterPosition.y)
    placeholderPlaceable?.place(IntPx.Zero, textCenterPosition.y)
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
        children: @Composable (
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
private const val TrailingLeadingAlpha = 0.54f
private const val ContainerAlpha = 0.12f
private val TextFieldMinHeight = 56.dp
private val TextFieldMinWidth = 280.dp
private val FirstBaselineOffset = 20.dp
private val LastBaselineOffset = 16.dp
private val HorizontalTextFieldPadding = 16.dp
private val HorizontalIconPadding = 12.dp