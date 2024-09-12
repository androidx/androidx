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

@file:JvmMultifileClass
@file:JvmName("AndroidDragAndDropSource_androidKt")
@file:Suppress("DEPRECATION")

package androidx.compose.foundation.draganddrop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * A Modifier that allows an element it is applied to to be treated like a source for drag and drop
 * operations. It displays the element dragged as a drag shadow.
 *
 * Learn how to use [Modifier.dragAndDropSource]:
 *
 * @sample androidx.compose.foundation.samples.TextDragAndDropSourceSample
 * @param block A lambda with a [DragAndDropSourceScope] as a receiver which provides a
 *   [PointerInputScope] to detect the drag gesture, after which a drag and drop gesture can be
 *   started with [DragAndDropSourceScope.startTransfer].
 */
@Deprecated(
    message =
        "Replaced by overload with a callback for obtain a transfer data," +
            "start detection is performed by Compose itself",
    replaceWith = ReplaceWith("Modifier.dragAndDropSource(transferData)")
)
@ExperimentalFoundationApi
fun Modifier.dragAndDropSource(block: suspend DragAndDropSourceScope.() -> Unit): Modifier =
    this then
        LegacyDragAndDropSourceWithDefaultShadowElement(
            dragAndDropSourceHandler = block,
        )

@ExperimentalFoundationApi
private class LegacyDragAndDropSourceWithDefaultShadowElement(
    /** @see Modifier.dragAndDropSource */
    val dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : ModifierNodeElement<LegacyDragSourceNodeWithDefaultPainter>() {
    override fun create() =
        LegacyDragSourceNodeWithDefaultPainter(
            dragAndDropSourceHandler = dragAndDropSourceHandler,
        )

    override fun update(node: LegacyDragSourceNodeWithDefaultPainter) =
        with(node) {
            dragAndDropSourceHandler =
                this@LegacyDragAndDropSourceWithDefaultShadowElement.dragAndDropSourceHandler
        }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSourceWithDefaultPainter"
        properties["dragAndDropSourceHandler"] = dragAndDropSourceHandler
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LegacyDragAndDropSourceWithDefaultShadowElement) return false

        return dragAndDropSourceHandler == other.dragAndDropSourceHandler
    }

    override fun hashCode(): Int {
        return dragAndDropSourceHandler.hashCode()
    }
}

@ExperimentalFoundationApi
private class LegacyDragSourceNodeWithDefaultPainter(
    var dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : DelegatingNode() {

    init {
        val cacheDrawScopeDragShadowCallback =
            CacheDrawScopeDragShadowCallback().also {
                delegate(CacheDrawModifierNode(it::cachePicture))
            }
        delegate(
            LegacyDragAndDropSourceNode(
                drawDragDecoration = { cacheDrawScopeDragShadowCallback.drawDragShadow(this) },
                dragAndDropSourceHandler = { dragAndDropSourceHandler.invoke(this) }
            )
        )
    }
}
