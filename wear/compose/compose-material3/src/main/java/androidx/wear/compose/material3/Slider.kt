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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.SliderDefaults.MaxSegmentSteps
import androidx.wear.compose.material3.internal.Icons
import androidx.wear.compose.material3.internal.Strings.Companion.SliderDecreaseButtonContentDescription
import androidx.wear.compose.material3.internal.Strings.Companion.SliderIncreaseButtonContentDescription
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.material3.tokens.SliderTokens
import androidx.wear.compose.materialcore.InlineSliderButton
import androidx.wear.compose.materialcore.RangeDefaults.calculateCurrentStepValue
import androidx.wear.compose.materialcore.RangeDefaults.snapValueToStep
import androidx.wear.compose.materialcore.directedValue
import kotlin.math.roundToInt

/**
 * [Slider] allows users to make a selection from a range of values. The range of selections is
 * shown as a bar between the minimum and maximum values of the range, from which users may select a
 * single value. Slider is ideal for adjusting settings such as volume or brightness.
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
 *
 * @sample androidx.wear.compose.material3.samples.SliderSample
 *
 * A segmented slider sample:
 *
 * @sample androidx.wear.compose.material3.samples.SliderSegmentedSample
 * @param value Current value of the Slider. If outside of [valueRange] provided, value will be
 *   coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 *   distributed across the whole value range. Must not be negative. If 0, slider will have only min
 *   and max values and no steps in between.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (start) button such as
 *   [SliderDefaults.DecreaseIcon].
 * @param increaseIcon A slot for an icon which is placed on the increase (end) button such as
 *   [SliderDefaults.IncreaseIcon].
 * @param modifier Modifiers for the Slider layout.
 * @param enabled Controls the enabled state of the slider. When `false`, this slider will not be
 *   clickable.
 * @param valueRange Range of values that Slider value can take. Passed [value] will be coerced to
 *   this range.
 * @param segmented A boolean value which specifies whether a bar will be split into segments or
 *   not. Recommendation is while using this flag do not have more than [MaxSegmentSteps] steps as
 *   it might affect user experience. By default true if number of [steps] is <= [MaxSegmentSteps].
 * @param shape Defines slider's shape. It is strongly recommended to use the default as this shape
 *   is a key characteristic of the Wear Material3 Theme.
 * @param colors [SliderColors] that will be used to resolve the background and content color for
 *   this slider in different states.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    decreaseIcon: @Composable () -> Unit = { SliderDefaults.DecreaseIcon() },
    increaseIcon: @Composable () -> Unit = { SliderDefaults.IncreaseIcon() },
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= MaxSegmentSteps,
    shape: Shape = SliderDefaults.shape,
    colors: SliderColors = SliderDefaults.sliderColors(),
) {
    require(steps >= 0) { "steps should be >= 0" }
    val currentStep =
        remember(value, valueRange, steps) { snapValueToStep(value, valueRange, steps) }
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .rangeSemantics(value, enabled, onValueChange, valueRange, steps)
                .height(SLIDER_HEIGHT)
                .clip(shape)
    ) {
        val visibleSegments = if (segmented) steps + 1 else 1

        val updateValue: (Int) -> Unit = { stepDiff ->
            val newValue = calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
            if (newValue != value) onValueChange(newValue)
        }
        val selectedBarColor = colors.barColor(enabled, true)
        val unselectedBarColor = colors.barColor(enabled, false)
        val containerColor = colors.containerColor(enabled)
        val selectedBarSeparatorColor = colors.barSeparatorColor(enabled, true)
        val unselectedBarSeparatorColor = colors.barSeparatorColor(enabled, false)
        CompositionLocalProvider(
            LocalIndication provides ripple(bounded = false, radius = this.maxWidth / 2)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth().background(containerColor.value)
            ) {
                val increaseButtonEnabled = enabled && currentStep < steps + 1
                val decreaseButtonEnabled = enabled && currentStep > 0

                InlineSliderButton(
                    enabled = decreaseButtonEnabled,
                    onClick = { updateValue(-1) },
                    contentAlignment = Alignment.Center,
                    buttonControlSize = CONTROL_SIZE,
                    modifier = Modifier,
                    content = {
                        SliderButtonContent(
                            decreaseButtonEnabled,
                            { enabled -> colors.buttonIconColor(enabled) },
                            decreaseIcon
                        )
                    }
                )

                val valueRatio by
                    animateFloatAsState(
                        targetValue = currentStep.toFloat() / (steps + 1).toFloat(),
                        animationSpec =
                            tween(
                                durationMillis = MotionTokens.DurationShort3,
                                delayMillis = 0,
                                easing = MotionTokens.EasingStandardDecelerate
                            ),
                        label = "sliderProgressBarAnimation"
                    )

                Box(
                    modifier =
                        Modifier.height(BAR_HEIGHT)
                            .weight(1f)
                            .clip(shape)
                            .drawProgressBar(
                                selectedBarColor = selectedBarColor,
                                unselectedBarColor = unselectedBarColor,
                                selectedBarSeparatorColor = selectedBarSeparatorColor,
                                unselectedBarSeparatorColor = unselectedBarSeparatorColor,
                                visibleSegments = visibleSegments,
                                valueRatio = valueRatio,
                                direction = LocalLayoutDirection.current,
                                segmented = segmented
                            )
                )

                InlineSliderButton(
                    enabled = increaseButtonEnabled,
                    onClick = { updateValue(1) },
                    contentAlignment = Alignment.Center,
                    buttonControlSize = CONTROL_SIZE,
                    modifier = Modifier,
                    content = {
                        SliderButtonContent(
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
 * [Slider] allows users to make a selection from a range of values. The range of selections is
 * shown as a bar between the minimum and maximum values of the range, from which users may select a
 * single value. Slider is ideal for adjusting settings such as volume or brightness.
 *
 * Value can be increased and decreased by clicking on the increase and decrease buttons, located
 * accordingly to the start and end of the control. Buttons can have custom icons - [decreaseIcon]
 * and [increaseIcon].
 *
 * The bar in the middle of control can have separators if [segmented] flag is set to true. A number
 * of steps is calculated as the difference between max and min values of [valueProgression] divided
 * by [valueProgression].step - 1. For example, with a range of 100..120 and a step 5, number of by
 * [valueProgression].step - 1. For example, with a range of 100..120 and a step 5, number of steps
 * will be (120-100)/ 5 - 1 = 3. Steps are 100(first), 105, 110, 115, 120(last)
 *
 * If [valueProgression] range is not equally divisible by [valueProgression].step, then
 * [valueProgression].last will be adjusted to the closest divisible value in the range. For
 * example, 1..13 range and a step = 5, steps will be 1(first) , 6 , 11(last)
 *
 * A continuous non-segmented slider sample:
 *
 * @sample androidx.wear.compose.material3.samples.SliderWithIntegerSample
 *
 * A segmented slider sample:
 *
 * @sample androidx.wear.compose.material3.samples.SliderSegmentedSample
 * @param value Current value of the Slider. If outside of [valueProgression] provided, value will
 *   be coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param valueProgression Progression of values that Slider value can take. Consists of rangeStart,
 *   rangeEnd and step. Range will be equally divided by step size.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (start) button such as
 *   [SliderDefaults.DecreaseIcon].
 * @param increaseIcon A slot for an icon which is placed on the increase (end) button such as
 *   [SliderDefaults.IncreaseIcon].
 * @param modifier Modifiers for the Slider layout.
 * @param enabled Controls the enabled state of the slider. When `false`, this slider will not be
 *   clickable.
 * @param segmented A boolean value which specifies whether a bar will be split into segments or
 *   not. Recommendation is while using this flag do not have more than [MaxSegmentSteps] steps as
 *   it might affect user experience. By default true if number of steps is <= [MaxSegmentSteps].
 * @param shape Defines slider's shape. It is strongly recommended to use the default as this shape
 *   is a key characteristic of the Wear Material3 Theme.
 * @param colors [SliderColors] that will be used to resolve the background and content color for
 *   this slider in different states.
 */
