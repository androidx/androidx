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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.materialcore.SelectionStage
import androidx.wear.compose.materialcore.animateSelectionColor
import androidx.wear.compose.materialcore.animateTick
import androidx.wear.compose.materialcore.isLayoutDirectionRtl

/**
 * [Checkbox] provides an animated checkbox for use as a toggle control in
 * [ToggleButton] or [SplitToggleButton].
 *
 * Checkbox sample:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithCheckbox
 *
 * @param modifier Modifier to be applied to the checkbox. This can be used to provide a
 * content description for accessibility.
 * @param colors [CheckboxColors] from which the box and checkmark colors will be obtained.
 */
@Composable
fun ToggleControlScope.Checkbox(
    modifier: Modifier = Modifier,
    colors: CheckboxColors = CheckboxDefaults.colors(),
) = androidx.wear.compose.materialcore.Checkbox(
    checked = isChecked,
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
    enabled = isEnabled,
    onCheckedChange = null,
    interactionSource = null,
    drawBox = { drawScope, color, progress, isRtl ->
        drawScope.drawBox(
            color = color,
            progress = progress,
            isRtl = isRtl
        )
    },
    progressAnimationSpec = PROGRESS_ANIMATION_SPEC,
    width = WIDTH,
    height = CHECKBOX_HEIGHT,
    ripple = rippleOrFallbackImplementation()
)

/**
 * [Switch] provides an animated switch for use as a toggle control in
 * [ToggleButton] or [SplitToggleButton].
 *
 * Switch samples:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithSwitch
 *
 * @param modifier Modifier to be applied to the switch. This can be used to provide a
 * content description for accessibility.
 * @param colors [SwitchColors] from which the colors of the thumb and track will be obtained.
 */
