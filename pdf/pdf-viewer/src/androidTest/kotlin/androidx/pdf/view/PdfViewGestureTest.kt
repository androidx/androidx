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

import android.graphics.Point
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewGestureTest {
    @Before
    fun before() {
        val fakePdfDocument = FakePdfDocument(List(100) { Point(500, 1000) })
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testScrollGesture() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var swipeStart = FloatArray(2) { Float.MIN_VALUE }
        var swipeEnd = FloatArray(2) { Float.MAX_VALUE }
        var gestureStateCounter = 1
        val gestureStates = IntArray(4)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val userGestureListener =
                object : PdfView.OnGestureStateChangedListener {
                    override fun onGestureStateChanged(newState: Int) {
                        gestureStates[gestureStateCounter] = newState
                        gestureStateCounter++
                    }
                }
            val startLoc = GeneralLocation.CENTER
            val endLoc = GeneralLocation.TOP_CENTER
            Espresso.onView(withId(PDF_VIEW_ID))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    gestureStates[0] = (view as PdfView).gestureState
                    view.addOnGestureStateChangedListener(userGestureListener)
                    scrollBefore = Point(view.scrollX, view.scrollY)
                    swipeStart = startLoc.calculateCoordinates(view)
                    swipeEnd = endLoc.calculateCoordinates(view)
                }
                .perform(scroll(startLoc, endLoc))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    scrollAfter = Point(view.scrollX, view.scrollY)
                }
            close()
        }

        val distanceScrolled = scrollAfter.y - scrollBefore.y
        val distanceSwiped = swipeStart[1] - swipeEnd[1]
        // We shouldn't have scrolled horizontally
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
        // Distance scrolled should be == to distance swiped, but we allow for a 2% difference
        // Empirically there is typically a single digit pixel difference between the two values
        assertThat(abs(distanceScrolled.toFloat() - distanceSwiped))
            .isLessThan(0.02F * distanceSwiped)
        // Before interaction = idle
        assertThat(gestureStates[0]).isEqualTo(PdfView.GESTURE_STATE_IDLE)
        // During interaction = interacting
        assertThat(gestureStates[1]).isEqualTo(PdfView.GESTURE_STATE_INTERACTING)
        // After interaction = idle, because this was an unanimated / "normal" scroll
        assertThat(gestureStates[2]).isEqualTo(PdfView.GESTURE_STATE_IDLE)
    }

    @Test
    fun testFlingGesture() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var swipeStart = FloatArray(2) { Float.MIN_VALUE }
        var swipeEnd = FloatArray(2) { Float.MAX_VALUE }
        var gestureStateCounter = 1
        val gestureStates = IntArray(4)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val userGestureListener =
                object : PdfView.OnGestureStateChangedListener {
                    override fun onGestureStateChanged(newState: Int) {
                        gestureStates[gestureStateCounter] = newState
                        gestureStateCounter++
                    }
                }
            val startLoc = GeneralLocation.CENTER
            val endLoc = GeneralLocation.TOP_CENTER
            Espresso.onView(withId(PDF_VIEW_ID))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    gestureStates[0] = (view as PdfView).gestureState
                    view.addOnGestureStateChangedListener(userGestureListener)
                    scrollBefore = Point(view.scrollX, view.scrollY)
                    swipeStart = startLoc.calculateCoordinates(view)
                    swipeEnd = endLoc.calculateCoordinates(view)
                }
                .perform(fling(startLoc, endLoc))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    scrollAfter = Point(view.scrollX, view.scrollY)
                }
            close()
        }

        val distanceFlung = scrollAfter.y - scrollBefore.y
        val distanceSwiped = swipeStart[1] - swipeEnd[1]
        // We shouldn't have scrolled horizontally
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
        // Distance flung should be >> distance swiped, but the exact relationship is dictated by
        // Overscroller, an external class. We only *require* the difference to be > 10%
        assertThat(distanceFlung).isGreaterThan((distanceSwiped * 1.1F).roundToInt())
        // Before interaction = idle
        assertThat(gestureStates[0]).isEqualTo(PdfView.GESTURE_STATE_IDLE)
        // During interaction = interacting
        assertThat(gestureStates[1]).isEqualTo(PdfView.GESTURE_STATE_INTERACTING)
        // After interaction = settling, because fling triggers animated scroll
        assertThat(gestureStates[2]).isEqualTo(PdfView.GESTURE_STATE_SETTLING)
    }

    @Test
    fun testDoubleTapToZoomGesture() {
        var zoomBefore = Float.MIN_VALUE
        var zoomAfter = Float.MIN_VALUE
        var gestureStateCounter = 1
        val gestureStates = IntArray(4)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val userGestureListener =
                object : PdfView.OnGestureStateChangedListener {
                    override fun onGestureStateChanged(newState: Int) {
                        gestureStates[gestureStateCounter] = newState
                        gestureStateCounter++
                    }
                }
            Espresso.onView(withId(PDF_VIEW_ID))
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    gestureStates[0] = (view as PdfView).gestureState
                    view.addOnGestureStateChangedListener(userGestureListener)
                    zoomBefore = view.zoom
                }
                .perform(ViewActions.doubleClick())
                .check { view, noViewFoundException ->
                    view ?: throw noViewFoundException
                    zoomAfter = (view as PdfView).zoom
                }
            close()
        }

        // Double tap = zoom in
        assertThat(zoomAfter).isGreaterThan(zoomBefore)
        // Before interaction = idle
        assertThat(gestureStates[0]).isEqualTo(PdfView.GESTURE_STATE_IDLE)
        // During interaction = interacting
        assertThat(gestureStates[1]).isEqualTo(PdfView.GESTURE_STATE_INTERACTING)
        // After interaction = settling, because double tap triggers an animated zoom in
        assertThat(gestureStates[2]).isEqualTo(PdfView.GESTURE_STATE_SETTLING)
    }
}

private fun scroll(from: CoordinatesProvider, to: CoordinatesProvider): ViewAction {
    return GeneralSwipeAction(PdfViewSwipe.SCROLL, from, to, Press.FINGER)
}

private fun fling(from: CoordinatesProvider, to: CoordinatesProvider): ViewAction {
    return GeneralSwipeAction(PdfViewSwipe.FLING, from, to, Press.FINGER)
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
