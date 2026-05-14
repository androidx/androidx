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
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.outlineCutout
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.MotionTokens.EasingEmphasizedAccelerateCubicBezier
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
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp as lerpInt
import kotlin.math.max
import kotlin.math.roundToInt

internal enum class TextFieldType {
    Filled,
    Outlined,
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

    val typography = MaterialTheme.typography
    val bodyLarge = typography.bodyLarge
    val bodySmall = typography.bodySmall
    val overrideLabelTextStyleColor =
        (bodyLarge.color == Color.Unspecified && bodySmall.color != Color.Unspecified) ||
            (bodyLarge.color != Color.Unspecified && bodySmall.color == Color.Unspecified)

    val transition = updateTransition(inputState, label = "TextFieldInputState")

    val showExpandedLabel = label != null && labelPosition.showExpandedLabel

    val labelProgress =
        if (label != null) {
            transition.labelProgress(showExpandedLabel)
        } else {
            null
        }

    val placeholderAlpha =
        if (placeholder != null) {
            transition.placeholderOpacity(showExpandedLabel)
        } else {
            null
        }

    val affixAlpha =
        if (prefix != null || suffix != null) {
            transition.affixOpacity(showExpandedLabel)
        } else {
            null
        }

    val decoratedLabel =
        label?.let { label ->
            @Composable {
                DecoratedLabel(
                    labelProgress = labelProgress,
                    colors = colors,
                    enabled = enabled,
                    isError = isError,
                    isFocused = isFocused,
                    overrideLabelTextStyleColor = overrideLabelTextStyleColor,
                    transition = transition,
                    bodySmall = bodySmall,
                    bodyLarge = bodyLarge,
                    content = label,
                )
            }
        }

    // Transparent components interfere with Talkback (b/261061240), so if any components below
    // have alpha == 0, we set the component to null instead.
    val placeholderColor = colors.placeholderColor(enabled, isError, isFocused)
    val showPlaceholder by remember {
        derivedStateOf(structuralEqualityPolicy()) { (placeholderAlpha?.value ?: 0f) > 0f }
    }
    val decoratedPlaceholder: @Composable ((Modifier) -> Unit)? =
        if (placeholder != null && visualText.isEmpty() && showPlaceholder) {
            @Composable { modifier ->
                Box(modifier) {
                    Decoration(
                        contentColor = placeholderColor,
                        textStyle = bodyLarge,
                        content = placeholder,
                    )
                }
            }
        } else null

    val prefixColor = colors.prefixColor(enabled, isError, isFocused)
    val showAffix by remember {
        derivedStateOf(structuralEqualityPolicy()) { (affixAlpha?.value ?: 0f) > 0f }
    }
    val decoratedPrefix: @Composable (() -> Unit)? =
        if (prefix != null && showAffix) {
            @Composable {
                Decoration(contentColor = prefixColor, textStyle = bodyLarge, content = prefix)
            }
        } else null

    val suffixColor = colors.suffixColor(enabled, isError, isFocused)
    val decoratedSuffix: @Composable (() -> Unit)? =
        if (suffix != null && showAffix) {
            @Composable {
                Decoration(contentColor = suffixColor, textStyle = bodyLarge, content = suffix)
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
                Decoration(contentColor = supportingTextColor, textStyle = bodySmall, content = it)
            }
        }

