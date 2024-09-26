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

package androidx.pdf.actions

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.pdf.R
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed

class SelectionViewActions {
    private var selectionHandleDistance = 0f

    fun longClickAndDragRight(duration: Long = 500): ViewAction {
        return object : ViewAction {
            override fun getConstraints() = isDisplayed()

            override fun getDescription() = "Long click and drag right"

            override fun perform(uiController: UiController, view: View) {
                val startHandle = view.findViewById<ImageView>(R.id.start_drag_handle)
                val stopHandle = view.findViewById<ImageView>(R.id.stop_drag_handle)

                val startHandleCoordinates =
                    GeneralLocation.CENTER.calculateCoordinates(startHandle)
                val stopHandleCoordinates = GeneralLocation.CENTER.calculateCoordinates(stopHandle)

                selectionHandleDistance = stopHandleCoordinates[0] - startHandleCoordinates[0]

                val downEvent =
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis() + duration,
                        MotionEvent.ACTION_DOWN,
                        stopHandleCoordinates[0],
                        stopHandleCoordinates[1],
                        0
                    )
                uiController.injectMotionEvent(downEvent)

                var timingAdjust = 100
                val numDragEvents = 10
                val dragEvents = mutableListOf<MotionEvent>()
                for (i in 1..numDragEvents) {
                    val newX = stopHandleCoordinates[0] + numDragEvents * i
                    val newEvent =
                        MotionEvent.obtain(
                            SystemClock.uptimeMillis() + duration + i * timingAdjust,
                            SystemClock.uptimeMillis() + duration + i * timingAdjust,
                            MotionEvent.ACTION_MOVE,
                            newX,
                            stopHandleCoordinates[1],
                            0
                        )
                    dragEvents.add(newEvent)
                }
                dragEvents.forEach { uiController.injectMotionEvent(it) }

                timingAdjust = 1100
                val upEvent =
                    MotionEvent.obtain(
                        SystemClock.uptimeMillis() + duration + timingAdjust,
                        SystemClock.uptimeMillis() + duration + timingAdjust,
                        MotionEvent.ACTION_UP,
                        stopHandleCoordinates[0] + 100,
                        stopHandleCoordinates[1],
                        0
                    )
                uiController.injectMotionEvent(upEvent)
            }
        }
    }

    fun stopHandleMoved(): ViewAssertion {
        return ViewAssertion { view, _ ->
            val startHandle = view?.findViewById<ImageView>(R.id.start_drag_handle)
            val stopHandle = view?.findViewById<ImageView>(R.id.stop_drag_handle)

            val startHandleCoordinates = GeneralLocation.CENTER.calculateCoordinates(startHandle)
            val stopHandleCoordinates = GeneralLocation.CENTER.calculateCoordinates(stopHandle)
            val initialDistance = selectionHandleDistance
            selectionHandleDistance = stopHandleCoordinates[0] - startHandleCoordinates[0]
            assert(selectionHandleDistance > initialDistance)
        }
    }
}
