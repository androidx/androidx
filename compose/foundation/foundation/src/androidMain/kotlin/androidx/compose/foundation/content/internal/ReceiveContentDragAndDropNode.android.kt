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

package androidx.compose.foundation.content.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.platform.toClipMetadata

@OptIn(ExperimentalFoundationApi::class)
internal actual fun ReceiveContentDragAndDropNode(
    receiveContentConfiguration: ReceiveContentConfiguration,
    dragAndDropRequestPermission: (DragAndDropEvent) -> Unit
): DragAndDropModifierNode {
    return DragAndDropModifierNode(
        shouldStartDragAndDrop = {
            // accept any dragging item. The actual decider will be the onReceive callback.
            true
        },
        target =
            object : DragAndDropTarget {
                override fun onStarted(event: DragAndDropEvent) {
                    receiveContentConfiguration.receiveContentListener.onDragStart()
                }

                override fun onEnded(event: DragAndDropEvent) {
                    receiveContentConfiguration.receiveContentListener.onDragEnd()
                }

                override fun onEntered(event: DragAndDropEvent) {
                    receiveContentConfiguration.receiveContentListener.onDragEnter()
                }

                override fun onExited(event: DragAndDropEvent) {
                    receiveContentConfiguration.receiveContentListener.onDragExit()
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    dragAndDropRequestPermission(event)

                    val original = event.toTransferableContent()
                    val remaining =
                        receiveContentConfiguration.receiveContentListener.onReceive(original)
                    return original != remaining
                }
            }
    )
}

@OptIn(ExperimentalFoundationApi::class)
internal fun DragAndDropEvent.toTransferableContent(): TransferableContent {
    return with(toAndroidDragEvent()) {
        TransferableContent(
            clipEntry = clipData.toClipEntry(),
            clipMetadata = clipDescription.toClipMetadata(),
            source = TransferableContent.Source.DragAndDrop
        )
    }
}
