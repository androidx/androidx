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

import androidx.paging.DiffingChangePayload.ITEM_TO_PLACEHOLDER
import androidx.paging.DiffingChangePayload.PLACEHOLDER_POSITION_CHANGE
import androidx.paging.DiffingChangePayload.PLACEHOLDER_TO_ITEM
import androidx.paging.ListUpdateCallbackFake.OnChangedEvent
import androidx.paging.ListUpdateCallbackFake.OnInsertedEvent
import androidx.paging.ListUpdateCallbackFake.OnMovedEvent
import androidx.paging.ListUpdateCallbackFake.OnRemovedEvent
import androidx.recyclerview.widget.DiffUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NullPaddedListDiffHelperTest {
    class Storage(
        override val placeholdersBefore: Int,
        private val data: List<String>,
        override val placeholdersAfter: Int
    ) : NullPaddedList<String> {
        override fun getFromStorage(localIndex: Int): String = data[localIndex]
        override val size: Int
            get() = placeholdersBefore + data.size + placeholdersAfter
        override val storageCount: Int
            get() = data.size
    }

    @Test
    fun sameListNoUpdates() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c"), 5),
            Storage(5, listOf("a", "b", "c"), 5)
        ) {
            assertEquals(0, it.interactions)
        }
    }

    @Test
    fun appendFill() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b"), 5),
            Storage(5, listOf("a", "b", "c"), 4)
        ) {
            assertEquals(
                OnChangedEvent(7, 1, PLACEHOLDER_TO_ITEM),
                it.onChangedEvents[0]
            )
            assertEquals(1, it.interactions)
        }
    }

    @Test
    fun appendUnload() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c"), 4),
            Storage(5, listOf("a", "b"), 5),
        ) {
            assertEquals(
                OnChangedEvent(7, 1, ITEM_TO_PLACEHOLDER),
                it.onChangedEvents[0]
            )
            assertEquals(1, it.interactions)
        }
    }

    @Test
    fun prependFill() {
        validateTwoListDiff(
            Storage(5, listOf("b", "c"), 5),
            Storage(4, listOf("a", "b", "c"), 5)
        ) {
            assertEquals(
                OnChangedEvent(4, 1, PLACEHOLDER_TO_ITEM),
                it.onChangedEvents[0]
            )
            assertEquals(1, it.interactions)
        }
    }

    @Test
    fun prependUnload() {
        validateTwoListDiff(
            Storage(4, listOf("a", "b", "c"), 5),
            Storage(5, listOf("b", "c"), 5),
        ) {
            assertEquals(
                OnChangedEvent(4, 1, ITEM_TO_PLACEHOLDER),
                it.onChangedEvents[0]
            )
            assertEquals(1, it.interactions)
        }
    }

    @Test
    fun change() {
        validateTwoListDiff(
            Storage(5, listOf("a1", "b1", "c1"), 5),
            Storage(5, listOf("a2", "b1", "c2"), 5)
        ) {
            assertEquals(OnChangedEvent(5, 1, null), it.onChangedEvents[0])
            assertEquals(OnChangedEvent(7, 1, null), it.onChangedEvents[1])
            assertEquals(2, it.interactions)
        }
    }

    @Test
    fun move() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c", "d"), 5),
            Storage(5, listOf("a", "b", "d", "c"), 5)
        ) {
            // 8, 7 would also be valid, but below is what DiffUtil outputs
            assertEquals(OnMovedEvent(7, 8), it.onMovedEvents[0])
            assertEquals(1, it.interactions)
        }
    }

    @Test
    fun moveBeforePlaceholders() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c"), 5),
            Storage(5, listOf("c", "x", "y", "a", "b"), 5)
        ) {
            assertThat(
                it.allEvents
            ).containsExactly(
                // convert 2 placeholders to x,y
                OnChangedEvent(3, 2, PLACEHOLDER_TO_ITEM),
                // move c to before x,y
                OnMovedEvent(7, 3),
                // these placeholders will shift down in the list as we'll re-add 2 placeholders
                OnChangedEvent(0, 3, PLACEHOLDER_POSITION_CHANGE),
                // now we need 2 new placeholders
                OnInsertedEvent(0, 2),
                // all trailing placeholders shifted 2 positions
                OnChangedEvent(10, 5, PLACEHOLDER_POSITION_CHANGE),
            )
        }
    }

    @Test
    fun moveBeforePlaceholders_noPlaceholderShift() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c"), 5),
            Storage(3, listOf("c", "x", "y", "a", "b"), 5)
        ) {
            assertThat(
                it.allEvents
            ).containsExactly(
                OnChangedEvent(3, 2, PLACEHOLDER_TO_ITEM),
                OnMovedEvent(7, 3)
            )
        }
    }

    @Test
    fun moveAfterPlaceholders() {
        validateTwoListDiff(
            Storage(5, listOf("a", "b", "c"), 5),
            Storage(5, listOf("b", "c", "x", "y", "a"), 5)
        ) {
            assertThat(
                it.allEvents
            ).containsExactly(
                // insert x, y as placeholder changes
                OnChangedEvent(8, 2, PLACEHOLDER_TO_ITEM),
                // move a to after x,y
                OnMovedEvent(5, 9),
                // insert new placeholders to the end
                OnInsertedEvent(13, 2)
            )
        }
    }

    @Test
    fun loadedMore_withMorePlaceholdersAfter() {
        validateTwoListDiff(
            Storage(4, listOf("a", "b", "c", "d", "e"), 1),
            Storage(1, listOf("d", "e", "f", "g"), 20)
        ) {
            assertThat(
                it.allEvents
            ).containsExactly(
                // add f to replace the placeholder after e
                OnChangedEvent(9, 1, PLACEHOLDER_TO_ITEM),
                // add g, we don't have a placeholder for it so it is an insertion
                OnInsertedEvent(10, 1),
                // rm a,b,c
                OnRemovedEvent(4, 3),
                // rm 3 unnecessary leading placeholders
                OnRemovedEvent(0, 3),
                // 3rd placeholder moved to pos 0, so dispatch a change for it
                OnChangedEvent(0, 1, PLACEHOLDER_POSITION_CHANGE),
                // add 20 trailing placeholders
                OnInsertedEvent(5, 20)
            )
        }
    }

    @Test
    fun transformAnchorIndex_removal() {
        validateTwoListDiffTransform(
            Storage(5, listOf("a", "b", "c", "d", "e"), 5),
            Storage(5, listOf("a", "d", "e"), 5)
        ) { transformAnchorIndex ->
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
            Storage(5, listOf("a", "d", "e"), 5),
            Storage(5, listOf("a", "b", "c", "d", "e"), 5)
        ) { transformAnchorIndex ->
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
            Storage(5, listOf("a", "d", "e", "b", "c"), 5),
            Storage(5, listOf("a", "b", "c", "d", "e"), 5)
        ) { transformAnchorIndex ->
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
            Storage(5, listOf("a", "d", "e", "b", "c"), 5),
            Storage(5, listOf("f", "g", "h", "i", "j"), 5)
        ) { transformAnchorIndex ->
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
            Storage(5, listOf("a"), 6),
            Storage(7, listOf("a"), 8)
        ) { transformAnchorIndex ->
            assertEquals(7, transformAnchorIndex(5))
        }
    }

    @Test
    fun transformAnchorIndex_nullBehavior() {
        validateTwoListDiffTransform(
            Storage(3, listOf("a"), 4),
            Storage(1, listOf("a"), 2)
        ) { transformAnchorIndex ->
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
            Storage(3, listOf("a"), 4),
            Storage(1, listOf("a"), 2)
        ) { transformAnchorIndex ->
            // shouldn't happen, but to be safe, indices are clamped
            assertEquals(0, transformAnchorIndex(-1))
            assertEquals(3, transformAnchorIndex(100))
        }
    }

    @Test
    fun distinct_jumpToTop() {
        validateTwoListDiff(
            Storage(4, listOf("c_4", "4", "d_5", "5", "e_6", "6"), 3),
            Storage(0, listOf("a_0", "0", "b_1", "1"), 8)
        ) {
            assertThat(it.allEvents).containsExactly(
                // replace previous items with placeholders
                OnChangedEvent(4, 6, ITEM_TO_PLACEHOLDER),
                // swap first 4 placeholders with newly loaded items
                OnChangedEvent(0, 4, PLACEHOLDER_TO_ITEM),
                // remove extra placeholder
                OnRemovedEvent(12, 1)
            ).inOrder()
        }
    }

    @Test
    fun distinct_jumpToBottom() {
        validateTwoListDiff(
            Storage(4, listOf("a_4", "4", "b_5", "5", "c_6", "6"), 3),
            Storage(8, listOf("d_8", "8", "e_9", "9"), 0),
        ) {
            assertThat(it.allEvents).containsExactly(
                // remove c_6 and 6, their positions overlap w/ newly loaded items
                OnRemovedEvent(8, 2),
                // now insert d_8 and 8
                OnInsertedEvent(8, 2),
                // first 4 of the loaded items becomes placeholders: "a_4", "4", "b_5", "5"
                OnChangedEvent(4, 4, ITEM_TO_PLACEHOLDER),
                // insert e_9 and 9 using placeholders
                OnChangedEvent(10, 2, PLACEHOLDER_TO_ITEM),
                // finally, remove the last placeholder that we won't use
                OnRemovedEvent(12, 1)
            )
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
            oldList: Storage,
            newList: Storage,
            validator: (callback: ListUpdateCallbackFake) -> Unit
        ) {
            val diffResult = oldList.computeDiff(newList, DIFF_CALLBACK)
            val listUpdateCallback = ListUpdateCallbackFake()
            oldList.dispatchDiff(listUpdateCallback, newList, diffResult)

            validator(listUpdateCallback)
        }

        private fun validateTwoListDiffTransform(
            oldList: Storage,
            newList: Storage,
            validator: (positionMapper: (Int) -> Int) -> Unit
        ) {
            validator {
                oldList.transformAnchorIndex(
                    oldList.computeDiff(newList, DIFF_CALLBACK),
                    newList,
                    it
                )
            }
        }
    }
}
