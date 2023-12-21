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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException

/**
 * Without shift it starts the new selection from scratch.
 * With shift it expands/shrinks existing selection.
 * A click sets the start and end of the selection,
 * but shift click only sets the end of the selection.
 */
internal interface MouseSelectionObserver {
    /**
     * Invoked on click (with shift).
     * @return if event will be consumed
     */
    fun onExtend(downPosition: Offset): Boolean

    /**
     * Invoked on drag after shift click.
     * @return if event will be consumed
     */
    fun onExtendDrag(dragPosition: Offset): Boolean

    /**
     * Invoked on first click (without shift).
     * @return if event will be consumed
     */
    // if returns true event will be consumed
    fun onStart(downPosition: Offset, adjustment: SelectionAdjustment): Boolean

    /**
     * Invoked when dragging (without shift).
     * @return if event will be consumed
     */
    fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean

    /**
     * Invoked when finishing a selection mouse gesture.
     */
    fun onDragDone()
}

// TODO(b/281584353) This is a stand in for updating the state in some global way.
//  For example, any touch/click in compose should change touch mode.
//  This only updates when the pointer is within the bounds of what it is modifying,
//  thus it is a placeholder until the other functionality is implemented.
private const val STATIC_KEY = 867_5309 // unique key to not clash with other global pointer inputs
internal fun Modifier.updateSelectionTouchMode(
    updateTouchMode: (Boolean) -> Unit
): Modifier = this.pointerInput(STATIC_KEY) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            updateTouchMode(!event.isPrecisePointer)
        }
    }
}

internal fun Modifier.selectionGestureInput(
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver,
) = this.pointerInput(mouseSelectionObserver, textDragObserver) {
    val clicksCounter = ClicksCounter(viewConfiguration)
    awaitEachGesture {
        val down = awaitDown()
        if (
            down.isPrecisePointer &&
            down.buttons.isPrimaryPressed &&
            down.changes.fastAll { !it.isConsumed }
        ) {
            mouseSelection(mouseSelectionObserver, clicksCounter, down)
        } else if (!down.isPrecisePointer) {
            touchSelection(textDragObserver, down)
        }
    }
}

private suspend fun AwaitPointerEventScope.touchSelection(
    observer: TextDragObserver,
    down: PointerEvent
) {
    try {
        val firstDown = down.changes.first()
        val drag = awaitLongPressOrCancellation(firstDown.id)
        if (drag != null && distanceIsTolerable(firstDown.position, drag.position)) {
            observer.onStart(drag.position)
            if (
                drag(drag.id) {
                    observer.onDrag(it.positionChange())
                    it.consume()
                }
            ) {
                // consume up if we quit drag gracefully with the up
                currentEvent.changes.fastForEach {
                    if (it.changedToUp()) it.consume()
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

private suspend fun AwaitPointerEventScope.mouseSelection(
    observer: MouseSelectionObserver,
    clicksCounter: ClicksCounter,
    down: PointerEvent
) {
    clicksCounter.update(down)
    val downChange = down.changes[0]
    if (down.isShiftPressed) {
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
        val selectionMode = when (clicksCounter.clicks) {
            // TODO(b/281585400) switch 1 to Character adjustment.
            //     This will result in multi text bugs,
            //     like a blank line selection resulting in a single char being selected.
            1 -> SelectionAdjustment.None
            2 -> SelectionAdjustment.Word
            else -> SelectionAdjustment.Paragraph
        }
        val started = observer.onStart(downChange.position, selectionMode)
        if (started) {
            val shouldConsumeUp = drag(downChange.id) {
                if (observer.onDrag(it.position, selectionMode)) {
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

internal const val ClicksSlop = 100.0

private class ClicksCounter(
    private val viewConfiguration: ViewConfiguration
) {
    var clicks = 0
    var prevClick: PointerInputChange? = null

    fun update(event: PointerEvent) {
        val currentPrevClick = prevClick
        val newClick = event.changes[0]
        if (currentPrevClick != null &&
            timeIsTolerable(currentPrevClick, newClick) &&
            positionIsTolerable(currentPrevClick, newClick)
        ) {
            clicks += 1
        } else {
            clicks = 1
        }
        prevClick = newClick
    }

    fun timeIsTolerable(prevClick: PointerInputChange, newClick: PointerInputChange): Boolean =
        newClick.uptimeMillis - prevClick.uptimeMillis < viewConfiguration.doubleTapTimeoutMillis

    fun positionIsTolerable(prevClick: PointerInputChange, newClick: PointerInputChange): Boolean =
        (newClick.position - prevClick.position).getDistance() < ClicksSlop
}

private suspend fun AwaitPointerEventScope.awaitDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (!event.changes.fastAll { it.changedToDownIgnoreConsumed() })
    return event
}

private fun AwaitPointerEventScope.distanceIsTolerable(offset1: Offset, offset2: Offset): Boolean =
    (offset1 - offset2).getDistance() < viewConfiguration.touchSlop

// TODO(b/281585410) this does not support touch pads as they have a pointer type of Touch
//             Supporting that will require public api changes
//             since the necessary info is in the ui module.
internal val PointerEvent.isPrecisePointer
    get() = this.changes.fastAll { it.type == PointerType.Mouse }
