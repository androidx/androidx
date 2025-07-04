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
    private var lastTransferredMotionEvent: MotionEvent? = null
    private var lastTransferredEventTargetFrameTime: Long = 0L
    private var lastTransferredEventCallback: IMotionEventTransferCallback? = null

    @Before
    fun setup() {
        lastTransferredMotionEvent = null
        lastTransferredEventTargetFrameTime = 0L
        lastTransferredEventCallback = null

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
                    eventTargetFrameTime: Long,
                    eventTransferCallback: IMotionEventTransferCallback?,
                ) {
                    lastTransferredMotionEvent = motionEvent
                    lastTransferredEventTargetFrameTime = eventTargetFrameTime
                    lastTransferredEventCallback = eventTransferCallback
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
    fun transferEventsWithTargetTimeOnTouchEventTest() {
        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        val timeNow = AnimationUtils.currentAnimationTimeMillis()
        contentView.onTouchEvent(downEvent)

        assertThat(lastTransferredMotionEvent).isEqualTo(downEvent)
        assertThat(lastTransferredEventTargetFrameTime).isAtLeast(timeNow)
        assertThat(lastTransferredEventCallback).isNotNull()
    }

    @Test
    fun requestDisallowInterceptShouldNotCallParentIfContentViewIsDetachedTest() {
        // Closing the activity, detaching the View.
        activityScenarioRule.scenario.close()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        contentView.onTouchEvent(downEvent)
        assertThat(lastTransferredEventCallback).isNotNull()

        requestDisallowInterceptTracker.resetCountDownLatch()
        lastTransferredEventCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsNotCalled()
    }

    @Test
    fun requestDisallowInterceptShouldBeOnlyApplicableOnRunningGestureTest() {
        val downEvent1 = createMotionEvent(MotionEvent.ACTION_DOWN)
        contentView.onTouchEvent(downEvent1)
        assertThat(lastTransferredEventCallback).isNotNull()
        val previousGestureCallback = lastTransferredEventCallback

        val downEvent2 = createMotionEvent(MotionEvent.ACTION_DOWN)
        contentView.onTouchEvent(downEvent2)

        requestDisallowInterceptTracker.resetCountDownLatch()
        previousGestureCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsNotCalled()

        lastTransferredEventCallback!!.requestDisallowIntercept(true)
        requestDisallowInterceptTracker.assertRequestDisallowInterceptIsCalled()
    }

    @Test
    fun passMotionEventTransferCallbackAsNullOnUp() {
        assertThat(lastTransferredEventCallback).isNull()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        contentView.onTouchEvent(downEvent)
        assertThat(lastTransferredEventCallback).isNotNull()

        val cancelEvent = createMotionEvent(MotionEvent.ACTION_UP)
        contentView.onTouchEvent(cancelEvent)
        assertThat(lastTransferredEventCallback).isNull()
    }

    @Test
    fun passMotionEventTransferCallbackAsNullOnCancel() {
        assertThat(lastTransferredEventCallback).isNull()

        val downEvent = createMotionEvent(MotionEvent.ACTION_DOWN)
        contentView.onTouchEvent(downEvent)
        assertThat(lastTransferredEventCallback).isNotNull()

        val cancelEvent = createMotionEvent(MotionEvent.ACTION_CANCEL)
        contentView.onTouchEvent(cancelEvent)
        assertThat(lastTransferredEventCallback).isNull()
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
