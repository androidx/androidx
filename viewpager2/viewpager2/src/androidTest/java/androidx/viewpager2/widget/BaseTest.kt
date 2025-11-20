/*
 * Copyright 2018 The Android Open Source Project
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
import android.util.Log
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.testutils.LocaleTestUtils
import androidx.testutils.recreate
import androidx.testutils.setSystemExclusionRectsForEspressoSwipes
import androidx.testutils.waitForExecution
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.test.R
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import androidx.viewpager2.widget.swipe.FragmentAdapter
import androidx.viewpager2.widget.swipe.PageSwiper
import androidx.viewpager2.widget.swipe.PageSwiperEspresso
import androidx.viewpager2.widget.swipe.PageSwiperFakeDrag
import androidx.viewpager2.widget.swipe.PageSwiperManual
import androidx.viewpager2.widget.swipe.SelfChecking
import androidx.viewpager2.widget.swipe.TestActivity
import androidx.viewpager2.widget.swipe.ViewAdapter
import androidx.viewpager2.widget.swipe.WaitForInjectMotionEventsAction.Companion.waitForInjectMotionEvents
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class BaseTest {
    companion object {
        const val TAG = "VP2_TESTS"
        const val ACTION_ID_PAGE_LEFT = android.R.id.accessibilityActionPageLeft
        const val ACTION_ID_PAGE_RIGHT = android.R.id.accessibilityActionPageRight
        const val ACTION_ID_PAGE_UP = android.R.id.accessibilityActionPageUp
        const val ACTION_ID_PAGE_DOWN = android.R.id.accessibilityActionPageDown
    }

    lateinit var localeUtil: LocaleTestUtils

    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule =
        androidx.test.rule.ActivityTestRule<TestActivity>(TestActivity::class.java, false, false)

    @Before
    open fun setUp() {
        localeUtil =
            LocaleTestUtils(ApplicationProvider.getApplicationContext() as android.content.Context)
        // Ensure a predictable test environment by explicitly setting a locale
        localeUtil.setLocale(LocaleTestUtils.DEFAULT_TEST_LANGUAGE)
    }

    @After
    open fun tearDown() {
        localeUtil.resetLocale()
    }

    fun setUpTest(@ViewPager2.Orientation orientation: Int): Context {
        val intent = Intent()
        if (localeUtil.isLocaleChangedAndLock()) {
            intent.putExtra(TestActivity.EXTRA_LANGUAGE, localeUtil.getLocale().toString())
        }
        activityTestRule.launchActivity(intent)
        onView(withId(R.id.view_pager)).perform(waitForInjectMotionEvents())

        val viewPager: ViewPager2 = activityTestRule.activity.findViewById(R.id.view_pager)
        activityTestRule.runOnUiThread {
            viewPager.orientation = orientation
            viewPager.setSystemExclusionRectsForEspressoSwipes(requestLayout = true)
        }
        onView(withId(R.id.view_pager)).check(matches(isDisplayed()))

        return Context(activityTestRule)
    }

    data class Context(
        @Suppress("DEPRECATION")
        val activityTestRule: androidx.test.rule.ActivityTestRule<TestActivity>
    ) {
        fun recreateActivity(
            adapterProvider: AdapterProvider,
            onCreateCallback: ((ViewPager2) -> Unit) = {},
        ) {
            val orientation = viewPager.orientation
            val isUserInputEnabled = viewPager.isUserInputEnabled
            TestActivity.onCreateCallback = { activity ->
                val viewPager = activity.findViewById<ViewPager2>(R.id.view_pager)
                viewPager.orientation = orientation
                viewPager.isUserInputEnabled = isUserInputEnabled
                viewPager.adapter = adapterProvider(activity)
                viewPager.setSystemExclusionRectsForEspressoSwipes(requestLayout = false)
                onCreateCallback(viewPager)
            }
            activity = activityTestRule.recreate()
            TestActivity.onCreateCallback = {}
            onView(withId(R.id.view_pager)).perform(waitForInjectMotionEvents())
        }

        var activity: TestActivity = activityTestRule.activity
            private set(value) {
                field = value
            }

        fun <T> runOnUiThreadSync(f: () -> T): T {
            var thrownError: Throwable? = null
            var result: T? = null
            activityTestRule.runOnUiThread {
                try {
                    result = f()
                } catch (t: Throwable) {
                    thrownError = t
                }
            }
            thrownError?.let { throw it }
            return result!!
        }

        val viewPager: ViewPager2
            get() = activity.findViewById(R.id.view_pager)

        fun peekForward() {
            peek(adjustForRtl(adjustForTouchSlop(-50f)))
        }

        fun peekBackward() {
            peek(adjustForRtl(adjustForTouchSlop(50f)))
        }

        enum class SwipeMethod {
            ESPRESSO,
            MANUAL,
            FAKE_DRAG,
        }

        fun swipe(currentPageIx: Int, nextPageIx: Int, method: SwipeMethod = SwipeMethod.ESPRESSO) {
            val lastPageIx = viewPager.adapter!!.itemCount - 1

            if (nextPageIx > lastPageIx) {
                throw IllegalArgumentException("Invalid next page: beyond last page.")
            }

            if (currentPageIx == nextPageIx) { // dedicated for testing edge behaviour
                if (nextPageIx == 0) {
                    swipeBackward(method) // bounce off the "left" edge
                    return
                }
                if (nextPageIx == lastPageIx) { // bounce off the "right" edge
                    swipeForward(method)
                    return
                }
                throw IllegalArgumentException(
                    "Invalid sequence. Not on an edge, and current page = next page."
                )
            }

            if (Math.abs(nextPageIx - currentPageIx) > 1) {
                throw IllegalArgumentException(
                    "Specified next page not adjacent to the current page."
                )
            }

            if (nextPageIx > currentPageIx) {
                swipeForward(method)
            } else {
                swipeBackward(method)
            }
        }

        fun swipeForward(method: SwipeMethod = SwipeMethod.ESPRESSO) {
            swiper(method).swipeNext()
        }

        fun swipeBackward(method: SwipeMethod = SwipeMethod.ESPRESSO) {
            swiper(method).swipePrevious()
        }

        private fun swiper(method: SwipeMethod = SwipeMethod.ESPRESSO): PageSwiper {
            return when (method) {
                SwipeMethod.ESPRESSO -> PageSwiperEspresso(viewPager)
                SwipeMethod.MANUAL -> PageSwiperManual(viewPager)
                SwipeMethod.FAKE_DRAG -> PageSwiperFakeDrag(viewPager) { viewPager.pageSize }
            }
        }

        private fun adjustForTouchSlop(offset: Float): Float {
            val touchSlop = ViewConfiguration.get(viewPager.context).scaledPagingTouchSlop
            return when {
                offset < 0 -> offset - touchSlop
                offset > 0 -> offset + touchSlop
                else -> 0f
            }
        }

        private fun adjustForRtl(offset: Float): Float {
            return if (viewPager.isHorizontal && viewPager.isRtl) -offset else offset
        }

        private fun peek(offset: Float) {
            onView(allOf(isDisplayed(), isAssignableFrom(ViewPager2::class.java)))
                .perform(
                    actionWithAssertions(
                        GeneralSwipeAction(
                            Swipe.SLOW,
                            GeneralLocation.CENTER,
                            CoordinatesProvider { view ->
                                val coordinates = GeneralLocation.CENTER.calculateCoordinates(view)
                                if (viewPager.orientation == ORIENTATION_HORIZONTAL) {
                                    coordinates[0] += offset
                                } else {
                                    coordinates[1] += offset
                                }
                                coordinates
                            },
                            Press.FINGER,
                        )
                    )
                )
        }

        fun assertPageActions() {
            if (!ViewPager2.sFeatureEnhancedA11yEnabled) {
                return // these assertions only apply to enhanced a11y
            }

            var customActions = getActionList(viewPager)
            var currentPage = viewPager.currentItem
            var numPages = viewPager.adapter!!.itemCount
            var isUserInputEnabled = viewPager.isUserInputEnabled
            var isHorizontalOrientation = viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL
            var isVerticalOrientation = viewPager.orientation == ViewPager2.ORIENTATION_VERTICAL

            val expectPageLeftAction =
                isUserInputEnabled &&
                    isHorizontalOrientation &&
                    (if (viewPager.isRtl) currentPage < numPages - 1 else currentPage > 0)

            val expectPageRightAction =
                isUserInputEnabled &&
                    isHorizontalOrientation &&
                    (if (viewPager.isRtl) currentPage > 0 else currentPage < numPages - 1)

            val expectPageUpAction = isUserInputEnabled && isVerticalOrientation && currentPage > 0

            val expectPageDownAction =
                isUserInputEnabled && isVerticalOrientation && currentPage < numPages - 1

            val expectScrollBackwardAction = isUserInputEnabled && currentPage > 0

            val expectScrollForwardAction = isUserInputEnabled && currentPage < numPages - 1

            assertThat(
                "Left action expected: $expectPageLeftAction",
                hasPageAction(customActions, ACTION_ID_PAGE_LEFT),
                equalTo(expectPageLeftAction),
            )

            assertThat(
                "Right action expected: $expectPageRightAction",
                hasPageAction(customActions, ACTION_ID_PAGE_RIGHT),
                equalTo(expectPageRightAction),
            )
            assertThat(
                "Up action expected: $expectPageUpAction",
                hasPageAction(customActions, ACTION_ID_PAGE_UP),
                equalTo(expectPageUpAction),
            )
            assertThat(
                "Down action expected: $expectPageDownAction",
                hasPageAction(customActions, ACTION_ID_PAGE_DOWN),
                equalTo(expectPageDownAction),
            )

            @Suppress("DEPRECATION") var node = AccessibilityNodeInfo.obtain()
            runOnUiThreadSync { viewPager.onInitializeAccessibilityNodeInfo(node) }
            @Suppress("DEPRECATION") var standardActions = node.actions

            assertThat(
                "scroll backward action expected: $expectScrollBackwardAction",
                hasScrollAction(standardActions, ACTION_SCROLL_BACKWARD),
                equalTo(expectScrollBackwardAction),
            )

            assertThat(
                "Scroll forward action expected: $expectScrollForwardAction",
                hasScrollAction(standardActions, ACTION_SCROLL_FORWARD),
                equalTo(expectScrollForwardAction),
            )
        }

        private fun hasScrollAction(actions: Int, accessibilityActionId: Int): Boolean {
            return actions and accessibilityActionId != 0
        }

        private fun hasPageAction(
            actions: List<AccessibilityNodeInfoCompat.AccessibilityActionCompat>,
            accessibilityActionId: Int,
        ): Boolean {
            return actions.any { it.id == accessibilityActionId }
        }

        @Suppress("UNCHECKED_CAST")
        private fun getActionList(
            view: View
        ): List<AccessibilityNodeInfoCompat.AccessibilityActionCompat> {
            return view.getTag(androidx.core.R.id.tag_accessibility_actions)
                as? ArrayList<AccessibilityNodeInfoCompat.AccessibilityActionCompat> ?: ArrayList()
        }
    }

    /**
     * Note: returned latch relies on the tested API, so it's critical to check that the final
     * visible page is correct using [assertBasicState].
     */
    fun ViewPager2.addWaitForScrolledLatch(
        targetPage: Int,
        waitForIdle: Boolean = true,
    ): CountDownLatch {
        val latch = CountDownLatch(if (waitForIdle) 2 else 1)
        var lastScrollFired = false

        registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (lastScrollFired && state == SCROLL_STATE_IDLE) {
                        latch.countDown()
                    }
                }

                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                    if (position == targetPage && positionOffsetPixels == 0) {
                        latch.countDown()
                        lastScrollFired = true
                    }
                }
            }
        )

        return latch
    }

    fun Context.setAdapterSync(adapterProvider: AdapterProvider) {
        lateinit var waitForRenderLatch: CountDownLatch

        runOnUiThreadSync {
            waitForRenderLatch = viewPager.addWaitForLayoutChangeLatch()
            viewPager.adapter = adapterProvider(activity)
        }

        waitForRenderLatch.await(5, TimeUnit.SECONDS)
    }

    fun ViewPager2.addWaitForLayoutChangeLatch(): CountDownLatch {
        return CountDownLatch(1).also {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> it.countDown() }
        }
    }

    fun ViewPager2.addWaitForIdleLatch(): CountDownLatch {
        return addWaitForStateLatch(SCROLL_STATE_IDLE)
    }

    fun ViewPager2.addWaitForStateLatch(targetState: Int): CountDownLatch {
        val latch = CountDownLatch(1)

        registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state == targetState) {
                        latch.countDown()
                        post { unregisterOnPageChangeCallback(this) }
                    }
                }
            }
        )

        return latch
    }

    fun ViewPager2.addWaitForDistanceToTarget(target: Int, distance: Float): CountDownLatch {
        val latch = CountDownLatch(1)

        registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int,
                ) {
                    if (abs(target - position - positionOffset) <= distance) {
                        latch.countDown()
                        post { unregisterOnPageChangeCallback(this) }
                    }
                }
            }
        )

        return latch
    }

    fun ViewPager2.addWaitForFirstScrollEventLatch(): CountDownLatch {
        val latch = CountDownLatch(1)
        registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {
                    latch.countDown()
                    post { unregisterOnPageChangeCallback(this) }
                }
            }
        )
        return latch
    }

    val ViewPager2.linearLayoutManager: LinearLayoutManager
        get() = recyclerView.layoutManager as LinearLayoutManager

    val ViewPager2.recyclerView: RecyclerView
        get() {
            return getChildAt(0) as RecyclerView
        }

    val ViewPager2.currentCompletelyVisibleItem: Int
        get() {
            var position = RecyclerView.NO_POSITION
            activityTestRule.runOnUiThread {
                position = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
            }
            return position
        }

    /**
     * Checks:
     * 1. Expected page is the current ViewPager2 page
     * 2. Expected state is SCROLL_STATE_IDLE
     * 3. Expected text is displayed
     * 4. Internal activity state is valid (as per activity self-test)
     */
    fun Context.assertBasicState(
        pageIx: Int,
        value: String? = pageIx.toString(),
        performSelfCheck: Boolean = true,
    ) {
        assertThat<Int>(
            "viewPager.getCurrentItem() should return $pageIx",
            viewPager.currentItem,
            equalTo(pageIx),
        )
        assertThat("viewPager should be IDLE", viewPager.scrollState, equalTo(SCROLL_STATE_IDLE))
        if (value != null) {
            onView(allOf<View>(withId(R.id.text_view), isCompletelyDisplayed()))
                .check(matches(withText(value)))
        }

        // TODO(b/130153109): Wire offscreenPageLimit into FragmentAdapter, remove performSelfCheck
        if (performSelfCheck && viewPager.adapter is SelfChecking) {
            (viewPager.adapter as SelfChecking).selfCheck()
        }
        assertPageActions()
    }

    fun Context.resetViewPagerTo(page: Int) {
        viewPager.setCurrentItemSync(page, false, 2, TimeUnit.SECONDS)
        // VP2 was potentially settling while the RetryException was raised,
        // in which case we must wait until the IDLE event has been fired
        activityTestRule.waitForExecution(1)
    }

    fun Context.modifyDataSetSync(block: () -> Unit) {
        val layoutChangedLatch = viewPager.addWaitForLayoutChangeLatch()
        runOnUiThreadSync { block() }
        layoutChangedLatch.await(1, TimeUnit.SECONDS)

        // Let animations run
        val animationLatch = CountDownLatch(1)
        viewPager.recyclerView.itemAnimator!!.isRunning { animationLatch.countDown() }
        animationLatch.await(1, TimeUnit.SECONDS)
    }

    fun ViewPager2.setCurrentItemSync(
        targetPage: Int,
        smoothScroll: Boolean,
        timeout: Long,
        unit: TimeUnit,
        expectEvents: Boolean = (targetPage != currentItem),
    ) {
        val latch =
            if (expectEvents) addWaitForScrolledLatch(targetPage, smoothScroll)
            else CountDownLatch(1)
        post {
            setCurrentItem(targetPage, smoothScroll)
            if (!expectEvents) {
                latch.countDown()
            }
        }
        latch.await(timeout, unit)
    }

    enum class SortOrder(val sign: Int) {
        ASC(1),
        DESC(-1),
    }

    fun <T, R : Comparable<R>> List<T>.assertSorted(selector: (T) -> R) {
        assertThat(this, equalTo(this.sortedBy(selector)))
    }

    /**
     * Returns the slice between the first and second element. First and second element are not
     * included in the results. Search for the second element starts on the element after the first
     * element. If first element is not found, an empty list is returned. If second element is not
     * found, all elements after the first are returned.
     *
     * @return A list with all elements between the first and the second element
     */
    fun <T> List<T>.slice(first: T, second: T): List<T> {
        return dropWhile { it != first }.drop(1).takeWhile { it != second }
    }

    /**
     * Is between [min, max)
     *
     * @param min - inclusive
     * @param max - exclusive
     */
    fun <T : Comparable<T>> isBetweenInEx(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThan<T>(max))
    }

    /**
     * Is between [min, max]
     *
     * @param min - inclusive
     * @param max - inclusive
     */
    fun <T : Comparable<T>> isBetweenInIn(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThanOrEqualTo<T>(max))
    }

    /**
     * Is between [min(a, b), max(a, b)]
     *
     * @param a - inclusive
     * @param b - inclusive
     */
    fun <T : Comparable<T>> isBetweenInInMinMax(a: T, b: T): Matcher<T> {
        return isBetweenInIn(minOf(a, b), maxOf(a, b))
    }
}

