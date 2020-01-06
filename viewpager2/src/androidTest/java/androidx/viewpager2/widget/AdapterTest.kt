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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.AdapterTest.Event.OnPageScrollStateChangedEvent
import androidx.viewpager2.widget.AdapterTest.Event.OnPageScrolledEvent
import androidx.viewpager2.widget.AdapterTest.Event.OnPageSelectedEvent
import androidx.viewpager2.widget.BaseTest.Context.SwipeMethod
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit.SECONDS

@LargeTest
@RunWith(AndroidJUnit4::class)
class AdapterTest : BaseTest() {
    private val pageCount = 5

    private lateinit var test: Context
    private lateinit var dataSet: MutableList<String>

    override fun setUp() {
        super.setUp()
        test = setUpTest(ViewPager2.ORIENTATION_HORIZONTAL)
    }

    @Test
    fun test_setAdapter() {
        val recorder = test.viewPager.addNewRecordingCallback()
        test.setAdapterSync(viewAdapterProvider.provider(stringSequence(pageCount)))
        test.assertBasicState(0)
        test.viewPager.setCurrentItemSync(1, false, 2, SECONDS)
        test.assertBasicState(1)
        test.runOnUiThreadSync {
            test.viewPager.adapter = test.viewPager.adapter
        }
        test.assertBasicState(0)

        assertThat(recorder.allEvents, equalTo(
            expectedEventsForPage(0) // for setting the adapter
                .plus(expectedEventsForPage(1)) // for going to page 1
                .plus(expectedEventsForPage(0)) // for setting it again
        ))
    }

    private fun setUpWithoutAdapter() {
        assertThat(test.viewPager.adapter, nullValue())
        assertThat(test.viewPager.currentItem, equalTo(0))
    }

    private fun setUpWithEmptyAdapter() {
        setUpAdapterSync(0)
        assertThat(test.viewPager.adapter, notNullValue())
        assertThat(test.viewPager.currentItem, equalTo(0))
    }

    @Test
    fun test_setCurrentItemNullAdapter() {
        test_setCurrentItemWithoutContent(this::setUpWithoutAdapter)
    }

    @Test
    fun test_setCurrentItemEmptyAdapter() {
        test_setCurrentItemWithoutContent(this::setUpWithEmptyAdapter)
    }

    private fun test_setCurrentItemWithoutContent(setUp: () -> Unit) {
        setUp()
        listOf(-1, 0, 1, 10).forEach { targetPage ->
            // given
            val recorder = test.viewPager.addNewRecordingCallback()

            // when
            test.viewPager.setCurrentItemSync(targetPage, false, 2, SECONDS, false)

            // then
            assertThat(test.viewPager.currentItem, equalTo(0))
            assertThat(recorder.allEvents, equalTo(emptyList()))
            test.viewPager.unregisterOnPageChangeCallback(recorder)
        }
    }

    @Test
    fun test_swipeNullAdapter() {
        test_swipeWithoutContent(this::setUpWithoutAdapter)
    }

    @Test
    fun test_swipeEmptyAdapter() {
        test_swipeWithoutContent(this::setUpWithEmptyAdapter)
    }

    private fun test_swipeWithoutContent(setUp: () -> Unit) {
        setUp()
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

    @Test
    fun test_removeAllLookingAt0() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount)
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            expectedEventsForPage(0) // for setting the adapter
                .plus(expectedEventsForPage(0)) // for clearing it
        ))
    }

    @Test
    fun test_removeAllLookingAt1() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount, initialPage = 1)
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            expectedEventsForPage(0) // for setting the adapter
                .plus(expectedEventsForPage(1)) // for going to page 1
                .plus(expectedEventsForPage(0)) // for clearing it
        ))
    }

    @Test
    fun test_addItemsWhileEmpty() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(0)
        fillDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            expectedEventsForPage(0) // for populating the adapter
        ))
    }

    @Test
    fun test_removeAllAddAllRemoveAgain() {
        val recorder = test.viewPager.addNewRecordingCallback()
        setUpAdapterSync(pageCount)

        clearDataSet()
        fillDataSet()
        clearDataSet()

        // check events
        assertThat(recorder.allEvents, equalTo(
            expectedEventsForPage(0) // for setting the adapter
                .plus(expectedEventsForPage(0)) // for clearing it
                .plus(expectedEventsForPage(0)) // for repopulating it
                .plus(expectedEventsForPage(0)) // for clearing it again
        ))
    }

    private fun expectedEventsForPage(page: Int): List<Event> {
        return listOf(
            OnPageSelectedEvent(page),
            OnPageScrolledEvent(page, 0f, 0)
        )
    }

    private fun setUpAdapterSync(pageCount: Int, initialPage: Int? = null) {
        dataSet = stringSequence(pageCount).toMutableList()
        test.setAdapterSync(viewAdapterProvider.provider(dataSet))

        if (initialPage != null) {
            test.viewPager.setCurrentItemSync(initialPage, false, 2, SECONDS)
        }

        val expectedPosition = initialPage ?: 0
        val expectedText = if (pageCount == 0) null else "$expectedPosition"
        test.assertBasicState(expectedPosition, expectedText)
    }

    private fun clearDataSet() {
        assertThat(dataSet.size, greaterThan(0))
        test.modifyDataSetSync {
            val itemCount = dataSet.size
            dataSet.clear()
            test.viewPager.adapter!!.notifyItemRangeRemoved(0, itemCount)
        }
        test.assertBasicState(0, null)
    }

    private fun fillDataSet() {
        assertThat(dataSet.size, equalTo(0))
        test.modifyDataSetSync {
            dataSet.addAll(stringSequence(pageCount))
            test.viewPager.adapter!!.notifyItemRangeInserted(0, pageCount)
        }
        test.assertBasicState(0)
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

        val allEvents get() = events.toList()

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
}
