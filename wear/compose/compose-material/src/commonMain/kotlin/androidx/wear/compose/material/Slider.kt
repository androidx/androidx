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

package androidx.wear.compose.material

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * [InlineSlider] allows users to make a selection from a range of values. The range of selections
 * is shown as a bar between the minimum and maximum values of the range,
 * from which users may select a single value.
 * InlineSlider is ideal for adjusting settings such as volume or brightness.
 *
 * Value can be increased and decreased by clicking on the increase and decrease buttons, located
 * accordingly to the start and end of the control. Buttons can have custom icons -
 * [decreaseIcon] and [increaseIcon].
 *
 * The selection bar in the middle of control can have separators if [segmented] flag is set to true.
 * A single step value is calculated as the difference between min and max values of [valueRange]
 * divided by [steps] value.
 *
 * A continuous non-segmented slider sample:
 * @sample androidx.wear.compose.material.samples.InlineSliderSample
 *
 * A segmented slider sample:
 * @sample androidx.wear.compose.material.samples.InlineSliderSegmentedSample
 *
 * @param value Current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param steps Specifies the number of discrete values, evenly distributed
 * between across the whole value range. Must not be negative. If 0, slider will have only
 * min and max values and no steps in between
 * @param modifier Modifiers for the Slider layout
 * @param enabled Controls the enabled state of the slider.
 * When `false`, this slider will not be clickable
 * @param valueRange Range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param segmented A boolean value which specifies whether a bar will be split into
 * segments or not. Recommendation is while using this flag do not have more than 8 [steps]
 * as it might affect user experience. By default true if number of [steps] is <=8.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (start) button
 * @param increaseIcon A slot for an icon which is placed on the increase (end) button
 * @param colors [InlineSliderColors] that will be used to resolve the background and content
 * color for this slider in different states
 */
@Composable
fun InlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= 8,
    decreaseIcon: @Composable () -> Unit = {
        Icon(
            imageVector = InlineSliderDefaults.DecreaseIcon,
            contentDescription = "Decrease" // TODO(b/204187777) i18n
        )
    },
    increaseIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Increase" // TODO(b/204187777) i18n
        )
    },
    colors: InlineSliderColors = InlineSliderDefaults.colors(),
) {
    require(steps >= 0) { "steps should be >= 0" }
    val currentStep =
        remember(value, valueRange, steps) { snapValueToStep(value, valueRange, steps) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .inlineSliderSemantics(
                currentStep,
                enabled,
                onValueChange,
                valueRange,
                steps
            )
            .height(InlineSliderDefaults.SliderHeight)
            .clip(MaterialTheme.shapes.small),
    ) {

        val visibleSegments = if (segmented) steps + 1 else 1

        val updateValue: (Int) -> Unit = { stepDiff ->
            val newValue = calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
            if (newValue != value) onValueChange(newValue)
        }
        val selectedBarColor = colors.barColor(enabled, true)
        val unselectedBarColor = colors.barColor(enabled, false)
        val backgroundColor = colors.backgroundColor(enabled)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth().background(backgroundColor.value)
        ) {
            ActionButton(
                enabled = enabled,
                onClick = { updateValue(-1) },
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .padding(start = InlineSliderDefaults.OuterHorizontalMargin),
                content = decreaseIcon
            )

            Box(
                Modifier.width(InlineSliderDefaults.SpacersWidth)
                    .fillMaxHeight()
                    .background(colors.spacerColor(enabled).value)
            )

            val valueRatio by animateFloatAsState(currentStep.toFloat() / (steps + 1).toFloat())

            Box(
                modifier = Modifier
                    .padding(horizontal = InlineSliderDefaults.BarMargin)
                    .height(InlineSliderDefaults.BarHeight)
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .drawProgressBar(
                        selectedBarColor = selectedBarColor,
                        unselectedBarColor = unselectedBarColor,
                        backgroundColor = backgroundColor,
                        visibleSegments = visibleSegments,
                        valueRatio = valueRatio,
                        direction = LocalLayoutDirection.current
                    )
            )

            Box(
                Modifier.width(InlineSliderDefaults.SpacersWidth)
                    .fillMaxHeight()
                    .background(colors.spacerColor(enabled).value)
            )

            ActionButton(
                enabled = enabled,
                onClick = { updateValue(1) },
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier.padding(end = InlineSliderDefaults.OuterHorizontalMargin),
                content = increaseIcon
            )
        }
    }
}

