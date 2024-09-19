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

package androidx.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m2.material.io/components/text-fields#filled-text-field" class="external"
 * target="_blank">Material Design filled text field</a>.
 *
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * This overload of [TextField] uses [TextFieldState] to keep track of its text content and position
 * of the cursor or selection.
 *
 * A simple single line text field looks like:
 *
 * @sample androidx.compose.material.samples.SimpleTextFieldSample
 *
 * You can control the initial text input and selection:
 *
 * @sample androidx.compose.material.samples.TextFieldWithInitialValueAndSelection
 *
 * You may provide a placeholder:
 *
 * @sample androidx.compose.material.samples.TextFieldWithPlaceholder
 *
 * You can also provide leading and trailing icons:
 *
 * @sample androidx.compose.material.samples.TextFieldWithIcons
 *
 * To handle the error input state, use [isError] parameter:
 *
 * @sample androidx.compose.material.samples.TextFieldWithErrorState
 *
 * Additionally, you may provide additional message at the bottom:
 *
 * @sample androidx.compose.material.samples.TextFieldWithHelperMessage
 *
 * Hiding a software keyboard on IME action performed:
 *
 * @sample androidx.compose.material.samples.TextFieldWithHideKeyboardOnImeAction
 * @param state [TextFieldState] object that holds the internal editing state of this text field.
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [TextField]. When `false`, the text field will
 *   be neither editable nor focusable, the input of the text field will not be selectable, visually
 *   text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [TextField]. When `true`, the text field can
 *   not be modified, however, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that user can not edit
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 *   [LocalTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default text
 *   style for internal [Text] is [Typography.caption] when the text field is in focus and
 *   [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param isError indicates if the text field's current value is in error. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param inputTransformation Optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. the transformation will not immediately affect the
 *   current [state].
 * @param outputTransformation An [OutputTransformation] that transforms how the contents of the
 *   text field are presented.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction Called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param lineLimits Whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' '), ensuring that
 *   the contents of the text field are presented in a single line.
 * @param scrollState Scroll state that manages either horizontal or vertical scroll of the text
 *   field. If [lineLimits] is [SingleLine], this text field is treated as single line with
 *   horizontal scroll behavior. In other cases the text field becomes vertically scrollable.
 * @param shape the shape of the text field's container
 * @param colors [TextFieldColors] that will be used to resolve color of the text, content
 *   (including label, placeholder, leading and trailing icons, indicator line) and background for
 *   this text field in different states. See [TextFieldDefaults.textFieldColors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        state = state,
        modifier =
            modifier
                .indicatorLine(enabled, isError, interactionSource, colors)
                .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        inputTransformation = inputTransformation,
        outputTransformation = outputTransformation,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        interactionSource = interactionSource,
        scrollState = scrollState,
        lineLimits = lineLimits,
        decorator = { innerTextField ->
            val textPostTransformation =
                if (outputTransformation == null) {
                    state.text.toString()
                } else {
                    // TODO: use constructor to create TextFieldBuffer from TextFieldState when
                    // available
                    lateinit var buffer: TextFieldBuffer
                    state.edit { buffer = this }
                    // after edit completes, mutations on buffer are ineffective
                    with(outputTransformation) { buffer.transformOutput() }
                    buffer.asCharSequence().toString()
                }

            TextFieldDefaults.TextFieldDecorationBox(
                value = textPostTransformation,
                visualTransformation = VisualTransformation.None,
                innerTextField = innerTextField,
                placeholder = placeholder,
                label = label,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                singleLine = lineLimits == SingleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                shape = shape,
                colors = colors,
            )
        }
    )
}

/**
 * <a href="https://m2.material.io/components/text-fields#filled-text-field" class="external"
 * target="_blank">Material Design filled text field</a>.
 *
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the TextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [TextField]. When `false`, the text field will
 *   be neither editable nor focusable, the input of the text field will not be selectable, visually
 *   text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [TextField]. When `true`, the text field can
 *   not be modified, however, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that user can not edit
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 *   [LocalTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default text
 *   style for internal [Text] is [Typography.caption] when the text field is in focus and
 *   [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param isError indicates if the text field's current value is in error. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the shape of the text field's container
 * @param colors [TextFieldColors] that will be used to resolve color of the text, content
 *   (including label, placeholder, leading and trailing icons, indicator line) and background for
 *   this text field in different states. See [TextFieldDefaults.textFieldColors]
 */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier =
            modifier
                .indicatorLine(enabled, isError, interactionSource, colors)
                .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox =
            @Composable { innerTextField ->
                // places leading icon, text field with label and placeholder, trailing icon
                TextFieldDefaults.TextFieldDecorationBox(
                    value = value,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    shape = shape,
                    colors = colors,
                )
            }
    )
}

