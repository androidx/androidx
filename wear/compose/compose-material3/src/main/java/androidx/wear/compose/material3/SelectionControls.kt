/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.materialcore.animateSelectionColor
import androidx.wear.compose.materialcore.directionVector
import androidx.wear.compose.materialcore.toRadians

/**
 * [Checkbox] provides an animated checkbox for use as a selection control in
 * [ToggleButton] or [SplitToggleButton].
 *
 * Checkbox sample:
 * @sample androidx.wear.compose.material3.samples.CheckboxSample
 *
 * @param checked Boolean flag indicating whether this checkbox is currently checked.
 * @param modifier Modifier to be applied to the checkbox. This can be used to provide a
 * content description for accessibility.
 * @param colors [CheckboxColors] from which the box and checkmark colors will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [Checkbox] (affects
 * the color).
 * @param onCheckedChange Callback to be invoked when Checkbox is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleButton] or [SplitToggleButton]).
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the Checkbox.
 */
@Composable
fun Checkbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = androidx.wear.compose.materialcore.Checkbox(
    checked = checked,
    modifier = modifier,
    boxColor = { isEnabled, isChecked ->
        colors.boxColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    checkmarkColor = { isEnabled, isChecked ->
        colors.checkmarkColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    enabled = enabled,
    onCheckedChange = onCheckedChange,
    interactionSource = interactionSource,
    drawBox = { drawScope, color, progress, isRtl ->
        drawScope.drawBox(
            color = color,
            progress = progress,
            isRtl = isRtl
        )
    },
    progressAnimationSpec = PROGRESS_ANIMATION_SPEC,
    width = WIDTH,
    height = HEIGHT
)

/**
 * [Switch] provides an animated switch for use as a selection control in
 * [ToggleButton] or [SplitToggleButton].
 *
 * Switch samples:
 * @sample androidx.wear.compose.material3.samples.SwitchSample
 * Example of a switch in an RTL locale
 * @sample androidx.wear.compose.material3.samples.RtlSwitchSample
 *
 * @param checked Boolean flag indicating whether this switch is currently toggled on.
 * @param modifier Modifier to be applied to the switch. This can be used to provide a
 * content description for accessibility.
 * @param colors [SwitchColors] from which the colors of the thumb and track will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [Switch] (affects
 * the color).
 * @param onCheckedChange Callback to be invoked when Switch is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleButton] or [SplitToggleButton]).
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the Switch.
 */
@Composable
fun Switch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.colors(),
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = androidx.wear.compose.materialcore.Switch(
    modifier = modifier,
    checked = checked,
    enabled = enabled,
    onCheckedChange = onCheckedChange,
    interactionSource = interactionSource,
    trackFillColor = { isEnabled, isChecked ->
        colors.trackColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    trackStrokeColor = { isEnabled, isChecked ->
        colors.trackStrokeColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    thumbColor = { isEnabled, isChecked ->
        colors.thumbColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    thumbIconColor = { isEnabled, isChecked ->
        colors.thumbIconColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    trackWidth = TRACK_WIDTH,
    trackHeight = TRACK_HEIGHT,
    drawThumb = { drawScope, thumbColor, progress, thumbIconColor, isRtl ->
        drawScope.drawThumb(
            thumbColor,
            progress,
            thumbIconColor,
            isRtl
        )
    },
    progressAnimationSpec = SWITCH_PROGRESS_ANIMATION_SPEC,
    width = WIDTH,
    height = HEIGHT
)

/**
 * [RadioButton] provides an animated radio button for use as a selection control in
 * [ToggleButton] or [SplitToggleButton].
 *
 * RadioButton sample:
 * @sample androidx.wear.compose.material3.samples.RadioButtonSample
 *
 * @param selected Boolean flag indicating whether this radio button is currently toggled on.
 * @param modifier Modifier to be applied to the radio button. This can be used to provide a
 * content description for accessibility.
 * @param colors [RadioButtonColors] from which the RadioButton colors will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [RadioButton] (affects
 * the color).
 * @param onClick Callback to be invoked when RadioButton is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleButton] or [SplitToggleButton]).
 * @param interactionSource When also providing [onClick], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the RadioButton.
 */
@Composable
fun RadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = androidx.wear.compose.materialcore.RadioButton(
    modifier = modifier,
    selected = selected,
    enabled = enabled,
    ringColor = { isEnabled, isSelected ->
        colors.color(
            enabled = isEnabled,
            selected = isSelected
        )
    },
    dotColor = { isEnabled, isSelected ->
        colors.color(
            enabled = isEnabled,
            selected = isSelected
        )
    },
    onClick = onClick,
    interactionSource = interactionSource,
    dotRadiusProgressDuration = { isSelected -> if (isSelected) MEDIUM_1 else SHORT_3 },
    dotAlphaProgressDuration = SHORT_3,
    dotAlphaProgressDelay = SHORT_2,
    easing = STANDARD_DECELERATE,
    width = WIDTH,
    height = HEIGHT
)

/**
 * Represents the content colors used in [Checkbox] in different states.
 *
 * @param checkedBoxColor The box color of [Checkbox] when enabled and checked.
 * @param checkedCheckmarkColor The check mark color of [Checkbox] when enabled
 * and checked.
 * @param uncheckedBoxColor The box color of [Checkbox] when enabled and unchecked.
 * @param uncheckedCheckmarkColor The check mark color of [Checkbox] when enabled
 * and unchecked.
 * @param disabledCheckedBoxColor The box color of [Checkbox] when disabled and checked.
 * @param disabledCheckedCheckmarkColor The check mark color of [Checkbox] when disabled
 * and checked.
 * @param disabledUncheckedBoxColor The box color of [Checkbox] when disabled and unchecked.
 * @param disabledUncheckedCheckmarkColor The check mark color of [Checkbox] when disabled
 * and unchecked.
 */
@Immutable
class CheckboxColors(
    val checkedBoxColor: Color,
    val checkedCheckmarkColor: Color,
    val uncheckedBoxColor: Color,
    val uncheckedCheckmarkColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledCheckedCheckmarkColor: Color,
    val disabledUncheckedBoxColor: Color,
    val disabledUncheckedCheckmarkColor: Color,
) {

    /**
     * Represents the box color for this [Checkbox], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Checkbox] is enabled
     * @param checked Whether the [Checkbox] is currently checked or unchecked
     */
    @Composable
    internal fun boxColor(enabled: Boolean, checked: Boolean): State<Color> = animateSelectionColor(
        enabled = enabled,
        checked = checked,
        checkedColor = checkedBoxColor,
        uncheckedColor = uncheckedBoxColor,
        disabledCheckedColor = disabledCheckedBoxColor,
        disabledUncheckedColor = disabledUncheckedBoxColor,
        animationSpec = COLOR_ANIMATION_SPEC
    )

    /**
     * Represents the checkmark color for this [Checkbox], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Checkbox] is enabled
     * @param checked Whether the [Checkbox] is currently checked or unchecked
     */
    @Composable
    internal fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedCheckmarkColor,
            uncheckedColor = uncheckedCheckmarkColor,
            disabledCheckedColor = disabledCheckedCheckmarkColor,
            disabledUncheckedColor = disabledUncheckedCheckmarkColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is CheckboxColors) return false

        if (checkedBoxColor != other.checkedBoxColor) return false
        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedCheckmarkColor != other.uncheckedCheckmarkColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledCheckedCheckmarkColor != other.disabledCheckedCheckmarkColor) return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false
        if (disabledUncheckedCheckmarkColor != other.disabledUncheckedCheckmarkColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedBoxColor.hashCode()
        result = 31 * result + checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        result = 31 * result + disabledUncheckedCheckmarkColor.hashCode()
        return result
    }
}

/**
 * Represents the content colors used in [Switch] in different states.
 *
 * @param checkedThumbColor The thumb color of [Switch] when enabled and checked.
 * @param checkedThumbIconColor The thumb icon color of [Switch] when enabled and checked.
 * @param checkedTrackColor The track color of [Switch] when enabled and checked.
 * @param checkedTrackBorderColor The track border color of [Switch] when enabled and checked.
 * @param uncheckedThumbColor The thumb color of [Switch] when enabled and unchecked.
 * @param uncheckedThumbIconColor The thumb icon color of [Switch] when enabled and unchecked.
 * @param uncheckedTrackColor The track color of [Switch] when enabled and unchecked.
 * @param uncheckedTrackBorderColor The track border color of [Switch] when enabled and unchecked.
 * @param disabledCheckedThumbColor The thumb color of [Switch] when disabled and checked.
 * @param disabledCheckedThumbIconColor The thumb icon color of [Switch] when disabled and checked.
 * @param disabledCheckedTrackColor The track color of [Switch] when disabled and checked.
 * @param disabledCheckedTrackBorderColor The track border color of [Switch] when disabled
 * and checked.
 * @param disabledUncheckedThumbColor The thumb color of [Switch] when disabled and unchecked.
 * @param disabledUncheckedThumbIconColor The thumb icon color of [Switch] when disabled
 * and unchecked.
 * @param disabledUncheckedTrackColor The track color of [Switch] when disabled and unchecked.
 * @param disabledUncheckedTrackBorderColor The track border color of [Switch] when disabled
 * and unchecked.
 */
@Immutable
class SwitchColors(
    val checkedThumbColor: Color,
    val checkedThumbIconColor: Color,
    val checkedTrackColor: Color,
    val checkedTrackBorderColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedThumbIconColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedTrackBorderColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledCheckedThumbIconColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledCheckedTrackBorderColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledUncheckedThumbIconColor: Color,
    val disabledUncheckedTrackColor: Color,
    val disabledUncheckedTrackBorderColor: Color,
) {
    @Composable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbColor,
            uncheckedColor = uncheckedThumbColor,
            disabledCheckedColor = disabledCheckedThumbColor,
            disabledUncheckedColor = disabledUncheckedThumbColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    internal fun thumbIconColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbIconColor,
            uncheckedColor = uncheckedThumbIconColor,
            disabledCheckedColor = disabledCheckedThumbIconColor,
            disabledUncheckedColor = disabledUncheckedThumbIconColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    internal fun trackColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackColor,
            uncheckedColor = uncheckedTrackColor,
            disabledCheckedColor = disabledCheckedTrackBorderColor,
            disabledUncheckedColor = disabledUncheckedTrackColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    internal fun trackStrokeColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackBorderColor,
            uncheckedColor = uncheckedTrackBorderColor,
            disabledCheckedColor = disabledCheckedTrackColor,
            disabledUncheckedColor = disabledUncheckedTrackBorderColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SwitchColors) return false

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedThumbIconColor != other.checkedThumbIconColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedTrackBorderColor != other.checkedTrackBorderColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedThumbIconColor != other.uncheckedThumbIconColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedTrackBorderColor != other.uncheckedTrackBorderColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedThumbIconColor != other.disabledCheckedThumbIconColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedThumbIconColor != other.disabledUncheckedThumbIconColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false
        if (disabledUncheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedThumbIconColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedTrackBorderColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedThumbIconColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedThumbIconColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedThumbIconColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedTrackBorderColor.hashCode()
        return result
    }
}

/**
 * Represents the content colors used in [RadioButton] in different states.
 *
 * @param selectedColor The color of the radio button when enabled and selected.
 * @param unselectedColor The color of the radio button when enabled and unselected.
 * @param disabledSelectedColor The color of the radio button when disabled and selected.
 * @param disabledUnselectedColor The color of the radio button when disabled and unselected.
 */
@Immutable
class RadioButtonColors(
    val selectedColor: Color,
    val unselectedColor: Color,
    val disabledSelectedColor: Color,
    val disabledUnselectedColor: Color
) {
    @Composable
    internal fun color(enabled: Boolean, selected: Boolean): State<Color> = animateSelectionColor(
        enabled = enabled,
        checked = selected,
        checkedColor = selectedColor,
        uncheckedColor = unselectedColor,
        disabledCheckedColor = disabledSelectedColor,
        disabledUncheckedColor = disabledUnselectedColor,
        animationSpec = COLOR_ANIMATION_SPEC
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RadioButtonColors) return false

        if (selectedColor != other.selectedColor) return false
        if (unselectedColor != other.unselectedColor) return false
        if (disabledSelectedColor != other.disabledSelectedColor) return false
        if (disabledUnselectedColor != other.disabledUnselectedColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedColor.hashCode()
        result = 31 * result + unselectedColor.hashCode()
        result = 31 * result + disabledSelectedColor.hashCode()
        result = 31 * result + disabledUnselectedColor.hashCode()
        return result
    }
}

/**
 * Contains the default values used by [Checkbox].
 */
object CheckboxDefaults {
    /**
     * Creates a [CheckboxColors] for use in a [Checkbox].
     *
     * @param checkedBoxColor The box color of this [Checkbox] when enabled and checked.
     * @param checkedCheckmarkColor The check mark color of this [Checkbox] when enabled
     * and checked.
     * @param uncheckedBoxColor The box color of this [Checkbox] when enabled and unchecked.
     * @param uncheckedCheckmarkColor The check mark color of this [Checkbox] when enabled
     * and unchecked.
     */
    @Composable
    fun colors(
        checkedBoxColor: Color = MaterialTheme.colorScheme.primary,
        checkedCheckmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
        uncheckedBoxColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedCheckmarkColor: Color = MaterialTheme.colorScheme.background,
    ): CheckboxColors = CheckboxColors(
        checkedBoxColor = checkedBoxColor,
        checkedCheckmarkColor = checkedCheckmarkColor,
        uncheckedBoxColor = uncheckedBoxColor,
        uncheckedCheckmarkColor = uncheckedCheckmarkColor,
        disabledCheckedBoxColor = checkedBoxColor.toDisabledColor(),
        disabledCheckedCheckmarkColor = checkedCheckmarkColor.toDisabledColor(),
        disabledUncheckedBoxColor = uncheckedBoxColor.toDisabledColor(),
        disabledUncheckedCheckmarkColor = uncheckedCheckmarkColor.toDisabledColor()
    )
}

/**
 * Contains the default values used by [Switch].
 */
object SwitchDefaults {
    /**
     * Creates a [SwitchColors] for use in a [Switch].
     *
     * @param checkedThumbColor The thumb color of this [Switch] when enabled and checked.
     * @param checkedThumbIconColor The thumb icon color of this [Switch] when enabled and checked.
     * @param checkedTrackColor The track color of this [Switch] when enabled and checked.
     * @param checkedTrackBorderColor The track border color of this [Switch] when enabled and checked.
     * @param uncheckedThumbColor The thumb color of this [Switch] when enabled and unchecked.
     * @param uncheckedThumbIconColor The thumb icon color of this [Switch] when enabled and checked.
     * @param uncheckedTrackColor The track color of this [Switch] when enabled and unchecked.
     * @param uncheckedTrackBorderColor The track border color of this [Switch] when enabled and unchecked.
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
        checkedThumbIconColor: Color = MaterialTheme.colorScheme.primary,
        checkedTrackColor: Color = MaterialTheme.colorScheme.primaryDim,
        checkedTrackBorderColor: Color = MaterialTheme.colorScheme.primaryDim,
        uncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        uncheckedThumbIconColor: Color = MaterialTheme.colorScheme.background,
        uncheckedTrackColor: Color = MaterialTheme.colorScheme.surface,
        uncheckedTrackBorderColor: Color = MaterialTheme.colorScheme.outline
    ): SwitchColors = SwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedThumbIconColor = checkedThumbIconColor,
        checkedTrackColor = checkedTrackColor,
        checkedTrackBorderColor = checkedTrackBorderColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedThumbIconColor = uncheckedThumbIconColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedTrackBorderColor = uncheckedTrackBorderColor,
        disabledCheckedThumbColor = checkedThumbColor.toDisabledColor(),
        disabledCheckedThumbIconColor = checkedThumbIconColor.toDisabledColor(),
        disabledCheckedTrackColor = checkedTrackColor.toDisabledColor(
            disabledAlpha = DisabledContainerAlpha
        ),
        disabledCheckedTrackBorderColor = checkedTrackBorderColor.toDisabledColor(
            disabledAlpha = DisabledBorderAlpha
        ),
        disabledUncheckedThumbColor = uncheckedThumbColor.toDisabledColor(),
        disabledUncheckedThumbIconColor = uncheckedThumbIconColor.toDisabledColor(),
        disabledUncheckedTrackColor = uncheckedTrackColor.toDisabledColor(
            disabledAlpha = DisabledContainerAlpha
        ),
        disabledUncheckedTrackBorderColor = uncheckedTrackBorderColor.toDisabledColor(
            disabledAlpha = DisabledBorderAlpha
        )
    )
}

/**
 * Contains the default values used by [RadioButton].
 */
object RadioButtonDefaults {
    /**
     * Creates a [RadioButtonColors] for use in a [RadioButton].
     *
     * @param selectedColor The color of the radio button when enabled and selected.
     * @param unselectedColor The color of the radio button when enabled and unselected.
     */
    @Composable
    fun colors(
        selectedColor: Color = MaterialTheme.colorScheme.primary,
        unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ): RadioButtonColors {
        return RadioButtonColors(
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            disabledSelectedColor = selectedColor.toDisabledColor(),
            disabledUnselectedColor = unselectedColor.toDisabledColor()
        )
    }
}

private fun DrawScope.drawBox(color: Color, progress: Float, isRtl: Boolean) {
    // Centering vertically.
    val topCornerPx = (HEIGHT - BOX_SIZE).toPx() / 2
    val strokeWidthPx = BOX_STROKE.toPx()
    val halfStrokeWidthPx = strokeWidthPx / 2.0f
    val radiusPx = BOX_RADIUS.toPx()
    val checkboxSizePx = BOX_SIZE.toPx()
    // Aligning the box to the end.
    val startXOffsetPx = if (isRtl) 0f else (WIDTH - HEIGHT).toPx()

    // Draw the outline of the box.
    drawRoundRect(
        color,
        topLeft = Offset(
            topCornerPx + halfStrokeWidthPx + startXOffsetPx,
            topCornerPx + halfStrokeWidthPx
        ),
        size = Size(checkboxSizePx - strokeWidthPx, checkboxSizePx - strokeWidthPx),
        cornerRadius = CornerRadius(radiusPx - halfStrokeWidthPx),
        alpha = 1 - progress,
        style = Stroke(strokeWidthPx)
    )

    // Fills the box.
    drawRoundRect(
        color,
        topLeft = Offset(topCornerPx + startXOffsetPx, topCornerPx),
        size = Size(checkboxSizePx, checkboxSizePx),
        cornerRadius = CornerRadius(radiusPx),
        alpha = progress,
        style = Fill
    )
}

private fun DrawScope.drawThumb(
    thumbColor: Color,
    progress: Float,
    thumbIconColor: Color,
    isRtl: Boolean
) {

    val thumbPaddingUnchecked = TRACK_HEIGHT / 2 - THUMB_RADIUS_UNCHECKED
    val thumbPaddingChecked = TRACK_HEIGHT / 2 - THUMB_RADIUS_CHECKED

    val switchThumbRadiusPx = lerp(
        start = THUMB_RADIUS_UNCHECKED.toPx(),
        stop = THUMB_RADIUS_CHECKED.toPx(),
        fraction = progress
    )

    val switchTrackLengthPx = WIDTH.toPx()

    // For Rtl mode the thumb progress will start from the end of the switch.
    val thumbProgressPx = if (isRtl)
        lerp(
            start = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingUnchecked.toPx(),
            stop = switchThumbRadiusPx + thumbPaddingChecked.toPx(),
            fraction = progress
        )
    else
        lerp(
            start = switchThumbRadiusPx + thumbPaddingUnchecked.toPx(),
            stop = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingChecked.toPx(),
            fraction = progress
        )

    drawCircle(
        color = thumbColor,
        radius = switchThumbRadiusPx,
        center = Offset(thumbProgressPx, center.y)
    )

    val totalDist = switchTrackLengthPx - 2 * switchThumbRadiusPx - 4.dp.toPx()

    // Offset value to be added if RTL mode is enabled.
    // We need to move the tick to the checked position in ltr mode when unchecked.
    val rtlOffset = switchTrackLengthPx - 2 * THUMB_RADIUS_CHECKED.toPx() - 4.dp.toPx()

    val distMoved = if (isRtl) rtlOffset - progress * totalDist else progress * totalDist

    // Draw tick icon
    drawTickIcon(thumbIconColor, progress, distMoved)
}

private fun DrawScope.drawTickIcon(tickColor: Color, alpha: Float, distMoved: Float) {
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val center = Offset(9.dp.toPx(), 9.dp.toPx())
    val angle = TICK_ROTATION - TICK_ROTATION / tickTotalLength * tickTotalLength
    val angleRadians = angle.toRadians()

    val baseStart = Offset(6.7f.dp.toPx() + distMoved, 13.3f.dp.toPx())

    val path = Path()
    path.moveTo(baseStart.rotate(angleRadians, center))
    path.lineTo(
        (baseStart + Offset(tickBaseLength, tickBaseLength)).rotate(angleRadians, center)
    )

    val stickStart = Offset(9.3f.dp.toPx() + distMoved, 16.3f.dp.toPx())
    // Move back to the start of the stick (without drawing)
    path.moveTo(stickStart.rotate(angleRadians, center))
    path.lineTo(
        Offset(stickStart.x + tickStickLength, stickStart.y - tickStickLength).rotate(
            angleRadians,
            center
        )
    )
    // Use StrokeCap.Butt because Square adds an extension on the end of each line.
    drawPath(
        path,
        tickColor,
        style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt),
        alpha = alpha
    )
}

private fun Path.moveTo(offset: Offset) {
    moveTo(offset.x, offset.y)
}

private fun Path.lineTo(offset: Offset) {
    lineTo(offset.x, offset.y)
}

private fun Offset.rotate(angleRadians: Float): Offset {
    val angledDirection = directionVector(angleRadians)
    return angledDirection * x + angledDirection.rotate90() * y
}

private fun Offset.rotate(angleRadians: Float, center: Offset): Offset =
    (this - center).rotate(angleRadians) + center

private fun Offset.rotate90() = Offset(-y, x)

private val BOX_STROKE = 2.dp
private val BOX_RADIUS = 2.dp
private val BOX_SIZE = 18.dp

private val THUMB_RADIUS_UNCHECKED = 7.dp
private val THUMB_RADIUS_CHECKED = 9.dp
private val TRACK_WIDTH = 32.dp
private val TRACK_HEIGHT = 22.dp
private val TICK_BASE_LENGTH = 3.dp
private val TICK_STICK_LENGTH = 7.dp
private const val TICK_ROTATION = 15f

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> = tween(MEDIUM_1, 0, STANDARD_DECELERATE)
private val PROGRESS_ANIMATION_SPEC: TweenSpec<Float> = tween(MEDIUM_1, 0, STANDARD_DECELERATE)
private val SWITCH_PROGRESS_ANIMATION_SPEC: TweenSpec<Float> =
    tween(MEDIUM_2, 0, STANDARD_DECELERATE)

private val WIDTH = 32.dp
private val HEIGHT = 24.dp
