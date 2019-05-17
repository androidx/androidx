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

import androidx.recyclerview.widget.RecyclerView
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.testutils.waitForExecution
import androidx.viewpager2.test.ui.TouchConsumingTextView
import androidx.viewpager2.widget.DisableUserInputTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.DisableUserInputTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.DisableUserInputTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.DisableUserInputTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.swipe.ViewAdapter
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.math.roundToInt

/**
 * Tests what happens when a smooth scroll is interrupted by a drag
 */
@RunWith(Parameterized::class)
@LargeTest
class DisableUserInputTest(private val config: TestConfig) : BaseTest() {
    data class TestConfig(
        @ViewPager2.Orientation val orientation: Int,
        val childViewConsumesTouches: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }

    private val pageCount = 10
    private val firstPage = 0
    private val middlePage = pageCount / 2
    private val lastPage = pageCount - 1

    private lateinit var test: Context
    private lateinit var adapterProvider: AdapterProvider

    private val touchConsumingViewAdapter: AdapterProviderForItems = { items ->
        {
            object : ViewAdapter(items) {
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    super.onBindViewHolder(holder, position)
                    (holder.itemView as TouchConsumingTextView).consumeTouches =
                            config.childViewConsumesTouches
                }
            }
        }
    }

    override fun setUp() {
        super.setUp()
        adapterProvider = touchConsumingViewAdapter(stringSequence(pageCount))
        test = setUpTest(config.orientation).also {
            it.viewPager.isUserInputEnabled = false
            it.setAdapterSync(adapterProvider)
            it.assertBasicState(firstPage)
        }
    }

    @Test
    @LargeTest
    fun testSwipe() {
        listOf(firstPage, firstPage + 1).forEach { swipeToAndVerifyNothingHappened(it) }
        test.viewPager.setCurrentItemSync(middlePage, false, 2, SECONDS)
        test.assertBasicState(middlePage)
        listOf(middlePage - 1, middlePage + 1).forEach { swipeToAndVerifyNothingHappened(it) }
        test.viewPager.setCurrentItemSync(lastPage, false, 2, SECONDS)
        test.assertBasicState(lastPage)
        listOf(lastPage - 1, lastPage).forEach { swipeToAndVerifyNothingHappened(it) }
    }

    private fun swipeToAndVerifyNothingHappened(targetPage: Int) {
        // given
        val recorder = test.viewPager.addNewRecordingCallback()
        val currentPage = test.viewPager.currentItem

        // when
        test.swipe(currentPage, targetPage)
        test.activityTestRule.waitForExecution(3)

        // then
        test.assertBasicState(currentPage)
        assertThat(recorder.eventCount, equalTo(0))

        test.viewPager.unregisterOnPageChangeCallback(recorder)

        // end with a config change to see if internal state didn't get screwed up
        doConfigChangeAndVerify(currentPage)
    }

    private fun testSetCurrentItem(smoothScroll: Boolean) {
        listOf(1, 9, 7, 0).forEach { targetPage ->
            // given
            val currentPage = test.viewPager.currentItem
            val recorder = test.viewPager.addNewRecordingCallback()

            // when
            test.viewPager.setCurrentItemSync(targetPage, smoothScroll, 2, SECONDS)

            // then
            test.assertBasicState(targetPage)
            val pageSize = test.viewPager.pageSize
            recorder.scrollEvents.assertValueSanity(currentPage, targetPage, pageSize)
            recorder.scrollEvents.assertLastCorrect(targetPage)
            recorder.selectEvents.assertSelected(listOf(targetPage))

            test.viewPager.unregisterOnPageChangeCallback(recorder)

            // end with a config change to see if internal state survives
            doConfigChangeAndVerify(targetPage)
        }
    }

    @Test
    @LargeTest
    fun testSetCurrentItemSmooth() {
        testSetCurrentItem(true)
    }

    @Test
    @MediumTest
    fun testSetCurrentItemNotSmooth() {
        testSetCurrentItem(false)
    }

    private fun doConfigChangeAndVerify(page: Int) {
        test.recreateActivity(adapterProvider)
        test.assertBasicState(page)
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

        val eventCount get() = events.size
        val scrollEvents get() = events.mapNotNull { it as? OnPageScrolledEvent }
        val selectEvents get() = events.mapNotNull { it as? OnPageSelectedEvent }

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

    private fun List<OnPageScrolledEvent>.assertLastCorrect(targetPage: Int) {
        last().apply {
            assertThat(position, equalTo(targetPage))
            assertThat(positionOffsetPixels, equalTo(0))
        }
    }

    private fun List<OnPageSelectedEvent>.assertSelected(pages: List<Int>) {
        assertThat(map { it.position }, equalTo(pages))
    }
}

// region Test Suite creation

private fun createTestSet(): List<TestConfig> {
    return listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).flatMap { orientation ->
        listOf(true, false).map { consumeTouches ->
            TestConfig(
                orientation,
                consumeTouches
            )
        }
    }
}

// endregion
