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

package androidx.compose.ui.demos.gestures

import android.view.View
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.round
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup

/*
 * Moves UI to a Popup on long press which changes the top-level Compose container.
 */
@Composable
fun ContainerMovesContentToPopupOnDrag(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val movableContent = remember { movableContentOf(content) }
    var showPopup by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier =
            modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        showPopup = true
                        offset = Offset.Zero
                    },
                    onDrag = { _, deltaOffset -> offset += deltaOffset },
                    onDragCancel = {
                        showPopup = false
                        offset = Offset.Zero
                    },
                    onDragEnd = {
                        showPopup = false
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        if (showPopup) {
            Popup { Box(Modifier.offset { offset.round() }) { movableContent() } }
        } else {
            movableContent()
        }
    }
}

@Preview
@Composable
fun LongPressChangesHierarchyDemo() {
    MaterialTheme {
        ContainerMovesContentToPopupOnDrag {
            AndroidView(factory = ::View, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                it.setBackgroundColor(Color.Red.toArgb())
            }
        }
    }
}
