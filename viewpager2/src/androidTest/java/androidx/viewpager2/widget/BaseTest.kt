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

import android.content.Intent
import android.os.Build
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.testutils.FragmentActivityUtils
import androidx.viewpager2.LocaleTestUtils
import androidx.viewpager2.test.R
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.swipe.FragmentAdapter
import androidx.viewpager2.widget.swipe.PageSwiper
import androidx.viewpager2.widget.swipe.TestActivity
import androidx.viewpager2.widget.swipe.ViewAdapter
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

open class BaseTest {
    lateinit var localeUtil: LocaleTestUtils

    @Before
    open fun setUp() {
        localeUtil = LocaleTestUtils(InstrumentationRegistry.getTargetContext())
        // Ensure a predictable test environment by explicitly setting a locale
        localeUtil.setLocale(LocaleTestUtils.DEFAULT_TEST_LANGUAGE)
    }

    @After
    open fun tearDown() {
        localeUtil.resetLocale()
    }

    fun setUpTest(@ViewPager2.Orientation orientation: Int): Context {
        val activityTestRule = object : ActivityTestRule<TestActivity>(TestActivity::class.java) {
            override fun getActivityIntent(): Intent {
                val intent = Intent()
                if (localeUtil.isLocaleChangedAndLock()) {
                    intent.putExtra(TestActivity.EXTRA_LANGUAGE, localeUtil.getLocale().toString())
                }
                return intent
            }
        }
        activityTestRule.launchActivity(null)

        val viewPager: ViewPager2 = activityTestRule.activity.findViewById(R.id.view_pager)
        activityTestRule.runOnUiThread { viewPager.orientation = orientation }
        onView(withId(R.id.view_pager)).check(matches(isDisplayed()))

        // animations getting in the way on API < 16
        if (Build.VERSION.SDK_INT < 16) {
            val recyclerView: RecyclerView = viewPager.getChildAt(0) as RecyclerView
            recyclerView.overScrollMode = OVER_SCROLL_NEVER
        }

        return Context(activityTestRule)
    }

    data class Context(val activityTestRule: ActivityTestRule<TestActivity>) {
        fun recreateActivity(adapterProvider: AdapterProvider) {
            TestActivity.adapterProvider = adapterProvider
            activity = FragmentActivityUtils.recreateActivity(activityTestRule, activity)
            TestActivity.adapterProvider = null
        }

        var activity: TestActivity = activityTestRule.activity
            private set(value) {
                field = value
            }

        fun runOnUiThread(f: () -> Unit) = activity.runOnUiThread(f)

        val viewPager: ViewPager2 get() = activity.findViewById(R.id.view_pager)

        val swiper: PageSwiper
            get() = PageSwiper(viewPager.adapter.itemCount, viewPager.orientation, isRtl)

        val isRtl get() = ViewCompat.getLayoutDirection(viewPager) ==
                ViewCompat.LAYOUT_DIRECTION_RTL

        fun peekForward(@ViewPager2.Orientation orientation: Int) {
            peek(orientation, adjustForRtl(orientation, -50f))
        }

        fun peekBackward(@ViewPager2.Orientation orientation: Int) {
            peek(orientation, adjustForRtl(orientation, 50f))
        }

        private fun adjustForRtl(@ViewPager2.Orientation orientation: Int, offset: Float): Float {
            return if (orientation == ORIENTATION_HORIZONTAL && isRtl) -offset else offset
        }

        private fun peek(@ViewPager2.Orientation orientation: Int, offset: Float) {
            onView(allOf(isDisplayed(), isAssignableFrom(ViewPager2::class.java)))
                .perform(actionWithAssertions(
                    GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER,
                        CoordinatesProvider { view ->
                            val coordinates = GeneralLocation.CENTER.calculateCoordinates(view)
                            if (orientation == ORIENTATION_HORIZONTAL) {
                                coordinates[0] += offset
                            } else {
                                coordinates[1] += offset
                            }
                            coordinates
                        }, Press.FINGER)))
        }
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
                if (lastScrollFired && state == SCROLL_STATE_IDLE) {
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

    fun Context.setAdapterSync(adapterProvider: AdapterProvider) {
        val waitForRenderLatch = CountDownLatch(1)

        viewPager.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            waitForRenderLatch.countDown()
        }

        runOnUiThread {
            viewPager.adapter = adapterProvider(activity)
        }

        waitForRenderLatch.await(5, TimeUnit.SECONDS)
    }

