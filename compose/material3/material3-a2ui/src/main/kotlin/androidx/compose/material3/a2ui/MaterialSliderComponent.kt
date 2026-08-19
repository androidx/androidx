/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Slider"` component schema.
 *
 * Displays a [Slider] for selecting an integer value from a range. A header row above the slider
 * displays an optional label on the start and the current selected value on the end.
 *
 * **Schema Properties:**
 * * `value` (Dynamic Number, required): The current integer value of the slider.
 * * `min` (Number, optional): The minimum integer value of the slider. Defaults to `0`.
 * * `max` (Number, required): The maximum integer value of the slider.
 * * `label` (Dynamic String, optional): The label for the slider.
 */
public object MaterialSliderComponent : A2uiComponent {

    private val valueProp =
        A2uiProperty.dynamicNumber(
            key = "value",
            required = true,
            description = "The current value of the slider.",
        )
    private val minProp =
        A2uiProperty.number(
            key = "min",
            required = false,
            description = "The minimum value of the slider.",
        )
    private val maxProp =
        A2uiProperty.number(
            key = "max",
            required = true,
            description = "The maximum value of the slider.",
        )
    private val labelProp =
        A2uiProperty.dynamicString(
            key = "label",
            required = false,
            description = "The label for the slider.",
        )

    override val name: String = "Slider"
    override val description: String = "A slider component for selecting a value from a range."
    override val properties: List<A2uiProperty<*>> = listOf(valueProp, minProp, maxProp, labelProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(valueProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val min = properties[minProp]?.toInt() ?: 0
        val max =
            checkNotNull(properties[maxProp]?.toInt()) {
                "Required property '${maxProp.key}' is missing."
            }
        val remoteValue =
            checkNotNull(properties.bind(valueProp)?.toInt()) {
                "Required property '${valueProp.key}' is missing."
            }
        val label = properties.bind(labelProp)
        val onRemoteValueChange = properties.bindUpdater(valueProp)
        val isEnabled = onRemoteValueChange != null

        // TODO(b/549060875): Figure out how this should be reflected in the UI: switch back to the
        //  loading state or show some kind of error.
        if (min > max) {
            SideEffect(min, max) {
                reportError(
                    A2uiException.A2uiRuntimeException(
                        "Min value cannot be greater than max value."
                    )
                )
            }
        } else {
            val coercedValue = remoteValue.coerceIn(min, max)
            val valueRange = min.toFloat()..max.toFloat()
            val steps = (max - min - 1).coerceAtLeast(0)

            Column(
                modifier = modifier.then(SliderBottomPaddingModifier),
                verticalArrangement = SliderVerticalArrangement,
            ) {
                Row(
                    modifier = SliderHeaderRowModifier,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (label != null) {
                        Text(text = label, modifier = Modifier.weight(1f, fill = false))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Text(text = coercedValue.toString())
                }

                Slider(
                    value = coercedValue.toFloat(),
                    onValueChange = { newValue ->
                        onRemoteValueChange?.invoke(newValue.roundToInt())
                    },
                    valueRange = valueRange,
                    steps = steps,
                    enabled = isEnabled,
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            enabled = isEnabled,
                            drawTick = EmptySliderTrack,
                        )
                    },
                )
            }
        }
    }
}

private val SliderVerticalArrangement = Arrangement.spacedBy(4.dp)
private val SliderHeaderRowModifier = Modifier.fillMaxWidth()
private val SliderBottomPaddingModifier = Modifier.padding(bottom = 8.dp)
private val EmptySliderTrack: DrawScope.(Offset, Color) -> Unit =
    { _, _ -> /* no-op to hide step dots */
    }
