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

import androidx.compose.Composable
import androidx.compose.getValue
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.drawBehind
import androidx.ui.core.id
import androidx.ui.core.layoutId
import androidx.ui.core.offset
import androidx.compose.foundation.Box
import androidx.compose.foundation.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.currentTextStyle
import androidx.compose.foundation.shape.corner.ZeroCornerSize
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import androidx.ui.input.TextFieldValue
import androidx.ui.input.VisualTransformation
import androidx.ui.layout.padding
import androidx.ui.text.InternalTextApi
import androidx.ui.text.LastBaseline
import androidx.ui.text.SoftwareKeyboardController
import androidx.ui.text.TextStyle
import androidx.ui.text.constrain
import androidx.ui.unit.Dp
import androidx.ui.unit.IntSize
import androidx.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Material Design implementation of a
 * [Filled TextField](https://material.io/components/text-fields/#filled-text-field)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
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
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
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
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
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
fun TextField(
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
    onFocusChanged: (Boolean) -> Unit = {},
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
        onFocusChanged = onFocusChanged,
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
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
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
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
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
@Deprecated(
    "FilledTextField has been renamed to TextField for better discoverability. Use " +
            "TextField instead", replaceWith = ReplaceWith(
        "TextField(" +
                "value = value," +
                " onValueChange = onValueChange," +
                " label = label," +
                " modifier = modifier, " +
                "textStyle = textStyle," +
                " placeholder = placeholder," +
                " leadingIcon = leadingIcon," +
                " trailingIcon = trailingIcon," +
                " isErrorValue = isErrorValue," +
                " visualTransformation = visualTransformation, keyboardType = keyboardType," +
                " imeAction = imeAction," +
                " onImeActionPerformed = onImeActionPerformed," +
                " onFocusChange = onFocusChange," +
                " onTextInputStarted = onTextInputStarted," +
                " activeColor = activeColor," +
                " inactiveColor = inactiveColor," +
                " errorColor = errorColor," +
                " backgroundColor = backgroundColor," +
                " shape = shape)",
        "androidx.ui.material.TextField"
    )
)
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
    onFocusChanged: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error,
    backgroundColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomLeft = ZeroCornerSize, bottomRight = ZeroCornerSize)
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChanged = onFocusChanged,
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
 * If you are looking for an outlined version, see [OutlinedTextField].
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
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
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
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
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
fun TextField(
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
    onFocusChanged: (Boolean) -> Unit = {},
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
        onFocusChanged = onFocusChanged,
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
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use [androidx.ui.input.PasswordVisualTransformation] to create a password
 * text field. By default no visual transformation is applied
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
 * @param onFocusChanged a callback to be invoked when the text field receives or loses focus
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
@Deprecated(
    "FilledTextField has been renamed to TextField for better discoverability. Use " +
            "TextField instead", replaceWith = ReplaceWith(
        "TextField(" +
                "value = value," +
                " onValueChange = onValueChange," +
                " label = label," +
                " modifier = modifier, " +
                "textStyle = textStyle," +
                " placeholder = placeholder," +
                " leadingIcon = leadingIcon," +
                " trailingIcon = trailingIcon," +
                " isErrorValue = isErrorValue," +
                " visualTransformation = visualTransformation, keyboardType = keyboardType," +
                " imeAction = imeAction," +
                " onImeActionPerformed = onImeActionPerformed," +
                " onFocusChange = onFocusChange," +
                " onTextInputStarted = onTextInputStarted," +
                " activeColor = activeColor," +
                " inactiveColor = inactiveColor," +
                " errorColor = errorColor," +
                " backgroundColor = backgroundColor," +
                " shape = shape)",
        "androidx.ui.material.TextField"
    )
)
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
    onFocusChanged: (Boolean) -> Unit = {},
    onTextInputStarted: (SoftwareKeyboardController) -> Unit = {},
    activeColor: Color = MaterialTheme.colors.primary,
    inactiveColor: Color = MaterialTheme.colors.onSurface,
    errorColor: Color = MaterialTheme.colors.error,
    backgroundColor: Color = MaterialTheme.colors.onSurface,
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomLeft = ZeroCornerSize, bottomRight = ZeroCornerSize)
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        isErrorValue = isErrorValue,
        visualTransformation = visualTransformation,
        keyboardType = keyboardType,
        imeAction = imeAction,
        onImeActionPerformed = onImeActionPerformed,
        onFocusChanged = onFocusChanged,
        onTextInputStarted = onTextInputStarted,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        errorColor = errorColor,
        backgroundColor = backgroundColor,
        shape = shape
    )
}

