/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.viewpager2.integration.testapp.test

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.VelocityTracker
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.viewpager2.integration.testapp.FakeDragActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.integration.testapp.test.util.EventRecorder
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrollStateChangedEvent
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrolledEvent
import androidx.viewpager2.integration.testapp.test.util.RetryException
import androidx.viewpager2.integration.testapp.test.util.tryNTimes
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.lang.reflect.Field
import kotlin.math.sign

@LargeTest
@RunWith(Parameterized::class)
class FakeDragTest(private val config: TestConfig) :
    BaseTest<FakeDragActivity>(FakeDragActivity::class.java) {
    data class TestConfig(
        val orientation: Int
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> {
            return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).map { orientation ->
                TestConfig(orientation)
            }
        }

        val mFakeDragger: Field
        val mVelocityTracker: Field

        init {
            mFakeDragger = ViewPager2::class.java.getDeclaredField("mFakeDragger")
            mFakeDragger.isAccessible = true

            val fakeDragClass = Class.forName("androidx.viewpager2.widget.FakeDrag")
            mVelocityTracker = fakeDragClass.getDeclaredField("mVelocityTracker")
            mVelocityTracker.isAccessible = true
        }
    }

    private val twoOfSpadesPage = "2\n♣"
    private val threeOfSpadesPage = "3\n♣"

    override val layoutId get() = R.id.viewPager

    private val phoneOrientation
        get() = getInstrumentation().targetContext.resources.configuration.orientation

    @Before
    override fun setUp() {
        super.setUp()
        selectOrientation(config.orientation)
    }

    @Test
    fun testFakeDragging() {
        // test if ViewPager2 goes to the next page when fake dragging
        fakeDragForward()
        verifyCurrentPage(threeOfSpadesPage)

        // test if ViewPager2 goes back to the first page when fake dragging the other way
        fakeDragBackward()
        verifyCurrentPage(twoOfSpadesPage)
    }

    private fun fakeDragForward() {
        ensureSettleDirection {
            onTouchpad().perform(swipeNext())
            idleWatcher.waitForIdle()
            onIdle()
        }
    }

    private fun fakeDragBackward() {
        ensureSettleDirection {
            onTouchpad().perform(swipePrevious())
            idleWatcher.waitForIdle()
            onIdle()
        }
    }

    private fun onTouchpad(): ViewInteraction {
        return onView(withId(R.id.touchpad))
    }

    private fun swipeNext(): ViewAction {
        return when (phoneOrientation) {
            ORIENTATION_LANDSCAPE -> swipeUp()
            ORIENTATION_PORTRAIT -> if (isRtl) swipeRight() else swipeLeft()
            else -> throw RuntimeException("Orientation should be landscape or portrait")
        }
    }

    private fun swipePrevious(): ViewAction {
        return when (phoneOrientation) {
            ORIENTATION_LANDSCAPE -> swipeDown()
            ORIENTATION_PORTRAIT -> if (isRtl) swipeLeft() else swipeRight()
            else -> throw RuntimeException("Orientation should be landscape or portrait")
        }
    }

    private fun ensureSettleDirection(swipeAction: () -> Unit) {
        var recorder: EventRecorder
        val initialPage = viewPager.currentItem
        val resetViewPager = {
            activityTestRule.runOnUiThread { viewPager.setCurrentItem(initialPage, false) }
        }

        tryNTimes(3, resetBlock = resetViewPager) {
            recorder = EventRecorder().also { viewPager.registerOnPageChangeCallback(it) }
            swipeAction()
            if (recorder.swipeDirection != viewPager.fakeDragVelocityDirection) {
                // Retry if VelocityTracker calculates velocity in opposite direction of the swipe
                // http://issuetracker.google.com/37048172
                throw RetryException("Fling was in opposite direction of the swipe")
            }
        }
    }

    private val ViewPager2.fakeDragVelocityDirection: Float
        get() {
            val fakeDragger = mFakeDragger.get(this)
            val velocityTracker = mVelocityTracker.get(fakeDragger) as VelocityTracker
            return if (orientation == ORIENTATION_HORIZONTAL) {
                sign(velocityTracker.xVelocity)
            } else {
                sign(velocityTracker.yVelocity)
            }
        }

    // region EventRecorder related extensions

    private val OnPageScrolledEvent.exactPosition
        get() = position + positionOffset

    private val List<OnPageScrolledEvent>.deltaSigns
        get() = zipWithNext().map { sign(it.second.exactPosition - it.first.exactPosition) }

    private val EventRecorder.swipeDirection: Float
        get() {
            val startDraggingEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING)
            val swipeDeltaSigns = allEvents
                .dropWhile { it != startDraggingEvent }.drop(1)
                .takeWhile { it is OnPageScrolledEvent }
                .map { it as OnPageScrolledEvent }
                .deltaSigns
                .distinct()
            assertThat(swipeDeltaSigns.size, equalTo(1))
            return swipeDeltaSigns.first()
        }

    // endregion
}