@Deprecated(
    "Maintained for binary compatibility. Use version with minLines instead",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape =
        MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    TextField(
        value,
        onValueChange,
        modifier,
        enabled,
        readOnly,
        textStyle,
        label,
        placeholder,
        leadingIcon,
        trailingIcon,
        isError,
        visualTransformation,
        keyboardOptions,
        keyboardActions,
        singleLine,
        maxLines,
        1,
        interactionSource,
        shape,
        colors
    )
}

/**
 * <a href="https://m2.material.io/components/text-fields#filled-text-field" class="external"
 * target="_blank">Material Design filled text field</a>.
 *
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField]. For a text field
 * specifically designed for passwords or other secure content, see [SecureTextField].
 *
 * This overload provides access to the input text, cursor position, selection range and IME
 * composition. If you only want to observe an input text change, use the TextField overload with
 * the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 *   [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [TextField]. When `false`, the text field will
 *   be neither editable nor focusable, the input of the text field will not be selectable, visually
 *   text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [TextField]. When `true`, the text field can
 *   not be modified, however, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that user can not edit
 * @param textStyle the style to be applied to the input text. The default [textStyle] uses the
 *   [LocalTextStyle] defined by the theme
 * @param label the optional label to be displayed inside the text field container. The default text
 *   style for internal [Text] is [Typography.caption] when the text field is in focus and
 *   [Typography.subtitle1] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param isError indicates if the text field's current value is in error state. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value]. For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the shape of the text field's container
 * @param colors [TextFieldColors] that will be used to resolve color of the text, content
 *   (including label, placeholder, leading and trailing icons, indicator line) and background for
 *   this text field in different states. See [TextFieldDefaults.textFieldColors]
 */
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier =
            modifier
                .indicatorLine(enabled, isError, interactionSource, colors)
                .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
                ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox =
            @Composable { innerTextField ->
                // places leading icon, text field with label and placeholder, trailing icon
                TextFieldDefaults.TextFieldDecorationBox(
                    value = value.text,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    shape = shape,
                    colors = colors,
                )
            }
    )
}

@Deprecated(
    "Maintained for binary compatibility. Use version with minLines instead",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun TextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions(),
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.TextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors()
) {
    TextField(
        value,
        onValueChange,
        modifier,
        enabled,
        readOnly,
        textStyle,
        label,
        placeholder,
        leadingIcon,
        trailingIcon,
        isError,
        visualTransformation,
        keyboardOptions,
        keyboardActions,
        singleLine,
        maxLines,
        1,
        interactionSource,
        shape,
        colors
    )
}

/**
 * Composable responsible for measuring and laying out leading and trailing icons, label,
 * placeholder and the input field.
 */
@Composable
internal fun TextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable ((Modifier) -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    animationProgress: Float,
    paddingValues: PaddingValues
) {
    val measurePolicy =
        remember(singleLine, animationProgress, paddingValues) {
            TextFieldMeasurePolicy(singleLine, animationProgress, paddingValues)
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)
            val padding =
                Modifier.padding(
                    start =
                        if (leading != null) {
                            (startTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
                        } else {
                            startTextFieldPadding
                        },
                    end =
                        if (trailing != null) {
                            (endTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
                        } else {
                            endTextFieldPadding
                        }
                )
            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).then(padding))
            }
            if (label != null) {
                Box(Modifier.layoutId(LabelId).then(padding)) { label() }
            }
            Box(
                modifier = Modifier.layoutId(TextFieldId).then(padding),
                propagateMinConstraints = true,
            ) {
                textField()
            }
        },
        measurePolicy = measurePolicy
    )
}

