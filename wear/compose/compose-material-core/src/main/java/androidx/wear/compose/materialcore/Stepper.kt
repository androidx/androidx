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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
 * @param enabledButtonProviderValues Values of CompositionLocal providers for enabled button such
 * as LocalContentColor, LocalContentAlpha, LocalTextStyle which are dependent on a specific
 * material design version and are not part of this material-agnostic library.
 * @param disabledButtonProviderValues Values of CompositionLocal providers for disabled button such
 * as LocalContentColor, LocalContentAlpha, LocalTextStyle which are dependent on a specific
 * material design version and are not part of this material-agnostic library.
 * @param content Content body for the Stepper.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun Stepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    decreaseIcon: @Composable () -> Unit,
    increaseIcon: @Composable () -> Unit,
    modifier: Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    backgroundColor: Color,
    enabledButtonProviderValues: Array<ProvidedValue<*>>,
    disabledButtonProviderValues: Array<ProvidedValue<*>>,
    content: @Composable BoxScope.() -> Unit
) {
    require(steps >= 0) { "steps should be >= 0" }
    val currentStep = remember(value, valueRange, steps) {
        RangeDefaults.snapValueToStep(
            value, valueRange, steps
        )
    }

    val updateValue: (Int) -> Unit = { stepDiff ->
        val newValue =
            RangeDefaults.calculateCurrentStepValue(currentStep + stepDiff, steps, valueRange)
        if (newValue != value) onValueChange(newValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val increaseButtonEnabled = currentStep < steps + 1
        val decreaseButtonEnabled = currentStep > 0

        // Increase button.
        FullScreenButton(
            onClick = { updateValue(1) },
            contentAlignment = Alignment.TopCenter,
            paddingValues = PaddingValues(top = StepperDefaults.BorderPadding),
            enabled = increaseButtonEnabled,
            buttonProviderValues = if (increaseButtonEnabled) enabledButtonProviderValues
            else disabledButtonProviderValues,
            content = increaseIcon
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(StepperDefaults.ContentWeight),
            contentAlignment = Alignment.Center,
            content = content
        )
        // Decrease button.
        FullScreenButton(
            onClick = { updateValue(-1) },
            contentAlignment = Alignment.BottomCenter,
            paddingValues = PaddingValues(bottom = StepperDefaults.BorderPadding),
            enabled = decreaseButtonEnabled,
            buttonProviderValues = if (decreaseButtonEnabled) enabledButtonProviderValues
            else disabledButtonProviderValues,
            content = decreaseIcon
        )
    }
}

@Composable
private fun ColumnScope.FullScreenButton(
    onClick: () -> Unit,
    contentAlignment: Alignment,
    paddingValues: PaddingValues,
    enabled: Boolean,
    buttonProviderValues: Array<ProvidedValue<*>>,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(StepperDefaults.ButtonWeight)
            .repeatableClickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            )
            .wrapContentWidth()
            .indication(interactionSource, rememberRipple(bounded = false))
            .padding(paddingValues),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(
            values = buttonProviderValues, content = content
        )
    }
}

/**
 * Defaults used by stepper
 */
private object StepperDefaults {
    const val ButtonWeight = 0.35f
    const val ContentWeight = 0.3f
    val BorderPadding = 22.dp
}
