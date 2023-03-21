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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.detectDragGesturesAfterLongPressWithObserver
import androidx.compose.foundation.text.isInTouchMode
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.MultiWidgetSelectionDelegate
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.foundation.text.selection.mouseSelectionDetector
import androidx.compose.foundation.text.textPointerHoverIcon
import androidx.compose.foundation.text.textPointerIcon
import androidx.compose.runtime.RememberObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow

internal open class StaticTextSelectionParams(
    val layoutCoordinates: LayoutCoordinates?,
    val textLayoutResult: TextLayoutResult?
) {
    companion object {
        val Empty = StaticTextSelectionParams(null, null)
    }

    open fun getPathForRange(start: Int, end: Int): Path? {
        return textLayoutResult?.getPathForRange(start, end)
    }

    open val shouldClip: Boolean
        get() = textLayoutResult?.layoutInput?.overflow == TextOverflow.Visible

    // if this copy shows up in traces, this class may become mutable
    fun copy(
        layoutCoordinates: LayoutCoordinates? = this.layoutCoordinates,
        textLayoutResult: TextLayoutResult? = this.textLayoutResult
    ): StaticTextSelectionParams {
        return StaticTextSelectionParams(
            layoutCoordinates,
            textLayoutResult
        )
    }
}

/**
 * Holder for selection modifiers while we wait for pointerInput to be ported to new modifiers.
 */
// This is _basically_ a Modifier.Node but moved into remember because we need to do pointerInput
internal class SelectionController(
    private val selectionRegistrar: SelectionRegistrar,
    private val backgroundSelectionColor: Color,
    // TODO: Move these into Modifer.element eventually
    private var params: StaticTextSelectionParams = StaticTextSelectionParams.Empty
) : RememberObserver {
    private var selectable: Selectable? = null
    private val selectableId = selectionRegistrar.nextSelectableId()

    val modifier: Modifier = selectionRegistrar.makeSelectionModifier(
        selectableId = selectableId,
        layoutCoordinates = { params.layoutCoordinates },
        textLayoutResult = { params.textLayoutResult },
        isInTouchMode = isInTouchMode
    ).textPointerHoverIcon(selectionRegistrar)

    override fun onRemembered() {
        selectable = selectionRegistrar.subscribe(
            MultiWidgetSelectionDelegate(
                selectableId = selectableId,
                coordinatesCallback = { params.layoutCoordinates },
                layoutResultCallback = { params.textLayoutResult }
            )
        )
    }

    override fun onForgotten() {
        val localSelectable = selectable
        if (localSelectable != null) {
            selectionRegistrar.unsubscribe(localSelectable)
            selectable = null
        }
    }

    override fun onAbandoned() {
        val localSelectable = selectable
        if (localSelectable != null) {
            selectionRegistrar.unsubscribe(localSelectable)
            selectable = null
        }
    }

    fun updateTextLayout(textLayoutResult: TextLayoutResult) {
        params = params.copy(textLayoutResult = textLayoutResult)
    }

    fun updateGlobalPosition(coordinates: LayoutCoordinates) {
        params = params.copy(layoutCoordinates = coordinates)
    }

    fun draw(drawScope: DrawScope) {
        val selection = selectionRegistrar.subselections[selectableId] ?: return

        val start = if (!selection.handlesCrossed) {
            selection.start.offset
        } else {
            selection.end.offset
        }
        val end = if (!selection.handlesCrossed) {
            selection.end.offset
        } else {
            selection.start.offset
        }

        if (start == end) return

        val lastOffset = selectable?.getLastVisibleOffset() ?: 0
        val clippedStart = start.coerceAtMost(lastOffset)
        val clippedEnd = end.coerceAtMost(lastOffset)

        val selectionPath = params.getPathForRange(clippedStart, clippedEnd) ?: return

        with(drawScope) {
            if (params.shouldClip) {
                clipRect {
                    drawPath(selectionPath, backgroundSelectionColor)
                }
            } else {
                drawPath(selectionPath, backgroundSelectionColor)
            }
        }
    }
}

