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

package androidx.privacysandbox.ui.provider.test

import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.Display
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.provider.TouchFocusTransferringView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.slowSwipeLeft
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RunWith(AndroidJUnit4::class)
@LargeTest
class BinderAdapterDelegateTest {
    companion object {
        const val TIMEOUT_MILLIS: Long = 2000
        const val WIDTH = 500
        const val HEIGHT = 500
        const val SURFACE_VIEW_RES = "androidx.privacysandbox.ui.provider.test:id/surface_view"
    }

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private val transferTouchFocusLatch = CountDownLatch(1)

    @Before
    fun setUp() {
        Assume.assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activity = activityScenarioRule.withActivity { this }
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        activity.runOnUiThread {
            val surfaceView = activity.findViewById<SurfaceView>(R.id.surface_view)
            val surfaceControlViewHost =
                GestureTransferringSurfaceControlViewHost(
                    activity, activity.display!!, Binder(), transferTouchFocusLatch)
            val touchFocusTransferringView =
                TouchFocusTransferringView(context, surfaceControlViewHost)
            touchFocusTransferringView.addView(TestView(context))
            surfaceControlViewHost.setView(touchFocusTransferringView, WIDTH, HEIGHT)
            surfaceView.setChildSurfacePackage(surfaceControlViewHost.surfacePackage!!)
            surfaceView.setZOrderOnTop(true)
        }
    }

    @Test
    fun touchFocusTransferredForSwipeUp() {
        onView(withId(R.id.surface_view)).perform(swipeUp())
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun touchFocusNotTransferredForSwipeLeft() {
        onView(withId(R.id.surface_view)).perform(swipeLeft())
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun touchFocusNotTransferredForSlowSwipeLeft() {
        onView(withId(R.id.surface_view)).perform(slowSwipeLeft())
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun touchFocusNotTransferredForClicks() {
        onView(withId(R.id.surface_view)).perform(click())
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isFalse()
    }

    @Test
    fun touchFocusTransferredForFlingForward() {
        val scrollView = UiScrollable(UiSelector().resourceId(SURFACE_VIEW_RES))
        scrollView.flingForward()
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun touchFocusTransferredForFlingBackward() {
        val scrollView = UiScrollable(UiSelector().resourceId(SURFACE_VIEW_RES))
        scrollView.flingBackward()
        assertThat(transferTouchFocusLatch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).isTrue()
    }

    /**
     * SCVH that takes note of when touch focus is transferred.
     *
     * TODO(b/290629538): Add full integration test.
     */
    private class GestureTransferringSurfaceControlViewHost(
        context: Context,
        display: Display,
        hostToken: IBinder,
        countDownLatch: CountDownLatch
    ) : SurfaceControlViewHost(context, display, hostToken) {

        val latch = countDownLatch

        override fun transferTouchGestureToHost(): Boolean {
            latch.countDown()
            return true
        }
    }

    private class TestView(context: Context) : View(context) {
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            return true
        }
    }
}
