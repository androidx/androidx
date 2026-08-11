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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.wear.compose.material3.macrobenchmark.common.FIND_OBJECT_TIMEOUT_MS
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.retryIfStale
import androidx.wear.compose.navigation3.samples.NavDisplayWithOnBackBehaviorSample

val Navigation3Screen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = { NavDisplayWithOnBackBehaviorSample() }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                repeat(2) {
                    retryIfStale {
                            requireNotNull(
                                device.wait(
                                    Until.findObject(By.text("Second")),
                                    FIND_OBJECT_TIMEOUT_MS,
                                )
                            ) {
                                "Button 'Second' not found"
                            }
                        }
                        .click()
                    check(device.wait(Until.gone(By.text("First")), FIND_OBJECT_TIMEOUT_MS)) {
                        "First screen still visible"
                    }
                    check(device.wait(Until.hasObject(By.text("Second")), FIND_OBJECT_TIMEOUT_MS)) {
                        "Destination 'Second' not found"
                    }
                    device.waitForIdle()
                    device.pressBack()
                    check(device.wait(Until.hasObject(By.text("First")), FIND_OBJECT_TIMEOUT_MS)) {
                        "Destination 'First' not found"
                    }
                    device.waitForIdle()
                }
            }
    }
