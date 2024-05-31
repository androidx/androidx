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
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * [RadioButton] provides an animated radio selection control for use in [SelectableButton] or
 * [SplitSelectableButton].
 *
 * RadioButton sample:
 *
 * @sample androidx.wear.compose.material3.samples.SelectableButtonSample
 *
 * @param modifier Modifier to be applied to the radio button control. This can be used to provide a
 *   content description for accessibility.
 * @param colors [RadioButtonColors] from which the [RadioButton] control colors will be obtained.
 */
@Composable
fun SelectionControlScope.RadioButton(
    modifier: Modifier = Modifier,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
) =
    androidx.wear.compose.materialcore.RadioButton(
        modifier = modifier,
        selected = isSelected,
        enabled = isEnabled,
        ringColor = { isEnabled, isSelected ->
            colors.color(enabled = isEnabled, selected = isSelected)
        },
        dotColor = { isEnabled, isSelected ->
            colors.color(enabled = isEnabled, selected = isSelected)
        },
        onClick = null,
        interactionSource = null,
        dotRadiusProgressDuration = { isSelected ->
            if (isSelected) MotionTokens.DurationMedium1 else MotionTokens.DurationShort3
        },
        dotAlphaProgressDuration = MotionTokens.DurationShort3,
        dotAlphaProgressDelay = MotionTokens.DurationShort2,
        easing = MotionTokens.EasingStandardDecelerate,
        width = WIDTH,
        height = HEIGHT,
        ripple = rippleOrFallbackImplementation()
    )

/**
 * Represents the content colors used in the [RadioButton] selection control in different states.
 *
 * @param selectedColor The color of the radio control when enabled and selected.
 * @param unselectedColor The color of the radio control when enabled and unselected.
 * @param disabledSelectedColor The color of the radio control when disabled and selected.
 * @param disabledUnselectedColor The color of the radio control when disabled and unselected.
 */
@Immutable
class RadioButtonColors(
    val selectedColor: Color,
    val unselectedColor: Color,
    val disabledSelectedColor: Color,
    val disabledUnselectedColor: Color
) {
    @Composable
    internal fun color(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
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

/** Contains the default values used by the [RadioButton] selection control. */
object RadioButtonDefaults {
    /**
     * Creates a [RadioButtonColors] for use in a [RadioButton] selection control.
     *
     * @param selectedColor The color of the radio control when enabled and selected.
     * @param unselectedColor The color of the radio control when enabled and unselected.
     * @param disabledSelectedColor The color of the radio control when selected and disabled
     * @param disabledUnselectedColor The color of the radio control unselected and disabled
     */
    @Composable
    fun colors(
        selectedColor: Color = MaterialTheme.colorScheme.primary,
        unselectedColor: Color = MaterialTheme.colorScheme.outline,
        disabledSelectedColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
        disabledUnselectedColor: Color =
            MaterialTheme.colorScheme.onSurface.toDisabledColor(disabledAlpha = 0.12f),
    ): RadioButtonColors {
        return RadioButtonColors(
            selectedColor = selectedColor,
            unselectedColor = unselectedColor,
            disabledSelectedColor = disabledSelectedColor,
            disabledUnselectedColor = disabledUnselectedColor
        )
    }
}

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)

private val WIDTH = 32.dp
private val HEIGHT = 24.dp
