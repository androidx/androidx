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
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldLayout
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.material3.TextFieldLayout
import androidx.compose.material3.outlineCutout
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.SmallIconButtonTokens
import androidx.compose.material3.tokens.TypeScaleTokens
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
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified

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
    label: @Composable (TextFieldLabelScope.() -> Unit)?,
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
        showExpandedLabel = label != null && labelPosition.showExpandedLabel,
    ) { labelProgress, labelTextStyleColor, labelContentColor, placeholderAlpha, prefixSuffixAlpha
        ->
        val labelScope = remember {
            object : TextFieldLabelScope {
                override val labelMinimizedProgress: Float
                    get() = labelProgress.value
            }
        }
        val decoratedLabel: @Composable (() -> Unit)? =
            label?.let { label ->
                @Composable {
                    val labelTextStyle =
                        lerp(bodyLarge, bodySmall, labelProgress.value).let { textStyle ->
                            if (overrideLabelTextStyleColor) {
                                textStyle.copy(color = labelTextStyleColor.value)
                            } else {
                                textStyle
                            }
                        }
                    Decoration(labelContentColor.value, labelTextStyle) { labelScope.label() }
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
                    labelProgress = labelProgress.value,
                    paddingValues = contentPadding
                )
            }
            TextFieldType.Outlined -> {
                // Outlined cutout
                val cutoutSize = remember { mutableStateOf(Size.Zero) }
                val borderContainerWithId: @Composable () -> Unit = {
                    Box(
                        Modifier.layoutId(ContainerId)
                            .outlineCutout(
                                labelSize = cutoutSize::value,
                                alignment = labelPosition.minimizedAlignment,
                                paddingValues = contentPadding
                            ),
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
                        if (labelPosition is TextFieldLabelPosition.Above) {
                            return@OutlinedTextFieldLayout
                        }
                        val progress = labelProgress.value
                        val labelWidth = it.width * progress
                        val labelHeight = it.height * progress
                        if (
                            cutoutSize.value.width != labelWidth ||
                                cutoutSize.value.height != labelHeight
                        ) {
                            cutoutSize.value = Size(labelWidth, labelHeight)
                        }
                    },
                    labelPosition = labelPosition,
                    // TODO(b/271000818): progress state read should be deferred to layout phase
                    labelProgress = labelProgress.value,
                    container = borderContainerWithId,
                    paddingValues = contentPadding
                )
            }
        }
    }
}

private val TextFieldLabelPosition.showExpandedLabel: Boolean
    get() = this is TextFieldLabelPosition.Attached && !alwaysMinimize

internal val TextFieldLabelPosition.minimizedAlignment: Alignment.Horizontal
    get() =
        when (this) {
            is TextFieldLabelPosition.Above -> alignment
            is TextFieldLabelPosition.Attached -> minimizedAlignment
            else -> throw IllegalArgumentException("Unknown position: $this")
        }

internal val TextFieldLabelPosition.expandedAlignment: Alignment.Horizontal
    get() =
        when (this) {
            is TextFieldLabelPosition.Above -> alignment
            is TextFieldLabelPosition.Attached -> expandedAlignment
            else -> throw IllegalArgumentException("Unknown position: $this")
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
 * Replacement for Modifier.background which takes color lazily to avoid recomposition while
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
    val labelTransitionSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val labelProgress =
        transition.animateFloat(label = "LabelProgress", transitionSpec = { labelTransitionSpec }) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 1f
            }
        }

    val fastOpacityTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
    val slowOpacityTransitionSpec = MotionSchemeKeyTokens.SlowEffects.value<Float>()
    val placeholderOpacity =
        transition.animateFloat(
            label = "PlaceholderOpacity",
            transitionSpec = {
                if (InputPhase.Focused isTransitioningTo InputPhase.UnfocusedEmpty) {
                    fastOpacityTransitionSpec
                } else if (
                    InputPhase.UnfocusedEmpty isTransitioningTo InputPhase.Focused ||
                        InputPhase.UnfocusedNotEmpty isTransitioningTo InputPhase.UnfocusedEmpty
                ) {
                    slowOpacityTransitionSpec
                } else {
                    fastOpacityTransitionSpec
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
            transitionSpec = { fastOpacityTransitionSpec }
        ) {
            when (it) {
                InputPhase.Focused -> 1f
                InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
                InputPhase.UnfocusedNotEmpty -> 1f
            }
        }

    val colorTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Color>()
    val labelTextStyleColor =
        transition.animateColor(
            transitionSpec = { colorTransitionSpec },
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
            transitionSpec = { colorTransitionSpec },
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
    val colorAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Color>()
    val indicatorColor =
        if (enabled) {
            animateColorAsState(targetColor, colorAnimationSpec)
        } else {
            rememberUpdatedState(targetColor)
        }

    val thicknessAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Dp>()
    val thickness =
        if (enabled) {
            val targetThickness = if (focused) focusedBorderThickness else unfocusedBorderThickness
            animateDpAsState(targetThickness, thicknessAnimationSpec)
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

// Icons are 24dp but padded to LocalMinimumInteractiveComponentSize (48dp by default), so we need
// to account for this visual discrepancy when applying user padding.
@Composable
internal fun textFieldHorizontalIconPadding(): Dp {
    val interactiveSizeOrNaN = LocalMinimumInteractiveComponentSize.current
    val interactiveSize = if (interactiveSizeOrNaN.isUnspecified) 0.dp else interactiveSizeOrNaN
    return ((interactiveSize - SmallIconButtonTokens.IconSize) / 2).coerceAtLeast(0.dp)
}

@Composable
internal fun minimizedLabelHalfHeight(): Dp {
    val compositionLocalValue = MaterialTheme.typography.bodySmall.lineHeight
    val fallbackValue = TypeScaleTokens.BodySmallLineHeight
    val value = if (compositionLocalValue.isSp) compositionLocalValue else fallbackValue
    return with(LocalDensity.current) { value.toDp() / 2 }
}

internal val TextFieldPadding = 16.dp
internal val AboveLabelHorizontalPadding = 4.dp
internal val AboveLabelBottomPadding = 4.dp
internal val SupportingTopPadding = 4.dp
internal val PrefixSuffixTextPadding = 2.dp
internal val MinTextLineHeight = 24.dp
internal val MinFocusedLabelLineHeight = 16.dp
internal val MinSupportingTextLineHeight = 16.dp
