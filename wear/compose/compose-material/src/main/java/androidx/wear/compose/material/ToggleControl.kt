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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
) {
    val targetState = if (checked) ToggleStage.Checked else ToggleStage.Unchecked
    val transition = updateTransition(targetState)
    val tickProgress = animateProgress(transition, "Checkbox")

    // For Checkbox, the color and alpha animations have the same duration and easing,
    // so we don't need to explicitly animate alpha.
    val boxColor = colors.boxColor(enabled = enabled, checked = checked)
    val checkColor = colors.checkmarkColor(enabled = enabled, checked = checked)

    Canvas(
        modifier = modifier.maybeToggleable(
            onCheckedChange, enabled, checked, interactionSource, rememberRipple(),
            Role.Checkbox
        )
    ) {
        drawBox(color = boxColor.value)

        if (targetState == ToggleStage.Checked) {
            drawTick(
                tickProgress = tickProgress.value,
                tickColor = checkColor.value,
            )
        } else {
            eraseTick(
                tickProgress = tickProgress.value,
                tickColor = checkColor.value,
            )
        }
    }
}

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
) {
    val targetState = if (checked) ToggleStage.Checked else ToggleStage.Unchecked
    val transition = updateTransition(targetState)

    // For Switch, the color and alpha animations have the same duration and easing,
    // so we don't need to explicitly animate alpha.
    val thumbProgress = animateProgress(transition, "Switch")
    val thumbColor = colors.thumbColor(enabled = enabled, checked = checked)
    val trackColor = colors.trackColor(enabled = enabled, checked = checked)

    Canvas(
        modifier = modifier.maybeToggleable(
            onCheckedChange, enabled, checked, interactionSource, rememberRipple(), Role.Switch
        )
    ) {
        val switchTrackLengthPx = SWITCH_TRACK_LENGTH.toPx()
        val switchTrackHeightPx = SWITCH_TRACK_HEIGHT.toPx()
        val switchThumbRadiusPx = SWITCH_THUMB_RADIUS.toPx()

        val thumbProgressPx = lerp(
            start = switchThumbRadiusPx,
            stop = switchTrackLengthPx - switchThumbRadiusPx,
            fraction = thumbProgress.value
        )
        drawTrack(trackColor.value, switchTrackLengthPx, switchTrackHeightPx)
        // Use BlendMode.Src to overwrite overlapping pixels with the thumb color
        // (by default, the track shows through any transparency).
        drawCircle(
            color = thumbColor.value,
            radius = switchThumbRadiusPx,
            center = Offset(thumbProgressPx, center.y),
            blendMode = BlendMode.Src)
    }
}

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
) {
    val targetState = if (selected) ToggleStage.Checked else ToggleStage.Unchecked
    val transition = updateTransition(targetState)

    val circleColor = colors.ringColor(enabled = enabled, selected = selected)
    val dotRadiusProgress = animateProgress(
        transition,
        durationMillis = if (selected) QUICK else STANDARD,
        label = "dot-radius"
    )
    val dotColor = colors.dotColor(enabled = enabled, selected = selected)
    // Animation of the dot alpha only happens when toggling On to Off.
    val dotAlphaProgress =
        if (targetState == ToggleStage.Unchecked)
            animateProgress(
                transition, durationMillis = RAPID, delayMillis = FLASH, label = "dot-alpha"
            )
        else
            null

    Canvas(
        modifier = modifier.maybeSelectable(
            onClick, enabled, selected, interactionSource, rememberRipple()
        )
    ) {
        // Outer circle has a constant radius.
        drawCircle(
            radius = RADIO_CIRCLE_RADIUS.toPx(),
            color = circleColor.value,
            center = center,
            style = Stroke(RADIO_CIRCLE_STROKE.toPx()),
        )
        // Inner dot radius expands/shrinks.
        drawCircle(
            radius = dotRadiusProgress.value * RADIO_DOT_RADIUS.toPx(),
            color = dotColor.value.copy(
                alpha = (dotAlphaProgress?.value ?: 1f) * dotColor.value.alpha
            ),
            center = center,
            style = Fill,
        )
    }
}

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
                checkedBoxColor.copy(alpha = ContentAlpha.disabled),
            disabledCheckedCheckmarkColor =
                checkedCheckmarkColor.copy(alpha = ContentAlpha.disabled),
            disabledUncheckedBoxColor =
                uncheckedBoxColor.copy(alpha = ContentAlpha.disabled),
            disabledUncheckedCheckmarkColor =
                uncheckedCheckmarkColor.copy(alpha = ContentAlpha.disabled),
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
            disabledCheckedThumbColor = checkedThumbColor.copy(alpha = ContentAlpha.disabled),
            disabledCheckedTrackColor = checkedTrackColor.copy(
                alpha = checkedTrackColor.alpha * ContentAlpha.disabled
            ),
            disabledUncheckedThumbColor =
                uncheckedThumbColor.copy(alpha = uncheckedThumbColor.alpha * ContentAlpha.disabled),
            disabledUncheckedTrackColor =
                uncheckedTrackColor.copy(alpha = uncheckedTrackColor.alpha * ContentAlpha.disabled),
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
            disabledSelectedRingColor = selectedRingColor.copy(alpha = ContentAlpha.disabled),
            disabledSelectedDotColor = selectedDotColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedRingColor =
                unselectedRingColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedDotColor = unselectedDotColor.copy(alpha = ContentAlpha.disabled),
        )
    }
}

