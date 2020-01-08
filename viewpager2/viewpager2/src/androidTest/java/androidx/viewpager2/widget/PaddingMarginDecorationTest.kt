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

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.test.filters.LargeTest
import androidx.testutils.LocaleTestUtils
import androidx.viewpager2.widget.PaddingMarginDecorationTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.PaddingMarginDecorationTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.PaddingMarginDecorationTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.PaddingMarginDecorationTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_SETTLING
import androidx.viewpager2.widget.swipe.ViewAdapter
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt

@RunWith(Parameterized::class)
@LargeTest
class PaddingMarginDecorationTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean,
        val vpPaddingPx: Int,
        val rvPaddingPx: Int,
        val itemMarginPx: Int,
        val itemDecorationPx: Int
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()

        // Set unequal decorations, to prevent symmetry from hiding bugs
        // Similarly, make sure no margin is an exact multiple of another margin
        const val fLeft = 2
        const val fTop = 3
        const val fRight = 7
        const val fBottom = 5

        fun View.applyMargin(margin: Int) {
            val lp = layoutParams as MarginLayoutParams
            lp.setMargins(margin * fLeft, margin * fTop, margin * fRight, margin * fBottom)
            layoutParams = lp
        }

        fun View.applyPadding(padding: Int) {
            setPadding(padding * fLeft, padding * fTop, padding * fRight, padding * fBottom)
        }
    }

    private lateinit var test: Context
    private val viewPager get() = test.viewPager

    private val vpSize: Int get() {
        return if (viewPager.isHorizontal) viewPager.width else viewPager.height
    }

    private val vpPadding: Int get() {
        return if (viewPager.isHorizontal)
            viewPager.paddingLeft + viewPager.paddingRight
        else
            viewPager.paddingTop + viewPager.paddingBottom
    }

    private val rvSize: Int get() {
        val rv = viewPager.recyclerView
        return if (viewPager.isHorizontal) rv.width else rv.height
    }

    private val rvMargin: Int get() {
        return if (viewPager.isHorizontal)
            horizontalMargin(viewPager.recyclerView.layoutParams)
        else
            verticalMargin(viewPager.recyclerView.layoutParams)
    }

    private val rvPadding: Int get() {
        val rv = viewPager.recyclerView
        return if (viewPager.isHorizontal)
            rv.paddingLeft + rv.paddingRight
        else
            rv.paddingTop + rv.paddingBottom
    }

    private val itemSize: Int get() {
        val item = viewPager.linearLayoutManager.findViewByPosition(0)!!
        return if (viewPager.isHorizontal) item.width else item.height
    }

    private val itemMargin: Int get() {
        val item = viewPager.linearLayoutManager.findViewByPosition(0)!!
        return if (viewPager.isHorizontal)
            horizontalMargin(item.layoutParams)
        else
            verticalMargin(item.layoutParams)
    }

    private val itemDecoration: Int get() {
        val llm = viewPager.linearLayoutManager
        val item = llm.findViewByPosition(0)!!
        return if (viewPager.isHorizontal)
            llm.getLeftDecorationWidth(item) + llm.getRightDecorationWidth(item)
        else
            llm.getTopDecorationHeight(item) + llm.getBottomDecorationHeight(item)
    }

    private val adapterProvider: AdapterProviderForItems get() {
            return AdapterProviderForItems("adapterProvider", if (config.itemMarginPx > 0) {
                { items -> { MarginViewAdapter(config.itemMarginPx, items) } }
            } else {
                { items -> { ViewAdapter(items) } }
            })
        }

    class MarginViewAdapter(private val margin: Int, items: List<String>) : ViewAdapter(items) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return super.onCreateViewHolder(parent, viewType).apply { itemView.applyMargin(margin) }
        }
    }

    class ItemDecorator(private val size: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.left = size * fLeft
            outRect.top = size * fTop
            outRect.right = size * fRight
            outRect.bottom = size * fBottom
        }
    }

    override fun setUp() {
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }
        test = setUpTest(config.orientation)
        test.runOnUiThreadSync {
            viewPager.clipToPadding = false
            viewPager.applyPadding(config.vpPaddingPx)
            viewPager.recyclerView.clipToPadding = false
            viewPager.recyclerView.applyPadding(config.rvPaddingPx)
            viewPager.addItemDecoration(ItemDecorator(config.itemDecorationPx))
        }
    }

    private fun horizontalMargin(lp: ViewGroup.LayoutParams): Int {
        return if (lp is MarginLayoutParams) lp.leftMargin + lp.rightMargin else 0
    }

    private fun verticalMargin(lp: ViewGroup.LayoutParams): Int {
        return if (lp is MarginLayoutParams) lp.topMargin + lp.bottomMargin else 0
    }

    @Test
    fun test_pageSize() {
        test.setAdapterSync(adapterProvider.provider(stringSequence(1)))

        val f = if (viewPager.isHorizontal) fLeft + fRight else fTop + fBottom

        assertThat(vpPadding, equalTo(config.vpPaddingPx * f))
        assertThat(rvPadding, equalTo(config.rvPaddingPx * f))
        assertThat(itemMargin, equalTo(config.itemMarginPx * f))
        assertThat(itemDecoration, equalTo(config.itemDecorationPx * f))

        assertThat(viewPager.pageSize, equalTo(rvSize - rvPadding))
        assertThat(viewPager.pageSize, equalTo(vpSize - vpPadding - rvMargin - rvPadding))
        assertThat(viewPager.pageSize, equalTo(itemSize + itemDecoration + itemMargin))
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
        test.setAdapterSync(adapterProvider.provider(stringSequence(2)))
        listOf(1, 0).forEach { targetPage ->
            // given
            val initialPage = viewPager.currentItem
            assertThat(Math.abs(initialPage - targetPage), equalTo(1))

            val callback = viewPager.addNewRecordingCallback()
            val latch = viewPager.addWaitForScrolledLatch(targetPage)

            // when
            test.swipe(initialPage, targetPage)
            latch.await(2, SECONDS)

            // then
            test.assertBasicState(targetPage)

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
        val totalPages = 2
        val edgePages = setOf(0, totalPages - 1)

        test.setAdapterSync(adapterProvider.provider(stringSequence(totalPages)))
        listOf(0, 1, 1).forEach { targetPage ->
            // given
            val initialPage = viewPager.currentItem
            val callback = viewPager.addNewRecordingCallback()
            val latch = viewPager.addWaitForScrolledLatch(targetPage)

            // when
            test.swipe(initialPage, targetPage)
            latch.await(2, SECONDS)

            // then
            test.assertBasicState(targetPage)

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
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback() {
        private val events = mutableListOf<Event>()

        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val eventCount get() = events.size
        val scrollEventCount get() = scrollEvents.size
        val lastIx get() = events.size - 1
        val firstScrolledIx get() = events.indexOfFirst { it is OnPageScrolledEvent }
        val lastScrolledIx get() = events.indexOfLast { it is OnPageScrolledEvent }
        val settlingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_SETTLING))
        val draggingIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_DRAGGING))
        val idleIx get() = events.indexOf(OnPageScrollStateChangedEvent(SCROLL_STATE_IDLE))
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

    private fun List<OnPageScrolledEvent>.assertMaxShownPages() {
        assertThat(map { it.position }.distinct().size, isBetweenInIn(0, 4))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(false, true).flatMap { rtl ->
            listOf(
                TestConfig(orientation, rtl, 0, 0, 0, 0),
                TestConfig(orientation, rtl, 0, 0, 0, 10),
                TestConfig(orientation, rtl, 0, 0, 10, 0),
                TestConfig(orientation, rtl, 0, 10, 0, 0),
                TestConfig(orientation, rtl, 10, 0, 0, 0),
                TestConfig(orientation, rtl, 1, 2, 3, 4)
            )
        }
    }
}

// endregion
