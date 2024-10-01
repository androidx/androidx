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

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.internal.Strings.Companion.StepperDecreaseIconContentDescription
import androidx.wear.compose.material3.internal.Strings.Companion.StepperIncreaseIconContentDescription
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.materialcore.RangeDefaults
import androidx.wear.compose.materialcore.RangeIcons
import androidx.wear.compose.materialcore.repeatableClickable
import kotlin.math.roundToInt

/**
 * [Stepper] allows users to make a selection from a range of values. It's a full-screen control
 * with increase button on the top, decrease button on the bottom and a slot (expected to have
 * either [Text] or [Button]) in the middle. Value can be increased and decreased by clicking on the
 * increase and decrease buttons. Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Step value is calculated as the difference between min and max values divided by [steps]+1.
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [LevelIndicator] if required. If [value] is not equal to any step value, then it will be coerced
 * to the closest step value. However, the [value] itself will not be changed and [onValueChange] in
 * this case will not be triggered. To add range semantics on Stepper, use
 * [Modifier.rangeSemantics].
 *
 * Example of a simple [Stepper] with [LevelIndicator]:
 *
 * @sample androidx.wear.compose.material3.samples.StepperSample
 *
 * Example of a [Stepper] with range semantics:
 *
 * @sample androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample
 *
 * Example of a [Stepper] with Custom icons and Button content:
 *
 * @sample androidx.wear.compose.material3.samples.StepperWithButtonSample
 * @param value Current value of the Stepper. If outside of [valueRange] provided, value will be
 *   coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 *   distributed across the whole value range. Must not be negative. If 0, stepper will have only
 *   min and max values and no steps in between.
 * @param modifier Modifiers for the Stepper layout.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button.
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button.
 * @param enabled Whether the [Stepper] is enabled.
 * @param valueRange Range of values that Stepper value can take. Passed [value] will be coerced to
 *   this range.
 * @param colors [StepperColors] that will be used to resolve the colors used for this [Stepper].
 *   See [StepperDefaults.colors].
 * @param content Content body for the Stepper.
 */
@Composable
fun Stepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    decreaseIcon: @Composable () -> Unit = StepperDefaults.DecreaseIcon,
    increaseIcon: @Composable () -> Unit = StepperDefaults.IncreaseIcon,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    colors: StepperColors = StepperDefaults.colors(),
    content: @Composable BoxScope.() -> Unit
) {
    StepperImpl(
        value = value,
        onValueChange = onValueChange,
        steps = steps,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        valueRange = valueRange,
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        content = provideScopeContent(colors.contentColor(enabled = enabled), content)
    )
}

/**
 * [Stepper] allows users to make a selection from a range of values. It's a full-screen control
 * with increase button on the top, decrease button on the bottom and a slot (expected to have
 * either [Text] or [Button]) in the middle. Value can be increased and decreased by clicking on the
 * increase and decrease buttons. Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [LevelIndicator] if required. To add range semantics on Stepper, use [Modifier.rangeSemantics].
 *
 * Example of a [Stepper] with integer values:
 *
 * @sample androidx.wear.compose.material3.samples.StepperWithIntegerSample
 *
 * A number of steps is calculated as the difference between max and min values of
 * [valueProgression] divided by [valueProgression].step - 1. For example, with a range of 100..120
 * and a step 5, number of steps will be (120-100)/ 5 - 1 = 3. Steps are 100(first), 105, 110, 115,
 * 120(last)
 *
 * If [valueProgression] range is not equally divisible by [valueProgression].step, then
 * [valueProgression].last will be adjusted to the closest divisible value in the range. For
 * example, 1..13 range and a step = 5, steps will be 1(first), 6, 11(last)
 *
 * If [value] is not equal to any step value, then it will be coerced to the closest step value.
 * However, the [value] itself will not be changed and [onValueChange] in this case will not be
 * triggered.
 *
 * @param value Current value of the Stepper. If outside of [valueProgression] provided, value will
 *   be coerced to this range.
 * @param onValueChange Lambda in which value should be updated.
 * @param valueProgression Progression of values that Stepper value can take. Consists of
 *   rangeStart, rangeEnd and step. Range will be equally divided by step size.
 * @param modifier Modifiers for the Stepper layout.
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button.
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button.
 * @param enabled Whether the [Stepper] is enabled.
 * @param colors [StepperColors] that will be used to resolve the colors used for this [Stepper].
 *   See [StepperDefaults.colors].
 * @param content Content body for the Stepper.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    modifier: Modifier = Modifier,
    decreaseIcon: @Composable () -> Unit = StepperDefaults.DecreaseIcon,
    increaseIcon: @Composable () -> Unit = StepperDefaults.IncreaseIcon,
    enabled: Boolean = true,
    colors: StepperColors = StepperDefaults.colors(),
    content: @Composable BoxScope.() -> Unit
) {
    Stepper(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        steps = valueProgression.stepsNumber(),
        modifier = modifier,
        valueRange = valueProgression.first.toFloat()..valueProgression.last.toFloat(),
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        colors = colors,
        enabled = enabled,
        content = content
    )
}

/** Defaults used by [Stepper]. */
object StepperDefaults {
    /** Default size for increase and decrease icons. */
    val IconSize = 24.dp

