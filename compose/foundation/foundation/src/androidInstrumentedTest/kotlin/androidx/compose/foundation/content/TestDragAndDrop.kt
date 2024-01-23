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
import android.content.ClipDescription
import android.net.Uri
import android.view.DragEvent
import android.view.View
import androidx.compose.foundation.text2.input.internal.DragAndDropTestUtils
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density

/**
 * A helper scope creator to test multi-window Drag And Drop interactions.
 */
internal fun testDragAndDrop(view: View, density: Density, block: DragAndDropScope.() -> Unit) {
    DragAndDropScopeImpl(view, density).block()
}

@Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")
private class DragAndDropScopeImpl(
    val view: View,
    density: Density
) : DragAndDropScope, Density by density {
    private var lastDraggingItem: Pair<Offset, Any>? = null

    override fun drag(
        offset: Offset,
        item: Any,
    ) {
        val _lastDraggingItem = lastDraggingItem
        if (_lastDraggingItem == null || _lastDraggingItem.second != item) {
            view.dispatchDragEvent(
                makeDragEvent(DragEvent.ACTION_DRAG_STARTED, item)
            )
        }
        view.dispatchDragEvent(
            makeDragEvent(
                DragEvent.ACTION_DRAG_LOCATION,
                item = item,
                offset = offset
            )
        )
        lastDraggingItem = offset to item
    }

    override fun drop() {
        val _lastDraggingItem = lastDraggingItem
        check(_lastDraggingItem != null) { "There are no ongoing dragging event to drop" }

        view.dispatchDragEvent(
            makeDragEvent(
                DragEvent.ACTION_DROP,
                item = _lastDraggingItem.second,
                offset = _lastDraggingItem.first
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

            is List<*> -> {
                val mimeTypes = mutableSetOf<String>()
                val clipDataItems = mutableListOf<ClipData.Item>()
                item.filterNotNull().forEach { actualItem ->
                    when (actualItem) {
                        is String -> {
                            mimeTypes.add(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            clipDataItems.add(ClipData.Item(actualItem))
                        }

                        is Uri -> {
                            mimeTypes.add("image/*")
                            clipDataItems.add(ClipData.Item(actualItem))
                        }
                    }
                }
                DragAndDropTestUtils.makeDragEvent(
                    action = action,
                    items = clipDataItems,
                    mimeTypes = mimeTypes.toList(),
                    offset = offset
                )
            }

            else -> {
                DragAndDropTestUtils.makeImageDragEvent(action, offset = offset)
            }
        }
    }
}

internal interface DragAndDropScope : Density {

    /**
     * Drags an item which represent the payload to the [offset] location.
     *
     * @param item Should either be a [String] or a [Uri]. It can also be a [List] of [String]s or
     * [Uri]s.
     */
    fun drag(offset: Offset, item: Any)

    /**
     * Drops the previously declared dragging item.
     */
    fun drop()

    /**
     * Cancels the ongoing drag without dropping it.
     */
    fun cancelDrag()
}
