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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.InlineSliderButton
import androidx.wear.compose.materialcore.RangeDefaults.calculateCurrentStepValue
import androidx.wear.compose.materialcore.RangeDefaults.snapValueToStep
import androidx.wear.compose.materialcore.RangeIcons
import androidx.wear.compose.materialcore.directedValue
import androidx.wear.compose.materialcore.drawProgressBar
import kotlin.math.roundToInt

/**
 * [InlineSlider] allows users to make a selection from a range of values. The range of selections
 * is shown as a bar between the minimum and maximum values of the range, from which users may
 * select a single value. InlineSlider is ideal for adjusting settings such as volume or brightness.
 *
 * Value can be increased and decreased by clicking on the increase and decrease buttons, located
 * accordingly to the start and end of the control. Buttons can have custom icons - [decreaseIcon]
 * and [increaseIcon].
 *
 * The bar in the middle of control can have separators if [segmented] flag is set to true. A single
 * step value is calculated as the difference between min and max values of [valueRange] divided by
 * [steps] + 1 value.
 *
 * A continuous non-segmented slider sample:
 * @sample androidx.wear.compose.material3.samples.InlineSliderSample
 *
 * A segmented slider sample:
 * @sample androidx.wear.compose.material3.samples.InlineSliderSegmentedSample
 *
 * @param value Current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 * distributed across the whole value range. Must not be negative. If 0, slider will have only min
 * and max values and no steps in between.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (start) button such as
 * [InlineSliderDefaults.Decrease].
 * @param increaseIcon A slot for an icon which is placed on the increase (end) button such as
 * [InlineSliderDefaults.Increase].
 * @param modifier Modifiers for the Slider layout.
 * @param enabled Controls the enabled state of the slider. When `false`, this slider will not be
 * clickable.
 * @param valueRange Range of values that Slider value can take. Passed [value] will be coerced to
 * this range.
 * @param segmented A boolean value which specifies whether a bar will be split into segments or
 * not. Recommendation is while using this flag do not have more than 8 [steps] as it might affect
 * user experience. By default true if number of [steps] is <=8.
 * @param colors [InlineSliderColors] that will be used to resolve the background and content color
 * for this slider in different states.
 */
@ExperimentalWearMaterial3Api
@Composable
fun InlineSlider(
    value: Float,
    @Suppress("PrimitiveInLambda")
    onValueChange: (Float) -> Unit,
    steps: Int,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= 8,
    colors: InlineSliderColors = InlineSliderDefaults.colors(),
) {
    require(steps >= 0) { "steps should be >= 0" }
    val currentStep =
        remember(value, valueRange, steps) { snapValueToStep(value, valueRange, steps) }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .rangeSemantics(
                value, enabled, onValueChange, valueRange, steps
            )
            .height(InlineSliderDefaults.SliderHeight)
            .clip(CircleShape) // TODO(b/290625297) Replace with tokens
    ) {
        val visibleSegments = if (segmented) steps + 1 else 1

        @Suppress("PrimitiveInLambda")
        val updateValue: (Int) -> Unit = { stepDiff ->
            val newValue = calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
            if (newValue != value) onValueChange(newValue)
        }
        val selectedBarColor = colors.barColor(enabled, true)
        val unselectedBarColor = colors.barColor(enabled, false)
        val containerColor = colors.containerColor(enabled)
        val barSeparatorColor = colors.barSeparatorColor(enabled)
        CompositionLocalProvider(
            LocalIndication provides rememberRipple(bounded = false, radius = this.maxWidth / 2)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(containerColor.value)
            ) {
                val increaseButtonEnabled = enabled && currentStep < steps + 1
                val decreaseButtonEnabled = enabled && currentStep > 0

                InlineSliderButton(
                    enabled = decreaseButtonEnabled,
                    onClick = { updateValue(-1) },
                    contentAlignment = Alignment.CenterStart,
                    buttonControlSize = InlineSliderDefaults.ControlSize,
                    modifier = Modifier.padding(start = InlineSliderDefaults.OuterHorizontalMargin),
                    content = {
                        InlineSliderButtonContent(
                            decreaseButtonEnabled,
                            { enabled -> colors.buttonIconColor(enabled) },
                            decreaseIcon
                        )
                    }
                )

                val valueRatio by animateFloatAsState(
                    targetValue = currentStep.toFloat() / (steps + 1).toFloat(),
                    animationSpec = tween(SHORT_3, 0, STANDARD_DECELERATE)
                )

                Box(
                    modifier = Modifier
                        .height(InlineSliderDefaults.BarHeight)
                        .weight(1f)
                        .clip(CircleShape) // TODO(b/290625297) Replace with token
                        .drawProgressBar(
                            selectedBarColor = selectedBarColor,
                            unselectedBarColor = unselectedBarColor,
                            barSeparatorColor = barSeparatorColor,
                            visibleSegments = visibleSegments,
                            valueRatio = valueRatio,
                            direction = LocalLayoutDirection.current,
                            drawSelectedProgressBar = { color, ratio, direction, drawScope ->
                                drawScope.drawSelectedProgressBar(color, ratio, direction)
                            },
                            drawUnselectedProgressBar = { color, ratio, direction, drawScope ->
                                drawScope.drawUnselectedProgressBar(color, ratio, direction)
                            },
                            drawProgressBarSeparator = { color, position, drawScope ->
                                drawScope.drawProgressBarSeparator(color, position)
                            }
                        )
                )

                InlineSliderButton(
                    enabled = increaseButtonEnabled,
                    onClick = { updateValue(1) },
                    contentAlignment = Alignment.CenterEnd,
                    buttonControlSize = InlineSliderDefaults.ControlSize,
                    modifier = Modifier.padding(end = InlineSliderDefaults.OuterHorizontalMargin),
                    content = {
                        InlineSliderButtonContent(
                            increaseButtonEnabled,
                            { enabled -> colors.buttonIconColor(enabled) },
                            increaseIcon
                        )
                    }
                )
            }
        }
    }
}

