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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox

object SwipeToDismissBoxBenchmark : MacrobenchmarkScreen {
    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val state = rememberSwipeToDismissBoxState()

            LaunchedEffect(state.currentValue) {
                if (state.currentValue == SwipeToDismissValue.Dismissed) {
                    state.snapTo(SwipeToDismissValue.Default)
                }
            }

            SwipeToDismissBox(
                modifier = Modifier.semantics { contentDescription = CONTENT_DESCRIPTION },
                state = state,
            ) { isBackground ->
                if (isBackground) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                    )
                } else {
                    Box(
                        modifier =
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            val swipeToDismiss = device.findObject(By.desc(CONTENT_DESCRIPTION))
            repeat(2) {
                swipeToDismiss.swipe(Direction.RIGHT, 1f, 500)
                device.waitForIdle()
                // Wait 250ms so SwipeToDismissBox state can be reset and ready for next swipe
                SystemClock.sleep(250)
            }
        }
}
