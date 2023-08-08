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
package androidx.wear.compose.material

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * [Checkbox] provides an animated checkbox for use as a toggle control in
 * [ToggleChip] or [SplitToggleChip].
 *
 * Example of a [SplitToggleChip] with [Checkbox] toggle control:
 * @sample androidx.wear.compose.material.samples.SplitToggleChipWithCheckbox
 *
 * @param checked Boolean flag indicating whether this checkbox is currently checked.
 * @param modifier Modifier to be applied to the checkbox. This can be used to provide a
 * content description for accessibility.
 * @param colors [CheckboxColors] from which the box and checkmark colors will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [Checkbox] (affects
 * the color).
 * @param onCheckedChange Callback to be invoked when Checkbox is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleChip] or [SplitToggleChip]).
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the Checkbox.
 */
@Composable
public fun Checkbox(
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
    drawBox = { drawScope, color, _, _ -> drawScope.drawBox(color) },
    progressAnimationSpec = PROGRESS_ANIMATION_SPEC,
    width = WIDTH,
    height = HEIGHT
)

/**
 * [Switch] provides an animated switch for use as a toggle control in
 * [ToggleChip] or [SplitToggleChip].
 *
 * Example of a [ToggleChip] with [Switch] toggle control:
 * @sample androidx.wear.compose.material.samples.ToggleChipWithSwitch
 *
 * @param checked Boolean flag indicating whether this switch is currently toggled on.
 * @param modifier Modifier to be applied to the switch. This can be used to provide a
 * content description for accessibility.
 * @param colors [SwitchColors] from which the colors of the thumb and track will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [Switch] (affects
 * the color).
 * @param onCheckedChange Callback to be invoked when Switch is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleChip] or [SplitToggleChip]).
 * @param interactionSource When also providing [onCheckedChange], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the Switch.
 */
@Composable
public fun Switch(
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
        colors.trackColor(
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
        colors.thumbColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    trackWidth = SWITCH_TRACK_LENGTH,
    trackHeight = SWITCH_TRACK_HEIGHT,
    drawThumb = { drawScope, color, progress, _, isRtl ->
        drawScope.drawThumb(color = color, progress = progress, isRtl = isRtl)
    },
    progressAnimationSpec = PROGRESS_ANIMATION_SPEC,
    width = WIDTH,
    height = HEIGHT
)

/**
 * [RadioButton] provides an animated radio button for use as a toggle control in
 * [ToggleChip] or [SplitToggleChip].
 *
 * Example of a [ToggleChip] with [RadioButton] toggle control:
 * @sample androidx.wear.compose.material.samples.ToggleChipWithRadioButton
 *
 * @param selected Boolean flag indicating whether this radio button is currently toggled on.
 * @param modifier Modifier to be applied to the radio button. This can be used to provide a
 * content description for accessibility.
 * @param colors [ToggleChipColors] from which the toggleControlColors will be obtained.
 * @param enabled Boolean flag indicating the enabled state of the [RadioButton] (affects
 * the color).
 * @param onClick Callback to be invoked when RadioButton is clicked. If null, then this is
 * passive and relies entirely on a higher-level component to control the state
 * (such as [ToggleChip] or [SplitToggleChip]).
 * @param interactionSource When also providing [onClick], the [MutableInteractionSource]
 * representing the stream of [Interaction]s for the "toggleable" tap area -
 * can be used to customise the appearance / behavior of the RadioButton.
 */
@Composable
public fun RadioButton(
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
        colors.ringColor(
            enabled = isEnabled,
            selected = isSelected
        )
    },
    dotColor = { isEnabled, isSelected ->
        colors.dotColor(
            enabled = isEnabled,
            selected = isSelected
        )
    },
    onClick = onClick,
    interactionSource = interactionSource,
    dotRadiusProgressDuration = { isSelected -> if (isSelected) QUICK else RAPID },
    dotAlphaProgressDuration = RAPID,
    dotAlphaProgressDelay = FLASH,
    easing = STANDARD_IN,
    width = WIDTH,
    height = HEIGHT
)

/**
 * Represents the content colors used in [Checkbox] in different states.
 */
