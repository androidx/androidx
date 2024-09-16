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

import androidx.annotation.FloatRange
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldLineLimits.MultiLine
import androidx.compose.foundation.text.input.TextFieldLineLimits.SingleLine
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.internal.CommonDecorationBox
import androidx.compose.material3.internal.SupportingTopPadding
import androidx.compose.material3.internal.TextFieldPadding
import androidx.compose.material3.internal.TextFieldType
import androidx.compose.material3.internal.animateBorderStrokeAsState
import androidx.compose.material3.internal.textFieldBackground
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.OutlinedTextFieldTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.AnnotatedString
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
    val shape: Shape
        @Composable get() = FilledTextFieldTokens.ContainerShape.value

    /**
     * The default min height applied to a [TextField]. Note that you can override it by applying
     * Modifier.heightIn directly on a text field.
     */
    val MinHeight = 56.dp

    /**
     * The default min width applied to a [TextField]. Note that you can override it by applying
     * Modifier.widthIn directly on a text field.
     */
    val MinWidth = 280.dp

    /** The default thickness of the indicator line in [TextField] in unfocused state. */
    val UnfocusedIndicatorThickness = 1.dp

    /** The default thickness of the indicator line in [TextField] in focused state. */
    val FocusedIndicatorThickness = 2.dp

    /**
     * A decorator used to create custom text fields based on <a
     * href="https://m3.material.io/components/text-fields/overview" class="external"
     * target="_blank">Material Design filled text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [TextField], such as
     * the indicator line thickness, consider using this decorator to achieve the desired design.
     *
     * For example, if you wish to customise the bottom indicator line, you can pass a custom
     * [Container] to this decorator's [container].
     *
     * This decorator is meant to be used in conjunction with the overload of [BasicTextField] that
     * accepts a [TextFieldDecorator] parameter. For other overloads of [BasicTextField] that use a
     * `decorationBox`, see [DecorationBox].
     *
     * An example of building a custom text field using [decorator]:
     *
     * @sample androidx.compose.material3.samples.CustomTextFieldUsingDecorator
     * @param state [TextFieldState] object that holds the internal editing state of the text field.
     * @param enabled the enabled state of the text field. When `false`, this decorator will appear
     *   visually disabled. This must be the same value that is passed to [BasicTextField].
     * @param lineLimits whether the text field is [SingleLine] or [MultiLine]. This must be the
     *   same value that is passed to [BasicTextField].
     * @param outputTransformation [OutputTransformation] that transforms how the contents of the
     *   text field are presented. This must be the same value that is passed to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     *   [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     *   [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     *   then pass the same instance to this decorator to observe [Interaction]s and customize the
     *   appearance/behavior of the text field in different states.
     * @param labelPosition the position of the label. See [TextFieldLabelPosition].
     * @param label the optional label to be displayed with this text field. The default text style
     *   uses [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
     * @param placeholder the optional placeholder to be displayed when the input text is empty. The
     *   default text style uses [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     *   field container.
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     *   container.
     * @param prefix the optional prefix to be displayed before the input text in the text field.
     * @param suffix the optional suffix to be displayed after the input text in the text field.
     * @param supportingText the optional supporting text to be displayed below the text field.
     * @param isError indicates if the text field's current value is in an error state. When `true`,
     *   this decorator will display its contents in an error color.
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     *   field decorator in different states. See [TextFieldDefaults.colors].
     * @param contentPadding the padding between the input field and the surrounding elements of the
     *   decorator. Note that the padding values may not be respected if they are incompatible with
     *   the text field's size constraints or layout. See
     *   [TextFieldDefaults.contentPaddingWithLabel] and
     *   [TextFieldDefaults.contentPaddingWithoutLabel].
     * @param container the container to be drawn behind the text field. By default, this uses
     *   [Container]. Default colors for the container come from the [colors].
     */
    @Composable
    @ExperimentalMaterial3Api
    fun decorator(
        state: TextFieldState,
        enabled: Boolean,
        lineLimits: TextFieldLineLimits,
        outputTransformation: OutputTransformation?,
        interactionSource: InteractionSource,
        labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Default(),
        label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues =
            if (label == null || labelPosition is TextFieldLabelPosition.Above) {
                contentPaddingWithoutLabel()
            } else {
                contentPaddingWithLabel()
            },
        container: @Composable () -> Unit = {
            Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                shape = shape,
                focusedIndicatorLineThickness = FocusedIndicatorThickness,
                unfocusedIndicatorLineThickness = UnfocusedIndicatorThickness,
            )
        }
    ): TextFieldDecorator = TextFieldDecorator { innerTextField ->
        val visualText =
            if (outputTransformation == null) state.text
            else {
                // TODO: use constructor to create TextFieldBuffer from TextFieldState when
                // available
                lateinit var buffer: TextFieldBuffer
                state.edit { buffer = this }
                // after edit completes, mutations on buffer are ineffective
                with(outputTransformation) { buffer.transformOutput() }
                buffer.asCharSequence()
            }

        CommonDecorationBox(
            type = TextFieldType.Filled,
            visualText = visualText,
            innerTextField = innerTextField,
            placeholder = placeholder,
            labelPosition = labelPosition,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = lineLimits == SingleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = container,
        )
    }

    /**
     * Composable that draws a default container for a [TextField] with an indicator line at the
     * bottom. You can apply it to a [BasicTextField] using [DecorationBox] to create a custom text
     * field based on the styling of a Material filled text field. The [TextField] component applies
     * it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of the text field. Used to determine if the
     *   text field is in focus or not
     * @param modifier the [Modifier] of this container
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param shape the shape of this container
     * @param focusedIndicatorLineThickness thickness of the indicator line when the text field is
     *   focused
     * @param unfocusedIndicatorLineThickness thickness of the indicator line when the text field is
     *   not focused
     */
    @ExperimentalMaterial3Api
    @Composable
    fun Container(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        modifier: Modifier = Modifier,
        colors: TextFieldColors = colors(),
        shape: Shape = TextFieldDefaults.shape,
        focusedIndicatorLineThickness: Dp = FocusedIndicatorThickness,
        unfocusedIndicatorLineThickness: Dp = UnfocusedIndicatorThickness,
    ) {
        val focused = interactionSource.collectIsFocusedAsState().value
        // TODO Load the motionScheme tokens from the component tokens file
        val containerColor =
            animateColorAsState(
                targetValue = colors.containerColor(enabled, isError, focused),
                animationSpec = MotionSchemeKeyTokens.FastEffects.value(),
            )
        Box(
            modifier
                .textFieldBackground(containerColor::value, shape)
                .indicatorLine(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    focusedIndicatorLineThickness = focusedIndicatorLineThickness,
                    unfocusedIndicatorLineThickness = unfocusedIndicatorLineThickness,
                )
        )
    }

    /**
     * A modifier to draw a default bottom indicator line for [TextField]. You can apply it to a
     * [BasicTextField] or to [DecorationBox] to create a custom text field based on the styling of
     * a Material filled text field.
     *
     * Consider using [Container], which automatically applies this modifier as well as other text
     * field container styling.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of the text field. Used to determine if the
     *   text field is in focus or not
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param focusedIndicatorLineThickness thickness of the indicator line when the text field is
     *   focused
     * @param unfocusedIndicatorLineThickness thickness of the indicator line when the text field is
     *   not focused
     */
    @ExperimentalMaterial3Api
    fun Modifier.indicatorLine(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        focusedIndicatorLineThickness: Dp = FocusedIndicatorThickness,
        unfocusedIndicatorLineThickness: Dp = UnfocusedIndicatorThickness
    ) =
        composed(
            inspectorInfo =
                debugInspectorInfo {
                    name = "indicatorLine"
                    properties["enabled"] = enabled
                    properties["isError"] = isError
                    properties["interactionSource"] = interactionSource
                    properties["colors"] = colors
                    properties["focusedIndicatorLineThickness"] = focusedIndicatorLineThickness
                    properties["unfocusedIndicatorLineThickness"] = unfocusedIndicatorLineThickness
                }
        ) {
            val focused = interactionSource.collectIsFocusedAsState().value
            val stroke =
                animateBorderStrokeAsState(
                    enabled,
                    isError,
                    focused,
                    colors,
                    focusedIndicatorLineThickness,
                    unfocusedIndicatorLineThickness
                )
            Modifier.drawIndicatorLine(stroke)
        }

    /**
     * A decoration box used to create custom text fields based on <a
     * href="https://m3.material.io/components/text-fields/overview" class="external"
     * target="_blank">Material Design filled text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [TextField], consider
     * using this decoration box to achieve the desired design.
     *
     * For example, if you wish to customise the bottom indicator line, you can pass a custom
     * [Container] to this decoration box's [container].
     *
     * This decoration box is meant to be used in conjunction with overloads of [BasicTextField]
     * that accept a `decorationBox` parameter. For other overloads of [BasicTextField] that use a
     * [TextFieldDecorator], see [decorator].
     *
     * An example of building a custom text field using [DecorationBox]:
     *
     * @sample androidx.compose.material3.samples.CustomTextFieldBasedOnDecorationBox
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     *   framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     *   the [BasicTextField]
     * @param enabled the enabled state of the text field. When `false`, this decoration box will
     *   appear visually disabled. This must be the same value that is passed to [BasicTextField].
     * @param singleLine indicates if this is a single line or multi line text field. This must be
     *   the same value that is passed to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. This
     *   must be the same value that is passed to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     *   [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     *   [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     *   then pass the same instance to this decoration box to observe [Interaction]s and customize
     *   the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in an error state. When `true`,
     *   this decoration box will display its contents in an error color.
     * @param label the optional label to be displayed with this text field. The default text style
     *   uses [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     *   and the input text is empty. The default text style for internal [Text] is
     *   [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     *   field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     *   container
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param shape defines the shape of this decoration box's container
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     *   field decoration box in different states. See [TextFieldDefaults.colors].
     * @param contentPadding the padding between the input field and the surrounding elements of the
     *   decoration box. Note that the padding values may not be respected if they are incompatible
     *   with the text field's size constraints or layout. See
     *   [TextFieldDefaults.contentPaddingWithLabel] and
     *   [TextFieldDefaults.contentPaddingWithoutLabel].
     * @param container the container to be drawn behind the text field. By default, this uses
     *   [Container]. Default colors for the container come from the [colors].
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
            Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                modifier = Modifier,
                colors = colors,
                shape = shape,
                focusedIndicatorLineThickness = FocusedIndicatorThickness,
                unfocusedIndicatorLineThickness = UnfocusedIndicatorThickness,
            )
        }
    ) {
        val visualText =
            remember(value, visualTransformation) {
                    visualTransformation.filter(AnnotatedString(value))
                }
                .text
                .text

        CommonDecorationBox(
            type = TextFieldType.Filled,
            visualText = visualText,
            innerTextField = innerTextField,
            placeholder = placeholder,
            labelPosition = TextFieldLabelPosition.Default(),
            label = label?.let { { it.invoke() } },
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
            container = container,
        )
    }

    /**
     * Default content padding of the input field within the [TextField] when there is an inside
     * label. Note that the top padding represents the padding above the label in the focused state.
     * The input field is placed directly beneath the label.
     *
     * Horizontal padding represents the distance between the input field and the leading/trailing
     * icons (if present) or the horizontal edges of the container if there are no icons.
     */
    fun contentPaddingWithLabel(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = TextFieldWithLabelVerticalPadding,
        bottom: Dp = TextFieldWithLabelVerticalPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default content padding of the input field within the [TextField] when the label is null or
     * positioned [TextFieldLabelPosition.Above].
     *
     * Horizontal padding represents the distance between the input field and the leading/trailing
     * icons (if present) or the horizontal edges of the container if there are no icons.
     */
    fun contentPaddingWithoutLabel(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues = PaddingValues(start, top, end, bottom)

    /**
     * Default padding applied to supporting text for both [TextField] and [OutlinedTextField]. See
     * [PaddingValues] for more details.
     */
    // TODO(246775477): consider making this public
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
    @Composable fun colors() = MaterialTheme.colorScheme.defaultTextFieldColors

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in a [TextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     *   focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     *   state
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
     *   focused
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
     *   focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     *   disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     *   state
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
    ): TextFieldColors =
        MaterialTheme.colorScheme.defaultTextFieldColors.copy(
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
            return defaultTextFieldColorsCached?.let { cachedColors ->
                val localTextSelectionColors = LocalTextSelectionColors.current
                if (cachedColors.textSelectionColors == localTextSelectionColors) {
                    cachedColors
                } else {
                    cachedColors.copy(textSelectionColors = localTextSelectionColors).also {
                        defaultTextFieldColorsCached = it
                    }
                }
            }
                ?: TextFieldColors(
                        focusedTextColor = fromToken(FilledTextFieldTokens.FocusInputColor),
                        unfocusedTextColor = fromToken(FilledTextFieldTokens.InputColor),
                        disabledTextColor =
                            fromToken(FilledTextFieldTokens.DisabledInputColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                        errorTextColor = fromToken(FilledTextFieldTokens.ErrorInputColor),
                        focusedContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                        unfocusedContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                        disabledContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                        errorContainerColor = fromToken(FilledTextFieldTokens.ContainerColor),
                        cursorColor = fromToken(FilledTextFieldTokens.CaretColor),
                        errorCursorColor = fromToken(FilledTextFieldTokens.ErrorFocusCaretColor),
                        textSelectionColors = LocalTextSelectionColors.current,
                        focusedIndicatorColor =
                            fromToken(FilledTextFieldTokens.FocusActiveIndicatorColor),
                        unfocusedIndicatorColor =
                            fromToken(FilledTextFieldTokens.ActiveIndicatorColor),
                        disabledIndicatorColor =
                            fromToken(FilledTextFieldTokens.DisabledActiveIndicatorColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledActiveIndicatorOpacity),
                        errorIndicatorColor =
                            fromToken(FilledTextFieldTokens.ErrorActiveIndicatorColor),
                        focusedLeadingIconColor =
                            fromToken(FilledTextFieldTokens.FocusLeadingIconColor),
                        unfocusedLeadingIconColor =
                            fromToken(FilledTextFieldTokens.LeadingIconColor),
                        disabledLeadingIconColor =
                            fromToken(FilledTextFieldTokens.DisabledLeadingIconColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
                        errorLeadingIconColor =
                            fromToken(FilledTextFieldTokens.ErrorLeadingIconColor),
                        focusedTrailingIconColor =
                            fromToken(FilledTextFieldTokens.FocusTrailingIconColor),
                        unfocusedTrailingIconColor =
                            fromToken(FilledTextFieldTokens.TrailingIconColor),
                        disabledTrailingIconColor =
                            fromToken(FilledTextFieldTokens.DisabledTrailingIconColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
                        errorTrailingIconColor =
                            fromToken(FilledTextFieldTokens.ErrorTrailingIconColor),
                        focusedLabelColor = fromToken(FilledTextFieldTokens.FocusLabelColor),
                        unfocusedLabelColor = fromToken(FilledTextFieldTokens.LabelColor),
                        disabledLabelColor =
                            fromToken(FilledTextFieldTokens.DisabledLabelColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledLabelOpacity),
                        errorLabelColor = fromToken(FilledTextFieldTokens.ErrorLabelColor),
                        focusedPlaceholderColor =
                            fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                        unfocusedPlaceholderColor =
                            fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                        disabledPlaceholderColor =
                            fromToken(FilledTextFieldTokens.DisabledInputColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                        errorPlaceholderColor =
                            fromToken(FilledTextFieldTokens.InputPlaceholderColor),
                        focusedSupportingTextColor =
                            fromToken(FilledTextFieldTokens.FocusSupportingColor),
                        unfocusedSupportingTextColor =
                            fromToken(FilledTextFieldTokens.SupportingColor),
                        disabledSupportingTextColor =
                            fromToken(FilledTextFieldTokens.DisabledSupportingColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledSupportingOpacity),
                        errorSupportingTextColor =
                            fromToken(FilledTextFieldTokens.ErrorSupportingColor),
                        focusedPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                        unfocusedPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                        disabledPrefixColor =
                            fromToken(FilledTextFieldTokens.InputPrefixColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                        errorPrefixColor = fromToken(FilledTextFieldTokens.InputPrefixColor),
                        focusedSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
                        unfocusedSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
                        disabledSuffixColor =
                            fromToken(FilledTextFieldTokens.InputSuffixColor)
                                .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
                        errorSuffixColor = fromToken(FilledTextFieldTokens.InputSuffixColor),
                    )
                    .also { defaultTextFieldColorsCached = it }
        }

    @Deprecated(
        message = "Renamed to TextFieldDefaults.Container",
        replaceWith =
            ReplaceWith(
                "Container(\n" +
                    "    enabled = enabled,\n" +
                    "    isError = isError,\n" +
                    "    interactionSource = interactionSource,\n" +
                    "    colors = colors,\n" +
                    "    shape = shape,\n" +
                    ")"
            ),
        level = DeprecationLevel.WARNING
    )
    @ExperimentalMaterial3Api
    @Composable
    fun ContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors,
        shape: Shape = TextFieldDefaults.shape,
    ) =
        Container(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            modifier = Modifier,
            colors = colors,
            shape = shape,
            focusedIndicatorLineThickness = FocusedIndicatorThickness,
            unfocusedIndicatorLineThickness = UnfocusedIndicatorThickness,
        )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.shape`",
        replaceWith =
            ReplaceWith(
                "OutlinedTextFieldDefaults.shape",
                "androidx.compose.material.OutlinedTextFieldDefaults"
            ),
        level = DeprecationLevel.WARNING
    )
    val outlinedShape: Shape
        @Composable get() = OutlinedTextFieldDefaults.shape

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.shape`",
        replaceWith = ReplaceWith("TextFieldDefaults.shape"),
        level = DeprecationLevel.WARNING
    )
    val filledShape: Shape
        @Composable get() = shape

    @Deprecated(
        message =
            "Split into `TextFieldDefaults.UnfocusedIndicatorThickness` and " +
                "`OutlinedTextFieldDefaults.UnfocusedBorderThickness`. Please update as appropriate.",
        replaceWith = ReplaceWith("TextFieldDefaults.UnfocusedIndicatorThickness"),
        level = DeprecationLevel.WARNING,
    )
    val UnfocusedBorderThickness = UnfocusedIndicatorThickness

    @Deprecated(
        message =
            "Split into `TextFieldDefaults.FocusedIndicatorThickness` and " +
                "`OutlinedTextFieldDefaults.FocusedBorderThickness`. Please update as appropriate.",
        replaceWith = ReplaceWith("TextFieldDefaults.FocusedIndicatorThickness"),
        level = DeprecationLevel.WARNING,
    )
    val FocusedBorderThickness = FocusedIndicatorThickness

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.contentPaddingWithLabel`",
        replaceWith =
            ReplaceWith(
                "TextFieldDefaults.contentPaddingWithLabel(\n" +
                    "        start = start,\n" +
                    "        top = top,\n" +
                    "        end = end,\n" +
                    "        bottom = bottom,\n" +
                    "    )"
            ),
        level = DeprecationLevel.WARNING
    )
    fun textFieldWithLabelPadding(
        start: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        top: Dp = TextFieldWithLabelVerticalPadding,
        bottom: Dp = TextFieldWithLabelVerticalPadding
    ): PaddingValues =
        contentPaddingWithLabel(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )

    @Deprecated(
        message = "Renamed to `TextFieldDefaults.contentPaddingWithoutLabel`",
        replaceWith =
            ReplaceWith(
                "TextFieldDefaults.contentPaddingWithoutLabel(\n" +
                    "        start = start,\n" +
                    "        top = top,\n" +
                    "        end = end,\n" +
                    "        bottom = bottom,\n" +
                    "    )"
            ),
        level = DeprecationLevel.WARNING
    )
    fun textFieldWithoutLabelPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues =
        contentPaddingWithoutLabel(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )

    @Deprecated(
        message = "Renamed to `OutlinedTextFieldDefaults.contentPadding`",
        replaceWith =
            ReplaceWith(
                "OutlinedTextFieldDefaults.contentPadding(\n" +
                    "        start = start,\n" +
                    "        top = top,\n" +
                    "        end = end,\n" +
                    "        bottom = bottom,\n" +
                    "    )",
                "androidx.compose.material.OutlinedTextFieldDefaults"
            ),
        level = DeprecationLevel.WARNING
    )
    fun outlinedTextFieldPadding(
        start: Dp = TextFieldPadding,
        top: Dp = TextFieldPadding,
        end: Dp = TextFieldPadding,
        bottom: Dp = TextFieldPadding
    ): PaddingValues =
        OutlinedTextFieldDefaults.contentPadding(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
        )
}

/**
 * Contains the default values used by [OutlinedTextField]. For defaults used in [TextField], see
 * [TextFieldDefaults].
 */
@Immutable
object OutlinedTextFieldDefaults {
    /** Default shape for an [OutlinedTextField]. */
    val shape: Shape
        @Composable get() = OutlinedTextFieldTokens.ContainerShape.value

    /**
     * The default min height applied to an [OutlinedTextField]. Note that you can override it by
     * applying Modifier.heightIn directly on a text field.
     */
    val MinHeight = 56.dp

    /**
     * The default min width applied to an [OutlinedTextField]. Note that you can override it by
     * applying Modifier.widthIn directly on a text field.
     */
    val MinWidth = 280.dp

    /** The default thickness of the border in [OutlinedTextField] in unfocused state. */
    val UnfocusedBorderThickness = 1.dp

    /** The default thickness of the border in [OutlinedTextField] in focused state. */
    val FocusedBorderThickness = 2.dp

    /**
     * A decorator used to create custom text fields based on <a
     * href="https://m3.material.io/components/text-fields/overview" class="external"
     * target="_blank">Material Design outlined text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [OutlinedTextField],
     * such as the border thickness, consider using this decorator to achieve the desired design.
     *
     * For example, if you wish to customize the thickness of the border, you can pass a custom
     * [Container] to this decoration box's [container].
     *
     * This decorator is meant to be used in conjunction with the overload of [BasicTextField] that
     * accepts a [TextFieldDecorator] parameter. For other overloads of [BasicTextField] that use a
     * `decorationBox`, see [DecorationBox].
     *
     * An example of building a custom text field using [decorator]:
     *
     * @sample androidx.compose.material3.samples.CustomOutlinedTextFieldUsingDecorator
     * @param state [TextFieldState] object that holds the internal editing state of the text field.
     * @param enabled the enabled state of the text field. When `false`, this decorator will appear
     *   visually disabled. This must be the same value that is passed to [BasicTextField].
     * @param lineLimits whether the text field is [SingleLine] or [MultiLine]. This must be the
     *   same value that is passed to [BasicTextField].
     * @param outputTransformation [OutputTransformation] that transforms how the contents of the
     *   text field are presented. This must be the same value that is passed to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     *   [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     *   [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     *   then pass the same instance to this decorator to observe [Interaction]s and customize the
     *   appearance/behavior of the text field in different states.
     * @param labelPosition the position of the label. See [TextFieldLabelPosition].
     * @param label the optional label to be displayed with this text field. The default text style
     *   uses [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
     * @param placeholder the optional placeholder to be displayed when the input text is empty. The
     *   default text style uses [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     *   field container.
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     *   container.
     * @param prefix the optional prefix to be displayed before the input text in the text field.
     * @param suffix the optional suffix to be displayed after the input text in the text field.
     * @param supportingText the optional supporting text to be displayed below the text field.
     * @param isError indicates if the text field's current value is in an error state. When `true`,
     *   this decorator will display its contents in an error color.
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     *   field decorator in different states. See [OutlinedTextFieldDefaults.colors].
     * @param contentPadding the padding between the input field and the surrounding elements of the
     *   decorator. Note that the padding values may not be respected if they are incompatible with
     *   the text field's size constraints or layout. See
     *   [OutlinedTextFieldDefaults.contentPadding].
     * @param container the container to be drawn behind the text field. By default, this is
     *   transparent and only includes a border. The cutout in the border to fit the [label] will be
     *   automatically added by the framework. Default colors for the container come from the
     *   [colors].
     */
    @Composable
    @ExperimentalMaterial3Api
    fun decorator(
        state: TextFieldState,
        enabled: Boolean,
        lineLimits: TextFieldLineLimits,
        outputTransformation: OutputTransformation?,
        interactionSource: InteractionSource,
        labelPosition: TextFieldLabelPosition = TextFieldLabelPosition.Default(),
        label: @Composable (TextFieldLabelScope.() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        leadingIcon: @Composable (() -> Unit)? = null,
        trailingIcon: @Composable (() -> Unit)? = null,
        prefix: @Composable (() -> Unit)? = null,
        suffix: @Composable (() -> Unit)? = null,
        supportingText: @Composable (() -> Unit)? = null,
        isError: Boolean = false,
        colors: TextFieldColors = colors(),
        contentPadding: PaddingValues = contentPadding(),
        container: @Composable () -> Unit = {
            Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                shape = shape,
                focusedBorderThickness = FocusedBorderThickness,
                unfocusedBorderThickness = UnfocusedBorderThickness,
            )
        }
    ): TextFieldDecorator = TextFieldDecorator { innerTextField ->
        val visualText =
            if (outputTransformation == null) state.text
            else {
                // TODO: use constructor to create TextFieldBuffer from TextFieldState when
                // available
                lateinit var buffer: TextFieldBuffer
                state.edit { buffer = this }
                // after edit completes, mutations on buffer are ineffective
                with(outputTransformation) { buffer.transformOutput() }
                buffer.asCharSequence()
            }

        CommonDecorationBox(
            type = TextFieldType.Outlined,
            visualText = visualText,
            innerTextField = innerTextField,
            placeholder = placeholder,
            labelPosition = labelPosition,
            label = label,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            prefix = prefix,
            suffix = suffix,
            supportingText = supportingText,
            singleLine = lineLimits == SingleLine,
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = contentPadding,
            container = container,
        )
    }

    /**
     * Composable that draws a default container for an [OutlinedTextField] with a border stroke.
     * You can apply it to a [BasicTextField] using [DecorationBox] to create a custom text field
     * based on the styling of a Material outlined text field. The [OutlinedTextField] component
     * applies it automatically.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param interactionSource the [InteractionSource] of the text field. Used to determine if the
     *   text field is in focus or not
     * @param modifier the [Modifier] of this container
     * @param colors [TextFieldColors] used to resolve colors of the text field
     * @param shape the shape of this container
     * @param focusedBorderThickness thickness of the border when the text field is focused
     * @param unfocusedBorderThickness thickness of the border when the text field is not focused
     */
    @ExperimentalMaterial3Api
    @Composable
    fun Container(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        modifier: Modifier = Modifier,
        colors: TextFieldColors = colors(),
        shape: Shape = OutlinedTextFieldDefaults.shape,
        focusedBorderThickness: Dp = FocusedBorderThickness,
        unfocusedBorderThickness: Dp = UnfocusedBorderThickness,
    ) {
        val focused = interactionSource.collectIsFocusedAsState().value
        val borderStroke =
            animateBorderStrokeAsState(
                enabled,
                isError,
                focused,
                colors,
                focusedBorderThickness,
                unfocusedBorderThickness,
            )
        // TODO Load the motionScheme tokens from the component tokens file
        val containerColor =
            animateColorAsState(
                targetValue = colors.containerColor(enabled, isError, focused),
                animationSpec = MotionSchemeKeyTokens.FastEffects.value(),
            )
        Box(
            modifier
                .border(borderStroke.value, shape)
                .textFieldBackground(containerColor::value, shape)
        )
    }

    /**
     * A decoration box used to create custom text fields based on <a
     * href="https://m3.material.io/components/text-fields/overview" class="external"
     * target="_blank">Material Design outlined text field</a>.
     *
     * If your text field requires customising elements that aren't exposed by [OutlinedTextField],
     * consider using this decoration box to achieve the desired design.
     *
     * For example, if you wish to customize the thickness of the border, you can pass a custom
     * [Container] to this decoration box's [container].
     *
     * This decoration box is meant to be used in conjunction with overloads of [BasicTextField]
     * that accept a `decorationBox` parameter. For other overloads of [BasicTextField] that use a
     * [TextFieldDecorator], see [decorator].
     *
     * An example of building a custom text field using [DecorationBox]:
     *
     * @sample androidx.compose.material3.samples.CustomOutlinedTextFieldBasedOnDecorationBox
     * @param value the input [String] shown by the text field
     * @param innerTextField input text field that this decoration box wraps. You will pass here a
     *   framework-controlled composable parameter "innerTextField" from the decorationBox lambda of
     *   the [BasicTextField]
     * @param enabled the enabled state of the text field. When `false`, this decoration box will
     *   appear visually disabled. This must be the same value that is passed to [BasicTextField].
     * @param singleLine indicates if this is a single line or multi line text field. This must be
     *   the same value that is passed to [BasicTextField].
     * @param visualTransformation transforms the visual representation of the input [value]. This
     *   must be the same value that is passed to [BasicTextField].
     * @param interactionSource the read-only [InteractionSource] representing the stream of
     *   [Interaction]s for this text field. You must first create and pass in your own `remember`ed
     *   [MutableInteractionSource] instance to the [BasicTextField] for it to dispatch events. And
     *   then pass the same instance to this decoration box to observe [Interaction]s and customize
     *   the appearance / behavior of this text field in different states.
     * @param isError indicates if the text field's current value is in an error state. When `true`,
     *   this decoration box will display its contents in an error color.
     * @param label the optional label to be displayed with this text field. The default text style
     *   uses [Typography.bodySmall] when minimized and [Typography.bodyLarge] when expanded.
     * @param placeholder the optional placeholder to be displayed when the text field is in focus
     *   and the input text is empty. The default text style for internal [Text] is
     *   [Typography.bodyLarge].
     * @param leadingIcon the optional leading icon to be displayed at the beginning of the text
     *   field container
     * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
     *   container
     * @param prefix the optional prefix to be displayed before the input text in the text field
     * @param suffix the optional suffix to be displayed after the input text in the text field
     * @param supportingText the optional supporting text to be displayed below the text field
     * @param colors [TextFieldColors] that will be used to resolve the colors used for this text
     *   field in different states. See [OutlinedTextFieldDefaults.colors].
     * @param contentPadding the padding between the input field and the surrounding elements of the
     *   decoration box. Note that the padding values may not be respected if they are incompatible
     *   with the text field's size constraints or layout. See
     *   [OutlinedTextFieldDefaults.contentPadding].
     * @param container the container to be drawn behind the text field. By default, this is
     *   transparent and only includes a border. The cutout in the border to fit the [label] will be
     *   automatically added by the framework. Default colors for the container come from the
     *   [colors].
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
            Container(
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                modifier = Modifier,
                colors = colors,
                shape = shape,
                focusedBorderThickness = FocusedBorderThickness,
                unfocusedBorderThickness = UnfocusedBorderThickness,
            )
        }
    ) {
        val visualText =
            remember(value, visualTransformation) {
                    visualTransformation.filter(AnnotatedString(value))
                }
                .text
                .text

        CommonDecorationBox(
            type = TextFieldType.Outlined,
            visualText = visualText,
            innerTextField = innerTextField,
            placeholder = placeholder,
            labelPosition = TextFieldLabelPosition.Default(),
            label = label?.let { { it.invoke() } },
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
            container = container,
        )
    }

    /**
     * Default content padding of the input field within the [OutlinedTextField].
     *
     * Horizontal padding represents the distance between the input field and the leading/trailing
     * icons (if present) or the horizontal edges of the container if there are no icons.
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
    @Composable fun colors() = MaterialTheme.colorScheme.defaultOutlinedTextFieldColors

    /**
     * Creates a [TextFieldColors] that represents the default input text, container, and content
     * colors (including label, placeholder, icons, etc.) used in an [OutlinedTextField].
     *
     * @param focusedTextColor the color used for the input text of this text field when focused
     * @param unfocusedTextColor the color used for the input text of this text field when not
     *   focused
     * @param disabledTextColor the color used for the input text of this text field when disabled
     * @param errorTextColor the color used for the input text of this text field when in error
     *   state
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
     * @param unfocusedTrailingIconColor the trailing icon color for this text field when not
     *   focused
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
     *   focused
     * @param disabledSupportingTextColor the supporting text color for this text field when
     *   disabled
     * @param errorSupportingTextColor the supporting text color for this text field when in error
     *   state
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
            return defaultOutlinedTextFieldColorsCached?.let { cachedColors ->
                val localTextSelectionColors = LocalTextSelectionColors.current
                if (cachedColors.textSelectionColors == localTextSelectionColors) {
                    cachedColors
                } else {
                    cachedColors.copy(textSelectionColors = localTextSelectionColors).also {
                        defaultOutlinedTextFieldColorsCached = it
                    }
                }
            }
                ?: TextFieldColors(
                        focusedTextColor = fromToken(OutlinedTextFieldTokens.FocusInputColor),
                        unfocusedTextColor = fromToken(OutlinedTextFieldTokens.InputColor),
                        disabledTextColor =
                            fromToken(OutlinedTextFieldTokens.DisabledInputColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                        errorTextColor = fromToken(OutlinedTextFieldTokens.ErrorInputColor),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                        cursorColor = fromToken(OutlinedTextFieldTokens.CaretColor),
                        errorCursorColor = fromToken(OutlinedTextFieldTokens.ErrorFocusCaretColor),
                        textSelectionColors = LocalTextSelectionColors.current,
                        focusedIndicatorColor =
                            fromToken(OutlinedTextFieldTokens.FocusOutlineColor),
                        unfocusedIndicatorColor = fromToken(OutlinedTextFieldTokens.OutlineColor),
                        disabledIndicatorColor =
                            fromToken(OutlinedTextFieldTokens.DisabledOutlineColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledOutlineOpacity),
                        errorIndicatorColor = fromToken(OutlinedTextFieldTokens.ErrorOutlineColor),
                        focusedLeadingIconColor =
                            fromToken(OutlinedTextFieldTokens.FocusLeadingIconColor),
                        unfocusedLeadingIconColor =
                            fromToken(OutlinedTextFieldTokens.LeadingIconColor),
                        disabledLeadingIconColor =
                            fromToken(OutlinedTextFieldTokens.DisabledLeadingIconColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledLeadingIconOpacity),
                        errorLeadingIconColor =
                            fromToken(OutlinedTextFieldTokens.ErrorLeadingIconColor),
                        focusedTrailingIconColor =
                            fromToken(OutlinedTextFieldTokens.FocusTrailingIconColor),
                        unfocusedTrailingIconColor =
                            fromToken(OutlinedTextFieldTokens.TrailingIconColor),
                        disabledTrailingIconColor =
                            fromToken(OutlinedTextFieldTokens.DisabledTrailingIconColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledTrailingIconOpacity),
                        errorTrailingIconColor =
                            fromToken(OutlinedTextFieldTokens.ErrorTrailingIconColor),
                        focusedLabelColor = fromToken(OutlinedTextFieldTokens.FocusLabelColor),
                        unfocusedLabelColor = fromToken(OutlinedTextFieldTokens.LabelColor),
                        disabledLabelColor =
                            fromToken(OutlinedTextFieldTokens.DisabledLabelColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledLabelOpacity),
                        errorLabelColor = fromToken(OutlinedTextFieldTokens.ErrorLabelColor),
                        focusedPlaceholderColor =
                            fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                        unfocusedPlaceholderColor =
                            fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                        disabledPlaceholderColor =
                            fromToken(OutlinedTextFieldTokens.DisabledInputColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                        errorPlaceholderColor =
                            fromToken(OutlinedTextFieldTokens.InputPlaceholderColor),
                        focusedSupportingTextColor =
                            fromToken(OutlinedTextFieldTokens.FocusSupportingColor),
                        unfocusedSupportingTextColor =
                            fromToken(OutlinedTextFieldTokens.SupportingColor),
                        disabledSupportingTextColor =
                            fromToken(OutlinedTextFieldTokens.DisabledSupportingColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledSupportingOpacity),
                        errorSupportingTextColor =
                            fromToken(OutlinedTextFieldTokens.ErrorSupportingColor),
                        focusedPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                        unfocusedPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                        disabledPrefixColor =
                            fromToken(OutlinedTextFieldTokens.InputPrefixColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                        errorPrefixColor = fromToken(OutlinedTextFieldTokens.InputPrefixColor),
                        focusedSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor),
                        unfocusedSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor),
                        disabledSuffixColor =
                            fromToken(OutlinedTextFieldTokens.InputSuffixColor)
                                .copy(alpha = OutlinedTextFieldTokens.DisabledInputOpacity),
                        errorSuffixColor = fromToken(OutlinedTextFieldTokens.InputSuffixColor)
                    )
                    .also { defaultOutlinedTextFieldColorsCached = it }
        }

    @Deprecated(
        message = "Renamed to OutlinedTextFieldDefaults.Container",
        replaceWith =
            ReplaceWith(
                "Container(\n" +
                    "    enabled = enabled,\n" +
                    "    isError = isError,\n" +
                    "    interactionSource = interactionSource,\n" +
                    "    colors = colors,\n" +
                    "    shape = shape,\n" +
                    "    focusedBorderThickness = focusedBorderThickness,\n" +
                    "    unfocusedBorderThickness = unfocusedBorderThickness,\n" +
                    ")"
            ),
        level = DeprecationLevel.WARNING
    )
    @ExperimentalMaterial3Api
    @Composable
    fun ContainerBox(
        enabled: Boolean,
        isError: Boolean,
        interactionSource: InteractionSource,
        colors: TextFieldColors = colors(),
        shape: Shape = OutlinedTextFieldDefaults.shape,
        focusedBorderThickness: Dp = FocusedBorderThickness,
        unfocusedBorderThickness: Dp = UnfocusedBorderThickness,
    ) =
        Container(
            enabled = enabled,
            isError = isError,
            interactionSource = interactionSource,
            modifier = Modifier,
            colors = colors,
            shape = shape,
            focusedBorderThickness = focusedBorderThickness,
            unfocusedBorderThickness = unfocusedBorderThickness,
        )
}

/**
 * Represents the colors of the input text, container, and content (including label, placeholder,
 * leading and trailing icons) used in a text field in different states.
 *
 * @param focusedTextColor the color used for the input text of this text field when focused
 * @param unfocusedTextColor the color used for the input text of this text field when not focused
 * @param disabledTextColor the color used for the input text of this text field when disabled
 * @param errorTextColor the color used for the input text of this text field when in error state
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
 *   focused
 * @param disabledSupportingTextColor the supporting text color for this text field when disabled
 * @param errorSupportingTextColor the supporting text color for this text field when in error state
 * @param focusedPrefixColor the prefix color for this text field when focused
 * @param unfocusedPrefixColor the prefix color for this text field when not focused
 * @param disabledPrefixColor the prefix color for this text field when disabled
 * @param errorPrefixColor the prefix color for this text field when in error state
 * @param focusedSuffixColor the suffix color for this text field when focused
 * @param unfocusedSuffixColor the suffix color for this text field when not focused
 * @param disabledSuffixColor the suffix color for this text field when disabled
 * @param errorSuffixColor the suffix color for this text field when in error state
 * @constructor create an instance with arbitrary colors. See [TextFieldDefaults.colors] for the
 *   default colors used in [TextField]. See [OutlinedTextFieldDefaults.colors] for the default
 *   colors used in [OutlinedTextField].
 */
@Immutable
class TextFieldColors
constructor(
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
     * Returns a copy of this ChipColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
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
    ) =
        TextFieldColors(
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

    internal fun TextSelectionColors?.takeOrElse(
        block: () -> TextSelectionColors
    ): TextSelectionColors = this ?: block()

    /**
     * Represents the color used for the leading icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun leadingIconColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledLeadingIconColor
            isError -> errorLeadingIconColor
            focused -> focusedLeadingIconColor
            else -> unfocusedLeadingIconColor
        }

    /**
     * Represents the color used for the trailing icon of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun trailingIconColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledTrailingIconColor
            isError -> errorTrailingIconColor
            focused -> focusedTrailingIconColor
            else -> unfocusedTrailingIconColor
        }

    /**
     * Represents the color used for the border indicator of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun indicatorColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledIndicatorColor
            isError -> errorIndicatorColor
            focused -> focusedIndicatorColor
            else -> unfocusedIndicatorColor
        }

    /**
     * Represents the container color for this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun containerColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledContainerColor
            isError -> errorContainerColor
            focused -> focusedContainerColor
            else -> unfocusedContainerColor
        }

    /**
     * Represents the color used for the placeholder of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun placeholderColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledPlaceholderColor
            isError -> errorPlaceholderColor
            focused -> focusedPlaceholderColor
            else -> unfocusedPlaceholderColor
        }

    /**
     * Represents the color used for the label of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun labelColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledLabelColor
            isError -> errorLabelColor
            focused -> focusedLabelColor
            else -> unfocusedLabelColor
        }

    /**
     * Represents the color used for the input field of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun textColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledTextColor
            isError -> errorTextColor
            focused -> focusedTextColor
            else -> unfocusedTextColor
        }

    /**
     * Represents the colors used for the supporting text of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun supportingTextColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledSupportingTextColor
            isError -> errorSupportingTextColor
            focused -> focusedSupportingTextColor
            else -> unfocusedSupportingTextColor
        }

    /**
     * Represents the color used for the prefix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun prefixColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledPrefixColor
            isError -> errorPrefixColor
            focused -> focusedPrefixColor
            else -> unfocusedPrefixColor
        }

    /**
     * Represents the color used for the suffix of this text field.
     *
     * @param enabled whether the text field is enabled
     * @param isError whether the text field's current value is in error
     * @param focused whether the text field is in focus
     */
    @Stable
    internal fun suffixColor(
        enabled: Boolean,
        isError: Boolean,
        focused: Boolean,
    ): Color =
        when {
            !enabled -> disabledSuffixColor
            isError -> errorSuffixColor
            focused -> focusedSuffixColor
            else -> unfocusedSuffixColor
        }

    /**
     * Represents the color used for the cursor of this text field.
     *
     * @param isError whether the text field's current value is in error
     */
    @Stable
    internal fun cursorColor(isError: Boolean): Color =
        if (isError) errorCursorColor else cursorColor

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

/** The position of the label with respect to the text field. */
abstract class TextFieldLabelPosition private constructor() {
    /**
     * The default label position.
     *
     * For [TextField], the label is positioned inside the text field container. For
     * [OutlinedTextField], the label is positioned inside the text field container when expanded
     * and cuts into the border when minimized.
     */
    class Default(
        @get:Suppress("GetterSetterNames") override val alwaysMinimize: Boolean = false,
        override val minimizedAlignment: Alignment.Horizontal = Alignment.Start,
        override val expandedAlignment: Alignment.Horizontal = Alignment.Start,
    ) : TextFieldLabelPosition() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Default) return false

            if (alwaysMinimize != other.alwaysMinimize) return false
            if (minimizedAlignment != other.minimizedAlignment) return false
            if (expandedAlignment != other.expandedAlignment) return false

            return true
        }

        override fun hashCode(): Int {
            var result = alwaysMinimize.hashCode()
            result = 31 * result + minimizedAlignment.hashCode()
            result = 31 * result + expandedAlignment.hashCode()
            return result
        }

        override fun toString(): String {
            return "Default(" +
                "alwaysMinimize=$alwaysMinimize, " +
                "minimizedAlignment=$minimizedAlignment, " +
                "expandedAlignment=$expandedAlignment" +
                ")"
        }
    }

    /**
     * The label is positioned above and outside the text field container. This results in the label
     * always being minimized.
     */
    class Above(override val minimizedAlignment: Alignment.Horizontal = Alignment.Start) :
        TextFieldLabelPosition() {
        @get:Suppress("GetterSetterNames")
        override val alwaysMinimize: Boolean
            get() = true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Above) return false

            return minimizedAlignment == other.minimizedAlignment
        }

        override fun hashCode(): Int {
            return minimizedAlignment.hashCode()
        }

        override fun toString(): String = "Above(minimizedAlignment=$minimizedAlignment)"
    }

    /**
     * Whether to always keep the label of the text field minimized.
     *
     * If `false`, the label will expand to occupy the input area when the text field is unfocused
     * and empty. If `true`, this allows displaying the placeholder, prefix, and suffix alongside
     * the label when the text field is unfocused and empty.
     */
    @get:Suppress("GetterSetterNames") abstract val alwaysMinimize: Boolean

    /** The horizontal alignment of the label when it is minimized. */
    abstract val minimizedAlignment: Alignment.Horizontal

    /** The horizontal alignment of the label when it is expanded. */
    open val expandedAlignment: Alignment.Horizontal
        get() = minimizedAlignment
}

/** Scope for the label of a [TextField] or [OutlinedTextField]. */
@Stable
interface TextFieldLabelScope {
    /**
     * The animation progress of a label between its expanded and minimized sizes, where 0
     * represents an expanded label and 1 represents a minimized label.
     *
     * Label animation is handled by the framework when using a component that reads from
     * [LocalTextStyle], such as the default [Text]. This [progress] value can be used to coordinate
     * other animations in conjunction with the default animation.
     */
    @get:FloatRange(from = 0.0, to = 1.0) val progress: Float
}