@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    modifier: Modifier = Modifier,
    decreaseIcon: @Composable () -> Unit = { SliderDefaults.DecreaseIcon() },
    increaseIcon: @Composable () -> Unit = { SliderDefaults.IncreaseIcon() },
    enabled: Boolean = true,
    segmented: Boolean = valueProgression.stepsNumber() <= MaxSegmentSteps,
    shape: Shape = SliderDefaults.shape,
    colors: SliderColors = SliderDefaults.sliderColors(),
) {
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        steps = valueProgression.stepsNumber(),
        modifier = modifier,
        enabled = enabled,
        valueRange = valueProgression.first.toFloat()..valueProgression.last.toFloat(),
        segmented = segmented,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        shape = shape,
        colors = colors
    )
}

/** Defaults used by slider. */
object SliderDefaults {
    /** The recommended size for Slider [DecreaseIcon] and [IncreaseIcon] button icons. */
    val IconSize = 24.dp

    /** The maximum recommended number of steps for a segmented [Slider]. */
    val MaxSegmentSteps = 8

    /** The recommended [Shape] for Slider. */
    val shape: Shape
        @Composable get() = SliderTokens.ContainerShape.value

    /** Decrease Icon. */
    @Composable
    fun DecreaseIcon(modifier: Modifier = Modifier) =
        Icon(
            Icons.Remove,
            getString(SliderDecreaseButtonContentDescription),
            modifier.size(IconSize)
        )