/**
 * Represents the background and content colors used in [InlineSlider] in different states.
 */
@Stable
public interface InlineSliderColors {
    /**
     * Represents the background color for this slider, depending on [enabled].
     * @param enabled Whether the slider is enabled
     */
    @Composable
    public fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the color of the progress bar in the middle of the slider,
     * depending on the [enabled] and [selected].
     *
     * @param enabled Whether the slider is enabled
     * @param selected Whether color is for selected part of the slider
     */
    @Composable
    public fun barColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the color of the spacer between buttons and a progress bar,
     * depending on the [enabled]
     *
     * @param enabled Whether the slider is enabled
     */
    @Composable
    public fun spacerColor(enabled: Boolean): State<Color>
}

/**
 * Defaults used by [InlineSlider]
 */
object InlineSliderDefaults {

    /**
     * The default height applied for the [InlineSlider].
     * Note that you can override it by applying Modifier.size directly on [InlineSlider].
     */
    internal val SliderHeight = 52.dp

    internal val ControlSize = 36.dp

    internal val OuterHorizontalMargin = 8.dp

    internal val SpacersWidth = 1.dp

    internal val BarMargin = 7.dp

    internal val BarHeight = 6.dp

    internal val BarSeparatorWidth = 1.dp

    /**
     * Creates a [InlineSliderColors] that represents the default background
     * and content colors used in an [InlineSlider].
     *
     * @param backgroundColor The background color of this [InlineSlider] when enabled
     * @param spacerColor The color of the spacer between buttons and a progress bar when enabled
     * @param selectedBarColor The color of the progress bar when enabled
     * @param unselectedBarColor The background color of the progress bar when enabled
     * @param disabledBackgroundColor The background color of this [InlineSlider] when disabled
     * @param disabledSpacerColor The color of the spacer between buttons and a progress bar
     * when disabled
     * @param disabledSelectedBarColor The color of the progress bar when disabled
     * @param disabledUnselectedBarColor The background color of the progress bar when disabled
     */
    @Composable
    fun colors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        spacerColor: Color = MaterialTheme.colors.background,
        selectedBarColor: Color = MaterialTheme.colors.secondary,
        unselectedBarColor: Color = MaterialTheme.colors.onSurface.copy(0.1f),
        disabledBackgroundColor: Color = backgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledSpacerColor: Color = spacerColor,
        disabledSelectedBarColor: Color = selectedBarColor.copy(alpha = ContentAlpha.disabled),
        disabledUnselectedBarColor: Color = unselectedBarColor.copy(alpha = 0.05f),
    ): InlineSliderColors = DefaultInlineSliderColors(
        backgroundColor = backgroundColor,
        spacerColor = spacerColor,
        selectedBarColor = selectedBarColor,
        unselectedBarColor = unselectedBarColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledSpacerColor = disabledSpacerColor,
        disabledSelectedBarColor = disabledSelectedBarColor,
        disabledUnselectedBarColor = disabledUnselectedBarColor
    )

    /**
     * An [Icon] with a minus sign.
     */
    val DecreaseIcon: ImageVector
        get() = if (_decreaseIcon != null) _decreaseIcon!!
        else {
            _decreaseIcon = materialIcon(name = "DecreaseIcon") {
                materialPath {
                    moveTo(19.0f, 13.0f)
                    horizontalLineTo(5.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(14.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }
            _decreaseIcon!!
        }

    private var _decreaseIcon: ImageVector? = null
}

@Immutable
private class DefaultInlineSliderColors(
    private val backgroundColor: Color,
    private val spacerColor: Color,
    private val selectedBarColor: Color,
    private val unselectedBarColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledSpacerColor: Color,
    private val disabledSelectedBarColor: Color,
    private val disabledUnselectedBarColor: Color,
) : InlineSliderColors {
    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> =
        animateColorAsState(if (enabled) backgroundColor else disabledBackgroundColor)

    @Composable
    override fun spacerColor(enabled: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) spacerColor else disabledSpacerColor
        )

    @Composable
    override fun barColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateColorAsState(
            if (enabled) {
                if (selected) selectedBarColor else unselectedBarColor
            } else {
                if (selected) disabledSelectedBarColor else disabledUnselectedBarColor
            }
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultInlineSliderColors

        if (backgroundColor != other.backgroundColor) return false
        if (spacerColor != other.spacerColor) return false
        if (selectedBarColor != other.selectedBarColor) return false
        if (unselectedBarColor != other.unselectedBarColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledSpacerColor != other.disabledSpacerColor) return false
        if (disabledSelectedBarColor != other.disabledSelectedBarColor) return false
        if (disabledUnselectedBarColor != other.disabledUnselectedBarColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + spacerColor.hashCode()
        result = 31 * result + selectedBarColor.hashCode()
        result = 31 * result + unselectedBarColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledSpacerColor.hashCode()
        result = 31 * result + disabledSelectedBarColor.hashCode()
        result = 31 * result + disabledUnselectedBarColor.hashCode()
        return result
    }
}

@Composable
private fun ActionButton(
    enabled: Boolean,
    onClick: () -> Unit,
    contentAlignment: Alignment,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.width(InlineSliderDefaults.ControlSize)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick)
            .then(modifier),
        contentAlignment = contentAlignment
    ) {
        CompositionLocalProvider(
            LocalContentAlpha provides
                if (enabled) LocalContentAlpha.current else ContentAlpha.disabled,
            content = content
        )
    }
}

