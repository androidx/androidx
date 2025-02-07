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
import android.view.InputDevice
import android.view.MotionEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

object RotaryScrollBenchmark : MacrobenchmarkScreen {
    private var itemHeightDp: Dp = 60.dp
    private var defaultItemSpacingDp: Dp = 8.dp

    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            MaterialTheme {
                TransformingLazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(
                            space = defaultItemSpacingDp,
                            alignment = Alignment.CenterVertically
                        ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
                    state = rememberTransformingLazyColumnState(),
                ) {
                    items(5000) { it ->
                        Box(Modifier.requiredHeight(itemHeightDp).fillMaxSize()) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "Item $it",
                            )
                        }
                    }
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            InstrumentationRegistry.getInstrumentation().uiAutomation.apply {
                repeat(10) {
                    injectInputEvent(rotaryEvent(-10f), true)
                    SystemClock.sleep(50)
                }
            }
        }

    private fun rotaryEvent(scrollPixels: Float): MotionEvent {
        return MotionEvent.obtain(
            /* downTime = */ 0,
            /* eventTime = */ System.currentTimeMillis(),
            /* action = */ MotionEvent.ACTION_SCROLL,
            /* pointerCount = */ 1,
            /* pointerProperties = */ arrayOf(
                MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = MotionEvent.TOOL_TYPE_UNKNOWN
                }
            ),
            /* pointerCoords = */ arrayOf(
                MotionEvent.PointerCoords().apply {
                    setAxisValue(MotionEvent.AXIS_SCROLL, scrollPixels)
                }
            ),
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 1f,
            /* yPrecision = */ 1f,
            /* deviceId = */ getRotaryInputDevice(),
            /* edgeFlags = */ 0,
            /* source = */ InputDevice.SOURCE_ROTARY_ENCODER,
            /* flags = */ 0
        )
    }
}
