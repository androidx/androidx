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

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewParent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class GestureTrackerTest {
    private val gestureHandlerSpy =
        mock<GestureTracker.GestureHandler>().apply {
            // We must return true from these listeners or GestureDetector won't continue sending
            // us callbacks
            whenever(onScaleBegin(any())).thenReturn(true)
            whenever(onScale(any())).thenReturn(true)
        }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var gestureTracker: GestureTracker

    @Before
    fun setup() {
        // Initialize GestureTracker before each case to avoid state leaking between tests
        gestureTracker = GestureTracker(context).apply { delegate = gestureHandlerSpy }
    }

    @Test
    fun testSingleTap() {
        gestureTracker.feed(down(PointF(50f, 50f)))
        gestureTracker.feed(up(PointF(50f, 50f)))
        // We expected to issue onGestureStart and onSingleTapUp callbacks, and we expect the
        // current detected gesture to be FIRST_TAP (single tap that still may become a double tap)
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy).onSingleTapUp(any())
        assertThat(gestureTracker.matches(GestureTracker.Gesture.FIRST_TAP)).isTrue()

        // Advance time by the double tap timeout
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getDoubleTapTimeout().toLong(),
            TimeUnit.MILLISECONDS,
        )

        // We expect to issue onSingleTapConfirmed and onGestureEnd callbacks, and we expect the
        // current detect gesture to be a SINGLE_TAP (i.e. confirmed *not* to be a double tap)
        verify(gestureHandlerSpy).onSingleTapConfirmed(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.SINGLE_TAP))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.SINGLE_TAP)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDoubleTap() {
        val downTime = SystemClock.elapsedRealtime()
        gestureTracker.feed(down(PointF(50f, 50f), time = downTime))
        gestureTracker.feed(up(PointF(50f, 50f), downTime = downTime))
        // We expected to issue onGestureStart and onSingleTapUp callbacks, and we expect the
        // current detected gesture to be FIRST_TAP (single tap that still may become a double tap)
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy).onSingleTapUp(any())
        assertThat(gestureTracker.matches(GestureTracker.Gesture.FIRST_TAP)).isTrue()

        // Advance time by less than the double tap timeout, and issue another up / down sequence.
        // The minimum time between down events to detect a double tap is a hidden API in
        // ViewConfiguration, so use half the maximum time.
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getDoubleTapTimeout().toLong() / 2,
            TimeUnit.MILLISECONDS,
        )
        gestureTracker.feed(down(PointF(50f, 50f)))
        gestureTracker.feed(up(PointF(50f, 50f), downTime = downTime))

        // We expect to issue onDoubleTap and onGestureEnd callbacks; we expect to *not* have issued
        // an onSingleTapConfirmed callback; and we expect the current detected gesture to be a
        // DOUBLE_TAP
        verify(gestureHandlerSpy, times(2)).onSingleTapUp(any())
        verify(gestureHandlerSpy, never()).onSingleTapConfirmed(any())
        verify(gestureHandlerSpy).onDoubleTap(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DOUBLE_TAP))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DOUBLE_TAP)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testLongPress() {
        val downTime = SystemClock.elapsedRealtime()
        gestureTracker.feed(down(PointF(50f, 50f), time = downTime))
        // We expected to issue an onGestureStart and onSingleTapUp callback, and we expect the
        // current detected gesture to be TOUCH (Down with no Up yet)
        verify(gestureHandlerSpy).onGestureStart()
        assertThat(gestureTracker.matches(GestureTracker.Gesture.TOUCH)).isTrue()

        // Advance time by the long press timeout
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getLongPressTimeout().toLong() + 1,
            TimeUnit.MILLISECONDS,
        )
        gestureTracker.feed(up(PointF(50f, 50f), downTime = downTime))

        // We shouldn't have issued these callbacks
        verify(gestureHandlerSpy, never()).onSingleTapConfirmed(any())
        verify(gestureHandlerSpy, never()).onSingleTapUp(any())
        // We expect to have issued onShowPress, onLongPress, and onGestureEnd callbacks, and we
        // expect the current detected gesture to be a LONG_PRESS
        verify(gestureHandlerSpy).onShowPress(any())
        verify(gestureHandlerSpy).onLongPress(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.LONG_PRESS))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.LONG_PRESS)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDragX() {
        for (event in
            oneFingerDrag(
                start = PointF(50f, 50f),
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG_X))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDragY() {
        for (event in
            oneFingerDrag(
                start = PointF(50f, 50f),
                velocity = Point(0, ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2),
            )) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG_Y))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_Y)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDrag() {
        val velocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity / 4
        for (event in
            oneFingerDrag(start = PointF(50f, 50f), velocity = Point(velocity, velocity))) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testFling() {
        val velocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 2
        for (event in
            oneFingerDrag(start = PointF(50f, 50f), velocity = Point(velocity, velocity))) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onFling(any(), any(), any(), any())
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.FLING))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.FLING)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testZoomIn_pinch() {
        // Drag pointer 1 in the positive X direction from (50, 50) at the same time as dragging
        // pointer 2 in the negative X direction from (500, 500)
        val velocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2
        val start1 = PointF(50f, 50f)
        val start2 = PointF(500f, 500f)
        val velocity1 = Point(velocity, 0)
        val velocity2 = Point(-velocity, 0)
        for (event in twoFingerDrag(start1, start2, velocity1, velocity2)) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScaleBegin(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScale(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScaleEnd(any())
        assertThat(gestureTracker.matches(GestureTracker.Gesture.ZOOM)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Ignore // b/376314114
    @Test
    fun testZoomOut_pinch() {
        // Drag pointer 1 in the negative Y direction from (500, 500) at the same time as dragging
        // pointer 2 in the positive Y direction from (500, 500)
        val velocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2
        val start1 = PointF(500f, 500f)
        val start2 = PointF(500f, 500f)
        val velocity1 = Point(0, -velocity)
        val velocity2 = Point(0, velocity)
        for (event in twoFingerDrag(start1, start2, velocity1, velocity2)) {
            gestureTracker.feed(event)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScaleBegin(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScale(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScaleEnd(any())
        assertThat(gestureTracker.matches(GestureTracker.Gesture.ZOOM)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testZoom_quickScale() {
        // First, send a single tap
        val startPoint = PointF(50f, 50f)
        val downTime = SystemClock.elapsedRealtime()
        gestureTracker.feed(down(startPoint, time = downTime))
        gestureTracker.feed(up(startPoint, downTime = downTime))
        // Then, advance time by less than the double tap timeout, and tap again, but don't release
        // the pointer
        // The minimum time between down events to detect a double tap is a hidden API in
        // ViewConfiguration, so use a fraction of the maximum time.
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getDoubleTapTimeout().toLong() / 5,
            TimeUnit.MILLISECONDS,
        )
        gestureTracker.feed(down(startPoint))
        // Finally, tap and drag in the +y direction from same point
        val velocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2
        for (event in oneFingerDrag(startPoint, Point(0, velocity), skipDown = true)) {
            gestureTracker.feed(event)
        }

        // These are intermediate callbacks we expect to receive while the quick scale gesture is
        // being formed
        verify(gestureHandlerSpy).onSingleTapUp(any())
        verify(gestureHandlerSpy).onShowPress(any())
        verify(gestureHandlerSpy).onLongPress(any())
        // These are the callbacks we really want to receive after the quick scale gesture is made
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScaleBegin(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScale(any())
        verify(gestureHandlerSpy, atLeastOnce()).onScaleEnd(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.ZOOM))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.ZOOM)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testTap_thenDrag_noDoubleTap() {
        // First, issue a single tap and release the pointer
        val point = PointF(50f, 50f)
        gestureTracker.feed(down(point))
        gestureTracker.feed(up(point))
        // We expected to issue onGestureStart and onSingleTapUp callbacks, and we expect the
        // current detected gesture to be FIRST_TAP (single tap that still may become a double tap)
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy).onSingleTapUp(any())
        assertThat(gestureTracker.matches(GestureTracker.Gesture.FIRST_TAP)).isTrue()

        // Then, drag from the same point
        for (event in
            oneFingerDrag(
                point,
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event)
        }

        // These are the terminal callbacks from the completion of the single tap
        verify(gestureHandlerSpy).onSingleTapConfirmed(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.SINGLE_TAP))
        // These are the callbacks we expect to receive as part of the scroll gesture
        verify(gestureHandlerSpy, times(2)).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(GestureTracker.Gesture.DRAG_X)
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()
        // And we should never have detected a double tap
        verify(gestureHandlerSpy, never()).onDoubleTap(any())

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDrag_thenTap_noDoubleTap() {
        val point = PointF(50f, 50f)
        // First, drag
        for (event in
            oneFingerDrag(
                point,
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event)
        }
        // These are the callbacks we expect to receive as part of the scroll
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(GestureTracker.Gesture.DRAG_X)
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()

        // Then, issue a single tap from the same point
        gestureTracker.feed(down(point))
        gestureTracker.feed(up(point))
        // Advance time by the double tap timeout
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getDoubleTapTimeout().toLong(),
            TimeUnit.MILLISECONDS,
        )

        // These are the callbacks we expect to receive as part of the single tap
        verify(gestureHandlerSpy, times(2)).onGestureStart()
        verify(gestureHandlerSpy).onSingleTapUp(any())
        verify(gestureHandlerSpy).onSingleTapConfirmed(any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.SINGLE_TAP))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.SINGLE_TAP)).isTrue()
        // And we should never have detected a double tap
        verify(gestureHandlerSpy, never()).onDoubleTap(any())

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testTwoQuickDrags_sameDownPosition_noQuickScale() {
        // Drag once
        val point = PointF(50f, 50f)
        for (event in
            oneFingerDrag(
                start = point,
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event)
        }
        // Make sure we detect one set of drag / scroll callbacks
        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG_X))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()

        // Then, advance time by less than the double tap timeout
        ShadowLooper.idleMainLooper(
            ViewConfiguration.getDoubleTapTimeout().toLong() / 2,
            TimeUnit.MILLISECONDS,
        )
        // Finally, drag again, from the same point
        for (event in
            oneFingerDrag(
                start = point,
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event)
        }

        // Make sure we detected a second set of drag callbacks
        verify(gestureHandlerSpy, times(2)).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(gestureHandlerSpy, times(2)).onGestureEnd(eq(GestureTracker.Gesture.DRAG_X))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()
        // And that we never detected a zoom / quick scale
        verify(gestureHandlerSpy, never()).onScale(any())

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDragX_nonNullViewParent_contentAtEdge_onScrollInterceptDisallowed() {
        val disallowInterceptCaptor = ArgumentCaptor.forClass(Boolean::class.java)
        val viewParentSpy = mock<ViewParent>().apply { requestDisallowInterceptTouchEvent(true) }

        for (event in
            oneFingerDrag(
                start = PointF(50f, 50f),
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event, viewParentSpy, contentAtEdge = true)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(viewParentSpy, atLeastOnce())
            .requestDisallowInterceptTouchEvent(disallowInterceptCaptor.capture())
        assertThat(disallowInterceptCaptor.value).isFalse()

        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG_X))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()
        assertThat(disallowInterceptCaptor.value).isFalse()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }

    @Test
    fun testDragX_nonNullViewParent_contentNotAtEdge_onScrollInterceptAllowed() {
        val disallowInterceptCaptor = ArgumentCaptor.forClass(Boolean::class.java)
        val viewParentSpy = mock<ViewParent>().apply { requestDisallowInterceptTouchEvent(true) }

        for (event in
            oneFingerDrag(
                start = PointF(50f, 50f),
                velocity = Point(ViewConfiguration.get(context).scaledMinimumFlingVelocity / 2, 0),
            )) {
            gestureTracker.feed(event, viewParentSpy, contentAtEdge = false)
        }

        verify(gestureHandlerSpy).onGestureStart()
        verify(gestureHandlerSpy, atLeastOnce()).onScroll(any(), any(), any(), any())
        verify(viewParentSpy, atLeastOnce())
            .requestDisallowInterceptTouchEvent(disallowInterceptCaptor.capture())
        assertThat(disallowInterceptCaptor.value).isTrue()

        verify(gestureHandlerSpy).onGestureEnd(eq(GestureTracker.Gesture.DRAG_X))
        assertThat(gestureTracker.matches(GestureTracker.Gesture.DRAG_X)).isTrue()

        verifyNoMoreInteractions(gestureHandlerSpy)
    }
}

/**
 * Returns a [MotionEvent] with [action] at [downTime] and [location], and with [eventTime] as the
 * time the corresponding [MotionEvent.ACTION_DOWN] was issued
 */
private fun motionEvent(
    action: Int,
    location: PointF,
    downTime: Long = SystemClock.elapsedRealtime(),
    eventTime: Long = SystemClock.elapsedRealtime(),
) = MotionEvent.obtain(downTime, eventTime, action, location.x, location.y, 0)

/** Returns a [MotionEvent] with [MotionEvent.ACTION_DOWN] at [time] and [location] */
private fun down(location: PointF, time: Long = SystemClock.elapsedRealtime()) =
    motionEvent(MotionEvent.ACTION_DOWN, location, downTime = time, eventTime = time)

/**
 * Returns a [MotionEvent] with [MotionEvent.ACTION_MOVE] at [eventTime] and [location] and with
 * [downTime] as the time the corresponding [MotionEvent.ACTION_DOWN] was issued
 */
private fun move(
    location: PointF,
    downTime: Long = SystemClock.elapsedRealtime(),
    eventTime: Long = SystemClock.elapsedRealtime(),
) = motionEvent(MotionEvent.ACTION_MOVE, location, downTime, eventTime)

/**
 * Returns a [MotionEvent] with [MotionEvent.ACTION_UP] at [eventTime] and [location] and with
 * [downTime] as the time the corresponding [MotionEvent.ACTION_DOWN] was issued
 */
private fun up(
    location: PointF,
    downTime: Long = SystemClock.elapsedRealtime(),
    eventTime: Long = SystemClock.elapsedRealtime(),
) = motionEvent(MotionEvent.ACTION_UP, location, downTime, eventTime)

/**
 * Returns a [List] of [MotionEvent] simulating a user dragging one pointers, from [start] at
 * [velocity]
 */
private fun oneFingerDrag(
    start: PointF,
    velocity: Point,
    downTime: Long = SystemClock.elapsedRealtime(),
    skipDown: Boolean = false,
): List<MotionEvent> {
    val sequence = mutableListOf<MotionEvent>()
    if (!skipDown) sequence.add(down(start, time = downTime))
    var x = start.x
    var y = start.y
    // Record 1 move event every 10ms
    for (i in 0..1000 step 10) {
        // Pixels per second, 10ms step
        x += 0.01f * velocity.x
        y += 0.01f * velocity.y
        ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
        sequence.add(move(PointF(x, y), downTime = downTime))
    }
    sequence.add(up(PointF(x, y), downTime = downTime))
    return sequence.toList()
}

/**
 * Returns a [List] of [MotionEvent] simulating a user dragging 2 pointers simultaneously, from
 * [start1] and [start2] at [velocity1] and [velocity2], for 1 second
 */
private fun twoFingerDrag(
    start1: PointF,
    start2: PointF,
    velocity1: Point,
    velocity2: Point,
): List<MotionEvent> {
    // Specify the touch properties for the finger events.
    val pp1 = MotionEvent.PointerProperties()
    pp1.id = 0
    pp1.toolType = MotionEvent.TOOL_TYPE_FINGER
    val pp2 = MotionEvent.PointerProperties()
    pp2.id = 1
    pp2.toolType = MotionEvent.TOOL_TYPE_FINGER
    val pointerProperties = arrayOf(pp1, pp2)

    // Specify the motion properties of the two touch points.
    val pc1 = MotionEvent.PointerCoords()
    pc1.x = start1.x
    pc1.y = start1.y
    pc1.pressure = 1f
    pc1.size = 1f
    val pc2 = MotionEvent.PointerCoords()
    pc2.x = start2.x
    pc2.y = start2.y
    pc2.pressure = 1f
    pc2.size = 1f
    val pointerCoords = arrayOf(pc1, pc2)

    // Two down events, 1 for each pointer
    val downTime = SystemClock.elapsedRealtime()
    val firstFingerEvent =
        MotionEvent.obtain(
            downTime,
            SystemClock.elapsedRealtime(),
            MotionEvent.ACTION_DOWN,
            1,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
    val secondFingerEvent =
        MotionEvent.obtain(
            downTime,
            SystemClock.elapsedRealtime(),
            MotionEvent.ACTION_POINTER_DOWN + (pp2.id shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            2,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
    val sequence = mutableListOf(firstFingerEvent, secondFingerEvent)
    // Compute a series of ACTION_MOVE events with interpolated coordinates for each pointer
    for (i in 0..1000 step 10) {
        pc1.x += 0.01f * velocity1.x
        pc1.y += 0.01f * velocity1.y
        pc2.x += 0.01f * velocity2.x
        pc2.y += 0.01f * velocity2.y

        ShadowLooper.idleMainLooper(10, TimeUnit.MILLISECONDS)
        val twoPointerMove =
            MotionEvent.obtain(
                downTime,
                SystemClock.elapsedRealtime(),
                MotionEvent.ACTION_MOVE,
                2,
                pointerProperties,
                pointerCoords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                0,
                0,
            )
        sequence.add(twoPointerMove)
    }
    // Send 2 up events, one for each pointer
    val secondFingerUpEvent =
        MotionEvent.obtain(
            downTime,
            SystemClock.elapsedRealtime(),
            MotionEvent.ACTION_POINTER_UP,
            2,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
    val firstFingerUpEvent =
        MotionEvent.obtain(
            downTime,
            SystemClock.elapsedRealtime(),
            MotionEvent.ACTION_POINTER_UP,
            1,
            pointerProperties,
            pointerCoords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            0,
            0,
        )
    sequence.add(secondFingerUpEvent)
    sequence.add(firstFingerUpEvent)

    return sequence.toList()
}