@Composable
fun ToggleControlScope.Switch(
    modifier: Modifier = Modifier,
    colors: SwitchColors = SwitchDefaults.colors(),
) {
    val isRtl = isLayoutDirectionRtl()
    val targetState = if (isChecked) SelectionStage.Checked else SelectionStage.Unchecked
    val transition = updateTransition(targetState, label = "switchTransition")
    val thumbProgress = transition.animateFloat(
        transitionSpec = { SWITCH_PROGRESS_ANIMATION_SPEC }, label = "switchTransition"
    ) {
        when (it) {
            SelectionStage.Unchecked -> 0f
            SelectionStage.Checked -> 1f
        }
    }
    val thumbColor = colors.thumbColor(enabled = isEnabled, checked = isChecked).value
    val thumbIconColor = colors.thumbIconColor(enabled = isEnabled, checked = isChecked).value
    val trackColor = colors.trackColor(enabled = isEnabled, checked = isChecked).value
    val trackBorderColor = colors.trackBorderColor(enabled = isEnabled, checked = isChecked).value
    Box(
        modifier = modifier
            .semantics { this.role = Role.Switch }
            .height(SWITCH_HEIGHT)
            .width(WIDTH)
            .border(
                width = SWITCH_TRACK_WIDTH,
                shape = CircleShape,
                color = if (trackColor == trackBorderColor) Color.Transparent else trackBorderColor
            )
            .background(
                color = trackColor,
                shape = CircleShape
            )
            .drawBehind {
                drawThumbAndTick(
                    isEnabled,
                    isChecked,
                    thumbColor,
                    thumbProgress.value,
                    thumbIconColor,
                    isRtl
                )
            }
            .wrapContentSize(Alignment.CenterEnd)
    )
}

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
    internal fun trackBorderColor(enabled: Boolean, checked: Boolean): State<Color> =
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
     * @param disabledCheckedBoxColor The box color of this [Checkbox] when disabled and checked
     * @param disabledCheckedCheckmarkColor The check mark color of this [Checkbox] when disabled
     * and checked
     * @param disabledUncheckedBoxColor The box color of this [Checkbox] when disabled and
     * unchecked
     * @param disabledUncheckedCheckmarkColor The check mark color of this [Checkbox] when disabled
     * and unchecked
     */
    @Composable
    fun colors(
        checkedBoxColor: Color = MaterialTheme.colorScheme.primary,
        checkedCheckmarkColor: Color = MaterialTheme.colorScheme.onPrimary,
        uncheckedBoxColor: Color = MaterialTheme.colorScheme.outline,
        uncheckedCheckmarkColor: Color = Color.Transparent,
        disabledCheckedBoxColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledCheckedCheckmarkColor: Color =
            MaterialTheme.colorScheme.background.toDisabledColor(),
        disabledUncheckedBoxColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledUncheckedCheckmarkColor: Color = Color.Transparent
    ): CheckboxColors = CheckboxColors(
        checkedBoxColor = checkedBoxColor,
        checkedCheckmarkColor = checkedCheckmarkColor,
        uncheckedBoxColor = uncheckedBoxColor,
        uncheckedCheckmarkColor = uncheckedCheckmarkColor,
        disabledCheckedBoxColor = disabledCheckedBoxColor,
        disabledCheckedCheckmarkColor = disabledCheckedCheckmarkColor,
        disabledUncheckedBoxColor = disabledUncheckedBoxColor,
        disabledUncheckedCheckmarkColor = disabledUncheckedCheckmarkColor
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
     * @param checkedTrackBorderColor The border color of this [Switch] when enabled and checked.
     * @param uncheckedThumbColor The thumb color of this [Switch] when enabled and unchecked.
     * @param uncheckedThumbIconColor The thumb icon color of this [Switch] when enabled and
     * checked.
     * @param uncheckedTrackColor The track color of this [Switch] when enabled and unchecked.
     * @param uncheckedTrackBorderColor The border color of this [Switch] when enabled and
     * unchecked.
     * @param disabledCheckedThumbColor The thumb color of this [Switch] when disabled and checked.
     * @param disabledCheckedThumbIconColor The thumb icon color of this [Switch] when disabled
     * and checked.
     * @param disabledCheckedTrackColor The track color of this [Switch] when disabled and checked.
     * @param disabledCheckedTrackBorderColor The border color of this [Switch] when disabled and
     * unchecked.
     * @param disabledUncheckedThumbColor The thumb color of this [Switch] when disabled and
     * unchecked.
     * @param disabledUncheckedThumbIconColor The thumb icon color of this [Switch] when disabled
     * and unchecked.
     * @param disabledUncheckedTrackColor The track color of this [Switch] when disabled and
     * unchecked.
     * @param disabledUncheckedTrackBorderColor The border color of this [Switch] when disabled
     * and unchecked.
     */
    @Composable
    fun colors(
        checkedThumbColor: Color = MaterialTheme.colorScheme.onPrimary,
        checkedThumbIconColor: Color = MaterialTheme.colorScheme.primary,
        checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
        checkedTrackBorderColor: Color = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor: Color = MaterialTheme.colorScheme.outline,
        uncheckedThumbIconColor: Color = Color.Transparent,
        uncheckedTrackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        uncheckedTrackBorderColor: Color = MaterialTheme.colorScheme.outline,
        disabledCheckedThumbColor: Color = MaterialTheme.colorScheme.background.toDisabledColor(),
        disabledCheckedThumbIconColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledCheckedTrackColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledCheckedTrackBorderColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledUncheckedThumbColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledUncheckedThumbIconColor: Color = Color.Transparent,
        disabledUncheckedTrackColor: Color = Color.Transparent,
        disabledUncheckedTrackBorderColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f)
    ): SwitchColors = SwitchColors(
        checkedThumbColor = checkedThumbColor,
        checkedThumbIconColor = checkedThumbIconColor,
        checkedTrackColor = checkedTrackColor,
        checkedTrackBorderColor = checkedTrackBorderColor,
        uncheckedThumbColor = uncheckedThumbColor,
        uncheckedThumbIconColor = uncheckedThumbIconColor,
        uncheckedTrackColor = uncheckedTrackColor,
        uncheckedTrackBorderColor = uncheckedTrackBorderColor,
        disabledCheckedThumbColor = disabledCheckedThumbColor,
        disabledCheckedThumbIconColor = disabledCheckedThumbIconColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledCheckedTrackBorderColor = disabledCheckedTrackBorderColor,
        disabledUncheckedThumbColor = disabledUncheckedThumbColor,
        disabledUncheckedThumbIconColor = disabledUncheckedThumbIconColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        disabledUncheckedTrackBorderColor = disabledUncheckedTrackBorderColor
    )
}