@Stable
public interface CheckboxColors {
    /**
     * Represents the box color for this [Checkbox], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Checkbox] is enabled
     * @param checked Whether the [Checkbox] is currently checked or unchecked
     */
    @Composable
    public fun boxColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the checkmark color for this [Checkbox], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Checkbox] is enabled
     * @param checked Whether the [Checkbox] is currently checked or unchecked
     */
    @Composable
    public fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Represents the content colors used in [Switch] in different states.
 */
@Stable
public interface SwitchColors {
    /**
     * Represents the thumb color for this [Switch], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Switch] is enabled
     * @param checked Whether the [Switch] is currently checked or unchecked
     */
    @Composable
    public fun thumbColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the track color for this [Switch], depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [Switch] is enabled
     * @param checked Whether the [Switch] is currently checked or unchecked
     */
    @Composable
    public fun trackColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Represents the content colors used in [RadioButton] in different states.
 */
@Stable
public interface RadioButtonColors {
    /**
     * Represents the outer ring color for this [RadioButton], depending on
     * the [enabled] and [selected] properties.
     *
     * @param enabled Whether the [RadioButton] is enabled
     * @param selected Whether the [RadioButton] is currently selected or unselected
     */
    @Composable
    public fun ringColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the inner dot color for this [RadioButton], depending on
     * the [enabled] and [selected] properties.
     *
     * @param enabled Whether the [RadioButton] is enabled
     * @param selected Whether the [RadioButton] is currently selected or unselected
     */
    @Composable
    public fun dotColor(enabled: Boolean, selected: Boolean): State<Color>
}

/**
 * Contains the default values used by [Checkbox].
 */
public object CheckboxDefaults {
    /**
     * Creates a [CheckboxColors] for use in a [Checkbox].
     *
     * @param checkedBoxColor The box color of this [Checkbox] when enabled and checked.
     * @param uncheckedBoxColor The box color of this [Checkbox] when enabled and unchecked.
     * @param checkedCheckmarkColor The check mark color of this [Checkbox] when enabled
     * and checked.
     * @param uncheckedCheckmarkColor The check mark color of this [Checkbox] when enabled
     * and unchecked.
     */
    @Composable
    public fun colors(
        checkedBoxColor: Color = MaterialTheme.colors.secondary,
        checkedCheckmarkColor: Color = checkedBoxColor,
        uncheckedBoxColor: Color = contentColorFor(
            MaterialTheme.colors.primary.copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colors.surface)
        ),
        uncheckedCheckmarkColor: Color = uncheckedBoxColor,
    ): CheckboxColors {
        return DefaultCheckboxColors(
            checkedBoxColor = checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedBoxColor = uncheckedBoxColor,
            uncheckedCheckmarkColor = uncheckedCheckmarkColor,
            disabledCheckedBoxColor =
            checkedBoxColor.toDisabledColor(),
            disabledCheckedCheckmarkColor =
            checkedCheckmarkColor.toDisabledColor(),
            disabledUncheckedBoxColor =
            uncheckedBoxColor.toDisabledColor(),
            disabledUncheckedCheckmarkColor =
            uncheckedCheckmarkColor.toDisabledColor(),
        )
    }
}

/**
 * Contains the default values used by [Switch].
 */
public object SwitchDefaults {
    /**
     * Creates a [SwitchColors] for use in a [Switch].
     *
     * @param checkedThumbColor The thumb color of this [Switch] when enabled and checked.
     * @param checkedTrackColor The track color of this [Switch] when enabled and checked.
     * @param uncheckedThumbColor The thumb color of this [Switch] when enabled and unchecked.
     * @param uncheckedTrackColor The track color of this [Switch] when enabled and unchecked.
     */
    @Composable
    public fun colors(
        checkedThumbColor: Color = MaterialTheme.colors.secondary,
        checkedTrackColor: Color = checkedThumbColor.copy(alpha = ContentAlpha.disabled),
        uncheckedThumbColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        uncheckedTrackColor: Color =
            uncheckedThumbColor.copy(alpha = uncheckedThumbColor.alpha * ContentAlpha.disabled),
    ): SwitchColors {
        return DefaultSwitchColors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            disabledCheckedThumbColor = checkedThumbColor.toDisabledColor(),
            disabledCheckedTrackColor = checkedTrackColor.toDisabledColor(
                disabledContentAlpha = checkedTrackColor.alpha * ContentAlpha.disabled),
            disabledUncheckedThumbColor =
            uncheckedThumbColor.toDisabledColor(
                disabledContentAlpha = uncheckedThumbColor.alpha * ContentAlpha.disabled),
            disabledUncheckedTrackColor =
            uncheckedTrackColor.toDisabledColor(
                disabledContentAlpha = uncheckedTrackColor.alpha * ContentAlpha.disabled),
        )
    }
}

