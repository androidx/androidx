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

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text

object ScrollIndicatorBenchmark : MacrobenchmarkScreen {

    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val numberOfItems = 1000
            val scrollState = rememberLazyListState(numberOfItems / 2)
            Box {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    state = scrollState,
                ) {
                    items(numberOfItems) { Text(text = "Item $it") }
                }

                ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = scrollState)
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(10) {
                // scroll down
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight / 2,
                    device.displayWidth / 2,
                    device.displayHeight * 9 / 10,
                    10,
                )
                device.waitForIdle()
                // scroll up
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 9 / 10,
                    device.displayWidth / 2,
                    device.displayHeight / 2,
                    10,
                )
                device.waitForIdle()
                SystemClock.sleep(500)
            }
        }
}
