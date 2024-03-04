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

package androidx.compose.material3.internal

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldLayout
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLayout
import androidx.compose.material3.outlineCutout
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.value
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class TextFieldType {
    Filled,
    Outlined
}

@Composable
internal fun CommonDecorationBox(
    type: TextFieldType,
    visualText: CharSequence,
    innerTextField: @Composable () -> Unit,
    labelPosition: TextFieldLabelPosition,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    singleLine: Boolean,
    enabled: Boolean,
    isError: Boolean,
    interactionSource: InteractionSource,
    contentPadding: PaddingValues,
    colors: TextFieldColors,
    container: @Composable () -> Unit,
) {
    val isFocused = interactionSource.collectIsFocusedAsState().value
    val inputState =
        when {
            isFocused -> InputPhase.Focused
            visualText.isEmpty() -> InputPhase.UnfocusedEmpty
            else -> InputPhase.UnfocusedNotEmpty
        }

    val labelColor = colors.labelColor(enabled, isError, isFocused)

    val typography = MaterialTheme.typography
    val bodyLarge = typography.bodyLarge
    val bodySmall = typography.bodySmall
    val overrideLabelTextStyleColor =
        (bodyLarge.color == Color.Unspecified && bodySmall.color != Color.Unspecified) ||
            (bodyLarge.color != Color.Unspecified && bodySmall.color == Color.Unspecified)

    TextFieldTransitionScope(
        inputState = inputState,
        focusedLabelTextStyleColor =
            with(bodySmall.color) {
                if (overrideLabelTextStyleColor) this.takeOrElse { labelColor } else this
            },
        unfocusedLabelTextStyleColor =
            with(bodyLarge.color) {
                if (overrideLabelTextStyleColor) this.takeOrElse { labelColor } else this
            },
        labelColor = labelColor,
        showExpandedLabel = label != null && !labelPosition.alwaysMinimize,
    ) { labelProgress, labelTextStyleColor, labelContentColor, placeholderAlpha, prefixSuffixAlpha
        ->
        val labelProgressValue = labelProgress.value
        val decoratedLabel: @Composable (() -> Unit)? =
            label?.let {
                @Composable {
                    val labelTextStyle =
                        lerp(bodyLarge, bodySmall, labelProgressValue).let { textStyle ->
                            if (overrideLabelTextStyleColor) {
                                textStyle.copy(color = labelTextStyleColor.value)
                            } else {
                                textStyle
                            }
                        }
                    Decoration(labelContentColor.value, labelTextStyle, it)
                }
            }

        // Transparent components interfere with Talkback (b/261061240), so if any components below
        // have alpha == 0, we set the component to null instead.

        val placeholderColor = colors.placeholderColor(enabled, isError, isFocused)
        val showPlaceholder by remember {
            derivedStateOf(structuralEqualityPolicy()) { placeholderAlpha.value > 0f }
        }
        val decoratedPlaceholder: @Composable ((Modifier) -> Unit)? =
            if (placeholder != null && visualText.isEmpty() && showPlaceholder) {
                @Composable { modifier ->
                    Box(modifier.graphicsLayer { alpha = placeholderAlpha.value }) {
                        Decoration(
                            contentColor = placeholderColor,
                            textStyle = bodyLarge,
                            content = placeholder
                        )
                    }
                }
            } else null

        val prefixColor = colors.prefixColor(enabled, isError, isFocused)
        val showPrefixSuffix by remember {
            derivedStateOf(structuralEqualityPolicy()) { prefixSuffixAlpha.value > 0f }
        }
        val decoratedPrefix: @Composable (() -> Unit)? =
            if (prefix != null && showPrefixSuffix) {
                @Composable {
                    Box(Modifier.graphicsLayer { alpha = prefixSuffixAlpha.value }) {
                        Decoration(
                            contentColor = prefixColor,
                            textStyle = bodyLarge,
                            content = prefix
                        )
                    }
                }
            } else null

        val suffixColor = colors.suffixColor(enabled, isError, isFocused)
        val decoratedSuffix: @Composable (() -> Unit)? =
            if (suffix != null && showPrefixSuffix) {
                @Composable {
                    Box(Modifier.graphicsLayer { alpha = prefixSuffixAlpha.value }) {
                        Decoration(
                            contentColor = suffixColor,
                            textStyle = bodyLarge,
                            content = suffix
                        )
                    }
                }
            } else null

        val leadingIconColor = colors.leadingIconColor(enabled, isError, isFocused)
        val decoratedLeading: @Composable (() -> Unit)? =
            leadingIcon?.let {
                @Composable { Decoration(contentColor = leadingIconColor, content = it) }
            }

        val trailingIconColor = colors.trailingIconColor(enabled, isError, isFocused)
        val decoratedTrailing: @Composable (() -> Unit)? =
            trailingIcon?.let {
                @Composable { Decoration(contentColor = trailingIconColor, content = it) }
            }

        val supportingTextColor = colors.supportingTextColor(enabled, isError, isFocused)
        val decoratedSupporting: @Composable (() -> Unit)? =
            supportingText?.let {
                @Composable {
                    Decoration(
                        contentColor = supportingTextColor,
                        textStyle = bodySmall,
                        content = it
                    )
                }
            }

        when (type) {
            TextFieldType.Filled -> {
                val containerWithId: @Composable () -> Unit = {
                    Box(Modifier.layoutId(ContainerId), propagateMinConstraints = true) {
                        container()
                    }
                }

                TextFieldLayout(
                    modifier = Modifier,
                    textField = innerTextField,
                    placeholder = decoratedPlaceholder,
                    label = decoratedLabel,
                    leading = decoratedLeading,
                    trailing = decoratedTrailing,
                    prefix = decoratedPrefix,
                    suffix = decoratedSuffix,
                    container = containerWithId,
                    supporting = decoratedSupporting,
                    singleLine = singleLine,
                    labelPosition = labelPosition,
                    // TODO(b/271000818): progress state read should be deferred to layout phase
                    labelProgress = labelProgressValue,
                    paddingValues = contentPadding
                )
            }
            TextFieldType.Outlined -> {
                // Outlined cutout
                val cutoutSize = remember { mutableStateOf(Size.Zero) }
                val borderContainerWithId: @Composable () -> Unit = {
                    Box(
                        Modifier.layoutId(ContainerId)
                            .outlineCutout(cutoutSize::value, contentPadding),
                        propagateMinConstraints = true
                    ) {
                        container()
                    }
                }

                OutlinedTextFieldLayout(
                    modifier = Modifier,
                    textField = innerTextField,
                    placeholder = decoratedPlaceholder,
                    label = decoratedLabel,
                    leading = decoratedLeading,
                    trailing = decoratedTrailing,
                    prefix = decoratedPrefix,
                    suffix = decoratedSuffix,
                    supporting = decoratedSupporting,
                    singleLine = singleLine,
                    onLabelMeasured = {
                        if (labelPosition !is TextFieldLabelPosition.Default) {
                            return@OutlinedTextFieldLayout
                        }
                        val labelWidth = it.width * labelProgressValue
                        val labelHeight = it.height * labelProgressValue
                        if (
                            cutoutSize.value.width != labelWidth ||
                                cutoutSize.value.height != labelHeight
                        ) {
                            cutoutSize.value = Size(labelWidth, labelHeight)
                        }
                    },
                    labelPosition = labelPosition,
                    // TODO(b/271000818): progress state read should be deferred to layout phase
                    labelProgress = labelProgressValue,
                    container = borderContainerWithId,
                    paddingValues = contentPadding
                )
            }
        }
    }
}

