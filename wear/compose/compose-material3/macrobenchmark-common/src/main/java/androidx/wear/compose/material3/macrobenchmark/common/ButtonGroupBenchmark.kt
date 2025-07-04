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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Text

object ButtonGroupBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                ButtonGroup(Modifier.fillMaxWidth()) {
                    Button(
                        modifier = Modifier.semantics { contentDescription = LEFT_BUTTON },
                        onClick = {},
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Left")
                        }
                    }
                    Button(
                        modifier = Modifier.semantics { contentDescription = RIGHT_BUTTON },
                        onClick = {},
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Right")
                        }
                    }
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(3) {
                device
                    .wait(Until.findObject(By.desc(LEFT_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(150)
                SystemClock.sleep(250)

                device
                    .wait(Until.findObject(By.desc(RIGHT_BUTTON)), FIND_OBJECT_TIMEOUT_MS)
                    .click(150)
                SystemClock.sleep(250)
            }
        }

    private const val LEFT_BUTTON = "LEFT_BUTTON"
    private const val RIGHT_BUTTON = "RIGHT_BUTTON"
}
