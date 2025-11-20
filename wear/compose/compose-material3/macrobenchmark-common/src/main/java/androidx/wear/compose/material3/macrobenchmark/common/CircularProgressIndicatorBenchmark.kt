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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.Text

object CircularProgressIndicatorBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val progress = remember { mutableFloatStateOf(0f) }

            Box(
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                        .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Slider(
                        value = progress.floatValue,
                        onValueChange = { progress.floatValue = it },
                        valueRange = 0f..2f,
                        steps = 9,
                        colors =
                            SliderDefaults.sliderColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                        segmented = false,
                        decreaseIcon = {
                            Text(
                                text = "-",
                                modifier =
                                    Modifier.semantics { contentDescription = DECREASE_BUTTON },
                            )
                        },
                        increaseIcon = {
                            Text(
                                text = "+",
                                modifier =
                                    Modifier.semantics { contentDescription = INCREASE_BUTTON },
                            )
                        },
                    )
                }

                CircularProgressIndicator(
                    progress = { progress.floatValue },
                    allowProgressOverflow = true,
                )
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(8) {
                device
                    .wait(Until.findObject(By.desc(INCREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click()
                SystemClock.sleep(500)
            }

            repeat(8) {
                device
                    .wait(Until.findObject(By.desc(DECREASE_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click()
                SystemClock.sleep(500)
            }
        }

    private const val INCREASE_BUTTON = "INCREASE_BUTTON"
    private const val DECREASE_BUTTON = "DECREASE_BUTTON"
}
