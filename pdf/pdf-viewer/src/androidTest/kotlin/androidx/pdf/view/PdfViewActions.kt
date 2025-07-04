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
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.pdf.PdfPoint
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralClickAction
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