    /** Increase Icon. */
    @Composable
    fun IncreaseIcon(modifier: Modifier = Modifier) =
        Icon(Icons.Add, getString(SliderIncreaseButtonContentDescription), modifier.size(IconSize))

    /**
     * Creates a [SliderColors] that represents the default background and content colors used in an
     * [Slider].
     */
    @Composable fun sliderColors() = MaterialTheme.colorScheme.defaultSliderColor

    /**
     * Creates a [SliderColors] that represents the default background and content colors used in an
     * [Slider].
     *
     * @param containerColor The background color of this [Slider] when enabled
     * @param buttonIconColor The color of the icon of buttons when enabled
     * @param selectedBarColor The color of the progress bar when enabled
     * @param unselectedBarColor The background color of the progress bar when enabled
     * @param selectedBarSeparatorColor The color of separator between visible segments within the
     *   selected portion of the bar when enabled
     * @param unselectedBarSeparatorColor The color of unselected separator between visible segments
     *   within the unselected portion of the bar when enabled
     * @param disabledContainerColor The background color of this [Slider] when disabled
     * @param disabledButtonIconColor The color of the icon of buttons when disabled
     * @param disabledSelectedBarColor The color of the progress bar when disabled
     * @param disabledUnselectedBarColor The background color of the progress bar when disabled
     * @param disabledSelectedBarSeparatorColor The color of selected separator between visible
     *   segments when disabled
     * @param disabledUnselectedBarSeparatorColor The color of unselected separator between visible
     *   segments when disabled
     */
    @Composable
    fun sliderColors(
        containerColor: Color = Color.Unspecified,
        buttonIconColor: Color = Color.Unspecified,
        selectedBarColor: Color = Color.Unspecified,
        unselectedBarColor: Color = Color.Unspecified,
        selectedBarSeparatorColor: Color = Color.Unspecified,
        unselectedBarSeparatorColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledButtonIconColor: Color = Color.Unspecified,
        disabledSelectedBarColor: Color = Color.Unspecified,
        disabledUnselectedBarColor: Color = Color.Unspecified,
        disabledSelectedBarSeparatorColor: Color = Color.Unspecified,
        disabledUnselectedBarSeparatorColor: Color = Color.Unspecified
    ): SliderColors =
        MaterialTheme.colorScheme.defaultSliderColor.copy(
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
            disabledUnselectedBarSeparatorColor = disabledUnselectedBarSeparatorColor
        )

    /**
     * Creates a [SliderColors] as an alternative to the default colors, providing a visual
     * indication of value changes within a [Slider].
     *
     * Example of a slider uses [variantSliderColors] when its value is different from the initial
     * value:
     *
     * @sample androidx.wear.compose.material3.samples.ChangedSliderSample
     */
    @Composable fun variantSliderColors() = MaterialTheme.colorScheme.defaultVariantSliderColor

