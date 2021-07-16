/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadStateAdapterTest.AdapterEventRecorder.Event.CHANGE
import androidx.paging.LoadStateAdapterTest.AdapterEventRecorder.Event.INSERT
import androidx.paging.LoadStateAdapterTest.AdapterEventRecorder.Event.REMOVED
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LoadStateAdapterTest {
    class AdapterEventRecorder : RecyclerView.AdapterDataObserver() {
        enum class Event {
            CHANGE, INSERT, REMOVED
        }

        private val observedEvents = mutableListOf<Event>()

        fun getClearEvents(): List<Event> {
            val ret = observedEvents.toList()
            observedEvents.clear()
            return ret
        }

        override fun onChanged() {
            fail("Unexpected full change")
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            assertEquals(0, positionStart)
            assertEquals(1, itemCount)
            observedEvents.add(CHANGE)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            if (payload != null) {
                fail("Unexpected payload")
            } else {
                onItemRangeChanged(positionStart, itemCount)
            }
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            assertEquals(0, positionStart)
            assertEquals(1, itemCount)
            observedEvents.add(INSERT)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            assertEquals(0, positionStart)
            assertEquals(1, itemCount)
            observedEvents.add(REMOVED)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            fail("Unexpected move")
        }
    }

    class SimpleLoadStateAdapter : LoadStateAdapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            loadState: LoadState
        ): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(View(parent.context)) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, loadState: LoadState) {
        }
    }

    @Test
    fun init() {
        val adapter = SimpleLoadStateAdapter()
        assertEquals(0, adapter.itemCount)
        assertFalse(
            adapter.displayLoadStateAsItem(
                NotLoading(endOfPaginationReached = false)
            )
        )
        assertFalse(
            adapter.displayLoadStateAsItem(
                NotLoading(endOfPaginationReached = true)
            )
        )
        assertTrue(adapter.displayLoadStateAsItem(Error(Throwable())))
        assertTrue(adapter.displayLoadStateAsItem(Loading))
    }

    @Test
    fun notifyEquality() {
        val adapter = SimpleLoadStateAdapter()
        adapter.loadState = Loading

        val eventRecorder = AdapterEventRecorder()
        adapter.registerAdapterDataObserver(eventRecorder)

        adapter.loadState = Loading
        assertTrue(eventRecorder.getClearEvents().isEmpty())
    }

    @Test
    fun notifyEqualityError() {
        val throwable1 = Throwable()
        val throwable2 = Throwable()
        assertFalse("sanity check", throwable1 == throwable2)

        val adapter = SimpleLoadStateAdapter()
        adapter.loadState = Error(throwable1)

        val eventRecorder = AdapterEventRecorder()
        adapter.registerAdapterDataObserver(eventRecorder)

        adapter.loadState = Error(throwable2)
        assertEquals(listOf(CHANGE), eventRecorder.getClearEvents())
    }

    @Test
    fun notifyInsertChangeRemove() {
        val adapter = SimpleLoadStateAdapter()

        val eventRecorder = AdapterEventRecorder()
        adapter.registerAdapterDataObserver(eventRecorder)

        // idle, done, nothing should happen
        adapter.loadState = NotLoading(endOfPaginationReached = false)
        assertTrue(eventRecorder.getClearEvents().isEmpty())
        adapter.loadState = NotLoading(endOfPaginationReached = true)
        assertTrue(eventRecorder.getClearEvents().isEmpty())

        // insert item
        adapter.loadState = Loading
        assertEquals(listOf(INSERT), eventRecorder.getClearEvents())

        // change to error
        adapter.loadState = Error(Throwable())
        assertEquals(listOf(CHANGE), eventRecorder.getClearEvents())

        // change to different error
        adapter.loadState = Error(Throwable())
        assertEquals(listOf(CHANGE), eventRecorder.getClearEvents())

        // remove
        adapter.loadState = NotLoading(endOfPaginationReached = true)
        assertEquals(listOf(REMOVED), eventRecorder.getClearEvents())
    }
}