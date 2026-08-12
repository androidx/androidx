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

package androidx.pdf.view.annotation.draganddrop

import android.graphics.Canvas
import android.graphics.Point
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import androidx.core.view.ViewCompat

/**
 * A delegate class that encapsulates all touch handling logic for the
 * [androidx.pdf.view.annotation.AnnotationToolbarView].
 *
 * This handler is responsible for detecting a long-press on the contents of the toolbar to initiate
 * a system drag-and-drop operation.
 *
 * @param toolbarView The [androidx.pdf.view.annotation.AnnotationToolbarView] instance whose
 *   touches are being handled.
 * @param isTouchOnInteractiveChild A lambda to verify if long press is registered on long press
 *   interactive child(for e.g. brush size selector).
 */
internal class AnnotationToolbarTouchHandler(
    private val toolbarView: View,
    private val isTouchOnInteractiveChild: (MotionEvent) -> Boolean,
) {

    internal var areAnimationsEnabled: Boolean = true

    private var lastEventProcessed: MotionEvent? = null
    private var shouldIntercept: Boolean = false

    private val gestureDetector: GestureDetector =
        GestureDetector(
            toolbarView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(event: MotionEvent) {
                    shouldIntercept = true
                    startDrag()
                }

                override fun onDown(e: MotionEvent): Boolean {
                    // Re-enable long press detection at the start of every new gesture.
                    gestureDetector.setIsLongpressEnabled(true)
                    shouldIntercept = false
                    // Necessary to continue tracking the gesture
                    return true
                }
            },
        )

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (isTouchOnInteractiveChild(event)) return false

        if (event.actionMasked == ACTION_DOWN) {
            gestureDetector.setIsLongpressEnabled(true)
            shouldIntercept = false
        }

        gestureDetector.onTouchEvent(event)
        lastEventProcessed = event
        return shouldIntercept
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchOnInteractiveChild(event)) return false

        if (event.actionMasked == ACTION_DOWN) {
            gestureDetector.setIsLongpressEnabled(true)
            shouldIntercept = false
        }

        // TEST HOOK: If animations are disabled (e.g., during Espresso tests),
        // bypass the 500ms long-press delay and start dragging immediately.
        if (!areAnimationsEnabled && event.actionMasked == ACTION_DOWN) {
            startDrag()
        }

        if (lastEventProcessed !== event && !shouldIntercept) {
            gestureDetector.onTouchEvent(event)
        }
        return true
    }

    private fun startDrag() {
        toolbarView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        ViewCompat.startDragAndDrop(
            /* v = */ toolbarView,
            /* data =*/ null,
            /* shadowBuilder = */ object : View.DragShadowBuilder() {
                override fun onProvideShadowMetrics(
                    outShadowSize: Point,
                    outShadowTouchPoint: Point,
                ) {
                    // We'll set minimum size of shadow allowed by system, as we don't require drop
                    // shadow. ToolbarCoordinator takes care of showing anchor points.
                    outShadowSize.set(1, 1)
                    outShadowTouchPoint.set(0, 0)
                }

                override fun onDrawShadow(canvas: Canvas) {
                    // No - Op
                }
            },
            /* myLocalState = */ null,
            /* flags =*/ 0,
        )
    }
}
