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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout

@Composable
fun ConditionallyPlacedUiWithPointerInput(modifier: Modifier = Modifier) {
    var isPointerInputPlaced by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }
    var offset by remember { mutableFloatStateOf(0f) }
    Box(modifier.fillMaxSize().safeContentPadding()) {
        Box(
            Modifier.layout { measurable, constraints ->
                    measurable.measure(constraints).run {
                        layout(width, height) {
                            if (isPointerInputPlaced) {
                                place(0, 0)
                            }
                        }
                    }
                }
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onVerticalDrag = { _, amount -> offset += amount },
                    )
                }
        )

        Column {
            Button({ isPointerInputPlaced = !isPointerInputPlaced }) {
                Text("Toggle pointerInput placement")
            }
            Text("isPointerInputPlaced=$isPointerInputPlaced")
            Text("isDragging: $isDragging")
            Text("Offset: $offset")
        }
    }
}
