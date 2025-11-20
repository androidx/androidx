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
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.pointerSlop
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException

/**
 * Without shift it starts the new selection from scratch. With shift it expands/shrinks existing
 * selection. A click sets the start and end of the selection, but shift click only sets the end of
 * the selection.
 */
internal interface MouseSelectionObserver {
    /**
     * Invoked on click (with shift).
     *
     * @return if event will be consumed
     */
    fun onExtend(downPosition: Offset): Boolean

    /**
     * Invoked on drag after shift click.
     *
     * @return if event will be consumed
     */
    fun onExtendDrag(dragPosition: Offset): Boolean

    /**
     * Invoked on first click (without shift).
     *
     * @return if event will be consumed
     */
    // if returns true event will be consumed
    fun onStart(downPosition: Offset, adjustment: SelectionAdjustment, clickCount: Int): Boolean

    /**
     * Invoked when dragging (without shift).
     *
     * @return if event will be consumed
     */
    fun onDrag(dragPosition: Offset, adjustment: SelectionAdjustment): Boolean

    /** Invoked when finishing a selection mouse gesture. */
    fun onDragDone()
}

// TODO(b/281584353) This is a stand in for updating the state in some global way.
//  For example, any touch/click in compose should change touch mode.
//  This only updates when the pointer is within the bounds of what it is modifying,
//  thus it is a placeholder until the other functionality is implemented.
private const val STATIC_KEY = 867_5309 // unique key to not clash with other global pointer inputs

internal fun Modifier.updateSelectionTouchMode(updateTouchMode: (Boolean) -> Unit): Modifier =
    this.pointerInput(STATIC_KEY) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                updateTouchMode(!event.isMouseOrTouchPad())
            }
        }
    }

/**
 * Gesture handler for mouse and touch. Determines whether this is mouse or touch based on the first
 * down, then uses the gesture handler for that input type, delegating to the appropriate observer.
 * This handler is used by all text selection surfaces; SelectionContainer, BTF1, and BTF2.
 */
internal suspend fun PointerInputScope.awaitSelectionGestures(
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver,
) {
    val clicksCounter = ClicksCounter(viewConfiguration)
    awaitEachGesture {
        val downEvent = awaitDown()
        clicksCounter.update(downEvent)
        val isPrecise = downEvent.isMouseOrTouchPad()
        if (
            isPrecise &&
                downEvent.buttons.isPrimaryPressed &&
                downEvent.changes.fastAll { !it.isConsumed }
        ) {
            mouseSelection(mouseSelectionObserver, clicksCounter, downEvent)
        } else if (!isPrecise) {
            when (clicksCounter.clicks) {
                1 -> touchSelectionFirstPress(textDragObserver, downEvent)
                else ->
                    touchSelectionSubsequentPress(textDragObserver, downEvent, clicksCounter.clicks)
            }
        }
    }
}

/**
 * Gesture handler for touch selection on only the first press. The first press will wait for a long
 * press instead of immediately looking for drags. If no long press is found, this does not trigger
 * any observer.
 */
