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

package androidx.privacysandbox.ui.client.test

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.privacysandbox.ui.client.ContentView
import androidx.privacysandbox.ui.client.IRemoteSessionController
import androidx.privacysandbox.ui.core.IMotionEventTransferCallback
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ContentViewTest {
    companion object {
        const val WIDTH = 500
        const val HEIGHT = 500
    }

    @get:Rule var activityScenarioRule = ActivityScenarioRule(UiLibActivity::class.java)

    private lateinit var contentView: ContentView
    private lateinit var requestDisallowInterceptTracker: RequestDisallowInterceptTracker
    private var lastTransferredTouchEvent: MotionEvent? = null
    private var lastTransferredTouchEventTargetTime: Long = 0L
    private var lastTransferredTouchEventCallback: IMotionEventTransferCallback? = null
    private var lastTransferredHoverEvent: MotionEvent? = null
    private var lastTransferredHoverEventTargetTime: Long = 0L

    @Before
    fun setup() {
        val remoteController =
            object : IRemoteSessionController {
                override fun close() {}

                override fun notifyConfigurationChanged(configuration: Configuration) {}

                override fun notifyResized(width: Int, height: Int) {}

                override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}

                override fun notifyFetchUiForSession() {}

                override fun notifyUiChanged(uiContainerInfo: Bundle) {}

                override fun notifySessionRendered(supportedSignalOptions: List<String>) {}

                override fun notifyMotionEvent(
                    motionEvent: MotionEvent,
                    eventTargetTime: Long,
                    eventTransferCallback: IMotionEventTransferCallback?,
                ) {
                    lastTransferredTouchEvent = motionEvent
                    lastTransferredTouchEventTargetTime = eventTargetTime
                    lastTransferredTouchEventCallback = eventTransferCallback
                }

                override fun notifyHoverEvent(hoverEvent: MotionEvent, eventTargetTime: Long) {
                    lastTransferredHoverEvent = hoverEvent
                    lastTransferredHoverEventTargetTime = eventTargetTime
                }
            }

        activityScenarioRule.scenario.onActivity { activity ->
            requestDisallowInterceptTracker = RequestDisallowInterceptTracker(activity)
            contentView = ContentView(activity, remoteController)
            contentView.layoutParams = FrameLayout.LayoutParams(WIDTH, HEIGHT)
            requestDisallowInterceptTracker.addView(contentView)
            activity.setContentView(requestDisallowInterceptTracker)
        }
    }

    @Test
    fun transferTouchEventsWithTargetTimeOnTouchEventTest() {
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        val timeNow = AnimationUtils.currentAnimationTimeMillis()
        contentView.onTouchEvent(downEvent)

        assertThat(lastTransferredTouchEvent).isEqualTo(downEvent)
        assertThat(lastTransferredTouchEventTargetTime).isAtLeast(timeNow)
        assertThat(lastTransferredTouchEventCallback).isNotNull()
    }

    @Test
    fun transferHoverEventsWithTargetTimeOnTouchEventTest() {
        val hoverEvent = createMotionEvent(MotionEvent.ACTION_HOVER_ENTER)
        val timeNow = AnimationUtils.currentAnimationTimeMillis()
        activityScenarioRule.scenario.onActivity { activity ->
            contentView.onHoverEvent(hoverEvent)
        }

        assertThat(lastTransferredHoverEvent).isEqualTo(hoverEvent)
        assertThat(lastTransferredHoverEventTargetTime).isAtLeast(timeNow)
    }

    @Test
    fun requestDisallowInterceptShouldNotCallParentIfContentViewIsDetachedTest() {
        // Detaching the view by removing from parent.
        activityScenarioRule.scenario.onActivity { activity ->
            (contentView.parent as ViewGroup).removeView(contentView)
        }

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        activityScenarioRule.scenario.onActivity { activity -> contentView.onTouchEvent(downEvent) }
        assertThat(lastTransferredTouchEventCallback).isNotNull()

        requestDisallowInterceptTracker.resetCountDownLatch()
        lastTransferredTouchEventCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsNotCalled()
    }

    @Test
    fun requestDisallowInterceptShouldBeOnlyApplicableOnRunningGestureTest() {
        val downEvent1 = createMotionEvent(MotionEvent.ACTION_DOWN)
        activityScenarioRule.scenario.onActivity { activity ->
            contentView.onTouchEvent(downEvent1)
        }
        assertThat(lastTransferredTouchEventCallback).isNotNull()
        val previousGestureCallback = lastTransferredTouchEventCallback

        val downEvent2 = createMotionEvent(MotionEvent.ACTION_DOWN)
        activityScenarioRule.scenario.onActivity { activity ->
            contentView.onTouchEvent(downEvent2)
        }

        requestDisallowInterceptTracker.resetCountDownLatch()
        previousGestureCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsNotCalled()

        lastTransferredTouchEventCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsCalled()
    }

    @Test
    fun passMotionEventTransferCallbackAsNullOnUp() {
        assertThat(lastTransferredTouchEventCallback).isNull()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        activityScenarioRule.scenario.onActivity { activity -> contentView.onTouchEvent(downEvent) }
        assertThat(lastTransferredTouchEventCallback).isNotNull()

        val upEvent = createMotionEvent(MotionEvent.ACTION_UP)
        activityScenarioRule.scenario.onActivity { activity -> contentView.onTouchEvent(upEvent) }
        assertThat(lastTransferredTouchEventCallback).isNull()
    }

    @Test
    fun passMotionEventTransferCallbackAsNullOnCancel() {
        assertThat(lastTransferredTouchEventCallback).isNull()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        activityScenarioRule.scenario.onActivity { activity -> contentView.onTouchEvent(downEvent) }
        assertThat(lastTransferredTouchEventCallback).isNotNull()

        val cancelEvent = createMotionEvent(MotionEvent.ACTION_CANCEL)
        activityScenarioRule.scenario.onActivity { activity ->
            contentView.onTouchEvent(cancelEvent)
        }
        assertThat(lastTransferredTouchEventCallback).isNull()
    }

    private class RequestDisallowInterceptTracker(context: Context) : FrameLayout(context) {
        var requestDisallowInterceptCountDownLatch: CountDownLatch? = null

        override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            requestDisallowInterceptCountDownLatch?.countDown()
        }

        fun resetCountDownLatch() {
            requestDisallowInterceptCountDownLatch = CountDownLatch(1)
        }

        fun assertRequestDisallowInterceptIsCalled() {
            assertThat(requestDisallowInterceptCountDownLatch!!.await(1, TimeUnit.SECONDS)).isTrue()
        }

        fun assertRequestDisallowInterceptIsNotCalled() {
            assertThat(requestDisallowInterceptCountDownLatch!!.await(1, TimeUnit.SECONDS))
                .isFalse()
        }
    }

    private fun createMotionEvent(motionEventAction: Int): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            motionEventAction,
            0f,
            0f,
            /* metaState = */ 0,
        )
    }
}
