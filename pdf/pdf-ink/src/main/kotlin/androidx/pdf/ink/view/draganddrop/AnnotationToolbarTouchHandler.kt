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

package androidx.pdf.ink.view.draganddrop

import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * A delegate class that encapsulates all touch handling logic for the
 * [androidx.pdf.ink.view.AnnotationToolbar].
 *
 * This handler is responsible for:
 * 1. **Detecting a long-press** on the contents of the toolbar to initiate a drag operation.
 * 2. **Notifying an external [ToolbarDragListener]** about the start, move, and end of a drag
 *    gesture, without handling the view movement itself.
 *
 * @param toolbarView The [androidx.pdf.ink.view.AnnotationToolbar] instance whose touches are being
 *   handled.
 * @param isTouchOnInteractiveChild A lambda to verify if long press is registered on long press
 *   interactive child(for e.g. brush size selector).
 */
internal class AnnotationToolbarTouchHandler(
    private val toolbarView: View,
    private val isTouchOnInteractiveChild: (MotionEvent) -> Boolean,
) {

    internal var areAnimationsEnabled: Boolean = true

    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(toolbarView.context).scaledTouchSlop

    private var dragListener: ToolbarDragListener? = null

    fun setOnDragListener(listener: ToolbarDragListener) {
        dragListener = listener
    }

    private val gestureDetector: GestureDetector =
        GestureDetector(
            toolbarView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(event: MotionEvent) {
                    startDrag(event)
                }

                override fun onScroll(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    distanceX: Float,
                    distanceY: Float,
                ): Boolean {
                    val dx = abs(e2.x - (e1?.x ?: 0f))
                    val dy = abs(e2.y - (e1?.y ?: 0f))

                    // Only make a decision once the user has moved past the touch slop threshold
                    if (dx > touchSlop || dy > touchSlop) {
                        gestureDetector.setIsLongpressEnabled(false)
                    }

                    // We return false here because we don't want onScroll to consume the event.
                    // We only use it to detect the start of a scroll and disable long press.
                    return false
                }

                override fun onDown(e: MotionEvent): Boolean {
                    // Re-enable long press detection at the start of every new gesture.
                    gestureDetector.setIsLongpressEnabled(true)
                    // Necessary to continue tracking the gesture
                    return true
                }
            },
        )

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // If not dragging, let buttons handle their own touches.
        if (!isDragging && isTouchOnInteractiveChild(event)) return false

        gestureDetector.onTouchEvent(event)

        // If we intercept an UP/CANCEL, the system will NOT call onTouchEvent.
        // We must clean up state here manually.
        when (event.actionMasked) {
            ACTION_UP,
            ACTION_CANCEL -> {
                if (isDragging) {
                    endDrag()
                    return true // Ensures child gets ACTION_CANCEL, not ACTION_UP
                }
            }
        }

        // If dragging is in progress, "steal" the event stream from child views (buttons)
        return isDragging
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDragging && isTouchOnInteractiveChild(event)) return false

        // TEST HOOK: If animations are disabled (e.g., during Espresso tests),
        // bypass the 500ms long-press delay and start dragging immediately.
        if (!areAnimationsEnabled && event.actionMasked == ACTION_DOWN) {
            startDrag(event)
        }

        // If we are dragging, we handle movement manually. No need to feed the detector.
        if (!isDragging) gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) dragListener?.onDragMove(event)
            }
            ACTION_UP,
            ACTION_CANCEL -> {
                if (isDragging) {
                    endDrag()
                    return true // Notify system we've consumed event stream
                }
            }
        }

        return isDragging
    }

    private fun startDrag(event: MotionEvent) {
        isDragging = true
        // Critical: Stop children (buttons) from handling this touch any further
        toolbarView.parent.requestDisallowInterceptTouchEvent(true)
        toolbarView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        dragListener?.onDragStart(event)
    }

    private fun endDrag() {
        dragListener?.onDragEnd()
        isDragging = false
        // Re-enable parent interception for normal scrolling behavior
        toolbarView.parent.requestDisallowInterceptTouchEvent(false)
    }
}
