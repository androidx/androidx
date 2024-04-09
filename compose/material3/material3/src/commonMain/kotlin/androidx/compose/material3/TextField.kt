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

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
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
import androidx.compose.material3.internal.TextFieldPadding
import androidx.compose.material3.internal.TrailingId
import androidx.compose.material3.internal.ZeroConstraints
import androidx.compose.material3.internal.defaultErrorSemantics
import androidx.compose.material3.internal.getString
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.layoutId
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
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
 * <a href="https://m3.material.io/components/text-fields/overview" class="external" target="_blank">Material Design filled text field</a>.
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * A simple single line text field looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTextFieldSample
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
 * Password text field example:
 *
 * @sample androidx.compose.material3.samples.PasswordTextField
 *
 * Hiding a software keyboard on IME action performed:
 *
 * @sample androidx.compose.material3.samples.TextFieldWithHideKeyboardOnImeAction
 *
 * If apart from input text change you also want to observe the cursor location, selection range,
 * or IME composition use the TextField overload with the [TextFieldValue] parameter instead.
 *
 * @param value the input text to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates the text. An
 * updated text comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 * be modified. However, a user can focus it and copy text from it. Read-only text fields are
 * usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
 * [Typography.bodyLarge] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error. If set to true, the
 * label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value]
 * For example, you can use
 * [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 * create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 * instead of wrapping onto multiple lines. The keyboard will be informed to not show the return key
 * as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines attribute will
 * be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this text field. You can use this to change the text field's
 * appearance or preview the text field in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 * in different states. See [TextFieldDefaults.colors].
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
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        val focused = interactionSource.collectIsFocusedAsState().value
        colors.textColor(enabled, isError, focused)
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier = modifier
                .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
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
            decorationBox = @Composable { innerTextField ->
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
                    colors = colors
                )
            }
        )
    }
}

