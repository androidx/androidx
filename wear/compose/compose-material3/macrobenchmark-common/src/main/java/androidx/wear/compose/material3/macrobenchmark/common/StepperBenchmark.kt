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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperLevelIndicator
import androidx.wear.compose.material3.Text

object StepperBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            var value by remember { mutableFloatStateOf(2f) }
            val valueRange = remember { 0f..4f }
            Box(modifier = Modifier.fillMaxSize()) {
                Stepper(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = valueRange,
                    steps = 7,
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
                ) {
                    Text(String.format("Value: %.1f".format(value)))
                }
                StepperLevelIndicator(
                    value = { value },
                    valueRange = valueRange,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(8) {
                device
                    .wait(Until.findObject(By.desc(INCREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(50)
                SystemClock.sleep(50)
            }

            repeat(8) {
                device
                    .wait(Until.findObject(By.desc(DECREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(50)
                SystemClock.sleep(50)
            }
        }

    private const val INCREASE_BUTTON = "INCREASE_BUTTON"
    private const val DECREASE_BUTTON = "DECREASE_BUTTON"
}
