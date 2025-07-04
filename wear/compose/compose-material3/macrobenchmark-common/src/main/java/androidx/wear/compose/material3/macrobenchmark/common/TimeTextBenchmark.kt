/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText

object TimeTextBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                var showTimeText by remember { mutableStateOf(true) }

                Button(
                    onClick = { showTimeText = !showTimeText },
                    modifier = Modifier.semantics { contentDescription = TOGGLE_DISPLAY },
                ) {
                    Text("Toggle")
                }
                if (showTimeText) {
                    TimeText { time -> timeTextCurvedText(time) }
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(2) {
                device
                    .wait(Until.findObject(By.desc(TOGGLE_DISPLAY)), FIND_OBJECT_TIMEOUT_MS)
                    .click()
                device.waitForIdle()
            }
        }

    private const val TOGGLE_DISPLAY = "TOGGLE_DISPLAY"
}