/**
 * <a href="https://m3.material.io/components/text-fields/overview" class="external" target="_blank">Material Design filled text field</a>.
 *
 * Text fields allow users to enter text into a UI. They typically appear in forms and dialogs.
 * Filled text fields have more visual emphasis than outlined text fields, making them stand out
 * when surrounded by other content and components.
 *
 * ![Filled text field image](https://developer.android.com/images/reference/androidx/compose/material3/filled-text-field.png)
 *
 * If you are looking for an outlined version, see [OutlinedTextField].
 *
 * See example usage:
 * @sample androidx.compose.material3.samples.TextFieldSample
 *
 * This overload provides access to the input text, cursor position, selection range and
 * IME composition. If you only want to observe an input text change, use the TextField
 * overload with the [String] parameter instead.
 *
 * @param value the input [TextFieldValue] to be shown in the text field
 * @param onValueChange the callback that is triggered when the input service updates values in
 * [TextFieldValue]. An updated [TextFieldValue] comes as a parameter of the callback
 * @param modifier the [Modifier] to be applied to this text field
 * @param enabled controls the enabled state of this text field. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param readOnly controls the editable state of the text field. When `true`, the text field cannot
 * be modified. However, a user can focus it and copy text from it. Read-only text fields are
 * usually used to display pre-filled forms that a user cannot edit.
 * @param textStyle the style to be applied to the input text. Defaults to [LocalTextStyle].
 * @param label the optional label to be displayed inside the text field container. The default
 * text style for internal [Text] is [Typography.bodySmall] when the text field is in focus and
 * [Typography.bodyLarge] when the text field is not in focus
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.bodyLarge]
 * @param leadingIcon the optional leading icon to be displayed at the beginning of the text field
 * container
 * @param trailingIcon the optional trailing icon to be displayed at the end of the text field
 * container
 * @param prefix the optional prefix to be displayed before the input text in the text field
 * @param suffix the optional suffix to be displayed after the input text in the text field
 * @param supportingText the optional supporting text to be displayed below the text field
 * @param isError indicates if the text field's current value is in error state. If set to
 * true, the label, bottom indicator and trailing icon by default will be displayed in error color
 * @param visualTransformation transforms the visual representation of the input [value].
 * For example, you can use
 * [PasswordVisualTransformation][androidx.compose.ui.text.input.PasswordVisualTransformation] to
 * create a password text field. By default, no visual transformation is applied.
 * @param keyboardOptions software keyboard options that contains configuration such as
 * [KeyboardType] and [ImeAction].
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param singleLine when `true`, this text field becomes a single horizontally scrolling text field
 * instead of wrapping onto multiple lines. The keyboard will be informed to not show the return key
 * as the [ImeAction]. Note that [maxLines] parameter will be ignored as the maxLines attribute will
 * be automatically set to 1.
 * @param maxLines the maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param minLines the minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines]. This parameter is ignored when [singleLine] is true.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this text field. You can use this to change the text field's
 * appearance or preview the text field in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param shape defines the shape of this text field's container
 * @param colors [TextFieldColors] that will be used to resolve the colors used for this text field
 * in different states. See [TextFieldDefaults.colors].
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
    colors: TextFieldColors = TextFieldDefaults.colors()
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        val focused = interactionSource.collectIsFocusedAsState().value
        colors.textColor(enabled, isError, focused)
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier = modifier
                .defaultErrorSemantics(isError, getString(Strings.DefaultErrorMessage))
                .defaultMinSize(
                    minWidth = TextFieldDefaults.MinWidth,
                    minHeight = TextFieldDefaults.MinHeight
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
            decorationBox = @Composable { innerTextField ->
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
                    colors = colors
                )
            }
        )
    }
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
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    singleLine: Boolean,
    animationProgress: Float,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues
) {
    val measurePolicy = remember(singleLine, animationProgress, paddingValues) {
        TextFieldMeasurePolicy(singleLine, animationProgress, paddingValues)
    }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            // The container is given as a Composable instead of a background modifier so that
            // elements like supporting text can be placed outside of it while still contributing
            // to the text field's measurements overall.
            container()

            if (leading != null) {
                Box(
                    modifier = Modifier
                        .layoutId(LeadingId)
                        .then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier
                        .layoutId(TrailingId)
                        .then(IconDefaultSizeModifier),
                    contentAlignment = Alignment.Center
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val startPadding = if (leading != null) {
                (startTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
            } else {
                startTextFieldPadding
            }
            val endPadding = if (trailing != null) {
                (endTextFieldPadding - HorizontalIconPadding).coerceAtLeast(0.dp)
            } else {
                endTextFieldPadding
            }

            if (prefix != null) {
                Box(
                    Modifier
                        .layoutId(PrefixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = startPadding, end = PrefixSuffixTextPadding)
                ) {
                    prefix()
                }
            }
            if (suffix != null) {
                Box(
                    Modifier
                        .layoutId(SuffixId)
                        .heightIn(min = MinTextLineHeight)
                        .wrapContentHeight()
                        .padding(start = PrefixSuffixTextPadding, end = endPadding)
                ) {
                    suffix()
                }
            }

            if (label != null) {
                Box(
                    Modifier
                        .layoutId(LabelId)
                        .heightIn(min = lerp(
                            MinTextLineHeight, MinFocusedLabelLineHeight, animationProgress))
                        .wrapContentHeight()
                        .padding(start = startPadding, end = endPadding)) { label() }
            }

            val textPadding = Modifier
                .heightIn(min = MinTextLineHeight)
                .wrapContentHeight()
                .padding(
                    start = if (prefix == null) startPadding else 0.dp,
                    end = if (suffix == null) endPadding else 0.dp,
                )

            if (placeholder != null) {
                placeholder(Modifier
                    .layoutId(PlaceholderId)
                    .then(textPadding))
            }
            Box(
                modifier = Modifier
                    .layoutId(TextFieldId)
                    .then(textPadding),
                propagateMinConstraints = true,
            ) {
                textField()
            }

            if (supporting != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                Box(Modifier
                    .layoutId(SupportingId)
                    .heightIn(min = MinSupportingTextLineHeight)
                    .wrapContentHeight()
                    .padding(TextFieldDefaults.supportingTextPadding())
                ) { supporting() }
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

        var occupiedSpaceHorizontally = 0
        var occupiedSpaceVertically = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)
        occupiedSpaceVertically = max(occupiedSpaceVertically, heightOrZero(leadingPlaceable))

        // measure trailing icon
        val trailingPlaceable = measurables.fastFirstOrNull { it.layoutId == TrailingId }
            ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)
        occupiedSpaceVertically = max(occupiedSpaceVertically, heightOrZero(trailingPlaceable))

        // measure prefix
        val prefixPlaceable = measurables.fastFirstOrNull { it.layoutId == PrefixId }
            ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(prefixPlaceable)
        occupiedSpaceVertically = max(occupiedSpaceVertically, heightOrZero(prefixPlaceable))

        // measure suffix
        val suffixPlaceable = measurables.fastFirstOrNull { it.layoutId == SuffixId }
            ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += widthOrZero(suffixPlaceable)
        occupiedSpaceVertically = max(occupiedSpaceVertically, heightOrZero(suffixPlaceable))

        // measure label
        val labelConstraints = looseConstraints
            .offset(
                vertical = -bottomPaddingValue,
                horizontal = -occupiedSpaceHorizontally
            )
        val labelPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LabelId }?.measure(labelConstraints)

        // supporting text must be measured after other elements, but we
        // reserve space for it using its intrinsic height as a heuristic
        val supportingMeasurable = measurables.fastFirstOrNull { it.layoutId == SupportingId }
        val supportingIntrinsicHeight =
            supportingMeasurable?.minIntrinsicHeight(constraints.minWidth) ?: 0

        // measure input field
        val effectiveTopOffset = topPaddingValue + heightOrZero(labelPlaceable)
        val textFieldConstraints = constraints
            .copy(minHeight = 0)
            .offset(
                vertical = -effectiveTopOffset - bottomPaddingValue - supportingIntrinsicHeight,
                horizontal = -occupiedSpaceHorizontally
            )
        val textFieldPlaceable = measurables
            .fastFirst { it.layoutId == TextFieldId }
            .measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable = measurables
            .fastFirstOrNull { it.layoutId == PlaceholderId }
            ?.measure(placeholderConstraints)

        occupiedSpaceVertically = max(
            occupiedSpaceVertically,
            max(heightOrZero(textFieldPlaceable), heightOrZero(placeholderPlaceable)) +
                effectiveTopOffset + bottomPaddingValue
        )
        val width = calculateWidth(
            leadingWidth = widthOrZero(leadingPlaceable),
            trailingWidth = widthOrZero(trailingPlaceable),
            prefixWidth = widthOrZero(prefixPlaceable),
            suffixWidth = widthOrZero(suffixPlaceable),
            textFieldWidth = textFieldPlaceable.width,
            labelWidth = widthOrZero(labelPlaceable),
            placeholderWidth = widthOrZero(placeholderPlaceable),
            constraints = constraints,
        )

        // measure supporting text
        val supportingConstraints = looseConstraints.offset(
            vertical = -occupiedSpaceVertically
        ).copy(minHeight = 0, maxWidth = width)
        val supportingPlaceable = supportingMeasurable?.measure(supportingConstraints)
        val supportingHeight = heightOrZero(supportingPlaceable)

        val totalHeight = calculateHeight(
            textFieldHeight = textFieldPlaceable.height,
            labelHeight = heightOrZero(labelPlaceable),
            leadingHeight = heightOrZero(leadingPlaceable),
            trailingHeight = heightOrZero(trailingPlaceable),
            prefixHeight = heightOrZero(prefixPlaceable),
            suffixHeight = heightOrZero(suffixPlaceable),
            placeholderHeight = heightOrZero(placeholderPlaceable),
            supportingHeight = heightOrZero(supportingPlaceable),
            animationProgress = animationProgress,
            constraints = constraints,
            density = density,
            paddingValues = paddingValues,
        )
        val height = totalHeight - supportingHeight

        val containerPlaceable = measurables.fastFirst { it.layoutId == ContainerId }.measure(
            Constraints(
                minWidth = if (width != Constraints.Infinity) width else 0,
                maxWidth = width,
                minHeight = if (height != Constraints.Infinity) height else 0,
                maxHeight = height
            )
        )

        return layout(width, totalHeight) {
            if (labelPlaceable != null) {
                placeWithLabel(
                    width = width,
                    totalHeight = totalHeight,
                    textfieldPlaceable = textFieldPlaceable,
                    labelPlaceable = labelPlaceable,
                    placeholderPlaceable = placeholderPlaceable,
                    leadingPlaceable = leadingPlaceable,
                    trailingPlaceable = trailingPlaceable,
                    prefixPlaceable = prefixPlaceable,
                    suffixPlaceable = suffixPlaceable,
                    containerPlaceable = containerPlaceable,
                    supportingPlaceable = supportingPlaceable,
                    singleLine = singleLine,
                    labelEndPosition = topPaddingValue,
                    textPosition = topPaddingValue + labelPlaceable.height,
                    animationProgress = animationProgress,
                    density = density,
                )
            } else {
                placeWithoutLabel(
                    width = width,
                    totalHeight = totalHeight,
                    textPlaceable = textFieldPlaceable,
                    placeholderPlaceable = placeholderPlaceable,
                    leadingPlaceable = leadingPlaceable,
                    trailingPlaceable = trailingPlaceable,
                    prefixPlaceable = prefixPlaceable,
                    suffixPlaceable = suffixPlaceable,
                    containerPlaceable = containerPlaceable,
                    supportingPlaceable = supportingPlaceable,
                    singleLine = singleLine,
                    density = density,
                    paddingValues = paddingValues
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
        val labelWidth = measurables.fastFirstOrNull { it.layoutId == LabelId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val trailingWidth = measurables.fastFirstOrNull { it.layoutId == TrailingId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val prefixWidth = measurables.fastFirstOrNull { it.layoutId == PrefixId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val suffixWidth = measurables.fastFirstOrNull { it.layoutId == SuffixId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val leadingWidth = measurables.fastFirstOrNull { it.layoutId == LeadingId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        val placeholderWidth = measurables.fastFirstOrNull { it.layoutId == PlaceholderId }?.let {
            intrinsicMeasurer(it, height)
        } ?: 0
        return calculateWidth(
            leadingWidth = leadingWidth,
            trailingWidth = trailingWidth,
            prefixWidth = prefixWidth,
            suffixWidth = suffixWidth,
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
        val leadingHeight = measurables.fastFirstOrNull { it.layoutId == LeadingId }?.let {
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            intrinsicMeasurer(it, width)
        } ?: 0
        val trailingHeight = measurables.fastFirstOrNull { it.layoutId == TrailingId }?.let {
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            intrinsicMeasurer(it, width)
        } ?: 0
        val labelHeight = measurables.fastFirstOrNull { it.layoutId == LabelId }?.let {
            intrinsicMeasurer(it, remainingWidth)
        } ?: 0

        val prefixHeight = measurables.fastFirstOrNull { it.layoutId == PrefixId }?.let {
            val height = intrinsicMeasurer(it, remainingWidth)
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            height
        } ?: 0
        val suffixHeight = measurables.fastFirstOrNull { it.layoutId == SuffixId }?.let {
            val height = intrinsicMeasurer(it, remainingWidth)
            remainingWidth = remainingWidth.substractConstraintSafely(
                it.maxIntrinsicWidth(Constraints.Infinity)
            )
            height
        } ?: 0

        val textFieldHeight =
            intrinsicMeasurer(measurables.fastFirst { it.layoutId == TextFieldId }, remainingWidth)
        val placeholderHeight = measurables.fastFirstOrNull { it.layoutId == PlaceholderId }?.let {
            intrinsicMeasurer(it, remainingWidth)
        } ?: 0

        val supportingHeight = measurables.fastFirstOrNull { it.layoutId == SupportingId }?.let {
            intrinsicMeasurer(it, width)
        } ?: 0

        return calculateHeight(
            textFieldHeight = textFieldHeight,
            labelHeight = labelHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            placeholderHeight = placeholderHeight,
            supportingHeight = supportingHeight,
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

private fun calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    prefixWidth: Int,
    suffixWidth: Int,
    textFieldWidth: Int,
    labelWidth: Int,
    placeholderWidth: Int,
    constraints: Constraints
): Int {
    val affixTotalWidth = prefixWidth + suffixWidth
    val middleSection = maxOf(
        textFieldWidth + affixTotalWidth,
        placeholderWidth + affixTotalWidth,
        // Prefix/suffix does not get applied to label
        labelWidth,
    )
    val wrappedWidth = leadingWidth + middleSection + trailingWidth
    return max(wrappedWidth, constraints.minWidth)
}

private fun calculateHeight(
    textFieldHeight: Int,
    labelHeight: Int,
    leadingHeight: Int,
    trailingHeight: Int,
    prefixHeight: Int,
    suffixHeight: Int,
    placeholderHeight: Int,
    supportingHeight: Int,
    animationProgress: Float,
    constraints: Constraints,
    density: Float,
    paddingValues: PaddingValues
): Int {
    val hasLabel = labelHeight > 0

    val verticalPadding = density *
        (paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding()).value
    // Even though the padding is defined by the developer, if there's a label, it only affects the
    // text field in the focused state. Otherwise, we use the default value.
    val actualVerticalPadding = if (hasLabel) {
        lerp((TextFieldPadding * 2).value * density, verticalPadding, animationProgress)
    } else {
        verticalPadding
    }

    val inputFieldHeight = maxOf(
        textFieldHeight,
        placeholderHeight,
        prefixHeight,
        suffixHeight,
        lerp(labelHeight, 0, animationProgress)
    )

    val middleSectionHeight =
        actualVerticalPadding + lerp(0, labelHeight, animationProgress) + inputFieldHeight

    return max(
        constraints.minHeight,
        maxOf(
            leadingHeight,
            trailingHeight,
            middleSectionHeight.roundToInt()
        ) + supportingHeight
    )
}

/**
 * Places the provided text field, placeholder, and label in the TextField given the PaddingValues
 * when there is a label. When there is no label, [placeWithoutLabel] is used instead.
 */
