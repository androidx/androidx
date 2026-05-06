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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DelegatingNode

internal class SelectionModifierNode(
    private var selectionRegistrar: SelectionRegistrar,
    private var selectableId: Long,
    private var layoutCoordinatesProvider: () -> LayoutCoordinates?,
) : DelegatingNode() {

    private var pointerInputNode =
        delegate(
            SuspendingPointerInputModifierNode {
                awaitSelectionGestures(mouseSelectionObserver, longPressDragObserver)
            }
        )

    private var longPressDragObserver = createLongPressDragObserver()

    private fun createLongPressDragObserver() =
        selectionRegistrar.DefaultLongPressDragObserver(
            selectableIdProvider = { selectableId },
            layoutCoordinatesProvider =
                // `layoutCoordinatesProvider` is a var, hence the lambda, to refer to the latest
                @Suppress("UnnecessaryLambdaCreation") { layoutCoordinatesProvider() },
        )

    private var mouseSelectionObserver = createMouseSelectionObserver()

    private fun createMouseSelectionObserver() =
        selectionRegistrar.DefaultMouseSelectionObserver(
            selectableIdProvider = { selectableId },
            layoutCoordinatesProvider =
                // `layoutCoordinatesProvider` is a var, hence the lambda, to refer to the latest
                @Suppress("UnnecessaryLambdaCreation") { layoutCoordinatesProvider() },
        )

    fun update(
        selectionRegistrar: SelectionRegistrar,
        selectableId: Long,
        layoutCoordinatesProvider: () -> LayoutCoordinates?,
    ) {
        val selectionRegistrarChanged = selectionRegistrar != this.selectionRegistrar

        this.selectionRegistrar = selectionRegistrar
        this.selectableId = selectableId
        this.layoutCoordinatesProvider = layoutCoordinatesProvider

        // When the SelectionRegistrar itself changes (which should be very rare), recreate the
        // input observers altogether (the alternative would be to pass them the registrar via a
        // lambda).
        // The reason we don't just use nested objects for the two observers is that having them
        // in separate functions allows them to be reused in CMP.
        if (selectionRegistrarChanged) {
            longPressDragObserver = createLongPressDragObserver()
            mouseSelectionObserver = createMouseSelectionObserver()
        }

        pointerInputNode.resetPointerInputHandler()
    }
}

internal fun SelectionRegistrar.DefaultLongPressDragObserver(
    selectableIdProvider: () -> Long,
    layoutCoordinatesProvider: () -> LayoutCoordinates?,
): TextDragObserver {
    return object : TextDragObserver {
        val selectableId: Long
            get() = selectableIdProvider()

        /** The beginning position of the current unconsumed drag movement. */
        var lastPosition = Offset.Unspecified

        /**
         * The position of the element relative to root at the start of the current unconsumed drag
         * movement.
         *
         * Tracking this is needed because of b/343917640, to add the scrolled delta to the
         * movement. When that bug is fixed, this will not be needed.
         */
        var lastOffsetInRoot = Offset.Unspecified

        /** The total distance of the unconsumed drag movement. */
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
            layoutCoordinatesProvider()?.let {
                if (!it.isAttached) return

                notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = startPoint,
                    adjustment = selectionAdjustmentMode,
                    isInTouchMode = true,
                )

                lastPosition = startPoint
                lastOffsetInRoot = it.localToRoot(Offset.Zero)
            }
            // selection never started
            if (!hasSelection(selectableId)) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset) {
            layoutCoordinatesProvider()?.let {
                if (!it.isAttached) return
                // selection never started, did not consume any drag
                if (!hasSelection(selectableId)) return

                dragTotalDistance += delta
                val scrollOffset = it.localToRoot(Offset.Zero) - lastOffsetInRoot
                val newPosition = lastPosition + dragTotalDistance - scrollOffset

                // Notice that only the end position needs to be updated here.
                // Start position is left unchanged. This is typically important when
                // long-press is using SelectionAdjustment.WORD or
                // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                // the dragBeginPosition.
                val consumed =
                    notifySelectionUpdate(
                        layoutCoordinates = it,
                        previousPosition = lastPosition,
                        newPosition = newPosition,
                        isStartHandle = false,
                        adjustment = selectionAdjustmentMode,
                        isInTouchMode = true,
                    )
                if (consumed) {
                    lastPosition = newPosition
                    lastOffsetInRoot = it.localToRoot(Offset.Zero)
                    dragTotalDistance = Offset.Zero
                }
            }
        }

        override fun onStop() {
            if (hasSelection(selectableId)) {
                notifySelectionUpdateEnd()
            }
            lastPosition = Offset.Unspecified
            lastOffsetInRoot = Offset.Unspecified
        }

        override fun onCancel() {
            if (hasSelection(selectableId)) {
                notifySelectionUpdateEnd()
            }
            lastPosition = Offset.Unspecified
            lastOffsetInRoot = Offset.Unspecified
        }
    }
}

internal fun SelectionRegistrar.DefaultMouseSelectionObserver(
    selectableIdProvider: () -> Long,
    layoutCoordinatesProvider: () -> LayoutCoordinates?,
): MouseSelectionObserver {
    return object : MouseSelectionObserver {
        val selectableId: Long
            get() = selectableIdProvider()

        var lastPosition = Offset.Zero

        override fun onExtend(downPosition: Offset): Boolean {
            layoutCoordinatesProvider()?.let { layoutCoordinates ->
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

                return hasSelection(selectableId)
            }
            return false
        }

        override fun onExtendDrag(dragPosition: Offset): Boolean {
            layoutCoordinatesProvider()?.let { layoutCoordinates ->
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
            }
            return true
        }

        override fun onStart(
            downPosition: Offset,
            adjustment: SelectionAdjustment,
            clickCount: Int,
        ): Boolean {
            layoutCoordinatesProvider()?.let {
                if (!it.isAttached) return false

                notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = downPosition,
                    adjustment = adjustment,
                    isInTouchMode = false,
                )

                lastPosition = downPosition

                return hasSelection(selectableId)
            }

            return false
        }

        override fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean {
            layoutCoordinatesProvider()?.let {
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
            }
            return true
        }

        override fun onDragDone() {
            notifySelectionUpdateEnd()
        }
    }
}
