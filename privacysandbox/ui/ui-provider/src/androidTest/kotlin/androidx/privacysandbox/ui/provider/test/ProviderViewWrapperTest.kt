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

package androidx.privacysandbox.ui.provider.test

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback
import androidx.privacysandbox.ui.provider.ProviderViewWrapper
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProviderViewWrapperTest {
    companion object {
        const val TIMEOUT_MILLIS: Long = 2000
        const val VSYNC_INTERVAL_MS: Long = 100
        const val WIDTH = 500
        const val HEIGHT = 500
    }

    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var mainHandler: Handler
    private lateinit var providerViewWrapper: ProviderViewWrapper
    private lateinit var providerView: View
    private var dispatchedEventsSinceLastFrame = 0L
    private lateinit var motionEventTransferCallback: MotionEventTransferCallbackProxy

    @Before
    fun setup() {
        activityRule.scenario.onActivity { activity ->
            mainHandler = Handler(Looper.getMainLooper())
            providerViewWrapper = ProviderViewWrapper(activity)
            providerView = View(activity)
            providerView.layoutParams = FrameLayout.LayoutParams(WIDTH, HEIGHT)
            providerViewWrapper.addView(providerView)
            activity.setContentView(providerViewWrapper)
            setUpOnTouchListener()
        }
        motionEventTransferCallback = MotionEventTransferCallbackProxy()
    }

    @Test
    fun processingOfMotionEventsWillNotScheduledIfProviderViewDetachedTest() {
        // Closing the activity, detaching the View.
        activityRule.scenario.close()

        val frameTimes = simulateFrameTimes(/* frames */ 3)

        val events = createGestureEvents(/* moveEventNumbers */ 1)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                -(VSYNC_INTERVAL_MS / 2), // (vsync_interval / 2) before frame3
            )
        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames,
        )

        val expectedEventsPerFrame = listOf<Long>(0, 0, 0)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    @Test
    fun removePendingMotionEventDispatchMessagesIfViewIsDetachedTest() {
        val frameTimes = simulateFrameTimes(/* frames */ 3)

        val events = createGestureEvents(/* moveEventNumbers */ 1)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                -(VSYNC_INTERVAL_MS / 2), // (vsync_interval / 2) before frame3
            )
        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames,
        )

        activityRule.scenario.close()

        val expectedEventsPerFrame = listOf<Long>(0, 0, 0)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    @Test
    fun processMotionEventsTargetingNextFrameTest() {
        val frameTimes = simulateFrameTimes(/* frames */ 5)

        // To simulate real scenarios, setting DOWN, UP and last MOVE as unbuffered events, and
        // send them randomly between frames.
        val events = createGestureEvents(/* moveEventNumbers */ 3)
        val eventTargetTimesRelativeToFrames =
            listOf<Long>(
                VSYNC_INTERVAL_MS / 2, // (vsync_interval / 2) after frame1
                0, // at frame2 time
                0, // at frame3 time
                -(VSYNC_INTERVAL_MS / 2), // (vsync_interval / 2) before frame4
                -(VSYNC_INTERVAL_MS / 2), // (vsync_interval / 2) before frame5
            )

        simulateSchedulingEventDispatchingMessages(
            events,
            frameTimes,
            eventTargetTimesRelativeToFrames,
        )

        val expectedEventsPerFrame = listOf<Long>(0, 1, 1, 2, 1)
        assertNumberOfDispatchedEventsOnSimulateFrameTimes(frameTimes, expectedEventsPerFrame)
    }

    @Test
    fun requestDisallowInterceptTest() {
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isFalse()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent, motionEventTransferCallback)

        activityRule.scenario.onActivity { _ ->
            providerViewWrapper.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(false)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isFalse()
    }

    @Test
    fun requestDisallowInterceptOnlyCalledOnLastTransferredCallbackTest() {
        val eventTransferCallback1 = MotionEventTransferCallbackProxy()
        val eventTransferCallback2 = MotionEventTransferCallbackProxy()
        assertThat(eventTransferCallback1.lastValueForDisallowIntercept).isFalse()
        assertThat(eventTransferCallback2.lastValueForDisallowIntercept).isFalse()

        val downEvent1 = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent1, eventTransferCallback1)
        val downEvent2 = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent2, eventTransferCallback2)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(eventTransferCallback1.lastValueForDisallowIntercept).isFalse()
        assertThat(eventTransferCallback2.lastValueForDisallowIntercept).isTrue()
    }

    @Test
    fun requestDisallowInterceptWillOnlyProxiedOnChangeValueTest() {
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isFalse()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(0)

        val downEvent1 = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent1, motionEventTransferCallback)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(false)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isFalse()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(0)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(1)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(1)
    }

    @Test
    fun cachingLastValueOfRequestDisallowInterceptShouldResetForNewGestureTest() {
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isFalse()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(0)

        val downEvent1 = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent1, motionEventTransferCallback)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(1)

        val downEvent2 = createMotionEvent(MotionEvent.ACTION_DOWN)
        scheduleAndWaitForMotionEventProcessing(downEvent2, motionEventTransferCallback)
        // Calling requestDisallowIntercept with false, will have no effect for the new gesture.
        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(false)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(1)

        activityRule.scenario.onActivity { _ ->
            providerView.parent.requestDisallowInterceptTouchEvent(true)
        }
        assertThat(motionEventTransferCallback.lastValueForDisallowIntercept).isTrue()
        assertThat(motionEventTransferCallback.numberOfRequestDisallowInterceptCalls).isEqualTo(2)
    }

    private fun setUpOnTouchListener() {
        providerView.setOnTouchListener { _, _ ->
            dispatchedEventsSinceLastFrame++
            true
        }
    }

    private fun simulateFrameTimes(frames: Int): List<Long> {
        val firstFrameTime = SystemClock.uptimeMillis() + VSYNC_INTERVAL_MS
        return List(frames) { index -> firstFrameTime + (index * VSYNC_INTERVAL_MS) }
    }

    private fun createGestureEvents(moveEventNumbers: Int): List<MotionEvent> {
        val events: MutableList<MotionEvent> = mutableListOf()
        events.add(createMotionEvent(MotionEvent.ACTION_DOWN))
        repeat(moveEventNumbers) { events.add(createMotionEvent(MotionEvent.ACTION_MOVE)) }
        events.add(createMotionEvent(MotionEvent.ACTION_UP))
        return events
    }

    private fun createMotionEvent(motionEventAction: Int): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            motionEventAction,
            providerView.width / 2f,
            providerView.width / 2f,
            /* metaState = */ 0,
        )
    }

    private fun simulateSchedulingEventDispatchingMessages(
        events: List<MotionEvent>,
        frameTimes: List<Long>,
        eventTargetTimesRelativeToFrames: List<Long>,
    ) {
        val eventTargetTime =
            eventTargetTimesRelativeToFrames.withIndex().map { (index, value) ->
                frameTimes[index] + value
            }

        for (i in 0 until events.size) {
            providerViewWrapper.scheduleMotionEventProcessing(
                events[i],
                eventTargetTime[i],
                motionEventTransferCallback,
            )
        }
    }

    private fun assertNumberOfDispatchedEventsOnSimulateFrameTimes(
        frameTimes: List<Long>,
        expectedDispatchedEventsPerFrame: List<Long>,
    ) {
        val allFramesPassedLatch = CountDownLatch(frameTimes.size)
        // Simulating doFrame at the frame times.
        for (i in 0 until frameTimes.size) {
            mainHandler.postAtTime(
                {
                    if (dispatchedEventsSinceLastFrame == expectedDispatchedEventsPerFrame[i]) {
                        allFramesPassedLatch.countDown()
                    }
                    dispatchedEventsSinceLastFrame = 0
                },
                frameTimes[i],
            )
        }
        assertTrue(
            "Timeout before passing all frames",
            allFramesPassedLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
        )
    }

    fun scheduleAndWaitForMotionEventProcessing(
        motionEvent: MotionEvent,
        eventTransferCallback: MotionEventTransferCallbackProxy,
    ) {
        val eventDispatchLatch = CountDownLatch(1)
        activityRule.scenario.onActivity { _ ->
            providerView.setOnTouchListener { _, event ->
                eventDispatchLatch.countDown()
                providerView.onTouchEvent(event)
                true
            }
        }
        providerViewWrapper.scheduleMotionEventProcessing(
            motionEvent,
            motionEvent.eventTime,
            eventTransferCallback,
        )
        assertTrue(
            "dispatchTouchEvent on providerView was not called within the timeout",
            eventDispatchLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS),
        )
    }

    class MotionEventTransferCallbackProxy : IMotionEventTransferCallback.Stub() {
        var lastValueForDisallowIntercept = false
        var numberOfRequestDisallowInterceptCalls = 0

        override fun requestDisallowIntercept(disallowIntercept: Boolean) {
            lastValueForDisallowIntercept = disallowIntercept
            numberOfRequestDisallowInterceptCalls++
        }
    }
}