private fun Placeable.PlacementScope.placeWithLabel(
    width: Int,
    totalHeight: Int,
    textfieldPlaceable: Placeable,
    labelPlaceable: Placeable?,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    prefixPlaceable: Placeable?,
    suffixPlaceable: Placeable?,
    containerPlaceable: Placeable,
    supportingPlaceable: Placeable?,
    singleLine: Boolean,
    labelEndPosition: Int,
    textPosition: Int,
    animationProgress: Float,
    density: Float
) {
    // place container
    containerPlaceable.place(IntOffset.Zero)

    // Most elements should be positioned w.r.t the text field's "visual" height, i.e., excluding
    // the supporting text on bottom
    val height = totalHeight - heightOrZero(supportingPlaceable)

    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )
    labelPlaceable?.let {
        // if it's a single line, the label's start position is in the center of the
        // container. When it's a multiline text field, the label's start position is at the
        // top with padding
        val startPosition = if (singleLine) {
            Alignment.CenterVertically.align(it.height, height)
        } else {
            // Even though the padding is defined by the developer, it only affects the text field
            // when the text field is focused. Otherwise, we use the default value.
            (TextFieldPadding.value * density).roundToInt()
        }
        val distance = startPosition - labelEndPosition
        val positionY = startPosition - (distance * animationProgress).roundToInt()
        it.placeRelative(widthOrZero(leadingPlaceable), positionY)
    }

    prefixPlaceable?.placeRelative(widthOrZero(leadingPlaceable), textPosition)

    val textHorizontalPosition = widthOrZero(leadingPlaceable) + widthOrZero(prefixPlaceable)
    textfieldPlaceable.placeRelative(textHorizontalPosition, textPosition)
    placeholderPlaceable?.placeRelative(textHorizontalPosition, textPosition)

    suffixPlaceable?.placeRelative(
        width - widthOrZero(trailingPlaceable) - suffixPlaceable.width,
        textPosition,
    )

    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    supportingPlaceable?.placeRelative(0, height)
}

