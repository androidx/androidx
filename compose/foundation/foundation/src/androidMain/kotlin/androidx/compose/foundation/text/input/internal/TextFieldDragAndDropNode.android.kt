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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.MediaType
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTargetModifierNode
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipMetadata
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata

internal actual fun textFieldDragAndDropNode(
    hintMediaTypes: () -> Set<MediaType>,
    onDrop: (clipEntry: ClipEntry, clipMetadata: ClipMetadata) -> Boolean,
    dragAndDropRequestPermission: (DragAndDropEvent) -> Unit,
    onStarted: ((event: DragAndDropEvent) -> Unit)?,
    onEntered: ((event: DragAndDropEvent) -> Unit)?,
    onMoved: ((position: Offset) -> Unit)?,
    onChanged: ((event: DragAndDropEvent) -> Unit)?,
    onExited: ((event: DragAndDropEvent) -> Unit)?,
    onEnded: ((event: DragAndDropEvent) -> Unit)?,
): DragAndDropTargetModifierNode {
    return DragAndDropTargetModifierNode(
        shouldStartDragAndDrop = { dragAndDropEvent ->
            // If there's a receiveContent modifier wrapping around this TextField, initially all
            // dragging items should be accepted for drop. This is expected to be met by the caller
            // of [textFieldDragAndDropNode] function.
            val clipDescription = dragAndDropEvent.toAndroidDragEvent().clipDescription
            hintMediaTypes().any {
                it == MediaType.All || clipDescription.hasMimeType(it.representation)
            }
        },
        target =
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    dragAndDropRequestPermission(event)
                    return onDrop.invoke(
                        event.toAndroidDragEvent().clipData.toClipEntry(),
                        event.toAndroidDragEvent().clipDescription.toClipMetadata()
                    )
                }

                override fun onStarted(event: DragAndDropEvent) = onStarted?.invoke(event) ?: Unit

                override fun onEntered(event: DragAndDropEvent) = onEntered?.invoke(event) ?: Unit

                override fun onMoved(event: DragAndDropEvent) =
                    with(event.toAndroidDragEvent()) { onMoved?.invoke(Offset(x, y)) ?: Unit }

                override fun onExited(event: DragAndDropEvent) = onExited?.invoke(event) ?: Unit

                override fun onChanged(event: DragAndDropEvent) = onChanged?.invoke(event) ?: Unit

                override fun onEnded(event: DragAndDropEvent) = onEnded?.invoke(event) ?: Unit
            }
    )
}
