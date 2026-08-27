/*
 * Copyright 2026 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.role
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.max
import androidx.compose.remote.creation.compose.state.min
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.round
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.remote.material3.internal.RemoteIcons

/**
 * [RemoteSlider] allows users to make a selection from a range of values.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSliderSample
 * @param value Current value of the Slider.
 * @param steps The number of steps between the min and max value of the [valueRange].
 * @param modifier Modifiers to be applied to the Slider.
 * @param decreaseAction Action to perform when the decrease button is clicked.
 * @param increaseAction Action to perform when the increase button is clicked.
 * @param enabled Controls the enabled state of the Slider. Note that only constant values are
 *   currently supported for [enabled] for click handling.
 * @param valueRange The range of values that this slider can take.
 * @param segmented Controls whether the slider displays segment indicators.
 * @param shape Shape of the slider container.
 * @param colors [RemoteSliderColors] that will be used to resolve the colors used for this
 *   [RemoteSlider].
 * @param decreaseIcon A slot for an icon which is placed on the decrease button.
 * @param increaseIcon A slot for an icon which is placed on the increase button.
 */
@Composable
@RemoteComposable
public fun RemoteSlider(
    value: RemoteFloat,
    steps: Int = 0,
    modifier: RemoteModifier = RemoteModifier,
    decreaseAction: Action = Action.Empty,
    increaseAction: Action = Action.Empty,
    enabled: RemoteBoolean = true.rb,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= RemoteSliderDefaults.MaxSegmentSteps,
    shape: RemoteShape = RemoteSliderDefaults.shape,
    colors: RemoteSliderColors = RemoteSliderDefaults.sliderColors(),
    decreaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteSliderDefaults.DecreaseIcon()
    },
    increaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteSliderDefaults.IncreaseIcon()
    },
) {
    require(steps >= 0) { "steps should be greater than or equal to 0" }

    val isEnabled = enabled.constantValueOrNull ?: true
    val visibleSegments = if (segmented) steps + 1 else 1
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val valueRangeStart = valueRange.start.rf
    val rangeSpan = (valueRange.endInclusive - valueRange.start).rf
    val normalizedValue = (value - valueRangeStart) / rangeSpan
    val valueRatio =
        if (steps > 0) {
            val stepCount = (steps + 1).toFloat().rf
            min(max(round(normalizedValue * stepCount) / stepCount, 0f.rf), 1f.rf)
        } else {
            min(max(normalizedValue, 0f.rf), 1f.rf)
        }

    RemoteRow(
        verticalAlignment = RemoteAlignment.CenterVertically,
        horizontalArrangement = RemoteArrangement.Start,
        modifier =
            modifier
                .fillMaxWidth()
                .height(RemoteSliderDefaults.Height)
                .drawWithContent {
                    drawSolidColorShape(shape, colors.containerColor(enabled))
                    drawContent()
                }
                .clip(shape),
    ) {
        RemoteBox(
            modifier =
                RemoteModifier.size(RemoteSliderDefaults.ControlSize)
                    // TODO(b/557131896): Use repeatableClickable once supported
                    .clickable(decreaseAction, enabled = isEnabled)
                    .semantics(mergeDescendants = true) { role = Role.Button },
            contentAlignment = RemoteAlignment.Center,
            content =
                provideScopeContent(
                    colors.buttonIconColor(enabled),
                    RemoteMaterialTheme.typography.labelMedium,
                    decreaseIcon,
                ),
        )

        RemoteCanvas(
            modifier = RemoteModifier.height(RemoteSliderDefaults.BarHeight).weight(1f.rf)
        ) {
            drawProgressBar(
                selectedBarColor = colors.selectedBarColor(enabled),
                unselectedBarColor = colors.unselectedBarColor(enabled),
                selectedBarSeparatorColor = colors.selectedBarSeparatorColor(enabled),
                unselectedBarSeparatorColor = colors.unselectedBarSeparatorColor(enabled),
                visibleSegments = visibleSegments,
                valueRatio = valueRatio,
                isRtl = isRtl,
                segmented = segmented,
            )
        }

        RemoteBox(
            modifier =
                RemoteModifier.size(RemoteSliderDefaults.ControlSize)
                    // TODO(b/557131896): Use repeatableClickable once supported
                    .clickable(increaseAction, enabled = isEnabled)
                    .semantics(mergeDescendants = true) { role = Role.Button },
            contentAlignment = RemoteAlignment.Center,
            content =
                provideScopeContent(
                    colors.buttonIconColor(enabled),
                    RemoteMaterialTheme.typography.labelMedium,
                    increaseIcon,
                ),
        )
    }
}