/**
 * Contains the default values used by [RadioButton].
 */
public object RadioButtonDefaults {
    /**
     * Creates a [RadioButtonColors] for use in a [RadioButton].
     *
     * @param selectedRingColor The outer ring color of this [RadioButton] when enabled
     * and selected.
     * @param selectedDotColor The inner dot color of this [RadioButton] when enabled
     * and selected.
     * @param unselectedRingColor The outer ring color of this [RadioButton] when enabled
     * and unselected.
     * @param unselectedDotColor The inner dot color of this [RadioButton] when enabled
     * and unselected.
     */
    @Composable
    public fun colors(
        selectedRingColor: Color = MaterialTheme.colors.secondary,
        selectedDotColor: Color = MaterialTheme.colors.secondary,
        unselectedRingColor: Color = contentColorFor(
            MaterialTheme.colors.primary.copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colors.surface)
        ),
        unselectedDotColor: Color = contentColorFor(
            MaterialTheme.colors.primary.copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colors.surface)
        ),
    ): RadioButtonColors {
        return DefaultRadioButtonColors(
            selectedRingColor = selectedRingColor,
            selectedDotColor = selectedDotColor,
            unselectedRingColor = unselectedRingColor,
            unselectedDotColor = unselectedDotColor,
            disabledSelectedRingColor = selectedRingColor.toDisabledColor(),
            disabledSelectedDotColor = selectedDotColor.toDisabledColor(),
            disabledUnselectedRingColor = unselectedRingColor.toDisabledColor(),
            disabledUnselectedDotColor = unselectedDotColor.toDisabledColor(),
        )
    }
}

private fun DrawScope.drawBox(color: Color) {
    val topCornerPx = BOX_CORNER.toPx()
    val strokeWidthPx = BOX_STROKE.toPx()
    val halfStrokeWidthPx = strokeWidthPx / 2.0f
    val radiusPx = BOX_RADIUS.toPx()
    val checkboxSizePx = BOX_SIZE.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(topCornerPx + halfStrokeWidthPx, topCornerPx + halfStrokeWidthPx),
        size = Size(checkboxSizePx - strokeWidthPx, checkboxSizePx - strokeWidthPx),
        cornerRadius = CornerRadius(radiusPx - halfStrokeWidthPx),
        style = Stroke(strokeWidthPx)
    )
}

private fun DrawScope.drawThumb(
    color: Color,
    progress: Float,
    isRtl: Boolean
) {

    val switchThumbRadiusPx = SWITCH_THUMB_RADIUS.toPx()
    val switchTrackLengthPx = SWITCH_TRACK_LENGTH.toPx()

    val thumbProgressPx =
        lerp(
            start = if (isRtl) switchTrackLengthPx - switchThumbRadiusPx else switchThumbRadiusPx,
            stop = if (isRtl) switchThumbRadiusPx else switchTrackLengthPx - switchThumbRadiusPx,
            fraction = progress
        )

    drawCircle(
        color = color,
        radius = switchThumbRadiusPx,
        center = Offset(thumbProgressPx, center.y),
        blendMode = BlendMode.Src
    )
}

/**
 * Default [CheckboxColors] implementation.
 */
