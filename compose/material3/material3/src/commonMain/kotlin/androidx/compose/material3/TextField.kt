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

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.material3.MaterialTheme.LocalMaterialTheme
import androidx.compose.material3.TextFieldDefaults.defaultTextFieldColors
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.topPaddingForLabelCutout
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField]. For a text field
 * specifically designed for passwords or other secure content, see [SecureTextField].
 *
 * This overload of [TextField] uses [TextFieldState] to keep track of its text content and position
 * of the cursor or selection.
 *
 * A simple single line text field looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTextFieldSample
 *
 * You can control the initial text input and selection:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithInitialValueAndSelection
 *
 * Use input and output transformations to control user input and the displayed text:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithTransformations
 *
 * You may provide a placeholder:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithPlaceholder
 *
 * You can also provide leading and trailing icons:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithIcons
 *
 * You can also provide a prefix or suffix to the text:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithPrefixAndSuffix
 *
 * To handle the error input state, use [isError] parameter:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithErrorState
 *
 * Additionally, you may provide additional message at the bottom:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithSupportingText
 *
 * You can change the content padding to create a dense text field:
 *
 * @sample androidx.compose.material3.samples.DenseTextFieldContentPadding
 *
 * Hiding a software keyboard on IME action performed:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithHideKeyboardOnImeAction
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
 * @param shape defines the shape of this text field's container.
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 * @param contentPadding the padding applied to the inner text field that separates it from the
 *   surrounding elements of the text field. Note that the padding values may not be respected if
 *   they are incompatible with the text field's size constraints or layout. See
 *   [TextFieldDefaults.contentPaddingWithLabel] and [TextFieldDefaults.contentPaddingWithoutLabel].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this text field. You can use this to change the text field's
 *   appearance or preview the text field in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Inside(),
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
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    contentPadding: PaddingValues = TextFieldDefaults.defaultContentPadding(label, labelPosition),
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
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
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
                TextFieldDefaults.decorator(
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
                        TextFieldDefaults.Container(
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
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * If apart from input text change you also want to observe the cursor location, selection range, or
 * IME composition use the TextField overload with the [TextFieldValue] parameter instead.
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
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
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
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
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
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
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
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
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
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
        )
    }
}

