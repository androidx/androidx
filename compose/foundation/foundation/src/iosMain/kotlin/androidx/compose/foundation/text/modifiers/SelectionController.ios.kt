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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.text.selection.ClicksCounter
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.foundation.text.selection.isMouseOrTouchPad
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private interface CupertinoTextDragObserver {
    fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment)
    fun onDrag(delta: Offset, selectionAdjustment: SelectionAdjustment)
    fun onStop()
    fun onCancel()
}

internal actual fun SelectionRegistrar.makeSelectionModifier(
    selectableId: Long,
    layoutCoordinatesProvider: () -> LayoutCoordinates?
): Modifier {
    return CupertinoSelectionModifierElement(
        selectionRegistrar = this,
        selectableId = selectableId,
        layoutCoordinates = layoutCoordinatesProvider,
    )
}

internal class CupertinoSelectionModifierElement(
    private val selectionRegistrar: SelectionRegistrar,
    private val selectableId: Long,
    private val layoutCoordinates: () -> LayoutCoordinates?,
) : ModifierNodeElement<CupertinoSelectionModifierNode>() {

    override fun create() =
        CupertinoSelectionModifierNode(
            selectionRegistrar = selectionRegistrar,
            selectableId = selectableId,
            layoutCoordinates = layoutCoordinates,
        )

    override fun update(node: CupertinoSelectionModifierNode) {
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
        if (other !is CupertinoSelectionModifierElement) return false

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

internal class CupertinoSelectionModifierNode(
    private var selectionRegistrar: SelectionRegistrar,
    private var selectableId: Long,
    private var layoutCoordinates: () -> LayoutCoordinates?,
) : DelegatingNode(), CompositionLocalConsumerModifierNode {

    private val pointerInputNode = delegate(
        SuspendingPointerInputModifierNode {
            val hapticFeedback = currentValueOf(LocalHapticFeedback)
            val clicksCounter = ClicksCounter(viewConfiguration)

            awaitEachGesture {
                val down = awaitDown()

                if (
                    down.isMouseOrTouchPad() &&
                    down.buttons.isPrimaryPressed &&
                    down.changes.fastAll { !it.isConsumed }
                ) {
                    mouseSelection(mouseSelectionObserver, clicksCounter, down)
                } else if (!down.isMouseOrTouchPad()) {
                    touchSelection(longPressDragObserver, clicksCounter, down, hapticFeedback)
                }
            }
        }
    )

    private val longPressDragObserver = object : CupertinoTextDragObserver {
        /**
         * The beginning position of the drag gesture. Every time a new drag gesture starts, it will
         * be recalculated.
         */
        var lastPosition = Offset.Zero

        /**
         * The total distance being dragged of the drag gesture. Every time a new drag gesture
         * starts, it will be zeroed out.
         */
        var dragTotalDistance = Offset.Zero

        override fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return

                selectionRegistrar.notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = startPoint,
                    adjustment = selectionAdjustment,
                    isInTouchMode = true
                )

                lastPosition = startPoint
            }
            // selection never started
            if (!selectionRegistrar.hasSelection(selectableId)) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset, selectionAdjustment: SelectionAdjustment) {
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
                val consumed = selectionRegistrar.notifySelectionUpdate(
                    layoutCoordinates = it,
                    previousPosition = lastPosition,
                    newPosition = delta,
                    isStartHandle = false,
                    adjustment = selectionAdjustment,
                    isInTouchMode = true
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

private suspend fun AwaitPointerEventScope.touchSelection(
    observer: CupertinoTextDragObserver,
    clicksCounter: ClicksCounter,
    down: PointerEvent,
    hapticFeedback: HapticFeedback?,
) {
    try {
        val firstDown = down.changes.first()
        val drag = awaitLongPressOrCancellation(firstDown.id)
        clicksCounter.update(firstDown)
        when (clicksCounter.clicks) {
            1 -> { /* Should be ignored without drag */ }
            2 -> {
                observer.onStart(firstDown.position, SelectionAdjustment.Word)
                observer.onStop()
            }
            else -> {
                observer.onStart(firstDown.position, SelectionAdjustment.Paragraph)
                observer.onStop()
            }
        }

        if (drag != null) {
            observer.onStart(firstDown.position, SelectionAdjustment.Word)
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
            if (
                drag(drag.id) {
                    observer.onDrag(it.position, SelectionAdjustment.CharacterWithWordAccelerate)
                }
            ) {
                currentEvent.changes.fastForEach {
                    if (it.changedToUp()) { it.consume() }
                }
                observer.onStop()
            } else {
                observer.onCancel()
            }
        }
    } catch (c: CancellationException) {
        observer.onCancel()
        throw c
    }
}

// Copied from SelectionGestures.kt
private suspend fun AwaitPointerEventScope.mouseSelection(
    observer: MouseSelectionObserver,
    clicksCounter: ClicksCounter,
    down: PointerEvent
) {
    val downChange = down.changes[0]
    clicksCounter.update(downChange)
    if (down.keyboardModifiers.isShiftPressed) {
        val started = observer.onExtend(downChange.position)
        if (started) {
            val shouldConsumeUp = drag(downChange.id) {
                if (observer.onExtendDrag(it.position)) {
                    it.consume()
                }
            }

            if (shouldConsumeUp) {
                currentEvent.changes.fastForEach {
                    if (it.changedToUp()) it.consume()
                }
            }

            observer.onDragDone()
        }
    } else {
        val selectionAdjustment = when (clicksCounter.clicks) {
            1 -> SelectionAdjustment.None
            2 -> SelectionAdjustment.Word
            else -> SelectionAdjustment.Paragraph
        }

        val started = observer.onStart(downChange.position, selectionAdjustment, clicksCounter.clicks)
        if (started) {
            val shouldConsumeUp = drag(downChange.id) {
                if (observer.onDrag(it.position, selectionAdjustment)) {
                    it.consume()
                }
            }

            if (shouldConsumeUp) {
                currentEvent.changes.fastForEach {
                    if (it.changedToUp()) it.consume()
                }
            }

            observer.onDragDone()
        }
    }
}

// Copied from SelectionGestures.kt
private suspend fun AwaitPointerEventScope.awaitDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (!event.changes.fastAll { it.changedToDownIgnoreConsumed() })
    return event
}