typealias AdapterProvider = (TestActivity) -> RecyclerView.Adapter<out RecyclerView.ViewHolder>

data class AdapterProviderForItems(
    val name: String,
    val provider: (items: List<String>) -> AdapterProvider,
) {
    override fun toString(): String = name
}

val fragmentAdapterProvider =
    AdapterProviderForItems("fragmentAdapterProvider") { items ->
        { activity: TestActivity ->
            FragmentAdapter(activity.supportFragmentManager, activity.lifecycle, items)
        }
    }

/**
 * Same as [fragmentAdapterProvider] but with a custom implementation of
 * [FragmentStateAdapter.getItemId] and [FragmentStateAdapter.containsItem]. Not suitable for
 * testing [RecyclerView.Adapter.notifyDataSetChanged].
 */
val fragmentAdapterProviderCustomIds =
    AdapterProviderForItems("fragmentAdapterProviderCustomIds") { items ->
        { activity ->
            fragmentAdapterProvider.provider(items)(activity).also {
                // more than position can represent, so a good test if ids are used consistently
                val offset = 3L * Int.MAX_VALUE
                val adapter = it as FragmentAdapter
                adapter.positionToItemId = { position -> position + offset }
                adapter.itemIdToContains = { itemId ->
                    val position = itemId - offset
                    position in (0 until adapter.itemCount)
                }
            }
        }
    }

