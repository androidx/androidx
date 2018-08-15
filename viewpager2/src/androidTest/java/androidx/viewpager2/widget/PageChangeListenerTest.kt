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

import android.os.SystemClock.sleep
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.ViewPager2.Orientation
import androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL
import androidx.viewpager2.widget.ViewPager2.ScrollState.DRAGGING
import androidx.viewpager2.widget.ViewPager2.ScrollState.IDLE
import androidx.viewpager2.widget.ViewPager2.ScrollState.SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@LargeTest
class PageChangeListenerTest : BaseTest() {

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
        setUpTest(4, orientation).apply {
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
                assertBasicState(targetPage)

                listener.apply {
                    // verify all events
                    assertThat(draggingIx, equalTo(0))
                    assertThat(settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
                    assertThat(idleIx, equalTo(lastIx))
                    assertThat(pageSelectedIx(targetPage), equalTo(settlingIx + 1))
                    assertThat(scrollEventCount, equalTo(eventCount - 4))

                    // dive into scroll events
                    val sortOrder =
                            if (targetPage - initialPage > 0) SortOrder.ASC
                            else SortOrder.DESC
                    scrollEvents.assertPositionSorted(sortOrder)
                    scrollEvents.assertOffsetSorted(sortOrder)
                    scrollEvents.assertValueSanity(initialPage, targetPage, viewPager.pageSize)
                    scrollEvents.assertLastCorrect(targetPage)
                    scrollEvents.assertMaxShownPages(initialPage)
                }
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

        setUpTest(totalPages, orientation).apply {

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
                assertBasicState(targetPage)

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
        setUpTest(3, orientation).apply {
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
        setUpTest(3, orientation).apply {
            // get to page 1
            val latch0 = viewPager.addWaitForScrolledLatch(2, false)
            runOnUiThread { viewPager.setCurrentItem(2, false) }
            latch0.await(200, MILLISECONDS)

            // set up test listeners
            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch1 = viewPager.addWaitForScrolledLatch(2)

            // when
            peekBackward(orientation)
            latch1.await(10, SECONDS)

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
        setUpTest(1000, orientation).apply {

            // when
            listOf(6, 5, 6, 4, 7, 3, 8, 2, 9, 1, 10, 0, 0, 999, 999, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()
                val latch = viewPager.addWaitForScrolledLatch(targetPage)

                // temporary hack to stop the tests from failing
                // this most likely shows a bug in PageChangeListener - communicating IDLE before
                // RecyclerView is ready; TODO: investigate further and fix
                val latchRV = CountDownLatch(1)
                val rv = viewPager.getChildAt(0) as RecyclerView
                rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == 0) {
                            latchRV.countDown()
                        }
                    }
                })
                runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
                latch.await(2, SECONDS)
                latchRV.await(2, SECONDS)

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
                            scrollEvents.assertValueSanity(currentPage, targetPage,
                                    viewPager.pageSize)
                            scrollEvents.assertLastCorrect(targetPage)
                            scrollEvents.assertMaxShownPages(currentPage)
                        }
                    }
                }
            }
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
        setUpTest(3, orientation).apply {

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()
                val latch = viewPager.addWaitForScrolledLatch(targetPage, false)

                runOnUiThread { viewPager.setCurrentItem(targetPage, false) }
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
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_horizontal() {
        test_selectItemProgrammatically_noSmoothScroll(HORIZONTAL)
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_vertical() {
        test_selectItemProgrammatically_noSmoothScroll(VERTICAL)
    }

    /**
     * Test behavior when no OnPageChangeListeners are attached.
     * Introduced after finding a regression.
     */
    private fun test_selectItemProgrammatically_noListener(
        @Orientation orientation: Int,
        smoothScroll: Boolean
    ) {
        // given
        setUpTest(3, orientation).apply {

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                runOnUiThread { viewPager.setCurrentItem(targetPage, smoothScroll) }

                // poll the viewpager on the ui thread
                val targetReached = AtomicBoolean(false)
                val checkFunctionExecuted = AtomicBoolean(false)

                // this function will be called on the ui thread
                fun checkTargetReached() {
                    targetReached.set(targetPage == viewPager.currentCompletelyVisibleItem)
                    checkFunctionExecuted.set(true)
                }

                // poll for up to 2 seconds
                val end = System.currentTimeMillis() + 2000
                runOnUiThread(::checkTargetReached)
                while (System.currentTimeMillis() < end) {
                    if (checkFunctionExecuted.get()) {
                        if (targetReached.get()) {
                            break
                        } else {
                            checkFunctionExecuted.set(false)
                            runOnUiThread(::checkTargetReached)
                        }
                    }
                    sleep(10)
                }
                // wait until scroll events have propagated in the system
                sleep(100)

                // then
                assertThat(targetPage, equalTo(viewPager.currentItem))
                assertThat(targetPage, equalTo(viewPager.currentCompletelyVisibleItem))
            }
        }
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_noListener_horizontal() {
        test_selectItemProgrammatically_noListener(HORIZONTAL, false)
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_noListener_vertical() {
        test_selectItemProgrammatically_noListener(VERTICAL, false)
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_noListener_horizontal() {
        test_selectItemProgrammatically_noListener(HORIZONTAL, true)
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_noListener_vertical() {
        test_selectItemProgrammatically_noListener(VERTICAL, true)
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
        viewPager.adapter = noOpAdapter

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

    private fun ViewPager2.addNewRecordingListener(): RecordingListener {
        val listener = RecordingListener()
        addOnPageChangeListener(listener)
        return listener
    }

    private val ViewPager2.currentCompletelyVisibleItem: Int
        get() {
            return ((getChildAt(0) as RecyclerView)
                    .layoutManager as LinearLayoutManager)
                    .findFirstCompletelyVisibleItemPosition()
        }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
        data class OnPageSelectedEvent(val position: Int) : Event()
        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
    }

    private class RecordingListener : ViewPager2.OnPageChangeListener {
        private val events = mutableListOf<Event>()

        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val scrollEventsBeforeSettling
            get() = events.subList(0, settlingIx).mapNotNull { it as? OnPageScrolledEvent }
        val scrollEventsAfterSettling
            get() = events.subList(settlingIx + 1, events.size)
                    .mapNotNull { it as? OnPageScrolledEvent }
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

    private fun List<OnPageScrolledEvent>.assertOffsetSorted(sortOrder: SortOrder) {
        groupBy { it.position }.forEach { (_, events) ->
            events.assertSorted { it.positionOffsetPixels * sortOrder.sign }
        }
    }

    private fun List<OnPageScrolledEvent>.assertLastCorrect(targetPage: Int) {
        last().apply {
            assertThat(position, equalTo(targetPage))
            assertThat(positionOffsetPixels, equalTo(0))
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
        assertThat((it.positionOffset * pageSize).roundToInt(), equalTo(it.positionOffsetPixels))
    }

    private fun List<OnPageScrolledEvent>.assertPositionSorted(sortOrder: SortOrder) {
        map { it.position }.assertSorted { it * sortOrder.sign }
    }

    private fun List<OnPageScrolledEvent>.assertMaxShownPages(startPage: Int) {
        assertThat(map { it.position }.distinct().minus(startPage).size, isBetweenInIn(0, 3))
    }
}
