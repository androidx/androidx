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

package androidx.viewpager2.widget

import android.os.SystemClock.sleep
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.testutils.LocaleTestUtils
import androidx.testutils.PollingCheck
import androidx.testutils.waitForExecution
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.BaseTest.Context.SwipeMethod
import androidx.viewpager2.widget.PageChangeCallbackTest.Event.MarkerEvent
import androidx.viewpager2.widget.PageChangeCallbackTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.PageChangeCallbackTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PageChangeCallbackTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.PageChangeCallbackTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import androidx.viewpager2.widget.swipe.PageSwiperManual
import androidx.viewpager2.widget.swipe.ViewAdapter
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt

@RunWith(Parameterized::class)
@LargeTest
class PageChangeCallbackTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean,
        val pageMarginPx: Int
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

    private val adapterProvider: AdapterProviderForItems get() {
        return if (config.pageMarginPx > 0) {
            { items -> { MarginViewAdapter(config.pageMarginPx, items) } }
        } else {
            { items -> { ViewAdapter(items) } }
        }
    }

    class MarginViewAdapter(private val margin: Int, items: List<String>) : ViewAdapter(items) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val viewHolder = super.onCreateViewHolder(parent, viewType)
            val lp = viewHolder.itemView.layoutParams as ViewGroup.MarginLayoutParams
            // Set unequal margins, to prevent symmetry from hiding bugs
            // Similarly, make sure no margin is an exact multiple of another margin
            lp.setMargins(margin * 2, margin * 3, margin * 7, margin * 5)
            viewHolder.itemView.layoutParams = lp
            return viewHolder
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
            setAdapterSync(adapterProvider(stringSequence(4)))
            listOf(1, 2, 3, 2, 1, 0).forEach { targetPage ->
                // given
                val initialPage = viewPager.currentItem
                assertThat(Math.abs(initialPage - targetPage), equalTo(1))

                val callback = viewPager.addNewRecordingCallback()
                val latch = viewPager.addWaitForScrolledLatch(targetPage)

                // when
                swipe(initialPage, targetPage)
                latch.await(2, SECONDS)

                // then
                assertBasicState(targetPage)

                callback.apply {
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

                viewPager.unregisterOnPageChangeCallback(callback)
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

            setAdapterSync(adapterProvider(stringSequence(totalPages)))
            listOf(0, 0, 1, 2, 2, 2, 1, 2, 2, 2, 1, 0, 0, 0).forEach { targetPage ->
                // given
                val initialPage = viewPager.currentItem
                val callback = viewPager.addNewRecordingCallback()
                val latch = viewPager.addWaitForScrolledLatch(targetPage)

                // when
                swipe(initialPage, targetPage)
                latch.await(2, SECONDS)

                // then
                assertBasicState(targetPage)

                if (targetPage == initialPage && edgePages.contains(targetPage)) {
                    callback.apply {
                        // verify all events
                        assertThat("Events should start with a state change to DRAGGING",
                            draggingIx, equalTo(0))
                        assertThat("Last event should be a state change to IDLE",
                            idleIx, equalTo(lastIx))
                        assertThat("All events but the state changes to DRAGGING and IDLE" +
                                " should be scroll events",
                            scrollEventCount, equalTo(eventCount - 2))

                        // dive into scroll events
                        scrollEvents.forEach {
                            assertThat("All scroll events should report page $targetPage",
                                it.position, equalTo(targetPage))
                            assertThat("All scroll events should report an offset of 0f",
                                it.positionOffset, equalTo(0f))
                            assertThat("All scroll events should report an offset of 0px",
                                it.positionOffsetPixels, equalTo(0))
                        }
                    }
                }

                viewPager.unregisterOnPageChangeCallback(callback)
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
            setAdapterSync(adapterProvider(stringSequence(3)))
            val callback = viewPager.addNewRecordingCallback()
            val latch = viewPager.addWaitForScrolledLatch(0)

            // when
            peekForward()
            latch.await(5, SECONDS)

            // then
            callback.apply {
                // verify all events
                assertThat("There should be exactly 1 dragging event",
                    stateEvents(SCROLL_STATE_DRAGGING).size, equalTo(1))
                assertThat("There should be exactly 1 settling event",
                    stateEvents(SCROLL_STATE_SETTLING).size, equalTo(1))
                assertThat("There should be exactly 1 idle event",
                    stateEvents(SCROLL_STATE_IDLE).size, equalTo(1))
                assertThat("Events should start with a state change to DRAGGING",
                    draggingIx, equalTo(0))
                assertThat("The settling event should be fired between the first and the last" +
                        " scroll event",
                    settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
                assertThat("The idle event should be the last global event",
                    idleIx, equalTo(lastIx))
                assertThat("All events other then the state changes should be scroll events",
                    scrollEventCount, equalTo(eventCount - 3))

                // dive into scroll events
                scrollEvents.assertPositionSorted(SortOrder.DESC)
                scrollEventsBeforeSettling.assertOffsetSorted(SortOrder.ASC)
                assertThat(scrollEvents, // sanity check
                        equalTo(scrollEventsBeforeSettling + scrollEventsAfterSettling))
                scrollEvents.assertValueSanity(0, 0, viewPager.pageSize)
                scrollEvents.assertLastCorrect(0)
            }

            viewPager.unregisterOnPageChangeCallback(callback)
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
            setAdapterSync(adapterProvider(stringSequence(3)))

            viewPager.setCurrentItemSync(2, false, 1, SECONDS)

            // set up test callbacks
            val callback = viewPager.addNewRecordingCallback()
            val latch = viewPager.addWaitForScrolledLatch(2)

            // when
            peekBackward()
            latch.await(5, SECONDS)

            // then
            callback.apply {
                // verify all events
                assertThat("There should be exactly 1 dragging event",
                    stateEvents(SCROLL_STATE_DRAGGING).size, equalTo(1))
                assertThat("There should be exactly 1 settling event",
                    stateEvents(SCROLL_STATE_SETTLING).size, equalTo(1))
                assertThat("There should be exactly 1 idle event",
                    stateEvents(SCROLL_STATE_IDLE).size, equalTo(1))
                assertThat("Events should start with a state change to DRAGGING",
                    draggingIx, equalTo(0))
                assertThat("The settling event should be fired between the first and the last " +
                        "scroll event",
                    settlingIx, isBetweenInEx(firstScrolledIx + 1, lastScrolledIx))
                assertThat("The idle event should be the last global event",
                    idleIx, equalTo(lastIx))
                assertThat("All events other then the state changes should be scroll events",
                    scrollEventCount, equalTo(eventCount - 3))

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

            viewPager.unregisterOnPageChangeCallback(callback)
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
            setAdapterSync(adapterProvider(stringSequence(1000)))

            // when
            listOf(6, 5, 6, 3, 10, 0, 0, 999, 999, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                val callback = viewPager.addNewRecordingCallback()

                viewPager.setCurrentItemSync(targetPage, true, 2, SECONDS)

                // then
                val pageIxDelta = targetPage - currentPage
                callback.apply {
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
                viewPager.unregisterOnPageChangeCallback(callback)
            }
        }
    }

    @Test
    fun test_multiplePageChanges() {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(adapterProvider(stringSequence(10)))
            val targetPages = listOf(4, 9)
            val callback = viewPager.addNewRecordingCallback()
            val latch = viewPager.addWaitForScrolledLatch(targetPages.last(), true)

            // when
            runOnUiThread {
                targetPages.forEach {
                    viewPager.setCurrentItem(it, true)
                }
            }
            latch.await(2, SECONDS)

            // then
            callback.apply {
                val targetPage = targetPages.last()
                assertThat(settlingIx, equalTo(0))
                assertThat(viewPager.currentItem, equalTo(targetPage))
                assertThat(viewPager.currentCompletelyVisibleItem, equalTo(targetPage))
                assertAllPagesSelected(targetPages)
                assertScrollsAreBetweenSelectedPages()
            }

            viewPager.unregisterOnPageChangeCallback(callback)
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
            setAdapterSync(adapterProvider(stringSequence(6)))
            val targetPage = 4
            val marker = 1
            val callback = viewPager.addNewRecordingCallback()

            // when
            runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
            viewPager.addWaitForDistanceToTarget(targetPage, 2f).await(2, SECONDS)
            runOnUiThread {
                viewPager.setCurrentItem(targetPage, false)
                callback.markEvent(marker)
            }
            viewPager.addWaitForIdleLatch().await(2, SECONDS)

            // then
            callback.apply {
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
     * Example trace:
     *
     * 0 -> 4 (smooth)
     * >> viewPager.setCurrentItem(4, true)
     * onPageScrollStateChanged(2)
     * onPageSelected(4)
     * onPageScrolled(1, 0.000000, 0)
     * >> config change
     * onPageScrolled(4, 0.000000, 0)
     */
    @Test
    fun test_configChangeDuringStartOfFarSmoothScroll() {
        test_configChangeDuringFarSmoothScroll(4) {
            // no delay
        }
    }

    /**
     * Example trace:
     *
     * 0 -> 4 (smooth)
     * >> viewPager.setCurrentItem(4, true)
     * onPageScrollStateChanged(2)
     * onPageSelected(4)
     * onPageScrolled(1, 0.000000, 0)
     * onPageScrolled(1, 0.254016, 253)
     * onPageScrolled(1, 0.641566, 639)
     * onPageScrolled(2, 0.315261, 314)
     * >> config change
     * onPageScrolled(4, 0.000000, 0)
     */
    @Test
    fun test_configChangeDuringMiddleOfFarSmoothScroll() {
        val targetPage = 4
        test_configChangeDuringFarSmoothScroll(targetPage) { viewPager ->
            // let it scroll until we're 2 pages away from the target
            viewPager.addWaitForDistanceToTarget(targetPage, 2f).await(2, SECONDS)
        }
    }

    /**
     * Example trace:
     *
     * 0 -> 4 (smooth)
     * >> viewPager.setCurrentItem(4, true)
     * onPageScrollStateChanged(2)
     * onPageSelected(4)
     * onPageScrolled(1, 0.000000, 0)
     * onPageScrolled(1, 0.371486, 370)
     * onPageScrolled(1, 0.557229, 555)
     * onPageScrolled(2, 0.180723, 180)
     * onPageScrolled(2, 0.574297, 572)
     * onPageScrolled(2, 0.903614, 900)
     * onPageScrolled(3, 0.218875, 218)
     * onPageScrolled(3, 0.437751, 436)
     * onPageScrolled(3, 0.655622, 653)
     * onPageScrolled(3, 0.803213, 800)
     * onPageScrolled(3, 0.911647, 908)
     * onPageScrolled(3, 0.978916, 975)
     * onPageScrolled(4, 0.000000, 0)
     * onPageScrollStateChanged(0)
     * >> config change
     * onPageScrolled(4, 0.000000, 0)
     */
    @Test
    fun test_configChangeAfterFarSmoothScroll() {
        test_configChangeDuringFarSmoothScroll(4) { viewPager ->
            // wait until it is finished
            viewPager.addWaitForIdleLatch().await(2, SECONDS)
        }
    }

    /**
     * Tests what happens when a config change happens during a smooth scroll to any page more then
     * 3 pages further. After the config change, the smooth scroll should be interrupted and the
     * view pager should instantly skip to the target page instead.
     *
     * The configuration change is triggered after the delay callback has executed. Thus, the delay
     * callback controls when the config change happens by the time it takes to execute.
     *
     * @param delayCallback The callback that determines when the configuration change is triggered
     */
    private fun test_configChangeDuringFarSmoothScroll(
        targetPage: Int,
        delayCallback: (ViewPager2) -> Unit
    ) {
        // given
        assertThat(targetPage, greaterThanOrEqualTo(4))
        setUpTest(config.orientation).apply {
            val adapterProvider = adapterProvider(stringSequence(5))
            setAdapterSync(adapterProvider)
            val marker = 1
            val callback = viewPager.addNewRecordingCallback()

            // when
            runOnUiThread { viewPager.setCurrentItem(targetPage, true) }
            delayCallback(viewPager)

            recreateActivity(adapterProvider) { newViewPager ->
                // mark the config change in the callback
                callback.markEvent(marker)
                // viewPager is recreated, so need to reattach callback
                newViewPager.registerOnPageChangeCallback(callback)
            }

            // wait until we're at the target page. can take a while on stuttering devices.
            // viewPager may have fired all events already, so poll the visible page instead
            viewPager.waitUntilSnappedOnTargetByPolling(targetPage)

            // then
            callback.apply {
                assertThat("viewPager.getCurrentItem() does not return the target page",
                    viewPager.currentItem, equalTo(targetPage))
                assertThat("Currently shown page is not the target page",
                    viewPager.currentCompletelyVisibleItem, equalTo(targetPage))
                assertThat("First overall event is not a SETTLING event",
                    settlingIx, equalTo(0))
                assertThat("Number of onPageSelected events is not 2",
                    selectEvents.count(), equalTo(2))
                assertThat("First onPageSelected event is not the second overall event",
                    pageSelectedIx(targetPage), equalTo(1))
                assertThat("Unexpected events were fired after the config change",
                    eventsAfter(marker), equalTo(listOf(
                        OnPageSelectedEvent(targetPage),
                        OnPageScrolledEvent(targetPage, 0f, 0)
                    )))
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
            setAdapterSync(adapterProvider(stringSequence(3)))

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                val currentPage = viewPager.currentItem
                val callback = viewPager.addNewRecordingCallback()

                viewPager.setCurrentItemSync(targetPage, false, 1, SECONDS)

                // then
                val pageIxDelta = targetPage - currentPage
                callback.apply {
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

                viewPager.unregisterOnPageChangeCallback(callback)
            }
        }
    }

    @Test
    fun test_swipeReleaseSwipeBack() {
        // given
        val test = setUpTest(config.orientation)
        test.setAdapterSync(adapterProvider(stringSequence(3)))
        val currentPage = test.viewPager.currentItem
        val halfPage = test.viewPager.pageSize / 2f
        val pageSwiper = PageSwiperManual(test.viewPager)
        var recorder = test.viewPager.addNewRecordingCallback()

        val vc = ViewConfiguration.get(test.viewPager.context)
        val touchSlop = vc.scaledTouchSlop

        // when
        tryNTimes(3, resetBlock = {
            test.viewPager.setCurrentItemSync(currentPage, false, 2, SECONDS)
            activityTestRule.waitForExecution(1)
            test.viewPager.unregisterOnPageChangeCallback(recorder)
            recorder = test.viewPager.addNewRecordingCallback()
        }) {
            val settleLatch = test.viewPager.addWaitForStateLatch(SCROLL_STATE_SETTLING)
            val idleLatch = test.viewPager.addWaitForIdleLatch()

            // Swipe towards next page
            pageSwiper.swipeForward(halfPage + 2 * touchSlop, AccelerateInterpolator())
            settleLatch.await(2, SECONDS)
            var scrollLatch: CountDownLatch? = null
            activityTestRule.runOnUiThread {
                scrollLatch = test.viewPager.addWaitForFirstScrollEventLatch()
            }
            scrollLatch!!.await(2, SECONDS)

            // now catch the settling view pager and swipe back
            pageSwiper.swipeBackward(halfPage, AccelerateInterpolator())
            idleLatch.await(2, SECONDS)

            if (!recorder.wasSettleInterrupted) {
                throw RetryException("Settling phase of first swipe was not interrupted in time")
            }
        }

        // then:

        // 1) We're at the right page
        assertThat(test.viewPager.currentItem, equalTo(0))
        assertThat(test.viewPager.currentCompletelyVisibleItem, equalTo(0))

        // 2) State sequence was DRAGGING -> SETTLING -> DRAGGING -> SETTLING -> IDLE
        assertThat(
            recorder.stateEvents.map { it.state },
            equalTo(listOf(SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING,
                SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING, SCROLL_STATE_IDLE))
        )

        // 3) Page selected sequence was select(1) -> select(0)
        assertThat(
            recorder.selectEvents.map { it.position },
            equalTo(listOf(1, 0))
        )

        val idle = OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE)
        val dragging = OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING)
        val settling = OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING)

        // 4) Scroll events during the first swipe were ascending
        recorder.allEvents
            .assertScrollEventsBetweenEventsSorted(dragging, dragging, SortOrder.ASC)
        // 5) Scroll events during the second swipe were descending
        recorder.allEvents.dropWhile { it != settling }
            .assertScrollEventsBetweenEventsSorted(dragging, idle, SortOrder.DESC)
    }

    /**
     * Test behavior when no {@link OnPageChangeCallback}s are attached.
     * Introduced after finding a regression.
     */
    private fun test_selectItemProgrammatically_noCallback(smoothScroll: Boolean) {
        // given
        setUpTest(config.orientation).apply {
            setAdapterSync(adapterProvider(stringSequence(3)))

            // when
            listOf(2, 2, 0, 0, 1, 2, 1, 0).forEach { targetPage ->
                runOnUiThread { viewPager.setCurrentItem(targetPage, smoothScroll) }

                // poll the viewpager on the ui thread
                viewPager.waitUntilSnappedOnTargetByPolling(targetPage)

                // wait until scroll events have propagated in the system
                sleep(100)

                // then
                assertThat(targetPage, equalTo(viewPager.currentItem))
                assertThat(targetPage, equalTo(viewPager.currentCompletelyVisibleItem))
            }
        }
    }

    @Test
    fun test_selectItemProgrammatically_noSmoothScroll_noCallback() {
        test_selectItemProgrammatically_noCallback(false)
    }

    @Test
    fun test_selectItemProgrammatically_smoothScroll_noCallback() {
        test_selectItemProgrammatically_noCallback(true)
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
    fun test_getScrollState() {
        val test = setUpTest(config.orientation)
        test.setAdapterSync(viewAdapterProvider(stringSequence(5)))

        // Test SCROLL_STATE_SETTLING
        test_getScrollState(test, SCROLL_STATE_SETTLING, 1) {
            test.runOnUiThread { test.viewPager.setCurrentItem(1, true) }
        }

        // Test SCROLL_STATE_DRAGGING (real drag)
        test_getScrollState(test, SCROLL_STATE_DRAGGING, 2, true) {
            // Perform manual swipe in separate thread, because the SwipeMethod.MANUAL blocks while
            // injecting events, and we need to check getScrollState() during the swipe.
            newSingleThreadExecutor().execute { test.swipeForward(SwipeMethod.MANUAL) }
        }

        // Test SCROLL_STATE_DRAGGING (fake drag)
        test_getScrollState(test, SCROLL_STATE_DRAGGING, 3, true) {
            test.swipeForward(SwipeMethod.FAKE_DRAG)
        }
    }

    private fun test_getScrollState(
        test: Context,
        @ViewPager2.ScrollState state: Int,
        expectedTargetPage: Int,
        checkSettling: Boolean = false,
        viewPagerAction: () -> Unit
    ) {
        val stateLatch = test.viewPager.addWaitForStateLatch(state)
        val settlingLatch = test.viewPager.addWaitForStateLatch(SCROLL_STATE_SETTLING)
        val idleLatch = test.viewPager.addWaitForIdleLatch()
        viewPagerAction()
        // Wait for onScrollStateChanged
        assertThat(stateLatch.await(1, SECONDS), equalTo(true))
        // Check scrollState
        assertThat(test.viewPager.scrollState, equalTo(state))
        if (checkSettling) {
            assertThat(settlingLatch.await(2, SECONDS), equalTo(true))
        }
        // Let the animation finish
        assertThat(idleLatch.await(2, SECONDS), equalTo(true))
        test.assertBasicState(expectedTargetPage)
    }

    @Test
    fun test_setCurrentItem_noAdapter() {
        val test = setUpTest(config.orientation)
        assertThat(test.viewPager.adapter, nullValue())
        assertThat(test.viewPager.currentItem, equalTo(0))

        listOf(-1, 0, 1, 10).forEach { targetPage ->
            // given
            val callback = test.viewPager.addNewRecordingCallback()

            // when
            test.viewPager.setCurrentItemSync(targetPage, false, 2, SECONDS, false)

            // then
            assertThat(test.viewPager.currentItem, equalTo(0))
            assertThat(callback.eventCount, equalTo(0))
            test.viewPager.unregisterOnPageChangeCallback(callback)
        }
    }

    @Test
    fun test_swipe_noAdapter() {
        val test = setUpTest(config.orientation)
        assertThat(test.viewPager.adapter, nullValue())
        assertThat(test.viewPager.currentItem, equalTo(0))

        listOf(test::swipeForward, test::swipeBackward).forEach { swipe ->
            val recorder = test.viewPager.addNewRecordingCallback()

            val idleLatch = test.viewPager.addWaitForIdleLatch()
            swipe(SwipeMethod.ESPRESSO)
            idleLatch.await(2, SECONDS)

            assertThat(recorder.allEvents, equalTo(listOf(
                OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING) as Event,
                OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE) as Event
            )))
            test.viewPager.unregisterOnPageChangeCallback(recorder)
        }
    }

    /**
     * Expected trace (marker events left out):
     *
     * >> viewPager.setAdapter(adapter)
     * onPageSelected(0)
     * onPageScrolled(0, 0.000000, 0)
     * >> config change
     * onPageSelected(0)
     * onPageScrolled(0, 0.000000, 0)
     * >> viewPager.setCurrentItem(2, false)
     * onPageSelected(2)
     * onPageScrolled(2, 0.000000, 0)
     * >> config change
     * onPageSelected(2)
     * onPageScrolled(2, 0.000000, 0)
     */
    @Test
    fun test_initialEvents() {
        // given
        val test = setUpTest(config.orientation)
        val recorder = test.viewPager.addNewRecordingCallback()
        val adapterProvider = viewAdapterProvider(stringSequence(3))
        val marker = 1

        fun expectedEvents(page: Int): List<Event> {
            return listOf(
                OnPageSelectedEvent(page) as Event,
                OnPageScrolledEvent(page, 0f, 0) as Event
            )
        }

        // when
        test.setAdapterSync(adapterProvider)
        // then
        assertThat(recorder.allEvents, equalTo(expectedEvents(0)))

        // when
        recorder.reset()
        test.recreateActivity(adapterProvider) { newViewPager ->
            recorder.markEvent(marker)
            // viewPager is recreated, so need to reattach callback
            newViewPager.registerOnPageChangeCallback(recorder)
        }
        // then
        assertThat(recorder.allEvents, equalTo(
            listOf(MarkerEvent(marker))
                .plus(expectedEvents(0)))
        )

        // given
        val targetPage = 2
        // when
        recorder.reset()
        test.viewPager.setCurrentItemSync(targetPage, false, 2, SECONDS)
        test.recreateActivity(adapterProvider) { newViewPager ->
            recorder.markEvent(marker)
            // viewPager is recreated, so need to reattach callback
            newViewPager.registerOnPageChangeCallback(recorder)
        }
        // then
        assertThat(recorder.allEvents, equalTo(
            expectedEvents(targetPage)
                .plus(MarkerEvent(marker))
                .plus(expectedEvents(targetPage)))
        )
    }

    private fun test_setCurrentItem_outOfBounds(smoothScroll: Boolean) {
        val test = setUpTest(config.orientation)
        val n = 3
        test.setAdapterSync(adapterProvider(stringSequence(n)))
        val adapterCount = test.viewPager.adapter!!.itemCount

        listOf(-5, -1, n, n + 1, adapterCount, adapterCount + 1).forEach { targetPage ->
            assertThat("Test should only test setCurrentItem for pages out of bounds, " +
                    "bounds are [0, $n)", targetPage, not(isBetweenInEx(0, n)))
            // given
            val initialPage = test.viewPager.currentItem
            val callback = test.viewPager.addNewRecordingCallback()
            val targetBoundary = if (targetPage <= 0) 0 else n - 1
            // only expect events when we're going to the boundary on the other side
            val expectEvents = initialPage != targetBoundary

            // when
            test.viewPager.setCurrentItemSync(targetPage, smoothScroll, 2, SECONDS, expectEvents)

            // then the viewpager must have scrolled to the respective boundary
            assertThat(test.viewPager.currentItem, equalTo(targetBoundary))
            if (!expectEvents) {
                assertThat(callback.eventCount, equalTo(0))
            } else {
                // make sure the page select events and scroll events are correct
                val pageSize = test.viewPager.pageSize
                callback.scrollEvents.assertValueSanity(initialPage, targetBoundary, pageSize)
                callback.scrollEvents.assertLastCorrect(targetBoundary)
                callback.assertAllPagesSelected(listOf(targetBoundary))
            }
            test.viewPager.unregisterOnPageChangeCallback(callback)
        }
    }

    @Test
    fun test_setCurrentItem_outOfBounds_smoothScroll() {
        test_setCurrentItem_outOfBounds(true)
    }

    @Test
    fun test_setCurrentItem_outOfBounds_noSmoothScroll() {
        test_setCurrentItem_outOfBounds(false)
    }

    @Test
    fun test_setCurrentItemBeforeRender() {
        // given
        val viewPager =
            ViewPager2(ApplicationProvider.getApplicationContext() as android.content.Context)
        val noOpAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(View(parent.context)) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
            override fun getItemCount(): Int = 5
        }
        viewPager.adapter = noOpAdapter

        // when
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
        viewPager.setCurrentItem(2, true)
        viewPager.setCurrentItem(3, false)

        // then
        // no crash
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
    }

    private fun ViewPager2.waitUntilSnappedOnTargetByPolling(targetPage: Int) {
        PollingCheck.waitFor(2000) {
            currentCompletelyVisibleItem == targetPage
        }
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

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val allEvents get() = events
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

        val wasSettleInterrupted: Boolean get() {
            val changeToSettlingEvent = OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING)
            val lastScrollEvent = events
                .dropWhile { it != changeToSettlingEvent }
                .dropWhile { it !is OnPageScrolledEvent }
                .takeWhile { it is OnPageScrolledEvent }
                .lastOrNull() as? OnPageScrolledEvent
            return lastScrollEvent?.let { it.positionOffsetPixels != 0 } ?: false
        }

        fun stateEvents(state: Int): List<OnPageScrollStateChangedEvent> {
            return stateEvents.filter { it.state == state }
        }

        fun reset() {
            events.clear()
        }

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

    private fun RecordingCallback.assertAllPagesSelected(pages: List<Int>) {
        assertThat(listOf(1, 2), not(equalTo(listOf(2, 1))))
        assertThat(selectEvents.map { it.position }, equalTo(pages))
    }

    private fun RecordingCallback.assertScrollsAreBetweenSelectedPages() {
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

    private fun RecordingCallback.assertTargetReachedAfterMarker(targetPage: Int, marker: Int) {
        val finalEvents = eventsAfter(marker)
        assertThat(finalEvents.size, greaterThan(0))
        assertThat(finalEvents[0], equalTo(OnPageScrolledEvent(targetPage, 0f, 0) as Event))
        assertThat(
            finalEvents[1],
            equalTo(OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE) as Event)
        )
    }

    private fun List<OnPageScrolledEvent>.assertPositionSorted(sortOrder: SortOrder) {
        map { it.position }.assertSorted { it * sortOrder.sign }
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

    private fun List<Event>.assertScrollEventsBetweenEventsSorted(
        first: Event,
        second: Event,
        sortOrder: SortOrder
    ) {
        slice(first, second)
            .mapNotNull { it as? OnPageScrolledEvent }
            .assertOffsetSorted(sortOrder)
    }

    private fun List<OnPageScrolledEvent>.assertOffsetSorted(sortOrder: SortOrder) {
        map { it.position + it.positionOffset }.assertSorted { it * sortOrder.sign }
    }

    private fun List<OnPageScrolledEvent>.assertMaxShownPages() {
        assertThat(map { it.position }.distinct().size, isBetweenInIn(0, 4))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).flatMap { rtl ->
            listOf(0, 10, -10).map { margin ->
                TestConfig(orientation, rtl, margin)
            }
        }
    }
}

// endregion