    /** Default icon for the increase button. */
    val IncreaseIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = getString(StepperIncreaseIconContentDescription),
            modifier = Modifier.size(IconSize)
        )
    }

    /** Default icon for the decrease button. */
    val DecreaseIcon: @Composable () -> Unit = {
        Icon(
            imageVector = RangeIcons.Minus,
            contentDescription = getString(StepperDecreaseIconContentDescription),
            modifier = Modifier.size(IconSize)
        )
    }

    /** Creates a [StepperColors] that represents the default colors used in a [Stepper]. */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultStepperColors

    /**
     * Creates a [StepperColors] that represents the default colors used in a [Stepper].
     *
     * @param contentColor the content color for this [Stepper].
     * @param buttonContainerColor the button container color for this [Stepper].
     * @param buttonIconColor the button icon color for this [Stepper].
     * @param disabledContentColor the disabled content color for this [Stepper].
     * @param disabledButtonContainerColor the disabled button container color for this [Stepper].
     * @param disabledButtonIconColor the disabled button icon color for this [Stepper].
     */
    @Composable
    fun colors(
        contentColor: Color = Color.Unspecified,
        buttonContainerColor: Color = Color.Unspecified,
        buttonIconColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledButtonContainerColor: Color = Color.Unspecified,
        disabledButtonIconColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultStepperColors.copy(
            contentColor = contentColor,
            buttonContainerColor = buttonContainerColor,
            buttonIconColor = buttonIconColor,
            disabledContentColor = disabledContentColor,
            disabledButtonContainerColor = disabledButtonContainerColor,
            disabledButtonIconColor = disabledButtonIconColor,
        )

    // TODO: b/369766493 - Add color tokens for Stepper.
    private val ColorScheme.defaultStepperColors: StepperColors
        get() =
            defaultStepperColorsCached
                ?: StepperColors(
                        contentColor = fromToken(ColorSchemeKeyTokens.OnSurface),
                        buttonContainerColor = fromToken(ColorSchemeKeyTokens.PrimaryContainer),
                        buttonIconColor = fromToken(ColorSchemeKeyTokens.Primary),
                        disabledContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .toDisabledColor(disabledAlpha = DisabledContentAlpha),
                        disabledButtonContainerColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .toDisabledColor(disabledAlpha = DisabledContainerAlpha),
                        disabledButtonIconColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .toDisabledColor(disabledAlpha = DisabledContentAlpha),
                    )
                    .also { defaultStepperColorsCached = it }
}

/**
 * Represents Colors used in [Stepper].
 *
 * @param contentColor the content color of this [Stepper].
 * @param buttonContainerColor the button background color of this [Stepper].
 * @param buttonIconColor icon tint [Color] for this [Stepper].
 * @param disabledContentColor the content color of this [Stepper] in disabled state.
 * @param disabledButtonContainerColor the button background color of this [Stepper] in disabled
 *   state.
 * @param disabledButtonIconColor icon tint [Color] for this [Stepper] in disabled state.
 */
