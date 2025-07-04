/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.macrobenchmark.common

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.Text

object SliderBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val initialValue = 3f
            var value by remember { mutableFloatStateOf(initialValue) }
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = 1f..4f,
                steps = 7,
                segmented = true,
                colors =
                    if (value == initialValue) {
                        SliderDefaults.sliderColors()
                    } else {
                        SliderDefaults.variantSliderColors()
                    },
                decreaseIcon = {
                    Text(
                        text = "-",
                        modifier = Modifier.semantics { contentDescription = DECREASE_BUTTON },
                    )
                },
                increaseIcon = {
                    Text(
                        text = "+",
                        modifier = Modifier.semantics { contentDescription = INCREASE_BUTTON },
                    )
                },
            )
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(3) {
                device
                    .wait(Until.findObject(By.desc(DECREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(50)
                SystemClock.sleep(50)
            }
            repeat(3) {
                device
                    .wait(Until.findObject(By.desc(INCREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(50)
                SystemClock.sleep(50)
            }
        }

    private const val DECREASE_BUTTON = "DECREASE_BUTTON"
    private const val INCREASE_BUTTON = "INCREASE_BUTTON"
}
