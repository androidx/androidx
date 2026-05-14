/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.topPaddingForLabelCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * If you are looking for a filled version, see [TextField]. For a text field specifically designed
 * for passwords or other secure content, see [OutlinedSecureTextField].
 *
 * This overload of [OutlinedTextField] uses [TextFieldState] to keep track of its text content and
 * position of the cursor or selection.
 *
 * See example usage:
 *
 * @sample androidx.compose.material3.samples.SimpleOutlinedTextFieldSample
 * @sample androidx.compose.material3.samples.OutlinedTextFieldWithInitialValueAndSelection
 * @param state [TextFieldState] object that holds the internal editing state of the text field.
 * @param modifier the [Modifier] to be applied to this text field.
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param labelPosition the position of the label. See [TextFieldLabelPosition].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the input text is empty. The
 *   default text style uses [Typography.bodyLarge].
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container.
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container.
 * @param prefix the optional prefix to be displayed before the input text in the text field.
 * @param suffix the optional suffix to be displayed after the input text in the text field.
 * @param supportingText the optional supporting text to be displayed below the text field.
 * @param isError indicates if the text field's current value is in error. When `true`, the
 *   components of the text field will be displayed in an error color, and an error will be
 *   announced to accessibility services.
 * @param inputTransformation optional [InputTransformation] that will be used to transform changes
 *   to the [TextFieldState] made by the user. The transformation will be applied to changes made by
 *   hardware and software keyboard events, pasting or dropping text, accessibility services, and
 *   tests. The transformation will _not_ be applied when changing the [state] programmatically, or
 *   when the transformation is changed. If the transformation is changed on an existing text field,
 *   it will be applied to the next user edit. The transformation will not immediately affect the
 *   current [state].
 * @param outputTransformation optional [OutputTransformation] that transforms how the contents of
 *   the text field are presented.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param onKeyboardAction called when the user presses the action button in the input method editor
 *   (IME), or by pressing the enter key on a hardware keyboard. By default this parameter is null,
 *   and would execute the default behavior for a received IME Action e.g., [ImeAction.Done] would
 *   close the keyboard, [ImeAction.Next] would switch the focus to the next focusable item on the
 *   screen.
 * @param lineLimits whether the text field should be [SingleLine], scroll horizontally, and ignore
 *   newlines; or [MultiLine] and grow and scroll vertically. If [SingleLine] is passed, all newline
 *   characters ('\n') within the text will be replaced with regular whitespace (' ').
 * @param onTextLayout Callback that is executed when the text layout becomes queryable. The
 *   callback receives a function that returns a [TextLayoutResult] if the layout can be calculated,
 *   or null if it cannot. The function reads the layout result from a snapshot state object, and
 *   will invalidate its caller when the layout result changes. A [TextLayoutResult] object contains
 *   paragraph information, size of the text, baselines and other details. [Density] scope is the
 *   one that was used while creating the given text layout.
 * @param scrollState scroll state that manages either horizontal or vertical scroll of the text
 *   field. If [lineLimits] is [SingleLine], this text field is treated as single line with
 *   horizontal scroll behavior. Otherwise, the text field becomes vertically scrollable.
 * @param shape defines the shape of this text field's border.
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 * @param contentPadding the padding applied to the inner text field that separates it from the
 *   surrounding elements of the text field. Note that the padding values may not be respected if
 *   they are incompatible with the text field's size constraints or layout. See
 *   [OutlinedTextFieldDefaults.contentPaddingWithoutLabel] or
 *   [OutlinedTextFieldDefaults.contentPaddingWithLabel]..
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Cutout(),
    label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    contentPadding: PaddingValues =
        OutlinedTextFieldDefaults.defaultContentPadding(label, labelPosition),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = state,
            modifier =
                modifier
                    .topPaddingForLabelCutout(label, labelPosition)
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            onTextLayout = onTextLayout,
            interactionSource = interactionSource,
            inputTransformation = inputTransformation,
            outputTransformation = outputTransformation,
            scrollState = scrollState,
            decorator =
                OutlinedTextFieldDefaults.decorator(
                    state = state,
                    enabled = enabled,
                    lineLimits = lineLimits,
                    outputTransformation = outputTransformation,
                    interactionSource = interactionSource,
                    labelPosition = labelPosition,
                    label = label,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    isError = isError,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = enabled,
                            isError = isError,
                            interactionSource = interactionSource,
                            colors = colors,
                            shape = shape,
                        )
                    },
                ),
        )
    }
}

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the OutlinedTextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 *   updated text comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .topPaddingForLabelCutout(label, TextFieldLabelPosition.Cutout())
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = colors,
                                shape = shape,
                            )
                        },
                    )
                },
        )
    }
}

/**
 * [Material Design outlined text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Outlined text fields have less visual emphasis than filled text fields. When they appear in
 * places like forms, where many text fields are placed together, their reduced emphasis helps
 * simplify the layout.
 *
 * ![Outlined text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-text-field.png)
 *
 * This overload provides access to the input text, cursor position and selection range and IME
 * composition. If you only want to observe an input text change, use the OutlinedTextField overload
 * with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 *   [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 *   be modified. However, a user can focus it and copy text from it. Read-only text fields are
 *   usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed with this text field. The default text style uses
 *   [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 *   the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 *   container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 *   container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error state. If set to true, the
 *   label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value] For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction]
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction]
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 *   instead of wrapping onto multiple lines. The keyboard will be informed to not show the return
 *   key as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines
 *   attribute will be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 *   that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape defines the shape of this text field's border
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [OutlinedTextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .topPaddingForLabelCutout(label, TextFieldLabelPosition.Cutout())
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix,
                        suffix = suffix,
                        supportingText = supportingText,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = colors,
                                shape = shape,
                            )
                        },
                    )
                },
        )
    }
}

internal fun Modifier.outlineCutout(
    labelSize: () -> Size,
    alignment: Alignment.Horizontal,
    paddingValues: PaddingValues,
) =
    this.drawWithContent {
        val labelSizeValue = labelSize()
        val labelWidth = labelSizeValue.width
        if (labelWidth > 0f) {
            val innerPadding = OutlinedTextFieldInnerPadding.toPx()
            val leftPadding = paddingValues.calculateLeftPadding(layoutDirection).toPx()
            val rightPadding = paddingValues.calculateRightPadding(layoutDirection).toPx()
            val labelCenter =
                alignment.align(
                    size = labelWidth.roundToInt(),
                    space = (size.width - leftPadding - rightPadding).roundToInt(),
                    layoutDirection = layoutDirection,
                ) + leftPadding + (labelWidth / 2)
            val left = (labelCenter - (labelWidth / 2) - innerPadding).coerceAtLeast(0f)
            val right = (labelCenter + (labelWidth / 2) + innerPadding).coerceAtMost(size.width)
            val labelHeight = labelSizeValue.height
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
