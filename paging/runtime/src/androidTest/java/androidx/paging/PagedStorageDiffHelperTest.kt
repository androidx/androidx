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

package androidx.paging

import androidx.test.filters.SmallTest
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
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
class PagedStorageDiffHelperTest {

    @Test
    fun sameListNoUpdates() {
        validateTwoListDiff(
                PagedStorage(5, listOf("a", "b", "c"), 5),
                PagedStorage(5, listOf("a", "b", "c"), 5)) {
            verifyZeroInteractions(it)
        }
    }

    @Test
    fun appendFill() {
        validateTwoListDiff(
                PagedStorage(5, listOf("a", "b"), 5),
                PagedStorage(5, listOf("a", "b", "c"), 4)) {
            verify(it).onRemoved(11, 1)
            verify(it).onInserted(7, 1)
            // NOTE: ideally would be onChanged(7, 1, null)
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun prependFill() {
        validateTwoListDiff(
                PagedStorage(5, listOf("b", "c"), 5),
                PagedStorage(4, listOf("a", "b", "c"), 5)) {
            verify(it).onRemoved(0, 1)
            verify(it).onInserted(4, 1)
            // NOTE: ideally would be onChanged(4, 1, null);
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun change() {
        validateTwoListDiff(
                PagedStorage(5, listOf("a1", "b1", "c1"), 5),
                PagedStorage(5, listOf("a2", "b1", "c2"), 5)) {
            verify(it).onChanged(5, 1, null)
            verify(it).onChanged(7, 1, null)
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun move() {
        validateTwoListDiff(
                PagedStorage(5, listOf("a", "b", "c", "d"), 5),
                PagedStorage(5, listOf("a", "b", "d", "c"), 5)) {
            // 7, 8 would also be valid, but below is what DiffUtil outputs
            verify(it).onMoved(8, 7)
            verifyNoMoreInteractions(it)
        }
    }

    @Test
    fun transformAnchorIndex_removal() {
        validateTwoListDiffTransform(
                PagedStorage(5, listOf("a", "b", "c", "d", "e"), 5),
                PagedStorage(5, listOf("a", "d", "e"), 5)) { transformAnchorIndex ->
            // a doesn't move
            assertEquals(5, transformAnchorIndex(5))

            // b / c missing, so impl maps b -> a's position, c -> d's
            assertEquals(5, transformAnchorIndex(6))
            assertEquals(6, transformAnchorIndex(7))

            // d / e move forward
            assertEquals(6, transformAnchorIndex(8))
            assertEquals(7, transformAnchorIndex(9))
        }
    }

    @Test
    fun transformAnchorIndex_insert() {
        validateTwoListDiffTransform(
                PagedStorage(5, listOf("a", "d", "e"), 5),
                PagedStorage(5, listOf("a", "b", "c", "d", "e"), 5)) { transformAnchorIndex ->
            // a doesn't move
            assertEquals(5, transformAnchorIndex(5))

            // d / e move back
            assertEquals(8, transformAnchorIndex(6))
            assertEquals(9, transformAnchorIndex(7))
        }
    }

    @Test
    fun transformAnchorIndex_move() {
        validateTwoListDiffTransform(
                PagedStorage(5, listOf("a", "d", "e", "b", "c"), 5),
                PagedStorage(5, listOf("a", "b", "c", "d", "e"), 5)) { transformAnchorIndex ->
            assertEquals(5, transformAnchorIndex(5))
            assertEquals(8, transformAnchorIndex(6))
            assertEquals(9, transformAnchorIndex(7))
            assertEquals(6, transformAnchorIndex(8))
            assertEquals(7, transformAnchorIndex(9))
        }
    }

    @Test
    fun transformAnchorIndex_allMissing() {
        validateTwoListDiffTransform(
                PagedStorage(5, listOf("a", "d", "e", "b", "c"), 5),
                PagedStorage(5, listOf("f", "g", "h", "i", "j"), 5)) { transformAnchorIndex ->
            assertEquals(5, transformAnchorIndex(5))
            assertEquals(6, transformAnchorIndex(6))
            assertEquals(7, transformAnchorIndex(7))
            assertEquals(8, transformAnchorIndex(8))
            assertEquals(9, transformAnchorIndex(9))
        }
    }

    @Test
    fun transformAnchorIndex_offset() {
        validateTwoListDiffTransform(
                PagedStorage(5, listOf("a"), 6),
                PagedStorage(7, listOf("a"), 8)) { transformAnchorIndex ->
            assertEquals(7, transformAnchorIndex(5))
        }
    }

    @Test
    fun transformAnchorIndex_nullBehavior() {
        validateTwoListDiffTransform(
                PagedStorage(3, listOf("a"), 4),
                PagedStorage(1, listOf("a"), 2)) { transformAnchorIndex ->
            // null, so map to same position in new list
            assertEquals(0, transformAnchorIndex(0))
            assertEquals(1, transformAnchorIndex(1))
            assertEquals(2, transformAnchorIndex(2))
            // a -> a, handling offsets
            assertEquals(1, transformAnchorIndex(3))
            // for rest, clamp to list size
            assertEquals(3, transformAnchorIndex(4))
            assertEquals(3, transformAnchorIndex(5))
            assertEquals(3, transformAnchorIndex(6))
            assertEquals(3, transformAnchorIndex(7))
            assertEquals(3, transformAnchorIndex(8))
        }
    }

    @Test
    fun transformAnchorIndex_boundaryBehavior() {
        validateTwoListDiffTransform(
                PagedStorage(3, listOf("a"), 4),
                PagedStorage(1, listOf("a"), 2)) { transformAnchorIndex ->
            // shouldn't happen, but to be safe, indices are clamped
            assertEquals(0, transformAnchorIndex(-1))
            assertEquals(3, transformAnchorIndex(100))
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                // first char means same item
                return oldItem[0] == newItem[0]
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }

        private fun validateTwoListDiff(
            oldList: PagedStorage<String>,
            newList: PagedStorage<String>,
            validator: (callback: ListUpdateCallback) -> Unit
        ) {
            val diffResult = oldList.computeDiff(newList, DIFF_CALLBACK)
            val listUpdateCallback = mock(ListUpdateCallback::class.java)
            oldList.dispatchDiff(listUpdateCallback, newList, diffResult)

            validator(listUpdateCallback)
        }
        private fun validateTwoListDiffTransform(
            oldList: PagedStorage<String>,
            newList: PagedStorage<String>,
            validator: (positionMapper: (Int) -> Int) -> Unit
        ) {
            validator {
                oldList.transformAnchorIndex(
                        oldList.computeDiff(newList, DIFF_CALLBACK),
                        newList,
                        it)
            }
        }
    }
}
