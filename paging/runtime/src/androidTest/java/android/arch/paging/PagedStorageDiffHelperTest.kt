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

package android.arch.paging

import android.support.test.filters.SmallTest
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.util.ListUpdateCallback
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.verifyZeroInteractions

@SmallTest
@RunWith(JUnit4::class)
class PagedStorageDiffHelperTest {

    @Test
    fun sameListNoUpdates() {
        validateTwoListDiff(
                PagedStorage(5, createPage("a", "b", "c"), 5),
                PagedStorage(5, createPage("a", "b", "c"), 5)) {
            verifyZeroInteractions(it)
        }
    }

    @Test
    fun sameListNoUpdatesPlaceholder() {
        val storageNoPlaceholder = PagedStorage(0, createPage("a", "b", "c"), 10)

        val storageWithPlaceholder = PagedStorage(0, createPage("a", "b", "c"), 10)
        storageWithPlaceholder.allocatePlaceholders(3, 0, 3,
                /* ignored */ mock(PagedStorage.Callback::class.java))

        // even though one has placeholders, and null counts are different...
        assertEquals(10, storageNoPlaceholder.trailingNullCount)
        assertEquals(7, storageWithPlaceholder.trailingNullCount)

        // ... should be no interactions, since content still same
        validateTwoListDiff(
                storageNoPlaceholder,
                storageWithPlaceholder) {
            verifyZeroInteractions(it)
        }
    }

    @Test
    fun appendFill() {
        validateTwoListDiff(
                PagedStorage(5, createPage("a", "b"), 5),
                PagedStorage(5, createPage("a", "b", "c"), 4)) {
            verify(it).onRemoved(11, 1)
            verify(it).onInserted(7, 1)
            // NOTE: ideally would be onChanged(7, 1, null)
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun prependFill() {
        validateTwoListDiff(
                PagedStorage(5, createPage("b", "c"), 5),
                PagedStorage(4, createPage("a", "b", "c"), 5)) {
            verify(it).onRemoved(0, 1)
            verify(it).onInserted(4, 1)
            //NOTE: ideally would be onChanged(4, 1, null);
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun change() {
        validateTwoListDiff(
                PagedStorage(5, createPage("a1", "b1", "c1"), 5),
                PagedStorage(5, createPage("a2", "b1", "c2"), 5)) {
            verify(it).onChanged(5, 1, null)
            verify(it).onChanged(7, 1, null)
            verifyNoMoreInteractions(it)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                // first char means same item
                return oldItem[0] == newItem[0]
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        private fun createPage(vararg items: String): Page<Int, String> {
            return Page(items.toList())
        }

        private fun validateTwoListDiff(oldList: PagedStorage<*, String>,
                                        newList: PagedStorage<*, String>,
                                        validator: (callback: ListUpdateCallback) -> Unit) {
            val diffResult = PagedStorageDiffHelper.computeDiff(
                    oldList, newList, DIFF_CALLBACK)

            val listUpdateCallback = mock(ListUpdateCallback::class.java)
            PagedStorageDiffHelper.dispatchDiff(listUpdateCallback, oldList, newList, diffResult)

            validator(listUpdateCallback)
        }
    }
}
