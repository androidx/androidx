/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.demos.performance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview

/**
 * This demo draws a straight line from where the screen was first touched to where the
 * pointer is now. It basically tracks drag operations, drawing a line between the start
 * and end positions of that drag. The intention of this app is to allow easy testing of
 * what Compose is doing during drag operations (allocations, etc).
 */
@Preview
@Composable
fun DrawingDemo() {
    val start = remember { mutableStateOf(Offset(0f, 0f)) }
    val end = remember { mutableStateOf(Offset(0f, 0f)) }
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(Color.White)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    start.value = it
                    end.value = it
                }
            ) { _, dragAmount ->
                end.value += dragAmount
            }
        }
    ) {
        drawLine(Color.Blue, start = start.value, end = end.value)
    }
}
