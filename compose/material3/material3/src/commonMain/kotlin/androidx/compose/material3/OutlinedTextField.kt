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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.internal.AboveLabelBottomPadding
import androidx.compose.material3.internal.AboveLabelHorizontalPadding
import androidx.compose.material3.internal.ContainerId
import androidx.compose.material3.internal.HorizontalIconPadding
import androidx.compose.material3.internal.IconDefaultSizeModifier
import androidx.compose.material3.internal.LabelId
import androidx.compose.material3.internal.LeadingId
import androidx.compose.material3.internal.MinFocusedLabelLineHeight
import androidx.compose.material3.internal.MinSupportingTextLineHeight
import androidx.compose.material3.internal.MinTextLineHeight
import androidx.compose.material3.internal.PlaceholderId
import androidx.compose.material3.internal.PrefixId
import androidx.compose.material3.internal.PrefixSuffixTextPadding
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.SuffixId
import androidx.compose.material3.internal.SupportingId
import androidx.compose.material3.internal.TextFieldId
import androidx.compose.material3.internal.TrailingId
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.layoutId
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.TypeScaleTokens
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * <a href="https://m3.material.io/components/text-fields/overview" class="external"
 * target="_blank">Material Design outlined text field</a>.
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
 *   [OutlinedTextFieldDefaults.contentPadding].
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
    labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Default(),
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
    contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
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

    val density = LocalDensity.current

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            state = state,
            modifier =
                modifier
                    .then(
                        if (label != null && labelPosition !is TextFieldLabelPosition.Above) {
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
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight
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
                    }
                )
        )
    }
}

/**
 * <a href="https://m3.material.io/components/text-fields/overview" class="external"
 * target="_blank">Material Design outlined text field</a>.
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
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
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

    val density = LocalDensity.current

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
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
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight
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
                        }
                    )
                }
        )
    }
}

/**
 * <a href="https://m3.material.io/components/text-fields/overview" class="external"
 * target="_blank">Material Design outlined text field</a>.
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
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
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

    val density = LocalDensity.current

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
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
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight
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
                        }
                    )
                }
        )
    }
}

/**
 * Layout of the leading and trailing icons and the text field, label and placeholder in
 * [OutlinedTextField]. It doesn't use Row to position the icons and middle part because label
 * should not be positioned in the middle part.
 */
