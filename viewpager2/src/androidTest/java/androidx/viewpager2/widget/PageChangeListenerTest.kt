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
import androidx.recyclerview.widget.RecyclerView
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.LocaleTestUtils
import androidx.viewpager2.widget.PageChangeListenerTest.Event.MarkerEvent
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PageChangeListenerTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.PageChangeListenerTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

@RunWith(Parameterized::class)
@LargeTest
class PageChangeListenerTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    override fun setUp() {
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }
    }

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
    @Test
    fun test_swipeBetweenPages() {
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(4)))
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
                assertBasicState(targetPage, "$targetPage")

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
                    scrollEvents.assertMaxShownPages()
                }
            }
        }
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
    @Test
    fun test_swipeBeyondEdgePages() {
        val totalPages = 3
        val edgePages = setOf(0, totalPages - 1)

        setUpTest(config.orientation).apply {

            setAdapterSync(viewAdapterProvider(stringSequence(totalPages)))
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
                assertBasicState(targetPage, "$targetPage")

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
    @Test
    fun test_peekOnAdjacentPage_next() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(3)))
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(0)

            // when
            peekForward(config.orientation)
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
    @Test
    fun test_peekOnAdjacentPage_previous() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(3)))

            viewPager.setCurrentItemSync(2, false, 200, MILLISECONDS)

            // set up test listeners
            viewPager.clearOnPageChangeListeners()
            val listener = viewPager.addNewRecordingListener()
            val latch1 = viewPager.addWaitForScrolledLatch(2)

            // when
            peekBackward(config.orientation)
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
    @Test
    fun test_selectItemProgrammatically_smoothScroll() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(1000)))

            // when
            listOf(6, 5, 6, 3, 10, 0, 0, 999, 999, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()

                viewPager.setCurrentItemSync(targetPage, true, 2, SECONDS)

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
                            scrollEvents.assertMaxShownPages()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun test_multiplePageChanges() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(10)))
            val targetPages = listOf(4, 9)
            val listener = viewPager.addNewRecordingListener()
            val latch = viewPager.addWaitForScrolledLatch(targetPages.last(), false)

            // when
            runOnUiThread {
                targetPages.forEach {
                    viewPager.setCurrentItem(it, true)
                }
            }
            latch.await(2, SECONDS)

            // then
            listener.apply {
                val targetPage = targetPages.last()
                assertThat(settlingIx, equalTo(0))
                assertThat(viewPager.currentItem, equalTo(targetPage))
                assertThat(viewPager.currentCompletelyVisibleItem, equalTo(targetPage))
                assertAllPagesSelected(targetPages)
                assertScrollsAreBetweenSelectedPages()
            }
        }
    }

    /**
     * Tests the case where setCurrentItem(x, false) is called while the smooth scroll from
     * setCurrentItem(x, true) is not yet finished.
     *
     * Sample log to guide te test:
     *
     * 0 -> 4 (smooth) -> 4 (not smooth)
     * >> setCurrentItem(4, true);
     * onPageScrollStateChanged(2)
     * onPageSelected(4)
     * onPageScrolled(1, 0.000000, 0)
     * onPageScrolled(1, 0.271084, 270)
     * onPageScrolled(1, 0.557229, 555)
     * onPageScrolled(1, 0.843373, 840)
     * onPageScrolled(2, 0.129518, 129)
     * >> setCurrentItem(4, false);
     * onPageScrolled(4, 0.000000, 0)
     * onPageScrollStateChanged(0)
     */
    @Test
    fun test_noSmoothScroll_after_smoothScroll() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(6)))
            val targetPage = 4
            val marker = 1
            val listener = viewPager.addNewRecordingListener()

            // when
            runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
            viewPager.addWaitForDistanceToTarget(targetPage, 2f).await(2, SECONDS)
            runOnUiThread {
                viewPager.setCurrentItem(targetPage, false)
                listener.markEvent(marker)
            }
            viewPager.addWaitForIdleLatch().await(2, SECONDS)

            // then
            listener.apply {
                assertThat(selectEvents.map { it.position }, equalTo(listOf(targetPage)))
                assertThat(
                    stateEvents.map { it.state },
                    equalTo(listOf(SCROLL_STATE_SETTLING, SCROLL_STATE_IDLE))
                )
                assertTargetReachedAfterMarker(targetPage, marker)
            }
        }
    }

    /**
     * Tests a very specific case that can theoretically happen when a config change happens right
     * after an invocation to setCurrentItem. Due to a workaround for b/114019007, a smooth scroll
     * to a 'far away' page is split over two frames: first an instant scroll is done to a page
     * 'close by' and in the next frame a smooth scroll is started to the actual page.
     *
     * Now, if the config change occurs between these two frames, the second part is never executed,
     * and ViewPager2 will correct this after the config change. This test makes sure that this
     * correction has no side effects.
     *
     * Note that this test can be removed if we remove our workaround.
     */
    @Test
    fun test_configChangeDuringFarSmoothScroll() {
        // given
        setUpTest(config.orientation).apply {
            val adapterProvider = viewAdapterProvider(stringSequence(5))
            setAdapterSync(adapterProvider)
            val targetPage = 4
            val marker = 1
            val listener = viewPager.addNewRecordingListener()

            // when
            runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
            recreateActivity(adapterProvider)
            // mark the config change in the listener
            listener.markEvent(marker)
            // viewPager is recreated, so need to reattach listener
            viewPager.addOnPageChangeListener(listener)

            // then
            listener.apply {
                assertThat(viewPager.currentItem, equalTo(targetPage))
                assertThat(viewPager.currentCompletelyVisibleItem, equalTo(targetPage))
                assertThat(settlingIx, equalTo(0))
                assertThat(selectEvents.count(), equalTo(1))
                assertThat(pageSelectedIx(targetPage), equalTo(1))
                assertThat(markIx(marker), equalTo(lastIx))
            }
        }
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
    @Test
    fun test_selectItemProgrammatically_noSmoothScroll() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(3)))

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                viewPager.clearOnPageChangeListeners()
                val listener = viewPager.addNewRecordingListener()

                viewPager.setCurrentItemSync(targetPage, false, 200, MILLISECONDS)

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

    /**
     * Test behavior when no OnPageChangeListeners are attached.
     * Introduced after finding a regression.
     */
    private fun test_selectItemProgrammatically_noListener(smoothScroll: Boolean) {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(viewAdapterProvider(stringSequence(3)))

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                runOnUiThread { viewPager.setCurrentItem(targetPage, smoothScroll) }

                // poll the viewpager on the ui thread
                val targetReached = AtomicBoolean(false)
                PollingCheck.waitFor(2000) {
                    runOnUiThread {
                        targetReached.set(targetPage == viewPager.currentCompletelyVisibleItem)
                    }
                    targetReached.get()
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
    fun test_selectItemProgrammatically_noSmoothScroll_noListener() {
        test_selectItemProgrammatically_noListener(false)
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_noListener() {
        test_selectItemProgrammatically_noListener(true)
    }

    @Test
    fun test_scrollStateValuesInSync() {
        assertThat(ViewPager2.SCROLL_STATE_IDLE, allOf(equalTo(ViewPager.SCROLL_STATE_IDLE),
                equalTo(RecyclerView.SCROLL_STATE_IDLE)))
        assertThat(ViewPager2.SCROLL_STATE_DRAGGING, allOf(equalTo(ViewPager.SCROLL_STATE_DRAGGING),
                equalTo(RecyclerView.SCROLL_STATE_DRAGGING)))
        assertThat(ViewPager2.SCROLL_STATE_SETTLING, allOf(equalTo(ViewPager.SCROLL_STATE_SETTLING),
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
        return RecordingListener().also { addOnPageChangeListener(it) }
    }

    private sealed class Event {
        data class OnPageScrolledEvent(
            val position: Int,
            val positionOffset: Float,
            val positionOffsetPixels: Int
        ) : Event()
        data class OnPageSelectedEvent(val position: Int) : Event()
        data class OnPageScrollStateChangedEvent(val state: Int) : Event()
        data class MarkerEvent(val id: Int) : Event()
    }

    private class RecordingListener : ViewPager2.OnPageChangeListener {
        private val events = mutableListOf<Event>()

        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val scrollEventsBeforeSettling
            get() = events.subList(0, settlingIx).mapNotNull { it as? OnPageScrolledEvent }
        val scrollEventsAfterSettling
            get() = events.subList(settlingIx + 1, events.size)
                    .mapNotNull { it as? OnPageScrolledEvent }
        val selectEvents get() = events.mapNotNull { it as? OnPageSelectedEvent }
        val stateEvents get() = events.mapNotNull { it as? OnPageScrollStateChangedEvent }
        val scrollAndSelectEvents get() = events.mapNotNull {
            it as? OnPageScrolledEvent ?: it as? OnPageSelectedEvent
        }
        val eventsAfter: (mark: Int) -> List<Event> = { mark ->
            events.dropWhile { (it as? MarkerEvent)?.id != mark }.drop(1)
        }
        val eventCount get() = events.size
        val scrollEventCount get() = scrollEvents.size
        val lastIx get() = events.size - 1
        val firstScrolledIx get() = events.indexOfFirst { it is OnPageScrolledEvent }
        val lastScrolledIx get() = events.indexOfLast { it is OnPageScrolledEvent }
        val settlingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING))
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING))
        val idleIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE))
        val pageSelectedIx: (page: Int) -> Int = { events.indexOf(OnPageSelectedEvent(it)) }
        val markIx: (id: Int) -> Int = { events.indexOf(MarkerEvent(it)) }

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

        fun markEvent(id: Int) {
            events.add(MarkerEvent(id))
        }
    }

    private fun RecordingListener.assertAllPagesSelected(pages: List<Int>) {
        assertThat(listOf(1, 2), not(equalTo(listOf(2, 1))))
        assertThat(selectEvents.map { it.position }, equalTo(pages))
    }

    private fun RecordingListener.assertScrollsAreBetweenSelectedPages() {
        var selectedPage = -1
        var prevScrollPosition = 0f
        scrollAndSelectEvents.forEach { event ->
            when (event) {
                is OnPageSelectedEvent -> selectedPage = event.position
                is OnPageScrolledEvent -> {
                    assertThat(selectedPage, not(equalTo(-1)))
                    val currScrollPosition = event.position + event.positionOffset
                    assertThat(currScrollPosition,
                        isBetweenInIn(prevScrollPosition, selectedPage.toFloat()))
                    prevScrollPosition = currScrollPosition
                }
            }
        }
    }

    private fun RecordingListener.assertTargetReachedAfterMarker(targetPage: Int, marker: Int) {
        val finalEvents = eventsAfter(marker)
        assertThat(finalEvents.get(0), equalTo(OnPageScrolledEvent(targetPage, 0f, 0) as Event))
        assertThat(
            finalEvents.get(1),
            equalTo(OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE) as Event)
        )
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
        assertThat(it.position, isBetweenInInMinMax(initialPage, otherPage))
        assertThat(it.positionOffset, isBetweenInEx(0f, 1f))
        assertThat((it.positionOffset * pageSize).roundToInt(), equalTo(it.positionOffsetPixels))
    }

    private fun List<OnPageScrolledEvent>.assertPositionSorted(sortOrder: SortOrder) {
        map { it.position }.assertSorted { it * sortOrder.sign }
    }

    private fun List<OnPageScrolledEvent>.assertMaxShownPages() {
        assertThat(map { it.position }.distinct().size, isBetweenInIn(0, 4))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).map { rtl ->
            TestConfig(orientation, rtl)
        }
    }
}

// endregion