/**
 * Places the provided text field and placeholder in [TextField] when there is no label. When
 * there is a label, [placeWithLabel] is used
 */
private fun Placeable.PlacementScope.placeWithoutLabel(
    width: Int,
    totalHeight: Int,
    textPlaceable: Placeable,
    placeholderPlaceable: Placeable?,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    prefixPlaceable: Placeable?,
    suffixPlaceable: Placeable?,
    containerPlaceable: Placeable,
    supportingPlaceable: Placeable?,
    singleLine: Boolean,
    density: Float,
    paddingValues: PaddingValues
) {
    // place container
    containerPlaceable.place(IntOffset.Zero)

    // Most elements should be positioned w.r.t the text field's "visual" height, i.e., excluding
    // the supporting text on bottom
    val height = totalHeight - heightOrZero(supportingPlaceable)
    val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

    leadingPlaceable?.placeRelative(
        0,
        Alignment.CenterVertically.align(leadingPlaceable.height, height)
    )

    // Single line text field without label places its text components centered vertically.
    // Multiline text field without label places its text components at the top with padding.
    fun calculateVerticalPosition(placeable: Placeable): Int {
        return if (singleLine) {
            Alignment.CenterVertically.align(placeable.height, height)
        } else {
            topPadding
        }
    }

    prefixPlaceable?.placeRelative(
        widthOrZero(leadingPlaceable),
        calculateVerticalPosition(prefixPlaceable)
    )

    val textHorizontalPosition = widthOrZero(leadingPlaceable) + widthOrZero(prefixPlaceable)

    textPlaceable.placeRelative(textHorizontalPosition, calculateVerticalPosition(textPlaceable))

    placeholderPlaceable?.placeRelative(
        textHorizontalPosition,
        calculateVerticalPosition(placeholderPlaceable)
    )

    suffixPlaceable?.placeRelative(
        width - widthOrZero(trailingPlaceable) - suffixPlaceable.width,
        calculateVerticalPosition(suffixPlaceable),
    )

    trailingPlaceable?.placeRelative(
        width - trailingPlaceable.width,
        Alignment.CenterVertically.align(trailingPlaceable.height, height)
    )

    supportingPlaceable?.placeRelative(0, height)
}

/**
 * A draw modifier that draws a bottom indicator line in [TextField]
 */
internal fun Modifier.drawIndicatorLine(indicatorBorder: State<BorderStroke>): Modifier {
    return drawWithContent {
        drawContent()
        val strokeWidth = indicatorBorder.value.width.toPx()
        val y = size.height - strokeWidth / 2
        drawLine(
            indicatorBorder.value.brush,
            Offset(0f, y),
            Offset(size.width, y),
            strokeWidth
        )
    }
}

/** Padding from text field top to label top, and from input field bottom to text field bottom */
/*@VisibleForTesting*/
internal val TextFieldWithLabelVerticalPadding = 8.dp
