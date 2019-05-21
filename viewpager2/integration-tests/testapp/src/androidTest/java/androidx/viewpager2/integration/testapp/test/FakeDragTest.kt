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
import android.os.Build
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.viewpager2.integration.testapp.FakeDragActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.integration.testapp.test.util.EventRecorder
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrollStateChangedEvent
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageScrolledEvent
import androidx.viewpager2.integration.testapp.test.util.OnPageChangeCallbackEvent.OnPageSelectedEvent
import androidx.viewpager2.integration.testapp.test.util.RetryException
import androidx.viewpager2.integration.testapp.test.util.tryNTimes
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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
            ORIENTATION_PORTRAIT -> swipeLeft()
            else -> throw RuntimeException("Orientation should be landscape or portrait")
        }
    }

    private fun swipePrevious(): ViewAction {
        return when (phoneOrientation) {
            ORIENTATION_LANDSCAPE -> swipeDown()
            ORIENTATION_PORTRAIT -> swipeRight()
            else -> throw RuntimeException("Orientation should be landscape or portrait")
        }
    }

    private fun ensureSettleDirection(swipeAction: () -> Unit) {
        var recorder: EventRecorder
        val initialPage = viewPager.currentItem
        val resetViewPager = {
            activityTestRule.runOnUiThread { viewPager.setCurrentItem(initialPage, false) }
        }

        tryNTimes(if (Build.VERSION.SDK_INT == 23) 3 else 1, resetBlock = resetViewPager) {
            recorder = EventRecorder().also { viewPager.registerOnPageChangeCallback(it) }
            swipeAction()
            if (!recorder.settleDirectionSameAsSwipeDirection()) {
                throw RetryException("Swipe settled in different direction than the swipe itself")
            }
        }
    }

    // region EventRecorder related extensions

    private val OnPageScrolledEvent.exactPosition
        get() = position + positionOffset

    private val List<OnPageScrolledEvent>.deltaSigns
        get() = zipWithNext().map { sign(it.second.exactPosition - it.first.exactPosition) }

    private fun EventRecorder.settleDirectionSameAsSwipeDirection(): Boolean {
        val startDraggingEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING)
        val startSettlingEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING)
        val idleEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE)

        val swipeEvents = allEvents
            .dropWhile { it != startDraggingEvent }.drop(1)
            .takeWhile { it != startSettlingEvent }
            .map { it as OnPageScrolledEvent }
        val settleEvents = allEvents
            .dropWhile { it != startSettlingEvent }.drop(1)
            .dropWhile { it is OnPageSelectedEvent }
            .takeWhile { it != idleEvent }
            .map { it as OnPageScrolledEvent }

        val swipeDeltaSigns = swipeEvents.deltaSigns.distinct()
        val settleDeltaSigns = settleEvents.deltaSigns.distinct()

        return swipeDeltaSigns.size == 1 && swipeDeltaSigns == settleDeltaSigns
    }

    // endregion
}
