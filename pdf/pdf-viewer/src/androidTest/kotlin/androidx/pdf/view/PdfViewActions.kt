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

package androidx.pdf.view

import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.pdf.PdfPoint
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/** Performs a [ViewAction] that scrolls [PdfView] by [dy] pixels in the Y direction */
internal fun ViewInteraction.scrollByY(dy: Int) = this.perform(ScrollPdfViewByPixels(dy = dy))

/** Performs a [ViewAction] that sets [PdfView.zoom] to [newZoom] */
internal fun ViewInteraction.zoomTo(newZoom: Float) = this.perform(ZoomPdfView(newZoom))

/** Performs a [ViewAction] that sets [PdfView.zoom] to [newZoom] */
internal fun ViewInteraction.smoothZoomTo(newZoom: Float, numSteps: Int) =
    this.perform(ZoomPdfView(newZoom, numSteps))

/** Performs a [ViewAction] that calls [PdfView.scrollToPage] with the provided [pageNum] */
internal fun ViewInteraction.scrollToPage(pageNum: Int) = this.perform(ScrollPdfViewToPage(pageNum))

/** Performs a [ViewAction] that calls [PdfView.scrollToPosition] with the provided [pdfPoint] */
internal fun ViewInteraction.scrollToPosition(pdfPoint: PdfPoint) =
    this.perform(ScrollPdfViewToPage(pdfPoint))

/** Performs a [ViewAction] that scrolls any View by [totalPixels] in [numSteps] */
internal fun ViewInteraction.smoothScrollBy(totalPixels: Int, numSteps: Int) =
    this.perform(SmoothScrollY(totalPixels, numSteps))

/**
 * Performs a [ViewAction] that simulates a drag gesture on the [PdfView].
 *
 * @param startX The starting horizontal coordinate, relative to the view.
 * @param startY The starting vertical coordinate, relative to the view.
 * @param endX The ending horizontal coordinate, relative to the view.
 * @param endY The ending vertical coordinate, relative to the view.
 * @param buttonState The state of the mouse buttons during the drag, see
 *   [MotionEvent.getButtonState].
 * @param metaState The state of any meta keys pressed during the drag, see
 *   [MotionEvent.getMetaState].
 * @param sourceDevice The input device source for the event, see [InputDevice].
 */
internal fun ViewInteraction.drag(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    buttonState: Int = 0,
    metaState: Int = 0,
    sourceDevice: Int = InputDevice.SOURCE_TOUCHSCREEN,
) = this.perform(ActionDrag(startX, startY, endX, endY, buttonState, metaState, sourceDevice))

/**
 * Performs a [ViewAction] that simulates a mouse wheel scroll event on the [PdfView].
 *
 * @param vscroll The vertical scroll amount. A positive value indicates scrolling down, a negative
 *   value indicates scrolling up.
 * @param hscroll The horizontal scroll amount. A positive value indicates scrolling right, a
 *   negative value indicates scrolling left.
 * @param metaState The state of any meta keys pressed during the scroll, see
 *   [MotionEvent.getMetaState].
 */
internal fun ViewInteraction.scrollMouseWheel(vscroll: Float, hscroll: Float, metaState: Int = 0) =
    this.perform(ScrollMouseWheel(vscroll, hscroll, metaState))

/** [ViewAction] which scrolls a [PdfView] by ([dx], [dy]) */
private class ScrollPdfViewByPixels(val dx: Int = 0, val dy: Int = 0) : ViewAction {
    init {
        require(dx != 0 || dy != 0) { "Must scroll in at least 1 dimension" }
    }

    override fun getConstraints(): Matcher<View> =
        Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isAssignableFrom(PdfView::class.java),
        )

    override fun getDescription() = "Scroll PdfView by $dx, $dy"

    override fun perform(uiController: UiController, view: View) {
        view.scrollBy(dx, dy)
        uiController.loopMainThreadUntilIdle()
    }
}

/** [ViewAction] to scroll any View by [totalPixels] in [numSteps] smooth steps */
private class SmoothScrollY(private val totalPixels: Int, private val numSteps: Int) : ViewAction {
    private val stepSize = totalPixels / numSteps

