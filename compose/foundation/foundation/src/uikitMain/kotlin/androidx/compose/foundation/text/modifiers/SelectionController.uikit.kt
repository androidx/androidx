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
import androidx.compose.foundation.text.selection.isPrecisePointer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.cancellation.CancellationException

private interface CupertinoTextDragObserver {
    fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment)
    fun onDrag(delta: Offset, selectionAdjustment: SelectionAdjustment)
    fun onStop()
    fun onCancel()
}

internal actual fun SelectionRegistrar.makeSelectionModifier(
    selectableId: Long,
    layoutCoordinates: () -> LayoutCoordinates?
): Modifier {
    val longPressDragObserver = object : CupertinoTextDragObserver {
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

        override fun onStart(startPoint: Offset, selectionAdjustment: SelectionAdjustment) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return

                notifySelectionUpdateStart(
                    layoutCoordinates = it,
                    startPosition = startPoint,
                    adjustment = selectionAdjustment,
                    isInTouchMode = true
                )

                lastPosition = startPoint
            }
            // selection never started
            if (!hasSelection(selectableId)) return
            // Zero out the total distance that being dragged.
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(delta: Offset, selectionAdjustment: SelectionAdjustment) {
            layoutCoordinates()?.let {
                if (!it.isAttached) return
                // selection never started, did not consume any drag
                if (!hasSelection(selectableId)) return

                dragTotalDistance += delta
                val newPosition = lastPosition + dragTotalDistance

                // Notice that only the end position needs to be updated here.
                // Start position is left unchanged. This is typically important when
                // long-press is using SelectionAdjustment.WORD or
                // SelectionAdjustment.PARAGRAPH that updates the start handle position from
                // the dragBeginPosition.
                val consumed = notifySelectionUpdate(
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

    // The rest of that method copied from SelectionController.kt
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
                    adjustment = SelectionAdjustment.None,
                    isInTouchMode = false
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
                    adjustment = SelectionAdjustment.None,
                    isInTouchMode = false
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
                    adjustment = adjustment,
                    isInTouchMode = false
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
                    adjustment = adjustment,
                    isInTouchMode = false
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

    return Modifier.selectionGestureInput(mouseSelectionObserver, longPressDragObserver)
}

// Copied from SelectionController.kt, except CupertinoTextDragObserver parameter
private fun Modifier.selectionGestureInput(
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: CupertinoTextDragObserver,
): Modifier = composed {
    // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-79) how we can rewrite this without `composed`?
    val currentMouseSelectionObserver by rememberUpdatedState(mouseSelectionObserver)
    val currentTextDragObserver by rememberUpdatedState(textDragObserver)
    this.pointerInput(Unit) {
        val clicksCounter = ClicksCounter(viewConfiguration)
        awaitEachGesture {
            val down = awaitDown()
            if (
                down.isPrecisePointer &&
                down.buttons.isPrimaryPressed &&
                down.changes.fastAll { !it.isConsumed }
            ) {
                mouseSelection(currentMouseSelectionObserver, clicksCounter, down)
            } else if (!down.isPrecisePointer) {
                touchSelection(currentTextDragObserver, clicksCounter, down)
            }
        }
    }
}

private suspend fun AwaitPointerEventScope.touchSelection(
    observer: CupertinoTextDragObserver,
    clicksCounter: ClicksCounter,
    down: PointerEvent
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

        val started = observer.onStart(downChange.position, selectionAdjustment)
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