/**
 * Same as [fragmentAdapterProvider] but with a custom implementation of
 * [FragmentStateAdapter.getItemId] and [FragmentStateAdapter.containsItem]. Suitable for testing
 * [RecyclerView.Adapter.notifyDataSetChanged].
 */
val fragmentAdapterProviderValueId =
    AdapterProviderForItems("fragmentAdapterProviderValueId") { items ->
        { activity ->
            fragmentAdapterProvider.provider(items)(activity).also {
                val adapter = it as FragmentAdapter
                adapter.positionToItemId = { position -> items[position].getId() }
                adapter.itemIdToContains = { itemId ->
                    items.any { item -> item.getId() == itemId }
                }
            }
        }
    }

/** Extracts the sole number from a [String] and converts it to a [Long] */
private fun (String).getId(): Long {
    val matches = Regex("[0-9]+").findAll(this).toList()
    if (matches.size != 1) {
        throw IllegalStateException("There should be exactly one number in the input string")
    }
    return matches.first().value.toLong()
}

/**
 * Same as [viewAdapterProvider] but with a custom implementation of
 * [RecyclerView.Adapter.getItemId]. Suitable for testing
 * [RecyclerView.Adapter.notifyDataSetChanged].mu
 */
val viewAdapterProviderValueId =
    AdapterProviderForItems("viewAdapterProviderValueId") { items ->
        { activity ->
            viewAdapterProvider.provider(items)(activity).also {
                val adapter = it as ViewAdapter
                adapter.positionToItemId = { position -> items[position].getId() }
                adapter.setHasStableIds(true)
            }
        }
    }