    val labelProgressProducer = { labelProgress?.value ?: 1f }
    val placeholderAlphaProducer = { placeholderAlpha?.value ?: 0f }
    val affixAlphaProducer = { affixAlpha?.value ?: 0f }
    when (type) {
        TextFieldType.Filled -> {
            val containerWithId: @Composable () -> Unit = {
                Box(Modifier.layoutId(ContainerId), propagateMinConstraints = true) { container() }
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
                labelProgress = labelProgressProducer,
                placeholderAlpha = placeholderAlphaProducer,
                affixAlpha = affixAlphaProducer,
                paddingValues = contentPadding,
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
                            paddingValues = contentPadding,
                        ),
                    propagateMinConstraints = true,
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
                    val progress = labelProgressProducer()
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
                labelProgress = labelProgressProducer,
                placeholderAlpha = placeholderAlphaProducer,
                affixAlpha = affixAlphaProducer,
                container = borderContainerWithId,
                paddingValues = contentPadding,
            )
        }
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
    labelPosition: TextFieldLabelPosition,
    labelProgress: FloatProducer,
    placeholderAlpha: FloatProducer,
    affixAlpha: FloatProducer,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues,
) {
    val minimizedLabelHalfHeight = minimizedLabelHalfHeight()
    val measurePolicy =
        remember(
            singleLine,
            labelPosition,
            labelProgress,
            placeholderAlpha,
            affixAlpha,
            paddingValues,
            minimizedLabelHalfHeight,
        ) {
            TextFieldMeasurePolicy(
                singleLine = singleLine,
                labelPosition = labelPosition,
                labelProgress = labelProgress,
                placeholderAlpha = placeholderAlpha,
                affixAlpha = affixAlpha,
                paddingValues = paddingValues,
                minimizedLabelHalfHeight = minimizedLabelHalfHeight,
            )
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
                    modifier = Modifier.layoutId(LeadingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val horizontalIconPadding = textFieldHorizontalIconPadding()
            val startPadding =
                if (leading != null) {
                    (startTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    startTextFieldPadding
                }
            val endPadding =
                if (trailing != null) {
                    (endTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
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

            val labelPadding =
                if (labelPosition is TextFieldLabelPosition.Above) {
                    Modifier.padding(
                        start = AboveLabelHorizontalPadding,
                        end = AboveLabelHorizontalPadding,
                        bottom = AboveLabelBottomPadding,
                    )
                } else {
                    Modifier.padding(start = startPadding, end = endPadding)
                }
            if (label != null) {
                Box(
                    Modifier.layoutId(LabelId)
                        .textFieldLabelMinHeight {
                            lerpDp(MinTextLineHeight, MinFocusedLabelLineHeight, labelProgress())
                        }
                        .wrapContentHeight()
                        .then(labelPadding)
                ) {
                    label()
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
                propagateMinConstraints = true,
            ) {
                textField()
            }

            if (supporting != null) {
                @OptIn(ExperimentalMaterial3Api::class)
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
        measurePolicy = measurePolicy,
    )
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
    labelProgress: FloatProducer,
    placeholderAlpha: FloatProducer,
    affixAlpha: FloatProducer,
    onLabelMeasured: (Size) -> Unit,
    container: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)?,
    paddingValues: PaddingValues,
) {
    val horizontalIconPadding = textFieldHorizontalIconPadding()
    val measurePolicy =
        remember(
            onLabelMeasured,
            singleLine,
            labelPosition,
            labelProgress,
            placeholderAlpha,
            affixAlpha,
            paddingValues,
            horizontalIconPadding,
        ) {
            OutlinedTextFieldMeasurePolicy(
                onLabelMeasured = onLabelMeasured,
                singleLine = singleLine,
                labelPosition = labelPosition,
                labelProgress = labelProgress,
                placeholderAlpha = placeholderAlpha,
                affixAlpha = affixAlpha,
                paddingValues = paddingValues,
                horizontalIconPadding = horizontalIconPadding,
            )
        }
    val layoutDirection = LocalLayoutDirection.current
    Layout(
        modifier = modifier,
        content = {
            container()

            if (leading != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    leading()
                }
            }
            if (trailing != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId).minimumInteractiveComponentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    trailing()
                }
            }

            val startTextFieldPadding = paddingValues.calculateStartPadding(layoutDirection)
            val endTextFieldPadding = paddingValues.calculateEndPadding(layoutDirection)

            val startPadding =
                if (leading != null) {
                    (startTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
                } else {
                    startTextFieldPadding
                }
            val endPadding =
                if (trailing != null) {
                    (endTextFieldPadding - horizontalIconPadding).coerceAtLeast(0.dp)
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
                propagateMinConstraints = true,
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
                    Modifier.textFieldLabelMinHeight {
                            lerpDp(MinTextLineHeight, MinFocusedLabelLineHeight, labelProgress())
                        }
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
        measurePolicy = measurePolicy,
    )
}

private class TextFieldMeasurePolicy(
    private val singleLine: Boolean,
    private val labelPosition: TextFieldLabelPosition,
    private val labelProgress: FloatProducer,
    private val placeholderAlpha: FloatProducer,
    private val affixAlpha: FloatProducer,
    private val paddingValues: PaddingValues,
    private val minimizedLabelHalfHeight: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val labelProgress = labelProgress()
        val topPaddingValue = paddingValues.calculateTopPadding().roundToPx()
        val bottomPaddingValue = paddingValues.calculateBottomPadding().roundToPx()

        var occupiedSpaceHorizontally = 0
        var occupiedSpaceVertically = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // measure leading icon
        val leadingPlaceable =
            measurables.fastFirstOrNull { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += leadingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, leadingPlaceable.heightOrZero)

        // measure trailing icon
        val trailingPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += trailingPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, trailingPlaceable.heightOrZero)

        // measure prefix
        val prefixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += prefixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, prefixPlaceable.heightOrZero)

        // measure suffix
        val suffixPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
                ?.measure(looseConstraints.offset(horizontal = -occupiedSpaceHorizontally))
        occupiedSpaceHorizontally += suffixPlaceable.widthOrZero
        occupiedSpaceVertically = max(occupiedSpaceVertically, suffixPlaceable.heightOrZero)

        val isLabelAbove = labelPosition is TextFieldLabelPosition.Above
        val labelMeasurable = measurables.fastFirstOrNull { it.layoutId == LabelId }
        var labelPlaceable: Placeable? = null
        val labelIntrinsicHeight: Int
        if (!isLabelAbove) {
            // if label is not Above, we can measure it like normal
            val labelConstraints =
                looseConstraints.offset(
                    vertical = -bottomPaddingValue,
                    horizontal = -occupiedSpaceHorizontally,
                )
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
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

        // at most one of these is non-zero
        val labelHeightOrIntrinsic = labelPlaceable.heightOrZero + labelIntrinsicHeight

        // measure input field
        val effectiveTopOffset = topPaddingValue + labelHeightOrIntrinsic
        val textFieldConstraints =
            constraints
                .copy(minHeight = 0)
                .offset(
                    vertical = -effectiveTopOffset - bottomPaddingValue - supportingIntrinsicHeight,
                    horizontal = -occupiedSpaceHorizontally,
                )
        val textFieldPlaceable =
            measurables.fastFirst { it.layoutId == TextFieldId }.measure(textFieldConstraints)

        // measure placeholder
        val placeholderConstraints = textFieldConstraints.copy(minWidth = 0)
        val placeholderPlaceable =
            measurables
                .fastFirstOrNull { it.layoutId == PlaceholderId }
                ?.measure(placeholderConstraints)

        occupiedSpaceVertically =
            max(
                occupiedSpaceVertically,
                max(textFieldPlaceable.heightOrZero, placeholderPlaceable.heightOrZero) +
                    effectiveTopOffset +
                    bottomPaddingValue,
            )
        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                prefixWidth = prefixPlaceable.widthOrZero,
                suffixWidth = suffixPlaceable.widthOrZero,
                textFieldWidth = textFieldPlaceable.width,
                labelWidth = labelPlaceable.widthOrZero,
                placeholderWidth = placeholderPlaceable.widthOrZero,
                constraints = constraints,
            )

        if (isLabelAbove) {
            // now that we know the width, measure label
            val labelConstraints =
                looseConstraints.copy(maxHeight = labelIntrinsicHeight, maxWidth = width)
            labelPlaceable = labelMeasurable?.measure(labelConstraints)
        }

        // measure supporting text
        val supportingConstraints =
            looseConstraints
                .offset(vertical = -occupiedSpaceVertically)
                .copy(minHeight = 0, maxWidth = width)
        val supportingPlaceable = supportingMeasurable?.measure(supportingConstraints)
        val supportingHeight = supportingPlaceable.heightOrZero

        val totalHeight =
            calculateHeight(
                textFieldHeight = textFieldPlaceable.height,
                labelHeight = labelPlaceable.heightOrZero,
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                prefixHeight = prefixPlaceable.heightOrZero,
                suffixHeight = suffixPlaceable.heightOrZero,
                placeholderHeight = placeholderPlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                constraints = constraints,
                isLabelAbove = isLabelAbove,
                labelProgress = labelProgress,
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
                        maxHeight = height,
                    )
                )

        return layout(width, totalHeight) {
            if (labelPlaceable != null) {
                val labelStartY =
                    when {
                        isLabelAbove -> 0
                        singleLine ->
                            Alignment.CenterVertically.align(labelPlaceable.height, height)
                        else ->
                            // The padding defined by the user only applies to the text field when
                            // the label is focused. More padding needs to be added when the text
                            // field is unfocused.
                            topPaddingValue + minimizedLabelHalfHeight.roundToPx()
                    }
                val labelEndY =
                    when {
                        isLabelAbove -> 0
                        else -> topPaddingValue
                    }
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
                    labelStartY = labelStartY,
                    labelEndY = labelEndY,
                    isLabelAbove = isLabelAbove,
                    labelProgress = labelProgress,
                    placeholderAlpha = placeholderAlpha,
                    affixAlpha = affixAlpha,
                    textPosition =
                        topPaddingValue + (if (isLabelAbove) 0 else labelPlaceable.height),
                    layoutDirection = layoutDirection,
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
                    placeholderAlpha = placeholderAlpha,
                    affixAlpha = affixAlpha,
                    density = density,
                )
            }
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
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
        val prefixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == PrefixId }
                ?.let { intrinsicMeasurer(it, height) } ?: 0
        val suffixWidth =
            measurables
                .fastFirstOrNull { it.layoutId == SuffixId }
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
            prefixWidth = prefixWidth,
            suffixWidth = suffixWidth,
            textFieldWidth = textFieldWidth,
            labelWidth = labelWidth,
            placeholderWidth = placeholderWidth,
            constraints = Constraints(),
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
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
                ?.let { intrinsicMeasurer(it, remainingWidth) } ?: 0

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
            textFieldHeight = textFieldHeight,
            labelHeight = labelHeight,
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            prefixHeight = prefixHeight,
            suffixHeight = suffixHeight,
            placeholderHeight = placeholderHeight,
            supportingHeight = supportingHeight,
            constraints = Constraints(),
            isLabelAbove = labelPosition is TextFieldLabelPosition.Above,
            labelProgress = labelProgress(),
        )
    }

    private fun calculateWidth(
        leadingWidth: Int,
        trailingWidth: Int,
        prefixWidth: Int,
        suffixWidth: Int,
        textFieldWidth: Int,
        labelWidth: Int,
        placeholderWidth: Int,
        constraints: Constraints,
    ): Int {
        val affixTotalWidth = prefixWidth + suffixWidth
        val middleSection =
            maxOf(
                textFieldWidth + affixTotalWidth,
                placeholderWidth + affixTotalWidth,
                // Prefix/suffix does not get applied to label
                labelWidth,
            )
        val wrappedWidth = leadingWidth + middleSection + trailingWidth
        return constraints.constrainWidth(wrappedWidth)
    }

    private fun Density.calculateHeight(
        textFieldHeight: Int,
        labelHeight: Int,
        leadingHeight: Int,
        trailingHeight: Int,
        prefixHeight: Int,
        suffixHeight: Int,
        placeholderHeight: Int,
        supportingHeight: Int,
        constraints: Constraints,
        isLabelAbove: Boolean,
        labelProgress: Float,
    ): Int {
        val verticalPadding =
            (paddingValues.calculateTopPadding() + paddingValues.calculateBottomPadding())
                .roundToPx()

        val inputFieldHeight =
            maxOf(
                textFieldHeight,
                placeholderHeight,
                prefixHeight,
                suffixHeight,
                if (isLabelAbove) 0 else lerpInt(labelHeight, 0, labelProgress),
            )

        val hasLabel = labelHeight > 0
        val nonOverlappedLabelHeight =
            if (hasLabel && !isLabelAbove) {
                // The label animates from overlapping the input field to floating above it,
                // so its contribution to the height calculation changes over time. A baseline
                // height is provided in the unfocused state to keep the overall height consistent
                // across the animation.
                max(
                    (minimizedLabelHalfHeight * 2).roundToPx(),
                    lerpInt(
                        0,
                        labelHeight,
                        EasingEmphasizedAccelerateCubicBezier.transform(labelProgress),
                    ),
                )
            } else {
                0
            }

        val middleSectionHeight = verticalPadding + nonOverlappedLabelHeight + inputFieldHeight

        return constraints.constrainHeight(
            (if (isLabelAbove) labelHeight else 0) +
                maxOf(leadingHeight, trailingHeight, middleSectionHeight) +
                supportingHeight
        )
    }

    /**
     * Places the provided text field, placeholder, and label in the TextField given the
     * PaddingValues when there is a label. When there is no label, [placeWithoutLabel] is used
     * instead.
     */
    private fun Placeable.PlacementScope.placeWithLabel(
        width: Int,
        totalHeight: Int,
        textfieldPlaceable: Placeable,
        labelPlaceable: Placeable,
        placeholderPlaceable: Placeable?,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        prefixPlaceable: Placeable?,
        suffixPlaceable: Placeable?,
        containerPlaceable: Placeable,
        supportingPlaceable: Placeable?,
        labelStartY: Int,
        labelEndY: Int,
        isLabelAbove: Boolean,
        labelProgress: Float,
        placeholderAlpha: FloatProducer,
        affixAlpha: FloatProducer,
        textPosition: Int,
        layoutDirection: LayoutDirection,
    ) {
        val yOffset = if (isLabelAbove) labelPlaceable.height else 0

        // place container
        containerPlaceable.place(0, yOffset)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the label (if it's Above) and the supporting text on bottom
        val height =
            totalHeight -
                supportingPlaceable.heightOrZero -
                (if (isLabelAbove) labelPlaceable.height else 0)

        leadingPlaceable?.placeRelative(
            0,
            yOffset + Alignment.CenterVertically.align(leadingPlaceable.height, height),
        )

        val labelY = lerpInt(labelStartY, labelEndY, labelProgress)
        if (isLabelAbove) {
            val labelX =
                labelPosition.minimizedAlignment.align(
                    size = labelPlaceable.width,
                    space = width,
                    layoutDirection = layoutDirection,
                )
            // Not placeRelative because alignment already handles RTL
            labelPlaceable.place(labelX, labelY)
        } else {
            val leftIconWidth =
                if (layoutDirection == LayoutDirection.Ltr) leadingPlaceable.widthOrZero
                else trailingPlaceable.widthOrZero
            val labelStartX =
                labelPosition.expandedAlignment.align(
                    size = labelPlaceable.width,
                    space = width - leadingPlaceable.widthOrZero - trailingPlaceable.widthOrZero,
                    layoutDirection = layoutDirection,
                ) + leftIconWidth
            val labelEndX =
                labelPosition.minimizedAlignment.align(
                    size = labelPlaceable.width,
                    space = width - leadingPlaceable.widthOrZero - trailingPlaceable.widthOrZero,
                    layoutDirection = layoutDirection,
                ) + leftIconWidth
            val labelX = lerpInt(labelStartX, labelEndX, labelProgress)
            // Not placeRelative because alignment already handles RTL
            labelPlaceable.place(labelX, labelY)
        }

        prefixPlaceable?.placeRelativeWithLayer(
            leadingPlaceable.widthOrZero,
            yOffset + textPosition,
        ) {
            alpha = affixAlpha()
        }

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero
        textfieldPlaceable.placeRelative(textHorizontalPosition, yOffset + textPosition)
        placeholderPlaceable?.placeRelativeWithLayer(
            textHorizontalPosition,
            yOffset + textPosition,
        ) {
            alpha = placeholderAlpha()
        }

        suffixPlaceable?.placeRelativeWithLayer(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            yOffset + textPosition,
        ) {
            alpha = affixAlpha()
        }

        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            yOffset + Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        supportingPlaceable?.placeRelative(0, yOffset + height)
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
        placeholderAlpha: FloatProducer,
        affixAlpha: FloatProducer,
        density: Float,
    ) {
        // place container
        containerPlaceable.place(IntOffset.Zero)

        // Most elements should be positioned w.r.t the text field's "visual" height, i.e.,
        // excluding the supporting text on bottom
        val height = totalHeight - supportingPlaceable.heightOrZero
        val topPadding = (paddingValues.calculateTopPadding().value * density).roundToInt()

        leadingPlaceable?.placeRelative(
            0,
            Alignment.CenterVertically.align(leadingPlaceable.height, height),
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

        prefixPlaceable?.placeRelativeWithLayer(
            leadingPlaceable.widthOrZero,
            calculateVerticalPosition(prefixPlaceable),
        ) {
            alpha = affixAlpha()
        }

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero

        textPlaceable.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(textPlaceable),
        )

        placeholderPlaceable?.placeRelativeWithLayer(
            textHorizontalPosition,
            calculateVerticalPosition(placeholderPlaceable),
        ) {
            alpha = placeholderAlpha()
        }

        suffixPlaceable?.placeRelativeWithLayer(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            calculateVerticalPosition(suffixPlaceable),
        ) {
            alpha = affixAlpha()
        }

        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        supportingPlaceable?.placeRelative(0, height)
    }
}

