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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.filters.LargeTest
import androidx.viewpager2.LocaleTestUtils
import androidx.viewpager2.widget.OffscreenPageLimitTest.Event.OnChildViewAdded
import androidx.viewpager2.widget.OffscreenPageLimitTest.Event.OnChildViewRemoved
import androidx.viewpager2.widget.OffscreenPageLimitTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.OffscreenPageLimitTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.OffscreenPageLimitTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.OffscreenPageLimitTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThan
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@RunWith(Parameterized::class)
class OffscreenPageLimitTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val rtl: Boolean,
        val adapterProvider: AdapterProviderForItems,
        val offscreenPageLimit: Int
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    private val pageCount = 10
    private val firstPage = 0
    private val limit get() = config.offscreenPageLimit

    private lateinit var test: Context

    override fun setUp() {
        super.setUp()
        if (config.rtl) {
            localeUtil.resetLocale()
            localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        }
    }

    @Test
    @LargeTest
    fun test() {
        test = setUpTest(config.orientation)
        activityTestRule.runOnUiThread {
            test.viewPager.offscreenPageLimit = config.offscreenPageLimit
        }
        val recorder = test.viewPager.addNewRecordingCallback()
        test.setAdapterSync(config.adapterProvider(stringSequence(pageCount)))
        // Do not perform self check (which checks number of shown + cached fragments) in
        // this test, as that check is not valid in the presence of offscreen page limit
        test.assertBasicState(firstPage, performSelfCheck = false)

        listOf(
            Pair(9, true),
            Pair(5, false),
            Pair(4, true),
            Pair(1, false),
            Pair(6, true),
            Pair(0, false)
        ).forEach { target ->
            test.viewPager.setCurrentItemSync(target.first, target.second, 2, SECONDS)
            assertOffscreenPagesInvariant(recorder)
        }
    }

    /**
     * OffscreenPageLimit invariant: at all times, the number of pages laid out before and after the
     * currently visible page(s) must be equal to the offscreenPageLimit, or to the number of
     * available pages on that side, whichever is the lower.
     */
    private fun assertOffscreenPagesInvariant(recorder: RecordingCallback) {
        val onscreen = mutableSetOf<Int>()
        // Determine which pages were 'onscreen' (as opposed to 'offscreen') at any time, by
        // simulating the sequence of events and record the onscreen pages in the set 'onscreen'
        recorder.events.forEachIndexed { i, event ->
            when (event) {
                // When a child is added, add it to the onscreen set
                is OnChildViewAdded -> assertThat(onscreen.add(event.position), equalTo(true))
                // When a child is removed, remove it from the onscreen set
                is OnChildViewRemoved -> assertThat(onscreen.remove(event.position), equalTo(true))
                // When VP2 scrolls, check if the set of onscreen pages is the expected value
                is OnPageScrolledEvent -> {
                    val position = event.position + event.positionOffset
                    val lower = max(0, floor(position - limit).roundToInt())
                    val upper = min(pageCount - 1, ceil(position + limit).roundToInt())
                    // First verify this calculation:
                    assertThat(lower.toFloat(), lessThanOrEqualTo(position))
                    assertThat(upper.toFloat(), greaterThanOrEqualTo(position))
                    // Then verify the onscreen pages:
                    assertThat("There should be ${upper - lower + 1} pages laid out at event $i. " +
                            "Events: ${recorder.dumpEvents()}",
                        onscreen.size, equalTo(upper - lower + 1))
                    (lower..upper).forEach { laidOutPage ->
                        assertThat("Page $laidOutPage should be laid out at event $i. " +
                                "Events: ${recorder.dumpEvents()}",
                            onscreen, hasItem(laidOutPage))
                    }
                }
            }
        }
        // Verify that laid out pages don't change after the last scroll event
        assertThat("The last OnChildViewAdded should be before an OnPageScrolledEvent. " +
                "Events: ${recorder.dumpEvents()}",
            recorder.lastAddedIx, lessThan(recorder.lastScrolledIx))
        assertThat("The last OnChildViewRemoved should be before an OnPageScrolledEvent. " +
                "Events: ${recorder.dumpEvents()}",
            recorder.lastRemovedIx, lessThan(recorder.lastScrolledIx))
    }

    private fun ViewPager2.addNewRecordingCallback(): RecordingCallback {
        return RecordingCallback().also {
            registerOnPageChangeCallback(it)
            recyclerView.setOnHierarchyChangeListener(it)
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
        data class OnChildViewAdded(val position: Int) : Event()
        data class OnChildViewRemoved(val position: Int) : Event()
    }

    private class RecordingCallback : ViewPager2.OnPageChangeCallback(),
        ViewGroup.OnHierarchyChangeListener {
        val events = mutableListOf<Event>()

        val lastAddedIx get() = events.indexOfLast { it is OnChildViewAdded }
        val lastRemovedIx get() = events.indexOfLast { it is OnChildViewRemoved }
        val lastScrolledIx get() = events.indexOfLast { it is OnPageScrolledEvent }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            synchronized(events) {
                events.add(OnPageScrolledEvent(position, positionOffset, positionOffsetPixels))
            }
        }

        override fun onPageSelected(position: Int) {
            synchronized(events) {
                events.add(OnPageSelectedEvent(position))
            }
        }

        override fun onPageScrollStateChanged(state: Int) {
            synchronized(events) {
                events.add(OnPageScrollStateChangedEvent(state))
            }
        }

        override fun onChildViewAdded(parent: View, child: View) {
            synchronized(events) {
                events.add(OnChildViewAdded(
                    (parent as RecyclerView).getChildAdapterPosition(child)
                ))
            }
        }

        override fun onChildViewRemoved(parent: View, child: View) {
            synchronized(events) {
                events.add(OnChildViewRemoved(
                    (parent as RecyclerView).getChildAdapterPosition(child)
                ))
            }
        }

        fun dumpEvents(): String {
            return events.joinToString("\n- ", "\n(${scrollStateGlossary()})\n- ")
        }
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(viewAdapterProvider, fragmentAdapterProvider).flatMap { adapterProvider ->
            listOf(false, true).flatMap { rtl ->
                listOf(1, 5).map { offscreenPageLimit ->
                    TestConfig(orientation, rtl, adapterProvider, offscreenPageLimit)
                }
            }
        }
    }
}

// endregion