    fun ViewPager2.addWaitForIdleLatch(): CountDownLatch {
        val latch = CountDownLatch(1)

        addOnPageChangeListener(object : ViewPager2.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == SCROLL_STATE_IDLE) {
                    latch.countDown()
                    post { removeOnPageChangeListener(this) }
                }
            }
        })

        return latch
    }

    fun ViewPager2.addWaitForDistanceToTarget(target: Int, distance: Float): CountDownLatch {
        val latch = CountDownLatch(1)

        addOnPageChangeListener(object : ViewPager2.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (abs(target - position - positionOffset) <= distance) {
                    latch.countDown()
                    post { removeOnPageChangeListener(this) }
                }
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        return latch
    }

    val ViewPager2.pageSize: Int
        get() {
            return if (orientation == ORIENTATION_HORIZONTAL) {
                measuredWidth - paddingLeft - paddingRight
            } else {
                measuredHeight - paddingTop - paddingBottom
            }
        }

    val ViewPager2.currentCompletelyVisibleItem: Int
        get() {
            return ((getChildAt(0) as RecyclerView)
                .layoutManager as LinearLayoutManager)
                .findFirstCompletelyVisibleItemPosition()
        }

    /**
     * Checks:
     * 1. Expected page is the current ViewPager2 page
     * 2. Expected text is displayed
     * 3. Internal activity state is valid (as per activity self-test)
     */
    fun Context.assertBasicState(pageIx: Int, value: String) {
        assertThat<Int>(viewPager.currentItem, equalTo(pageIx))
        onView(allOf<View>(withId(R.id.text_view), isDisplayed())).check(
                matches(withText(value)))

        // FIXME: too tight coupling
        if (viewPager.adapter is FragmentAdapter) {
            val adapter = viewPager.adapter as FragmentAdapter
            assertThat(adapter.attachCount.get() - adapter.destroyCount.get(), isBetweenInIn(1, 4))
        }
    }

    fun ViewPager2.setCurrentItemSync(
        targetPage: Int,
        smoothScroll: Boolean,
        timeout: Long,
        unit: TimeUnit
    ) {
        val latch = addWaitForScrolledLatch(targetPage, false)

        // temporary hack to stop the tests from failing
        // this most likely shows a bug in PageChangeListener - communicating IDLE before
        // RecyclerView is ready; TODO: investigate further and fix
        val latchRV = CountDownLatch(1)
        val rv = getChildAt(0) as RecyclerView
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == 0) {
                    latchRV.countDown()
                }
            }
        })
        post { setCurrentItem(targetPage, smoothScroll) }
        latch.await(timeout, unit)
        latchRV.await(timeout, unit)
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

    /**
     * Is between [min(a, b), max(a, b)]
     * @param a - inclusive
     * @param b - inclusive
     */
    fun <T : Comparable<T>> isBetweenInInMinMax(a: T, b: T): Matcher<T> {
        return isBetweenInIn(minOf(a, b), maxOf(a, b))
    }
}

typealias AdapterProvider = (TestActivity) -> RecyclerView.Adapter<out RecyclerView.ViewHolder>

typealias AdapterProviderForItems = (items: List<String>) -> AdapterProvider

val fragmentAdapterProvider: AdapterProviderForItems = { items ->
    { activity: TestActivity -> FragmentAdapter(activity.supportFragmentManager, items) }
}

val viewAdapterProvider: AdapterProviderForItems = { items -> { ViewAdapter(items) } }

fun stringSequence(pageCount: Int) = (1..pageCount).mapIndexed { ix, _ -> ix.toString() }

val AdapterProviderForItems.supportsMutations: Boolean
    get() {
        return this == fragmentAdapterProvider
    }
