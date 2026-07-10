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

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

/**
 * An [View.OnTouchListener] for the [androidx.pdf.view.PdfContentLayout] that routes touch events
 * to either the annotation layers or the underlying PDF view.
 */
internal class PdfContentLayoutTouchListener(
    context: Context,
    private val annotationsTouchEventDispatcher: TouchEventDispatcher,
    private val pdfViewDispatcher: TouchEventDispatcher,
) : View.OnTouchListener {

    internal var isAnnotationInteractionEnabled: Boolean = true

    private var currentDispatcher: TouchEventDispatcher? = null
    private var isSingleTouchCommitted = false
    private var primaryPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (!isAnnotationInteractionEnabled) {
            if (currentDispatcher == annotationsTouchEventDispatcher) {
                // Send a CANCEL event to the annotations dispatcher to clean up its state.
                val cancelEvent =
                    MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                currentDispatcher?.dispatchTouchEvent(cancelEvent)
                cancelEvent.recycle()
                resetState()
            }
            return pdfViewDispatcher.dispatchTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> handleUpOrCancel(event)
        }
        return true
    }

    private fun handleDown(event: MotionEvent) {
        primaryPointerId = event.getPointerId(0)
        currentDispatcher =
            if (isAnnotationInteractionEnabled) {
                annotationsTouchEventDispatcher
            } else {
                pdfViewDispatcher
            }
        isSingleTouchCommitted = false
        downX = event.x
        downY = event.y
        currentDispatcher?.dispatchTouchEvent(event)
    }

    private fun handlePointerDown(event: MotionEvent) {
        if (isSingleTouchCommitted) {
            currentDispatcher?.dispatchTouchEvent(event)
        } else {
            val cancelEvent = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
            currentDispatcher?.dispatchTouchEvent(cancelEvent)
            cancelEvent.recycle()

            currentDispatcher = pdfViewDispatcher

            val downEvent =
                MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    MotionEvent.ACTION_DOWN,
                    event.getX(0),
                    event.getY(0),
                    event.metaState,
                )
            currentDispatcher?.dispatchTouchEvent(downEvent)
            downEvent.recycle()

            currentDispatcher?.dispatchTouchEvent(event)
        }
    }

    private fun handleMove(event: MotionEvent) {
        // If we receive a MOVE but haven't initialized state (because DOWN was consumed elsewhere),
        // we essentially treat this first MOVE as our "Start".
        if (primaryPointerId == MotionEvent.INVALID_POINTER_ID) {
            val fakeDown = MotionEvent.obtain(event)
            // Change the action to DOWN while keeping all other data intact
            fakeDown.action = MotionEvent.ACTION_DOWN

            handleDown(fakeDown)
            fakeDown.recycle()
            // If the user has already dragged past touch slop in this single event history,
            // we might want to commit immediately, but letting the next pass handle it is safer.
            return
        }

        val primaryPointerIndex = event.findPointerIndex(primaryPointerId)
        if (primaryPointerIndex == MotionEvent.INVALID_POINTER_ID) {
            return
        }

        if (currentDispatcher == annotationsTouchEventDispatcher && !isSingleTouchCommitted) {
            val dx = event.getX(primaryPointerIndex) - downX
            val dy = event.getY(primaryPointerIndex) - downY
            if (dx * dx + dy * dy > touchSlop * touchSlop) {
                isSingleTouchCommitted = true
            }
        }

        currentDispatcher?.dispatchTouchEvent(event)
    }

    private fun handlePointerUp(event: MotionEvent) {
        val actionIndex = event.actionIndex
        val liftedPointerId = event.getPointerId(actionIndex)

        if (
            currentDispatcher == annotationsTouchEventDispatcher &&
                liftedPointerId == primaryPointerId
        ) {
            val upEvent =
                MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    MotionEvent.ACTION_UP,
                    event.getX(actionIndex),
                    event.getY(actionIndex),
                    event.metaState,
                )
            currentDispatcher?.dispatchTouchEvent(upEvent)
            upEvent.recycle()
            resetState()
        } else {
            currentDispatcher?.dispatchTouchEvent(event)
        }
    }

    private fun handleUpOrCancel(event: MotionEvent) {
        currentDispatcher?.dispatchTouchEvent(event)
        resetState()
    }

    private fun resetState() {
        currentDispatcher = null
        isSingleTouchCommitted = false
        primaryPointerId = MotionEvent.INVALID_POINTER_ID
    }
}

/** Dispatches touch events to a target view. */
internal interface TouchEventDispatcher {
    fun dispatchTouchEvent(event: MotionEvent): Boolean
}
