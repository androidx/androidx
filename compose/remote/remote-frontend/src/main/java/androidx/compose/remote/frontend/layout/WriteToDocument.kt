/*
 * Copyright (C) 2025 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.frontend.capture.LogTodo
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

/** Break glass mechanism to make calls direct to the RecordingCanvas or document. */
@Composable
fun WriteToDocument(message: String? = null, content: RecordingCanvas.() -> Unit) {
    LogTodo(message ?: "WriteToDocument used")

    RemoteCanvas(modifier = RemoteModifier.size(0.dp)) {
        val canvas = drawScope.drawContext.canvas.nativeCanvas

        if (canvas is RecordingCanvas) {
            content(canvas)
        }
    }
}

@Composable
fun RecordingCanvas(content: RecordingCanvas.() -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.drawBehind {
            drawIntoCanvas {
                if (it.nativeCanvas is RecordingCanvas) {
                    val canvas = it.nativeCanvas as RecordingCanvas
                    content(canvas)
                }
            }
        }
    )
}
