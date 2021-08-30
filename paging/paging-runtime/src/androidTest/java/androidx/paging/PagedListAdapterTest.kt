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

package androidx.paging

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PagedListAdapterTest {
    private val mainThread = TestExecutor()
    private val diffThread = TestExecutor()

    private val differConfig = AsyncDifferConfig.Builder(STRING_DIFF_CALLBACK)
        .setBackgroundThreadExecutor(diffThread)
        .build()

    @Suppress("DEPRECATION")
    inner class Adapter(
        private val onChangedLegacy: AsyncPagedListDiffer.PagedListListener<String>? = null,
        private val onChanged: AsyncPagedListDiffer.PagedListListener<String>? = null
    ) : PagedListAdapter<String, RecyclerView.ViewHolder>(differConfig) {
        init {
            differ.mainThreadExecutor = mainThread
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            throw IllegalStateException("not supported")

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            throw IllegalStateException("not supported")

        @Suppress("OverridingDeprecatedMember")
        override fun onCurrentListChanged(currentList: PagedList<String>?) {
            onChangedLegacy?.onCurrentListChanged(null, currentList)
        }

        override fun onCurrentListChanged(
            previousList: PagedList<String>?,
            currentList: PagedList<String>?
        ) {
            onChanged?.onCurrentListChanged(previousList, currentList)
        }

        fun getItemPublic(position: Int): String? {
            return super.getItem(position)
        }
    }

    @Test
    fun initialState() {
        val listenerLegacy = PagedListListenerFake<String>()
        val listener = PagedListListenerFake<String>()

        val adapter = Adapter(listenerLegacy, listener)
        assertEquals(0, adapter.itemCount)
        assertEquals(null, adapter.currentList)

        assertEquals(0, listenerLegacy.onCurrentListChangedEvents.size)
        assertEquals(0, listener.onCurrentListChangedEvents.size)
    }

    @Test
    fun getItem() {
        val adapter = Adapter()
        adapter.submitList(StringPagedList(1, 0, "a"))
        assertEquals(null, adapter.getItemPublic(0))
        assertEquals("a", adapter.getItemPublic(1))
    }

    @Test
    fun getItemCount() {
        val adapter = Adapter()
        assertEquals(0, adapter.itemCount)
        adapter.submitList(StringPagedList(1, 0, "a"))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun getCurrentList() {
        val adapter = Adapter()
        val list = StringPagedList(1, 0, "a")

        assertEquals(null, adapter.currentList)
        adapter.submitList(list)
        assertSame(list, adapter.currentList)
    }

    @Test
    fun callbacks() {
        val callback = RunnableFake()
        val legacyListener = PagedListListenerFake<String>()
        val listener = PagedListListenerFake<String>()

        val adapter = Adapter(legacyListener, listener)

        // first - simple insert
        val first = StringPagedList(2, 2, "a", "b")

        // Assert no interactions with listeners
        assertEquals(0, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(0, listener.onCurrentListChangedEvents.size)

        adapter.submitList(first, callback)

        // Assert exactly 1 call to onCurrentListChanged with previousList = null,
        // currentList = first.
        assertEquals(1, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent(null, first),
            legacyListener.onCurrentListChangedEvents[0]
        )
        assertEquals(1, listener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent(null, first),
            listener.onCurrentListChangedEvents[0]
        )
        // Assert exactly 1 call to callback.run().
        assertEquals(1, callback.runEvents.size)

        // second - async update
        val second = StringPagedList(2, 2, "c", "d")
        adapter.submitList(second, callback)

        // Assert no calls to onCurrentListChanged until async work is triggered by drain().
        assertEquals(1, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(1, listener.onCurrentListChangedEvents.size)
        // Assert no calls to callback.run() until async work is triggered by drain().
        assertEquals(1, callback.runEvents.size)

        drain()

        // Assert exactly 1 call to onCurrentListChanged with previousList = first,
        // currentList = second.
        assertEquals(2, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent(null, second),
            legacyListener.onCurrentListChangedEvents[1]
        )
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent(first, second),
            listener.onCurrentListChangedEvents[1]
        )
        // Assert exactly 1 call to callback.run().
        assertEquals(2, callback.runEvents.size)

        // third - same list - only triggers callback
        adapter.submitList(second, callback)

        // Assert no calls to onCurrentListChanged.
        assertEquals(2, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        // Assert exactly 1 call to callback.run().
        assertEquals(3, callback.runEvents.size)

        drain()

        // Assert no calls to onCurrentListChanged.
        assertEquals(2, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(2, listener.onCurrentListChangedEvents.size)
        // Assert no calls to callback.run().
        assertEquals(3, callback.runEvents.size)

        // fourth - null
        adapter.submitList(null, callback)

        // Assert exactly 1 call to onCurrentListChanged with previousList = second,
        // currentList = null.
        assertEquals(3, legacyListener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent<String>(null, null),
            legacyListener.onCurrentListChangedEvents[2]
        )
        assertEquals(3, listener.onCurrentListChangedEvents.size)
        assertEquals(
            PagedListListenerFake.OnCurrentListChangedEvent(second, null),
            listener.onCurrentListChangedEvents[2]
        )
        // Assert exactly 1 call to callback.run().
        assertEquals(4, callback.runEvents.size)
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = diffThread.executeAll()
            executed = mainThread.executeAll() or executed
        } while (executed)
    }

    companion object {
        private val STRING_DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}
