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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m2.material.io/components/text-fields#outlined-text-field" class="external"
 * target="_blank">Material Design outlined text field</a>.
 *
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/outlined-text-field.png)
 *
 * If you are looking for a filled version, see [TextField]. For a text field specifically designed
 * for passwords or other secure content, see [OutlinedSecureTextField].
 *
 * This overload of [OutlinedTextField] uses [TextFieldState] to keep track of its text content and
 * position of the cursor or selection.
 *
 * See example usage:
 *
 * @sample androidx.compose.material.samples.SimpleOutlinedTextFieldSample
 * @sample androidx.compose.material.samples.OutlinedTextFieldWithInitialValueAndSelection
 * @param state [TextFieldState] object that holds the internal editing state of this text field.
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, the text
 *   field will be neither editable nor focusable, the input of the text field will not be
 *   selectable, visually text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [OutlinedTextField]. When `true`, the text
 *   field can not be modified, however, a user can focus it and copy text from it. Read-only text
 *   fields are usually used to display pre-filled forms that user can not edit
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
 *   [KeyboardType] and [ImeAction]
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
 * @param shape the shape of the text field's border
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 *   (including label, placeholder, leading and trailing icons, border) for this text field in
 *   different states. See [TextFieldDefaults.outlinedTextFieldColors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun OutlinedTextField(
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
    shape: Shape = TextFieldDefaults.OutlinedTextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val density = LocalDensity.current

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        state = state,
        modifier =
            modifier
                .then(
                    if (label != null) {
                        Modifier
                            // Merge semantics at the beginning of the modifier chain to ensure
                            // padding is considered part of the text field.
                            .semantics(mergeDescendants = true) {}
                            .padding(top = with(density) { OutlinedTextFieldTopPadding.toDp() })
                    } else {
                        Modifier
                    }
                )
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
                    val buffer = state.toTextFieldBuffer()
                    // after edit completes, mutations on buffer are ineffective
                    with(outputTransformation) { buffer.transformOutput() }
                    buffer.toString()
                }

            TextFieldDefaults.OutlinedTextFieldDecorationBox(
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
                border = {
                    TextFieldDefaults.BorderBox(enabled, isError, interactionSource, colors, shape)
                }
            )
        }
    )
}

/**
 * <a href="https://m2.material.io/components/text-fields#outlined-text-field" class="external"
 * target="_blank">Material Design outlined text field</a>.
 *
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/outlined-text-field.png)
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, the text
 *   field will be neither editable nor focusable, the input of the text field will not be
 *   selectable, visually text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [OutlinedTextField]. When `true`, the text
 *   field can not be modified, however, a user can focus it and copy text from it. Read-only text
 *   fields are usually used to display pre-filled forms that user can not edit
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
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the shape of the text field's border
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 *   (including label, placeholder, leading and trailing icons, border) for this text field in
 *   different states. See [TextFieldDefaults.outlinedTextFieldColors]
 */
@Composable
fun OutlinedTextField(
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextFieldDefaults.OutlinedTextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val density = LocalDensity.current

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier =
            modifier
                .then(
                    if (label != null) {
                        Modifier
                            // Merge semantics at the beginning of the modifier chain to ensure
                            // padding is considered part of the text field.
                            .semantics(mergeDescendants = true) {}
                            .padding(top = with(density) { OutlinedTextFieldTopPadding.toDp() })
                    } else {
                        Modifier
                    }
                )
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
                TextFieldDefaults.OutlinedTextFieldDecorationBox(
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
                    border = {
                        TextFieldDefaults.BorderBox(
                            enabled,
                            isError,
                            interactionSource,
                            colors,
                            shape
                        )
                    }
                )
            }
    )
}