private class TextFieldMeasurePolicy(
    private val singleLine: Boolean,
    private val animationProgress: Float,
    private val paddingValues: PaddingValues
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val topPaddingValue = paddingValues.calculateTopPadding().roundToPx()
        val bottomPaddingValue = paddingValues.calculateBottomPadding().roundToPx()

        // padding between label and input text
        val topPadding = TextFieldTopPadding.roundToPx()
        var occupiedSpaceHorizontally = 0

        // measure leading icon
        val looseConstraints = constraints.copyMaxDimensions()
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)

        // measure label
        val labelConstraints =
            looseConstraints.offset(
                vertical = -bottomPaddingValue,
                horizontal = -occupiedSpaceHorizontally
            )
        val labelPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LabelId }?.measure(labelConstraints)
        val lastBaseline =
            labelPlaceable?.get(LastBaseline)?.let {
                if (it != AlignmentLine.Unspecified) it else labelPlaceable.height
            } ?: 0
        val effectiveLabelBaseline = max(lastBaseline, topPaddingValue)

        // measure input field
        // input field is laid out differently depending on whether the label is present or not
        val verticalConstraintOffset =
            if (labelPlaceable != null) {
                -bottomPaddingValue - topPadding - effectiveLabelBaseline
            } else {
                -topPaddingValue - bottomPaddingValue
            }
        val textFieldConstraints =
            constraints
                .copy(minHeight = 0)
                .offset(
                    vertical = verticalConstraintOffset,
                    horizontal = -occupiedSpaceHorizontally
                )
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        val width =
            calculateWidth(
                widthOrZero(leadingPlaceable),
                widthOrZero(trailingPlaceable),
                textFieldPlaceable.width,
                widthOrZero(labelPlaceable),
                widthOrZero(placeholderPlaceable),
                constraints
            )
        val height =
            calculateHeight(
                textFieldPlaceable.height,
                labelPlaceable != null,
                effectiveLabelBaseline,
                heightOrZero(leadingPlaceable),
                heightOrZero(trailingPlaceable),
                heightOrZero(placeholderPlaceable),
                constraints,
                density,
                paddingValues
            )

        return layout(width, height) {
            if (labelPlaceable != null) {
                // label's final position is always relative to the baseline
                val labelEndPosition = (topPaddingValue - lastBaseline).coerceAtLeast(0)
                placeWithLabel(
                    width,
                    height,
                    textFieldPlaceable,
                    labelPlaceable,
                    placeholderPlaceable,
                    leadingPlaceable,
                    trailingPlaceable,
                    singleLine,
                    labelEndPosition,
                    effectiveLabelBaseline + topPadding,
                    animationProgress,
                    density
                )
            } else {
                placeWithoutLabel(
                    width,
                    height,
                    textFieldPlaceable,
                    placeholderPlaceable,
                    leadingPlaceable,
                    trailingPlaceable,
                    singleLine,
                    density,
                    paddingValues
                )
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        val textFieldWidth =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, height)
        val labelWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val trailingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val leadingWidth =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val placeholderWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        return calculateWidth(
            leadingWidth = leadingWidth,
            trailingWidth = trailingWidth,
            textFieldWidth = textFieldWidth,
            labelWidth = labelWidth,
            placeholderWidth = placeholderWidth,
            constraints = ZeroConstraints
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int
    ): Int {
        var remainingWidth = width
        val leadingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.substractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val trailingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.substractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0

        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        return calculateHeight(
            textFieldHeight = textFieldHeight,
            hasLabel = labelHeight > 0,
            labelBaseline = labelHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            placeholderHeight = placeholderHeight,
            constraints = ZeroConstraints,
            density = density,
            paddingValues = paddingValues
        )
    }
}

private fun Int.substractConstraintSafely(from: Int): Int {
    if (this == Constraints.Infinity) {
        return this
    }
    return this - from
}

private fun calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    textFieldWidth: Int,
    labelWidth: Int,
    placeholderWidth: Int,
    constraints: Constraints
): Int {
    val middleSection = maxOf(textFieldWidth, labelWidth, placeholderWidth)
    val wrappedWidth = leadingWidth + middleSection + trailingWidth
    return max(wrappedWidth, constraints.minWidth)
}

