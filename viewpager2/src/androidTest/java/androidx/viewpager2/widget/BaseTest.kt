/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.os.Build
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.testutils.FragmentActivityUtils
import androidx.viewpager2.test.R
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ScrollState.IDLE
import androidx.viewpager2.widget.swipe.BaseActivity
import androidx.viewpager2.widget.swipe.PageSwiper
import androidx.viewpager2.widget.swipe.ViewAdapterActivity
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import java.util.concurrent.CountDownLatch

open class BaseTest {
    fun setUpTest(
        totalPages: Int,
        @ViewPager2.Orientation orientation: Int,
        activityClass: Class<out BaseActivity> = ViewAdapterActivity::class.java
    ): Context {
        val activityTestRule = ActivityTestRule(activityClass, true, false)
        activityTestRule.launchActivity(BaseActivity.createIntent(totalPages))

        val viewPager: ViewPager2 = activityTestRule.activity.findViewById(R.id.view_pager)
        activityTestRule.runOnUiThread { viewPager.orientation = orientation }
        onView(withId(R.id.view_pager)).check(matches(isDisplayed()))

        val mPageSwiper = PageSwiper(totalPages, viewPager.orientation)

        // animations getting in the way on API < 16
        if (Build.VERSION.SDK_INT < 16) {
            val recyclerView: RecyclerView = viewPager.getChildAt(0) as RecyclerView
            recyclerView.overScrollMode = OVER_SCROLL_NEVER
        }

        return Context(activityTestRule, mPageSwiper).apply {
            assertBasicState(0) // sanity check
        }
    }

    data class Context(
        val activityTestRule: ActivityTestRule<out BaseActivity>,
        val swiper: PageSwiper
    ) {
        fun recreateActivity() {
            activity = FragmentActivityUtils.recreateActivity(activityTestRule, activity)
        }

        var activity: BaseActivity = activityTestRule.activity
            private set(value) {
                field = value
            }

        fun runOnUiThread(f: () -> Unit) = activity.runOnUiThread(f)

        val viewPager: ViewPager2 get() = activity.findViewById(R.id.view_pager)
    }

    fun peekForward(@ViewPager2.Orientation orientation: Int) {
        peek(orientation, -50f)
    }

    fun peekBackward(@ViewPager2.Orientation orientation: Int) {
        peek(orientation, 50f)
    }

    private fun peek(@ViewPager2.Orientation orientation: Int, offset: Float) {
        onView(allOf(isDisplayed(), withId(R.id.text_view))).perform(actionWithAssertions(
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER,
                        CoordinatesProvider { view ->
                            val coordinates = GeneralLocation.CENTER.calculateCoordinates(view)
                            if (orientation == HORIZONTAL) {
                                coordinates[0] += offset
                            } else {
                                coordinates[1] += offset
                            }
                            coordinates
                        }, Press.FINGER)))
    }

    /**
     * Note: returned latch relies on the tested API, so it's critical to check that the final
     * visible page is correct using [assertBasicState].
     */
    fun ViewPager2.addWaitForScrolledLatch(
        targetPage: Int,
        waitForIdle: Boolean = true
    ): CountDownLatch {
        val latch = CountDownLatch(if (waitForIdle) 2 else 1)
        var lastScrollFired = false

        addOnPageChangeListener(object : ViewPager2.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (lastScrollFired && state == IDLE) {
                    latch.countDown()
                }
            }

            override fun onPageSelected(position: Int) {
                // nothing
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (position == targetPage && positionOffsetPixels == 0) {
                    latch.countDown()
                    lastScrollFired = true
                }
            }
        })

        return latch
    }

    val ViewPager2.pageSize: Int
        get() {
            return if (orientation == HORIZONTAL) {
                measuredWidth - paddingLeft - paddingRight
            } else {
                measuredHeight - paddingTop - paddingBottom
            }
        }

    /**
     * Checks:
     * 1. Expected page is the current ViewPager2 page
     * 2. Expected text is displayed
     * 3. Internal activity state is valid (as per activity self-test)
     */
    fun Context.assertBasicState(pageIx: Int, value: Int = pageIx) {
        assertThat<Int>(viewPager.currentItem, equalTo(pageIx))
        onView(allOf<View>(withId(R.id.text_view), isDisplayed())).check(
                matches(withText(value.toString())))
        activity.validateState()
    }

    enum class SortOrder(val sign: Int) {
        ASC(1),
        DESC(-1)
    }

    fun <T, R : Comparable<R>> List<T>.assertSorted(selector: (T) -> R) {
        assertThat(this, equalTo(this.sortedBy(selector)))
    }

    /**
     * Is between [min, max)
     * @param min - inclusive
     * @param max - exclusive
     */
    fun <T : Comparable<T>> isBetweenInEx(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThan<T>(max))
    }

    /**
     * Is between [min, max]
     * @param min - inclusive
     * @param max - inclusive
     */
    fun <T : Comparable<T>> isBetweenInIn(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThanOrEqualTo<T>(max))
    }
}