/**
 * [Material Design filled text field](https://m3.material.io/components/text-fields/overview)
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * This overload provides access to the input text, cursor position, selection range and IME
 * composition. If you only want to observe an input text change, use the TextField overload with
 * the [String] parameter instead.
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
 * @param visualTransformation transforms the visual representation of the input [value]. For
 *   example, you can use
 *   [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 *   create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 *   [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is
 *   called. Note that this IME action may be different from what you specified in
 *   [KeyboardOptions.imeAction].
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
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 *   in different states. See [TextFieldDefaults.colors].
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    shape: Shape = TextFieldDefaults.shape,
    colors: TextFieldColors = TextFieldDefaults.colors(),
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
                    .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                    .defaultMinSize(
                        minWidth = TextFieldDefaults.MinWidth,
                        minHeight = TextFieldDefaults.MinHeight,
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
                    // places leading icon, text field with label and placeholder, trailing icon
                    TextFieldDefaults.DecorationBox(
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
                        shape = shape,
                        singleLine = singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = colors,
                    )
                },
        )
    }
}

internal data class IndicatorLineElement(
    val enabled: Boolean,
    val isError: Boolean,
    val interactionSource: InteractionSource,
    val colors: TextFieldColors?,
    val textFieldShape: Shape?,
    val focusedIndicatorLineThickness: Dp,
    val unfocusedIndicatorLineThickness: Dp,
) : ModifierNodeElement<IndicatorLineNode>() {
    override fun create(): IndicatorLineNode {
        return IndicatorLineNode(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            textFieldShape = textFieldShape,
            focusedIndicatorWidth = focusedIndicatorLineThickness,
            unfocusedIndicatorWidth = unfocusedIndicatorLineThickness,
        )
    }

    override fun update(node: IndicatorLineNode) {
        node.update(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            textFieldShape = textFieldShape,
            focusedIndicatorWidth = focusedIndicatorLineThickness,
            unfocusedIndicatorWidth = unfocusedIndicatorLineThickness,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "indicatorLine"
        properties["enabled"] = enabled
        properties["isError"] = isError
        properties["interactionSource"] = interactionSource
        properties["colors"] = colors
        properties["textFieldShape"] = textFieldShape
        properties["focusedIndicatorLineThickness"] = focusedIndicatorLineThickness
        properties["unfocusedIndicatorLineThickness"] = unfocusedIndicatorLineThickness
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class IndicatorLineNode(
    private var enabled: Boolean,
    private var isError: Boolean,
    private var interactionSource: InteractionSource,
    colors: TextFieldColors?,
    textFieldShape: Shape?,
    private var focusedIndicatorWidth: Dp,
    private var unfocusedIndicatorWidth: Dp,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    private var focused = false
    private var trackFocusStateJob: Job? = null

    private var _colors: TextFieldColors? = colors
    private val colors: TextFieldColors
        get() =
            _colors
                ?: currentValueOf(LocalMaterialTheme)
                    .colorScheme
                    .defaultTextFieldColors(currentValueOf(LocalTextSelectionColors))

    // Must be initialized in `onAttach` so `colors` can read from the `MaterialTheme`
    private var colorAnimatable: Animatable<Color, AnimationVector4D>? = null

    private var _shape: Shape? = textFieldShape
        private set(value) {
            if (field != value) {
                field = value
                drawWithCacheModifierNode.invalidateDrawCache()
            }
        }

    private val shape: Shape
        get() =
            _shape
                ?: currentValueOf(LocalMaterialTheme)
                    .shapes
                    .fromToken(FilledTextFieldTokens.ContainerShape)

    private val widthAnimatable: Animatable<Dp, AnimationVector1D> =
        Animatable(
            initialValue =
                if (focused && this.enabled) this.focusedIndicatorWidth
                else this.unfocusedIndicatorWidth,
            typeConverter = Dp.VectorConverter,
        )

    fun update(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors?,
        textFieldShape: Shape?,
        focusedIndicatorWidth: Dp,
        unfocusedIndicatorWidth: Dp,
    ) {
        var shouldInvalidate = false

        if (this.enabled != enabled) {
            this.enabled = enabled
            shouldInvalidate = true
        }

        if (this.isError != isError) {
            this.isError = isError
            shouldInvalidate = true
        }

        if (this.interactionSource !== interactionSource) {
            this.interactionSource = interactionSource
            trackFocusStateJob?.cancel()
            trackFocusStateJob = coroutineScope.launch { trackFocusState() }
        }

        if (this._colors != colors) {
            this._colors = colors
            shouldInvalidate = true
        }

        if (this._shape != textFieldShape) {
            this._shape = textFieldShape
            shouldInvalidate = true
        }

        if (this.focusedIndicatorWidth != focusedIndicatorWidth) {
            this.focusedIndicatorWidth = focusedIndicatorWidth
            shouldInvalidate = true
        }

        if (this.unfocusedIndicatorWidth != unfocusedIndicatorWidth) {
            this.unfocusedIndicatorWidth = unfocusedIndicatorWidth
            shouldInvalidate = true
        }

        if (shouldInvalidate) {
            invalidateIndicator()
        }
    }

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun onAttach() {
        trackFocusStateJob = coroutineScope.launch { trackFocusState() }
        if (colorAnimatable == null) {
            val initialColor = colors.indicatorColor(enabled, isError, focused)
            colorAnimatable =
                Animatable(
                    initialValue = initialColor,
                    typeConverter = Color.VectorConverter(initialColor.colorSpace),
                )
        }
    }

    /** Copied from [InteractionSource.collectIsFocusedAsState] */
    private suspend fun trackFocusState() {
        focused = false
        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction.Focus -> focusInteractions.add(interaction)
                is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
            }
            val isFocused = focusInteractions.isNotEmpty()
            if (isFocused != focused) {
                focused = isFocused
                invalidateIndicator()
            }
        }
    }

    private fun invalidateIndicator() {
        coroutineScope.launch {
            colorAnimatable?.animateTo(
                targetValue = colors.indicatorColor(enabled, isError, focused),
                animationSpec =
                    if (enabled) {
                        currentValueOf(LocalMaterialTheme)
                            .motionScheme
                            .fromToken<Color>(MotionSchemeKeyTokens.FastEffects)
                    } else {
                        snap()
                    },
            )
        }
        coroutineScope.launch {
            widthAnimatable.animateTo(
                targetValue =
                    if (focused && enabled) focusedIndicatorWidth else unfocusedIndicatorWidth,
                animationSpec =
                    if (enabled) {
                        currentValueOf(LocalMaterialTheme)
                            .motionScheme
                            .fromToken<Dp>(MotionSchemeKeyTokens.FastSpatial)
                    } else {
                        snap()
                    },
            )
        }
    }

    private val drawWithCacheModifierNode =
        delegate(
            CacheDrawModifierNode {
                val strokeWidth = widthAnimatable.value.toPx()
                val textFieldShapePath =
                    Path().apply {
                        addOutline(
                            this@IndicatorLineNode.shape.createOutline(
                                size,
                                layoutDirection,
                                density = this@CacheDrawModifierNode,
                            )
                        )
                    }
                val linePath =
                    Path().apply {
                        addRect(
                            Rect(
                                left = 0f,
                                top = size.height - strokeWidth,
                                right = size.width,
                                bottom = size.height,
                            )
                        )
                    }
                val clippedLine = linePath and textFieldShapePath

                onDrawWithContent {
                    drawContent()
                    drawPath(path = clippedLine, brush = SolidColor(colorAnimatable!!.value))
                }
            }
        )
}

/** Padding from text field top to label top, and from input field bottom to text field bottom */
/*@VisibleForTesting*/
internal val TextFieldWithLabelVerticalPadding = 8.dp
