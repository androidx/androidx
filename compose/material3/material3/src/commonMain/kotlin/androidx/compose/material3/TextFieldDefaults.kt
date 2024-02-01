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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.OutlinedTextFieldTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Contains the default values used by [TextField]. For defaults used in [OutlinedTextField], see
 * [OutlinedTextFieldDefaults].
 */
@Immutable
object TextFieldDefaults {
    /** Default shape for a [TextField]. */
    val shape: Shape @Composable get() = FilledTextFieldTokens.ContainerShape.value

    /**
     * The default min height applied to a [TextField].
     * Note that you can override it by applying Modifier.heightIn directly on a text field.
     */
    val MinHeight = 56.dp

    /**
     * The default min width applied to a [TextField].
     * Note that you can override it by applying Modifier.widthIn directly on a text field.
     */
    val MinWidth = 280.dp

    /**
     * The default thickness of the indicator line in [TextField] in unfocused state.
     */
    val UnfocusedIndicatorThickness = 1.dp

    /**
     * The default thickness of the indicator line in [TextField] in focused state.
     */
    val FocusedIndicatorThickness = 2.dp

    /**
     * Composable that draws a default container for the content of [TextField], with an indicator
     * line at the bottom. You can use it to draw a container for your custom text field based on
     * [TextFieldDefaults.DecorationBox]. [TextField] applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param shape shape of the container
     */
    @ExperimentalMaterial3Api
    @Composable
    fun ContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = TextFieldDefaults.shape,
    ) {
        Box(
            Modifier
                .background(colors.containerColor(enabled, isError, interactionSource).value, shape)
                .indicatorLine(enabled, isError, interactionSource, colors))
    }

    /**
     * A modifier to draw a default bottom indicator line in [TextField]. You can use this modifier
     * if you build your custom text field using [TextFieldDefaults.DecorationBox] whilst the
     * [TextField] applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param focusedIndicatorLineThickness thickness of the indicator line when text field is
     * focused
     * @param unfocusedIndicatorLineThickness thickness of the indicator line when text field is
     * not focused
     */
    @ExperimentalMaterial3Api
    fun Modifier.indicatorLine(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        focusedIndicatorLineThickness: Dp = FocusedIndicatorThickness,
        unfocusedIndicatorLineThickness: Dp = UnfocusedIndicatorThickness
    ) = composed(inspectorInfo = debugInspectorInfo {
        name = "indicatorLine"
        properties["enabled"] = enabled
        properties["isError"] = isError
        properties["interactionSource"] = interactionSource
        properties["colors"] = colors
        properties["focusedIndicatorLineThickness"] = focusedIndicatorLineThickness
        properties["unfocusedIndicatorLineThickness"] = unfocusedIndicatorLineThickness
    }) {
        val stroke = animateBorderStrokeAsState(
            enabled,
            isError,
            interactionSource,
            colors,
            focusedIndicatorLineThickness,
            unfocusedIndicatorLineThickness
        )
        Modifier.drawIndicatorLine(stroke.value)
    }

    /**
     * Default content padding applied to [TextField] when there is a label.
     *
     * Note that when the label is present, the "top" padding is a distance between the top edge of
     * the [TextField] and the top of the label, not to the top of the input field. The input field
     * is placed directly beneath the label.
     *
     * See [PaddingValues] for more details.
     */
    fun contentPaddingWithLabel(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = TextFieldWithLabelVerticalPadding,
        bottom: Dp = TextFieldWithLabelVerticalPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default content padding applied to [TextField] when the label is null.
     * See [PaddingValues] for more details.
     */
    fun contentPaddingWithoutLabel(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default padding applied to supporting text for both [TextField] and [OutlinedTextField].
     * See [PaddingValues] for more details.
     */
    // TODO(246775477): consider making this public
    @ExperimentalMaterial3Api
    internal fun supportingTextPadding(
        start: Dp = TextFieldPadding,
        top: Dp = SupportingTopPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = 0.dp,
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField].
     */
    @Composable
    fun colors() = MaterialTheme.colorScheme.defaultTextFieldColors

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedIndicatorColor the indicator color for this text field when focused
     * @param unfocusedIndicatorColor the indicator color for this text field when not focused
     * @param disabledIndicatorColor the indicator color for this text field when disabled
     * @param errorIndicatorColor the indicator color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     * focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedSupportingTextColor the supporting text color for this text field when focused
     * @param unfocusedSupportingTextColor the supporting text color for this text field when not
     * focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     * disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     * state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun colors(
        focusedTextColor: Color = Color.Unspecified,
        unfocusedTextColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        errorTextColor: Color = Color.Unspecified,
        focusedContainerColor: Color = Color.Unspecified,
        unfocusedContainerColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        errorContainerColor: Color = Color.Unspecified,
        cursorColor: Color = Color.Unspecified,
        errorCursorColor: Color = Color.Unspecified,
        selectionColors: TextSelectionColors? = null,
        focusedIndicatorColor: Color = Color.Unspecified,
        unfocusedIndicatorColor: Color = Color.Unspecified,
        disabledIndicatorColor: Color = Color.Unspecified,
        errorIndicatorColor: Color = Color.Unspecified,
        focusedLeadingIconColor: Color = Color.Unspecified,
        unfocusedLeadingIconColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        errorLeadingIconColor: Color = Color.Unspecified,
        focusedTrailingIconColor: Color = Color.Unspecified,
        unfocusedTrailingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        errorTrailingIconColor: Color = Color.Unspecified,
        focusedLabelColor: Color = Color.Unspecified,
        unfocusedLabelColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        errorLabelColor: Color = Color.Unspecified,
        focusedPlaceholderColor: Color = Color.Unspecified,
        unfocusedPlaceholderColor: Color = Color.Unspecified,
        disabledPlaceholderColor: Color = Color.Unspecified,
        errorPlaceholderColor: Color = Color.Unspecified,
        focusedSupportingTextColor: Color = Color.Unspecified,
        unfocusedSupportingTextColor: Color = Color.Unspecified,
        disabledSupportingTextColor: Color = Color.Unspecified,
        errorSupportingTextColor: Color = Color.Unspecified,
        focusedPrefixColor: Color = Color.Unspecified,
        unfocusedPrefixColor: Color = Color.Unspecified,
        disabledPrefixColor: Color = Color.Unspecified,
        errorPrefixColor: Color = Color.Unspecified,
        focusedSuffixColor: Color = Color.Unspecified,
        unfocusedSuffixColor: Color = Color.Unspecified,
        disabledSuffixColor: Color = Color.Unspecified,
        errorSuffixColor: Color = Color.Unspecified,
    ): TextFieldColors = MaterialTheme.colorScheme.defaultTextFieldColors.copy(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            textSelectionColors = selectionColors,
            focusedIndicatorColor = focusedIndicatorColor,
            unfocusedIndicatorColor = unfocusedIndicatorColor,
            disabledIndicatorColor = disabledIndicatorColor,
            errorIndicatorColor = errorIndicatorColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedSupportingTextColor = focusedSupportingTextColor,
            unfocusedSupportingTextColor = unfocusedSupportingTextColor,
            disabledSupportingTextColor = disabledSupportingTextColor,
            errorSupportingTextColor = errorSupportingTextColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    internal val ColorScheme.defaultTextFieldColors: TextFieldColors
        @Composable
        get() {
            return defaultTextFieldColorsCached ?: TextFieldColors(
                focusedTextColor = fromToken(FilledTextFieldTokens.FocusInputColor),
                unfocusedTextColor = fromToken(FilledTextFieldTokens.InputColor),
                disabledTextColor = fromToken(FilledTextFieldTokens.DisabledInputColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                errorTextColor = fromToken(FilledTextFieldTokens.ErrorInputColor),
                focusedContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                unfocusedContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                disabledContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                errorContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                cursorColor = fromToken(FilledTextFieldTokens.CaretColor),
                errorCursorColor = fromToken(FilledTextFieldTokens.ErrorFocusCaretColor),
                textSelectionColors = LocalTextSelectionColors.current,
                focusedIndicatorColor = fromToken(FilledTextFieldTokens.FocusActiveIndicatorColor),
                unfocusedIndicatorColor = fromToken(FilledTextFieldTokens.ActiveIndicatorColor),
                disabledIndicatorColor =
                fromToken(FilledTextFieldTokens.DisabledActiveIndicatorColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledActiveIndicatorOpacity),
                errorIndicatorColor = fromToken(FilledTextFieldTokens.ErrorActiveIndicatorColor),
                focusedLeadingIconColor = fromToken(FilledTextFieldTokens.FocusLeadingIconColor),
                unfocusedLeadingIconColor = fromToken(FilledTextFieldTokens.LeadingIconColor),
                disabledLeadingIconColor = fromToken(FilledTextFieldTokens.DisabledLeadingIconColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
                errorLeadingIconColor = fromToken(FilledTextFieldTokens.ErrorLeadingIconColor),
                focusedTrailingIconColor = fromToken(FilledTextFieldTokens.FocusTrailingIconColor),
                unfocusedTrailingIconColor = fromToken(FilledTextFieldTokens.TrailingIconColor),
                disabledTrailingIconColor =
                fromToken(FilledTextFieldTokens.DisabledTrailingIconColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
                errorTrailingIconColor = fromToken(FilledTextFieldTokens.ErrorTrailingIconColor),
                focusedLabelColor = fromToken(FilledTextFieldTokens.FocusLabelColor),
                unfocusedLabelColor = fromToken(FilledTextFieldTokens.LabelColor),
                disabledLabelColor = fromToken(FilledTextFieldTokens.DisabledLabelColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledLabelOpacity),
                errorLabelColor = fromToken(FilledTextFieldTokens.ErrorLabelColor),
                focusedPlaceholderColor = fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                unfocusedPlaceholderColor = fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                disabledPlaceholderColor = fromToken(FilledTextFieldTokens.DisabledInputColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                errorPlaceholderColor = fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                focusedSupportingTextColor = fromToken(FilledTextFieldTokens.FocusSupportingColor),
                unfocusedSupportingTextColor = fromToken(FilledTextFieldTokens.SupportingColor),
                disabledSupportingTextColor =
                fromToken(FilledTextFieldTokens.DisabledSupportingColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
                errorSupportingTextColor = fromToken(FilledTextFieldTokens.ErrorSupportingColor),
                focusedPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                unfocusedPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                disabledPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                errorPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                focusedSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
                unfocusedSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
                disabledSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor)
                    .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                errorSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
            ).also {
                defaultTextFieldColorsCached = it
            }
        }

    /**
     * A decoration box which helps creating custom text fields based on
     * <a href="https://material.io/components/text-fields#filled-text-field" class="external" target="_blank">Material Design filled text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [TextField],
     * consider using this decoration box to achieve the desired design.
     *
     * For example, if you need to create a dense text field, use [contentPadding] parameter to
     * decrease the paddings around the input field. If you need to customise the bottom indicator,
     * apply [indicatorLine] modifier to achieve that.
     *
     * See example of using [DecorationBox] to build your own custom text field
     * @sample androidx.compose.material3.samples.CustomTextFieldBasedOnDecorationBox
     *
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     * the [BasicTextField]
     * @param enabled controls the enabled state of the text field. When `false`, this component
     * will not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services. You must also pass the same value to the [BasicTextField] for it to
     * adjust the behavior accordingly.
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. You
     * must pass the same value as to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     * [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     * then pass the same instance to this decoration box to observe [Interaction]s and customize
     * the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error
     * color.
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
     * [Typography.bodyLarge] when the text field is not in focus.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     * and the input text is empty. The default text style for internal [Text] is
     * [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     * field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param shape defines the shape of this text field's container
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [TextFieldDefaults.colors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [TextFieldDefaults.contentPaddingWithLabel] and
     * [TextFieldDefaults.contentPaddingWithoutLabel].
     * Note that if there's a label in the text field, the [top][PaddingValues.calculateTopPadding]
     * padding represents the distance from the top edge of the container to the top of the label.
     * Otherwise if label is null, it represents the distance from the top edge of the container to
     * the top of the input field. All other paddings represent the distance from the corresponding
     * edge of the container to the corresponding edge of the closest element.
     * @param container the container to be drawn behind the text field. By default, this includes
     * the bottom indicator line. Default colors for the container come from the [colors].
     */
    @Composable
    @ExperimentalMaterial3Api
    fun DecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        shape: Shape = TextFieldDefaults.shape,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues =
            if (label == null) {
                contentPaddingWithoutLabel()
            } else {
                contentPaddingWithLabel()
            },
        container: @Composable () -> Unit = {
            ContainerBox(enabled, isError, interactionSource, colors, shape)
        }
    ) {
        CommonDecorationBox(
            type = TextFieldType.Filled,
            value = value,
            innerTextField = innerTextField,
            visualTransformation = visualTransformation,
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
            contentPadding = contentPadding,
            container = container
        )
    }

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.shape`",
        replaceWith = ReplaceWith("OutlinedTextFieldDefaults.shape",
            "androidx.compose.material.OutlinedTextFieldDefaults"),
        level = DeprecationLevel.WARNING
    )
    val outlinedShape: Shape @Composable get() = OutlinedTextFieldDefaults.shape

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.shape`",
        replaceWith = ReplaceWith("TextFieldDefaults.shape"),
        level = DeprecationLevel.WARNING
    )
    val filledShape: Shape @Composable get() = shape

    @Deprecated(
        message = "Split into `TextFieldDefaults.UnfocusedIndicatorThickness` and " +
            "`OutlinedTextFieldDefaults.UnfocusedBorderThickness`. Please update as appropriate.",
        replaceWith = ReplaceWith("TextFieldDefaults.UnfocusedIndicatorThickness"),
        level = DeprecationLevel.WARNING,
    )
    val UnfocusedBorderThickness = UnfocusedIndicatorThickness

    @Deprecated(
        message = "Split into `TextFieldDefaults.FocusedIndicatorThickness` and " +
            "`OutlinedTextFieldDefaults.FocusedBorderThickness`. Please update as appropriate.",
        replaceWith = ReplaceWith("TextFieldDefaults.FocusedIndicatorThickness"),
        level = DeprecationLevel.WARNING,
    )
    val FocusedBorderThickness = FocusedIndicatorThickness

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.ContainerBox`",
        replaceWith = ReplaceWith("TextFieldDefaults.ContainerBox(\n" +
            "        enabled = enabled,\n" +
            "        isError = isError,\n" +
            "        interactionSource = interactionSource,\n" +
            "        colors = colors,\n" +
            "        shape = shape,\n" +
            "    )"),
        level = DeprecationLevel.WARNING
    )
    @ExperimentalMaterial3Api
    @Composable
    fun FilledContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = TextFieldDefaults.shape,
    ) = ContainerBox(
        enabled = enabled,
        isError = isError,
        interactionSource = interactionSource,
        colors = colors,
        shape = shape,
    )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.ContainerBox`",
        replaceWith = ReplaceWith("OutlinedTextFieldDefaults.ContainerBox(\n" +
            "        enabled = enabled,\n" +
            "        isError = isError,\n" +
            "        interactionSource = interactionSource,\n" +
            "        colors = colors,\n" +
            "        shape = shape,\n" +
            "        focusedBorderThickness = focusedBorderThickness,\n" +
            "        unfocusedBorderThickness = unfocusedBorderThickness,\n" +
            "    )",
            "androidx.compose.material.OutlinedTextFieldDefaults"),
        level = DeprecationLevel.WARNING
    )
    @ExperimentalMaterial3Api
    @Composable
    fun OutlinedBorderContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = OutlinedTextFieldTokens.ContainerShape.value,
        focusedBorderThickness: Dp = OutlinedTextFieldDefaults.FocusedBorderThickness,
        unfocusedBorderThickness: Dp = OutlinedTextFieldDefaults.UnfocusedBorderThickness
    ) = OutlinedTextFieldDefaults.ContainerBox(
        enabled = enabled,
        isError = isError,
        interactionSource = interactionSource,
        colors = colors,
        shape = shape,
        focusedBorderThickness = focusedBorderThickness,
        unfocusedBorderThickness = unfocusedBorderThickness,
    )

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.contentPaddingWithLabel`",
        replaceWith = ReplaceWith("TextFieldDefaults.contentPaddingWithLabel(\n" +
            "        start = start,\n" +
            "        top = top,\n" +
            "        end = end,\n" +
            "        bottom = bottom,\n" +
            "    )"),
        level = DeprecationLevel.WARNING
    )
    fun textFieldWithLabelPadding(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = TextFieldWithLabelVerticalPadding,
        bottom: Dp = TextFieldWithLabelVerticalPadding
    ): PaddingValues = contentPaddingWithLabel(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
    )

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.contentPaddingWithoutLabel`",
        replaceWith = ReplaceWith("TextFieldDefaults.contentPaddingWithoutLabel(\n" +
            "        start = start,\n" +
            "        top = top,\n" +
            "        end = end,\n" +
            "        bottom = bottom,\n" +
            "    )"),
        level = DeprecationLevel.WARNING
    )
    fun textFieldWithoutLabelPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = contentPaddingWithoutLabel(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
    )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.contentPadding`",
        replaceWith = ReplaceWith("OutlinedTextFieldDefaults.contentPadding(\n" +
            "        start = start,\n" +
            "        top = top,\n" +
            "        end = end,\n" +
            "        bottom = bottom,\n" +
            "    )",
            "androidx.compose.material.OutlinedTextFieldDefaults"),
        level = DeprecationLevel.WARNING
    )
    fun outlinedTextFieldPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = OutlinedTextFieldDefaults.contentPadding(
        start = start,
        top = top,
        end = end,
        bottom = bottom,
    )

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.colors` with additional parameters to control" +
            "container color based on state.",
        replaceWith = ReplaceWith("TextFieldDefaults.colors(\n" +
            "        focusedTextColor = focusedTextColor,\n" +
            "        unfocusedTextColor = unfocusedTextColor,\n" +
            "        disabledTextColor = disabledTextColor,\n" +
            "        errorTextColor = errorTextColor,\n" +
            "        focusedContainerColor = containerColor,\n" +
            "        unfocusedContainerColor = containerColor,\n" +
            "        disabledContainerColor = containerColor,\n" +
            "        errorContainerColor = errorContainerColor,\n" +
            "        cursorColor = cursorColor,\n" +
            "        errorCursorColor = errorCursorColor,\n" +
            "        selectionColors = selectionColors,\n" +
            "        focusedIndicatorColor = focusedIndicatorColor,\n" +
            "        unfocusedIndicatorColor = unfocusedIndicatorColor,\n" +
            "        disabledIndicatorColor = disabledIndicatorColor,\n" +
            "        errorIndicatorColor = errorIndicatorColor,\n" +
            "        focusedLeadingIconColor = focusedLeadingIconColor,\n" +
            "        unfocusedLeadingIconColor = unfocusedLeadingIconColor,\n" +
            "        disabledLeadingIconColor = disabledLeadingIconColor,\n" +
            "        errorLeadingIconColor = errorLeadingIconColor,\n" +
            "        focusedTrailingIconColor = focusedTrailingIconColor,\n" +
            "        unfocusedTrailingIconColor = unfocusedTrailingIconColor,\n" +
            "        disabledTrailingIconColor = disabledTrailingIconColor,\n" +
            "        errorTrailingIconColor = errorTrailingIconColor,\n" +
            "        focusedLabelColor = focusedLabelColor,\n" +
            "        unfocusedLabelColor = unfocusedLabelColor,\n" +
            "        disabledLabelColor = disabledLabelColor,\n" +
            "        errorLabelColor = errorLabelColor,\n" +
            "        focusedPlaceholderColor = focusedPlaceholderColor,\n" +
            "        unfocusedPlaceholderColor = unfocusedPlaceholderColor,\n" +
            "        disabledPlaceholderColor = disabledPlaceholderColor,\n" +
            "        errorPlaceholderColor = errorPlaceholderColor,\n" +
            "        focusedSupportingTextColor = focusedSupportingTextColor,\n" +
            "        unfocusedSupportingTextColor = unfocusedSupportingTextColor,\n" +
            "        disabledSupportingTextColor = disabledSupportingTextColor,\n" +
            "        errorSupportingTextColor = errorSupportingTextColor,\n" +
            "        focusedPrefixColor = focusedPrefixColor,\n" +
            "        unfocusedPrefixColor = unfocusedPrefixColor,\n" +
            "        disabledPrefixColor = disabledPrefixColor,\n" +
            "        errorPrefixColor = errorPrefixColor,\n" +
            "        focusedSuffixColor = focusedSuffixColor,\n" +
            "        unfocusedSuffixColor = unfocusedSuffixColor,\n" +
            "        disabledSuffixColor = disabledSuffixColor,\n" +
            "        errorSuffixColor = errorSuffixColor,\n" +
            "    )"),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3Api
    @Composable
    fun textFieldColors(
        focusedTextColor: Color = FilledTextFieldTokens.FocusInputColor.value,
        unfocusedTextColor: Color = FilledTextFieldTokens.InputColor.value,
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorTextColor: Color = FilledTextFieldTokens.ErrorInputColor.value,
        containerColor: Color = FilledTextFieldTokens.ContainerColor.value,
        errorContainerColor: Color = FilledTextFieldTokens.ContainerColor.value,
        cursorColor: Color = FilledTextFieldTokens.CaretColor.value,
        errorCursorColor: Color = FilledTextFieldTokens.ErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color = FilledTextFieldTokens.FocusActiveIndicatorColor.value,
        unfocusedIndicatorColor: Color = FilledTextFieldTokens.ActiveIndicatorColor.value,
        disabledIndicatorColor: Color = FilledTextFieldTokens.DisabledActiveIndicatorColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledActiveIndicatorOpacity),
        errorIndicatorColor: Color = FilledTextFieldTokens.ErrorActiveIndicatorColor.value,
        focusedLeadingIconColor: Color = FilledTextFieldTokens.FocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = FilledTextFieldTokens.LeadingIconColor.value,
        disabledLeadingIconColor: Color = FilledTextFieldTokens.DisabledLeadingIconColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = FilledTextFieldTokens.ErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color = FilledTextFieldTokens.FocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color = FilledTextFieldTokens.TrailingIconColor.value,
        disabledTrailingIconColor: Color = FilledTextFieldTokens.DisabledTrailingIconColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = FilledTextFieldTokens.ErrorTrailingIconColor.value,
        focusedLabelColor: Color = FilledTextFieldTokens.FocusLabelColor.value,
        unfocusedLabelColor: Color = FilledTextFieldTokens.LabelColor.value,
        disabledLabelColor: Color = FilledTextFieldTokens.DisabledLabelColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = FilledTextFieldTokens.ErrorLabelColor.value,
        focusedPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.value,
        unfocusedPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.value,
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPlaceholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.value,
        focusedSupportingTextColor: Color = FilledTextFieldTokens.FocusSupportingColor.value,
        unfocusedSupportingTextColor: Color = FilledTextFieldTokens.SupportingColor.value,
        disabledSupportingTextColor: Color = FilledTextFieldTokens.DisabledSupportingColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = FilledTextFieldTokens.ErrorSupportingColor.value,
        focusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        unfocusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        disabledPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        focusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
        unfocusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
        disabledSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
    ): TextFieldColors = colors(
        focusedTextColor = focusedTextColor,
        unfocusedTextColor = unfocusedTextColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = errorTextColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = errorContainerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedIndicatorColor = focusedIndicatorColor,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        disabledIndicatorColor = disabledIndicatorColor,
        errorIndicatorColor = errorIndicatorColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = focusedPlaceholderColor,
        unfocusedPlaceholderColor = unfocusedPlaceholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = errorPlaceholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.colors` with additional parameters to" +
            "control container color based on state.",
        replaceWith = ReplaceWith("OutlinedTextFieldDefaults.colors(\n" +
            "        focusedTextColor = focusedTextColor,\n" +
            "        unfocusedTextColor = unfocusedTextColor,\n" +
            "        disabledTextColor = disabledTextColor,\n" +
            "        errorTextColor = errorTextColor,\n" +
            "        focusedContainerColor = containerColor,\n" +
            "        unfocusedContainerColor = containerColor,\n" +
            "        disabledContainerColor = containerColor,\n" +
            "        errorContainerColor = errorContainerColor,\n" +
            "        cursorColor = cursorColor,\n" +
            "        errorCursorColor = errorCursorColor,\n" +
            "        selectionColors = selectionColors,\n" +
            "        focusedBorderColor = focusedBorderColor,\n" +
            "        unfocusedBorderColor = unfocusedBorderColor,\n" +
            "        disabledBorderColor = disabledBorderColor,\n" +
            "        errorBorderColor = errorBorderColor,\n" +
            "        focusedLeadingIconColor = focusedLeadingIconColor,\n" +
            "        unfocusedLeadingIconColor = unfocusedLeadingIconColor,\n" +
            "        disabledLeadingIconColor = disabledLeadingIconColor,\n" +
            "        errorLeadingIconColor = errorLeadingIconColor,\n" +
            "        focusedTrailingIconColor = focusedTrailingIconColor,\n" +
            "        unfocusedTrailingIconColor = unfocusedTrailingIconColor,\n" +
            "        disabledTrailingIconColor = disabledTrailingIconColor,\n" +
            "        errorTrailingIconColor = errorTrailingIconColor,\n" +
            "        focusedLabelColor = focusedLabelColor,\n" +
            "        unfocusedLabelColor = unfocusedLabelColor,\n" +
            "        disabledLabelColor = disabledLabelColor,\n" +
            "        errorLabelColor = errorLabelColor,\n" +
            "        focusedPlaceholderColor = focusedPlaceholderColor,\n" +
            "        unfocusedPlaceholderColor = unfocusedPlaceholderColor,\n" +
            "        disabledPlaceholderColor = disabledPlaceholderColor,\n" +
            "        errorPlaceholderColor = errorPlaceholderColor,\n" +
            "        focusedSupportingTextColor = focusedSupportingTextColor,\n" +
            "        unfocusedSupportingTextColor = unfocusedSupportingTextColor,\n" +
            "        disabledSupportingTextColor = disabledSupportingTextColor,\n" +
            "        errorSupportingTextColor = errorSupportingTextColor,\n" +
            "        focusedPrefixColor = focusedPrefixColor,\n" +
            "        unfocusedPrefixColor = unfocusedPrefixColor,\n" +
            "        disabledPrefixColor = disabledPrefixColor,\n" +
            "        errorPrefixColor = errorPrefixColor,\n" +
            "        focusedSuffixColor = focusedSuffixColor,\n" +
            "        unfocusedSuffixColor = unfocusedSuffixColor,\n" +
            "        disabledSuffixColor = disabledSuffixColor,\n" +
            "        errorSuffixColor = errorSuffixColor,\n" +
            "    )",
            "androidx.compose.material.OutlinedTextFieldDefaults"),
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3Api
    @Composable
    fun outlinedTextFieldColors(
        focusedTextColor: Color = OutlinedTextFieldTokens.FocusInputColor.value,
        unfocusedTextColor: Color = OutlinedTextFieldTokens.InputColor.value,
        disabledTextColor: Color = OutlinedTextFieldTokens.DisabledInputColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorTextColor: Color = OutlinedTextFieldTokens.ErrorInputColor.value,
        containerColor: Color = Color.Transparent,
        errorContainerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedTextFieldTokens.CaretColor.value,
        errorCursorColor: Color = OutlinedTextFieldTokens.ErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedTextFieldTokens.FocusOutlineColor.value,
        unfocusedBorderColor: Color = OutlinedTextFieldTokens.OutlineColor.value,
        disabledBorderColor: Color = OutlinedTextFieldTokens.DisabledOutlineColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledOutlineOpacity),
        errorBorderColor: Color = OutlinedTextFieldTokens.ErrorOutlineColor.value,
        focusedLeadingIconColor: Color = OutlinedTextFieldTokens.FocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = OutlinedTextFieldTokens.LeadingIconColor.value,
        disabledLeadingIconColor: Color = OutlinedTextFieldTokens.DisabledLeadingIconColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = OutlinedTextFieldTokens.ErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color = OutlinedTextFieldTokens.FocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color = OutlinedTextFieldTokens.TrailingIconColor.value,
        disabledTrailingIconColor: Color = OutlinedTextFieldTokens.DisabledTrailingIconColor
            .value.copy(alpha = OutlinedTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = OutlinedTextFieldTokens.ErrorTrailingIconColor.value,
        focusedLabelColor: Color = OutlinedTextFieldTokens.FocusLabelColor.value,
        unfocusedLabelColor: Color = OutlinedTextFieldTokens.LabelColor.value,
        disabledLabelColor: Color = OutlinedTextFieldTokens.DisabledLabelColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = OutlinedTextFieldTokens.ErrorLabelColor.value,
        focusedPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.value,
        unfocusedPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.value,
        disabledPlaceholderColor: Color = OutlinedTextFieldTokens.DisabledInputColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPlaceholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.value,
        focusedSupportingTextColor: Color = OutlinedTextFieldTokens.FocusSupportingColor.value,
        unfocusedSupportingTextColor: Color = OutlinedTextFieldTokens.SupportingColor.value,
        disabledSupportingTextColor: Color = OutlinedTextFieldTokens.DisabledSupportingColor
            .value.copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = OutlinedTextFieldTokens.ErrorSupportingColor.value,
        focusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        unfocusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        disabledPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        focusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
        unfocusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
        disabledSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
    ): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = focusedTextColor,
        unfocusedTextColor = unfocusedTextColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = errorTextColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = errorContainerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        errorBorderColor = errorBorderColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = focusedPlaceholderColor,
        unfocusedPlaceholderColor = unfocusedPlaceholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = errorPlaceholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.DecorationBox`",
        replaceWith = ReplaceWith("TextFieldDefaults.DecorationBox(\n" +
            "        value = value,\n" +
            "        innerTextField = innerTextField,\n" +
            "        enabled = enabled,\n" +
            "        singleLine = singleLine,\n" +
            "        visualTransformation = visualTransformation,\n" +
            "        interactionSource = interactionSource,\n" +
            "        isError = isError,\n" +
            "        label = label,\n" +
            "        placeholder = placeholder,\n" +
            "        leadingIcon = leadingIcon,\n" +
            "        trailingIcon = trailingIcon,\n" +
            "        prefix = prefix,\n" +
            "        suffix = suffix,\n" +
            "        supportingText = supportingText,\n" +
            "        shape = shape,\n" +
            "        colors = colors,\n" +
            "        contentPadding = contentPadding,\n" +
            "        container = container,\n" +
            "    )"),
        level = DeprecationLevel.WARNING
    )
    @Composable
    @ExperimentalMaterial3Api
    fun TextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        shape: Shape = TextFieldDefaults.shape,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues =
            if (label == null) {
                contentPaddingWithoutLabel()
            } else {
                contentPaddingWithLabel()
            },
        container: @Composable () -> Unit = {
            ContainerBox(enabled, isError, interactionSource, colors, shape)
        }
    ) = DecorationBox(
        value = value,
        innerTextField = innerTextField,
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        isError = isError,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        container = container,
    )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.DecorationBox`",
        replaceWith = ReplaceWith("OutlinedTextFieldDefaults.DecorationBox(\n" +
            "        value = value,\n" +
            "        innerTextField = innerTextField,\n" +
            "        enabled = enabled,\n" +
            "        singleLine = singleLine,\n" +
            "        visualTransformation = visualTransformation,\n" +
            "        interactionSource = interactionSource,\n" +
            "        isError = isError,\n" +
            "        label = label,\n" +
            "        placeholder = placeholder,\n" +
            "        leadingIcon = leadingIcon,\n" +
            "        trailingIcon = trailingIcon,\n" +
            "        prefix = prefix,\n" +
            "        suffix = suffix,\n" +
            "        supportingText = supportingText,\n" +
            "        colors = colors,\n" +
            "        contentPadding = contentPadding,\n" +
            "        container = container,\n" +
            "    )",
            "androidx.compose.material.OutlinedTextFieldDefaults"),
        level = DeprecationLevel.WARNING
    )
    @Composable
    @ExperimentalMaterial3Api
    fun OutlinedTextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
        contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
        container: @Composable () -> Unit = {
            OutlinedTextFieldDefaults.ContainerBox(enabled, isError, interactionSource, colors)
        }
    ) = OutlinedTextFieldDefaults.DecorationBox(
        value = value,
        innerTextField = innerTextField,
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        isError = isError,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        colors = colors,
        contentPadding = contentPadding,
        container = container,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun textFieldColors(
        textColor: Color = FilledTextFieldTokens.InputColor.value,
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = FilledTextFieldTokens.ContainerColor.value,
        cursorColor: Color = FilledTextFieldTokens.CaretColor.value,
        errorCursorColor: Color = FilledTextFieldTokens.ErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedIndicatorColor: Color = FilledTextFieldTokens.FocusActiveIndicatorColor.value,
        unfocusedIndicatorColor: Color = FilledTextFieldTokens.ActiveIndicatorColor.value,
        disabledIndicatorColor: Color = FilledTextFieldTokens.DisabledActiveIndicatorColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledActiveIndicatorOpacity),
        errorIndicatorColor: Color = FilledTextFieldTokens.ErrorActiveIndicatorColor.value,
        focusedLeadingIconColor: Color = FilledTextFieldTokens.FocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = FilledTextFieldTokens.LeadingIconColor.value,
        disabledLeadingIconColor: Color = FilledTextFieldTokens.DisabledLeadingIconColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = FilledTextFieldTokens.ErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color = FilledTextFieldTokens.FocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color = FilledTextFieldTokens.TrailingIconColor.value,
        disabledTrailingIconColor: Color = FilledTextFieldTokens.DisabledTrailingIconColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = FilledTextFieldTokens.ErrorTrailingIconColor.value,
        focusedLabelColor: Color = FilledTextFieldTokens.FocusLabelColor.value,
        unfocusedLabelColor: Color = FilledTextFieldTokens.LabelColor.value,
        disabledLabelColor: Color = FilledTextFieldTokens.DisabledLabelColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = FilledTextFieldTokens.ErrorLabelColor.value,
        placeholderColor: Color = FilledTextFieldTokens.InputPlaceholderColor.value,
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        focusedSupportingTextColor: Color = FilledTextFieldTokens.FocusSupportingColor.value,
        unfocusedSupportingTextColor: Color = FilledTextFieldTokens.SupportingColor.value,
        disabledSupportingTextColor: Color = FilledTextFieldTokens.DisabledSupportingColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = FilledTextFieldTokens.ErrorSupportingColor.value,
        focusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        unfocusedPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        disabledPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = FilledTextFieldTokens.InputPrefixColor.value,
        focusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
        unfocusedSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
        disabledSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = FilledTextFieldTokens.InputSuffixColor.value,
    ): TextFieldColors = colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = textColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedIndicatorColor = focusedIndicatorColor,
        unfocusedIndicatorColor = unfocusedIndicatorColor,
        disabledIndicatorColor = disabledIndicatorColor,
        errorIndicatorColor = errorIndicatorColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = placeholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = OutlinedTextFieldTokens.InputColor.value,
        disabledTextColor: Color = OutlinedTextFieldTokens.DisabledInputColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        containerColor: Color = Color.Transparent,
        cursorColor: Color = OutlinedTextFieldTokens.CaretColor.value,
        errorCursorColor: Color = OutlinedTextFieldTokens.ErrorFocusCaretColor.value,
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedBorderColor: Color = OutlinedTextFieldTokens.FocusOutlineColor.value,
        unfocusedBorderColor: Color = OutlinedTextFieldTokens.OutlineColor.value,
        disabledBorderColor: Color = OutlinedTextFieldTokens.DisabledOutlineColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledOutlineOpacity),
        errorBorderColor: Color = OutlinedTextFieldTokens.ErrorOutlineColor.value,
        focusedLeadingIconColor: Color = OutlinedTextFieldTokens.FocusLeadingIconColor.value,
        unfocusedLeadingIconColor: Color = OutlinedTextFieldTokens.LeadingIconColor.value,
        disabledLeadingIconColor: Color = OutlinedTextFieldTokens.DisabledLeadingIconColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledLeadingIconOpacity),
        errorLeadingIconColor: Color = OutlinedTextFieldTokens.ErrorLeadingIconColor.value,
        focusedTrailingIconColor: Color = OutlinedTextFieldTokens.FocusTrailingIconColor.value,
        unfocusedTrailingIconColor: Color = OutlinedTextFieldTokens.TrailingIconColor.value,
        disabledTrailingIconColor: Color = OutlinedTextFieldTokens.DisabledTrailingIconColor
            .value.copy(alpha = OutlinedTextFieldTokens.DisabledTrailingIconOpacity),
        errorTrailingIconColor: Color = OutlinedTextFieldTokens.ErrorTrailingIconColor.value,
        focusedLabelColor: Color = OutlinedTextFieldTokens.FocusLabelColor.value,
        unfocusedLabelColor: Color = OutlinedTextFieldTokens.LabelColor.value,
        disabledLabelColor: Color = OutlinedTextFieldTokens.DisabledLabelColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledLabelOpacity),
        errorLabelColor: Color = OutlinedTextFieldTokens.ErrorLabelColor.value,
        placeholderColor: Color = OutlinedTextFieldTokens.InputPlaceholderColor.value,
        disabledPlaceholderColor: Color = OutlinedTextFieldTokens.DisabledInputColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        focusedSupportingTextColor: Color = OutlinedTextFieldTokens.FocusSupportingColor.value,
        unfocusedSupportingTextColor: Color = OutlinedTextFieldTokens.SupportingColor.value,
        disabledSupportingTextColor: Color = OutlinedTextFieldTokens.DisabledSupportingColor
            .value.copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
        errorSupportingTextColor: Color = OutlinedTextFieldTokens.ErrorSupportingColor.value,
        focusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        unfocusedPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        disabledPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorPrefixColor: Color = OutlinedTextFieldTokens.InputPrefixColor.value,
        focusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
        unfocusedSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
        disabledSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value
            .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
        errorSuffixColor: Color = OutlinedTextFieldTokens.InputSuffixColor.value,
    ): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = textColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = cursorColor,
        errorCursorColor = errorCursorColor,
        selectionColors = selectionColors,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        errorBorderColor = errorBorderColor,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        errorLeadingIconColor = errorLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        errorTrailingIconColor = errorTrailingIconColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledLabelColor,
        errorLabelColor = errorLabelColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
        errorPlaceholderColor = placeholderColor,
        focusedSupportingTextColor = focusedSupportingTextColor,
        unfocusedSupportingTextColor = unfocusedSupportingTextColor,
        disabledSupportingTextColor = disabledSupportingTextColor,
        errorSupportingTextColor = errorSupportingTextColor,
        focusedPrefixColor = focusedPrefixColor,
        unfocusedPrefixColor = unfocusedPrefixColor,
        disabledPrefixColor = disabledPrefixColor,
        errorPrefixColor = errorPrefixColor,
        focusedSuffixColor = focusedSuffixColor,
        unfocusedSuffixColor = unfocusedSuffixColor,
        disabledSuffixColor = disabledSuffixColor,
        errorSuffixColor = errorSuffixColor,
    )

    @Deprecated("Use overload with prefix and suffix parameters", level = DeprecationLevel.HIDDEN)
    @Composable
    @ExperimentalMaterial3Api
    fun TextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        shape: Shape = TextFieldDefaults.shape,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues =
            if (label == null) {
                contentPaddingWithoutLabel()
            } else {
                contentPaddingWithLabel()
            },
        container: @Composable () -> Unit = {
            ContainerBox(enabled, isError, interactionSource, colors, shape)
        }
    ) {
        DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = null,
            suffix = null,
            supportingText = supportingText,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            container = container,
        )
    }

    @Deprecated("Use overload with prefix and suffix parameters", level = DeprecationLevel.HIDDEN)
    @Composable
    @ExperimentalMaterial3Api
    fun OutlinedTextFieldDecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
        contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
        container: @Composable () -> Unit = {
            OutlinedTextFieldDefaults.ContainerBox(enabled, isError, interactionSource, colors)
        }
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            isError = isError,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = null,
            suffix = null,
            supportingText = supportingText,
            colors = colors,
            contentPadding = contentPadding,
            container = container
        )
    }
}

/**
 * Contains the default values used by [OutlinedTextField]. For defaults used in [TextField], see
 * [TextFieldDefaults].
 */
@Immutable
object OutlinedTextFieldDefaults {
    /** Default shape for an [OutlinedTextField]. */
    val shape: Shape @Composable get() = OutlinedTextFieldTokens.ContainerShape.value

    /**
     * The default min height applied to an [OutlinedTextField].
     * Note that you can override it by applying Modifier.heightIn directly on a text field.
     */
    val MinHeight = 56.dp

    /**
     * The default min width applied to an [OutlinedTextField].
     * Note that you can override it by applying Modifier.widthIn directly on a text field.
     */
    val MinWidth = 280.dp

    /**
     * The default thickness of the border in [OutlinedTextField] in unfocused state.
     */
    val UnfocusedBorderThickness = 1.dp

    /**
     * The default thickness of the border in [OutlinedTextField] in focused state.
     */
    val FocusedBorderThickness = 2.dp

    /**
     * Composable that draws a default container for [OutlinedTextField] with a border stroke. You
     * can use it to draw a border stroke in your custom text field based on
     * [OutlinedTextFieldDefaults.DecorationBox]. The [OutlinedTextField] applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param shape shape of the container
     * @param focusedBorderThickness thickness of the [OutlinedTextField]'s border when it is in
     * focused state
     * @param unfocusedBorderThickness thickness of the [OutlinedTextField]'s border when it is not
     * in focused state
     */
    @ExperimentalMaterial3Api
    @Composable
    fun ContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = OutlinedTextFieldTokens.ContainerShape.value,
        focusedBorderThickness: Dp = FocusedBorderThickness,
        unfocusedBorderThickness: Dp = UnfocusedBorderThickness
    ) {
        val borderStroke = animateBorderStrokeAsState(
            enabled,
            isError,
            interactionSource,
            colors,
            focusedBorderThickness,
            unfocusedBorderThickness
        )
        Box(
            Modifier
                .border(borderStroke.value, shape)
                .background(
                    colors.containerColor(enabled, isError, interactionSource).value, shape
                ))
    }

    /**
     * Default content padding applied to [OutlinedTextField].
     * See [PaddingValues] for more details.
     */
    fun contentPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField].
     */
    @Composable
    fun colors() = MaterialTheme.colorScheme.defaultOutlinedTextFieldColors

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     * focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     * state
     * @param focusedContainerColor the container color for this text field when focused
     * @param unfocusedContainerColor the container color for this text field when not focused
     * @param disabledContainerColor the container color for this text field when disabled
     * @param errorContainerColor the container color for this text field when in error state
     * @param cursorColor the cursor color for this text field
     * @param errorCursorColor the cursor color for this text field when in error state
     * @param selectionColors the colors used when the input text of this text field is selected
     * @param focusedBorderColor the border color for this text field when focused
     * @param unfocusedBorderColor the border color for this text field when not focused
     * @param disabledBorderColor the border color for this text field when disabled
     * @param errorBorderColor the border color for this text field when in error state
     * @param focusedLeadingIconColor the leading icon color for this text field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
     * @param disabledLeadingIconColor the leading icon color for this text field when disabled
     * @param errorLeadingIconColor the leading icon color for this text field when in error state
     * @param focusedTrailingIconColor the trailing icon color for this text field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not focused
     * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
     * @param errorTrailingIconColor the trailing icon color for this text field when in error state
     * @param focusedLabelColor the label color for this text field when focused
     * @param unfocusedLabelColor the label color for this text field when not focused
     * @param disabledLabelColor the label color for this text field when disabled
     * @param errorLabelColor the label color for this text field when in error state
     * @param focusedPlaceholderColor the placeholder color for this text field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
     * @param disabledPlaceholderColor the placeholder color for this text field when disabled
     * @param errorPlaceholderColor the placeholder color for this text field when in error state
     * @param focusedSupportingTextColor the supporting text color for this text field when focused
     * @param unfocusedSupportingTextColor the supporting text color for this text field when not
     * focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     * disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     * state
     * @param focusedPrefixColor the prefix color for this text field when focused
     * @param unfocusedPrefixColor the prefix color for this text field when not focused
     * @param disabledPrefixColor the prefix color for this text field when disabled
     * @param errorPrefixColor the prefix color for this text field when in error state
     * @param focusedSuffixColor the suffix color for this text field when focused
     * @param unfocusedSuffixColor the suffix color for this text field when not focused
     * @param disabledSuffixColor the suffix color for this text field when disabled
     * @param errorSuffixColor the suffix color for this text field when in error state
     */
    @Composable
    fun colors(
        focusedTextColor: Color = Color.Unspecified,
        unfocusedTextColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        errorTextColor: Color = Color.Unspecified,
        focusedContainerColor: Color = Color.Unspecified,
        unfocusedContainerColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        errorContainerColor: Color = Color.Unspecified,
        cursorColor: Color = Color.Unspecified,
        errorCursorColor: Color = Color.Unspecified,
        selectionColors: TextSelectionColors? = null,
        focusedBorderColor: Color = Color.Unspecified,
        unfocusedBorderColor: Color = Color.Unspecified,
        disabledBorderColor: Color = Color.Unspecified,
        errorBorderColor: Color = Color.Unspecified,
        focusedLeadingIconColor: Color = Color.Unspecified,
        unfocusedLeadingIconColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        errorLeadingIconColor: Color = Color.Unspecified,
        focusedTrailingIconColor: Color = Color.Unspecified,
        unfocusedTrailingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        errorTrailingIconColor: Color = Color.Unspecified,
        focusedLabelColor: Color = Color.Unspecified,
        unfocusedLabelColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        errorLabelColor: Color = Color.Unspecified,
        focusedPlaceholderColor: Color = Color.Unspecified,
        unfocusedPlaceholderColor: Color = Color.Unspecified,
        disabledPlaceholderColor: Color = Color.Unspecified,
        errorPlaceholderColor: Color = Color.Unspecified,
        focusedSupportingTextColor: Color = Color.Unspecified,
        unfocusedSupportingTextColor: Color = Color.Unspecified,
        disabledSupportingTextColor: Color = Color.Unspecified,
        errorSupportingTextColor: Color = Color.Unspecified,
        focusedPrefixColor: Color = Color.Unspecified,
        unfocusedPrefixColor: Color = Color.Unspecified,
        disabledPrefixColor: Color = Color.Unspecified,
        errorPrefixColor: Color = Color.Unspecified,
        focusedSuffixColor: Color = Color.Unspecified,
        unfocusedSuffixColor: Color = Color.Unspecified,
        disabledSuffixColor: Color = Color.Unspecified,
        errorSuffixColor: Color = Color.Unspecified,
    ): TextFieldColors =
        MaterialTheme.colorScheme.defaultOutlinedTextFieldColors.copy(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            errorTextColor = errorTextColor,
            focusedContainerColor = focusedContainerColor,
            unfocusedContainerColor = unfocusedContainerColor,
            disabledContainerColor = disabledContainerColor,
            errorContainerColor = errorContainerColor,
            cursorColor = cursorColor,
            errorCursorColor = errorCursorColor,
            textSelectionColors = selectionColors,
            focusedIndicatorColor = focusedBorderColor,
            unfocusedIndicatorColor = unfocusedBorderColor,
            disabledIndicatorColor = disabledBorderColor,
            errorIndicatorColor = errorBorderColor,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            errorLeadingIconColor = errorLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            errorTrailingIconColor = errorTrailingIconColor,
            focusedLabelColor = focusedLabelColor,
            unfocusedLabelColor = unfocusedLabelColor,
            disabledLabelColor = disabledLabelColor,
            errorLabelColor = errorLabelColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
            errorPlaceholderColor = errorPlaceholderColor,
            focusedSupportingTextColor = focusedSupportingTextColor,
            unfocusedSupportingTextColor = unfocusedSupportingTextColor,
            disabledSupportingTextColor = disabledSupportingTextColor,
            errorSupportingTextColor = errorSupportingTextColor,
            focusedPrefixColor = focusedPrefixColor,
            unfocusedPrefixColor = unfocusedPrefixColor,
            disabledPrefixColor = disabledPrefixColor,
            errorPrefixColor = errorPrefixColor,
            focusedSuffixColor = focusedSuffixColor,
            unfocusedSuffixColor = unfocusedSuffixColor,
            disabledSuffixColor = disabledSuffixColor,
            errorSuffixColor = errorSuffixColor,
        )

    internal val ColorScheme.defaultOutlinedTextFieldColors: TextFieldColors
        @Composable
        get() {
            return defaultOutlinedTextFieldColorsCached ?: TextFieldColors(
                focusedTextColor = fromToken(OutlinedTextFieldTokens.FocusInputColor),
                unfocusedTextColor = fromToken(OutlinedTextFieldTokens.InputColor),
                disabledTextColor = fromToken(OutlinedTextFieldTokens.DisabledInputColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                errorTextColor = fromToken(OutlinedTextFieldTokens.ErrorInputColor),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                cursorColor = fromToken(OutlinedTextFieldTokens.CaretColor),
                errorCursorColor = fromToken(OutlinedTextFieldTokens.ErrorFocusCaretColor),
                textSelectionColors = LocalTextSelectionColors.current,
                focusedIndicatorColor = fromToken(OutlinedTextFieldTokens.FocusOutlineColor),
                unfocusedIndicatorColor = fromToken(OutlinedTextFieldTokens.OutlineColor),
                disabledIndicatorColor = fromToken(OutlinedTextFieldTokens.DisabledOutlineColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledOutlineOpacity),
                errorIndicatorColor = fromToken(OutlinedTextFieldTokens.ErrorOutlineColor),
                focusedLeadingIconColor = fromToken(OutlinedTextFieldTokens.FocusLeadingIconColor),
                unfocusedLeadingIconColor = fromToken(OutlinedTextFieldTokens.LeadingIconColor),
                disabledLeadingIconColor =
                fromToken(OutlinedTextFieldTokens.DisabledLeadingIconColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledLeadingIconOpacity),
                errorLeadingIconColor = fromToken(OutlinedTextFieldTokens.ErrorLeadingIconColor),
                focusedTrailingIconColor =
                fromToken(OutlinedTextFieldTokens.FocusTrailingIconColor),
                unfocusedTrailingIconColor = fromToken(OutlinedTextFieldTokens.TrailingIconColor),
                disabledTrailingIconColor =
                fromToken(OutlinedTextFieldTokens.DisabledTrailingIconColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledTrailingIconOpacity),
                errorTrailingIconColor = fromToken(OutlinedTextFieldTokens.ErrorTrailingIconColor),
                focusedLabelColor = fromToken(OutlinedTextFieldTokens.FocusLabelColor),
                unfocusedLabelColor = fromToken(OutlinedTextFieldTokens.LabelColor),
                disabledLabelColor = fromToken(OutlinedTextFieldTokens.DisabledLabelColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledLabelOpacity),
                errorLabelColor = fromToken(OutlinedTextFieldTokens.ErrorLabelColor),
                focusedPlaceholderColor = fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                unfocusedPlaceholderColor =
                fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                disabledPlaceholderColor = fromToken(OutlinedTextFieldTokens.DisabledInputColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                errorPlaceholderColor = fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                focusedSupportingTextColor =
                fromToken(OutlinedTextFieldTokens.FocusSupportingColor),
                unfocusedSupportingTextColor = fromToken(OutlinedTextFieldTokens.SupportingColor),
                disabledSupportingTextColor = OutlinedTextFieldTokens.DisabledSupportingColor
                    .value.copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
                errorSupportingTextColor = fromToken(OutlinedTextFieldTokens.ErrorSupportingColor),
                focusedPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                unfocusedPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                disabledPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                errorPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                focusedSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor),
                unfocusedSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor),
                disabledSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor)
                    .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                errorSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor)
            ).also {
                defaultOutlinedTextFieldColorsCached = it
            }
        }
    /**
     * A decoration box which helps creating custom text fields based on
     * <a href="https://material.io/components/text-fields#outlined-text-field" class="external" target="_blank">Material Design outlined text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [OutlinedTextField],
     * consider using this decoration box to achieve the desired design.
     *
     * For example, if you need to create a dense outlined text field, use [contentPadding]
     * parameter to decrease the paddings around the input field. If you need to change the
     * thickness of the border, use [container] parameter to achieve that.
     *
     * Example of custom text field based on [OutlinedTextFieldDefaults.DecorationBox]:
     * @sample androidx.compose.material3.samples.CustomOutlinedTextFieldBasedOnDecorationBox
     *
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     * framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     * the [BasicTextField]
     * @param enabled controls the enabled state of the text field. When `false`, this component
     * will not respond to user input, and it will appear visually disabled and disabled to
     * accessibility services. You must also pass the same value to the [BasicTextField] for it to
     * adjust the behavior accordingly.
     * @param singleLine indicates if this is a single line or multi line text field. You must pass
     * the same value as to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. You
     * must pass the same value as to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     * [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     * [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     * then pass the same instance to this decoration box to observe [Interaction]s and customize
     * the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in error state. If set to
     * true, the label, bottom indicator and trailing icon by default will be displayed in error
     * color.
     * @param label the optional label to be displayed inside the text field container. The default
     * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
     * [Typography.bodyLarge] when the text field is not in focus.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     * and the input text is empty. The default text style for internal [Text] is
     * [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     * field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     * container
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     * field in different states. See [OutlinedTextFieldDefaults.colors].
     * @param contentPadding the spacing values to apply internally between the internals of text
     * field and the decoration box container. You can use it to implement dense text fields or
     * simply to control horizontal padding. See [OutlinedTextFieldDefaults.contentPadding].
     * @param container the container to be drawn behind the text field. By default, this is
     * transparent and only includes a border. The cutout in the border to fit the [label] will be
     * automatically added by the framework. Note that by default the color of the border comes from
     * the [colors].
     */
    @Composable
    @ExperimentalMaterial3Api
    fun DecorationBox(
        value: String,
        innerTextField: @Composable () -> Unit,
        enabled: Boolean,
        singleLine: Boolean,
        visualTransformation: VisualTransformation,
        interactionSource: InteractionSource,
        isError: Boolean = false,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues = contentPadding(),
        container: @Composable () -> Unit = {
            ContainerBox(
                enabled,
                isError,
                interactionSource,
                colors
            )
        }
    ) {
        CommonDecorationBox(
            type = TextFieldType.Outlined,
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
            contentPadding = contentPadding,
            container = container
        )
    }
}

/**
 * Represents the colors of the input text, container, and content (including label, placeholder,
 * leading and trailing icons) used in a text field in different states.
 *
 * @constructor create an instance with arbitrary colors.
 * See [TextFieldDefaults.colors] for the default colors used in [TextField].
 * See [OutlinedTextFieldDefaults.colors] for the default colors used in [OutlinedTextField].
 *
 * @param focusedTextColor the color used for the input text of this text field when focused
 * @param unfocusedTextColor the color used for the input text of this text field when not
 * focused
 * @param disabledTextColor the color used for the input text of this text field when disabled
 * @param errorTextColor the color used for the input text of this text field when in error
 * state
 * @param focusedContainerColor the container color for this text field when focused
 * @param unfocusedContainerColor the container color for this text field when not focused
 * @param disabledContainerColor the container color for this text field when disabled
 * @param errorContainerColor the container color for this text field when in error state
 * @param cursorColor the cursor color for this text field
 * @param errorCursorColor the cursor color for this text field when in error state
 * @param textSelectionColors the colors used when the input text of this text field is selected
 * @param focusedIndicatorColor the indicator color for this text field when focused
 * @param unfocusedIndicatorColor the indicator color for this text field when not focused
 * @param disabledIndicatorColor the indicator color for this text field when disabled
 * @param errorIndicatorColor the indicator color for this text field when in error state
 * @param focusedLeadingIconColor the leading icon color for this text field when focused
 * @param unfocusedLeadingIconColor the leading icon color for this text field when not focused
 * @param disabledLeadingIconColor the leading icon color for this text field when disabled
 * @param errorLeadingIconColor the leading icon color for this text field when in error state
 * @param focusedTrailingIconColor the trailing icon color for this text field when focused
 * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
 * focused
 * @param disabledTrailingIconColor the trailing icon color for this text field when disabled
 * @param errorTrailingIconColor the trailing icon color for this text field when in error state
 * @param focusedLabelColor the label color for this text field when focused
 * @param unfocusedLabelColor the label color for this text field when not focused
 * @param disabledLabelColor the label color for this text field when disabled
 * @param errorLabelColor the label color for this text field when in error state
 * @param focusedPlaceholderColor the placeholder color for this text field when focused
 * @param unfocusedPlaceholderColor the placeholder color for this text field when not focused
 * @param disabledPlaceholderColor the placeholder color for this text field when disabled
 * @param errorPlaceholderColor the placeholder color for this text field when in error state
 * @param focusedSupportingTextColor the supporting text color for this text field when focused
 * @param unfocusedSupportingTextColor the supporting text color for this text field when not
 * focused
 * @param disabledSupportingTextColor the supporting text color for this text field when
 * disabled
 * @param errorSupportingTextColor the supporting text color for this text field when in error
 * state
 * @param focusedPrefixColor the prefix color for this text field when focused
 * @param unfocusedPrefixColor the prefix color for this text field when not focused
 * @param disabledPrefixColor the prefix color for this text field when disabled
 * @param errorPrefixColor the prefix color for this text field when in error state
 * @param focusedSuffixColor the suffix color for this text field when focused
 * @param unfocusedSuffixColor the suffix color for this text field when not focused
 * @param disabledSuffixColor the suffix color for this text field when disabled
 * @param errorSuffixColor the suffix color for this text field when in error state
 */
@Immutable
class TextFieldColors constructor(
    val focusedTextColor: Color,
    val unfocusedTextColor: Color,
    val disabledTextColor: Color,
    val errorTextColor: Color,
    val focusedContainerColor: Color,
    val unfocusedContainerColor: Color,
    val disabledContainerColor: Color,
    val errorContainerColor: Color,
    val cursorColor: Color,
    val errorCursorColor: Color,
    val textSelectionColors: TextSelectionColors,
    val focusedIndicatorColor: Color,
    val unfocusedIndicatorColor: Color,
    val disabledIndicatorColor: Color,
    val errorIndicatorColor: Color,
    val focusedLeadingIconColor: Color,
    val unfocusedLeadingIconColor: Color,
    val disabledLeadingIconColor: Color,
    val errorLeadingIconColor: Color,
    val focusedTrailingIconColor: Color,
    val unfocusedTrailingIconColor: Color,
    val disabledTrailingIconColor: Color,
    val errorTrailingIconColor: Color,
    val focusedLabelColor: Color,
    val unfocusedLabelColor: Color,
    val disabledLabelColor: Color,
    val errorLabelColor: Color,
    val focusedPlaceholderColor: Color,
    val unfocusedPlaceholderColor: Color,
    val disabledPlaceholderColor: Color,
    val errorPlaceholderColor: Color,
    val focusedSupportingTextColor: Color,
    val unfocusedSupportingTextColor: Color,
    val disabledSupportingTextColor: Color,
    val errorSupportingTextColor: Color,
    val focusedPrefixColor: Color,
    val unfocusedPrefixColor: Color,
    val disabledPrefixColor: Color,
    val errorPrefixColor: Color,
    val focusedSuffixColor: Color,
    val unfocusedSuffixColor: Color,
    val disabledSuffixColor: Color,
    val errorSuffixColor: Color,
) {

    /**
     * Returns a copy of this ChipColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        focusedTextColor: Color = this.focusedTextColor,
        unfocusedTextColor: Color = this.unfocusedTextColor,
        disabledTextColor: Color = this.disabledTextColor,
        errorTextColor: Color = this.errorTextColor,
        focusedContainerColor: Color = this.focusedContainerColor,
        unfocusedContainerColor: Color = this.unfocusedContainerColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        errorContainerColor: Color = this.errorContainerColor,
        cursorColor: Color = this.cursorColor,
        errorCursorColor: Color = this.errorCursorColor,
        textSelectionColors: TextSelectionColors? = this.textSelectionColors,
        focusedIndicatorColor: Color = this.focusedIndicatorColor,
        unfocusedIndicatorColor: Color = this.unfocusedIndicatorColor,
        disabledIndicatorColor: Color = this.disabledIndicatorColor,
        errorIndicatorColor: Color = this.errorIndicatorColor,
        focusedLeadingIconColor: Color = this.focusedLeadingIconColor,
        unfocusedLeadingIconColor: Color = this.unfocusedLeadingIconColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        errorLeadingIconColor: Color = this.errorLeadingIconColor,
        focusedTrailingIconColor: Color = this.focusedTrailingIconColor,
        unfocusedTrailingIconColor: Color = this.unfocusedTrailingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
        errorTrailingIconColor: Color = this.errorTrailingIconColor,
        focusedLabelColor: Color = this.focusedLabelColor,
        unfocusedLabelColor: Color = this.unfocusedLabelColor,
        disabledLabelColor: Color = this.disabledLabelColor,
        errorLabelColor: Color = this.errorLabelColor,
        focusedPlaceholderColor: Color = this.focusedPlaceholderColor,
        unfocusedPlaceholderColor: Color = this.unfocusedPlaceholderColor,
        disabledPlaceholderColor: Color = this.disabledPlaceholderColor,
        errorPlaceholderColor: Color = this.errorPlaceholderColor,
        focusedSupportingTextColor: Color = this.focusedSupportingTextColor,
        unfocusedSupportingTextColor: Color = this.unfocusedSupportingTextColor,
        disabledSupportingTextColor: Color = this.disabledSupportingTextColor,
        errorSupportingTextColor: Color = this.errorSupportingTextColor,
        focusedPrefixColor: Color = this.focusedPrefixColor,
        unfocusedPrefixColor: Color = this.unfocusedPrefixColor,
        disabledPrefixColor: Color = this.disabledPrefixColor,
        errorPrefixColor: Color = this.errorPrefixColor,
        focusedSuffixColor: Color = this.focusedSuffixColor,
        unfocusedSuffixColor: Color = this.unfocusedSuffixColor,
        disabledSuffixColor: Color = this.disabledSuffixColor,
        errorSuffixColor: Color = this.errorSuffixColor,
    ) = TextFieldColors(
        focusedTextColor.takeOrElse { this.focusedTextColor },
        unfocusedTextColor.takeOrElse { this.unfocusedTextColor },
        disabledTextColor.takeOrElse { this.disabledTextColor },
        errorTextColor.takeOrElse { this.errorTextColor },
        focusedContainerColor.takeOrElse { this.focusedContainerColor },
        unfocusedContainerColor.takeOrElse { this.unfocusedContainerColor },
        disabledContainerColor.takeOrElse { this.disabledContainerColor },
        errorContainerColor.takeOrElse { this.errorContainerColor },
        cursorColor.takeOrElse { this.cursorColor },
        errorCursorColor.takeOrElse { this.errorCursorColor },
        textSelectionColors.takeOrElse { this.textSelectionColors },
        focusedIndicatorColor.takeOrElse { this.focusedIndicatorColor },
        unfocusedIndicatorColor.takeOrElse { this.unfocusedIndicatorColor },
        disabledIndicatorColor.takeOrElse { this.disabledIndicatorColor },
        errorIndicatorColor.takeOrElse { this.errorIndicatorColor },
        focusedLeadingIconColor.takeOrElse { this.focusedLeadingIconColor },
        unfocusedLeadingIconColor.takeOrElse { this.unfocusedLeadingIconColor },
        disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
        errorLeadingIconColor.takeOrElse { this.errorLeadingIconColor },
        focusedTrailingIconColor.takeOrElse { this.focusedTrailingIconColor },
        unfocusedTrailingIconColor.takeOrElse { this.unfocusedTrailingIconColor },
        disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
        errorTrailingIconColor.takeOrElse { this.errorTrailingIconColor },
        focusedLabelColor.takeOrElse { this.focusedLabelColor },
        unfocusedLabelColor.takeOrElse { this.unfocusedLabelColor },
        disabledLabelColor.takeOrElse { this.disabledLabelColor },
        errorLabelColor.takeOrElse { this.errorLabelColor },
        focusedPlaceholderColor.takeOrElse { this.focusedPlaceholderColor },
        unfocusedPlaceholderColor.takeOrElse { this.unfocusedPlaceholderColor },
        disabledPlaceholderColor.takeOrElse { this.disabledPlaceholderColor },
        errorPlaceholderColor.takeOrElse { this.errorPlaceholderColor },
        focusedSupportingTextColor.takeOrElse { this.focusedSupportingTextColor },
        unfocusedSupportingTextColor.takeOrElse { this.unfocusedSupportingTextColor },
        disabledSupportingTextColor.takeOrElse { this.disabledSupportingTextColor },
        errorSupportingTextColor.takeOrElse { this.errorSupportingTextColor },
        focusedPrefixColor.takeOrElse { this.focusedPrefixColor },
        unfocusedPrefixColor.takeOrElse { this.unfocusedPrefixColor },
        disabledPrefixColor.takeOrElse { this.disabledPrefixColor },
        errorPrefixColor.takeOrElse { this.errorPrefixColor },
        focusedSuffixColor.takeOrElse { this.focusedSuffixColor },
        unfocusedSuffixColor.takeOrElse { this.unfocusedSuffixColor },
        disabledSuffixColor.takeOrElse { this.disabledSuffixColor },
        errorSuffixColor.takeOrElse { this.errorSuffixColor },
    )

    internal fun TextSelectionColors?.takeOrElse(block: () -> TextSelectionColors):
        TextSelectionColors = this ?: block()

    /**
     * Represents the color used for the leading icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun leadingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledLeadingIconColor
                isError -> errorLeadingIconColor
                focused -> focusedLeadingIconColor
                else -> unfocusedLeadingIconColor
            }
        )
    }

    /**
     * Represents the color used for the trailing icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledTrailingIconColor
                isError -> errorTrailingIconColor
                focused -> focusedTrailingIconColor
                else -> unfocusedTrailingIconColor
            }
        )
    }

    /**
     * Represents the color used for the border indicator of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledIndicatorColor
            isError -> errorIndicatorColor
            focused -> focusedIndicatorColor
            else -> unfocusedIndicatorColor
        }
        return if (enabled) {
            animateColorAsState(targetValue, tween(durationMillis = AnimationDuration))
        } else {
            rememberUpdatedState(targetValue)
        }
    }

    /**
     * Represents the container color for this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun containerColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledContainerColor
            isError -> errorContainerColor
            focused -> focusedContainerColor
            else -> unfocusedContainerColor
        }
        return animateColorAsState(targetValue, tween(durationMillis = AnimationDuration))
    }

    /**
     * Represents the color used for the placeholder of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun placeholderColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledPlaceholderColor
            isError -> errorPlaceholderColor
            focused -> focusedPlaceholderColor
            else -> unfocusedPlaceholderColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the label of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun labelColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledLabelColor
            isError -> errorLabelColor
            focused -> focusedLabelColor
            else -> unfocusedLabelColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the input field of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun textColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledTextColor
            isError -> errorTextColor
            focused -> focusedTextColor
            else -> unfocusedTextColor
        }
        return rememberUpdatedState(targetValue)
    }

    @Composable
    internal fun supportingTextColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        return rememberUpdatedState(
            when {
                !enabled -> disabledSupportingTextColor
                isError -> errorSupportingTextColor
                focused -> focusedSupportingTextColor
                else -> unfocusedSupportingTextColor
            }
        )
    }

    /**
     * Represents the color used for the prefix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun prefixColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledPrefixColor
            isError -> errorPrefixColor
            focused -> focusedPrefixColor
            else -> unfocusedPrefixColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the suffix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of this text field. Helps to determine if
     * the text field is in focus or not
     */
    @Composable
    internal fun suffixColor(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource
    ): State<Color> {
        val focused by interactionSource.collectIsFocusedAsState()

        val targetValue = when {
            !enabled -> disabledSuffixColor
            isError -> errorSuffixColor
            focused -> focusedSuffixColor
            else -> unfocusedSuffixColor
        }
        return rememberUpdatedState(targetValue)
    }

    /**
     * Represents the color used for the cursor of this text field.
     *
     * @param isError whether the text field's current value is in error
     */
    @Composable
    internal fun cursorColor(isError: Boolean): State<Color> {
        return rememberUpdatedState(if (isError) errorCursorColor else cursorColor)
    }

    /**
     * Represents the colors used for text selection in this text field.
     */
    internal val selectionColors: TextSelectionColors
        @Composable get() = textSelectionColors

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextFieldColors) return false

        if (focusedTextColor != other.focusedTextColor) return false
        if (unfocusedTextColor != other.unfocusedTextColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (errorTextColor != other.errorTextColor) return false
        if (focusedContainerColor != other.focusedContainerColor) return false
        if (unfocusedContainerColor != other.unfocusedContainerColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (errorContainerColor != other.errorContainerColor) return false
        if (cursorColor != other.cursorColor) return false
        if (errorCursorColor != other.errorCursorColor) return false
        if (textSelectionColors != other.textSelectionColors) return false
        if (focusedIndicatorColor != other.focusedIndicatorColor) return false
        if (unfocusedIndicatorColor != other.unfocusedIndicatorColor) return false
        if (disabledIndicatorColor != other.disabledIndicatorColor) return false
        if (errorIndicatorColor != other.errorIndicatorColor) return false
        if (focusedLeadingIconColor != other.focusedLeadingIconColor) return false
        if (unfocusedLeadingIconColor != other.unfocusedLeadingIconColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (errorLeadingIconColor != other.errorLeadingIconColor) return false
        if (focusedTrailingIconColor != other.focusedTrailingIconColor) return false
        if (unfocusedTrailingIconColor != other.unfocusedTrailingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (errorTrailingIconColor != other.errorTrailingIconColor) return false
        if (focusedLabelColor != other.focusedLabelColor) return false
        if (unfocusedLabelColor != other.unfocusedLabelColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (errorLabelColor != other.errorLabelColor) return false
        if (focusedPlaceholderColor != other.focusedPlaceholderColor) return false
        if (unfocusedPlaceholderColor != other.unfocusedPlaceholderColor) return false
        if (disabledPlaceholderColor != other.disabledPlaceholderColor) return false
        if (errorPlaceholderColor != other.errorPlaceholderColor) return false
        if (focusedSupportingTextColor != other.focusedSupportingTextColor) return false
        if (unfocusedSupportingTextColor != other.unfocusedSupportingTextColor) return false
        if (disabledSupportingTextColor != other.disabledSupportingTextColor) return false
        if (errorSupportingTextColor != other.errorSupportingTextColor) return false
        if (focusedPrefixColor != other.focusedPrefixColor) return false
        if (unfocusedPrefixColor != other.unfocusedPrefixColor) return false
        if (disabledPrefixColor != other.disabledPrefixColor) return false
        if (errorPrefixColor != other.errorPrefixColor) return false
        if (focusedSuffixColor != other.focusedSuffixColor) return false
        if (unfocusedSuffixColor != other.unfocusedSuffixColor) return false
        if (disabledSuffixColor != other.disabledSuffixColor) return false
        if (errorSuffixColor != other.errorSuffixColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = focusedTextColor.hashCode()
        result = 31 * result + unfocusedTextColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + errorTextColor.hashCode()
        result = 31 * result + focusedContainerColor.hashCode()
        result = 31 * result + unfocusedContainerColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + errorContainerColor.hashCode()
        result = 31 * result + cursorColor.hashCode()
        result = 31 * result + errorCursorColor.hashCode()
        result = 31 * result + textSelectionColors.hashCode()
        result = 31 * result + focusedIndicatorColor.hashCode()
        result = 31 * result + unfocusedIndicatorColor.hashCode()
        result = 31 * result + disabledIndicatorColor.hashCode()
        result = 31 * result + errorIndicatorColor.hashCode()
        result = 31 * result + focusedLeadingIconColor.hashCode()
        result = 31 * result + unfocusedLeadingIconColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + errorLeadingIconColor.hashCode()
        result = 31 * result + focusedTrailingIconColor.hashCode()
        result = 31 * result + unfocusedTrailingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + errorTrailingIconColor.hashCode()
        result = 31 * result + focusedLabelColor.hashCode()
        result = 31 * result + unfocusedLabelColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + errorLabelColor.hashCode()
        result = 31 * result + focusedPlaceholderColor.hashCode()
        result = 31 * result + unfocusedPlaceholderColor.hashCode()
        result = 31 * result + disabledPlaceholderColor.hashCode()
        result = 31 * result + errorPlaceholderColor.hashCode()
        result = 31 * result + focusedSupportingTextColor.hashCode()
        result = 31 * result + unfocusedSupportingTextColor.hashCode()
        result = 31 * result + disabledSupportingTextColor.hashCode()
        result = 31 * result + errorSupportingTextColor.hashCode()
        result = 31 * result + focusedPrefixColor.hashCode()
        result = 31 * result + unfocusedPrefixColor.hashCode()
        result = 31 * result + disabledPrefixColor.hashCode()
        result = 31 * result + errorPrefixColor.hashCode()
        result = 31 * result + focusedSuffixColor.hashCode()
        result = 31 * result + unfocusedSuffixColor.hashCode()
        result = 31 * result + disabledSuffixColor.hashCode()
        result = 31 * result + errorSuffixColor.hashCode()
        return result
    }
}

@Composable
private fun animateBorderStrokeAsState(
    enabled: Boolean,
    isError: Boolean,
    interactionSource: InteractionSource,
    colors: TextFieldColors,
    focusedBorderThickness: Dp,
    unfocusedBorderThickness: Dp
): State<BorderStroke> {
    val focused by interactionSource.collectIsFocusedAsState()
    val indicatorColor = colors.indicatorColor(enabled, isError, interactionSource)
    val targetThickness = if (focused) focusedBorderThickness else unfocusedBorderThickness
    val animatedThickness = if (enabled) {
        animateDpAsState(targetThickness, tween(durationMillis = AnimationDuration))
    } else {
        rememberUpdatedState(unfocusedBorderThickness)
    }
    return rememberUpdatedState(
        BorderStroke(animatedThickness.value, SolidColor(indicatorColor.value))
    )
}
