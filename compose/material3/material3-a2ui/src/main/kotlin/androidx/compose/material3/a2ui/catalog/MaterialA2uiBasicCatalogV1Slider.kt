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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Slider"` component. */
internal object MaterialA2uiBasicCatalogV1Slider : A2uiBasicCatalogV1.Slider {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        label: String?,
        min: Float,
        max: Float,
        value: Float,
        onValueChange: (Float) -> Unit,
        enabled: Boolean,
        modifier: Modifier,
    ) {
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
            val coercedValue = value.coerceIn(min, max)
            val valueRange = min..max
            val steps = (max.toInt() - min.toInt() - 1).coerceAtLeast(0)

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
                    Text(text = coercedValue.roundToInt().toString())
                }

                val sliderState =
                    remember(steps, valueRange) {
                        SliderState(value = coercedValue, steps = steps, trackRange = valueRange)
                    }
                sliderState.value = coercedValue

                Slider(
                    state = sliderState,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    track = { state ->
                        SliderDefaults.Track(
                            sliderState = state,
                            enabled = enabled,
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
private val EmptySliderTrack: DrawScope.(Offset, Color) -> Unit = { _, _ ->
    /* no-op to hide step dots */
}
