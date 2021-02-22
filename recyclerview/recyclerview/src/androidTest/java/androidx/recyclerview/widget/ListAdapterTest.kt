/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget

import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.TestExecutor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions
import java.util.Collections.emptyList

@MediumTest
@RunWith(AndroidJUnit4::class)
class ListAdapterTest {
    private val mainThread = TestExecutor()
    private val diffThread = TestExecutor()

    private val differConfig = AsyncDifferConfig.Builder(STRING_DIFF_CALLBACK)
        .setBackgroundThreadExecutor(diffThread)
        .build()

    inner class Adapter(
        private val onChanged: AsyncListDiffer.ListListener<String>? = null

    ) : ListAdapter<String, RecyclerView.ViewHolder>(differConfig) {
        init {
            mDiffer.mMainThreadExecutor = mainThread
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            throw IllegalStateException("not supported")
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            throw IllegalStateException("not supported")
        }

        override fun onCurrentListChanged(previousList: List<String>, currentList: List<String>) {
            onChanged?.onCurrentListChanged(previousList, currentList)
        }

        fun getItemPublic(position: Int): String? {
            return super.getItem(position)
        }
    }

    @Test
    fun initialState() {
        @Suppress("UNCHECKED_CAST")
        val listener = mock(AsyncListDiffer.ListListener::class.java)
            as AsyncListDiffer.ListListener<String>

        val adapter = Adapter(listener)
        assertEquals(0, adapter.itemCount)
        assertEquals(emptyList<String>(), adapter.currentList)
        verifyZeroInteractions(listener)
    }

    @Test
    fun getItem() {
        val adapter = Adapter()
        adapter.submitList(listOf("a", "b"))
        assertEquals("a", adapter.getItemPublic(0))
        assertEquals("b", adapter.getItemPublic(1))
    }

    @Test
    fun getItemCount() {
        val adapter = Adapter()
        assertEquals(0, adapter.itemCount)
        adapter.submitList(listOf("a", "b"))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun getCurrentList() {
        val adapter = Adapter()
        val list = listOf("a", "b")

        assertEquals(emptyList<String>(), adapter.currentList)
        adapter.submitList(list)
        assertEquals(list, adapter.currentList)
    }

    @Test
    fun callbacks() {
        val callback = mock(Runnable::class.java)
        @Suppress("UNCHECKED_CAST")
        val listener = mock(AsyncListDiffer.ListListener::class.java)
            as AsyncListDiffer.ListListener<String>

        val adapter = Adapter(listener)

        // first - simple insert
        val first = listOf("a", "b")
        verifyZeroInteractions(listener)
        adapter.submitList(first, callback)
        verify(listener).onCurrentListChanged(emptyList<String>(), first)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // second - async update
        val second = listOf("c", "d")
        adapter.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        drain()
        verify(listener).onCurrentListChanged(first, second)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        reset(callback)

        // third - same list - only triggers callback
        adapter.submitList(second, callback)
        verifyNoMoreInteractions(listener)
        verify(callback).run()
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(listener)
        verifyNoMoreInteractions(callback)
        reset(callback)

        // fourth - null
        adapter.submitList(null, callback)
        verify(listener).onCurrentListChanged(second, emptyList<String>())
        verifyNoMoreInteractions(listener)
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