/**
 * [RemoteSlider] allows users to make a selection from a range of values.
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteSliderIntegerSample
 * @param value Current value of the Slider.
 * @param steps The number of steps between the min and max value of the [valueRange].
 * @param modifier Modifiers to be applied to the Slider.
 * @param decreaseAction Action to perform when the decrease button is clicked.
 * @param increaseAction Action to perform when the increase button is clicked.
 * @param enabled Controls the enabled state of the Slider. Note that only constant values are
 *   currently supported for [enabled] for click handling.
 * @param valueRange The range of values that this slider can take.
 * @param segmented Controls whether the slider displays segment indicators.
 * @param shape Shape of the slider container.
 * @param colors [RemoteSliderColors] that will be used to resolve the colors used for this
 *   [RemoteSlider].
 * @param decreaseIcon A slot for an icon which is placed on the decrease button.
 * @param increaseIcon A slot for an icon which is placed on the increase button.
 */
@Composable
@RemoteComposable
public fun RemoteSlider(
    value: RemoteInt,
    steps: Int = 0,
    modifier: RemoteModifier = RemoteModifier,
    decreaseAction: Action = Action.Empty,
    increaseAction: Action = Action.Empty,
    enabled: RemoteBoolean = true.rb,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= RemoteSliderDefaults.MaxSegmentSteps,
    shape: RemoteShape = RemoteSliderDefaults.shape,
    colors: RemoteSliderColors = RemoteSliderDefaults.sliderColors(),
    decreaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteSliderDefaults.DecreaseIcon()
    },
    increaseIcon: @Composable @RemoteComposable () -> Unit = {
        RemoteSliderDefaults.IncreaseIcon()
    },
): Unit =
    RemoteSlider(
        value = value.toRemoteFloat(),
        steps = steps,
        modifier = modifier,
        decreaseAction = decreaseAction,
        increaseAction = increaseAction,
        enabled = enabled,
        valueRange = valueRange,
        segmented = segmented,
        shape = shape,
        colors = colors,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
    )

