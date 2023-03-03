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

package androidx.compose.ui.draganddrop

import android.content.ClipData
import android.view.DragEvent
import android.view.View
import androidx.collection.ArraySet
import androidx.compose.ui.geometry.Offset

/**
 * [DragAndDropTransfer] representation for the Android platform.
 * It provides the [ClipData] required for drag and drop.
 */
actual class DragAndDropTransfer(
    /**
     * The [ClipData] being transferred.
     */
    val clipData: ClipData,
    /**
     * Optional local state for the DnD operation
     * @see [View.startDragAndDrop]
     */
    val localState: Any? = null,
    /**
     * Flags for the drag and drop operation.
     * @see [View.startDragAndDrop]
     */
    val flags: Int = 0,
)

/**
 * Android [DragAndDropEvent] which delegates to a [DragEvent]
 */
actual class DragAndDropEvent(
    internal val dragEvent: DragEvent,
    internal actual val interestedNodes: ArraySet<DragAndDropModifierNode> = ArraySet()
)

/**
 * Returns the backing [DragEvent] to read platform specific data
 */
val DragAndDropEvent.dragEvent: DragEvent get() = this.dragEvent

/**
 * The mime types present in a [DragAndDropEvent]
 */
// TODO (TJ) make this expect/actual when desktop implements
fun DragAndDropEvent.mimeTypes(): Set<String> {
    val clipDescription = dragEvent.clipDescription ?: return emptySet()
    return buildSet(clipDescription.mimeTypeCount) {
        for (i in 0 until clipDescription.mimeTypeCount) {
            add(clipDescription.getMimeType(i))
        }
    }
}

internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = Offset(
        x = dragEvent.x,
        y = dragEvent.y
    )
