/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v7.recyclerview.extensions

import android.arch.paging.TestExecutor
import android.support.test.filters.SmallTest
import android.support.v7.util.ListUpdateCallback
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@SmallTest
@RunWith(JUnit4::class)
class ListAdapterHelperTest {
    private val mMainThread = TestExecutor()
    private val mBackgroundThread = TestExecutor()

    private fun <T> createHelper(listUpdateCallback: ListUpdateCallback,
                                 diffCallback: DiffCallback<T>): ListAdapterHelper<T> {
        return ListAdapterHelper(listUpdateCallback,
                ListAdapterConfig.Builder<T>()
                        .setDiffCallback(diffCallback)
                        .setMainThreadExecutor(mMainThread)
                        .setBackgroundThreadExecutor(mBackgroundThread)
                        .build())
    }

    @Test
    fun initialState() {
        val callback = mock(ListUpdateCallback::class.java)
        val helper = createHelper(callback, STRING_DIFF_CALLBACK)
        assertEquals(0, helper.itemCount)
        verifyZeroInteractions(callback)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getEmpty() {
        val helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        helper.getItem(0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getNegative() {
        val helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        helper.setList(listOf("a", "b"))
        helper.getItem(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun getPastEnd() {
        val helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK)
        helper.setList(listOf("a", "b"))
        helper.getItem(2)
    }

    @Test
    fun setListSimple() {
        val callback = mock(ListUpdateCallback::class.java)
        val helper = createHelper(callback, STRING_DIFF_CALLBACK)

        helper.setList(listOf("a", "b"))

        assertEquals(2, helper.itemCount)
        assertEquals("a", helper.getItem(0))
        assertEquals("b", helper.getItem(1))

        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun setListUpdate() {
        val callback = mock(ListUpdateCallback::class.java)
        val helper = createHelper(callback, STRING_DIFF_CALLBACK)

        // initial list (immediate)
        helper.setList(listOf("a", "b"))
        verify(callback).onInserted(0, 2)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

        // update (deferred)
        helper.setList(listOf("a", "b", "c"))
        verifyNoMoreInteractions(callback)
        drain()
        verify(callback).onInserted(2, 1)
        verifyNoMoreInteractions(callback)

        // clear (immediate)
        helper.setList(null)
        verify(callback).onRemoved(0, 3)
        verifyNoMoreInteractions(callback)
        drain()
        verifyNoMoreInteractions(callback)

    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mBackgroundThread.executeAll()
            executed = mMainThread.executeAll() or executed
        } while (executed)
    }

    companion object {
        private val STRING_DIFF_CALLBACK = object : DiffCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        private val IGNORE_CALLBACK = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {}

            override fun onRemoved(position: Int, count: Int) {}

            override fun onMoved(fromPosition: Int, toPosition: Int) {}

            override fun onChanged(position: Int, count: Int, payload: Any) {}
        }
    }
}
