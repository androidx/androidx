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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt

/**
 * [Stepper] allows users to make a selection from a range of values.
 * It's a full-screen control with increase button on the top, decrease button on the bottom and
 * a slot (expected to have either [Text] or [Chip]) in the middle.
 * Value can be increased and decreased by clicking on the increase and decrease buttons.
 * Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Step value is calculated as the difference between min and max values divided by [steps]+1.
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [PositionIndicator] if required.
 * If [value] is not equal to any step value, then it will be coerced to the closest step value.
 * However, the [value] itself will not be changed and [onValueChange] in this case will
 * not be triggered.
 *
 * @sample androidx.wear.compose.material.samples.StepperSample
 * @sample androidx.wear.compose.material.samples.StepperWithoutRangeSemanticsSample
 * @sample androidx.wear.compose.material.samples.StepperWithCustomSemanticsSample
 *
 * @param value Current value of the Stepper. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 * distributed across the whole value range. Must not be negative. If 0, stepper will have only
 * min and max values and no steps in between
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param modifier Modifiers for the Stepper layout
 * @param valueRange Range of values that Stepper value can take. Passed [value] will be coerced to
 * this range
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 * @param enableRangeSemantics Boolean to decide if range semantics should be enabled.
 * Set to false to disable default stepper range semantics. Alternatively to customize semantics
 * set this value as false and chain new semantics to the modifier.
 * @param content Content body for the Stepper.
 */
@Composable
fun Stepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    enableRangeSemantics: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    androidx.wear.compose.materialcore.Stepper(
        value = value,
        onValueChange = onValueChange,
        steps = steps,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        valueRange = valueRange,
        modifier = if (enableRangeSemantics) {
            modifier.rangeSemantics(
                value, true, onValueChange, valueRange, steps
            )
        } else {
            modifier
        },
        backgroundColor = backgroundColor,
        enabledButtonProviderValues = arrayOf(
            LocalContentColor provides iconColor,
            LocalContentAlpha provides iconColor.alpha
        ),
        disabledButtonProviderValues = arrayOf(
            LocalContentColor provides iconColor.copy(alpha = ContentAlpha.disabled),
            LocalContentAlpha provides iconColor.copy(alpha = ContentAlpha.disabled).alpha
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor
        ) {
            content()
        }
    }
}

/**
 * [Stepper] allows users to make a selection from a range of values.
 * It's a full-screen control with increase button on the top, decrease button on the bottom and
 * a slot (expected to have either [Text] or [Chip]) in the middle.
 * Value can be increased and decreased by clicking on the increase and decrease buttons.
 * Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [PositionIndicator] if required.
 *
 * @sample androidx.wear.compose.material.samples.StepperWithIntegerSample
 *
 * A number of steps is calculated as the difference between max and min values of
 * [valueProgression] divided by [valueProgression].step - 1.
 * For example, with a range of 100..120 and a step 5,
 * number of steps will be (120-100)/ 5 - 1 = 3. Steps are 100(first), 105, 110, 115, 120(last)
 *
 * If [valueProgression] range is not equally divisible by [valueProgression].step,
 * then [valueProgression].last will be adjusted to the closest divisible value in the range.
 * For example, 1..13 range and a step = 5, steps will be 1(first) , 6 , 11(last)
 *
 * If [value] is not equal to any step value, then it will be coerced to the closest step value.
 * However, the [value] itself will not be changed and [onValueChange] in this case will
 * not be triggered.
 *
 * @param value Current value of the Stepper. If outside of [valueProgression] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param valueProgression Progression of values that Stepper value can take. Consists of
 * rangeStart, rangeEnd and step. Range will be equally divided by step size
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param modifier Modifiers for the Stepper layout
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 * @param enableRangeSemantics Boolean to decide if default stepper semantics should be enabled.
 * Set to false to disable default stepper range semantics. Alternatively to customize semantics
 * set this value as false and chain new semantics to the modifier.
 * @param content Content body for the Stepper.
 */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    enableRangeSemantics: Boolean = true,
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
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        iconColor = iconColor,
        enableRangeSemantics = enableRangeSemantics,
        content = content
    )
}

/**
 * [Stepper] allows users to make a selection from a range of values.
 * It's a full-screen control with increase button on the top, decrease button on the bottom and
 * a slot (expected to have either [Text] or [Chip]) in the middle.
 * Value can be increased and decreased by clicking on the increase and decrease buttons.
 * Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Step value is calculated as the difference between min and max values divided by [steps]+1.
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [PositionIndicator] if required.
 *
 * @sample androidx.wear.compose.material.samples.StepperSample
 *
 * @param value Current value of the Stepper. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 * distributed across the whole value range. Must not be negative. If 0, stepper will have only
 * min and max values and no steps in between
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param modifier Modifiers for the Stepper layout
 * @param valueRange Range of values that Stepper value can take. Passed [value] will be coerced to
 * this range
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 */
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1. " +
        "A newer overload is available with an additional enableDefaultSemantics parameter.",
    level = DeprecationLevel.HIDDEN
)
@Composable
public fun Stepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    content: @Composable BoxScope.() -> Unit
) = Stepper(
    value = value,
    onValueChange = onValueChange,
    steps = steps,
    decreaseIcon = decreaseIcon,
    increaseIcon = increaseIcon,
    modifier = modifier,
    valueRange = valueRange,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    iconColor = iconColor,
    enableRangeSemantics = true,
    content = content
)

/**
 * [Stepper] allows users to make a selection from a range of values.
 * It's a full-screen control with increase button on the top, decrease button on the bottom and
 * a slot (expected to have either [Text] or [Chip]) in the middle.
 * Value can be increased and decreased by clicking on the increase and decrease buttons.
 * Buttons can have custom icons - [decreaseIcon] and [increaseIcon].
 * Stepper itself doesn't show the current value but can be displayed via the content slot or
 * [PositionIndicator] if required.
 *
 * @sample androidx.wear.compose.material.samples.StepperWithIntegerSample
 *
 * A number of steps is calculated as the difference between max and min values of
 * [valueProgression] divided by [valueProgression].step - 1.
 * For example, with a range of 100..120 and a step 5,
 * number of steps will be (120-100)/ 5 - 1 = 3. Steps are 100(first), 105, 110, 115, 120(last)
 *
 * If [valueProgression] range is not equally divisible by [valueProgression].step,
 * then [valueProgression].last will be adjusted to the closest divisible value in the range.
 * For example, 1..13 range and a step = 5, steps will be 1(first) , 6 , 11(last)
 *
 * @param value Current value of the Stepper. If outside of [valueProgression] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param valueProgression Progression of values that Stepper value can take. Consists of
 * rangeStart, rangeEnd and step. Range will be equally divided by step size
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param modifier Modifiers for the Stepper layout
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 */
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1. " +
        "A newer overload is available with an additional enableDefaultSemantics parameter.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    content: @Composable BoxScope.() -> Unit
) {
    Stepper(
        value = value,
        onValueChange = onValueChange,
        valueProgression = valueProgression,
        decreaseIcon = decreaseIcon,
        increaseIcon = increaseIcon,
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        iconColor = iconColor,
        enableRangeSemantics = true,
        content = content
    )
}

/**
 * Defaults used by stepper
 */
public object StepperDefaults {
    /**
     * Decrease [ImageVector]
     */
    public val Decrease = androidx.wear.compose.materialcore.RangeIcons.Minus

    /**
     * Increase [ImageVector]
     */
    public val Increase = Icons.Filled.Add
}