internal suspend fun AwaitPointerEventScope.touchSelectionFirstPress(
    observer: TextDragObserver,
    downEvent: PointerEvent,
) {
    try {
        val firstDown = downEvent.changes.first()
        val longPress = awaitLongPressOrCancellation(firstDown.id)
        if (longPress != null && distanceIsTolerable(viewConfiguration, firstDown, longPress)) {
            observer.onStart(longPress.position, SelectionAdjustment.Word)
            val dragCompletedWithUp =
                drag(longPress.id) {
                    observer.onDrag(it.positionChange())
                    it.consume()
                }
            if (dragCompletedWithUp) {
                // consume up if we quit drag gracefully with the up
                currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
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

private enum class DownResolution {
    Up,
    Drag,
    Timeout,
    Cancel,
}

/**
 * Gesture handler for touch selection on all presses except for the first. Subsequent presses
 * immediately starts looking for drags when the press is received.
 *
 * @param clicks How many clicks were registered in succession.
 */
private suspend fun AwaitPointerEventScope.touchSelectionSubsequentPress(
    observer: TextDragObserver,
    downEvent: PointerEvent,
    clicks: Int,
) {
    try {
        val firstDown = downEvent.changes.first()
        val pointerId = firstDown.id

        // With a subsequent click it is guaranteed that a selection is started.
        observer.onStart(
            firstDown.position,
            if (clicks > 2) SelectionAdjustment.Paragraph else SelectionAdjustment.Word,
        )

        var overSlop: Offset = Offset.Unspecified
        val downResolution =
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                val firstDragPastSlop =
                    awaitTouchSlopOrCancellation(pointerId) { change, slop ->
                        change.consume()
                        overSlop = slop
                    }

                // If slop is passed, we have started a drag.
                if (firstDragPastSlop != null && overSlop.isSpecified) {
                    return@withTimeoutOrNull DownResolution.Drag
                }

                // Otherwise, this either was cancelled or the pointer is now up.
                val currentChange = currentEvent.changes.first()
                return@withTimeoutOrNull if (currentChange.changedToUpIgnoreConsumed()) {
                    currentChange.consume()
                    DownResolution.Up
                } else {
                    DownResolution.Cancel
                }
            } ?: DownResolution.Timeout

        if (downResolution == DownResolution.Cancel) {
            // On a cancel, we simply take no further action.
            observer.onCancel()
            return
        }

        if (downResolution == DownResolution.Up) {
            // This is a tap, immediately stop and let the initiated selection remain.
            observer.onStop()
            return
        } else if (downResolution == DownResolution.Drag) {
            // Drag already begun, run a drag on the over-slop and then proceed to wait for drags.
            observer.onDrag(overSlop)
        }
        // Finally, if waitResult was a Timeout, then this was a long press. Simply wait for drags.

        val dragCompletedWithUp =
            drag(pointerId) {
                observer.onDrag(it.positionChange())
                it.consume()
            }

        if (dragCompletedWithUp) {
            // consume up if we quit drag gracefully with the up
            currentEvent.changes.fastForEach {
                if (it.changedToUp()) {
                    it.consume()
                }
            }
            observer.onStop()
        } else {
            observer.onCancel()
        }
    } catch (c: CancellationException) {
        observer.onCancel()
        throw c
    }
}

/** Gesture handler for mouse selection. */
internal suspend fun AwaitPointerEventScope.mouseSelection(
    observer: MouseSelectionObserver,
    clicksCounter: ClicksCounter,
    down: PointerEvent,
) {
    val downChange = down.changes[0]
    if (down.keyboardModifiers.isShiftPressed) {
        val started = observer.onExtend(downChange.position)
        if (started) {
            try {
                downChange.consume()
                val shouldConsumeUp =
                    drag(downChange.id) {
                        if (observer.onExtendDrag(it.position)) {
                            it.consume()
                        }
                    }

                if (shouldConsumeUp) {
                    currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
                }
            } finally {
                observer.onDragDone()
            }
        }
    } else {
        val selectionAdjustment =
            when (clicksCounter.clicks) {
                1 -> SelectionAdjustment.None
                2 -> SelectionAdjustment.Word
                else -> SelectionAdjustment.Paragraph
            }

        val started =
            observer.onStart(downChange.position, selectionAdjustment, clicksCounter.clicks)
        if (started) {
            try {
                var dragConsumed = selectionAdjustment != SelectionAdjustment.None
                val shouldConsumeUp =
                    drag(downChange.id) {
                        if (observer.onDrag(it.position, selectionAdjustment)) {
                            it.consume()
                            dragConsumed = true
                        }
                    }

                if (shouldConsumeUp && dragConsumed) {
                    currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
                }
            } finally {
                observer.onDragDone()
            }
        }
    }
}

internal class ClicksCounter(private val viewConfiguration: ViewConfiguration) {
    var clicks = 0
    var prevClick: PointerInputChange? = null

    fun update(event: PointerEvent) {
        val currentPrevClick = prevClick
        val newClick = event.changes[0]
        if (
            currentPrevClick != null &&
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
        distanceIsTolerable(viewConfiguration, prevClick, newClick)
}

private suspend fun AwaitPointerEventScope.awaitDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(PointerEventPass.Main)
    } while (!event.changes.fastAll { it.changedToDown() })
    return event
}

private fun distanceIsTolerable(
    viewConfiguration: ViewConfiguration,
    change1: PointerInputChange,
    change2: PointerInputChange,
): Boolean {
    val slop = viewConfiguration.pointerSlop(change1.type)
    return (change1.position - change2.position).getDistance() < slop
}

internal expect fun PointerEvent.isMouseOrTouchPad(): Boolean