private class OutlinedTextFieldMeasurePolicy(
    private val onLabelMeasured: (Size) -> Unit,
    private val singleLine: Boolean,
    private val labelPosition: TextFieldLabelPosition,
    private val labelProgress: FloatProducer,
    private val placeholderAlpha: FloatProducer,
    private val affixAlpha: FloatProducer,
    private val paddingValues: PaddingValues,
    private val horizontalIconPadding: Dp,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val labelProgress = labelProgress()
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
                lerpInt(
                    occupiedSpaceHorizontally + totalHorizontalPadding, // label in middle
                    totalHorizontalPadding, // label in outline
                    labelProgress,
                )
            val labelConstraints =
                relaxedConstraints.offset(
                    horizontal = -labelHorizontalConstraintOffset,
                    vertical = -bottomPadding,
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
                    paddingValues.calculateTopPadding().roundToPx(),
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
                            supportingIntrinsicHeight,
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
                    bottomPadding,
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
                labelProgress = labelProgress,
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
                labelProgress = labelProgress,
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
                        maxHeight = height,
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
                placeholderAlpha = placeholderAlpha,
                affixAlpha = affixAlpha,
                density = density,
                layoutDirection = layoutDirection,
                isLabelAbove = isLabelAbove,
                labelProgress = labelProgress,
                iconPadding = horizontalIconPadding.toPx(),
            )
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.maxIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return intrinsicHeight(measurables, width) { intrinsicMeasurable, w ->
            intrinsicMeasurable.minIntrinsicHeight(w)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.maxIntrinsicWidth(h)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        return intrinsicWidth(measurables, height) { intrinsicMeasurable, h ->
            intrinsicMeasurable.minIntrinsicWidth(h)
        }
    }

    private fun IntrinsicMeasureScope.intrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
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
            labelProgress = labelProgress(),
        )
    }

    private fun IntrinsicMeasureScope.intrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
        intrinsicMeasurer: (IntrinsicMeasurable, Int) -> Int,
    ): Int {
        val labelProgress = labelProgress()
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
                ?.let { intrinsicMeasurer(it, lerpInt(remainingWidth, width, labelProgress)) } ?: 0

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
            labelProgress = labelProgress,
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
        labelProgress: Float,
    ): Int {
        val affixTotalWidth = prefixPlaceableWidth + suffixPlaceableWidth
        val middleSection =
            maxOf(
                textFieldPlaceableWidth + affixTotalWidth,
                placeholderPlaceableWidth + affixTotalWidth,
                // Prefix/suffix does not get applied to label
                lerpInt(labelPlaceableWidth, 0, labelProgress),
            )
        val wrappedWidth = leadingPlaceableWidth + middleSection + trailingPlaceableWidth

        // Actual LayoutDirection doesn't matter; we only need the sum
        val labelHorizontalPadding =
            (paddingValues.calculateLeftPadding(LayoutDirection.Ltr) +
                    paddingValues.calculateRightPadding(LayoutDirection.Ltr))
                .toPx()
        val focusedLabelWidth =
            ((labelPlaceableWidth + labelHorizontalPadding) * labelProgress).roundToInt()
        return constraints.constrainWidth(max(wrappedWidth, focusedLabelWidth))
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
        labelProgress: Float,
    ): Int {
        val inputFieldHeight =
            maxOf(
                textFieldHeight,
                placeholderHeight,
                prefixHeight,
                suffixHeight,
                if (isLabelAbove) 0 else lerpInt(labelHeight, 0, labelProgress),
            )
        val topPadding = paddingValues.calculateTopPadding().toPx()
        val actualTopPadding =
            if (isLabelAbove) {
                topPadding
            } else {
                lerpInt(topPadding, max(topPadding, labelHeight / 2f), labelProgress)
            }
        val bottomPadding = paddingValues.calculateBottomPadding().toPx()
        val middleSectionHeight = actualTopPadding + inputFieldHeight + bottomPadding

        return constraints.constrainHeight(
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
        placeholderAlpha: FloatProducer,
        affixAlpha: FloatProducer,
        density: Float,
        layoutDirection: LayoutDirection,
        isLabelAbove: Boolean,
        labelProgress: Float,
        iconPadding: Float,
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
            yOffset + Alignment.CenterVertically.align(leadingPlaceable.height, height),
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
            val positionY = lerpInt(startY, endY, labelProgress)

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
                        layoutDirection = layoutDirection,
                    ) + leftPadding
                val positionX = lerpInt(startX, endX, labelProgress).roundToInt()
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
            return if (labelPosition is TextFieldLabelPosition.Above) {
                defaultPosition
            } else {
                // Ensure components are placed below label when it's in the border
                max(defaultPosition, labelPlaceable.heightOrZero / 2)
            }
        }

        prefixPlaceable?.placeRelativeWithLayer(
            leadingPlaceable.widthOrZero,
            calculateVerticalPosition(prefixPlaceable),
        ) {
            alpha = affixAlpha()
        }

        val textHorizontalPosition = leadingPlaceable.widthOrZero + prefixPlaceable.widthOrZero

        textFieldPlaceable.placeRelative(
            textHorizontalPosition,
            calculateVerticalPosition(textFieldPlaceable),
        )

        // placed similar to the input text above
        placeholderPlaceable?.placeRelativeWithLayer(
            textHorizontalPosition,
            calculateVerticalPosition(placeholderPlaceable),
        ) {
            alpha = placeholderAlpha()
        }

        suffixPlaceable?.placeRelativeWithLayer(
            width - trailingPlaceable.widthOrZero - suffixPlaceable.width,
            calculateVerticalPosition(suffixPlaceable),
        ) {
            alpha = affixAlpha()
        }

        // placed center vertically and to the end edge horizontally
        trailingPlaceable?.placeRelative(
            width - trailingPlaceable.width,
            yOffset + Alignment.CenterVertically.align(trailingPlaceable.height, height),
        )

        supportingPlaceable?.placeRelative(0, yOffset + height)
    }
}