private fun calculateHeight(
    textFieldHeight: Int,
    hasLabel: Boolean,
    labelBaseline: Int,
    leadingHeight: Int,
    trailingHeight: Int,
    placeholderHeight: Int,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues
): Int {
    val paddingToLabel = TextFieldTopPadding.value * density
    val topPaddingValue = paddingValues.calculateTopPadding().value * density
    val bottomPaddingValue = paddingValues.calculateBottomPadding().value * density

    val inputFieldHeight = max(textFieldHeight, placeholderHeight)
    val middleSectionHeight =
        if (hasLabel) {
            labelBaseline + paddingToLabel + inputFieldHeight + bottomPaddingValue
        } else {
            topPaddingValue + inputFieldHeight + bottomPaddingValue
        }
    return maxOf(
        middleSectionHeight.roundToInt(),
        max(leadingHeight, trailingHeight),
        constraints.minHeight
    )
}

/**
 * Places the provided text field, placeholder and label with respect to the baseline offsets in
 * [TextField] when there is a label. When there is no label, [placeWithoutLabel] is used.
 */
private fun Placeable.PlacementScope.placeWithLabel(
    width: Int,
    height: Int,
    textfieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    singleLine: Boolean,
    labelEndPosition: Int,
    textPosition: Int,
    animationProgress: Float,
    density: Float
) {
    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )
    labelPlaceable?.let {
        // if it's a single line, the label's start position is in the center of the
        // container. When it's a multiline text field, the label's start position is at the
        // top with padding
        val startPosition =
            if (singleLine) {
                Alignment.CenterVertically.align(it.height, height)
            } else {
                // even though the padding is defined by developer, it only affects text field when
                // animation progress == 1,
                // which is when text field is focused or non-empty input text. The start position
                // of the label is always 16.dp.
                (TextFieldPadding.value * density).roundToInt()
            }
        val distance = startPosition - labelEndPosition
        val positionY = startPosition - (distance * animationProgress).roundToInt()
        it.placeRelative(widthOrZero(leadingPlaceable), positionY)
    }
    textfieldPlaceable.placeRelative(widthOrZero(leadingPlaceable), textPosition)
    placeholderPlaceable?.placeRelative(widthOrZero(leadingPlaceable), textPosition)
}

/**
 * Places the provided text field and placeholder in [TextField] when there is no label. When there
 * is a label, [placeWithLabel] is used
 */
private fun Placeable.PlacementScope.placeWithoutLabel(
    width: Int,
    height: Int,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    singleLine: Boolean,
    density: Float,
    paddingValues: PaddingValues
) {
    val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    // Single line text field without label places its input center vertically. Multiline text
    // field without label places its input at the top with padding
    val textVerticalPosition =
        if (singleLine) {
            Alignment.CenterVertically.align(textPlaceable.height, height)
        } else {
            topPadding
        }
    textPlaceable.placeRelative(widthOrZero(leadingPlaceable), textVerticalPosition)

    // placeholder is placed similar to the text input above
    placeholderPlaceable?.let {
        val placeholderVerticalPosition =
            if (singleLine) {
                Alignment.CenterVertically.align(placeholderPlaceable.height, height)
            } else {
                topPadding
            }
        it.placeRelative(widthOrZero(leadingPlaceable), placeholderVerticalPosition)
    }
}

/** A draw modifier that draws a bottom indicator line in [TextField] */
internal fun Modifier.drawIndicatorLine(indicatorBorder: BorderStroke): Modifier {
    val strokeWidthDp = indicatorBorder.width
    return drawWithContent {
        drawContent()
        if (strokeWidthDp == Dp.Hairline) return@drawWithContent
        val strokeWidth = strokeWidthDp.value * density
        val y = size.height - strokeWidth / 2
        drawLine(indicatorBorder.brush, Offset(0f, y), Offset(size.width, y), strokeWidth)
    }
}

/** Padding from the label's baseline to the top */
internal val FirstBaselineOffset = 20.dp

/** Padding from input field to the bottom */
internal val TextFieldBottomPadding = 10.dp

/** Padding from label's baseline (or FirstBaselineOffset) to the input field */
/*@VisibleForTesting*/
internal val TextFieldTopPadding = 2.dp