/** Contains the default values used by [RemoteSlider]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteSliderDefaults {
    /** The recommended shape for [RemoteSlider]. */
    public val shape: RemoteShape
        @Composable @RemoteComposable get() = RemoteCircleShape

    /** Default height for the [RemoteSlider]. */
    public val Height: RemoteDp = 52.rdp

    /** Default control button size for increase and decrease buttons. */
    public val ControlSize: RemoteDp = 48.rdp

    /** Default height of the slider bar area. */
    public val BarHeight: RemoteDp = 12.rdp

    /** Height of the selected portion of the slider bar. */
    public val SelectedBarHeight: RemoteDp = 12.rdp

    /** Height of the unselected portion of the slider bar. */
    public val UnselectedBarHeight: RemoteDp = 4.rdp

    /** Radius of segment separators in the slider bar. */
    public val BarSeparatorRadius: RemoteDp = 2.rdp

    /** Padding applied to segmented slider bar ends. */
    public val SegmentBarPadding: RemoteDp = 4.rdp

    /** Default size for increase and decrease icons. */
    public val IconSize: RemoteDp = 24.rdp

    /** The maximum number of steps allowed for a segmented slider. */
    public const val MaxSegmentSteps: Int = 8

    /** The default icon for decrease button. */
    @Composable
    @RemoteComposable
    public fun DecreaseIcon(
        modifier: RemoteModifier = RemoteModifier,
        contentDescription: RemoteString? = null,
    ): Unit =
        RemoteIcon(
            imageVector = RemoteIcons.Remove,
            contentDescription = contentDescription,
            modifier = modifier.size(IconSize),
        )

    /** The recommended increase icon. */
    @Composable
    @RemoteComposable
    public fun IncreaseIcon(
        modifier: RemoteModifier = RemoteModifier,
        contentDescription: RemoteString? = null,
    ): Unit =
        RemoteIcon(
            imageVector = RemoteIcons.Add,
            contentDescription = contentDescription,
            modifier = modifier.size(IconSize),
        )

    /** Creates a [RemoteSliderColors] with default colors. */
    @Composable
    public fun sliderColors(): RemoteSliderColors =
        RemoteMaterialTheme.colorScheme.defaultSliderColors

    /** Creates a [RemoteSliderColors] with customizable colors. */
    @Composable
    public fun sliderColors(
        containerColor: RemoteColor? = null,
        buttonIconColor: RemoteColor? = null,
        selectedBarColor: RemoteColor? = null,
        unselectedBarColor: RemoteColor? = null,
        selectedBarSeparatorColor: RemoteColor? = null,
        unselectedBarSeparatorColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledButtonIconColor: RemoteColor? = null,
        disabledSelectedBarColor: RemoteColor? = null,
        disabledUnselectedBarColor: RemoteColor? = null,
        disabledSelectedBarSeparatorColor: RemoteColor? = null,
        disabledUnselectedBarSeparatorColor: RemoteColor? = null,
    ): RemoteSliderColors =
        RemoteMaterialTheme.colorScheme.defaultSliderColors.copy(
            containerColor = containerColor,
            buttonIconColor = buttonIconColor,
            selectedBarColor = selectedBarColor,
            unselectedBarColor = unselectedBarColor,
            selectedBarSeparatorColor = selectedBarSeparatorColor,
            unselectedBarSeparatorColor = unselectedBarSeparatorColor,
            disabledContainerColor = disabledContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
            disabledSelectedBarColor = disabledSelectedBarColor,
            disabledUnselectedBarColor = disabledUnselectedBarColor,
            disabledSelectedBarSeparatorColor = disabledSelectedBarSeparatorColor,
            disabledUnselectedBarSeparatorColor = disabledUnselectedBarSeparatorColor,
        )

    /** Creates a [RemoteSliderColors] with variant colors. */
    @Composable
    public fun variantSliderColors(): RemoteSliderColors =
        RemoteMaterialTheme.colorScheme.variantSliderColors

    /** Creates a [RemoteSliderColors] with variant customizable colors. */
    @Composable
    public fun variantSliderColors(
        containerColor: RemoteColor? = null,
        buttonIconColor: RemoteColor? = null,
        selectedBarColor: RemoteColor? = null,
        unselectedBarColor: RemoteColor? = null,
        selectedBarSeparatorColor: RemoteColor? = null,
        unselectedBarSeparatorColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledButtonIconColor: RemoteColor? = null,
        disabledSelectedBarColor: RemoteColor? = null,
        disabledUnselectedBarColor: RemoteColor? = null,
        disabledSelectedBarSeparatorColor: RemoteColor? = null,
        disabledUnselectedBarSeparatorColor: RemoteColor? = null,
    ): RemoteSliderColors =
        RemoteMaterialTheme.colorScheme.variantSliderColors.copy(
            containerColor = containerColor,
            buttonIconColor = buttonIconColor,
            selectedBarColor = selectedBarColor,
            unselectedBarColor = unselectedBarColor,
            selectedBarSeparatorColor = selectedBarSeparatorColor,
            unselectedBarSeparatorColor = unselectedBarSeparatorColor,
            disabledContainerColor = disabledContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
            disabledSelectedBarColor = disabledSelectedBarColor,
            disabledUnselectedBarColor = disabledUnselectedBarColor,
            disabledSelectedBarSeparatorColor = disabledSelectedBarSeparatorColor,
            disabledUnselectedBarSeparatorColor = disabledUnselectedBarSeparatorColor,
        )

    /** Creates a [RemoteSliderColors] with default colors. */
    @Composable public fun colors(): RemoteSliderColors = sliderColors()

    /** Creates a [RemoteSliderColors] with customizable colors. */
    @Composable
    public fun colors(
        containerColor: RemoteColor? = null,
        buttonIconColor: RemoteColor? = null,
        selectedBarColor: RemoteColor? = null,
        unselectedBarColor: RemoteColor? = null,
        selectedBarSeparatorColor: RemoteColor? = null,
        unselectedBarSeparatorColor: RemoteColor? = null,
        disabledContainerColor: RemoteColor? = null,
        disabledButtonIconColor: RemoteColor? = null,
        disabledSelectedBarColor: RemoteColor? = null,
        disabledUnselectedBarColor: RemoteColor? = null,
        disabledSelectedBarSeparatorColor: RemoteColor? = null,
        disabledUnselectedBarSeparatorColor: RemoteColor? = null,
    ): RemoteSliderColors =
        sliderColors(
            containerColor = containerColor,
            buttonIconColor = buttonIconColor,
            selectedBarColor = selectedBarColor,
            unselectedBarColor = unselectedBarColor,
            selectedBarSeparatorColor = selectedBarSeparatorColor,
            unselectedBarSeparatorColor = unselectedBarSeparatorColor,
            disabledContainerColor = disabledContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
            disabledSelectedBarColor = disabledSelectedBarColor,
            disabledUnselectedBarColor = disabledUnselectedBarColor,
            disabledSelectedBarSeparatorColor = disabledSelectedBarSeparatorColor,
            disabledUnselectedBarSeparatorColor = disabledUnselectedBarSeparatorColor,
        )

    private val RemoteColorScheme.defaultSliderColors: RemoteSliderColors
        get() =
            RemoteSliderColors(
                containerColor = surfaceContainer,
                buttonIconColor = onSurface,
                selectedBarColor = primary,
                unselectedBarColor = background.copy(alpha = 0.3f.rf),
                selectedBarSeparatorColor = primaryContainer,
                unselectedBarSeparatorColor = primary.copy(alpha = 0.5f.rf),
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledButtonIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSelectedBarColor = outlineVariant,
                disabledUnselectedBarColor = background.copy(alpha = 0.3f.rf),
                disabledSelectedBarSeparatorColor = surfaceContainer,
                disabledUnselectedBarSeparatorColor = outlineVariant.copy(alpha = 0.5f.rf),
            )

    private val RemoteColorScheme.variantSliderColors: RemoteSliderColors
        get() =
            RemoteSliderColors(
                containerColor = surfaceContainer,
                buttonIconColor = onSurface,
                selectedBarColor = primaryDim,
                unselectedBarColor = background.copy(alpha = 0.3f.rf),
                selectedBarSeparatorColor = primaryContainer,
                unselectedBarSeparatorColor = primaryDim.copy(alpha = 0.5f.rf),
                disabledContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledButtonIconColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledSelectedBarColor = outlineVariant,
                disabledUnselectedBarColor = background.copy(alpha = 0.3f.rf),
                disabledSelectedBarSeparatorColor = surfaceContainer,
                disabledUnselectedBarSeparatorColor = outlineVariant.copy(alpha = 0.5f.rf),
            )
}

