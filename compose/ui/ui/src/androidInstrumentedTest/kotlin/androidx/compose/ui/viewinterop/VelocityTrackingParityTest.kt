/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastFirstOrNull
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swiper
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.errorprone.annotations.CanIgnoreReturnValue
import kotlin.math.absoluteValue
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class VelocityTrackingParityTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val draggableView: VelocityTrackingView
        get() = rule.activity.findViewById(R.id.draggable_view)

    private val composeView: ComposeView
        get() = rule.activity.findViewById(R.id.compose_view)

    private var latestComposeVelocity = Velocity.Zero

    @Before
    fun setUp() {
        latestComposeVelocity = Velocity.Zero
        VelocityTrackerAddPointsFix = true
    }

    fun tearDown() {
        draggableView.tearDown()
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_smallVeryFast() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        smallGestureVeryFast(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_smallFast() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        smallGestureFast(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_smallSlow() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        smallGestureSlow(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }
        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_largeFast() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        largeGestureFast(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_largeVeryFast() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        largeGestureVeryFast(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_orthogonal() {
        // Arrange
        createActivity(true)
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        orthogonalGesture(R.id.draggable_view)

        val latestVelocityInViewX = draggableView.latestVelocity.x
        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.x, latestVelocityInViewX)
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_regularSituationOne() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        regularGestureOne(R.id.draggable_view)

        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    @Test
    fun equalDraggable_withEqualSwipes_shouldProduceSimilarVelocity_regularSituationTwo() {
        // Arrange
        createActivity()
        checkVisibility(composeView, View.GONE)
        checkVisibility(draggableView, View.VISIBLE)

        // Act: Use system to send motion events and collect them.
        regularGestureTwo(R.id.draggable_view)

        val latestVelocityInViewY = draggableView.latestVelocity.y

        // switch visibility
        rule.runOnUiThread {
            composeView.visibility = View.VISIBLE
            draggableView.visibility = View.GONE
        }

        checkVisibility(composeView, View.VISIBLE)
        checkVisibility(draggableView, View.GONE)

        assertTrue { isValidGesture(draggableView.motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        for (event in draggableView.motionEvents) {
            composeView.dispatchTouchEvent(event)
        }

        // assert
        assertIsWithinTolerance(latestComposeVelocity.y, latestVelocityInViewY)
    }

    private fun createActivity(twoDimensional: Boolean = false) {
        rule.activityRule.scenario.createActivityWithComposeContent(
            R.layout.velocity_tracker_compose_vs_view
        ) {
            TestComposeDraggable(twoDimensional) { latestComposeVelocity = it }
        }
    }

    private fun checkVisibility(view: View, visibility: Int) = assertTrue {
        view.visibility == visibility
    }

    private fun assertIsWithinTolerance(composeVelocity: Float, viewVelocity: Float) {
        if (composeVelocity.absoluteValue > 1f && viewVelocity.absoluteValue > 1f) {
            val tolerance = VelocityDifferenceTolerance * kotlin.math.abs(viewVelocity)
            assertThat(composeVelocity).isWithin(tolerance).of(viewVelocity)
        } else {
            assertThat(composeVelocity.toInt()).isEqualTo(viewVelocity.toInt())
        }
    }
}

internal fun smallGestureVeryFast(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(15),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, 0f, -50f),
            )
        )
}

internal fun smallGestureFast(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(25),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, 0f, -50f),
            )
        )
}

internal fun smallGestureSlow(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(200),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, 0f, -50f),
            )
        )
}

internal fun largeGestureFast(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(25),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, 0f, -500f),
            )
        )
}

internal fun largeGestureVeryFast(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(15),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, 0f, -500f),
            )
        )
}

internal fun orthogonalGesture(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(50),
                GeneralLocation.CENTER,
                GeneralLocation.translate(GeneralLocation.CENTER, -200f, -200f),
            )
        )
}

internal fun regularGestureOne(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(
                SwiperWithTime(100),
                GeneralLocation.CENTER,
                GeneralLocation.BOTTOM_CENTER,
            )
        )
}

internal fun regularGestureTwo(id: Int) {
    Espresso.onView(withId(id))
        .perform(
            espressoSwipe(SwiperWithTime(70), GeneralLocation.CENTER, GeneralLocation.TOP_CENTER)
        )
}

private fun espressoSwipe(
    swiper: Swiper,
    start: CoordinatesProvider,
    end: CoordinatesProvider,
): GeneralSwipeAction {
    return GeneralSwipeAction(swiper, start, end, Press.FINGER)
}

@Composable
fun TestComposeDraggable(
    twoDimensional: Boolean = false,
    onDragStopped: (velocity: Velocity) -> Unit,
) {
    val viewConfiguration =
        object : ViewConfiguration by LocalViewConfiguration.current {
            override val maximumFlingVelocity: Float
                get() = Float.MAX_VALUE // unlimited
        }
    CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black)
                .then(
                    if (twoDimensional) {
                        Modifier.draggable2D(
                            rememberDraggable2DState {},
                            onDragStopped = onDragStopped,
                        )
                    } else {
                        Modifier.draggable(
                            rememberDraggableState(onDelta = {}),
                            onDragStopped = { onDragStopped.invoke(Velocity(0.0f, it)) },
                            orientation = Orientation.Vertical,
                        )
                    }
                )
        )
    }
}