@Composable
private fun DecoratedLabel(
    labelProgress: State<Float>?,
    colors: TextFieldColors,
    enabled: Boolean,
    isError: Boolean,
    isFocused: Boolean,
    overrideLabelTextStyleColor: Boolean,
    transition: Transition<InputPhase>,
    bodySmall: TextStyle,
    bodyLarge: TextStyle,
    content: @Composable (TextFieldLabelScope.() -> Unit),
) {
    val labelScope = remember {
        object : TextFieldLabelScope {
            override val labelMinimizedProgress: Float
                get() = labelProgress?.value ?: 1f
        }
    }

    val labelColor = colors.labelColor(enabled, isError, isFocused)
    val labelTextStyleColor =
        if (overrideLabelTextStyleColor) {
            transition.labelTextStyleColor(
                focusedLabelTextStyleColor =
                    with(bodySmall.color) {
                        if (overrideLabelTextStyleColor) this.takeOrElse { labelColor } else this
                    },
                unfocusedLabelTextStyleColor =
                    with(bodyLarge.color) {
                        if (overrideLabelTextStyleColor) this.takeOrElse { labelColor } else this
                    },
            )
        } else {
            null
        }
    val labelContentColor = transition.labelContentColor(labelColor)
    val labelTextStyle =
        lerp(bodyLarge, bodySmall, labelProgress?.value ?: 1f).let { textStyle ->
            if (overrideLabelTextStyleColor) {
                textStyle.copy(color = labelTextStyleColor!!.value)
            } else {
                textStyle
            }
        }
    Decoration(labelContentColor.value, labelTextStyle) { labelScope.content() }
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
internal fun Modifier.textFieldBackground(color: ColorProducer, shape: Shape): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind { drawOutline(outline, color = color()) }
    }