@Immutable
private class DefaultCheckboxColors(
    private val checkedBoxColor: Color,
    private val checkedCheckmarkColor: Color,
    private val uncheckedCheckmarkColor: Color,
    private val uncheckedBoxColor: Color,
    private val disabledCheckedBoxColor: Color,
    private val disabledCheckedCheckmarkColor: Color,
    private val disabledUncheckedBoxColor: Color,
    private val disabledUncheckedCheckmarkColor: Color,
) : CheckboxColors {
    @Composable
    override fun boxColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedBoxColor,
            uncheckedColor = uncheckedBoxColor,
            disabledCheckedColor = disabledCheckedBoxColor,
            disabledUncheckedColor = disabledUncheckedBoxColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    override fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color> =
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
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultCheckboxColors

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
 * Default [SwitchColors] implementation.
 */
@Immutable
private class DefaultSwitchColors(
    private val checkedThumbColor: Color,
    private val checkedTrackColor: Color,
    private val uncheckedThumbColor: Color,
    private val uncheckedTrackColor: Color,
    private val disabledCheckedThumbColor: Color,
    private val disabledCheckedTrackColor: Color,
    private val disabledUncheckedThumbColor: Color,
    private val disabledUncheckedTrackColor: Color,
) : SwitchColors {
    @Composable
    override fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> =
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
    override fun trackColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackColor,
            uncheckedColor = uncheckedTrackColor,
            disabledCheckedColor = disabledCheckedTrackColor,
            disabledUncheckedColor = disabledUncheckedTrackColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultSwitchColors

        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackColor != other.disabledUncheckedTrackColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedThumbColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackColor.hashCode()
        return result
    }
}

/**
 * Default [SwitchColors] implementation.
 */
@Immutable
private class DefaultRadioButtonColors(
    private val selectedRingColor: Color,
    private val selectedDotColor: Color,
    private val unselectedRingColor: Color,
    private val unselectedDotColor: Color,
    private val disabledSelectedRingColor: Color,
    private val disabledSelectedDotColor: Color,
    private val disabledUnselectedRingColor: Color,
    private val disabledUnselectedDotColor: Color,
) : RadioButtonColors {
    @Composable
    override fun ringColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedRingColor,
            uncheckedColor = unselectedRingColor,
            disabledCheckedColor = disabledSelectedRingColor,
            disabledUncheckedColor = disabledUnselectedRingColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    override fun dotColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedDotColor,
            uncheckedColor = unselectedDotColor,
            disabledCheckedColor = disabledSelectedDotColor,
            disabledUncheckedColor = disabledUnselectedDotColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultRadioButtonColors

        if (selectedRingColor != other.selectedRingColor) return false
        if (selectedDotColor != other.selectedDotColor) return false
        if (unselectedRingColor != other.unselectedRingColor) return false
        if (unselectedDotColor != other.unselectedDotColor) return false
        if (disabledSelectedRingColor != other.disabledSelectedRingColor) return false
        if (disabledSelectedDotColor != other.disabledSelectedDotColor) return false
        if (disabledUnselectedRingColor != other.disabledUnselectedRingColor) return false
        if (disabledUnselectedDotColor != other.disabledUnselectedDotColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedRingColor.hashCode()
        result = 31 * result + selectedDotColor.hashCode()
        result = 31 * result + unselectedRingColor.hashCode()
        result = 31 * result + unselectedDotColor.hashCode()
        result = 31 * result + disabledSelectedRingColor.hashCode()
        result = 31 * result + disabledSelectedDotColor.hashCode()
        result = 31 * result + disabledUnselectedRingColor.hashCode()
        result = 31 * result + disabledUnselectedDotColor.hashCode()
        return result
    }
}

private val BOX_CORNER = 3.dp
private val BOX_STROKE = 2.dp
private val BOX_RADIUS = 2.dp
private val BOX_SIZE = 18.dp

private val SWITCH_TRACK_LENGTH = 24.dp
private val SWITCH_TRACK_HEIGHT = 10.dp
private val SWITCH_THUMB_RADIUS = 7.dp

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> = tween(QUICK, 0, STANDARD_IN)
private val PROGRESS_ANIMATION_SPEC: TweenSpec<Float> = tween(QUICK, 0, STANDARD_IN)

private val WIDTH = 24.dp
private val HEIGHT = 24.dp