@Composable
internal fun FilledTextFieldLayout(
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
    IconsWithTextFieldLayout(
        modifier = textFieldModifier
            .background(
                color = backgroundColor.applyAlpha(alpha = ContainerAlpha),
                shape = shape
            )
            .drawIndicatorLine(
                lineWidth = indicatorWidth,
                color = indicatorColor
            ),
        textField = @Composable {
            // places input field, label, placeholder
            TextFieldWithLabelLayout(
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

/**
 * Layout of the text field, label and placeholder part in [TextField]
 */
@Composable
private fun TextFieldWithLabelLayout(
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
            measurables.find { it.id == PlaceholderId }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))

        val baseLineOffset = FirstBaselineOffset.toIntPx()
        val lastBaselineOffset = LastBaselineOffset.toIntPx()
        val padding = FilledTextFieldTopPadding.toIntPx()

        val labelConstraints = constraints
            .offset(vertical = -LastBaselineOffset.toIntPx())
            .copy(minWidth = 0, minHeight = 0)
        val labelPlaceable = measurables.first { it.id == LabelId }.measure(labelConstraints)
        val labelBaseline = labelPlaceable[LastBaseline].let {
            if (it != AlignmentLine.Unspecified) it else labelPlaceable.height
        }
        val labelEndPosition = (baseLineOffset - labelBaseline).coerceAtLeast(0)
        // to support cases where label is not a text
        val effectiveLabelBaseline = max(labelBaseline, baseLineOffset)

        val textFieldConstraints = constraints
            .offset(vertical = -lastBaselineOffset - padding - effectiveLabelBaseline)
            .copy(minHeight = 0)
        val textFieldPlaceable = measurables
            .first { it.id == TextFieldId }
            .measure(textFieldConstraints)
        val textFieldLastBaseline = textFieldPlaceable[LastBaseline]
        require(textFieldLastBaseline != AlignmentLine.Unspecified) { "No text last baseline." }

        val width = max(textFieldPlaceable.width, constraints.minWidth)
        val height = max(
            effectiveLabelBaseline + padding + textFieldLastBaseline + lastBaselineOffset,
            constraints.minHeight
        )

        layout(width, height) {
            // Text field and label are placed with respect to the baseline offsets.
            // But if label is empty, then the text field should be centered vertically.
            if (labelPlaceable.width != 0) {
                placeLabelAndTextfield(
                    width,
                    height,
                    textFieldPlaceable,
                    labelPlaceable,
                    placeholderPlaceable,
                    labelEndPosition,
                    effectiveLabelBaseline + padding,
                    animationProgress
                )
            } else {
                placeTextfield(height, textFieldPlaceable, placeholderPlaceable)
            }
        }
    }
}

/**
 * Layout of the leading and trailing icons and the text field part in [TextField].
 * It differs from the Row as it does not lose the minHeight constraint which is needed to
 * correctly place the text field and label.
 * Should be revisited if b/154202249 is fixed so that Row could be used instead
 */
@Composable
private fun IconsWithTextFieldLayout(
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
                    Decoration(
                        contentColor = leadingColor,
                        children = leading
                    )
                }
            }
            if (trailing != null) {
                Box(Modifier.layoutId("trailing").iconPadding(end = HorizontalIconPadding)) {
                    Decoration(
                        contentColor = trailingColor,
                        children = trailing
                    )
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
        occupiedSpace += widthOrZero(
            leadingPlaceable
        )

        val trailingPlaceable = measurables.find { it.id == "trailing" }
            ?.measure(constraints.offset(horizontal = -occupiedSpace))
        occupiedSpace += widthOrZero(
            trailingPlaceable
        )

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
                ).maxByOrNull { heightOrZero(it) }
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
 * [TextField]
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
 * Places the provided text field and placeholder center vertically in [TextField]
 */
private fun Placeable.PlacementScope.placeTextfield(
    height: Int,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?
) {
    textPlaceable.place(
        0,
        Alignment.CenterVertically.align(height - textPlaceable.height)
    )
    placeholderPlaceable?.place(
        0,
        Alignment.CenterVertically.align(height - placeholderPlaceable.height)
    )
}

/**
 * A draw modifier that draws a bottom indicator line in [TextField]
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

private val FirstBaselineOffset = 20.dp
private val LastBaselineOffset = 16.dp
private val FilledTextFieldTopPadding = 3.dp
private const val ContainerAlpha = 0.12f