    override fun getConstraints(): Matcher<View> =
        Matchers.allOf(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE))

    override fun getDescription() = "Scroll View by $totalPixels in $numSteps"

    override fun perform(uiController: UiController, view: View) {
        for (i in 0 until numSteps - 1) {
            view.scrollBy(/* x= */ 0, /* y= */ stepSize)
            uiController.loopMainThreadUntilIdle()
        }
        // Account for the remainder in the final step
        view.scrollBy(/* x= */ 0, /* y= */ stepSize + totalPixels % numSteps)
        uiController.loopMainThreadUntilIdle()
    }
}

/** [ViewAction] which sets [PdfView.zoom] */
private class ZoomPdfView(val newZoom: Float, val numSteps: Int = 1) : ViewAction {
    override fun getConstraints(): Matcher<View> =
        Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isAssignableFrom(PdfView::class.java),
        )

    override fun getDescription() = "Zoom PdfView to $newZoom"

    override fun perform(uiController: UiController, view: View) {
        // This should be guaranteed by our constraints, but this makes smartcasts work nicely
        check(view is PdfView)
        val stepSize = (newZoom - view.zoom) / numSteps
        for (i in 0 until numSteps) {
            view.zoom = view.zoom + stepSize
            uiController.loopMainThreadUntilIdle()
        }
    }
}

/**
 * [ViewAction] which invokes [PdfView.scrollToPage] if only a page number is provided, or
 * [PdfView.scrollToPosition] if a point on the page is provided
 */
private class ScrollPdfViewToPage : ViewAction {
    private val pageNum: Int
    private val pointOnPage: PointF?

    constructor(pageNum: Int) {
        this.pageNum = pageNum
        pointOnPage = null
    }

    constructor(point: PdfPoint) {
        pageNum = point.pageNum
        pointOnPage = PointF(point.x, point.y)
    }

    override fun getConstraints(): Matcher<View> =
        Matchers.allOf(
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            ViewMatchers.isAssignableFrom(PdfView::class.java),
        )

    override fun getDescription(): String {
        return if (pointOnPage != null) {
            "Scroll to $pointOnPage on $pageNum"
        } else {
            "Scroll to $pageNum"
        }
    }

    override fun perform(uiController: UiController, view: View) {
        // This should be guaranteed by our constraints, but this makes smartcasts work nicely
        check(view is PdfView)
        if (pointOnPage != null) {
            view.scrollToPosition(PdfPoint(pageNum, pointOnPage))
        } else {
            view.scrollToPage(pageNum)
        }
        uiController.loopMainThreadUntilIdle()
    }
}

/**
 * [ViewAction] that simulates a drag gesture from a starting point to an ending point.
 *
 * This is performed by injecting a sequence of [MotionEvent]s:
 * 1. `ACTION_DOWN` at the start coordinates.
 * 2. `ACTION_MOVE` events to simulate the drag.
 * 3. `ACTION_UP` at the end coordinates.
 *
 * The constructor parameter coordinates are relative to the view's top-left corner.
 */
private class ActionDrag : ViewAction {
    val startX: Float
    val startY: Float
    val endX: Float
    val endY: Float
    val buttonState: Int
    val metaState: Int
    val sourceDevice: Int

