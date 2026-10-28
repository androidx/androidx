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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.modifier.DrawWithContentModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.runtime.Composable

internal class RemoteCanvasNode : RemoteComposeNode() {
    var onDraw: (RemoteDrawScope.() -> Unit) = {}

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val scope = overriddenScope(creationState)
        val recordingModifier = scope.toRecordingModifier(modifier)
        creationState.document.startCanvas(recordingModifier)

        val drawModifiers =
            modifier.foldIn(mutableListOf<DrawWithContentModifier>()) { acc, element ->
                if (element is DrawWithContentModifier) {
                    acc.add(element)
                }
                acc
            }

        if (drawModifiers.isNotEmpty()) {
            var currentContent: RemoteDrawScope.() -> Unit = onDraw
            for (i in drawModifiers.lastIndex downTo 0) {
                val drawModifier = drawModifiers[i]
                val nextContent = currentContent
                currentContent = {
                    drawModifier.onDraw(RemoteContentDrawScope(remoteCanvas, nextContent))
                }
            }
            RemoteDrawScope(remoteCanvas).currentContent()
        } else {
            val drawScope = RemoteDrawScope(remoteCanvas)
            drawScope.onDraw()
        }

        remoteCanvas.flush()

        creationState.document.endCanvas()
    }
}

/**
 * A Composable that provides a [RemoteDrawScope] for drawing operations in RemoteCompose.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteCanvasSample
 * @sample androidx.compose.remote.creation.compose.samples.RemoteCanvasAnimationSample
 * @param modifier The [RemoteModifier] to apply to this layout.
 * @param onDraw The drawing commands to be executed on the remote canvas via [RemoteDrawScope].
 */
@RemoteComposable
@Composable
public fun RemoteCanvas(
    modifier: RemoteModifier = RemoteModifier,
    onDraw: RemoteDrawScope.() -> Unit,
) {
    RemoteComposeNode(
        factory = ::RemoteCanvasNode,
        update = {
            set(modifier) { this.modifier = it }
            set(onDraw) { this.onDraw = it }
        },
    )
}
