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

package androidx.compose.foundation.draganddrop

import android.graphics.Picture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

/**
 * A Modifier that allows an element it is applied to to be treated like a source for
 * drag and drop operations. It displays the element dragged as a drag shadow.
 *
 * Learn how to use [Modifier.dragAndDropSource]:
 * @sample androidx.compose.foundation.samples.TextDragAndDropSourceSample
 *
 * @param block A lambda with a [DragAndDropSourceScope] as a receiver
 * which provides a [PointerInputScope] to detect the drag gesture, after which a drag and drop
 * gesture can be started with [DragAndDropSourceScope.startTransfer].
 *
 */
@ExperimentalFoundationApi
fun Modifier.dragAndDropSource(
    block: suspend DragAndDropSourceScope.() -> Unit
): Modifier = this then DragAndDropSourceWithDefaultShadowElement(
    dragAndDropSourceHandler = block,
)

@ExperimentalFoundationApi
private class DragAndDropSourceWithDefaultShadowElement(
    /**
     * @see Modifier.dragAndDropSource
     */
    val dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : ModifierNodeElement<DragSourceNodeWithDefaultPainter>() {
    override fun create() = DragSourceNodeWithDefaultPainter(
        dragAndDropSourceHandler = dragAndDropSourceHandler,
    )

    override fun update(node: DragSourceNodeWithDefaultPainter) = with(node) {
        dragAndDropSourceHandler =
            this@DragAndDropSourceWithDefaultShadowElement.dragAndDropSourceHandler
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSourceWithDefaultPainter"
        properties["dragAndDropSourceHandler"] = dragAndDropSourceHandler
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DragAndDropSourceWithDefaultShadowElement) return false

        return dragAndDropSourceHandler == other.dragAndDropSourceHandler
    }

    override fun hashCode(): Int {
        return dragAndDropSourceHandler.hashCode()
    }
}

@ExperimentalFoundationApi
private class DragSourceNodeWithDefaultPainter(
    var dragAndDropSourceHandler: suspend DragAndDropSourceScope.() -> Unit
) : DelegatingNode() {

    init {
        val cacheDrawScopeDragShadowCallback = CacheDrawScopeDragShadowCallback().also {
            delegate(CacheDrawModifierNode(it::cachePicture))
        }
        delegate(
            DragAndDropSourceNode(
                drawDragDecoration = {
                    cacheDrawScopeDragShadowCallback.drawDragShadow(this)
                },
                dragAndDropSourceHandler = {
                    dragAndDropSourceHandler.invoke(this)
                }
            )
        )
    }
}

@ExperimentalFoundationApi
private class CacheDrawScopeDragShadowCallback {
    private var cachedPicture: Picture? = null

    fun drawDragShadow(drawScope: DrawScope) = with(drawScope) {
        when (val picture = cachedPicture) {
            null -> throw IllegalArgumentException(
                "No cached drag shadow. Check if Modifier.cacheDragShadow(painter) was called."
            )

            else -> drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawPicture(picture)
            }
        }
    }

    fun cachePicture(scope: CacheDrawScope): DrawResult = with(scope) {
        val picture = Picture()
        cachedPicture = picture
        val width = this.size.width.toInt()
        val height = this.size.height.toInt()
        onDrawWithContent {
            val pictureCanvas =
                androidx.compose.ui.graphics.Canvas(picture.beginRecording(width, height))
            draw(
                density = this,
                layoutDirection = this.layoutDirection,
                canvas = pictureCanvas,
                size = this.size
            ) {
                this@onDrawWithContent.drawContent()
            }
            picture.endRecording()

            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
        }
    }
}
