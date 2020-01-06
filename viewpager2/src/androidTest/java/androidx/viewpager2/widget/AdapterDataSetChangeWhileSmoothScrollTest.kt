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

import androidx.test.filters.LargeTest
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Event.MarkerEvent
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification.REMOVE_FIRST_VISIBLE
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification.SHIFT_FIRST_VISIBLE
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification.SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification.SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST_AND_LAST
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.Modification.SHIFT_FIRST_VISIBLE_THEN_REMOVE_LAST
import androidx.viewpager2.widget.AdapterDataSetChangeWhileSmoothScrollTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt
import kotlin.math.sign

/** Number of pages */
private const val pageCount = 25
/** Page where VP2 starts */
private const val initialPage = 0
/** Page where we smooth scroll to */
private const val targetPage = 20

private const val removeCountHead = 2

/** Id of the mark we make when modifying the dataset */
private const val modificationMark = 1

/** How many pages from x before x gets bound? */
private const val bindThreshold = 2
/** Value between 0 and 1/pageSizePx */
private const val epsilon = 0.00001f

@RunWith(Parameterized::class)
@LargeTest
class AdapterDataSetChangeWhileSmoothScrollTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean,
        val targetBound: Boolean,
        val modification: Modification,
        val adapterProvider: AdapterProviderForItems
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    enum class Modification {
        SHIFT_FIRST_VISIBLE,
        SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST,
        SHIFT_FIRST_VISIBLE_THEN_REMOVE_LAST,
        SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST_AND_LAST,
        REMOVE_FIRST_VISIBLE
    }

    // start and end of the window of opportunity to modify the dataset
    private val windowStart = targetPage - bindThreshold - if (config.targetBound) 0 else 1
    private val windowEnd = targetPage - if (config.targetBound) 1 else bindThreshold

    private lateinit var test: Context
    private lateinit var dataSet: MutableList<String>

    override fun setUp() {
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }

        test = setUpTest(config.orientation)
        activityTestRule.runOnUiThread { test.viewPager.offscreenPageLimit = 1 }
        dataSet = stringSequence(pageCount).toMutableList()
        test.setAdapterSync(config.adapterProvider.provider(dataSet))
    }

    @Test
    fun test() {
        tryNTimes(3, resetBlock = { test.resetViewPagerTo(initialPage) }) {
            // given
            test.assertBasicState(initialPage, dataSet[initialPage])

            // when we are scrolling to the target
            val recorder = test.viewPager.addNewRecordingCallback()
            val idleLatch = test.viewPager.addWaitForIdleLatch()

            scrollToTargetUntilWindowStart()

            // and we remove the first visible item
            test.modifyDataSetSync {
                verifyWindowOfOpportunity(recorder.scrollEvents.last())
                recorder.markModification()
                makeModification()
            }
            idleLatch.await(10, SECONDS)

            // then
            val expectedFinalPosition = test.viewPager.currentCompletelyVisibleItem
            test.assertBasicState(expectedFinalPosition, dataSet[expectedFinalPosition])
            recorder.apply {
                val removeItemMarkIx = markerIx(modificationMark)
                val expectedSelectEvents = if (targetPage == expectedFinalPosition) {
                    listOf(targetPage)
                } else {
                    listOf(targetPage, expectedFinalPosition)
                }
                // verify all events
                assertThat(settlingIx, equalTo(0))
                assertThat(pageSelectedIx(targetPage), equalTo(1))
                assertThat(removeItemMarkIx, greaterThan(1))
                assertThat(idleIx, equalTo(lastIx))
                assertThat(selectEvents.map { it.position }, equalTo(expectedSelectEvents))
                assertThat(scrollEventCount, equalTo(eventCount - 3 - expectedSelectEvents.size))

                // verify scroll events _before_ and _after_ the marker
                val scrollsBeforeMarker = scrollEventsBefore(removeItemMarkIx)
                val scrollsAfterMarker = scrollEventsAfter(removeItemMarkIx)
                listOf(scrollsBeforeMarker, scrollsAfterMarker).forEach {
                    it.assertPositionSorted(SortOrder.ASC)
                    it.assertValueSanity(0, targetPage + removeCountHead, test.viewPager.pageSize)
                }
                // Only check assertOffsetSorted on scroll events _before_ the marker:
                //   after the data set change, it can overshoot and reverse direction
                scrollsBeforeMarker.assertOffsetSorted(SortOrder.ASC)
                // Only check assertMaxShownPages on scroll events _before_ the marker:
                //   after the data set change, it can scroll an arbitrary number of pages
                scrollsBeforeMarker.assertMaxShownPages()
                // Only check assertLastCorrect on scroll events _after_ the marker:
                //   the target is not reached before the data set change
                scrollsAfterMarker.assertLastCorrect(expectedFinalPosition)
                // On scroll events _after_ the marker, allow it to overshoot and reverse direction
                scrollsAfterMarker.assertOffsetSortedWithOvershoot(SortOrder.ASC)
            }
        }
    }

    private fun scrollToTargetUntilWindowStart() {
        val latch = test.viewPager
            .addWaitForDistanceToTarget(targetPage, targetPage - windowStart - epsilon)
        test.runOnUiThreadSync {
            test.viewPager.setCurrentItem(targetPage, true)
        }
        latch.await(2, SECONDS)
    }

    private fun verifyWindowOfOpportunity(lastScrollEvent: OnPageScrolledEvent) {
        val lastScrollPosition = lastScrollEvent.let {
            it.position + it.positionOffset.toDouble()
        }
        if (lastScrollPosition >= windowEnd) {
            throw RetryException("Data set should be modified while scrolling through " +
                    "($windowStart, $windowEnd), but was modified at $lastScrollPosition")
        }
    }

    private fun makeModification() {
        when (config.modification) {
            REMOVE_FIRST_VISIBLE -> {
                removeCurrentPage()
            }
            SHIFT_FIRST_VISIBLE -> {
                shiftCurrentPageToStart()
            }
            SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST -> {
                shiftCurrentPageToStart()
                removeFirstPages()
            }
            SHIFT_FIRST_VISIBLE_THEN_REMOVE_LAST -> {
                shiftCurrentPageToStart()
                removeLastPages()
            }
            SHIFT_FIRST_VISIBLE_THEN_REMOVE_FIRST_AND_LAST -> {
                shiftCurrentPageToStart()
                removeLastPages()
                removeFirstPages()
            }
        }
    }

    private fun shiftCurrentPageToStart() {
        // Move currently visible position and target apart from each other
        repeat(test.viewPager.linearLayoutManager.findFirstVisibleItemPosition()) {
            val item = dataSet.removeAt(0)
            dataSet.add(targetPage - 1, item)
            test.viewPager.adapter!!.notifyItemMoved(0, targetPage - 1)
        }
    }

    private fun removeCurrentPage() {
        val position = test.viewPager.linearLayoutManager.findFirstVisibleItemPosition()
        dataSet.removeAt(position)
        test.viewPager.adapter!!.notifyItemRemoved(position)
    }

    private fun removeLastPages() {
        // Remove last items (including the target)
        val removeCount = pageCount - targetPage
        repeat(removeCount) {
            dataSet.removeAt(targetPage)
        }
        test.viewPager.adapter!!.notifyItemRangeRemoved(targetPage, removeCount)
    }

    private fun removeFirstPages() {
        // Remove first items (including the first visible item)
        repeat(removeCountHead) { dataSet.removeAt(0) }
        test.viewPager.adapter!!.notifyItemRangeRemoved(0, removeCountHead)
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also { registerOnPageChangeCallback(it) }
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

        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val selectEvents get() = events.mapNotNull { it as? OnPageSelectedEvent }
        val eventCount get() = events.size
        val scrollEventCount get() = scrollEvents.size
        val lastIx get() = events.size - 1
        val settlingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING))
        val idleIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE))
        val pageSelectedIx: (page: Int) -> Int = { events.indexOf(OnPageSelectedEvent(it)) }
        val markerIx: (id: Int) -> Int = { events.indexOf(MarkerEvent(it)) }

        val scrollEventsBefore: (ix: Int) -> List<OnPageScrolledEvent> =
            { scrollEventsBetween(0, it) }
        val scrollEventsAfter: (ix: Int) -> List<OnPageScrolledEvent> =
            { scrollEventsBetween(it + 1, events.size) }
        val scrollEventsBetween: (fromIx: Int, toIx: Int) -> List<OnPageScrolledEvent> = { a, b ->
            events.subList(a, b).mapNotNull { it as? OnPageScrolledEvent }
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

    private fun RecordingCallback.markModification() {
        markEvent(modificationMark)
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

    private fun List<OnPageScrolledEvent>.assertOffsetSorted(sortOrder: SortOrder) {
        map { it.position + it.positionOffset.toDouble() }.assertSorted { it * sortOrder.sign }
    }

    private fun List<OnPageScrolledEvent>.assertOffsetSortedWithOvershoot(sortOrder: SortOrder) {
        assertThat(
            map { it.position + it.positionOffset.toDouble() }
                .zipWithNext { a, b -> sign(b - a).toInt() }
                // got list with signs, first k should be sortOrder, last l should be !sortOrder
                .dropWhile { it == sortOrder.sign }
                .dropWhile { it != sortOrder.sign }
                .size,
            equalTo(0)
        )
    }

    private fun List<OnPageScrolledEvent>.assertMaxShownPages() {
        assertThat(map { it.position }.distinct().size, isBetweenInIn(0, 4))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(viewAdapterProvider, viewAdapterProviderValueId).flatMap { adapterProvider ->
        listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
            listOf(false, true).flatMap { rtl ->
                listOf(true, false).flatMap { targetBound ->
                    Modification.values().map { modification ->
                        TestConfig(orientation, rtl, targetBound, modification, adapterProvider)
                    }
                }
            }
        }
    }
}

// endregion
