/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink

import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import androidx.pdf.ink.state.AnnotationDrawingMode

/** Dispatches touch events for annotations to corresponding views for handling. */
internal class AnnotationsTouchEventDispatcher(
    private val annotationsViewDispatcher: TouchEventDispatcher,
    private val inkViewDispatcher: TouchEventDispatcher,
) : TouchEventDispatcher {
    internal var drawingMode: AnnotationDrawingMode? = null

    private var activeDispatcher: TouchEventDispatcher? = null
    private var lastDownTime: Long = 0

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            lastDownTime = event.downTime
        }

        // If a dispatcher is already active, forward the event only to it.
        val dispatcher = activeDispatcher
        if (dispatcher != null) {
            dispatcher.dispatchTouchEvent(event)
        } else {
            when (drawingMode) {
                is AnnotationDrawingMode.PenMode -> {
                    activeDispatcher = inkViewDispatcher
                    inkViewDispatcher.dispatchTouchEvent(event)
                }
                is AnnotationDrawingMode.HighlighterMode -> {
                    // In Highlighter mode, ACTION_MOVE is sent only to the ink view until text
                    // highlighting is confirmed. Other events are broadcast to both.
                    if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                        inkViewDispatcher.dispatchTouchEvent(event)
                    } else {
                        inkViewDispatcher.dispatchTouchEvent(event)
                        annotationsViewDispatcher.dispatchTouchEvent(event)
                    }
                }
                is AnnotationDrawingMode.EraserMode -> {
                    annotationsViewDispatcher.dispatchTouchEvent(event)
                }
                else -> {
                    return false
                }
            }
        }

        // If the gesture has ended, reset the active dispatcher for the next gesture.
        if (
            event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            activeDispatcher = null
        }

        // Always consume the event to ensure this router maintains control of the gesture.
        return true
    }

    /**
     * Sets the active dispatcher and sends a `CANCEL` event to the other dispatcher to ensure it
     * stops processing the current gesture.
     */
    fun switchActiveDispatcher(newActiveDispatcher: TouchEventDispatcher, viewPoint: PointF) {
        if (activeDispatcher == newActiveDispatcher) return

        val dispatcherToCancel =
            if (newActiveDispatcher === annotationsViewDispatcher) inkViewDispatcher
            else annotationsViewDispatcher

        // Create and dispatch a CANCEL event to the non-active dispatcher.
        val eventTime = SystemClock.uptimeMillis()
        val cancelEvent =
            MotionEvent.obtain(
                lastDownTime,
                eventTime,
                MotionEvent.ACTION_CANCEL,
                viewPoint.x,
                viewPoint.y,
                /** metaState= */
                0,
            )
        dispatcherToCancel.dispatchTouchEvent(cancelEvent)
        cancelEvent.recycle()

        activeDispatcher = newActiveDispatcher
    }
}