private fun DrawScope.drawBox(color: Color, progress: Float, isRtl: Boolean) {
    // Centering vertically.
    val topCornerPx = (CHECKBOX_HEIGHT - BOX_SIZE).toPx() / 2
    val strokeWidthPx = BOX_STROKE.toPx()
    val halfStrokeWidthPx = strokeWidthPx / 2.0f
    val radiusPx = BOX_RADIUS.toPx()
    val checkboxSizePx = BOX_SIZE.toPx()
    // Aligning the box to the end.
    val startXOffsetPx = if (isRtl) 0f else (WIDTH - CHECKBOX_HEIGHT).toPx()

    // Draw the outline of the box.
    drawRoundRect(
        color,
        topLeft = Offset(
            topCornerPx + halfStrokeWidthPx + startXOffsetPx, topCornerPx + halfStrokeWidthPx
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

private fun DrawScope.drawThumbAndTick(
    enabled: Boolean,
    checked: Boolean,
    thumbColor: Color,
    progress: Float,
    thumbIconColor: Color,
    isRtl: Boolean
) {

    val thumbPaddingUnchecked = SWITCH_HEIGHT / 2 - THUMB_RADIUS_UNCHECKED
    val thumbPaddingChecked = SWITCH_HEIGHT / 2 - THUMB_RADIUS_CHECKED

    val switchThumbRadiusPx = lerp(
        start = THUMB_RADIUS_UNCHECKED.toPx(),
        stop = THUMB_RADIUS_CHECKED.toPx(),
        fraction = progress
    )

    val switchTrackLengthPx = WIDTH.toPx()

    // For Rtl mode the thumb progress will start from the end of the switch.
    val thumbProgressPx = if (isRtl) lerp(
        start = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingUnchecked.toPx(),
        stop = switchThumbRadiusPx + thumbPaddingChecked.toPx(),
        fraction = progress
    )
    else lerp(
        start = switchThumbRadiusPx + thumbPaddingUnchecked.toPx(),
        stop = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingChecked.toPx(),
        fraction = progress
    )

    drawCircle(
        color = thumbColor,
        radius = switchThumbRadiusPx,
        center = Offset(thumbProgressPx, center.y)
    )

    val ltrAdditionalOffset = 5.dp.toPx()
    val rtlAdditionalOffset = 6.dp.toPx()

    val totalDist = switchTrackLengthPx - 2 * switchThumbRadiusPx - ltrAdditionalOffset

    // Offset value to be added if RTL mode is enabled.
    // We need to move the tick to the checked position in ltr mode when unchecked.
    val rtlOffset = switchTrackLengthPx - 2 * THUMB_RADIUS_CHECKED.toPx() - rtlAdditionalOffset

    val distMoved = if (isRtl) rtlOffset - progress * totalDist else progress * totalDist

    // Draw tick icon
    animateTick(
        enabled = enabled,
        checked = checked,
        tickColor = thumbIconColor,
        tickProgress = progress,
        startXOffset = distMoved.toDp()
    )
}

private val BOX_STROKE = 2.dp
private val BOX_RADIUS = 2.dp
private val BOX_SIZE = 18.dp

private val THUMB_RADIUS_UNCHECKED = 6.dp
private val THUMB_RADIUS_CHECKED = 9.dp
private val SWITCH_HEIGHT = 22.dp
private val CHECKBOX_HEIGHT = 24.dp
private val WIDTH = 32.dp
private val SWITCH_TRACK_WIDTH = 2.dp

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)
private val PROGRESS_ANIMATION_SPEC: TweenSpec<Float> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)
private val SWITCH_PROGRESS_ANIMATION_SPEC: TweenSpec<Float> =
    tween(MotionTokens.DurationMedium2, 0, MotionTokens.EasingStandardDecelerate)