private fun calculateCurrentStepValue(
    currentStep: Int,
    steps: Int,
    valueRange: ClosedFloatingPointRange<Float>
): Float = lerp(
    valueRange.start, valueRange.endInclusive,
    currentStep.toFloat() / (steps + 1).toFloat()
).coerceIn(valueRange)

private fun snapValueToStep(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Int = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start) * (steps + 1))
    .roundToInt().coerceIn(0, steps + 1)

private fun Modifier.inlineSliderSemantics(
    step: Int,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Modifier = semantics(mergeDescendants = true) {
    if (!enabled) disabled()
    setProgress(
        action = { targetValue ->
            val newStepIndex = snapValueToStep(targetValue, valueRange, steps)
            if (step == newStepIndex) {
                false
            } else {
                onValueChange(targetValue)
                true
            }
        }
    )
}.progressSemantics(calculateCurrentStepValue(step, steps, valueRange), valueRange, steps)

private fun Modifier.drawProgressBar(
    selectedBarColor: State<Color>,
    unselectedBarColor: State<Color>,
    backgroundColor: State<Color>,
    visibleSegments: Int,
    valueRatio: Float,
    direction: LayoutDirection,
): Modifier = drawWithContent {
    drawLine(
        selectedBarColor.value,
        Offset(
            directedValue(direction, 0f, size.width * (1 - valueRatio)), size.height / 2
        ),
        Offset(
            directedValue(direction, size.width * valueRatio, size.width), size.height / 2
        ),
        strokeWidth = InlineSliderDefaults.BarHeight.toPx()
    )
    drawLine(
        unselectedBarColor.value,
        Offset(
            directedValue(direction, size.width * valueRatio, 0f), size.height / 2
        ),
        Offset(
            directedValue(direction, size.width, size.width * (1 - valueRatio)), size.height / 2
        ),
        strokeWidth = InlineSliderDefaults.BarHeight.toPx()
    )
    for (separator in 1 until visibleSegments) {
        val x = separator * size.width / visibleSegments
        drawLine(
            backgroundColor.value,
            Offset(x, size.height / 2 - InlineSliderDefaults.BarHeight.toPx() / 2),
            Offset(x, size.height / 2 + InlineSliderDefaults.BarHeight.toPx() / 2),
            strokeWidth = InlineSliderDefaults.BarSeparatorWidth.toPx()
        )
    }
}

private fun <T> directedValue(layoutDirection: LayoutDirection, ltrValue: T, rtlValue: T): T =
    if (layoutDirection == LayoutDirection.Ltr) ltrValue else rtlValue