    constructor(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        buttonState: Int,
        metaState: Int,
        sourceDevice: Int,
    ) {
        this.startX = startX
        this.startY = startY
        this.endX = endX
        this.endY = endY
        this.buttonState = buttonState
        this.metaState = metaState
        this.sourceDevice = sourceDevice
    }

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(PdfView::class.java)
    }

    override fun getDescription(): String {
        return "Simulate a mouse drag from ($startX, $startY) to ($endX, $endY)"
    }

    override fun perform(uiController: UiController, view: View) {
        val screenPos = IntArray(2)
        view.getLocationOnScreen(screenPos)

        val startX = (screenPos[0] + startX)
        val startY = (screenPos[1] + startY)
        val endX = (screenPos[0] + endX)
        val endY = (screenPos[1] + endY)

        val downTime = SystemClock.uptimeMillis()

        val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })

        // Step 1: Send ACTION_DOWN event to start the gesture.
        val downPointerCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = startX
                    y = startY
                }
            )
        val downEvent =
            MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                1, // pointerCount
                pointerProperties,
                downPointerCoords,
                metaState,
                buttonState,
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                sourceDevice,
                0, // flags
            )
        uiController.injectMotionEvent(downEvent)

        // Step 2: Send ACTION_MOVE event at the start point to simulate starting the drag.
        val startMoveEventTime = SystemClock.uptimeMillis()
        val startMovePointerCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = startX
                    y = startY
                }
            )
        val moveEvent1 =
            MotionEvent.obtain(
                downTime,
                startMoveEventTime,
                MotionEvent.ACTION_MOVE,
                1, // pointerCount
                pointerProperties,
                startMovePointerCoords,
                metaState,
                buttonState, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                sourceDevice,
                0, // flags
            )
        uiController.injectMotionEvent(moveEvent1)

        // Step 3: Send ACTION_MOVE event at the end point to simulate ending the drag.
        val endMoveEventTime = SystemClock.uptimeMillis()
        val endMovePointerCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = endX
                    y = endY
                }
            )
        val moveEvent =
            MotionEvent.obtain(
                downTime,
                endMoveEventTime,
                MotionEvent.ACTION_MOVE,
                1, // pointerCount
                pointerProperties,
                endMovePointerCoords,
                metaState,
                buttonState, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                sourceDevice,
                0, // flags
            )
        uiController.injectMotionEvent(moveEvent)

        // Step 4: Send ACTION_UP event to end the gesture.
        val upEventTime = SystemClock.uptimeMillis()
        val upPointerCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    x = endX
                    y = endY
                }
            )
        val upEvent =
            MotionEvent.obtain(
                downTime,
                upEventTime,
                MotionEvent.ACTION_UP,
                1, // pointerCount
                pointerProperties,
                upPointerCoords,
                metaState,
                buttonState, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                sourceDevice,
                0, // flags
            )
        uiController.injectMotionEvent(upEvent)

        // Recycle the events.
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()

        // Allow time for the UI to process the events.
        uiController.loopMainThreadUntilIdle()
    }
}

/**
 * [ViewAction] that dispatches a generic [MotionEvent.ACTION_SCROLL] to simulate a mouse wheel
 * scroll. The event is dispatched at the center of the view.
 */
private class ScrollMouseWheel : ViewAction {
    val vscroll: Float
    val hscroll: Float
    val metaState: Int

    constructor(vscroll: Float, hscroll: Float, metaState: Int) {
        this.vscroll = vscroll
        this.hscroll = hscroll
        this.metaState = metaState
    }

    override fun getConstraints(): Matcher<View> {
        return ViewMatchers.isAssignableFrom(PdfView::class.java)
    }

    override fun getDescription(): String {
        return "dispatch generic motion event"
    }

    override fun perform(uiController: UiController, view: View) {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val pointerProperties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
        val pointerCoords =
            arrayOf(
                MotionEvent.PointerCoords().apply {
                    this.x = GeneralLocation.CENTER.calculateCoordinates(view)[0]
                    this.y = GeneralLocation.CENTER.calculateCoordinates(view)[1]
                    setAxisValue(MotionEvent.AXIS_VSCROLL, vscroll)
                    setAxisValue(MotionEvent.AXIS_HSCROLL, hscroll)
                }
            )
        val scrollEvent =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_SCROLL,
                1, // pointerCount
                pointerProperties,
                pointerCoords,
                metaState,
                0, // buttonState
                1f, // xPrecision
                1f, // yPrecision
                0, // deviceId
                0, // edgeFlags
                InputDevice.SOURCE_MOUSE,
                0, // flags
            )
        uiController.injectMotionEvent(scrollEvent)
    }
}

/**
 * Performs a [ViewAction] that results in single tap on a specific location (x, y) relative to a
 * given view. This action calculates the screen coordinates of the view and offsets them by the
 * provided (x, y) values to simulate a tap at the desired position on the screen.
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
 * Converts the center of the given [RectF] on a PDF page to view coordinates.
 *
 * @param pdfView The [PdfView] instance used to convert coordinates.
 * @param pageNumber The page number where the link resides.
 * @param bounds The bounds of the link in content coordinates.
 * @return A [PointF] in view coordinates where a tap should be performed.
 */
internal fun getTapPointFromContentBounds(
    pdfView: PdfView,
    pageNumber: Int,
    bounds: RectF,
): android.graphics.PointF {
    val centerX = bounds.centerX()
    val centerY = bounds.centerY()
    val viewPoint = pdfView.pdfToViewPoint(PdfPoint(pageNumber, centerX, centerY))
    return requireNotNull(viewPoint) { "Failed to convert PdfPoint to view coordinates" }
}
