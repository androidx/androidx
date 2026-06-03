/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.awaitSelectionGestures
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.bringIntoView
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@Suppress("ModifierFactoryExtensionFunction")
internal fun SelectionRegistrar.makeSkikoSelectionModifier(
    selectableId: Long,
    layoutCoordinatesProvider: () -> LayoutCoordinates?,
): Modifier {
    return SkikoSelectionModifierElement(
        selectionRegistrar = this,
        selectableId = selectableId,
        layoutCoordinates = layoutCoordinatesProvider,
    )
}

internal class SkikoSelectionModifierElement(
    private val selectionRegistrar: SelectionRegistrar,
    private val selectableId: Long,
    private val layoutCoordinates: () -> LayoutCoordinates?,
) : ModifierNodeElement<SkikoSelectionModifierNode>() {

    override fun create() =
        SkikoSelectionModifierNode(
            selectionRegistrar = selectionRegistrar,
            selectableId = selectableId,
            layoutCoordinates = layoutCoordinates,
        )

    override fun update(node: SkikoSelectionModifierNode) {
        node.update(
            selectionRegistrar = selectionRegistrar,
            selectableId = selectableId,
            layoutCoordinates = layoutCoordinates,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "selection"
        properties["selectableId"] = selectableId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkikoSelectionModifierElement) return false

        return selectionRegistrar == other.selectionRegistrar &&
            selectableId == other.selectableId &&
            layoutCoordinates === other.layoutCoordinates
    }

    override fun hashCode(): Int {
        var result = selectableId.hashCode()
        result = 31 * result + selectionRegistrar.hashCode()
        result = 31 * result + layoutCoordinates.hashCode()
        return result
    }
}

internal class SkikoSelectionModifierNode(
    private var selectionRegistrar: SelectionRegistrar,
    private var selectableId: Long,
    private var layoutCoordinates: () -> LayoutCoordinates?,
) : DelegatingNode() {

    private var pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            awaitSelectionGestures(mouseSelectionObserver, longPressDragObserver)
        }
    )

    private val longPressDragObserver = object : TextDragObserver {
        /**
         * The beginning position of the drag gesture. Every time a new drag gesture starts, it
         * will be recalculated.
         */
        var lastPosition = Offset.Zero

        /**
         * The total distance being dragged of the drag gesture. Every time a new drag gesture
         * starts, it will be zeroed out.
         */
        var dragTotalDistance = Offset.Zero

        var selectionAdjustmentMode = SelectionAdjustment.None

        override fun onDown(point: Offset) {
            // Not supported for long-press-drag.
        }

        override fun onUp() {
            // Nothing to do.
        }

        override fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment) {
            selectionAdjustmentMode = selectionAdjustment
            layoutCoordinates()?.let {
                if (!it.isAttached) return

                selectionRegistrar.notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = startPoint,
                    adjustment = selectionAdjustmentMode,
                    isInTouchMode = true,
                )

                lastPosition = startPoint
            }
            // selection never started
            if (!selectionRegistrar.hasSelection(selectableId)) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return
                // selection never started, did not consume any drag
                if (!selectionRegistrar.hasSelection(selectableId)) return

                dragTotalDistance += delta
                val newPosition = lastPosition + dragTotalDistance

                // Notice that only the end position needs to be updated here.
                // Start position is left unchanged. This is typically important when
                // long-press is using SelectionAdjustment.WORD or
                // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                // the dragBeginPosition.
                val consumed =
                    selectionRegistrar.notifySelectionUpdate(
                        layoutCoordinates = it,
                        previousPosition = lastPosition,
                        newPosition = newPosition,
                        isStartHandle = false,
                        adjustment = selectionAdjustmentMode,
                        isInTouchMode = true,
                    )
                if (consumed) {
                    lastPosition = newPosition
                    dragTotalDistance = Offset.Zero
                }
            }
        }

        override fun onStop() {
            if (selectionRegistrar.hasSelection(selectableId)) {
                selectionRegistrar.notifySelectionUpdateEnd()
            }
        }

        override fun onCancel() {
            if (selectionRegistrar.hasSelection(selectableId)) {
                selectionRegistrar.notifySelectionUpdateEnd()
            }
        }
    }

    private fun createMouseSelectionObserver() = selectionRegistrar.skikoMouseSelectionObserver(
        selectableId = selectableId,
        layoutCoordinates = layoutCoordinates,
        bringIntoView = ::bringIntoView
    )

    private var mouseSelectionObserver = createMouseSelectionObserver()

    private fun bringIntoView(offset: Offset) {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            bringIntoView {
                Rect(offset = offset, size = Size.Zero)
            }
        }
    }

    fun update(
        selectionRegistrar: SelectionRegistrar,
        selectableId: Long,
        layoutCoordinates: () -> LayoutCoordinates?,
    ) {
        this.selectionRegistrar = selectionRegistrar
        this.selectableId = selectableId
        this.layoutCoordinates = layoutCoordinates

        mouseSelectionObserver = createMouseSelectionObserver()
        pointerInputNode.resetPointerInputHandler()
    }
}

internal fun SelectionRegistrar.skikoMouseSelectionObserver(
    selectableId: Long,
    layoutCoordinates: () -> LayoutCoordinates?,
    bringIntoView: (Offset) -> Unit,
) : MouseSelectionObserver {
    return object : MouseSelectionObserver {
        var lastPosition = Offset.Zero

        override fun onExtend(downPosition: Offset): Boolean {
            layoutCoordinates()?.let { layoutCoordinates ->
                if (!layoutCoordinates.isAttached) return false
                val consumed =
                    notifySelectionUpdate(
                        layoutCoordinates = layoutCoordinates,
                        newPosition = downPosition,
                        previousPosition = lastPosition,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.None,
                        isInTouchMode = false,
                    )
                if (consumed) {
                    lastPosition = downPosition
                }

                bringIntoView(downPosition)

                return hasSelection(selectableId)
            }
            return false
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            layoutCoordinates()?.let { layoutCoordinates ->
                if (!layoutCoordinates.isAttached) return false
                if (!hasSelection(selectableId)) return false

                val consumed =
                    notifySelectionUpdate(
                        layoutCoordinates = layoutCoordinates,
                        newPosition = dragPosition,
                        previousPosition = lastPosition,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.None,
                        isInTouchMode = false,
                    )
                if (consumed) {
                    lastPosition = dragPosition
                }

                bringIntoView(dragPosition)
            }
            return true
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment,
            clickCount: Int,
        ): Boolean {
            layoutCoordinates()?.let {
                if (!it.isAttached) return false

                notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = downPosition,
                    adjustment = adjustment,
                    isInTouchMode = false,
                )

                lastPosition = downPosition

                bringIntoView(downPosition)

                return hasSelection(selectableId)
            }

            return false
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            layoutCoordinates()?.let {
                if (!it.isAttached) return false
                if (!hasSelection(selectableId)) return false

                val consumed =
                    notifySelectionUpdate(
                        layoutCoordinates = it,
                        previousPosition = lastPosition,
                        newPosition = dragPosition,
                        isStartHandle = false,
                        adjustment = adjustment,
                        isInTouchMode = false,
                    )
                if (consumed) {
                    lastPosition = dragPosition
                }

                bringIntoView(dragPosition)
            }
            return true
        }

        override fun onDragDone() {
            notifySelectionUpdateEnd()
        }
    }
}