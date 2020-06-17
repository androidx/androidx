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
import androidx.animation.transitionDefinition
import androidx.animation.tween
import androidx.annotation.VisibleForTesting
import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.Stable
import androidx.compose.StructurallyEqual
import androidx.compose.getValue
import androidx.compose.mutableStateOf
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.compose.stateFor
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.Ref
import androidx.ui.core.clipToBounds
import androidx.ui.core.constrainWidth
import androidx.ui.core.drawBehind
import androidx.ui.core.focus.FocusModifier
import androidx.ui.core.focus.FocusState
import androidx.ui.core.focus.focusState
import androidx.ui.core.id
import androidx.ui.core.layoutId
import androidx.ui.core.offset
import androidx.ui.core.semantics.semantics
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentColorAmbient
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.foundation.Text
import androidx.ui.foundation.TextField
import androidx.ui.foundation.clickable
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.ScrollableState
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.shape.corner.ZeroCornerSize
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Path
import androidx.ui.graphics.RectangleShape
import androidx.ui.graphics.Shape
import androidx.ui.graphics.drawscope.Stroke
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.TextFieldValue
import androidx.ui.input.VisualTransformation
import androidx.ui.layout.padding
import androidx.ui.layout.preferredSizeIn
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.savedinstancestate.Saver
import androidx.ui.savedinstancestate.rememberSavedInstanceState
import androidx.ui.text.InternalTextApi
import androidx.ui.text.LastBaseline
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.text.TextStyle
import androidx.ui.text.constrain
import androidx.ui.text.lerp
import androidx.ui.unit.Dp
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Material Design implementation of a
 * [Filled TextField](https://material.io/components/text-fields/#filled-text-field)
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
 * If apart from input text change you also want to observe the cursor location, selection range,
 * or IME composition use the FilledTextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
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
 * @param onFocusChange a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
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
        @OptIn(InternalTextApi::class)
        textFieldValue = TextFieldValue(
            text = value,
            selection = textFieldValue.selection.constrain(0, value.length)
        )
    }
    TextFieldImpl(
        type = TextFieldType.Filled,
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
 * Material Design implementation of a
 * [Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 * See example usage:
 * @sample androidx.ui.material.samples.FilledTextFieldSample
 *
 * This overload provides access to the input text, cursor position, selection range and
 * IME composition. If you only want to observe an input text change, use the FilledTextField
 * overload with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
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
 * @param onFocusChange a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
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
    TextFieldImpl(
        type = TextFieldType.Filled,
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

@Suppress("DEPRECATION")
@Composable
@Deprecated("Use the FilledTextField with androidx.ui.input.TextFieldValue instead.")
fun FilledTextField(
    value: androidx.ui.foundation.TextFieldValue,
    onValueChange: (androidx.ui.foundation.TextFieldValue) -> Unit,
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

    val fullModel = state { TextFieldValue() }
    if (fullModel.value.text != value.text || fullModel.value.selection != value.selection) {
        @OptIn(InternalTextApi::class)
        fullModel.value = TextFieldValue(
            text = value.text,
            selection = value.selection.constrain(0, value.text.length)
        )
    }

    val onValueChangeWrapper: (TextFieldValue) -> Unit = {
        val prevState = fullModel.value
        fullModel.value = it
        if (prevState.text != it.text || prevState.selection != it.selection) {
            onValueChange(
                androidx.ui.foundation.TextFieldValue(
                    it.text,
                    it.selection
                )
            )
        }
    }
    TextFieldImpl(
        type = TextFieldType.Filled,
        value = fullModel.value,
        onValueChange = onValueChangeWrapper,
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
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.ui.material.samples.SimpleOutlinedTextFieldSample
 *
 * If apart from input text change you also want to observe the cursor location, selection range,
 * or IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter
 * instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
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
 * @param onFocusChange a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
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
    errorColor: Color = MaterialTheme.colors.error
) {
    var textFieldValue by state { TextFieldValue() }
    if (textFieldValue.text != value) {
        @OptIn(InternalTextApi::class)
        textFieldValue = TextFieldValue(
            text = value,
            selection = textFieldValue.selection.constrain(0, value.length))
    }

    TextFieldImpl(
        type = TextFieldType.Outlined,
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
        backgroundColor = Color.Unset,
        shape = RectangleShape
    )
}

/**
 * Material Design implementation of an
 * [Outlined TextField](https://material.io/components/text-fields/#outlined-text-field)
 *
 * See example usage:
 * @sample androidx.ui.material.samples.OutlinedTextFieldSample
 *
 * This overload provides access to the input text, cursor position and selection range and
 * IME composition. If you only want to observe an input text change, use the OutlinedTextField
 * overload with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param label the label to be displayed inside the text field container. The default text style
 * for internal [Text] is [Typography.caption] when the text field is in focus and
 * [Typography.subtitle1] when the text field is not in focus
 * @param modifier a [Modifier] for this text field
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 * [currentTextStyle] defined by the theme
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
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
 * @param onFocusChange a callback to be invoked when the text field receives or loses focus
 * If the boolean parameter value is `true`, it means the text field has focus, and vice versa
 * @param onTextInputStarted a callback to be invoked when the connection with the platform's text
 * input service (e.g. software keyboard on Android) has been established. Called with the
 * [SoftwareKeyboardController] instance that can be used to request to show or hide the software
 * keyboard
 * @param activeColor the color of the label, bottom indicator and the cursor when the text field is
 * in focus
 * @param inactiveColor the color of either the input text or placeholder when the text field is in
 * focus, and the color of the label and bottom indicator when the text field is not in focus
 * @param errorColor the alternative color of the label, bottom indicator, cursor and trailing icon
 * used when [isErrorValue] is set to true
 */
@Composable
fun OutlinedTextField(
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
    errorColor: Color = MaterialTheme.colors.error
) {
    TextFieldImpl(
        type = TextFieldType.Outlined,
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
        backgroundColor = Color.Unset,
        shape = RectangleShape
    )
}

private enum class TextFieldType {
    Filled, Outlined
}

/**
 * Implementation of the [FilledTextField] and [OutlinedTextField]
 */
@Composable
private fun TextFieldImpl(
    type: TextFieldType,
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
            TextFieldScroller(
                scrollerPosition = rememberSavedInstanceState(
                    saver = TextFieldScrollerPosition.Saver
                ) {
                    TextFieldScrollerPosition()
                },
                modifier = tagModifier
            ) {
                TextField(
                    value = value,
                    modifier = focusModifier,
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
    }

    val textFieldModifier = modifier
        .semantics(mergeAllDescendants = true)
        .clickable(indication = RippleIndication(bounded = false)) {
            focusModifier.requestFocus()
            keyboardController.value?.showSoftwareKeyboard()
        }

    val emphasisLevels = EmphasisAmbient.current

    TextFieldTransitionScope.transition(
        inputState = inputState.value,
        activeColor = if (isErrorValue) {
            errorColor
        } else {
            emphasisLevels.high.applyEmphasis(activeColor)
        },
        labelInactiveColor = emphasisLevels.medium.applyEmphasis(inactiveColor),
        indicatorInactiveColor = inactiveColor.applyAlpha(alpha = IndicatorInactiveAlpha)
    ) { labelProgress, animatedLabelColor, indicatorWidth, animatedIndicatorColor ->

        val leadingColor = inactiveColor.applyAlpha(alpha = TrailingLeadingAlpha)
        val trailingColor = if (isErrorValue) errorColor else leadingColor

        val decoratedLabel = @Composable {
            val labelAnimatedStyle = lerp(
                MaterialTheme.typography.subtitle1,
                MaterialTheme.typography.caption,
                labelProgress
            )
            Decoration(
                contentColor = animatedLabelColor,
                typography = labelAnimatedStyle,
                children = label
            )
        }

        when (type) {
            TextFieldType.Filled -> {
                FilledTextFieldLayout(
                    textFieldModifier = Modifier
                        .preferredSizeIn(
                            minWidth = TextFieldMinWidth,
                            minHeight = TextFieldMinHeight
                        )
                        .plus(textFieldModifier),
                    decoratedTextField = decoratedTextField,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = leadingColor,
                    trailingColor = trailingColor,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor = animatedIndicatorColor,
                    backgroundColor = backgroundColor,
                    shape = shape
                )
            }
            TextFieldType.Outlined -> {
                OutlinedTextFieldLayout(
                    textFieldModifier = Modifier
                        .preferredSizeIn(
                            minWidth = TextFieldMinWidth,
                            minHeight = TextFieldMinHeight + OutlinedTextFieldTopPadding
                        )
                        .plus(textFieldModifier)
                        .padding(top = OutlinedTextFieldTopPadding),
                    decoratedTextField = decoratedTextField,
                    decoratedPlaceholder = decoratedPlaceholder,
                    decoratedLabel = decoratedLabel,
                    leading = leading,
                    trailing = trailing,
                    leadingColor = leadingColor,
                    trailingColor = trailingColor,
                    labelProgress = labelProgress,
                    indicatorWidth = indicatorWidth,
                    indicatorColor = animatedIndicatorColor,
                    focusModifier = focusModifier,
                    emptyInput = value.text.isEmpty()
                )
            }
        }
    }
}

@Composable
private fun FilledTextFieldLayout(
    textFieldModifier: Modifier = Modifier,
    decoratedTextField: @Composable (Modifier) -> Unit,
    decoratedPlaceholder: @Composable (() -> Unit)?,
    decoratedLabel: @Composable () -> Unit,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color,
    labelProgress: Float,
    indicatorWidth: Dp,
    indicatorColor: Color,
    backgroundColor: Color,
    shape: Shape
) {
    // places leading icon, text field with label and placeholder, trailing icon
    FilledTextField.IconsWithTextFieldLayout(
        modifier = textFieldModifier
            .drawBackground(
                color = backgroundColor.applyAlpha(alpha = ContainerAlpha),
                shape = shape
            )
            .drawIndicatorLine(
                lineWidth = indicatorWidth,
                color = indicatorColor
            ),
        textField = @Composable {
            // places input field, label, placeholder
            FilledTextField.TextFieldWithLabelLayout(
                animationProgress = labelProgress,
                modifier = Modifier
                    .padding(
                        start = TextFieldPadding,
                        end = TextFieldPadding
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

@Composable
private fun OutlinedTextFieldLayout(
    textFieldModifier: Modifier = Modifier,
    decoratedTextField: @Composable (Modifier) -> Unit,
    decoratedPlaceholder: @Composable (() -> Unit)?,
    decoratedLabel: @Composable () -> Unit,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    leadingColor: Color,
    trailingColor: Color,
    labelProgress: Float,
    indicatorWidth: Dp,
    indicatorColor: Color,
    focusModifier: FocusModifier,
    emptyInput: Boolean
) {
    val outlinedBorderParams = remember {
        OutlinedBorderParams(indicatorWidth, indicatorColor)
    }
    if (indicatorColor != outlinedBorderParams.color.value ||
        indicatorWidth != outlinedBorderParams.borderWidth.value
    ) {
        outlinedBorderParams.color.value = indicatorColor
        outlinedBorderParams.borderWidth.value = indicatorWidth
    }

    // places leading icon, input field, label, placeholder, trailing icon
    OutlinedTextField.IconsWithTextFieldLayout(
        modifier = textFieldModifier.drawOutlinedBorder(outlinedBorderParams),
        textField = decoratedTextField,
        leading = leading,
        trailing = trailing,
        leadingColor = leadingColor,
        trailingColor = trailingColor,
        onLabelMeasured = {
            val newLabelWidth = it * labelProgress

            val labelWidth = when {
                focusModifier.focusState == FocusState.Focused -> newLabelWidth
                !emptyInput -> newLabelWidth
                focusModifier.focusState == FocusState.NotFocused && emptyInput -> newLabelWidth
                else -> 0f
            }

            if (outlinedBorderParams.labelWidth.value != labelWidth) {
                outlinedBorderParams.labelWidth.value = labelWidth
            }
        },
        animationProgress = labelProgress,
        placeholder = decoratedPlaceholder,
        label = decoratedLabel
    )
}

/**
 * Similar to [androidx.ui.foundation.VerticalScroller] but does not lose the minWidth constraints.
 */
@VisibleForTesting
@Composable
internal fun TextFieldScroller(
    scrollerPosition: TextFieldScrollerPosition,
    modifier: Modifier = Modifier,
    textField: @Composable () -> Unit
) {
    Layout(
        modifier = modifier
            .clipToBounds()
            .scrollable(
                scrollableState = ScrollableState { delta ->
                    val newPosition = scrollerPosition.current + delta
                    val consumedDelta = when {
                        newPosition > scrollerPosition.maximum ->
                            scrollerPosition.maximum - scrollerPosition.current // too much down
                        newPosition < 0f -> -scrollerPosition.current // scrolled too much up
                        else -> delta
                    }
                    scrollerPosition.current += consumedDelta
                    consumedDelta
                },
                dragDirection = DragDirection.Vertical,
                enabled = scrollerPosition.maximum != 0f
            ),
        children = textField,
        measureBlock = { measurables, constraints ->
            val childConstraints = constraints.copy(maxHeight = Constraints.Infinity)
            val placeable = measurables.first().measure(childConstraints)
            val height = min(placeable.height, constraints.maxHeight)
            val diff = placeable.height.toFloat() - height.toFloat()
            layout(placeable.width, height) {
                // update current and maximum positions to correctly calculate delta in scrollable
                scrollerPosition.maximum = diff
                if (scrollerPosition.current > diff) scrollerPosition.current = diff

                val yOffset = scrollerPosition.current - diff
                placeable.place(0, yOffset.roundToInt())
            }
        }
    )
}

@VisibleForTesting
@Stable
internal class TextFieldScrollerPosition(private val initial: Float = 0f) {
    var current by mutableStateOf(initial, StructurallyEqual)
    var maximum by mutableStateOf(Float.POSITIVE_INFINITY, StructurallyEqual)

    companion object {
        val Saver = Saver<TextFieldScrollerPosition, Float>(
            save = { it.current },
            restore = { restored -> TextFieldScrollerPosition(initial = restored) }
        )
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

private object FilledTextField {
    /**
     * Layout of the text field, label and placeholder part in [FilledTextField]
     */
    @Composable
    fun TextFieldWithLabelLayout(
        animationProgress: Float,
        modifier: Modifier,
        placeholder: @Composable (() -> Unit)?,
        label: @Composable () -> Unit,
        textField: @Composable (Modifier) -> Unit
    ) {
        Layout(
            children = {
                if (placeholder != null) {
                    Box(modifier = Modifier.layoutId(PlaceholderId), children = placeholder)
                }
                Box(modifier = Modifier.layoutId(LabelId), children = label)
                textField(Modifier.layoutId(TextFieldId))
            },
            modifier = modifier
        ) { measurables, constraints ->
            val placeholderPlaceable =
                measurables.find { it.id == PlaceholderId }?.measure(constraints)

            val baseLineOffset = FirstBaselineOffset.toIntPx()

            val labelConstraints = constraints
                .offset(vertical = -LastBaselineOffset.toIntPx())
                .copy(minWidth = 0, minHeight = 0)
            val labelPlaceable = measurables.first { it.id == LabelId }.measure(labelConstraints)
            val labelBaseline = labelPlaceable[LastBaseline].let {
                if (it != AlignmentLine.Unspecified) it else labelPlaceable.height
            }
            val labelEndPosition = (baseLineOffset - labelBaseline).coerceAtLeast(0)
            val effectiveLabelBaseline = max(labelBaseline, baseLineOffset)

            val textFieldConstraints = constraints
                .offset(vertical = -LastBaselineOffset.toIntPx() - effectiveLabelBaseline)
                .copy(minHeight = 0)
            val textFieldPlaceable = measurables
                .first { it.id == TextFieldId }
                .measure(textFieldConstraints)
            val textFieldLastBaseline = textFieldPlaceable[LastBaseline]
            require(textFieldLastBaseline != AlignmentLine.Unspecified) { "No text last baseline." }

            val width = max(textFieldPlaceable.width, constraints.minWidth)
            val height = max(
                effectiveLabelBaseline + textFieldPlaceable.height + LastBaselineOffset.toIntPx(),
                constraints.minHeight
            )

            layout(width, height) {
                // Text field and label are placed with respect to the baseline offsets.
                // But if label is empty, then the text field should be centered vertically.
                if (labelPlaceable.width != 0) {
                    // only respects the offset from the last baseline to the bottom of the text field
                    val textfieldPositionY = height - LastBaselineOffset.toIntPx() -
                            min(textFieldLastBaseline, textFieldPlaceable.height)
                    placeLabelAndTextfield(
                        width,
                        height,
                        textFieldPlaceable,
                        labelPlaceable,
                        placeholderPlaceable,
                        labelEndPosition,
                        textfieldPositionY,
                        animationProgress
                    )
                } else {
                    placeTextfield(width, height, textFieldPlaceable, placeholderPlaceable)
                }
            }
        }
    }

    /**
     * Layout of the leading and trailing icons and the text field part in [FilledTextField].
     * It differs from the Row as it does not lose the minHeight constraint which is needed to
     * correctly place the text field and label.
     * Should be revisited if b/154202249 is fixed so that Row could be used instead
     */
    @Composable
    fun IconsWithTextFieldLayout(
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
                    Box(Modifier.layoutId("leading").iconPadding(start = HorizontalIconPadding)) {
                        Decoration(contentColor = leadingColor, children = leading)
                    }
                }
                if (trailing != null) {
                    Box(Modifier.layoutId("trailing").iconPadding(end = HorizontalIconPadding)) {
                        Decoration(contentColor = trailingColor, children = trailing)
                    }
                }
                textField()
            },
            modifier = modifier
        ) { measurables, incomingConstraints ->
            val constraints =
                incomingConstraints.copy(minWidth = 0, minHeight = 0)
            var occupiedSpace = 0

            val leadingPlaceable = measurables.find { it.id == "leading" }?.measure(constraints)
            occupiedSpace += widthOrZero(leadingPlaceable)

            val trailingPlaceable = measurables.find { it.id == "trailing" }
                ?.measure(constraints.offset(horizontal = -occupiedSpace))
            occupiedSpace += widthOrZero(trailingPlaceable)

            // represents the layout that holds textfield, label and placeholder
            val textFieldPlaceable = measurables.first {
                it.id != "leading" && it.id != "trailing"
            }.measure(incomingConstraints.offset(horizontal = -occupiedSpace))
            occupiedSpace += textFieldPlaceable.width

            val width = max(occupiedSpace, incomingConstraints.minWidth)
            val height = max(
                heightOrZero(
                    listOf(
                        leadingPlaceable,
                        trailingPlaceable,
                        textFieldPlaceable
                    ).maxBy { heightOrZero(it) }
                ),
                incomingConstraints.minHeight
            )
            layout(width, height) {
                leadingPlaceable?.place(
                    0,
                    Alignment.CenterVertically.align(height - leadingPlaceable.height)
                )
                textFieldPlaceable.place(
                    leadingPlaceable?.width ?: 0,
                    Alignment.CenterVertically.align(height - textFieldPlaceable.height)
                )
                trailingPlaceable?.place(
                    width - trailingPlaceable.width,
                    Alignment.CenterVertically.align(height - trailingPlaceable.height)
                )
            }
        }
    }

    /**
     * Places the provided text field, placeholder and label with respect to the baseline offsets in
     * [FilledTextField]
     */
    private fun Placeable.PlacementScope.placeLabelAndTextfield(
        width: Int,
        height: Int,
        textfieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        labelEndPosition: Int,
        textPosition: Int,
        animationProgress: Float
    ) {
        val labelCenterPosition = Alignment.CenterStart.align(
            IntSize(
                width - labelPlaceable.width,
                height - labelPlaceable.height
            )
        )
        val labelDistance = labelCenterPosition.y - labelEndPosition
        val labelPositionY =
            labelCenterPosition.y - (labelDistance * animationProgress).roundToInt()
        labelPlaceable.place(0, labelPositionY)

        textfieldPlaceable.place(0, textPosition)
        placeholderPlaceable?.place(0, textPosition)
    }

    /**
     * Places the provided text field and placeholder center vertically in [FilledTextField]
     */
    private fun Placeable.PlacementScope.placeTextfield(
        width: Int,
        height: Int,
        textPlaceable: Placeable,
        placeholderPlaceable: Placeable?
    ) {
        val textCenterPosition = Alignment.CenterStart.align(
            IntSize(
                width - textPlaceable.width,
                height - textPlaceable.height
            )
        )
        textPlaceable.place(0, textCenterPosition.y)
        placeholderPlaceable?.place(0, textCenterPosition.y)
    }
}

private object OutlinedTextField {
    /**
     * Layout of the leading and trailing icons and the text field, label and placeholder in
     * [OutlinedTextField].
     * It doesn't use Row to position the icons and middle part because label should not be
     * positioned in the middle part.
    \ */
    @Composable
    fun IconsWithTextFieldLayout(
        modifier: Modifier = Modifier,
        textField: @Composable (Modifier) -> Unit,
        placeholder: @Composable (() -> Unit)?,
        label: @Composable () -> Unit,
        leading: @Composable (() -> Unit)?,
        trailing: @Composable (() -> Unit)?,
        leadingColor: Color,
        trailingColor: Color,
        animationProgress: Float,
        onLabelMeasured: (Int) -> Unit
    ) {
        Layout(
            children = {
                if (leading != null) {
                    Box(Modifier.layoutId("leading").iconPadding(start = HorizontalIconPadding)) {
                        Decoration(contentColor = leadingColor, children = leading)
                    }
                }
                if (trailing != null) {
                    Box(Modifier.layoutId("trailing").iconPadding(end = HorizontalIconPadding)) {
                        Decoration(contentColor = trailingColor, children = trailing)
                    }
                }
                if (placeholder != null) {
                    Box(
                        modifier = Modifier
                            .layoutId(PlaceholderId)
                            .padding(horizontal = TextFieldPadding),
                        children = placeholder
                    )
                }

                textField(
                    Modifier
                        .layoutId(TextFieldId)
                        .padding(horizontal = TextFieldPadding)
                )

                Box(modifier = Modifier.layoutId(LabelId), children = label)
            },
            modifier = modifier
        ) { measurables, incomingConstraints ->
            // used to calculate the constraints for measuring elements that will be placed in a row
            var occupiedSpaceHorizontally = 0
            val bottomPadding = TextFieldPadding.toIntPx()

            // measure leading icon
            val constraints =
                incomingConstraints.copy(minWidth = 0, minHeight = 0)
            val leadingPlaceable = measurables.find { it.id == "leading" }?.measure(constraints)
            occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)

            // measure trailing icon
            val trailingPlaceable = measurables.find { it.id == "trailing" }
                ?.measure(constraints.offset(horizontal = -occupiedSpaceHorizontally))
            occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)

            // measure label
            val labelConstraints = constraints.offset(
                horizontal = -occupiedSpaceHorizontally,
                vertical = -bottomPadding
            )
            val labelPlaceable =
                measurables.first { it.id == LabelId }.measure(labelConstraints)
            onLabelMeasured(labelPlaceable.width)

            // measure text field
            // on top we offset either by default padding or by label's half height if its too big
            // minWidth must not be set to 0 due to how foundation TextField treats zero minWidth
            val topPadding = max(labelPlaceable.height / 2, bottomPadding)
            val textContraints = incomingConstraints.offset(
                horizontal = -occupiedSpaceHorizontally,
                vertical = -bottomPadding - topPadding
            ).copy(minHeight = 0)
            val textFieldPlaceable =
                measurables.first { it.id == TextFieldId }.measure(textContraints)

            // measure placeholder
            val placeholderConstraints = textContraints.copy(minWidth = 0)
            val placeholderPlaceable =
                measurables.find { it.id == PlaceholderId }?.measure(placeholderConstraints)

            val width = calculateWidth(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints
            )
            val height = calculateHeight(
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                incomingConstraints,
                density
            )
            layout(width, height) {
                place(
                    height,
                    width,
                    leadingPlaceable,
                    trailingPlaceable,
                    textFieldPlaceable,
                    labelPlaceable,
                    placeholderPlaceable,
                    animationProgress,
                    density
                )
            }
        }
    }

    /**
     * Calculate the width of the [OutlinedTextField] given all elements that should be
     * placed inside
     */
    private fun calculateWidth(
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        textFieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        constraints: Constraints
    ): Int {
        val middleSection = widthOrZero(
            listOf(
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable
            ).maxBy { widthOrZero(it) }
        )
        val wrappedWidth =
            widthOrZero(leadingPlaceable) + middleSection + widthOrZero(trailingPlaceable)
        return max(wrappedWidth, constraints.minWidth)
    }

    /**
     * Calculate the height of the [OutlinedTextField] given all elements that should be
     * placed inside
     */
    private fun calculateHeight(
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        textFieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        constraints: Constraints,
        density: Float
    ): Int {
        // middle section is defined as a height of the text field or placeholder ( whichever is
        // taller) plus 16.dp or half height of the label if it is taller, given that the label
        // is vertically centered to the top edge of the resulting text field's container
        val inputFieldHeight = max(textFieldPlaceable.height, heightOrZero(placeholderPlaceable))
        val topBottomPadding = TextFieldPadding.value * density
        val middleSectionHeight = inputFieldHeight + topBottomPadding + max(
            topBottomPadding,
            labelPlaceable.height / 2f
        )
        return max(
            listOf(
                heightOrZero(leadingPlaceable),
                heightOrZero(trailingPlaceable),
                middleSectionHeight.roundToInt()
            ).max() ?: 0,
            constraints.minHeight
        )
    }

    /**
     * Places the provided text field, placeholder, label, optional leading and trailing icons inside
     * the [OutlinedTextField]
     */
    private fun Placeable.PlacementScope.place(
        height: Int,
        width: Int,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        textFieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        animationProgress: Float,
        density: Float
    ) {
        // placed center vertically and to the start edge horizontally
        leadingPlaceable?.place(
            0,
            Alignment.CenterVertically.align(height - leadingPlaceable.height)
        )

        // placed center vertically and to the end edge horizontally
        trailingPlaceable?.place(
            width - trailingPlaceable.width,
            Alignment.CenterVertically.align(height - trailingPlaceable.height)
        )

        // if animation progress is 0, the label will be centered vertically
        // if animation progress is 1, vertically it will be centered to the container's top edge
        // horizontally it is placed after the leading icon
        val labelPositionY =
            Alignment.CenterVertically.align(height - labelPlaceable.height) * (1 -
                    animationProgress) - (labelPlaceable.height / 2) * animationProgress
        val labelPositionX = (TextFieldPadding.value * density) +
                widthOrZero(leadingPlaceable) * (1 - animationProgress)
        labelPlaceable.place(labelPositionX.roundToInt(), labelPositionY.roundToInt())

        // placed center vertically and after the leading icon horizontally
        textFieldPlaceable.place(
            widthOrZero(leadingPlaceable),
            Alignment.CenterVertically.align(height - textFieldPlaceable.height)
        )

        // placed center vertically and after the leading icon horizontally
        placeholderPlaceable?.place(
            widthOrZero(leadingPlaceable),
            Alignment.CenterVertically.align(height - placeholderPlaceable.height)
        )
    }
}

private val Placeable.nonZero: Boolean get() = this.width != 0 || this.height != 0
private fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0
private fun heightOrZero(placeable: Placeable?) = placeable?.height ?: 0

/**
 * A modifier that applies padding only if the size of the element is not zero
 */
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
                constraints.constrainWidth(placeable.width + horizontal)
            } else {
                0
            }
            return layout(width, placeable.height) {
                placeable.place(start.toIntPx(), 0)
            }
        }
    }

/**
 * A draw modifier that draws a bottom indicator line in [FilledTextField]
 */
private fun Modifier.drawIndicatorLine(lineWidth: Dp, color: Color): Modifier {
    return drawBehind {
        val strokeWidth = lineWidth.value * density
        val y = size.height - strokeWidth / 2
        drawLine(
            color,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth
        )
    }
}

/**
 * A draw modifier to draw a border line in [OutlinedTextField]
 */
private fun Modifier.drawOutlinedBorder(
    borderParams: OutlinedBorderParams
): Modifier = drawBehind {
    val padding = TextFieldPadding.value * density
    val innerPadding = OutlinedTextFieldInnerPadding.value * density

    val lineWidth = borderParams.borderWidth.value.value * density
    val width: Float = size.width
    val height: Float = size.height

    val radius = borderParams.cornerRadius.value * density
    val dx = if (radius > width / 2) width / 2 else radius
    val dy = if (radius > height / 2) height / 2 else radius

    val path = Path().apply {
        // width and height minus corners and line width
        val effectiveWidth: Float = width - 2 * dx - lineWidth
        val effectiveHeight: Float = height - 2 * dy - lineWidth

        // top-right corner
        moveTo(width - lineWidth / 2, dy + lineWidth / 2)
        relativeQuadraticBezierTo(0f, -dy, -dx, -dy)

        // top line with gap
        val diff = borderParams.labelWidth.value
        if (diff == 0f) {
            relativeLineTo(-effectiveWidth, 0f)
        } else {
            val effectivePadding = padding - innerPadding - dx - lineWidth / 2
            val gap = diff + 2 * innerPadding
            if (layoutDirection == LayoutDirection.Ltr) {
                relativeLineTo(-effectiveWidth + effectivePadding + gap, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectivePadding, 0f)
            } else {
                relativeLineTo(-effectivePadding, 0f)
                relativeMoveTo(-gap, 0f)
                relativeLineTo(-effectiveWidth + gap + effectivePadding, 0f)
            }
        }

        // top-left corner and left line
        relativeQuadraticBezierTo(-dx, 0f, -dx, dy)
        relativeLineTo(0f, effectiveHeight)

        // bottom-left corner and bottom line
        relativeQuadraticBezierTo(0f, dy, dx, dy)
        relativeLineTo(effectiveWidth, 0f)

        // bottom-right corner and right line
        relativeQuadraticBezierTo(dx, 0f, dx, -dy)
        relativeLineTo(0f, -effectiveHeight)
    }

    drawPath(
        path = path,
        color = borderParams.color.value,
        style = Stroke(width = lineWidth)
    )
}

/**
 * A data class that stores parameters needed for [drawOutlinedBorder] modifier
 */
@Stable
private class OutlinedBorderParams(
    initialBorderWidth: Dp,
    initialColor: Color
) {
    val borderWidth = mutableStateOf(initialBorderWidth)
    val color = mutableStateOf(initialColor)
    val cornerRadius = OutlinedTextFieldCornerRadius
    val labelWidth = mutableStateOf(0f)
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
        IndicatorColorProp using tween(durationMillis = AnimationDuration)
        IndicatorWidthProp using tween(durationMillis = AnimationDuration)
    }

    private fun TransitionSpec<InputPhase>.labelTransition() {
        LabelColorProp using tween(durationMillis = AnimationDuration)
        LabelProgressProp using tween(durationMillis = AnimationDuration)
    }
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

private const val TextFieldId = "TextField"
private const val PlaceholderId = "Hint"
private const val LabelId = "Label"

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
private val TextFieldPadding = 16.dp
private val HorizontalIconPadding = 12.dp
private val OutlinedTextFieldInnerPadding = 4.dp

// TODO(b/158077409) support shape in OutlinedTextField
private val OutlinedTextFieldCornerRadius = 4.dp

/*
This padding is used to allow label not overlap with the content above it. This 8.dp will work
for default cases when developers do not override the label's font size. If they do, they will
need to add additional padding themselves
*/
private val OutlinedTextFieldTopPadding = 8.dp