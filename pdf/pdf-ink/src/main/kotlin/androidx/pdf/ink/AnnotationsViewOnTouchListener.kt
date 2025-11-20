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

/** Routes touch events to either ink view or pdf view based on single or multi-touch gestures. */
internal class AnnotationsViewOnTouchListener(
    context: Context,
    private val wetStrokesViewDispatcher: TouchEventDispatcher,
    private val pdfViewDispatcher: TouchEventDispatcher,
) : View.OnTouchListener {

    private var currentDispatcher: TouchEventDispatcher? = null
    private var isSingleTouchCommitted = false
    private var primaryPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var downX = 0f
    private var downY = 0f
    private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
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
        currentDispatcher = wetStrokesViewDispatcher
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
        val primaryPointerIndex = event.findPointerIndex(primaryPointerId)
        if (isSingleTouchCommitted && currentDispatcher == wetStrokesViewDispatcher) {
            if (primaryPointerIndex != -1) {
                val x = event.getX(primaryPointerIndex)
                val y = event.getY(primaryPointerIndex)
                val singlePointerMove =
                    MotionEvent.obtain(
                        event.downTime,
                        event.eventTime,
                        event.action,
                        x,
                        y,
                        event.metaState,
                    )
                currentDispatcher?.dispatchTouchEvent(singlePointerMove)
                singlePointerMove.recycle()
            }
        } else {
            if (
                currentDispatcher == wetStrokesViewDispatcher &&
                    !isSingleTouchCommitted &&
                    primaryPointerIndex != -1
            ) {
                val dx = event.getX(primaryPointerIndex) - downX
                val dy = event.getY(primaryPointerIndex) - downY
                if (dx * dx + dy * dy > touchSlop * touchSlop) {
                    isSingleTouchCommitted = true
                }
            }
            currentDispatcher?.dispatchTouchEvent(event)
        }
    }

    private fun handlePointerUp(event: MotionEvent) {
        val actionIndex = event.actionIndex
        val liftedPointerId = event.getPointerId(actionIndex)

        if (currentDispatcher == wetStrokesViewDispatcher && liftedPointerId == primaryPointerId) {
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