/**
 * Replacement for Modifier.heightIn which takes the constraint lazily to avoid recomposition while
 * animating.
 */
internal fun Modifier.textFieldLabelMinHeight(minHeight: () -> Dp): Modifier =
    this.layout { measurable, constraints ->
        @Suppress("NAME_SHADOWING") val minHeight = minHeight()
        val resolvedMinHeight =
            constraints.constrainHeight(
                if (minHeight != Dp.Unspecified) minHeight.roundToPx() else 0
            )
        val placeable = measurable.measure(constraints.copy(minHeight = resolvedMinHeight))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

@Composable
private fun Transition<InputPhase>.labelProgress(showExpandedLabel: Boolean): State<Float> {
    val labelTransitionSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    return animateFloat(label = "LabelProgress", transitionSpec = { labelTransitionSpec }) {
        when (it) {
            InputPhase.Focused -> 1f
            InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
            InputPhase.UnfocusedNotEmpty -> 1f
        }
    }
}

@Composable
private fun Transition<InputPhase>.placeholderOpacity(showExpandedLabel: Boolean): State<Float> {
    val fastOpacityTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
    val slowOpacityTransitionSpec = MotionSchemeKeyTokens.SlowEffects.value<Float>()
    return animateFloat(
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
        },
    ) {
        when (it) {
            InputPhase.Focused -> 1f
            InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
            InputPhase.UnfocusedNotEmpty -> 0f
        }
    }
}