private fun ActivityScenario<*>.createActivityWithComposeContent(
    @LayoutRes layout: Int,
    content: @Composable () -> Unit,
) {
    onActivity { activity ->
        activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light)
        activity.setContentView(layout)
        with(activity.findViewById<ComposeView>(R.id.compose_view)) {
            setContent(content)
            visibility = View.GONE
        }

        activity.findViewById<VelocityTrackingView>(R.id.draggable_view)?.visibility = View.VISIBLE
    }
    moveToState(Lifecycle.State.RESUMED)
}

/** A view that adds data to a VelocityTracker. */
private class VelocityTrackingView(context: Context, attributeSet: AttributeSet) :
    View(context, attributeSet) {
    private val tracker = VelocityTracker.obtain()
    var latestVelocity: Velocity = Velocity.Zero
    val motionEvents = mutableListOf<MotionEvent?>()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        motionEvents.add(MotionEvent.obtain(event))
        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                tracker.computeCurrentVelocity(1000)
                latestVelocity = Velocity(tracker.xVelocity, tracker.yVelocity)
                tracker.clear()
            }
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> tracker.addMovement(event)
            else -> {
                tracker.clear()
                latestVelocity = Velocity.Zero
            }
        }
        return true
    }

    fun tearDown() {
        tracker.recycle()
    }
}

/** Checks the contents of [events] represents a swipe gesture. */
internal fun isValidGesture(events: List<MotionEvent>): Boolean {
    val down = events.filter { it.action == MotionEvent.ACTION_DOWN }
    val move = events.filter { it.action == MotionEvent.ACTION_MOVE }
    val up = events.filter { it.action == MotionEvent.ACTION_UP }
    return down.size == 1 && move.isNotEmpty() && up.size == 1
}

// 5% tolerance
private const val VelocityDifferenceTolerance = 0.05f

/** Copied from androidx.test.espresso.action.Swipe */
internal data class SwiperWithTime(val gestureDurationMs: Int) : Swiper {
    override fun sendSwipe(
        uiController: UiController,
        startCoordinates: FloatArray,
        endCoordinates: FloatArray,
        precision: FloatArray,
    ): Swiper.Status {
        return sendLinearSwipe(
            uiController,
            startCoordinates,
            endCoordinates,
            precision,
            gestureDurationMs,
        )
    }

    private fun checkElementIndex(index: Int, size: Int): Int {
        return checkElementIndex(index, size, "index")
    }

    @CanIgnoreReturnValue
    private fun checkElementIndex(index: Int, size: Int, desc: String): Int {
        // Carefully optimized for execution by hotspot (explanatory comment above)
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException(badElementIndex(index, size, desc))
        }
        return index
    }

    private fun badElementIndex(index: Int, size: Int, desc: String): String {
        return if (index < 0) {
            String.format("%s (%s) must not be negative", desc, index)
        } else if (size < 0) {
            throw IllegalArgumentException("negative size: $size")
        } else { // index >= size
            String.format("%s (%s) must be less than size (%s)", desc, index, size)
        }
    }

    private fun interpolate(start: FloatArray, end: FloatArray, steps: Int): Array<FloatArray> {
        checkElementIndex(1, start.size)
        checkElementIndex(1, end.size)
        val res = Array(steps) { FloatArray(2) }
        for (i in 1 until steps + 1) {
            res[i - 1][0] = start[0] + (end[0] - start[0]) * i / (steps + 2f)
            res[i - 1][1] = start[1] + (end[1] - start[1]) * i / (steps + 2f)
        }
        return res
    }

    private fun sendLinearSwipe(
        uiController: UiController,
        startCoordinates: FloatArray,
        endCoordinates: FloatArray,
        precision: FloatArray,
        duration: Int,
    ): Swiper.Status {
        val steps = interpolate(startCoordinates, endCoordinates, 10)
        val events: MutableList<MotionEvent> = ArrayList()
        val downEvent = MotionEvents.obtainDownEvent(startCoordinates, precision)
        events.add(downEvent)
        try {
            val intervalMS = (duration / steps.size).toLong()
            var eventTime = downEvent.downTime
            for (step in steps) {
                eventTime += intervalMS
                events.add(MotionEvents.obtainMovement(downEvent, eventTime, step))
            }
            eventTime += intervalMS
            events.add(MotionEvents.obtainUpEvent(downEvent, eventTime, endCoordinates))
            uiController.injectMotionEventSequence(events)
        } catch (e: Exception) {
            return Swiper.Status.FAILURE
        } finally {
            for (event in events) {
                event.recycle()
            }
        }
        return Swiper.Status.SUCCESS
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}
