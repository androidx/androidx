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

package androidx.compose.mpp.demo.bugs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// https://youtrack.jetbrains.com/issue/CMP-9030/wasm-canvas-incorrect-handling-of-multi-touch-input
@Composable
fun PointerInputDebugOverlay(
    modifier: Modifier = Modifier.fillMaxSize(),
    circleRadius: Dp = 50.dp,
    circleColor: Color = Color.Red,
) {
    // Keep a map of active pointer positions (keyed by pointer id value)
    val pointers = remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }
    val log = remember { mutableStateOf("") }

    fun logString(s: String) {
        log.value = s + "\n" + log.value
        if (log.value.length > 500) {
            log.value = log.value.take(500)
        }
    }

    Box(modifier = modifier) {
        Text(log.value)
        // overlay that listens to pointer events and draws circles
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    // Use awaitPointerEventScope to get raw pointer events and positions
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            logString(">")

                            // update positions for each pointer change
                            for (change in event.changes) {
                                val idValue = change.id.value
                                if (change.pressed) {
                                    // pointer is down or moved
                                    pointers.value += (idValue to change.position)
                                    logString("+ $idValue")
                                } else {
                                    // pointer was released
                                    logString("- $idValue")
                                    pointers.value -= idValue
                                }
                            }
                        }
                    }
                }
        ) {
            // Draw the active pointer locations
            Canvas(modifier = Modifier.matchParentSize()) {
                val radiusPx = circleRadius.toPx()
                for ((_, pos) in pointers.value) {
                    // draw a small red circle at each pointer position
                    drawCircle(
                        color = circleColor,
                        radius = radiusPx,
                        center = pos
                    )
                }
            }
        }
    }
}