// this is not chained, but is a standalone factory
@Suppress("ModifierFactoryExtensionFunction")
private fun SelectionRegistrar.makeSelectionModifier(
    selectableId: Long,
    layoutCoordinates: () -> LayoutCoordinates?,
    textLayoutResult: () -> TextLayoutResult?,
    isInTouchMode: Boolean
): Modifier {
    return if (isInTouchMode) {
        val longPressDragObserver = object : TextDragObserver {
            /**
             * The beginning position of the drag gesture. Every time a new drag gesture starts, it wil be
             * recalculated.
             */
            var lastPosition = Offset.Zero

            /**
             * The total distance being dragged of the drag gesture. Every time a new drag gesture starts,
             * it will be zeroed out.
             */
            var dragTotalDistance = Offset.Zero

            override fun onDown(point: Offset) {
                // Not supported for long-press-drag.
            }

            override fun onUp() {
                // Nothing to do.
            }

            override fun onStart(startPoint: Offset) {
                layoutCoordinates()?.let {
                    if (!it.isAttached) return

                    if (textLayoutResult().outOfBoundary(startPoint, startPoint)) {
                        notifySelectionUpdateSelectAll(
                            selectableId = selectableId
                        )
                    } else {
                        notifySelectionUpdateStart(
                            layoutCoordinates = it,
                            startPosition = startPoint,
                            adjustment = SelectionAdjustment.Word
                        )
                    }

                    lastPosition = startPoint
                }
                // selection never started
                if (!hasSelection(selectableId)) return
                // Zero out the total distance that being dragged.
                dragTotalDistance = Offset.Zero
            }

            override fun onDrag(delta: Offset) {
                layoutCoordinates()?.let {
                    if (!it.isAttached) return
                    // selection never started, did not consume any drag
                    if (!hasSelection(selectableId)) return

                    dragTotalDistance += delta
                    val newPosition = lastPosition + dragTotalDistance

                    if (!textLayoutResult().outOfBoundary(lastPosition, newPosition)) {
                        // Notice that only the end position needs to be updated here.
                        // Start position is left unchanged. This is typically important when
                        // long-press is using SelectionAdjustment.WORD or
                        // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                        // the dragBeginPosition.
                        val consumed = notifySelectionUpdate(
                            layoutCoordinates = it,
                            previousPosition = lastPosition,
                            newPosition = newPosition,
                            isStartHandle = false,
                            adjustment = SelectionAdjustment.CharacterWithWordAccelerate
                        )
                        if (consumed) {
                            lastPosition = newPosition
                            dragTotalDistance = Offset.Zero
                        }
                    }
                }
            }

            override fun onStop() {
                if (hasSelection(selectableId)) {
                    notifySelectionUpdateEnd()
                }
            }

            override fun onCancel() {
                if (hasSelection(selectableId)) {
                    notifySelectionUpdateEnd()
                }
            }
        }
        Modifier.pointerInput(longPressDragObserver) {
            detectDragGesturesAfterLongPressWithObserver(
                longPressDragObserver
            )
        }
    } else {
        val mouseSelectionObserver = object : MouseSelectionObserver {
            var lastPosition = Offset.Zero

            override fun onExtend(downPosition: Offset): Boolean {
                layoutCoordinates()?.let { layoutCoordinates ->
                    if (!layoutCoordinates.isAttached) return false
                    val consumed = notifySelectionUpdate(
                        layoutCoordinates = layoutCoordinates,
                        newPosition = downPosition,
                        previousPosition = lastPosition,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.None
                    )
                    if (consumed) {
                        lastPosition = downPosition
                    }
                    return hasSelection(selectableId)
                }
                return false
            }

            override fun onExtendDrag(dragPosition: Offset): Boolean {
                layoutCoordinates()?.let { layoutCoordinates ->
                    if (!layoutCoordinates.isAttached) return false
                    if (!hasSelection(selectableId)) return false

                    val consumed = notifySelectionUpdate(
                        layoutCoordinates = layoutCoordinates,
                        newPosition = dragPosition,
                        previousPosition = lastPosition,
                        isStartHandle = false,
                        adjustment = SelectionAdjustment.None
                    )

                    if (consumed) {
                        lastPosition = dragPosition
                    }
                }
                return true
            }

            override fun onStart(
                downPosition: Offset,
                adjustment: SelectionAdjustment
            ): Boolean {
                layoutCoordinates()?.let {
                    if (!it.isAttached) return false

                    notifySelectionUpdateStart(
                        layoutCoordinates = it,
                        startPosition = downPosition,
                        adjustment = adjustment
                    )

                    lastPosition = downPosition
                    return hasSelection(selectableId)
                }

                return false
            }

            override fun onDrag(
                dragPosition: Offset,
                adjustment: SelectionAdjustment
            ): Boolean {
                layoutCoordinates()?.let {
                    if (!it.isAttached) return false
                    if (!hasSelection(selectableId)) return false

                    val consumed = notifySelectionUpdate(
                        layoutCoordinates = it,
                        previousPosition = lastPosition,
                        newPosition = dragPosition,
                        isStartHandle = false,
                        adjustment = adjustment
                    )
                    if (consumed) {
                        lastPosition = dragPosition
                    }
                }
                return true
            }
        }
        Modifier.pointerInput(mouseSelectionObserver) {
            mouseSelectionDetector(mouseSelectionObserver)
        }.pointerHoverIcon(textPointerIcon)
    }
}

private fun TextLayoutResult?.outOfBoundary(start: Offset, end: Offset): Boolean {
    this ?: return false

    val lastOffset = layoutInput.text.text.length
    val rawStartOffset = getOffsetForPosition(start)
    val rawEndOffset = getOffsetForPosition(end)

    return rawStartOffset >= lastOffset - 1 && rawEndOffset >= lastOffset - 1 ||
        rawStartOffset < 0 && rawEndOffset < 0
}
