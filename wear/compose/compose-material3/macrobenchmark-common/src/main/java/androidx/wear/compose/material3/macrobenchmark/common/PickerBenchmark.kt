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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState

object PickerBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val items = listOf("One", "Two", "Three", "Four", "Five")
            val state = rememberPickerState(items.size)
            val contentDescription by remember {
                derivedStateOf { "${state.selectedOptionIndex + 1}" }
            }
            Picker(
                state = state,
                contentDescription = contentDescription,
            ) {
                Text(items[it])
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(20) {
                val endY = device.displayHeight * 9 / 10 // scroll down
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight / 2,
                    device.displayWidth / 2,
                    endY,
                    10
                )
                device.waitForIdle()
                SystemClock.sleep(500)
            }
        }
}