/** Represents the colors used by a [RemoteSlider] in different states. */
@Immutable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteSliderColors(
    public val containerColor: RemoteColor,
    public val buttonIconColor: RemoteColor,
    public val selectedBarColor: RemoteColor,
    public val unselectedBarColor: RemoteColor,
    public val selectedBarSeparatorColor: RemoteColor,
    public val unselectedBarSeparatorColor: RemoteColor,
    public val disabledContainerColor: RemoteColor,
    public val disabledButtonIconColor: RemoteColor,
    public val disabledSelectedBarColor: RemoteColor,
    public val disabledUnselectedBarColor: RemoteColor,
    public val disabledSelectedBarSeparatorColor: RemoteColor,
    public val disabledUnselectedBarSeparatorColor: RemoteColor,
) {
    /** Returns a copy of this [RemoteSliderColors], optionally overriding some values. */
    public fun copy(
        containerColor: RemoteColor? = this.containerColor,
        buttonIconColor: RemoteColor? = this.buttonIconColor,
        selectedBarColor: RemoteColor? = this.selectedBarColor,
        unselectedBarColor: RemoteColor? = this.unselectedBarColor,
        selectedBarSeparatorColor: RemoteColor? = this.selectedBarSeparatorColor,
        unselectedBarSeparatorColor: RemoteColor? = this.unselectedBarSeparatorColor,
        disabledContainerColor: RemoteColor? = this.disabledContainerColor,
        disabledButtonIconColor: RemoteColor? = this.disabledButtonIconColor,
        disabledSelectedBarColor: RemoteColor? = this.disabledSelectedBarColor,
        disabledUnselectedBarColor: RemoteColor? = this.disabledUnselectedBarColor,
        disabledSelectedBarSeparatorColor: RemoteColor? = this.disabledSelectedBarSeparatorColor,
        disabledUnselectedBarSeparatorColor: RemoteColor? =
            this.disabledUnselectedBarSeparatorColor,
    ): RemoteSliderColors =
        RemoteSliderColors(
            containerColor = containerColor ?: this.containerColor,
            buttonIconColor = buttonIconColor ?: this.buttonIconColor,
            selectedBarColor = selectedBarColor ?: this.selectedBarColor,
            unselectedBarColor = unselectedBarColor ?: this.unselectedBarColor,
            selectedBarSeparatorColor = selectedBarSeparatorColor ?: this.selectedBarSeparatorColor,
            unselectedBarSeparatorColor =
                unselectedBarSeparatorColor ?: this.unselectedBarSeparatorColor,
            disabledContainerColor = disabledContainerColor ?: this.disabledContainerColor,
            disabledButtonIconColor = disabledButtonIconColor ?: this.disabledButtonIconColor,
            disabledSelectedBarColor = disabledSelectedBarColor ?: this.disabledSelectedBarColor,
            disabledUnselectedBarColor =
                disabledUnselectedBarColor ?: this.disabledUnselectedBarColor,
            disabledSelectedBarSeparatorColor =
                disabledSelectedBarSeparatorColor ?: this.disabledSelectedBarSeparatorColor,
            disabledUnselectedBarSeparatorColor =
                disabledUnselectedBarSeparatorColor ?: this.disabledUnselectedBarSeparatorColor,
        )

    internal fun containerColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(containerColor, disabledContainerColor)

    internal fun buttonIconColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(buttonIconColor, disabledButtonIconColor)

    internal fun selectedBarColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(selectedBarColor, disabledSelectedBarColor)

    internal fun unselectedBarColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(unselectedBarColor, disabledUnselectedBarColor)

    internal fun selectedBarSeparatorColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(selectedBarSeparatorColor, disabledSelectedBarSeparatorColor)

    internal fun unselectedBarSeparatorColor(enabled: RemoteBoolean): RemoteColor =
        enabled.select(unselectedBarSeparatorColor, disabledUnselectedBarSeparatorColor)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteSliderColors) return false

        if (containerColor != other.containerColor) return false
        if (buttonIconColor != other.buttonIconColor) return false
        if (selectedBarColor != other.selectedBarColor) return false
        if (unselectedBarColor != other.unselectedBarColor) return false
        if (selectedBarSeparatorColor != other.selectedBarSeparatorColor) return false
        if (unselectedBarSeparatorColor != other.unselectedBarSeparatorColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledButtonIconColor != other.disabledButtonIconColor) return false
        if (disabledSelectedBarColor != other.disabledSelectedBarColor) return false
        if (disabledUnselectedBarColor != other.disabledUnselectedBarColor) return false
        if (disabledSelectedBarSeparatorColor != other.disabledSelectedBarSeparatorColor)
            return false
        if (disabledUnselectedBarSeparatorColor != other.disabledUnselectedBarSeparatorColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + buttonIconColor.hashCode()
        result = 31 * result + selectedBarColor.hashCode()
        result = 31 * result + unselectedBarColor.hashCode()
        result = 31 * result + selectedBarSeparatorColor.hashCode()
        result = 31 * result + unselectedBarSeparatorColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledButtonIconColor.hashCode()
        result = 31 * result + disabledSelectedBarColor.hashCode()
        result = 31 * result + disabledUnselectedBarColor.hashCode()
        result = 31 * result + disabledSelectedBarSeparatorColor.hashCode()
        result = 31 * result + disabledUnselectedBarSeparatorColor.hashCode()
        return result
    }
}