    /**
     * Creates a [SliderColors] as an alternative to the default colors, providing a visual
     * indication of value changes within a [Slider].
     *
     * Example of a slider uses [variantSliderColors] when its value is different from the initial
     * value:
     *
     * @sample androidx.wear.compose.material3.samples.ChangedSliderSample
     * @param containerColor The background color of this [Slider] when enabled
     * @param buttonIconColor The color of the icon of buttons when enabled
     * @param selectedBarColor The color of the progress bar when enabled
     * @param unselectedBarColor The background color of the progress bar when enabled
     * @param selectedBarSeparatorColor The color of separator between visible segments within the
     *   selected portion of the bar when enabled
     * @param unselectedBarSeparatorColor The color of unselected separator between visible segments
     *   within the unselected portion of the bar when enabled
     * @param disabledContainerColor The background color of this [Slider] when disabled
     * @param disabledButtonIconColor The color of the icon of buttons when disabled
     * @param disabledSelectedBarColor The color of the progress bar when disabled
     * @param disabledUnselectedBarColor The background color of the progress bar when disabled
     * @param disabledSelectedBarSeparatorColor The color of selected separator between visible
     *   segments when disabled
     * @param disabledUnselectedBarSeparatorColor The color of unselected separator between visible
     *   segments when disabled
     */
    @Composable
    fun variantSliderColors(
        containerColor: Color = Color.Unspecified,
        buttonIconColor: Color = Color.Unspecified,
        selectedBarColor: Color = Color.Unspecified,
        unselectedBarColor: Color = Color.Unspecified,
        selectedBarSeparatorColor: Color = Color.Unspecified,
        unselectedBarSeparatorColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledButtonIconColor: Color = Color.Unspecified,
        disabledSelectedBarColor: Color = Color.Unspecified,
        disabledUnselectedBarColor: Color = Color.Unspecified,
        disabledSelectedBarSeparatorColor: Color = Color.Unspecified,
        disabledUnselectedBarSeparatorColor: Color = Color.Unspecified
    ): SliderColors =
        MaterialTheme.colorScheme.defaultSliderColor.copy(
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
            disabledUnselectedBarSeparatorColor = disabledUnselectedBarSeparatorColor
        )

    private val ColorScheme.defaultSliderColor: SliderColors
        get() {
            return defaultSliderColorsCached
                ?: SliderColors(
                        containerColor = fromToken(SliderTokens.ContainerColor),
                        buttonIconColor = fromToken(SliderTokens.ButtonIconColor),
                        selectedBarColor = fromToken(SliderTokens.SelectedBarColor),
                        unselectedBarColor =
                            fromToken(SliderTokens.UnselectedBarColor)
                                .copy(alpha = SliderTokens.UnselectedBarOpacity),
                        selectedBarSeparatorColor =
                            fromToken(SliderTokens.SelectedBarSeparatorColor),
                        unselectedBarSeparatorColor =
                            fromToken(SliderTokens.UnselectedBarSeparatorColor)
                                .copy(alpha = SliderTokens.UnselectedBarSeparatorOpacity),
                        disabledContainerColor =
                            fromToken(SliderTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = SliderTokens.DisabledContainerOpacity
                                ),
                        disabledButtonIconColor =
                            fromToken(SliderTokens.DisabledButtonIconColor)
                                .toDisabledColor(
                                    disabledAlpha = SliderTokens.DisabledButtonIconOpacity
                                ),
                        disabledSelectedBarColor = fromToken(SliderTokens.DisabledSelectedBarColor),
                        disabledUnselectedBarColor =
                            fromToken(SliderTokens.DisabledUnselectedBarColor)
                                .copy(alpha = SliderTokens.DisabledUnselectedBarOpacity),
                        disabledSelectedBarSeparatorColor =
                            fromToken(SliderTokens.DisabledSelectedBarSeparatorColor),
                        disabledUnselectedBarSeparatorColor =
                            fromToken(SliderTokens.DisabledUnselectedBarSeparatorColor)
                                .copy(alpha = SliderTokens.DisabledUnselectedBarSeparatorOpacity),
                    )
                    .also { defaultSliderColorsCached = it }
        }

    private val ColorScheme.defaultVariantSliderColor: SliderColors
        get() {
            return defaultVariantSliderColorsCached
                ?: SliderColors(
                        containerColor = fromToken(SliderTokens.ContainerColor),
                        buttonIconColor = fromToken(SliderTokens.ButtonIconColor),
                        selectedBarColor = fromToken(SliderTokens.VariantSelectedBarColor),
                        unselectedBarColor =
                            fromToken(SliderTokens.UnselectedBarColor)
                                .copy(alpha = SliderTokens.UnselectedBarOpacity),
                        selectedBarSeparatorColor =
                            fromToken(SliderTokens.SelectedBarSeparatorColor),
                        unselectedBarSeparatorColor =
                            fromToken(SliderTokens.VariantUnselectedBarSeparatorColor)
                                .copy(alpha = SliderTokens.UnselectedBarSeparatorOpacity),
                        disabledContainerColor =
                            fromToken(SliderTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = SliderTokens.DisabledContainerOpacity
                                ),
                        disabledButtonIconColor =
                            fromToken(SliderTokens.DisabledButtonIconColor)
                                .toDisabledColor(
                                    disabledAlpha = SliderTokens.DisabledButtonIconOpacity
                                ),
                        disabledSelectedBarColor = fromToken(SliderTokens.DisabledSelectedBarColor),
                        disabledUnselectedBarColor =
                            fromToken(SliderTokens.DisabledUnselectedBarColor)
                                .copy(alpha = SliderTokens.DisabledUnselectedBarOpacity),
                        disabledSelectedBarSeparatorColor =
                            fromToken(SliderTokens.DisabledSelectedBarSeparatorColor),
                        disabledUnselectedBarSeparatorColor =
                            fromToken(SliderTokens.DisabledUnselectedBarSeparatorColor)
                                .copy(alpha = SliderTokens.DisabledUnselectedBarSeparatorOpacity),
                    )
                    .also { defaultVariantSliderColorsCached = it }
        }
}