/** Decorates [content] with [contentColor] and [textStyle]. */
@Composable
private fun Decoration(contentColor: Color, textStyle: TextStyle, content: @Composable () -> Unit) =
    ProvideContentColorTextStyle(contentColor, textStyle, content)

/** Decorates [content] with [contentColor]. */
@Composable
private fun Decoration(contentColor: Color, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)

// Developers need to handle invalid input manually. But since we don't provide an error message
// slot API, we can set the default error message in case developers forget about it.
internal fun Modifier.defaultErrorSemantics(
    isError: Boolean,
    defaultErrorMessage: String,
): Modifier = if (isError) semantics { error(defaultErrorMessage) } else this

/**
 * Replacement for Modifier.background which takes color as a State to avoid recomposition while
 * animating.
 */
internal fun Modifier.textFieldBackground(
    color: ColorProducer,
    shape: Shape,
): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, color = color()) }
    }

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private inline fun TextFieldTransitionScope(
    inputState: InputPhase,
    focusedLabelTextStyleColor: Color,
    unfocusedLabelTextStyleColor: Color,
    labelColor: Color,
    showExpandedLabel: Boolean,
    content:
        @Composable
        (
            labelProgress: State<Float>,
            labelTextStyleColor: State<Color>,
            labelContentColor: State<Color>,
            placeholderOpacity: State<Float>,
            prefixSuffixOpacity: State<Float>,
        ) -> Unit
) {
    // Transitions from/to InputPhase.Focused are the most critical in the transition below.
    // UnfocusedEmpty <-> UnfocusedNotEmpty are needed when a single state is used to control
    // multiple text fields.
    val transition = updateTransition(inputState, label = "TextFieldInputState")

    // TODO Load the motionScheme tokens from the component tokens file
    val labelProgress =
        transition.animateFloat(
            label = "LabelProgress",
            transitionSpec = { MotionSchemeKeyTokens.FastSpatial.value() }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 1f
            }
        }

    val placeholderOpacity =
        transition.animateFloat(
            label = "PlaceholderOpacity",
            transitionSpec = {
                if (InputPhase.Focused isTransitioningTo InputPhase.UnfocusedEmpty) {
                    MotionSchemeKeyTokens.FastEffects.value()
                } else if (
                    InputPhase.UnfocusedEmpty isTransitioningTo InputPhase.Focused ||
                        InputPhase.UnfocusedNotEmpty isTransitioningTo InputPhase.UnfocusedEmpty
                ) {
                    MotionSchemeKeyTokens.SlowEffects.value()
                } else {
                    MotionSchemeKeyTokens.FastEffects.value()
                }
            }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 0f
            }
        }

    val prefixSuffixOpacity =
        transition.animateFloat(
            label = "PrefixSuffixOpacity",
            transitionSpec = { MotionSchemeKeyTokens.FastEffects.value() }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 1f
            }
        }

    val labelTextStyleColor =
        transition.animateColor(
            transitionSpec = { MotionSchemeKeyTokens.FastEffects.value() },
            label = "LabelTextStyleColor"
        ) {
            when (it) {
                InputPhase.Focused -> focusedLabelTextStyleColor
                else -> unfocusedLabelTextStyleColor
            }
        }

    @Suppress("UnusedTransitionTargetStateParameter")
    val labelContentColor =
        transition.animateColor(
            transitionSpec = { MotionSchemeKeyTokens.FastEffects.value() },
            label = "LabelContentColor",
            targetValueByState = { labelColor }
        )

    content(
        labelProgress,
        labelTextStyleColor,
        labelContentColor,
        placeholderOpacity,
        prefixSuffixOpacity,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun animateBorderStrokeAsState(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
    colors: TextFieldColors,
    focusedBorderThickness: Dp,
    unfocusedBorderThickness: Dp
): State<BorderStroke> {
    // TODO Load the motionScheme tokens from the component tokens file
    val targetColor = colors.indicatorColor(enabled, isError, focused)
    val indicatorColor =
        if (enabled) {
            animateColorAsState(targetColor, MotionSchemeKeyTokens.FastEffects.value())
        } else {
            rememberUpdatedState(targetColor)
        }

    val thickness =
        if (enabled) {
            val targetThickness = if (focused) focusedBorderThickness else unfocusedBorderThickness
            animateDpAsState(targetThickness, MotionSchemeKeyTokens.FastSpatial.value())
        } else {
            rememberUpdatedState(unfocusedBorderThickness)
        }

    return rememberUpdatedState(BorderStroke(thickness.value, indicatorColor.value))
}

/** An internal state used to animate a label and an indicator. */
private enum class InputPhase {
    // Text field is focused
    Focused,

    // Text field is not focused and input text is empty
    UnfocusedEmpty,

    // Text field is not focused but input text is not empty
    UnfocusedNotEmpty
}

internal const val TextFieldId = "TextField"
internal const val PlaceholderId = "Hint"
internal const val LabelId = "Label"
internal const val LeadingId = "Leading"
internal const val TrailingId = "Trailing"
internal const val PrefixId = "Prefix"
internal const val SuffixId = "Suffix"
internal const val SupportingId = "Supporting"
internal const val ContainerId = "Container"

internal val TextFieldPadding = 16.dp
// SP not DP because it should scale with font size. Value equal to bodySmall line height / 2.
internal val TextFieldLabelExtraPadding = 8.sp
internal val HorizontalIconPadding = 12.dp
internal val AboveLabelHorizontalPadding = 4.dp
internal val AboveLabelBottomPadding = 4.dp
internal val SupportingTopPadding = 4.dp
internal val PrefixSuffixTextPadding = 2.dp
internal val MinTextLineHeight = 24.dp
internal val MinFocusedLabelLineHeight = 16.dp
internal val MinSupportingTextLineHeight = 16.dp

internal val IconDefaultSizeModifier = Modifier.defaultMinSize(48.dp, 48.dp)