@Composable
private fun Transition<InputPhase>.affixOpacity(showExpandedLabel: Boolean): State<Float> {
    val fastOpacityTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
    return animateFloat(
        label = "PrefixSuffixOpacity",
        transitionSpec = { fastOpacityTransitionSpec },
    ) {
        when (it) {
            InputPhase.Focused -> 1f
            InputPhase.UnfocusedEmpty -> if (showExpandedLabel) 0f else 1f
            InputPhase.UnfocusedNotEmpty -> 1f
        }
    }
}

@Composable
private fun Transition<InputPhase>.labelTextStyleColor(
    focusedLabelTextStyleColor: Color,
    unfocusedLabelTextStyleColor: Color,
): State<Color> {
    val colorTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Color>()
    return animateColor(transitionSpec = { colorTransitionSpec }, label = "LabelTextStyleColor") {
        when (it) {
            InputPhase.Focused -> focusedLabelTextStyleColor
            else -> unfocusedLabelTextStyleColor
        }
    }
}

@Composable
private fun Transition<InputPhase>.labelContentColor(labelColor: Color): State<Color> {
    val colorTransitionSpec = MotionSchemeKeyTokens.FastEffects.value<Color>()
    @Suppress("UnusedTransitionTargetStateParameter")
    return animateColor(
        transitionSpec = { colorTransitionSpec },
        label = "LabelContentColor",
        targetValueByState = { labelColor },
    )
}

/** An internal state used to animate a label and an indicator. */
private enum class InputPhase {
    // Text field is focused
    Focused,

    // Text field is not focused and input text is empty
    UnfocusedEmpty,

    // Text field is not focused but input text is not empty
    UnfocusedNotEmpty,
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