/**
 * Represents the background and content colors used in [Slider] in different states.
 *
 * @param containerColor The background color of this [Slider] when enabled.
 * @param buttonIconColor The color of the icon of buttons when enabled.
 * @param selectedBarColor The color of the progress bar when enabled.
 * @param unselectedBarColor The background color of the progress bar when enabled.
 * @param selectedBarSeparatorColor The color of selected separator between visible segments when
 *   enabled.
 * @param unselectedBarSeparatorColor The color of unselected separator between visible segments
 *   when enabled.
 * @param disabledContainerColor The background color of this [Slider] when disabled.
 * @param disabledButtonIconColor The color of the icon of buttons when disabled.
 * @param disabledSelectedBarColor The color of the progress bar when disabled.
 * @param disabledUnselectedBarColor The background color of the progress bar when disabled.
 * @param disabledSelectedBarSeparatorColor The color of selected separator between visible segments
 *   when disabled.
 * @param disabledUnselectedBarSeparatorColor The color of unselected separator between visible
 *   segments when disabled.
 * @constructor create an instance with arbitrary colors. See [SliderDefaults.sliderColors] for the
 *   default implementation that follows Material specifications.
 */
@Immutable
class SliderColors
constructor(
    val containerColor: Color,
    val buttonIconColor: Color,
    val selectedBarColor: Color,
    val unselectedBarColor: Color,
    val selectedBarSeparatorColor: Color,
    val unselectedBarSeparatorColor: Color,
    val disabledContainerColor: Color,
    val disabledButtonIconColor: Color,
    val disabledSelectedBarColor: Color,
    val disabledUnselectedBarColor: Color,
    val disabledSelectedBarSeparatorColor: Color,
    val disabledUnselectedBarSeparatorColor: Color,
) {
    internal fun copy(
        containerColor: Color = Color.Unspecified,
        buttonIconColor: Color = Color.Unspecified,
        selectedBarColor: Color = Color.Unspecified,
        unselectedBarColor: Color = Color.Unspecified,
        selectedBarSeparatorColor: Color = Color.Unspecified,
        unselectedBarSeparatorColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledButtonIconColor: Color = Color.Unspecified,
        disabledSelectedBarColor: Color = Color.Unspecified,
        disabledUnselectedBarColor: Color = Color.Unspecified,
        disabledSelectedBarSeparatorColor: Color = Color.Unspecified,
        disabledUnselectedBarSeparatorColor: Color = Color.Unspecified,
    ) =
        SliderColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            buttonIconColor = buttonIconColor.takeOrElse { this.buttonIconColor },
            selectedBarColor = selectedBarColor.takeOrElse { this.selectedBarColor },
            unselectedBarColor = unselectedBarColor.takeOrElse { this.unselectedBarColor },
            selectedBarSeparatorColor =
                selectedBarSeparatorColor.takeOrElse { this.selectedBarSeparatorColor },
            unselectedBarSeparatorColor =
                unselectedBarSeparatorColor.takeOrElse { this.unselectedBarSeparatorColor },
            disabledContainerColor =
                disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledButtonIconColor =
                disabledButtonIconColor.takeOrElse { this.disabledButtonIconColor },
            disabledSelectedBarColor =
                disabledSelectedBarColor.takeOrElse { this.disabledSelectedBarColor },
            disabledUnselectedBarColor =
                disabledUnselectedBarColor.takeOrElse { this.disabledUnselectedBarColor },
            disabledSelectedBarSeparatorColor =
                disabledSelectedBarSeparatorColor.takeOrElse {
                    this.disabledSelectedBarSeparatorColor
                },
            disabledUnselectedBarSeparatorColor =
                disabledUnselectedBarSeparatorColor.takeOrElse {
                    this.disabledUnselectedBarSeparatorColor
                },
        )

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
    internal fun barSeparatorColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateColorAsState(
            when {
                enabled && selected -> selectedBarSeparatorColor
                enabled && !selected -> unselectedBarSeparatorColor
                !enabled && selected -> disabledSelectedBarSeparatorColor
                else -> disabledUnselectedBarSeparatorColor
            },
            label = "sliderBarSeparatorColorAnimation"
        )

    @Composable
    internal fun barColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) {
                if (selected) selectedBarColor else unselectedBarColor
            } else {
                if (selected) disabledSelectedBarColor else disabledUnselectedBarColor
            },
            label = "sliderBarColorAnimation"
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SliderColors

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