/**
 * [InlineSlider] allows users to make a selection from a range of values. The range of selections
 * is shown as a bar between the minimum and maximum values of the range, from which users may
 * select a single value. InlineSlider is ideal for adjusting settings such as volume or brightness.
 *
 * Value can be increased and decreased by clicking on the increase and decrease buttons, located
 * accordingly to the start and end of the control. Buttons can have custom icons - [decreaseIcon]
 * and [increaseIcon].
 *
 * The bar in the middle of control can have separators if [segmented] flag is set to true. A number
 * of steps is calculated as the difference between max and min values of [valueProgression] divided
 * by [valueProgression].step - 1. For example, with a range of 100..120 and a step 5, number of
 * steps will be (120-100)/ 5 - 1 = 3. Steps are 100(first), 105, 110, 115, 120(last)
 *
 * If [valueProgression] range is not equally divisible by [valueProgression].step, then
 * [valueProgression].last will be adjusted to the closest divisible value in the range. For
 * example, 1..13 range and a step = 5, steps will be 1(first) , 6 , 11(last)
 *
 * A continuous non-segmented slider sample:
 * @sample androidx.wear.compose.material3.samples.InlineSliderWithIntegerSample
 *
 * A segmented slider sample:
 * @sample androidx.wear.compose.material3.samples.InlineSliderSegmentedSample
 *
 * @param value Current value of the Slider. If outside of [valueProgression] provided, value will
 * be coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param valueProgression Progression of values that Slider value can take. Consists of rangeStart,
 * rangeEnd and step. Range will be equally divided by step size.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (start) button such as
 * [InlineSliderDefaults.Decrease].
 * @param increaseIcon A slot for an icon which is placed on the increase (end) button such as
 * [InlineSliderDefaults.Increase].
 * @param modifier Modifiers for the Slider layout.
 * @param enabled Controls the enabled state of the slider. When `false`, this slider will not be
 * clickable.
 * @param segmented A boolean value which specifies whether a bar will be split into segments or
 * not. Recommendation is while using this flag do not have more than 8 steps as it might affect
 * user experience. By default true if number of steps is <=8.
 * @param colors [InlineSliderColors] that will be used to resolve the background and content color
 * for this slider in different states.
 */
@ExperimentalWearMaterial3Api
@Composable
fun InlineSlider(
    value: Int,
    @Suppress("PrimitiveInLambda")
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segmented: Boolean = valueProgression.stepsNumber() <= 8,
    colors: InlineSliderColors = InlineSliderDefaults.colors(),
) {
    InlineSlider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        steps = valueProgression.stepsNumber(),
        modifier = modifier,
        enabled = enabled,
        valueRange = valueProgression.first.toFloat()..valueProgression.last.toFloat(),
        segmented = segmented,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        colors = colors
    )
}

