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

import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.pdf.R
import androidx.pdf.view.PdfView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Matcher
import org.hamcrest.Matchers

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
                        0,
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
                            0,
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
                        0,
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

    /**
     * Performs a [ViewAction] that results in single tap on a specific location (x, y) relative to
     * a given view. This action calculates the screen coordinates of the view and offsets them by
     * the provided (x, y) values to simulate a tap at the desired position on the screen.
     *
     * @param x The horizontal offset (in pixels) from the top-left corner of the view.
     * @param y The vertical offset (in pixels) from the top-left corner of the view.
     * @return A ViewAction that can be used with Espresso to perform the tap.
     */
    internal fun performSingleTapOnCoords(x: Float, y: Float): ViewAction {
        return GeneralClickAction(
            Tap.SINGLE,
            { view ->
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)

                val screenX = (screenPos[0] + x)
                val screenY = (screenPos[1] + y)

                floatArrayOf(screenX, screenY)
            },
            Press.FINGER,
            InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.BUTTON_PRIMARY,
        )
    }

    /**
     * Helper method to convert PDF coordinates to view coordinates based on zoom and scroll values.
     *
     * @param pdfX X coordinate in PDF space.
     * @param pdfY Y coordinate in PDF space.
     * @param zoom Current zoom level of the PDF view.
     * @param scrollX Current scroll position of the PDF view (horizontal).
     * @param scrollY Current scroll position of the PDF view (vertical).
     * @return A PointF object containing the converted X and Y coordinates.
     */
    fun convertPdfToViewCoord(
        pdfX: Float,
        pdfY: Float,
        zoom: Float,
        scrollX: Int,
        scrollY: Int,
    ): PointF {
        val viewX = (pdfX * zoom) - scrollX
        val viewY = (pdfY * zoom) - scrollY
        return PointF(viewX, viewY)
    }

    /**
     * Performs a [ViewAction] that simulates a tap on the specified PDF link rectangle bounds. The
     * tap is simulated at the center of the given rectangle.
     *
     * @param linkBounds The bounds of the link in PDF space.
     * @return A ViewAction to be performed with Espresso.
     */
    fun tapOnPosition(linkBounds: RectF): ViewAction {
        val pdfCenterX = (linkBounds.left + linkBounds.right) / 2
        val pdfCenterY = (linkBounds.top + linkBounds.bottom) / 2

        return object : ViewAction {
            override fun getConstraints(): Matcher<View> =
                Matchers.allOf(
                    ViewMatchers.isDisplayed(),
                    ViewMatchers.isAssignableFrom(PdfView::class.java),
                )

            override fun getDescription() = "Tap on PDF link rectangle bounds"

            override fun perform(uiController: UiController, view: View) {
                val pdfView = view as PdfView

                val zoom = pdfView.zoom
                val scrollX = pdfView.scrollX
                val scrollY = pdfView.scrollY

                val point = convertPdfToViewCoord(pdfCenterX, pdfCenterY, zoom, scrollX, scrollY)
                performSingleTapOnCoords(point.x, point.y).perform(uiController, view)
            }
        }
    }
}
