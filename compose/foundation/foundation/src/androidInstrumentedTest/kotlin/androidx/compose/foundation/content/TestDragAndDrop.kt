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

package androidx.compose.foundation.content

import android.content.ClipData
import android.net.Uri
import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.text2.input.internal.DragAndDropTestUtils
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density

/**
 * A helper to test multi-window Drag And Drop interactions.
 */
internal fun testDragAndDrop(view: View, density: Density, block: DragAndDropScope.() -> Unit) {
    DragAndDropScopeImpl(view, density).block()
}

internal interface DragAndDropScope : Density {

    /**
     * Drags an item with ClipData that only holds the given [text] to the [offset] location.
     */
    fun drag(offset: Offset, text: String): Boolean

    /**
     * Drags an item with ClipData that only holds the given [uri] to the [offset] location.
     */
    fun drag(offset: Offset, uri: Uri): Boolean

    /**
     * Drags an item with [clipData] payload to the [offset] location.
     */
    fun drag(offset: Offset, clipData: ClipData): Boolean

    /**
     * Drops the previously declared dragging item.
     */
    fun drop(): Boolean

    /**
     * Cancels the ongoing drag without dropping it.
     */
    fun cancelDrag()
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
private class DragAndDropScopeImpl(
    val view: View,
    density: Density
) : DragAndDropScope, Density by density {
    private var lastDraggingOffsetAndItem: Pair<Offset, Any>? = null

    override fun drag(offset: Offset, text: String): Boolean = dragAny(offset, text)

    override fun drag(offset: Offset, uri: Uri): Boolean = dragAny(offset, uri)

    override fun drag(offset: Offset, clipData: ClipData): Boolean = dragAny(offset, clipData)

    /**
     * @param item Can be only [String], [Uri], or [ClipData].
     */
    private fun dragAny(offset: Offset, item: Any): Boolean {
        val _lastDraggingItem = lastDraggingOffsetAndItem
        var result = false
        if (_lastDraggingItem == null || _lastDraggingItem.second != item) {
            result = view.dispatchDragEvent(
                makeDragEvent(action = DragEvent.ACTION_DRAG_STARTED, item = item)
            ) || result
        }
        result = view.dispatchDragEvent(
            makeDragEvent(
                action = DragEvent.ACTION_DRAG_LOCATION,
                item = item,
                offset = offset
            )
        ) || result
        lastDraggingOffsetAndItem = offset to item
        return result
    }

    override fun drop(): Boolean {
        val lastDraggingOffsetAndItem = lastDraggingOffsetAndItem
        check(lastDraggingOffsetAndItem != null) { "There are no ongoing dragging event to drop" }
        val (lastDraggingOffset, lastDraggingItem) = lastDraggingOffsetAndItem

        return view.dispatchDragEvent(
            makeDragEvent(
                DragEvent.ACTION_DROP,
                item = lastDraggingItem,
                offset = lastDraggingOffset
            )
        )
    }

    override fun cancelDrag() {
        view.dispatchDragEvent(
            DragAndDropTestUtils.makeTextDragEvent(DragEvent.ACTION_DRAG_ENDED)
        )
    }

    private fun makeDragEvent(
        action: Int,
        item: Any,
        offset: Offset = Offset.Zero
    ): DragEvent {
        return when (item) {
            is String -> {
                DragAndDropTestUtils.makeTextDragEvent(action, item, offset)
            }

            is Uri -> {
                DragAndDropTestUtils.makeImageDragEvent(action, item, offset)
            }

            is ClipData -> {
                DragAndDropTestUtils.makeDragEvent(action, item, offset)
            }

            else -> {
                throw IllegalArgumentException(
                    "{item=$item} can only be one of [String], [Uri], or [ClipData]"
                )
            }
        }
    }
}