@Composable
internal fun OutlinedTextFieldLayout(
    modifier: Modifier,
    textField: @Composable () -> Unit,
    placeholder: @Composable ((Modifier) -> Unit)?,
    label: @Composable (() -> Unit)?,
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    singleLine: Boolean,
    labelPosition: TextFieldLabelPosition,
    labelProgress: Float,
    onLabelMeasured: (Size) -> Unit,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues
) {
    val measurePolicy =
        remember(onLabelMeasured, singleLine, labelPosition, labelProgress, paddingValues) {
            OutlinedTextFieldMeasurePolicy(
                onLabelMeasured,
                singleLine,
                labelPosition,
                labelProgress,
                paddingValues
            )
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            container()

            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val startPadding =
                if (leading != null) {
                    (startTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    startTextFieldPadding
                }
            val endPadding =
                if (trailing != null) {
                    (endTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    endTextFieldPadding
                }

            if (prefix != null) {
                Box(
                    Modifier.layoutId(PrefixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = startPadding, end = PrefixSuffixTextPadding)
                ) {
                    prefix()
                }
            }
            if (suffix != null) {
                Box(
                    Modifier.layoutId(SuffixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = PrefixSuffixTextPadding, end = endPadding)
                ) {
                    suffix()
                }
            }

            val textPadding =
                Modifier.heightIn(min = MinTextLineHeight)
                    .wrapContentHeight()
                    .padding(
                        start = if (prefix == null) startPadding else 0.dp,
                        end = if (suffix == null) endPadding else 0.dp,
                    )

            if (placeholder != null) {
                placeholder(Modifier.layoutId(PlaceholderId).then(textPadding))
            }

            Box(
                modifier = Modifier.layoutId(TextFieldId).then(textPadding),
                propagateMinConstraints = true
            ) {
                textField()
            }

            val labelPadding =
                if (labelPosition is TextFieldLabelPosition.Above) {
                    Modifier.padding(
                        start = AboveLabelHorizontalPadding,
                        end = AboveLabelHorizontalPadding,
                        bottom = AboveLabelBottomPadding,
                    )
                } else {
                    Modifier
                }

            if (label != null) {
                Box(
                    Modifier.heightIn(
                            min = lerp(MinTextLineHeight, MinFocusedLabelLineHeight, labelProgress)
                        )
                        .wrapContentHeight()
                        .layoutId(LabelId)
                        .then(labelPadding)
                ) {
                    label()
                }
            }

            if (supporting != null) {
                Box(
                    Modifier.layoutId(SupportingId)
                        .heightIn(min = MinSupportingTextLineHeight)
                        .wrapContentHeight()
                        .padding(TextFieldDefaults.supportingTextPadding())
                ) {
                    supporting()
                }
            }
        },
        measurePolicy = measurePolicy
    )
}

private class OutlinedTextFieldMeasurePolicy(
    private val onLabelMeasured: (Size) -> Unit,
    private val singleLine: Boolean,
    private val labelPosition: TextFieldLabelPosition,
    private val labelProgress: Float,
    private val paddingValues: PaddingValues
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        var occupiedSpaceHorizontally = 0
        var occupiedSpaceVertically = 0
        val bottomPadding = paddingValues.calculateBottomPadding().roundToPx()

        val relaxedConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(relaxedConstraints)
        occupiedSpaceHorizontally += leadingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, leadingPlaceable.heightOrZero)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += trailingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, trailingPlaceable.heightOrZero)

        // measure prefix
        val prefixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += prefixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, prefixPlaceable.heightOrZero)

        // measure suffix
        val suffixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.measure(relaxedConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += suffixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, suffixPlaceable.heightOrZero)

        // measure label
        val isLabelAbove = labelPosition is TextFieldLabelPosition.Above
        val labelMeasurable = measurables.fastFirstOrNull { it.layoutId == LabelId }
        var labelPlaceable: Placeable? = null
        val labelIntrinsicHeight: Int
        if (!isLabelAbove) {
            // if label is not Above, we can measure it like normal
            val totalHorizontalPadding =
                paddingValues.calculateLeftPadding(layoutDirection).roundToPx() +
                    paddingValues.calculateRightPadding(layoutDirection).roundToPx()
            val labelHorizontalConstraintOffset =
                lerp(
                    occupiedSpaceHorizontally + totalHorizontalPadding, // label in middle
                    totalHorizontalPadding, // label in outline
                    labelProgress,
                )
            val labelConstraints =
                relaxedConstraints.offset(
                    horizontal = -labelHorizontalConstraintOffset,
                    vertical = -bottomPadding
                )
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
            val labelSize =
                labelPlaceable?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
            onLabelMeasured(labelSize)
            labelIntrinsicHeight = 0
        } else {
            // if label is Above, it must be measured after other elements, but we
            // reserve space for it using its intrinsic height as a heuristic
            labelIntrinsicHeight = labelMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0
        }

        // supporting text must be measured after other elements, but we
        // reserve space for it using its intrinsic height as a heuristic
        val supportingMeasurable = measurables.fastFirstOrNull { it.layoutId == SupportingId }
        val supportingIntrinsicHeight =
            supportingMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0

        // measure text field
        val topPadding =
            if (isLabelAbove) {
                paddingValues.calculateTopPadding().roundToPx()
            } else {
                max(
                    labelPlaceable.heightOrZero / 2,
                    paddingValues.calculateTopPadding().roundToPx()
                )
            }
        val textConstraints =
            constraints
                .offset(
                    horizontal = -occupiedSpaceHorizontally,
                    vertical =
                        -bottomPadding -
                            topPadding -
                            labelIntrinsicHeight -
                            supportingIntrinsicHeight
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

        occupiedSpaceVertically =
            max(
                occupiedSpaceVertically,
                max(textFieldPlaceable.heightOrZero, placeholderPlaceable.heightOrZero) +
                    topPadding +
                    bottomPadding
            )

        val width =
            calculateWidth(
                leadingPlaceableWidth = leadingPlaceable.widthOrZero,
                trailingPlaceableWidth = trailingPlaceable.widthOrZero,
                prefixPlaceableWidth = prefixPlaceable.widthOrZero,
                suffixPlaceableWidth = suffixPlaceable.widthOrZero,
                textFieldPlaceableWidth = textFieldPlaceable.width,
                labelPlaceableWidth = labelPlaceable.widthOrZero,
                placeholderPlaceableWidth = placeholderPlaceable.widthOrZero,
                constraints = constraints,
            )

        if (isLabelAbove) {
            // now that we know the width, measure label
            val labelConstraints =
                relaxedConstraints.copy(maxHeight = labelIntrinsicHeight, maxWidth = width)
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
            val labelSize =
                labelPlaceable?.let { Size(it.width.toFloat(), it.height.toFloat()) } ?: Size.Zero
            onLabelMeasured(labelSize)
        }

        // measure supporting text
        val supportingConstraints =
            relaxedConstraints
                .offset(vertical = -occupiedSpaceVertically)
                .copy(minHeight = 0, maxWidth = width)
        val supportingPlaceable = supportingMeasurable?.measure(supportingConstraints)
        val supportingHeight = supportingPlaceable.heightOrZero

        val totalHeight =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                prefixHeight = prefixPlaceable.heightOrZero,
                suffixHeight = suffixPlaceable.heightOrZero,
                textFieldHeight = textFieldPlaceable.height,
                labelHeight = labelPlaceable.heightOrZero,
                placeholderHeight = placeholderPlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                constraints = constraints,
                isLabelAbove = isLabelAbove,
            )
        val height =
            totalHeight - supportingHeight - (if (isLabelAbove) labelPlaceable.heightOrZero else 0)

        val containerPlaceable =
            measurables
                .fastFirst { it.layoutId == ContainerId }
                .measure(
                    Constraints(
                        minWidth = if (width != Constraints.Infinity) width else 0,
                        maxWidth = width,
                        minHeight = if (height != Constraints.Infinity) height else 0,
                        maxHeight = height
                    )
                )
        return layout(width, totalHeight) {
            place(
                totalHeight = totalHeight,
                width = width,
                leadingPlaceable = leadingPlaceable,
                trailingPlaceable = trailingPlaceable,
                prefixPlaceable = prefixPlaceable,
                suffixPlaceable = suffixPlaceable,
                textFieldPlaceable = textFieldPlaceable,
                labelPlaceable = labelPlaceable,
                placeholderPlaceable = placeholderPlaceable,
                containerPlaceable = containerPlaceable,
                supportingPlaceable = supportingPlaceable,
                density = density,
                layoutDirection = layoutDirection,
                isLabelAbove = isLabelAbove,
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
        val prefixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val suffixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val placeholderWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        return calculateWidth(
            leadingPlaceableWidth = leadingWidth,
            trailingPlaceableWidth = trailingWidth,
            prefixPlaceableWidth = prefixWidth,
            suffixPlaceableWidth = suffixWidth,
            textFieldPlaceableWidth = textFieldWidth,
            labelPlaceableWidth = labelWidth,
            placeholderPlaceableWidth = placeholderWidth,
            constraints = Constraints(),
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
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0
        val trailingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.let {
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    intrinsicMeasurer(it, width)
                } ?: 0

        val labelHeight =
            measurables
                .fastFirstOrNull { it.layoutId == LabelId }
                ?.let { intrinsicMeasurer(it, lerp(remainingWidth, width, labelProgress)) } ?: 0

        val prefixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0
        val suffixHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.let {
                    val height = intrinsicMeasurer(it, remainingWidth)
                    remainingWidth =
                        remainingWidth.subtractConstraintSafely(
                            it.maxIntrinsicWidth(Constraints.Infinity)
                        )
                    height
                } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)

        val placeholderHeight =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

        val supportingHeight =
            measurables
                .fastFirstOrNull { it.layoutId == SupportingId }
                ?.let { intrinsicMeasurer(it, width) } ?: 0

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            textFieldHeight = textFieldHeight,
            labelHeight = labelHeight,
            placeholderHeight = placeholderHeight,
            supportingHeight = supportingHeight,
            constraints = Constraints(),
            isLabelAbove = labelPosition is TextFieldLabelPosition.Above,
        )
    }

    /**
     * Calculate the width of the [OutlinedTextField] given all elements that should be placed
     * inside.
     */
    private fun Density.calculateWidth(
        leadingPlaceableWidth: Int,
        trailingPlaceableWidth: Int,
        prefixPlaceableWidth: Int,
        suffixPlaceableWidth: Int,
        textFieldPlaceableWidth: Int,
        labelPlaceableWidth: Int,
        placeholderPlaceableWidth: Int,
        constraints: Constraints,
    ): Int {
        val affixTotalWidth = prefixPlaceableWidth + suffixPlaceableWidth
        val middleSection =
            maxOf(
                textFieldPlaceableWidth + affixTotalWidth,
                placeholderPlaceableWidth + affixTotalWidth,
                // Prefix/suffix does not get applied to label
                lerp(labelPlaceableWidth, 0, labelProgress),
            )
        val wrappedWidth = leadingPlaceableWidth + middleSection + trailingPlaceableWidth

        // Actual LayoutDirection doesn't matter; we only need the sum
        val labelHorizontalPadding =
            (paddingValues.calculateLeftPadding(LayoutDirection.Ltr) +
                    paddingValues.calculateRightPadding(LayoutDirection.Ltr))
                .toPx()
        val focusedLabelWidth =
            ((labelPlaceableWidth + labelHorizontalPadding) * labelProgress).roundToInt()
        return maxOf(wrappedWidth, focusedLabelWidth, constraints.minWidth)
    }

    /**
     * Calculate the height of the [OutlinedTextField] given all elements that should be placed
     * inside. This includes the supporting text, if it exists, even though this element is not
     * "visually" inside the text field.
     */
    private fun Density.calculateHeight(
        leadingHeight: Int,
        trailingHeight: Int,
        prefixHeight: Int,
        suffixHeight: Int,
        textFieldHeight: Int,
        labelHeight: Int,
        placeholderHeight: Int,
        supportingHeight: Int,
        constraints: Constraints,
        isLabelAbove: Boolean,
    ): Int {
        val inputFieldHeight =
            maxOf(
                textFieldHeight,
                placeholderHeight,
                prefixHeight,
                suffixHeight,
                if (isLabelAbove) 0 else lerp(labelHeight, 0, labelProgress),
            )
        val topPadding = paddingValues.calculateTopPadding().toPx()
        val actualTopPadding =
            if (isLabelAbove) {
                topPadding
            } else {
                lerp(topPadding, max(topPadding, labelHeight / 2f), labelProgress)
            }
        val bottomPadding = paddingValues.calculateBottomPadding().toPx()
        val middleSectionHeight = actualTopPadding + inputFieldHeight + bottomPadding

        return max(
            constraints.minHeight,
            (if (isLabelAbove) labelHeight else 0) +
                maxOf(leadingHeight, trailingHeight, middleSectionHeight.roundToInt()) +
                supportingHeight
        )
    }

    /**
     * Places the provided text field, placeholder, label, optional leading and trailing icons
     * inside the [OutlinedTextField]
     */
    private fun Placeable.PlacementScope.place(
        totalHeight: Int,
        width: Int,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        prefixPlaceable: Placeable?,
        suffixPlaceable: Placeable?,
        textFieldPlaceable: Placeable,
        labelPlaceable: Placeable?,
        placeholderPlaceable: Placeable?,
        containerPlaceable: Placeable,
        supportingPlaceable: Placeable?,
        density: Float,
        layoutDirection: LayoutDirection,
        isLabelAbove: Boolean,
    ) {
        val yOffset = if (isLabelAbove) labelPlaceable.heightOrZero else 0

        // place container
        containerPlaceable.place(0, yOffset)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the label (if it's Above) and the supporting text on bottom
        val height =
            totalHeight -
                supportingPlaceable.heightOrZero -
                (if (isLabelAbove) labelPlaceable.heightOrZero else 0)

        val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

        // placed center vertically and to the start edge horizontally
        leadingPlaceable?.placeRelative(
            0,
            yOffset + Alignment.CenterVertically.align(leadingPlaceable.height, height)
        )

        // label position is animated
        // in single line text field, label is centered vertically before animation starts
        labelPlaceable?.let {
            val startY =
                when {
                    isLabelAbove -> 0
                    singleLine -> Alignment.CenterVertically.align(it.height, height)
                    else -> topPadding
                }
            val endY =
                when {
                    isLabelAbove -> 0
                    else -> -(it.height / 2)
                }
            val positionY = lerp(startY, endY, labelProgress)

            if (isLabelAbove) {
                val positionX =
                    labelPosition.minimizedAlignment.align(
                        size = labelPlaceable.width,
                        space = width,
                        layoutDirection = layoutDirection,
                    )
                // Not placeRelative because alignment already handles RTL
                labelPlaceable.place(positionX, positionY)
            } else {
                val startPadding =
                    paddingValues.calculateStartPadding(layoutDirection).value * density
                val endPadding = paddingValues.calculateEndPadding(layoutDirection).value * density
                val iconPadding = HorizontalIconPadding.value * density
                val leadingPlusPadding =
                    if (leadingPlaceable == null) {
                        startPadding
                    } else {
                        leadingPlaceable.width + (startPadding - iconPadding).coerceAtLeast(0f)
                    }
                val trailingPlusPadding =
                    if (trailingPlaceable == null) {
                        endPadding
                    } else {
                        trailingPlaceable.width + (endPadding - iconPadding).coerceAtLeast(0f)
                    }
                val leftPadding =
                    if (layoutDirection == LayoutDirection.Ltr) startPadding else endPadding
                val leftIconPlusPadding =
                    if (layoutDirection == LayoutDirection.Ltr) leadingPlusPadding
                    else trailingPlusPadding
                val startX =
                    labelPosition.expandedAlignment.align(
                        size = labelPlaceable.width,
                        space = width - (leadingPlusPadding + trailingPlusPadding).roundToInt(),
                        layoutDirection = layoutDirection,
                    ) + leftIconPlusPadding

                val endX =
                    labelPosition.minimizedAlignment.align(
                        size = labelPlaceable.width,
                        space = width - (startPadding + endPadding).roundToInt(),
                        layoutDirection = layoutDirection
                    ) + leftPadding
                val positionX = lerp(startX, endX, labelProgress).roundToInt()
                // Not placeRelative because alignment already handles RTL
                labelPlaceable.place(positionX, positionY)
            }
        }

        fun calculateVerticalPosition(placeable: Placeable): Int {
            val defaultPosition =
                yOffset +
                    if (singleLine) {
                        // Single line text fields have text components centered vertically.
                        Alignment.CenterVertically.align(placeable.height, height)
                    } else {
                        // Multiline text fields have text components aligned to top with padding.
                        topPadding
                    }
            return if (labelPosition is TextFieldLabelPosition.Default) {
                // Ensure components are placed below label when it's in the border
                max(defaultPosition, labelPlaceable.heightOrZero / 2)
            } else {
                defaultPosition
            }
        }

        prefixPlaceable?.placeRelative(
            leadingPlaceable.widthOrZero,
            calculateVerticalPosition(prefixPlaceable)
        )

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero

        textFieldPlaceable.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(textFieldPlaceable)
        )

        // placed similar to the input text above
        placeholderPlaceable?.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(placeholderPlaceable)
        )

        suffixPlaceable?.placeRelative(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            calculateVerticalPosition(suffixPlaceable)
        )

        // placed center vertically and to the end edge horizontally
        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            yOffset + Alignment.CenterVertically.align(trailingPlaceable.height, height)
        )

        // place supporting text
        supportingPlaceable?.placeRelative(0, yOffset + height)
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

/**
 * In the focused state, the top half of the label sticks out above the text field. This default
 * padding is a best-effort approximation to keep the label from overlapping with the content above
 * it. It is sufficient when the label is a single line and developers do not override the label's
 * font size/style. Otherwise, developers will need to add additional padding themselves.
 */
internal val OutlinedTextFieldTopPadding = TypeScaleTokens.BodySmallLineHeight / 2