@Composable
private fun animateProgress(
    transition: Transition<ToggleStage>,
    label: String,
    durationMillis: Int = QUICK,
    delayMillis: Int = 0,
) =
    transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = STANDARD_IN)
        },
        label = label
    ) {
        // Return the tick progress as a Float in Px.
        when (it) {
            ToggleStage.Unchecked -> 0f
            ToggleStage.Checked -> 1f
        }
    }

private fun Modifier.maybeToggleable(
    onCheckedChange: ((Boolean) -> Unit)?,
    enabled: Boolean,
    checked: Boolean,
    interactionSource: MutableInteractionSource,
    indication: Indication,
    role: Role,
): Modifier {
    val standardModifier = this
        .wrapContentSize(Alignment.Center)
        .requiredSize(24.dp)

    return if (onCheckedChange == null) {
        standardModifier
    } else {
        standardModifier.then(
            Modifier.toggleable(
                enabled = enabled,
                value = checked,
                onValueChange = onCheckedChange,
                role = role,
                indication = indication,
                interactionSource = interactionSource
            )
        )
    }
}

private fun Modifier.maybeSelectable(
    onClick: (() -> Unit)?,
    enabled: Boolean,
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    indication: Indication,
): Modifier {
    val standardModifier = this
        .wrapContentSize(Alignment.Center)
        .requiredSize(24.dp)

    return if (onClick == null) {
        standardModifier
    } else {
        standardModifier.then(
            Modifier.selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = indication,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
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

private fun DrawScope.drawTick(tickColor: Color, tickProgress: Float) {
    // Using tickProgress animating from zero to TICK_TOTAL_LENGTH,
    // rotate the tick as we draw from 15 degrees to zero.
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength
    val center = Offset(12.dp.toPx(), 12.dp.toPx())
    val angle = TICK_ROTATION - TICK_ROTATION / tickTotalLength * tickProgressPx
    val angleRadians = angle.toRadians()

    // Animate the base of the tick.
    val baseStart = Offset(6.7f.dp.toPx(), 12.3f.dp.toPx())
    val tickBaseProgress = min(tickProgressPx, tickBaseLength)

    val path = Path()
    path.moveTo(baseStart.rotate(angleRadians, center))
    path.lineTo((baseStart + Offset(tickBaseProgress, tickBaseProgress))
        .rotate(angleRadians, center))

    if (tickProgressPx > tickBaseLength) {
        val tickStickProgress = min(tickProgressPx - tickBaseLength, tickStickLength)
        val stickStart = Offset(9.3f.dp.toPx(), 16.3f.dp.toPx())
        // Move back to the start of the stick (without drawing)
        path.moveTo(stickStart.rotate(angleRadians, center))
        path.lineTo(
            Offset(stickStart.x + tickStickProgress, stickStart.y - tickStickProgress)
                .rotate(angleRadians, center))
    }
    // Use StrokeCap.Butt because Square adds an extension on the end of each line.
    drawPath(path, tickColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt))
}

private fun DrawScope.eraseTick(tickColor: Color, tickProgress: Float) {
    val tickBaseLength = TICK_BASE_LENGTH.toPx()
    val tickStickLength = TICK_STICK_LENGTH.toPx()
    val tickTotalLength = tickBaseLength + tickStickLength
    val tickProgressPx = tickProgress * tickTotalLength

    // Animate the stick of the tick, drawing down the stick from the top.
    val stickStartX = 17.3f.dp.toPx()
    val stickStartY = 8.3f.dp.toPx()
    val tickStickProgress = min(tickProgressPx, tickStickLength)

    val path = Path()
    path.moveTo(stickStartX, stickStartY)
    path.lineTo(stickStartX - tickStickProgress, stickStartY + tickStickProgress)

    if (tickStickProgress > tickStickLength) {
        // Animate the base of the tick, drawing up the base from bottom of the stick.
        val tickBaseProgress = min(tickProgressPx - tickStickLength, tickBaseLength)
        val baseStartX = 10.7f.dp.toPx()
        val baseStartY = 16.3f.dp.toPx()
        path.moveTo(baseStartX, baseStartY)
        path.lineTo(baseStartX - tickBaseProgress, baseStartY - tickBaseProgress)
    }

    drawPath(path, tickColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt))
}

private fun DrawScope.drawTrack(
    color: Color,
    switchTrackLengthPx: Float,
    switchTrackHeightPx: Float,
) {
    val path = Path()
    val strokeRadius = switchTrackHeightPx / 2f
    path.moveTo(Offset(strokeRadius, center.y))
    path.lineTo(Offset(switchTrackLengthPx - strokeRadius, center.y))
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = switchTrackHeightPx, cap = StrokeCap.Round)
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
    override fun boxColor(enabled: Boolean, checked: Boolean): State<Color> = animateColorAsState(
        targetValue = toggleControlColor(
            enabled,
            checked,
            checkedBoxColor,
            uncheckedBoxColor,
            disabledCheckedBoxColor,
            disabledUncheckedBoxColor
        ),
        animationSpec = COLOR_ANIMATION_SPEC
    )

    @Composable
    override fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateColorAsState(
            targetValue = toggleControlColor(
                enabled,
                checked,
                checkedCheckmarkColor,
                uncheckedCheckmarkColor,
                disabledCheckedCheckmarkColor,
                disabledUncheckedCheckmarkColor
            ),
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
    override fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> = animateColorAsState(
        targetValue = toggleControlColor(
            enabled,
            checked,
            checkedThumbColor,
            uncheckedThumbColor,
            disabledCheckedThumbColor,
            disabledUncheckedThumbColor
        ),
        animationSpec = COLOR_ANIMATION_SPEC
    )

    @Composable
    override fun trackColor(enabled: Boolean, checked: Boolean): State<Color> = animateColorAsState(
        targetValue = toggleControlColor(
            enabled,
            checked,
            checkedTrackColor,
            uncheckedTrackColor,
            disabledCheckedTrackColor,
            disabledUncheckedTrackColor
        ),
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
        animateColorAsState(
            targetValue = toggleControlColor(
                enabled,
                selected,
                selectedRingColor,
                unselectedRingColor,
                disabledSelectedRingColor,
                disabledUnselectedRingColor
            ),
            animationSpec = COLOR_ANIMATION_SPEC
        )

    @Composable
    override fun dotColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateColorAsState(
            targetValue = toggleControlColor(
                enabled,
                selected,
                selectedDotColor,
                unselectedDotColor,
                disabledSelectedDotColor,
                disabledUnselectedDotColor
            ),
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

private fun directionVector(angleRadians: Float) = Offset(cos(angleRadians), sin(angleRadians))

private fun Offset.rotate90() = Offset(-y, x)

// This is duplicated from wear.compose.foundation/geometry.kt
// Any changes should be replicated there.
private fun Float.toRadians() = this * PI.toFloat() / 180f

private enum class ToggleStage {
    Unchecked, Checked
}

private fun toggleControlColor(
    enabled: Boolean,
    checked: Boolean,
    checkedColor: Color,
    uncheckedColor: Color,
    enabledCheckedColor: Color,
    disabledCheckedColor: Color
) =
    if (enabled) {
        if (checked) checkedColor else uncheckedColor
    } else {
        if (checked) enabledCheckedColor else disabledCheckedColor
    }

private val BOX_CORNER = 3.dp
private val BOX_STROKE = 2.dp
private val BOX_RADIUS = 2.dp
private val BOX_SIZE = 18.dp

private val TICK_BASE_LENGTH = 4.dp
private val TICK_STICK_LENGTH = 8.dp
private const val TICK_ROTATION = 15f

private val SWITCH_TRACK_LENGTH = 24.dp
private val SWITCH_TRACK_HEIGHT = 10.dp
private val SWITCH_THUMB_RADIUS = 7.dp

private val RADIO_CIRCLE_RADIUS = 9.dp
private val RADIO_CIRCLE_STROKE = 2.dp
private val RADIO_DOT_RADIUS = 5.dp

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> = tween(QUICK, 0, STANDARD_IN)
