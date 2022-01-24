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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.RangeDefaults.calculateCurrentStepValue
import androidx.wear.compose.material.RangeDefaults.snapValueToStep
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
 *
 * @sample androidx.wear.compose.material.samples.StepperSample
 *
 * @param value Current value of the Stepper. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param steps Specifies the number of discrete values, excluding min and max values, evenly
 * distributed across the whole value range. Must not be negative. If 0, stepper will have only
 * min and max values and no steps in between
 * @param modifier Modifiers for the Stepper layout
 * @param valueRange Range of values that Stepper value can take. Passed [value] will be coerced to
 * this range
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconTintColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 */
@Composable
public fun Stepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    decreaseIcon: @Composable () -> Unit = { StepperDefaults.DecreaseIcon() },
    increaseIcon: @Composable () -> Unit = { StepperDefaults.IncreaseIcon() },
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconTintColor: Color = contentColor,
    content: @Composable BoxScope.() -> Unit
) {
    require(steps >= 0) { "steps should be >= 0" }
    val currentStep =
        remember(value, valueRange, steps) { snapValueToStep(value, valueRange, steps) }

    val updateValue: (Int) -> Unit = { stepDiff ->
        val newValue = calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
        if (newValue != value) onValueChange(newValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .rangeSemantics(
                currentStep,
                true,
                onValueChange,
                valueRange,
                steps
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FullScreenButton(
            onClick = { updateValue(1) },
            contentAlignment = Alignment.TopCenter,
            paddingValues = PaddingValues(top = StepperDefaults.BorderPadding),
            iconTintColor = iconTintColor,
            content = increaseIcon
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(StepperDefaults.ContentWeight),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
        FullScreenButton(
            onClick = { updateValue(-1) },
            contentAlignment = Alignment.BottomCenter,
            paddingValues = PaddingValues(bottom = StepperDefaults.BorderPadding),
            content = decreaseIcon,
            iconTintColor = iconTintColor
        )
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
 * @param value Current value of the Stepper. If outside of [valueProgression] provided, value will be
 * coerced to this range.
 * @param onValueChange Lambda in which value should be updated
 * @param modifier Modifiers for the Stepper layout
 * @param valueProgression Progression of values that Stepper value can take. Consists of
 * rangeStart, rangeEnd and step. Range will be equally divided by step size
 * @param decreaseIcon A slot for an icon which is placed on the decrease (bottom) button
 * @param increaseIcon A slot for an icon which is placed on the increase (top) button
 * @param backgroundColor [Color] representing the background color for the stepper.
 * @param contentColor [Color] representing the color for [content] in the middle.
 * @param iconTintColor Icon tint [Color] which used by [increaseIcon] and [decreaseIcon]
 * that defaults to [contentColor], unless specifically overridden.
 */
@Composable
public fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueProgression: IntProgression,
    decreaseIcon: @Composable () -> Unit = { StepperDefaults.DecreaseIcon() },
    increaseIcon: @Composable () -> Unit = { StepperDefaults.IncreaseIcon() },
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconTintColor: Color = contentColor,
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
        iconTintColor = iconTintColor,
        content = content
    )
}

@Composable
private fun ColumnScope.FullScreenButton(
    onClick: () -> Unit,
    contentAlignment: Alignment,
    paddingValues: PaddingValues,
    iconTintColor: Color,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(StepperDefaults.ButtonWeight)
            .clickable(interactionSource, null, onClick = onClick)
            .wrapContentWidth()
            .indication(interactionSource, rememberRipple(bounded = false))
            .padding(paddingValues),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides iconTintColor, content = content)
    }
}

/**
 * Defaults used by stepper
 */
private object StepperDefaults {
    const val ButtonWeight = 0.35f
    const val ContentWeight = 0.3f
    val BorderPadding = 8.dp

    @Composable
    fun DecreaseIcon() {
        Icon(
            imageVector = RangeIcons.Minus,
            contentDescription = "Decrease", // TODO(b/204187777) i18n
            modifier = Modifier.padding(14.dp)
        )
    }

    @Composable
    fun IncreaseIcon() {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Increase", // TODO(b/204187777) i18n
            modifier = Modifier.padding(14.dp)
        )
    }
}