/** Defaults used by slider. */
@ExperimentalWearMaterial3Api
object InlineSliderDefaults {
    /**
     * Default slider measurements.
     */
    internal val SliderHeight = 52.dp

    internal val ControlSize = 36.dp

    internal val OuterHorizontalMargin = 6.dp

    internal val BarHeight = 10.dp

    internal val SelectedBarHeight = 10.dp

    internal val UnselectedBarHeight = 4.dp

    internal val BarSeparatorRadius = 2.dp

    /**
     * The recommended size for Slider [Decrease] and [Increase] button icons.
     */
    val IconSize = 24.dp

    /**
     * Creates a [InlineSliderColors] that represents the default background and content colors used
     * in an [InlineSlider].
     *
     * @param containerColor The background color of this [InlineSlider] when enabled
     * @param buttonIconColor The color of the icon of buttons when enabled
     * @param selectedBarColor The color of the progress bar when enabled
     * @param unselectedBarColor The background color of the progress bar when enabled
     * @param barSeparatorColor The color of separator between visible segments when enabled
     * @param disabledContainerColor The background color of this [InlineSlider] when disabled
     * @param disabledButtonIconColor The color of the icon of buttons when disabled
     * @param disabledSelectedBarColor The color of the progress bar when disabled
     * @param disabledUnselectedBarColor The background color of the progress bar when disabled
     * @param disabledBarSeparatorColor The color of separator between visible segments when disabled
     */
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        buttonIconColor: Color = MaterialTheme.colorScheme.secondary,
        selectedBarColor: Color = MaterialTheme.colorScheme.primary,
        unselectedBarColor: Color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
        barSeparatorColor: Color = MaterialTheme.colorScheme.primaryDim,
        disabledContainerColor: Color = containerColor.toDisabledColor(
            disabledAlpha = DisabledContainerAlpha
        ),
        disabledButtonIconColor: Color = buttonIconColor.toDisabledColor(),
        disabledSelectedBarColor: Color = selectedBarColor.toDisabledColor(),
        disabledUnselectedBarColor: Color = unselectedBarColor.toDisabledColor(),
        disabledBarSeparatorColor: Color = barSeparatorColor.toDisabledColor(
            disabledAlpha = DisabledContainerAlpha
        )
    ): InlineSliderColors = InlineSliderColors(
        containerColor = containerColor,
        buttonIconColor = buttonIconColor,
        selectedBarColor = selectedBarColor,
        unselectedBarColor = unselectedBarColor,
        barSeparatorColor = barSeparatorColor,
        disabledContainerColor = disabledContainerColor,
        disabledButtonIconColor = disabledButtonIconColor,
        disabledSelectedBarColor = disabledSelectedBarColor,
        disabledUnselectedBarColor = disabledUnselectedBarColor,
        disabledBarSeparatorColor = disabledBarSeparatorColor
    )

    /** Decrease [ImageVector]. */
    val Decrease = RangeIcons.Minus

    /** Increase [ImageVector]. */
    val Increase = Icons.Filled.Add
}

/**
 * Represents the background and content colors used in [InlineSlider] in different states.
 *
 * @constructor create an instance with arbitrary colors.
 * See [InlineSliderDefaults.colors] for the default implementation that follows Material
 * specifications.
 *
 * @param containerColor The background color of this [InlineSlider] when enabled.
 * @param buttonIconColor The color of the icon of buttons when enabled.
 * @param selectedBarColor The color of the progress bar when enabled.
 * @param unselectedBarColor The background color of the progress bar when enabled.
 * @param barSeparatorColor The color of separator between visible segments when enabled.
 * @param disabledContainerColor The background color of this [InlineSlider] when disabled.
 * @param disabledButtonIconColor The color of the icon of buttons when disabled.
 * @param disabledSelectedBarColor The color of the progress bar when disabled.
 * @param disabledUnselectedBarColor The background color of the progress bar when disabled.
 * @param disabledBarSeparatorColor The color of separator between visible segments when disabled.
 */