@Deprecated(
    "Maintained for binary compatibility. Use version with minLines instead",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun OutlinedTextField(
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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.OutlinedTextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    OutlinedTextField(
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
 * <a href="https://m2.material.io/components/text-fields#outlined-text-field" class="external"
 * target="_blank">Material Design outlined text field</a>.
 *
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material/outlined-text-field.png)
 *
 * This overload provides access to the input text, cursor position and selection range and IME
 * composition. If you only want to observe an input text change, use the OutlinedTextField overload
 * with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 *   [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier a [Modifier] for this text field
 * @param enabled controls the enabled state of the [OutlinedTextField]. When `false`, the text
 *   field will be neither editable nor focusable, the input of the text field will not be
 *   selectable, visually text field will appear in the disabled UI state
 * @param readOnly controls the editable state of the [OutlinedTextField]. When `true`, the text
 *   field can not be modified, however, a user can focus it and copy text from it. Read-only text
 *   fields are usually used to display pre-filled forms that user can not edit
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
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default no visual transformation is applied
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when set to true, this text field becomes a single horizontally scrolling text
 *   field instead of wrapping onto multiple lines. The keyboard will be informed to not show the
 *   return key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the shape of the text field's border
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 *   (including label, placeholder, leading and trailing icons, border) for this text field in
 *   different states. See [TextFieldDefaults.outlinedTextFieldColors]
 */
@Composable
fun OutlinedTextField(
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
    shape: Shape = TextFieldDefaults.OutlinedTextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse { colors.textColor(enabled).value }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    val density = LocalDensity.current

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier =
            modifier
                .then(
                    if (label != null) {
                        Modifier
                            // Merge semantics at the beginning of the modifier chain to ensure
                            // padding is considered part of the text field.
                            .semantics(mergeDescendants = true) {}
                            .padding(top = with(density) { OutlinedTextFieldTopPadding.toDp() })
                    } else {
                        Modifier
                    }
                )
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
                TextFieldDefaults.OutlinedTextFieldDecorationBox(
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
                    border = {
                        TextFieldDefaults.BorderBox(
                            enabled,
                            isError,
                            interactionSource,
                            colors,
                            shape
                        )
                    }
                )
            }
    )
}

@Deprecated(
    "Maintained for binary compatibility. Use version with minLines instead",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun OutlinedTextField(
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
    shape: Shape = TextFieldDefaults.OutlinedTextFieldShape,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    OutlinedTextField(
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
 * Layout of the leading and trailing icons and the text field, label and placeholder in
 * [OutlinedTextField]. It doesn't use Row to position the icons and middle part because label
 * should not be positioned in the middle part. \
 */
@Composable
internal fun OutlinedTextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    placeholder: @Composable ((Modifier) -> Unit)?,
    label: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    singleLine: Boolean,
    animationProgress: Float,
    onLabelMeasured: (Size) -> Unit,
    border: @Composable () -> Unit,
    paddingValues: PaddingValues
) {
    val measurePolicy =
        remember(onLabelMeasured, singleLine, animationProgress, paddingValues) {
            OutlinedTextFieldMeasurePolicy(
                onLabelMeasured,
                singleLine,
                animationProgress,
                paddingValues
            )
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            // We use additional box here to place an outlined cutout border as a sibling after the
            // rest of UI. This allows us to use Modifier.border to draw an outline on top of the
            // text field. We can't use the border modifier directly on the IconsWithTextFieldLayout
            // as we also need to do the clipping (to form the cutout) which should not affect
            // the rest of text field UI
            border()

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

            Box(
                modifier = Modifier.layoutId(TextFieldId).then(padding),
                propagateMinConstraints = true
            ) {
                textField()
            }

            if (label != null) {
                Box(modifier = Modifier.layoutId(LabelId)) { label() }
            }
        },
        measurePolicy = measurePolicy
    )
}

private class OutlinedTextFieldMeasurePolicy(
    private val onLabelMeasured: (Size) -> Unit,
    private val singleLine: Boolean,
    private val animationProgress: Float,
    private val paddingValues: PaddingValues
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        // used to calculate the constraints for measuring elements that will be placed in a row
        var occupiedSpaceHorizontally = 0
        val bottomPadding = paddingValues.calculateBottomPadding().roundToPx()

        // measure leading icon
        val relaxedConstraints = constraints.copyMaxDimensions()
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(relaxedConstraints)
        occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)

        // measure label
        val labelHorizontalPaddingOffset =
            paddingValues.calculateLeftPadding(layoutDirection).roundToPx() +
                paddingValues.calculateRightPadding(layoutDirection).roundToPx()
        val labelConstraints =
            relaxedConstraints.offset(
                horizontal =
                    lerp(
                        -occupiedSpaceHorizontally - labelHorizontalPaddingOffset,
                        -labelHorizontalPaddingOffset,
                        animationProgress,
                    ),
                vertical = -bottomPadding
            )
        val labelPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LabelId }?.measure(labelConstraints)
        val labelSize =
            labelPlaceable?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
        onLabelMeasured(labelSize)

        // measure text field
        // on top we offset either by default padding or by label's half height if its too big
        // minHeight must not be set to 0 due to how foundation TextField treats zero minHeight
        val topPadding =
            max(heightOrZero(labelPlaceable) / 2, paddingValues.calculateTopPadding().roundToPx())
        val textConstraints =
            constraints
                .offset(
                    horizontal = -occupiedSpaceHorizontally,
                    vertical = -bottomPadding - topPadding
                )
                .copy(minHeight = 0)
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textConstraints)

        // measure placeholder
        val placeholderConstraints = textConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        val width =
            calculateWidth(
                leadingPlaceableWidth = widthOrZero(leadingPlaceable),
                trailingPlaceableWidth = widthOrZero(trailingPlaceable),
                textFieldPlaceableWidth = textFieldPlaceable.width,
                labelPlaceableWidth = widthOrZero(labelPlaceable),
                placeholderPlaceableWidth = widthOrZero(placeholderPlaceable),
                animationProgress = animationProgress,
                constraints = constraints,
                density = density,
                paddingValues = paddingValues,
            )
        val height =
            calculateHeight(
                leadingPlaceableHeight = heightOrZero(leadingPlaceable),
                trailingPlaceableHeight = heightOrZero(trailingPlaceable),
                textFieldPlaceableHeight = textFieldPlaceable.height,
                labelPlaceableHeight = heightOrZero(labelPlaceable),
                placeholderPlaceableHeight = heightOrZero(placeholderPlaceable),
                animationProgress = animationProgress,
                constraints = constraints,
                density = density,
                paddingValues = paddingValues,
            )

        val borderPlaceable =
            measurables
                .fastFirst { it.layoutId == BorderId }
                .measure(
                    Constraints(
                        minWidth = if (width != Constraints.Infinity) width else 0,
                        maxWidth = width,
                        minHeight = if (height != Constraints.Infinity) height else 0,
                        maxHeight = height
                    )
                )
        return layout(width, height) {
            place(
                height,
                width,
                leadingPlaceable,
                trailingPlaceable,
                textFieldPlaceable,
                labelPlaceable,
                placeholderPlaceable,
                borderPlaceable,
                animationProgress,
                singleLine,
                density,
                layoutDirection,
                paddingValues
            )
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

    private fun IntrinsicMeasureScope.intrinsicWidth(
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
            leadingPlaceableWidth = leadingWidth,
            trailingPlaceableWidth = trailingWidth,
            textFieldPlaceableWidth = textFieldWidth,
            labelPlaceableWidth = labelWidth,
            placeholderPlaceableWidth = placeholderWidth,
            animationProgress = animationProgress,
            constraints = ZeroConstraints,
            density = density,
            paddingValues = paddingValues,
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
                ?.let { intrinsicMeasurer(it, lerp(remainingWidth, width, animationProgress)) } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        return calculateHeight(
            leadingPlaceableHeight = leadingHeight,
            trailingPlaceableHeight = trailingHeight,
            textFieldPlaceableHeight = textFieldHeight,
            labelPlaceableHeight = labelHeight,
            placeholderPlaceableHeight = placeholderHeight,
            animationProgress = animationProgress,
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

/**
 * Calculate the width of the [OutlinedTextField] given all elements that should be placed inside
 */
private fun calculateWidth(
    leadingPlaceableWidth: Int,
    trailingPlaceableWidth: Int,
    textFieldPlaceableWidth: Int,
    labelPlaceableWidth: Int,
    placeholderPlaceableWidth: Int,
    animationProgress: Float,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues,
): Int {
    val middleSection =
        maxOf(
            textFieldPlaceableWidth,
            lerp(labelPlaceableWidth, 0, animationProgress),
            placeholderPlaceableWidth
        )
    val wrappedWidth = leadingPlaceableWidth + middleSection + trailingPlaceableWidth

    // Actual LayoutDirection doesn't matter; we only need the sum
    val labelHorizontalPadding =
        (paddingValues.calculateLeftPadding(LayoutDirection.Ltr) +
                paddingValues.calculateRightPadding(LayoutDirection.Ltr))
            .value * density
    val focusedLabelWidth =
        ((labelPlaceableWidth + labelHorizontalPadding) * animationProgress).roundToInt()

    return maxOf(wrappedWidth, focusedLabelWidth, constraints.minWidth)
}

/**
 * Calculate the height of the [OutlinedTextField] given all elements that should be placed inside
 */
private fun calculateHeight(
    leadingPlaceableHeight: Int,
    trailingPlaceableHeight: Int,
    textFieldPlaceableHeight: Int,
    labelPlaceableHeight: Int,
    placeholderPlaceableHeight: Int,
    animationProgress: Float,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues
): Int {
    val inputFieldHeight =
        maxOf(
            textFieldPlaceableHeight,
            placeholderPlaceableHeight,
            lerp(labelPlaceableHeight, 0, animationProgress),
        )
    val topPadding = paddingValues.calculateTopPadding().value * density
    val actualTopPadding =
        lerp(
            topPadding,
            max(topPadding, labelPlaceableHeight / 2f),
            animationProgress,
        )
    val bottomPadding = paddingValues.calculateBottomPadding().value * density
    val middleSectionHeight = actualTopPadding + inputFieldHeight + bottomPadding

    return max(
        constraints.minHeight,
        maxOf(leadingPlaceableHeight, trailingPlaceableHeight, middleSectionHeight.roundToInt())
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
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    borderPlaceable: Placeable,
    animationProgress: Float,
    singleLine: Boolean,
    density: Float,
    layoutDirection: LayoutDirection,
    paddingValues: PaddingValues
) {
    val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()
    val startPadding =
        (paddingValues.calculateStartPadding(layoutDirection).value * density).roundToInt()

    val iconPadding = HorizontalIconPadding.value * density

    // placed center vertically and to the start edge horizontally
    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )

    // placed center vertically and to the end edge horizontally
    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    // label position is animated
    // in single line text field label is centered vertically before animation starts
    labelPlaceable?.let {
        val startPositionY =
            if (singleLine) {
                Alignment.CenterVertically.align(it.height, height)
            } else {
                topPadding
            }
        val positionY = lerp(startPositionY, -(it.height / 2), animationProgress)
        val positionX =
            (if (leadingPlaceable == null) {
                    0f
                } else {
                    (widthOrZero(leadingPlaceable) - iconPadding) * (1 - animationProgress)
                })
                .roundToInt() + startPadding
        it.placeRelative(positionX, positionY)
    }

    // placed center vertically and after the leading icon horizontally if single line text field
    // placed to the top with padding for multi line text field
    val textVerticalPosition =
        max(
            if (singleLine) {
                Alignment.CenterVertically.align(textFieldPlaceable.height, height)
            } else {
                topPadding
            },
            heightOrZero(labelPlaceable) / 2
        )
    textFieldPlaceable.placeRelative(widthOrZero(leadingPlaceable), textVerticalPosition)

    // placed similar to the input text above
    placeholderPlaceable?.let {
        val placeholderVerticalPosition =
            max(
                if (singleLine) {
                    Alignment.CenterVertically.align(it.height, height)
                } else {
                    topPadding
                },
                heightOrZero(labelPlaceable) / 2
            )
        it.placeRelative(widthOrZero(leadingPlaceable), placeholderVerticalPosition)
    }

    // place border
    borderPlaceable.place(IntOffset.Zero)
}

internal fun Modifier.outlineCutout(labelSize: Size, paddingValues: PaddingValues) =
    this.drawWithContent {
        val labelWidth = labelSize.width
        if (labelWidth > 0f) {
            val innerPadding = OutlinedTextFieldInnerPadding.toPx()
            val leftLtr = paddingValues.calculateLeftPadding(layoutDirection).toPx() - innerPadding
            val rightLtr = leftLtr + labelWidth + 2 * innerPadding
            val left =
                when (layoutDirection) {
                    LayoutDirection.Rtl -> size.width - rightLtr
                    else -> leftLtr.coerceAtLeast(0f)
                }
            val right =
                when (layoutDirection) {
                    LayoutDirection.Rtl -> size.width - leftLtr.coerceAtLeast(0f)
                    else -> rightLtr
                }
            val labelHeight = labelSize.height
            // using label height as a cutout area to make sure that no hairline artifacts are
            // left when we clip the border
            clipRect(left, -labelHeight / 2, right, labelHeight / 2, ClipOp.Difference) {
                this@drawWithContent.drawContent()
            }
        } else {
            this@drawWithContent.drawContent()
        }
    }

private val OutlinedTextFieldInnerPadding = 4.dp

/**
 * In the focused state, the top half of the label sticks out above the text field. This default
 * padding is a best-effort approximation to keep the label from overlapping with the content above
 * it. It is sufficient when the label is a single line and developers do not override the label's
 * font size/style. Otherwise, developers will need to add additional padding themselves.
 */
internal val OutlinedTextFieldTopPadding = 8.sp

internal const val BorderId = "border"
