/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.draganddrop

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropSourceModifierNode
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

/**
 * A scope that allows for the detection of the start of a drag and drop gesture, and subsequently
 * starting a drag and drop session.
 */
internal interface DragAndDropStartDetectorScope : PointerInputScope {
    /**
     * Requests a drag and drop transfer.
     *
     * @param offset the offset value representing position of the input pointer.
     */
    fun requestDragAndDropTransfer(offset: Offset = Offset.Unspecified)
}

/**
 * This typealias represents a suspend function with [DragAndDropStartDetectorScope] that is used to
 * detect the start of a drag and drop gesture and initiate a drag and drop session.
 */
internal typealias DragAndDropStartDetector = suspend DragAndDropStartDetectorScope.() -> Unit

/** Contains the default values used by [Modifier.dragAndDropSource]. */
@Immutable
internal expect object DragAndDropSourceDefaults {
    /**
     * The default start detector for drag and drop operations. It might vary on different
     * platforms.
     */
    val DefaultStartDetector: DragAndDropStartDetector
}

/**
 * A [Modifier] that allows an element it is applied to be treated like a source for drag and drop
 * operations. It displays the element dragged as a drag shadow.
 *
 * Learn how to use [Modifier.dragAndDropSource]:
 *
 * @sample androidx.compose.foundation.samples.TextDragAndDropSourceSample
 * @param transferData A function that receives the current offset of the drag operation and returns
 *   the [DragAndDropTransferData] to be transferred. If null is returned, the drag and drop
 *   transfer won't be started.
 */
fun Modifier.dragAndDropSource(transferData: (Offset) -> DragAndDropTransferData?): Modifier =
    this then
        DragAndDropSourceWithDefaultShadowElement(
            // TODO: Expose this as public argument
            detectDragStart = DragAndDropSourceDefaults.DefaultStartDetector,
            transferData = transferData
        )

/**
 * A [Modifier] that allows an element it is applied to be treated like a source for drag and drop
 * operations.
 *
 * Learn how to use [Modifier.dragAndDropSource] while providing a custom drag shadow:
 *
 * @sample androidx.compose.foundation.samples.DragAndDropSourceWithColoredDragShadowSample
 * @param drawDragDecoration provides the visual representation of the item dragged during the drag
 *   and drop gesture.
 * @param transferData A function that receives the current offset of the drag operation and returns
 *   the [DragAndDropTransferData] to be transferred. If null is returned, the drag and drop
 *   transfer won't be started.
 */
fun Modifier.dragAndDropSource(
    drawDragDecoration: DrawScope.() -> Unit,
    transferData: (Offset) -> DragAndDropTransferData?
): Modifier =
    this then
        DragAndDropSourceElement(
            drawDragDecoration = drawDragDecoration,
            // TODO: Expose this as public argument
            detectDragStart = DragAndDropSourceDefaults.DefaultStartDetector,
            transferData = transferData
        )

private data class DragAndDropSourceElement(
    /** @see Modifier.dragAndDropSource */
    val drawDragDecoration: DrawScope.() -> Unit,
    /** @see Modifier.dragAndDropSource */
    val detectDragStart: DragAndDropStartDetector,
    /** @see Modifier.dragAndDropSource */
    val transferData: (Offset) -> DragAndDropTransferData?
) : ModifierNodeElement<DragAndDropSourceNode>() {
    override fun create() =
        DragAndDropSourceNode(
            drawDragDecoration = drawDragDecoration,
            detectDragStart = detectDragStart,
            transferData = transferData
        )

    override fun update(node: DragAndDropSourceNode) =
        with(node) {
            drawDragDecoration = this@DragAndDropSourceElement.drawDragDecoration
            detectDragStart = this@DragAndDropSourceElement.detectDragStart
            transferData = this@DragAndDropSourceElement.transferData
        }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSource"
        properties["drawDragDecoration"] = drawDragDecoration
        properties["detectDragStart"] = detectDragStart
        properties["transferData"] = transferData
    }
}

internal class DragAndDropSourceNode(
    var drawDragDecoration: DrawScope.() -> Unit,
    var detectDragStart: DragAndDropStartDetector,
    var transferData: (Offset) -> DragAndDropTransferData?
) : DelegatingNode(), LayoutAwareModifierNode {

    private var size: IntSize = IntSize.Zero

    private val dragAndDropModifierNode =
        delegate(
            DragAndDropSourceModifierNode { offset ->
                val transferData = transferData(offset)
                if (transferData != null) {
                    startDragAndDropTransfer(
                        transferData = transferData,
                        decorationSize = size.toSize(),
                        drawDragDecoration = drawDragDecoration
                    )
                }
            }
        )

    private var inputModifierNode: PointerInputModifierNode? = null

    override fun onAttach() {
        if (dragAndDropModifierNode.isRequestDragAndDropTransferRequired) {
            inputModifierNode =
                delegate(
                    SuspendingPointerInputModifierNode {
                        detectDragStart(
                            object : DragAndDropStartDetectorScope, PointerInputScope by this {
                                override fun requestDragAndDropTransfer(offset: Offset) {
                                    dragAndDropModifierNode.requestDragAndDropTransfer(offset)
                                }
                            }
                        )
                    }
                )
        }
    }

    override fun onDetach() {
        inputModifierNode?.let { undelegate(it) }
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        dragAndDropModifierNode.onPlaced(coordinates)
    }

    override fun onRemeasured(size: IntSize) {
        this.size = size
        dragAndDropModifierNode.onRemeasured(size)
    }
}

private data class DragAndDropSourceWithDefaultShadowElement(
    /** @see Modifier.dragAndDropSource */
    var detectDragStart: DragAndDropStartDetector,
    /** @see Modifier.dragAndDropSource */
    var transferData: (Offset) -> DragAndDropTransferData?
) : ModifierNodeElement<DragSourceNodeWithDefaultPainter>() {
    override fun create() =
        DragSourceNodeWithDefaultPainter(
            detectDragStart = detectDragStart,
            transferData = transferData
        )

    override fun update(node: DragSourceNodeWithDefaultPainter) =
        with(node) {
            detectDragStart = this@DragAndDropSourceWithDefaultShadowElement.detectDragStart
            transferData = this@DragAndDropSourceWithDefaultShadowElement.transferData
        }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSourceWithDefaultPainter"
        properties["detectDragStart"] = detectDragStart
        properties["transferData"] = transferData
    }
}

private class DragSourceNodeWithDefaultPainter(
    detectDragStart: DragAndDropStartDetector,
    transferData: (Offset) -> DragAndDropTransferData?
) : DelegatingNode() {

    private val cacheDrawScopeDragShadowCallback =
        CacheDrawScopeDragShadowCallback().also {
            delegate(CacheDrawModifierNode(it::cachePicture))
        }

    private val dragAndDropModifierNode =
        delegate(
            DragAndDropSourceNode(
                drawDragDecoration = { cacheDrawScopeDragShadowCallback.drawDragShadow(this) },
                detectDragStart = detectDragStart,
                transferData = transferData
            )
        )

    var detectDragStart: DragAndDropStartDetector by dragAndDropModifierNode::detectDragStart
    var transferData: (Offset) -> DragAndDropTransferData? by dragAndDropModifierNode::transferData
}

internal expect class CacheDrawScopeDragShadowCallback() {
    fun drawDragShadow(drawScope: DrawScope)

    fun cachePicture(scope: CacheDrawScope): DrawResult
}
