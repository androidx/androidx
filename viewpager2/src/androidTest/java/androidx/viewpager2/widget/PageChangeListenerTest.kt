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

import android.view.View
import android.view.ViewGroup
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
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.test.R
import androidx.viewpager2.widget.ViewPager2.Orientation
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.ViewPager2.ScrollState.DRAGGING
import androidx.viewpager2.widget.ViewPager2.ScrollState.IDLE
import androidx.viewpager2.widget.ViewPager2.ScrollState.SETTLING
import androidx.viewpager2.widget.setup.TestSetup
import androidx.viewpager2.widget.swipe.ViewAdapterActivity
import androidx.viewpager2.widget.swipe.PageSwiper
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@SmallTest
@RunWith(AndroidJUnit4::class)
class PageChangeListenerTest {
    /*
    Sample log to guide the test

    1 -> 2
    onPageScrollStateChanged,1
    onPageScrolled,1,0.019444,21
    onPageScrolled,1,0.082407,88
    onPageScrolled,1,0.173148,187
    onPageScrollStateChanged,2
    onPageSelected,2
    onPageScrolled,1,0.343518,370
    onPageScrolled,1,0.855556,924
    onPageScrolled,1,0.984259,1063
    onPageScrolled,2,0.000000,0
    onPageScrollStateChanged,0

    2 -> 1
    onPageScrollStateChanged,1
    onPageScrolled,1,0.972222,1050
    onPageScrolled,1,0.910185,983
    onPageScrolled,1,0.835185,902
    onPageScrolled,1,0.764815,826
    onPageScrollStateChanged,2
    onPageSelected,1
    onPageScrolled,1,0.616667,666
    onPageScrolled,1,0.136111,147
    onPageScrolled,1,0.015741,17
    onPageScrolled,1,0.000000,0
    onPageScrollStateChanged,0
     */
    private fun test_swipeBetweenPages(@Orientation orientation: Int) {
        val (_, viewPager, swiper) = setUpTest(4, orientation)

        listOf(1, 2, 3, 2, 1, 0).forEach { targetPage ->
            // given
            val initialPage = viewPager.currentItem
            assertThat(Math.abs(initialPage - targetPage), equalTo(1))

            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(targetPage)

            // when
            swiper.swipe(initialPage, targetPage)
            latch.await(1, SECONDS)

            // then
            assertCurrentPageCorrect(viewPager, targetPage)

            listener.apply {
                // verify all events
                assertThat(draggingIx, equalTo(0))
                assertThat(settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
                assertThat(idleIx, equalTo(lastIx))
                assertThat(pageSelectedIx(targetPage), equalTo(settlingIx + 1))
                assertThat(scrollEventCount, equalTo(eventCount - 4))

                // dive into scroll events
                val sortOrder = if (targetPage - initialPage > 0) SortOrder.ASC else SortOrder.DESC
                scrollEvents.assertPositionSorted(sortOrder)
                scrollEvents.assertOffsetSorted(sortOrder)
                scrollEvents.assertValueSanity(initialPage, targetPage, viewPager.pageSize)
                scrollEvents.assertLastCorrect(targetPage)
            }
        }
    }

    @Test
    fun test_swipeBetweenPages_horizontal() {
        test_swipeBetweenPages(HORIZONTAL)
    }

    @Test
    fun test_swipeBetweenPages_vertical() {
        test_swipeBetweenPages(VERTICAL)
    }

    /*
    Before page 0
    onPageScrollStateChanged,1
    onPageScrolled,0,0.000000,0
    onPageScrolled,0,0.000000,0
    onPageScrolled,0,0.000000,0
    onPageScrolled,0,0.000000,0
    onPageScrollStateChanged,0

    After page 2
    onPageScrollStateChanged,1
    onPageScrolled,2,0.000000,0
    onPageScrolled,2,0.000000,0
    onPageScrolled,2,0.000000,0
    onPageScrollStateChanged,0
     */
    private fun test_swipeBeyondEdgePages(@Orientation orientation: Int) {
        val totalPages = 3
        val edgePages = setOf(0, totalPages - 1)
        val (_, viewPager, swiper) = setUpTest(totalPages, orientation)

        listOf(0, 0, 1, 2, 2, 2, 1, 2, 2, 2, 1, 0, 0, 0).forEach { targetPage ->
            // given
            val initialPage = viewPager.currentItem
            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(targetPage)

            // when
            swiper.swipe(initialPage, targetPage)
            latch.await(1, SECONDS)

            // then
            assertCurrentPageCorrect(viewPager, targetPage)

            if (targetPage == initialPage && edgePages.contains(targetPage)) {
                listener.apply {
                    // verify all events
                    assertThat(draggingIx, equalTo(0))
                    assertThat(idleIx, equalTo(lastIx))
                    assertThat(scrollEventCount, equalTo(eventCount - 2))

                    // dive into scroll events
                    scrollEvents.forEach {
                        assertThat(it.position, equalTo(targetPage))
                        assertThat(it.positionOffset, equalTo(0f))
                        assertThat(it.positionOffsetPixels, equalTo(0))
                    }
                }
            }
        }
    }

    @Test
    fun test_swipeBeyondEdgePages_horizontal() {
        test_swipeBeyondEdgePages(HORIZONTAL)
    }

    @Test
    fun test_swipeBeyondEdgePages_vertical() {
        test_swipeBeyondEdgePages(VERTICAL)
    }

    /*
    0 -> 1 (peek) -> 0
    onPageScrollStateChanged,1
    onPageScrolled,0,0.001852,2
    onPageScrolled,0,0.018519,20
    onPageScrolled,0,0.032407,35
    onPageScrolled,0,0.043519,47
    onPageScrollStateChanged,2
    onPageScrolled,0,0.045370,49
    onPageScrolled,0,0.029630,32
    onPageScrolled,0,0.017593,19
    onPageScrolled,0,0.010185,11
    onPageScrolled,0,0.005556,6
    onPageScrolled,0,0.002778,3
    onPageScrolled,0,0.000000,0
    onPageScrollStateChanged,0
     */
    private fun test_peekOnAdjacentPage_next(@Orientation orientation: Int) {
        // given
        val (_, viewPager, _) = setUpTest(3, orientation)
        val listener = viewPager.addNewRecordingListener()
        val latch = viewPager.addWaitForScrolledLatch(0)

        // when
        peekForward(orientation)
        latch.await(1, SECONDS)

        // then
        listener.apply {
            // verify all events
            assertThat(draggingIx, equalTo(0))
            assertThat(settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
            assertThat(idleIx, equalTo(lastIx))
            assertThat(scrollEventCount, equalTo(eventCount - 3))

            // dive into scroll events
            scrollEvents.assertPositionSorted(SortOrder.DESC)
            scrollEventsBeforeSettling.assertOffsetSorted(SortOrder.ASC)
            assertThat(scrollEvents, // sanity check
                    equalTo(scrollEventsBeforeSettling + scrollEventsAfterSettling))
            scrollEvents.assertValueSanity(0, 0, viewPager.pageSize)
            scrollEvents.assertLastCorrect(0)
        }
    }

    @Test
    fun test_peekOnAdjacentPage_horizontal_next() {
        test_peekOnAdjacentPage_next(HORIZONTAL)
    }

    @Test
    fun test_peekOnAdjacentPage_vertical_next() {
        test_peekOnAdjacentPage_next(VERTICAL)
    }

    /*
    1 -> 0 (peek) -> 1
    onPageScrollStateChanged,1
    onPageScrolled,0,0.997222,1077
    onPageScrolled,0,0.969444,1047
    onPageScrolled,0,0.953704,1030
    onPageScrolled,0,0.942593,1018
    onPageScrollStateChanged,2
    onPageScrolled,0,0.939815,1015
    onPageScrolled,0,0.975926,1054
    onPageScrolled,0,0.992593,1072
    onPageScrolled,0,0.999074,1079
    onPageScrolled,1,0.000000,0
    onPageScrollStateChanged,0
     */
    private fun test_peekOnAdjacentPage_previous(@Orientation orientation: Int) {
        // given
        val (activityTestRule, viewPager, _) = setUpTest(3, orientation)

        // get to page 1
        val latch0 = viewPager.addWaitForScrolledLatch(2, false)
        viewPager.setCurrentItemOnUIThread(2, false, activityTestRule)
        latch0.await(200, MILLISECONDS)

        // set up test listeners
        viewPager.clearOnPageChangeListeners()
        val listener = viewPager.addNewRecordingListener()
        val latch1 = viewPager.addWaitForScrolledLatch(2)

        // when
        peekBackward(orientation)
        latch1.await(1, SECONDS)

        // then
        listener.apply {
            // verify all events
            assertThat(draggingIx, equalTo(0))
            assertThat(settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
            assertThat(idleIx, equalTo(lastIx))
            assertThat(scrollEventCount, equalTo(eventCount - 3))

            // dive into scroll events
            scrollEvents.assertPositionSorted(SortOrder.ASC)
            scrollEventsBeforeSettling.assertOffsetSorted(SortOrder.DESC)
            scrollEventsAfterSettling.assertOffsetSorted(SortOrder.ASC)
            assertThat(scrollEvents, // sanity check
                    equalTo(scrollEventsBeforeSettling + scrollEventsAfterSettling))
            scrollEvents.assertValueSanity(1, 2, viewPager.pageSize)
            scrollEvents.dropLast(1).assertValueSanity(1, 1, viewPager.pageSize)
            scrollEvents.assertLastCorrect(2)
        }
    }

    @Test
    fun test_peekOnAdjacentPage_horizontal_previous() {
        test_peekOnAdjacentPage_previous(HORIZONTAL)
    }

    @Test
    fun test_peekOnAdjacentPage_vertical_previous() {
        test_peekOnAdjacentPage_previous(VERTICAL)
    }

    /*
    Sample log to guide the test

    0 -> 2
    onPageScrollStateChanged,2
    onPageSelected,2
    onPageScrolled,0,0.192593,208
    onPageScrolled,1,0.287963,310
    onPageScrolled,1,0.774074,836
    onPageScrolled,1,0.949074,1025
    onPageScrolled,1,0.994444,1074
    onPageScrolled,2,0.000000,0
    onPageScrollStateChanged,0

    2 -> 2
    // nothing

    2 -> 0
    onPageScrollStateChanged,2
    onPageSelected,0
    onPageScrolled,0,0.887037,958
    onPageScrolled,0,0.298148,322
    onPageScrolled,0,0.071296,77
    onPageScrolled,0,0.010185,11
    onPageScrolled,0,0.000000,0
    onPageScrollStateChanged,0
     */
    private fun test_selectItemProgrammatically_smoothScroll(@Orientation orientation: Int) {
        // given
        val (activityTestRule, viewPager, _) = setUpTest(5, orientation)

        // when
        listOf(4, 4, 2, 0, 0, 3).forEach { targetPage ->
            val currentPage = viewPager.currentItem
            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(targetPage)

            viewPager.setCurrentItemOnUIThread(targetPage, true, activityTestRule)
            latch.await(1, SECONDS)

            // then
            val pageIxDelta = targetPage - currentPage
            listener.apply {
                when (pageIxDelta) {
                    0 -> assertThat(eventCount, equalTo(0))
                    else -> {
                        // verify all events
                        assertThat(settlingIx, equalTo(0))
                        assertThat(pageSelectedIx(targetPage), equalTo(1))
                        assertThat(idleIx, equalTo(lastIx))
                        assertThat(scrollEventCount, equalTo(eventCount - 3))

                        // dive into scroll events
                        val sortOrder = if (pageIxDelta > 0) SortOrder.ASC else SortOrder.DESC
                        scrollEvents.assertPositionSorted(sortOrder)
                        scrollEvents.assertOffsetSorted(sortOrder)
                        scrollEvents.assertValueSanity(currentPage, targetPage, viewPager.pageSize)
                        scrollEvents.assertLastCorrect(targetPage)
                    }
                }
            }
        }
    }

    private fun List<OnPageScrolledEvent>.assertOffsetSorted(sortOrder: SortOrder) {
        groupBy { it.position }.forEach { (_, events) ->
            events.assertSorted { it.positionOffsetPixels * sortOrder.sign }
        }
    }

    private fun <T, R : Comparable<R>> List<T>.assertSorted(selector: (T) -> R) {
        assertThat(this, equalTo(this.sortedBy(selector)))
    }

    private fun List<OnPageScrolledEvent>.assertLastCorrect(targetPage: Int) {
        takeLast(1).forEach {
            assertThat(it.position, equalTo(targetPage))
            assertThat(it.positionOffsetPixels, equalTo(0))
        }
    }

    private fun List<OnPageScrolledEvent>.assertValueSanity(
        initialPage: Int,
        otherPage: Int,
        pageSize: Int
    ) = forEach {
        assertThat(it.position,
                isBetweenInIn(Math.min(initialPage, otherPage), Math.max(initialPage, otherPage)))
        assertThat(it.positionOffset, isBetweenInEx(0f, 1f))
        assertThat(it.positionOffsetPixels.toFloat() / pageSize,
                isBetweenInIn(it.positionOffset - 0.001f, it.positionOffset + 0.001f))
    }

    private fun List<OnPageScrolledEvent>.assertPositionSorted(sortOrder: SortOrder) {
        map { it.position }.apply {
            assertSorted { it * sortOrder.sign }
        }
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_horizontal() {
        test_selectItemProgrammatically_smoothScroll(HORIZONTAL)
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_vertical() {
        test_selectItemProgrammatically_smoothScroll(VERTICAL)
    }

    /*
    0 -> 0
    // nothing

    0 -> 2
    onPageSelected,2
    onPageScrolled,2,0.000000,0

    2 -> 2
    // nothing

    2 -> 0
    onPageSelected,0
    onPageScrolled,0,0.000000,0
     */
    private fun test_selectItemProgrammatically_noSmoothScroll(@Orientation orientation: Int) {
        // given
        val (activityTestRule, viewPager, _) = setUpTest(3, orientation)

        // when
        listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
            val currentPage = viewPager.currentItem
            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(targetPage, false)

            viewPager.setCurrentItemOnUIThread(targetPage, false, activityTestRule)
            latch.await(200, MILLISECONDS)

            // then
            val pageIxDelta = targetPage - currentPage
            listener.apply {
                when (pageIxDelta) {
                    0 -> assertThat(eventCount, equalTo(0))
                    else -> {
                        assertThat(eventCount, equalTo(2))
                        assertThat(pageSelectedIx(targetPage), equalTo(0))
                        assertThat(scrollEvents.last(), equalTo(
                                OnPageScrolledEvent(targetPage, 0f, 0)))
                    }
                }
            }
        }
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_horizontal() {
        test_selectItemProgrammatically_noSmoothScroll(HORIZONTAL)
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_vertical() {
        test_selectItemProgrammatically_noSmoothScroll(VERTICAL)
    }

    @Test
    fun test_scrollStateValuesInSync() {
        assertThat(ViewPager2.ScrollState.IDLE, allOf(equalTo(ViewPager.SCROLL_STATE_IDLE),
                equalTo(RecyclerView.SCROLL_STATE_IDLE)))
        assertThat(ViewPager2.ScrollState.DRAGGING, allOf(equalTo(ViewPager.SCROLL_STATE_DRAGGING),
                equalTo(RecyclerView.SCROLL_STATE_DRAGGING)))
        assertThat(ViewPager2.ScrollState.SETTLING, allOf(equalTo(ViewPager.SCROLL_STATE_SETTLING),
                equalTo(RecyclerView.SCROLL_STATE_SETTLING)))
    }

    @Test
    fun test_setCurrentItemBeforeRender() {
        // given
        val viewPager = ViewPager2(InstrumentationRegistry.getContext())
        val noOpAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(View(parent.context)) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 5
        }
        viewPager.setAdapter<RecyclerView.ViewHolder>(noOpAdapter)

        // when
        viewPager.addOnPageChangeListener(object : ViewPager2.OnPageChangeListener {
            override fun onPageScrolled(position: Int, offset: Float, offsetPx: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })
        viewPager.setCurrentItem(2, true)
        viewPager.setCurrentItem(3, false)

        // then
        // no crash
    }

    /**
     * Is between [min, max)
     * @param min - inclusive
     * @param max - exclusive
     */
    private fun <T : Comparable<T>> isBetweenInEx(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThan<T>(max))
    }

    /**
     * Is between [min, max]
     * @param min - inclusive
     * @param max - inclusive
     */
    private fun <T : Comparable<T>> isBetweenInIn(min: T, max: T): Matcher<T> {
        return allOf(greaterThanOrEqualTo<T>(min), lessThanOrEqualTo<T>(max))
    }

    private fun assertCurrentPageCorrect(viewPager: ViewPager2, pageIx: Int) {
        assertThat<Int>(viewPager.currentItem, equalTo(pageIx))
        onView(allOf<View>(withId(R.id.text_view), isDisplayed())).check(
                matches(withText(pageIx.toString())))
    }

    private val ViewPager2.pageSize: Int
        get() {
            return if (orientation == HORIZONTAL) {
                measuredWidth - paddingLeft - paddingRight
            } else {
                measuredHeight - paddingTop - paddingBottom
            }
        }

    /**
     * Note: returned latch relies on the tested API, so it's critical to check that the final
     * visible page is correct using [assertCurrentPageCorrect].
     */
    private fun ViewPager2.addWaitForScrolledLatch(
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

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val listener = RecordingListener()
        addOnPageChangeListener(listener)
        return listener
    }

    private fun ViewPager2.setCurrentItemOnUIThread(
        targetPage: Int,
        smoothScroll: Boolean,
        activityTestRule: ActivityTestRule<*>
    ) {
        activityTestRule.runOnUiThread { this.setCurrentItem(targetPage, smoothScroll) }
    }

    private enum class SortOrder(val sign: Int) {
        ASC(1),
        DESC(-1)
    }

    private fun peekForward(@Orientation orientation: Int) {
        peek(orientation, -50f)
    }

    private fun peekBackward(@Orientation orientation: Int) {
        peek(orientation, 50f)
    }

    private fun peek(@Orientation orientation: Int, offset: Float) {
        onView(allOf(isDisplayed(), withId(R.id.text_view))).perform(actionWithAssertions(
                GeneralSwipeAction(Swipe.SLOW, GeneralLocation.CENTER,
                        CoordinatesProvider { view ->
                            val coordinates = GeneralLocation.CENTER.calculateCoordinates(
                                    view)
                            if (orientation == HORIZONTAL) {
                                coordinates[0] += offset
                            } else {
                                coordinates[1] += offset
                            }
                            coordinates
                        }, Press.FINGER)))
    }

    private data class Context(
        val activityTestRule: ActivityTestRule<*>,
        val viewPager: ViewPager2,
        val swiper: PageSwiper
    )

    /**
     * Launches the view with relevant listeners wired in
     */
    private fun setUpTest(totalPages: Int, @Orientation orientation: Int): Context {
        val activityTestRule = ActivityTestRule(ViewAdapterActivity::class.java, true, false)
        activityTestRule.launchActivity(ViewAdapterActivity.createIntent(totalPages))

        val viewPager: ViewPager2 = activityTestRule.activity.findViewById(R.id.view_pager)
        activityTestRule.runOnUiThread { viewPager.orientation = orientation }
        onView(withId(R.id.view_pager)).check(matches(isDisplayed()))
        assertCurrentPageCorrect(viewPager, 0) // sanity check

        val mPageSwiper = PageSwiper(totalPages, viewPager.orientation)

        TestSetup(viewPager).applyWorkarounds()

        return Context(activityTestRule, viewPager, mPageSwiper)
    }

    private abstract class Event

    private data class OnPageScrolledEvent(
        val position: Int,
        val positionOffset: Float,
        val positionOffsetPixels: Int
    ) : Event()

    private data class OnPageSelectedEvent(val position: Int) : Event()

    private data class OnPageScrollStateChangedEvent(val state: Int) : Event()

    private class RecordingListener : ViewPager2.OnPageChangeListener {
        private val events = mutableListOf<Event>()

        val scrollEvents get() = events.filter { it is OnPageScrolledEvent }.map {
            it as OnPageScrolledEvent }
        val scrollEventsBeforeSettling
            get() = events.subList(0, settlingIx).filter { it is OnPageScrolledEvent }.map {
                it as OnPageScrolledEvent }
        val scrollEventsAfterSettling
            get() = events.subList(settlingIx + 1, events.size).filter {
                it is OnPageScrolledEvent }.map { it as OnPageScrolledEvent }
        val eventCount get() = events.size
        val scrollEventCount get() = scrollEvents.size
        val lastIx get() = events.size - 1
        val firstScrolledIx get() = events.indexOfFirst { it is OnPageScrolledEvent }
        val lastScrolledIx get() = events.indexOfLast { it is OnPageScrolledEvent }
        val settlingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SETTLING))
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(DRAGGING))
        val idleIx get() = events.indexOf(OnPageScrollStateChangedEvent(IDLE))
        val pageSelectedIx: (page: Int) -> Int = { events.indexOf(OnPageSelectedEvent(it)) }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
        }

        override fun onPageSelected(position: Int) {
            events.add(OnPageSelectedEvent(position))
        }

        override fun onPageScrollStateChanged(state: Int) {
            events.add(OnPageScrollStateChangedEvent(state))
        }
    }
}