private fun RemoteDrawScope.drawProgressBar(
    selectedBarColor: RemoteColor,
    unselectedBarColor: RemoteColor,
    selectedBarSeparatorColor: RemoteColor,
    unselectedBarSeparatorColor: RemoteColor,
    visibleSegments: Int,
    valueRatio: RemoteFloat,
    isRtl: Boolean,
    segmented: Boolean,
) {
    val barHeightInPx = RemoteSliderDefaults.SelectedBarHeight.toPx()
    val unselectedBarHeightInPx = RemoteSliderDefaults.UnselectedBarHeight.toPx()
    val separatorRadiusInPx = RemoteSliderDefaults.BarSeparatorRadius.toPx()
    val segmentBarPaddingInPx = RemoteSliderDefaults.SegmentBarPadding.toPx()

    val barWidthInPx =
        if (segmented) {
            val nonZeroWidth =
                (width + separatorRadiusInPx * 2f.rf) * valueRatio + segmentBarPaddingInPx
            valueRatio.isGreaterThan(0f.rf).select(nonZeroWidth, 0f.rf)
        } else {
            width * valueRatio
        }

    val selectedTopLeftX = if (isRtl) width - barWidthInPx else 0f.rf
    val unselectedTopLeftX = if (isRtl) 0f.rf else barWidthInPx

    // Unselected bar
    val unselectedPaint = RemotePaint {
        style = PaintingStyle.Fill
        color = unselectedBarColor
    }
    drawRoundRect(
        paint = unselectedPaint,
        topLeft = RemoteOffset(unselectedTopLeftX, (height - unselectedBarHeightInPx) / 2f.rf),
        size = RemoteSize(max(width - barWidthInPx, 0f.rf), unselectedBarHeightInPx),
        cornerRadius =
            RemoteOffset(unselectedBarHeightInPx / 2f.rf, unselectedBarHeightInPx / 2f.rf),
    )

    // Selected bar
    val selectedPaint = RemotePaint {
        style = PaintingStyle.Fill
        color = selectedBarColor
    }
    drawRoundRect(
        paint = selectedPaint,
        topLeft = RemoteOffset(selectedTopLeftX, (height - barHeightInPx) / 2f.rf),
        size = RemoteSize(barWidthInPx, barHeightInPx),
        cornerRadius = RemoteOffset(barHeightInPx / 2f.rf, barHeightInPx / 2f.rf),
    )

    // Separators
    for (separator in 1 until visibleSegments) {
        val separatorRatio = (separator.toFloat() / visibleSegments.toFloat()).rf
        val isSelected = separatorRatio.isLessThanOrEqualTo(valueRatio)
        val sepColor = isSelected.select(selectedBarSeparatorColor, unselectedBarSeparatorColor)
        val separatorX =
            if (!isRtl) {
                separator.toFloat().rf
            } else {
                (visibleSegments - separator).toFloat().rf
            } * (width + separatorRadiusInPx * 2f.rf) / visibleSegments.toFloat().rf -
                separatorRadiusInPx

        val separatorPaint = RemotePaint {
            style = PaintingStyle.Fill
            color = sepColor
            blendMode = BlendMode.Src
        }
        drawCircle(
            paint = separatorPaint,
            radius = separatorRadiusInPx,
            center = RemoteOffset(separatorX, height / 2f.rf),
        )
    }
}