val viewAdapterProvider =
    AdapterProviderForItems("viewAdapterProvider") { items -> { ViewAdapter(items) } }

fun stringSequence(pageCount: Int) = (0 until pageCount).map { it.toString() }

val AdapterProviderForItems.supportsMutations: Boolean
    get() {
        return this == fragmentAdapterProvider
    }

fun scrollStateToString(@ViewPager2.ScrollState state: Int): String {
    return when (state) {
        SCROLL_STATE_IDLE -> "IDLE"
        SCROLL_STATE_DRAGGING -> "DRAGGING"
        SCROLL_STATE_SETTLING -> "SETTLING"
        else -> throw IllegalArgumentException("Scroll state $state doesn't exist")
    }
}

fun scrollStateGlossary(): String {
    return "Scroll states: " +
        "$SCROLL_STATE_IDLE=${scrollStateToString(SCROLL_STATE_IDLE)}, " +
        "$SCROLL_STATE_DRAGGING=${scrollStateToString(SCROLL_STATE_DRAGGING)}, " +
        "$SCROLL_STATE_SETTLING=${scrollStateToString(SCROLL_STATE_SETTLING)})"
}

class RetryException(msg: String) : Exception(msg)

fun tryNTimes(n: Int, resetBlock: () -> Unit, tryBlock: () -> Unit) {
    repeat(n) { i ->
        try {
            tryBlock()
            return
        } catch (e: RetryException) {
            if (i < n - 1) {
                Log.w(BaseTest.TAG, "Bad state, retrying block", e)
            } else {
                throw AssertionError("Block hit bad state $n times", e)
            }
            resetBlock()
        }
    }
}

val View.isRtl: Boolean
    get() = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL

val ViewPager2.isHorizontal: Boolean
    get() = orientation == ORIENTATION_HORIZONTAL
