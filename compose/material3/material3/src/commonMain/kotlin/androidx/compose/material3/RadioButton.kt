/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.RadioButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/radio-button/overview" class="external"
 * target="_blank">Material Design radio button</a>.
 *
 * Radio buttons allow users to select one option from a set.
 *
 * ![Radio button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/radio-button.png)
 *
 * @sample androidx.compose.material3.samples.RadioButtonSample
 *
 * [RadioButton]s can be combined together with [Text] in the desired layout (e.g. [Column] or
 * [Row]) to achieve radio group-like behaviour, where the entire layout is selectable:
 *
 * @sample androidx.compose.material3.samples.RadioGroupSample
 * @param selected whether this radio button is selected or not
 * @param onClick called when this radio button is clicked. If `null`, then this radio button will
 *   not be interactable, unless something else handles its input events and updates its state.
 * @param modifier the [Modifier] to be applied to this radio button
 * @param enabled controls the enabled state of this radio button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [RadioButtonColors] that will be used to resolve the color used for this radio
 *   button in different states. See [RadioButtonDefaults.colors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this radio button. You can use this to change the radio button's
 *   appearance or preview the radio button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null
) {
    val dotRadius =
        animateDpAsState(
            targetValue = if (selected) RadioButtonDotSize / 2 else 0.dp,
            // TODO Load the motionScheme tokens from the component tokens file
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value()
        )
    val radioColor = colors.radioColor(enabled, selected)
    val selectableModifier =
        if (onClick != null) {
            Modifier.selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = RadioButtonTokens.StateLayerSize / 2)
            )
        } else {
            Modifier
        }
    Canvas(
        modifier
            .then(
                if (onClick != null) {
                    Modifier.minimumInteractiveComponentSize()
                } else {
                    Modifier
                }
            )
            .then(selectableModifier)
            .wrapContentSize(Alignment.Center)
            .padding(RadioButtonPadding)
            .requiredSize(RadioButtonTokens.IconSize)
    ) {
        // Draw the radio button
        val strokeWidth = RadioStrokeWidth.toPx()
        drawCircle(
            radioColor.value,
            radius = (RadioButtonTokens.IconSize / 2).toPx() - strokeWidth / 2,
            style = Stroke(strokeWidth)
        )
        if (dotRadius.value > 0.dp) {
            drawCircle(radioColor.value, dotRadius.value.toPx() - strokeWidth / 2, style = Fill)
        }
    }
}

/** Defaults used in [RadioButton]. */
object RadioButtonDefaults {

    /**
     * Creates a [RadioButtonColors] that will animate between the provided colors according to the
     * Material specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultRadioButtonColors

    /**
     * Creates a [RadioButtonColors] that will animate between the provided colors according to the
     * Material specification.
     *
     * @param selectedColor the color to use for the RadioButton when selected and enabled.
     * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
     * @param disabledSelectedColor the color to use for the RadioButton when disabled and selected.
     * @param disabledUnselectedColor the color to use for the RadioButton when disabled and not
     *   selected.
     * @return the resulting [RadioButtonColors] used for the RadioButton
     */
    @Composable
    fun colors(
        selectedColor: Color = Color.Unspecified,
        unselectedColor: Color = Color.Unspecified,
        disabledSelectedColor: Color = Color.Unspecified,
        disabledUnselectedColor: Color = Color.Unspecified
    ): RadioButtonColors =
        MaterialTheme.colorScheme.defaultRadioButtonColors.copy(
            selectedColor,
            unselectedColor,
            disabledSelectedColor,
            disabledUnselectedColor
        )

    internal val ColorScheme.defaultRadioButtonColors: RadioButtonColors
        get() {
            return defaultRadioButtonColorsCached
                ?: RadioButtonColors(
                        selectedColor = fromToken(RadioButtonTokens.SelectedIconColor),
                        unselectedColor = fromToken(RadioButtonTokens.UnselectedIconColor),
                        disabledSelectedColor =
                            fromToken(RadioButtonTokens.DisabledSelectedIconColor)
                                .copy(alpha = RadioButtonTokens.DisabledSelectedIconOpacity),
                        disabledUnselectedColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedIconColor)
                                .copy(alpha = RadioButtonTokens.DisabledUnselectedIconOpacity)
                    )
                    .also { defaultRadioButtonColorsCached = it }
        }
}

/**
 * Represents the color used by a [RadioButton] in different states.
 *
 * @param selectedColor the color to use for the RadioButton when selected and enabled.
 * @param unselectedColor the color to use for the RadioButton when unselected and enabled.
 * @param disabledSelectedColor the color to use for the RadioButton when disabled and selected.
 * @param disabledUnselectedColor the color to use for the RadioButton when disabled and not
 *   selected.
 * @constructor create an instance with arbitrary colors. See [RadioButtonDefaults.colors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
class RadioButtonColors
constructor(
    val selectedColor: Color,
    val unselectedColor: Color,
    val disabledSelectedColor: Color,
    val disabledUnselectedColor: Color
) {
    /**
     * Returns a copy of this SelectableChipColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        selectedColor: Color = this.selectedColor,
        unselectedColor: Color = this.unselectedColor,
        disabledSelectedColor: Color = this.disabledSelectedColor,
        disabledUnselectedColor: Color = this.disabledUnselectedColor,
    ) =
        RadioButtonColors(
            selectedColor.takeOrElse { this.selectedColor },
            unselectedColor.takeOrElse { this.unselectedColor },
            disabledSelectedColor.takeOrElse { this.disabledSelectedColor },
            disabledUnselectedColor.takeOrElse { this.disabledUnselectedColor },
        )

    /**
     * Represents the main color used to draw the outer and inner circles, depending on whether the
     * [RadioButton] is [enabled] / [selected].
     *
     * @param enabled whether the [RadioButton] is enabled
     * @param selected whether the [RadioButton] is selected
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    internal fun radioColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target =
            when {
                enabled && selected -> selectedColor
                enabled && !selected -> unselectedColor
                !enabled && selected -> disabledSelectedColor
                else -> disabledUnselectedColor
            }

        // If not enabled 'snap' to the disabled state, as there should be no animations between
        // enabled / disabled.
        return if (enabled) {
            // TODO Load the motionScheme tokens from the component tokens file
            animateColorAsState(target, MotionSchemeKeyTokens.DefaultEffects.value())
        } else {
            rememberUpdatedState(target)
        }
    }

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

private val RadioButtonPadding = 2.dp
private val RadioButtonDotSize = 12.dp
private val RadioStrokeWidth = 2.dp
