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
import androidx.test.filters.MediumTest
import androidx.testutils.TestExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@MediumTest
@RunWith(JUnit4::class)
class PagedListAdapterTest {
    private val mainThread = TestExecutor()
    private val diffThread = TestExecutor()

    private val differConfig = AsyncDifferConfig.Builder(STRING_DIFF_CALLBACK)
        .setBackgroundThreadExecutor(diffThread)
        .build()

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
        @Suppress("UNCHECKED_CAST")
        val listenerLegacy = mock(AsyncPagedListDiffer.PagedListListener::class.java)
                as AsyncPagedListDiffer.PagedListListener<String>
        @Suppress("UNCHECKED_CAST")
        val listener = mock(AsyncPagedListDiffer.PagedListListener::class.java)
                as AsyncPagedListDiffer.PagedListListener<String>

        val adapter = Adapter(listenerLegacy, listener)
        assertEquals(0, adapter.itemCount)
        assertEquals(null, adapter.currentList)
        verifyZeroInteractions(listenerLegacy)
        verifyZeroInteractions(listener)
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

    private fun verifyZeroInteractions(
        legacyListener: AsyncPagedListDiffer.PagedListListener<String>,
        listener: AsyncPagedListDiffer.PagedListListener<String>
    ) {
        verifyZeroInteractions(legacyListener)
        verifyZeroInteractions(listener)
    }

    private fun verifyNoMoreInteractions(
        legacyListener: AsyncPagedListDiffer.PagedListListener<String>,
        listener: AsyncPagedListDiffer.PagedListListener<String>
    ) {
        verifyNoMoreInteractions(legacyListener)
        verifyNoMoreInteractions(listener)
    }

    private fun verifyOnCurrentListChanged(
        legacyListener: AsyncPagedListDiffer.PagedListListener<String>,
        listener: AsyncPagedListDiffer.PagedListListener<String>,
        previousList: PagedList<String>?,
        currentList: PagedList<String>?
    ) {
        verify(legacyListener).onCurrentListChanged(null, currentList)
        verify(listener).onCurrentListChanged(previousList, currentList)
    }

    @Test
    fun callbacks() {
        val callback = mock(Runnable::class.java)

        @Suppress("UNCHECKED_CAST")
        val legacyListener = mock(AsyncPagedListDiffer.PagedListListener::class.java)
                as AsyncPagedListDiffer.PagedListListener<String>
        @Suppress("UNCHECKED_CAST")
        val listener = mock(AsyncPagedListDiffer.PagedListListener::class.java)
                as AsyncPagedListDiffer.PagedListListener<String>

        val adapter = Adapter(legacyListener, listener)

        // first - simple insert
        val first = StringPagedList(2, 2, "a", "b")
        verifyZeroInteractions(legacyListener, listener)
        adapter.submitList(first, callback)
        verifyOnCurrentListChanged(legacyListener, listener, null, first)
        verifyNoMoreInteractions(legacyListener, listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // second - async update
        val second = StringPagedList(2, 2, "c", "d")
        adapter.submitList(second, callback)
        verifyNoMoreInteractions(legacyListener, listener)
        verifyNoMoreInteractions(callback)
        drain()
        verifyOnCurrentListChanged(legacyListener, listener, first, second)

        verifyNoMoreInteractions(legacyListener, listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // third - same list - only triggers callback
        adapter.submitList(second, callback)
        verifyNoMoreInteractions(legacyListener, listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(legacyListener, listener)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // fourth - null
        adapter.submitList(null, callback)
        verifyOnCurrentListChanged(legacyListener, listener, second, null)
        verifyNoMoreInteractions(legacyListener, listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
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