@ExperimentalWearMaterial3Api
@Immutable
class InlineSliderColors constructor(
    val containerColor: Color,
    val buttonIconColor: Color,
    val selectedBarColor: Color,
    val unselectedBarColor: Color,
    val barSeparatorColor: Color,
    val disabledContainerColor: Color,
    val disabledButtonIconColor: Color,
    val disabledSelectedBarColor: Color,
    val disabledUnselectedBarColor: Color,
    val disabledBarSeparatorColor: Color
) {
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) containerColor else disabledContainerColor,
            label = "sliderContainerColorAnimation"
        )

    @Composable
    internal fun buttonIconColor(enabled: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) buttonIconColor else disabledButtonIconColor,
            label = "sliderButtonIconColorAnimation"
        )

    @Composable
    internal fun barSeparatorColor(enabled: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) barSeparatorColor else disabledBarSeparatorColor,
            label = "sliderBarSeparatorColorAnimation"
        )

    @Composable
    internal fun barColor(enabled: Boolean, selected: Boolean): State<Color> = animateColorAsState(
        if (enabled) {
            if (selected) selectedBarColor else unselectedBarColor
        } else {
            if (selected) disabledSelectedBarColor else disabledUnselectedBarColor
        }, label = "sliderBarColorAnimation"
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as InlineSliderColors

        if (containerColor != other.containerColor) return false
        if (buttonIconColor != other.buttonIconColor) return false
        if (selectedBarColor != other.selectedBarColor) return false
        if (unselectedBarColor != other.unselectedBarColor) return false
        if (barSeparatorColor != other.barSeparatorColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledButtonIconColor != other.disabledButtonIconColor) return false
        if (disabledSelectedBarColor != other.disabledSelectedBarColor) return false
        if (disabledUnselectedBarColor != other.disabledUnselectedBarColor) return false
        if (disabledBarSeparatorColor != other.disabledBarSeparatorColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + buttonIconColor.hashCode()
        result = 31 * result + selectedBarColor.hashCode()
        result = 31 * result + unselectedBarColor.hashCode()
        result = 31 * result + barSeparatorColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledButtonIconColor.hashCode()
        result = 31 * result + disabledSelectedBarColor.hashCode()
        result = 31 * result + disabledUnselectedBarColor.hashCode()
        result = 31 * result + disabledBarSeparatorColor.hashCode()
        return result
    }
}

@OptIn(ExperimentalWearMaterial3Api::class)
internal fun DrawScope.drawSelectedProgressBar(
    color: Color,
    valueRatio: Float,
    direction: LayoutDirection
) {
    val barHeightInPx = InlineSliderDefaults.SelectedBarHeight.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(
            directedValue(direction, 0f, size.width * (1 - valueRatio)),
            (size.height - barHeightInPx) / 2
        ),
        size = Size(size.width * valueRatio, barHeightInPx),
        cornerRadius = CornerRadius(barHeightInPx / 2)
    )
}

@OptIn(ExperimentalWearMaterial3Api::class)
internal fun DrawScope.drawUnselectedProgressBar(
    color: Color,
    valueRatio: Float,
    direction: LayoutDirection,
) {
    val barHeightInPx = InlineSliderDefaults.UnselectedBarHeight.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(
            directedValue(direction, size.width * valueRatio, 0f), (size.height - barHeightInPx) / 2
        ),
        size = Size(size.width * (1 - valueRatio), barHeightInPx),
        cornerRadius = CornerRadius(barHeightInPx / 2)
    )
}

@OptIn(ExperimentalWearMaterial3Api::class)
internal fun DrawScope.drawProgressBarSeparator(color: Color, position: Float) {
    drawCircle(
        color = color,
        radius = InlineSliderDefaults.BarSeparatorRadius.toPx(),
        center = Offset(position, size.height / 2),
        blendMode = BlendMode.Src
    )
}

@Composable
private fun InlineSliderButtonContent(
    enabled: Boolean,
    buttonIconColor: @Composable (enabled: Boolean) -> State<Color>,
    content: @Composable () -> Unit
) = CompositionLocalProvider(
    LocalContentColor provides buttonIconColor(enabled).value,
    LocalContentAlpha provides if (enabled) {
        LocalContentAlpha.current
    } else ContentAlpha.disabled,
    content = content
)