internal fun Modifier.drawProgressBar(
    selectedBarColor: State<Color>,
    unselectedBarColor: State<Color>,
    selectedBarSeparatorColor: State<Color>,
    unselectedBarSeparatorColor: State<Color>,
    visibleSegments: Int,
    valueRatio: Float,
    direction: LayoutDirection,
    segmented: Boolean,
): Modifier = drawWithContent {
    val barWidthInPx =
        if (segmented && valueRatio > 0f) {
            (size.width + 2 * BAR_SEPARATOR_RADIUS.toPx()) * valueRatio + SEGMENT_BAR_PADDING.toPx()
        } else {
            size.width * valueRatio
        }

    drawUnselectedProgressBar(unselectedBarColor.value, barWidthInPx, direction)

    drawSelectedProgressBar(selectedBarColor.value, barWidthInPx, direction)

    val separatorRadiusInPx = BAR_SEPARATOR_RADIUS.toPx()

    for (separator in 1 until visibleSegments) {
        val color =
            if (separator.toFloat() / visibleSegments.toFloat() <= valueRatio) {
                selectedBarSeparatorColor.value
            } else {
                unselectedBarSeparatorColor.value
            }
        val x =
            if (direction == LayoutDirection.Ltr) {
                separator
            } else {
                visibleSegments - separator
            } * (size.width + 2 * separatorRadiusInPx) / visibleSegments - separatorRadiusInPx

        drawProgressBarSeparator(color, x)
    }
}

internal fun DrawScope.drawSelectedProgressBar(
    color: Color,
    barWidth: Float,
    direction: LayoutDirection
) {
    val barHeightInPx = SELECTED_BAR_HEIGHT.toPx()
    drawRoundRect(
        color = color,
        topLeft =
            Offset(
                directedValue(direction, 0f, size.width - barWidth),
                (size.height - barHeightInPx) / 2
            ),
        size = Size(barWidth, barHeightInPx),
        cornerRadius = CornerRadius(barHeightInPx / 2)
    )
}

internal fun DrawScope.drawUnselectedProgressBar(
    color: Color,
    barWidth: Float,
    direction: LayoutDirection,
) {
    val barHeightInPx = UNSELECTED_BAR_HEIGHT.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(directedValue(direction, barWidth, 0f), (size.height - barHeightInPx) / 2),
        size = Size(size.width - barWidth, barHeightInPx),
        cornerRadius = CornerRadius(barHeightInPx / 2)
    )
}

internal fun DrawScope.drawProgressBarSeparator(color: Color, position: Float) {
    drawCircle(
        color = color,
        radius = BAR_SEPARATOR_RADIUS.toPx(),
        center = Offset(position, size.height / 2),
        blendMode = BlendMode.Src
    )
}

@Composable
private fun SliderButtonContent(
    enabled: Boolean,
    buttonIconColor: @Composable (enabled: Boolean) -> State<Color>,
    content: @Composable () -> Unit
) =
    CompositionLocalProvider(
        LocalContentColor provides buttonIconColor(enabled).value,
        content = content
    )

private val SLIDER_HEIGHT = 52.dp
private val CONTROL_SIZE = 48.dp
private val BAR_HEIGHT = 12.dp
private val SELECTED_BAR_HEIGHT = 12.dp
private val UNSELECTED_BAR_HEIGHT = 4.dp
private val BAR_SEPARATOR_RADIUS = 2.dp
private val SEGMENT_BAR_PADDING = 4.dp
