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
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.scrollDown
import androidx.wear.compose.material3.macrobenchmark.common.scrollUp
import androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample

val OneHandedGestureScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = { OneHandedGestureTransformingLazyColumnSample() }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                // Scroll down and up to exercise the list, one-handed scroll gesture, and scroll
                // indicator
                repeat(3) {
                    device.scrollDown()
                    device.waitForIdle()
                }
                repeat(3) {
                    device.scrollUp()
                    device.waitForIdle()
                }
            }
    }