class StepperColors(
    val contentColor: Color,
    val buttonContainerColor: Color,
    val buttonIconColor: Color,
    val disabledContentColor: Color,
    val disabledButtonContainerColor: Color,
    val disabledButtonIconColor: Color,
) {
    internal fun copy(
        contentColor: Color,
        buttonContainerColor: Color,
        buttonIconColor: Color,
        disabledContentColor: Color,
        disabledButtonContainerColor: Color,
        disabledButtonIconColor: Color,
    ) =
        StepperColors(
            contentColor = contentColor.takeOrElse { this.contentColor },
            buttonContainerColor = buttonContainerColor.takeOrElse { this.buttonContainerColor },
            buttonIconColor = buttonIconColor.takeOrElse { this.buttonIconColor },
            disabledContentColor = disabledContentColor.takeOrElse { this.disabledContentColor },
            disabledButtonContainerColor =
                disabledButtonContainerColor.takeOrElse { this.disabledButtonContainerColor },
            disabledButtonIconColor =
                disabledButtonIconColor.takeOrElse { this.disabledButtonIconColor },
        )

    internal fun contentColor(enabled: Boolean): Color {
        return if (enabled) contentColor else disabledContentColor
    }

    internal fun buttonContainerColor(enabled: Boolean): Color {
        return if (enabled) buttonContainerColor else disabledButtonContainerColor
    }

    internal fun buttonIconColor(enabled: Boolean): Color {
        return if (enabled) buttonIconColor else disabledButtonIconColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is StepperColors) return false

        if (contentColor != other.contentColor) return false
        if (buttonContainerColor != other.buttonContainerColor) return false
        if (buttonIconColor != other.buttonIconColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledButtonContainerColor != other.disabledButtonContainerColor) return false
        if (disabledButtonIconColor != other.disabledButtonIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentColor.hashCode()
        result = 31 * result + buttonContainerColor.hashCode()
        result = 31 * result + buttonIconColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledButtonContainerColor.hashCode()
        result = 31 * result + disabledButtonIconColor.hashCode()
        return result
    }
}

@Composable
private fun StepperImpl(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: StepperColors,
    content: @Composable BoxScope.() -> Unit
) {
    require(steps >= 0) { "Number of steps should be non-negative." }
    val currentStep =
        remember(value, valueRange, steps) {
            RangeDefaults.snapValueToStep(value, valueRange, steps)
        }

    val updateValue: (Int) -> Unit = { stepDiff ->
        val newValue =
            RangeDefaults.calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
        if (newValue != value) onValueChange(newValue)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(VerticalSpacing)
    ) {
        val increaseButtonEnabled = enabled && (currentStep < steps + 1)
        val decreaseButtonEnabled = enabled && (currentStep > 0)

        StepperButton(
            onClick = { updateValue(1) },
            colors = colors,
            enabled = increaseButtonEnabled,
            contentAlignment = Alignment.TopCenter,
            paddingValues = PaddingValues(top = verticalContentPadding()),
            content = increaseIcon,
        )

        Box(
            modifier = Modifier.fillMaxWidth().weight(ContentWeight),
            contentAlignment = Alignment.Center,
            content = content
        )

        StepperButton(
            onClick = { updateValue(-1) },
            colors = colors,
            enabled = decreaseButtonEnabled,
            contentAlignment = Alignment.BottomCenter,
            paddingValues = PaddingValues(bottom = verticalContentPadding()),
            content = decreaseIcon,
        )
    }
}

@Composable
private fun ColumnScope.StepperButton(
    onClick: () -> Unit,
    contentAlignment: Alignment,
    paddingValues: PaddingValues,
    enabled: Boolean,
    colors: StepperColors,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconProviderValues = arrayOf(LocalContentColor provides colors.buttonIconColor(enabled))

    Box(
        modifier =
            Modifier.align(Alignment.CenterHorizontally)
                .weight(ButtonWeight)
                .padding(paddingValues),
    ) {
        Box(
            modifier =
                Modifier.align(contentAlignment)
                    .semantics { role = Role.Button }
                    .clip(CircleShape)
                    .repeatableClickable(
                        enabled = enabled,
                        onClick = onClick,
                        interactionSource = interactionSource,
                        indication = null
                    )
                    .size(width = ButtonWidth, height = ButtonHeight)
                    .background(color = colors.buttonContainerColor(enabled), shape = CircleShape)
                    .indication(interactionSource, ripple()),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(values = iconProviderValues, content = content)
        }
    }
}

/** Weight proportion of the space taken by the increase/decrease buttons. */
internal const val ButtonWeight = 0.35f

/** Weight proportion of the space taken by the content. */
internal const val ContentWeight = 0.3f

/** Width of the increase/decrease icon button. */
internal val ButtonWidth = 60.dp

/** Height of the increase/decrease icon button. */
internal val ButtonHeight = 48.dp

/** Vertical spacing between the elements of the [Stepper]. */
internal val VerticalSpacing = 8.dp

/** Vertical top/bottom padding. */
@Composable
internal fun verticalContentPadding(): Dp =
    LocalConfiguration.current.screenHeightDp.dp * 5.2f / 100
