/*
 * Copyright 2021 The Android Open Source Project
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

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewGroup
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * For some tests, this test uses a real recyclerview with a real adapter to serve as an
 * integration test so that we can validate all updates and state restorations after updates.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NullPaddedListDiffWithRecyclerViewTest {
    private lateinit var context: Context
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NullPaddedListAdapter

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        recyclerView = RecyclerView(
            context
        ).also {
            it.layoutManager = LinearLayoutManager(context)
            it.itemAnimator = null
        }
        adapter = NullPaddedListAdapter()
        recyclerView.adapter = adapter
    }

    // this is no different that init but reads better in tests to have a reset method
    private fun reset() {
        init()
    }

    private fun measureAndLayout() {
        recyclerView.measure(EXACTLY or 100, EXACTLY or RV_HEIGHT)
        recyclerView.layout(0, 0, 100, RV_HEIGHT)
    }

    @Test
    fun basic() {
        val storage = NullPaddedStorage(
            placeholdersBefore = 0,
            data = createItems(0, 10),
            placeholdersAfter = 0
        )
        adapter.setItems(storage)
        measureAndLayout()
        val snapshot = captureUISnapshot()
        assertThat(snapshot).containsExactlyElementsIn(
            createExpectedSnapshot(
                firstItemTopOffset = 0,
                startItemIndex = 0,
                backingList = storage
            )
        )
    }

    @Test
    fun distinctLists_fullyOverlappingRange() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 8),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 100, count = 8),
            placeholdersAfter = 30
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_loadedBefore_or_After() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 10),
            placeholdersAfter = 10
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 5,
            data = createItems(startId = 5, count = 5),
            placeholdersAfter = 20
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post
        )
    }

    @Test
    fun distinctLists_partiallyOverlapping() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 0, count = 8),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 15,
            data = createItems(startId = 100, count = 8),
            placeholdersAfter = 30
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_fewerItemsLoaded_withMorePlaceholdersBefore() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 8),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 15,
            data = createItems(startId = 100, count = 3),
            placeholdersAfter = 30
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_noPlaceholdersLeft() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 8),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 0,
            data = createItems(startId = 100, count = 3),
            placeholdersAfter = 0
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_moreItemsLoaded() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 3),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 100, count = 8),
            placeholdersAfter = 30
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_moreItemsLoaded_andAlsoMoreOffset() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(startId = 10, count = 3),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 15,
            data = createItems(startId = 100, count = 8),
            placeholdersAfter = 30
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    @Test
    fun distinctLists_expandShrink() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(10, 10),
            placeholdersAfter = 20
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 0,
            data = createItems(100, 1),
            placeholdersAfter = 0
        )
        distinctListTest_withVariousInitialPositions(
            pre = pre,
            post = post,
        )
    }

    /**
     * Runs a state restoration test with various "current scroll positions".
     */
    private fun distinctListTest_withVariousInitialPositions(
        pre: NullPaddedStorage,
        post: NullPaddedStorage
    ) {
        // try restoring positions in different list states
        val minSize = minOf(pre.size, post.size)
        val lastTestablePosition = (minSize - (RV_HEIGHT / ITEM_HEIGHT)).coerceAtLeast(0)
        (0..lastTestablePosition).forEach { initialPos ->
            distinctListTest(
                pre = pre,
                post = post,
                initialListPos = initialPos,
            )
            reset()
            distinctListTest(
                pre = post, // intentional, we are trying to test going in reverse direction
                post = pre,
                initialListPos = initialPos,
            )
            reset()
        }
    }

    @Test
    fun distinctLists_visibleRangeRemoved() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(10, 10),
            placeholdersAfter = 30
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 0,
            data = createItems(100, 4),
            placeholdersAfter = 20
        )
        swapListTest(
            pre = pre,
            post = post,
            preSwapAction = {
                recyclerView.scrollBy(0, 30 * ITEM_HEIGHT)
            },
            validate = { _, newSnapshot ->
                assertThat(newSnapshot).containsExactlyElementsIn(
                    createExpectedSnapshot(
                        startItemIndex = post.size - RV_HEIGHT / ITEM_HEIGHT,
                        backingList = post
                    )
                )
            }
        )
    }

    @Test
    fun distinctLists_validateDiff() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 10,
            data = createItems(10, 10), // their positions won't be in the new list
            placeholdersAfter = 20
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 0,
            data = createItems(100, 1),
            placeholdersAfter = 0
        )
        updateDiffTest(pre, post)
    }

    @Test
    @LargeTest
    fun random_distinctListTest() {
        // this is a random test but if it fails, the exception will have enough information to
        // create an isolated test
        val rand = Random(System.nanoTime())
        fun randomNullPaddedStorage(startId: Int) = NullPaddedStorage(
            placeholdersBefore = rand.nextInt(0, 20),
            data = createItems(
                startId = startId,
                count = rand.nextInt(0, 20)
            ),
            placeholdersAfter = rand.nextInt(0, 20)
        )
        repeat(RANDOM_TEST_REPEAT_SIZE) {
            updateDiffTest(
                pre = randomNullPaddedStorage(0),
                post = randomNullPaddedStorage(1_000)
            )
        }
    }

    @Test
    fun continuousMatch_1() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 4,
            data = createItems(startId = 0, count = 16),
            placeholdersAfter = 1
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 1,
            data = createItems(startId = 13, count = 4),
            placeholdersAfter = 19
        )
        updateDiffTest(pre, post)
    }

    @Test
    fun continuousMatch_2() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 6,
            data = createItems(startId = 0, count = 9),
            placeholdersAfter = 19
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 14,
            data = createItems(startId = 4, count = 3),
            placeholdersAfter = 11
        )
        updateDiffTest(pre, post)
    }

    @Test
    fun continuousMatch_3() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 11,
            data = createItems(startId = 0, count = 4),
            placeholdersAfter = 6
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 7,
            data = createItems(startId = 0, count = 1),
            placeholdersAfter = 11
        )
        updateDiffTest(pre, post)
    }

    @Test
    fun continuousMatch_4() {
        val pre = NullPaddedStorage(
            placeholdersBefore = 4,
            data = createItems(startId = 0, count = 15),
            placeholdersAfter = 18
        )
        val post = NullPaddedStorage(
            placeholdersBefore = 11,
            data = createItems(startId = 5, count = 17),
            placeholdersAfter = 9
        )
        updateDiffTest(pre, post)
    }

    @Test
    @LargeTest
    fun randomTest_withContinuousMatch() {
        randomContinuousMatchTest(shuffle = false)
    }

    @Test
    @LargeTest
    fun randomTest_withContinuousMatch_withShuffle() {
        randomContinuousMatchTest(shuffle = true)
    }

    /**
     * Tests that if two lists have some overlaps, we dispatch the right diff events.
     * It can also optionally shuffle the lists.
     */
    private fun randomContinuousMatchTest(shuffle: Boolean) {
        // this is a random test but if it fails, the exception will have enough information to
        // create an isolated test
        val rand = Random(System.nanoTime())
        fun randomNullPaddedStorage(startId: Int) = NullPaddedStorage(
            placeholdersBefore = rand.nextInt(0, 20),
            data = createItems(
                startId = startId,
                count = rand.nextInt(0, 20)
            ).let {
                if (shuffle) it.shuffled()
                else it
            },
            placeholdersAfter = rand.nextInt(0, 20)
        )
        repeat(RANDOM_TEST_REPEAT_SIZE) {
            val pre = randomNullPaddedStorage(0)
            val post = randomNullPaddedStorage(
                startId = if (pre.storageCount > 0) {
                    pre.getFromStorage(rand.nextInt(pre.storageCount)).id
                } else {
                    0
                }
            )
            updateDiffTest(
                pre = pre,
                post = post
            )
        }
    }

    /**
     * Validates that the update events between [pre] and [post] are correct.
     */
    private fun updateDiffTest(
        pre: NullPaddedStorage,
        post: NullPaddedStorage
    ) {
        val callback = ValidatingListUpdateCallback(pre, post)
        val diffResult = pre.computeDiff(post, NullPaddedListItem.CALLBACK)
        pre.dispatchDiff(callback, post, diffResult)
        callback.validateRunningListAgainst()
    }

    private fun distinctListTest(
        pre: NullPaddedStorage,
        post: NullPaddedStorage,
        initialListPos: Int,
        finalListPos: Int = initialListPos
    ) {
        // try with various initial list positioning.
        // in every case, we should preserve our position
        swapListTest(
            pre = pre,
            post = post,
            preSwapAction = {
                recyclerView.scrollBy(
                    0,
                    initialListPos * ITEM_HEIGHT
                )
            },
            validate = { _, snapshot ->
                assertWithMessage(
                    """
                    initial pos: $initialListPos
                    expected final pos: $finalListPos
                    pre: $pre
                    post: $post
                    """.trimIndent()
                ).that(snapshot).containsExactlyElementsIn(
                    createExpectedSnapshot(
                        startItemIndex = finalListPos,
                        backingList = post
                    )
                )
            }
        )
    }

    /**
     * Helper function to run tests where we submit the [pre] list, run [preSwapAction] (where it
     * can scroll etc) then submit [post] list, run [postSwapAction] and then call [validate]
     * with UI snapshots.
     */
    private fun swapListTest(
        pre: NullPaddedStorage,
        post: NullPaddedStorage,
        preSwapAction: () -> Unit = {},
        postSwapAction: () -> Unit = {},
        validate: (preCapture: List<UIItemSnapshot>, postCapture: List<UIItemSnapshot>) -> Unit
    ) {
        adapter.setItems(pre)
        measureAndLayout()
        preSwapAction()
        val preSnapshot = captureUISnapshot()
        adapter.setItems(post)
        postSwapAction()
        measureAndLayout()
        val postSnapshot = captureUISnapshot()
        validate(preSnapshot, postSnapshot)
    }

    /**
     * Captures positions and data of each visible item in the RecyclerView.
     */
    private fun captureUISnapshot(): List<UIItemSnapshot> {
        return (0 until recyclerView.childCount).mapNotNull { childPos ->
            val view = recyclerView.getChildAt(childPos)!!
            if (view.top < RV_HEIGHT && view.bottom > 0) {
                val viewHolder = recyclerView.getChildViewHolder(view) as NullPaddedListViewHolder
                UIItemSnapshot(
                    top = view.top,
                    boundItem = viewHolder.boundItem,
                    boundPos = viewHolder.boundPos
                )
            } else {
                null
            }
        }
    }

    /**
     * Custom adapter class that also validates its update events to ensure they are correct.
     */
    private class NullPaddedListAdapter : RecyclerView.Adapter<NullPaddedListViewHolder>() {
        private var items: NullPaddedList<NullPaddedListItem>? = null

        fun setItems(items: NullPaddedList<NullPaddedListItem>) {
            val previousItems = this.items
            val myItems = this.items
            if (myItems == null) {
                notifyItemRangeInserted(0, items.size)
            } else {
                val diff = myItems.computeDiff(items, NullPaddedListItem.CALLBACK)
                val diffObserver = TrackingAdapterObserver(previousItems, items)
                registerAdapterDataObserver(diffObserver)
                val callback = AdapterListUpdateCallback(this)
                myItems.dispatchDiff(callback, items, diff)
                unregisterAdapterDataObserver(diffObserver)
                diffObserver.validateRunningListAgainst()
            }
            this.items = items
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): NullPaddedListViewHolder {
            return NullPaddedListViewHolder(parent.context).also {
                it.itemView.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    ITEM_HEIGHT
                )
            }
        }

        override fun onBindViewHolder(holder: NullPaddedListViewHolder, position: Int) {
            val item = items?.get(position)
            holder.boundItem = item
            holder.boundPos = position
        }

        override fun getItemCount(): Int {
            return items?.size ?: 0
        }
    }

    private data class NullPaddedListItem(
        val id: Int,
        val value: String
    ) {
        companion object {
            val CALLBACK = object : DiffUtil.ItemCallback<NullPaddedListItem>() {
                override fun areItemsTheSame(
                    oldItem: NullPaddedListItem,
                    newItem: NullPaddedListItem
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: NullPaddedListItem,
                    newItem: NullPaddedListItem
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }
    }

    private class NullPaddedListViewHolder(
        context: Context
    ) : RecyclerView.ViewHolder(View(context)) {
        var boundItem: NullPaddedListItem? = null
        var boundPos: Int = -1
        override fun toString(): String {
            return "VH[$boundPos , $boundItem]"
        }
    }

    private data class UIItemSnapshot(
        // top coordinate of the item
        val top: Int,
        // the item it is bound to, unless it was a placeholder
        val boundItem: NullPaddedListItem?,
        // the position it was bound to
        val boundPos: Int
    )

    private class NullPaddedStorage(
        override val placeholdersBefore: Int,
        private val data: List<NullPaddedListItem>,
        override val placeholdersAfter: Int
    ) : NullPaddedList<NullPaddedListItem> {
        private val stringRepresentation by lazy {
            """
            $placeholdersBefore:${data.size}:$placeholdersAfter
            $data
            """.trimIndent()
        }

        override fun getFromStorage(localIndex: Int): NullPaddedListItem = data[localIndex]

        override val size: Int
            get() = placeholdersBefore + data.size + placeholdersAfter

        override val storageCount: Int
            get() = data.size

        override fun toString() = stringRepresentation
    }

    private fun createItems(
        startId: Int,
        count: Int
    ): List<NullPaddedListItem> {
        return (startId until startId + count).map {
            NullPaddedListItem(
                id = it,
                value = "$it"
            )
        }
    }

    /**
     * Creates an expected UI snapshot based on the given list and scroll position / offset.
     */
    private fun createExpectedSnapshot(
        firstItemTopOffset: Int = 0,
        startItemIndex: Int,
        backingList: NullPaddedList<NullPaddedListItem>
    ): List<UIItemSnapshot> {
        check(firstItemTopOffset <= 0) {
            "first item offset should not be negative"
        }
        var remainingHeight = RV_HEIGHT - firstItemTopOffset
        var pos = startItemIndex
        var top = firstItemTopOffset
        val result = mutableListOf<UIItemSnapshot>()
        while (remainingHeight > 0 && pos < backingList.size) {
            result.add(
                UIItemSnapshot(
                    top = top,
                    boundItem = backingList.get(pos),
                    boundPos = pos
                )
            )
            top += ITEM_HEIGHT
            remainingHeight -= ITEM_HEIGHT
            pos++
        }
        return result
    }

    /**
     * A ListUpdateCallback implementation that tracks all change notifications and then validate
     * that
     * a) changes are correct
     * b) no unnecessary events are dispatched (e.g. dispatching change for an item then removing
     * it)
     */
    private class ValidatingListUpdateCallback<T>(
        previousList: NullPaddedList<T>?,
        private val newList: NullPaddedList<T>
    ) : ListUpdateCallback {
        // used in assertion messages
        val msg = """
                oldList: $previousList
                newList: $newList
        """.trimIndent()

        // all changes are applied to this list, at the end, we'll validate against the new list
        // to ensure all updates made sense and no unnecessary updates are made
        val runningList: MutableList<ListSnapshotItem> =
            previousList?.createSnapshot() ?: mutableListOf()

        private val size: Int
            get() = runningList.size

        private fun Int.assertWithinBounds() {
            assertWithMessage(msg).that(this).isAtLeast(0)
            assertWithMessage(msg).that(this).isAtMost(size)
        }

        override fun onInserted(position: Int, count: Int) {
            position.assertWithinBounds()
            assertWithMessage(msg).that(count).isAtLeast(1)
            repeat(count) {
                runningList.add(position, ListSnapshotItem.Inserted)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            position.assertWithinBounds()
            (position + count).assertWithinBounds()
            assertWithMessage(msg).that(count).isAtLeast(1)
            (position until position + count).forEach { pos ->
                assertWithMessage(
                    "$msg\nshouldn't be removing an item that already got a change event" +
                        " pos: $pos , ${runningList[pos]}"
                )
                    .that(runningList[pos].isOriginalItem())
                    .isTrue()
            }
            repeat(count) {
                runningList.removeAt(position)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            fromPosition.assertWithinBounds()
            toPosition.assertWithinBounds()
            runningList.add(toPosition, runningList.removeAt(fromPosition))
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            position.assertWithinBounds()
            (position + count).assertWithinBounds()
            assertWithMessage(msg).that(count).isAtLeast(1)
            (position until position + count).forEach { pos ->
                // make sure we don't dispatch overlapping updates
                assertWithMessage(
                    "$msg\nunnecessary change event for position $pos $payload " +
                        "${runningList[pos]}"
                )
                    .that(runningList[pos].isOriginalItem())
                    .isTrue()
                if (payload == DiffingChangePayload.PLACEHOLDER_TO_ITEM ||
                    payload == DiffingChangePayload.PLACEHOLDER_POSITION_CHANGE
                ) {
                    assertWithMessage(msg).that(runningList[pos]).isInstanceOf(
                        ListSnapshotItem.Placeholder::class.java
                    )
                } else {
                    assertWithMessage(msg).that(runningList[pos]).isInstanceOf(
                        ListSnapshotItem.Item::class.java
                    )
                }
                runningList[pos] = ListSnapshotItem.Changed(
                    payload = payload as? DiffingChangePayload
                )
            }
        }

        fun validateRunningListAgainst() {
            // check for size first
            assertWithMessage(msg).that(size).isEqualTo(newList.size)
            val newListSnapshot = newList.createSnapshot()
            runningList.forEachIndexed { index, listSnapshotItem ->
                val newListItem = newListSnapshot[index]
                listSnapshotItem.assertReplacement(
                    msg,
                    newListItem
                )
                if (!listSnapshotItem.isOriginalItem()) {
                    // if it changed, replace from new snapshot
                    runningList[index] = newListSnapshot[index]
                }
            }
            // now after this, each list must be exactly equal, if not, something is wrong
            assertWithMessage(msg).that(runningList).containsExactlyElementsIn(newListSnapshot)
        }
    }

    private class TrackingAdapterObserver<T>(
        previousList: NullPaddedList<T>?,
        postList: NullPaddedList<T>
    ) : RecyclerView.AdapterDataObserver() {
        private val callback = ValidatingListUpdateCallback(previousList, postList)

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            callback.onChanged(positionStart, itemCount, null)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            callback.onChanged(positionStart, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            callback.onInserted(positionStart, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            callback.onRemoved(positionStart, itemCount)
        }

        fun validateRunningListAgainst() {
            callback.validateRunningListAgainst()
        }
    }

    companion object {
        private const val RV_HEIGHT = 100
        private const val ITEM_HEIGHT = 10
        private const val RANDOM_TEST_REPEAT_SIZE = 1_000
    }
}

private fun <T> NullPaddedList<T>.get(index: Int): T? {
    if (index < placeholdersBefore) return null
    val storageIndex = index - placeholdersBefore
    if (storageIndex >= storageCount) return null
    return getFromStorage(storageIndex)
}

/**
 * Create a snapshot of this current that can be used to verify diffs.
 */
private fun <T> NullPaddedList<T>.createSnapshot(): MutableList<ListSnapshotItem> = (0 until size)
    .mapTo(mutableListOf()) { pos ->
        get(pos)?.let {
            ListSnapshotItem.Item(it)
        } ?: ListSnapshotItem.Placeholder(pos)
    }

/**
 * Sealed classes to identify items in the list.
 */
internal sealed class ListSnapshotItem {
    // means the item didn't change at all in diffs.
    fun isOriginalItem() = this is Item<*> || this is Placeholder

    /**
     * Asserts that this item properly represents the replacement (newListItem).
     */
    abstract fun assertReplacement(
        msg: String,
        newListItem: ListSnapshotItem
    )

    data class Item<T>(val item: T) : ListSnapshotItem() {
        override fun assertReplacement(
            msg: String,
            newListItem: ListSnapshotItem
        ) {
            // no change
            assertWithMessage(msg).that(
                newListItem
            ).isEqualTo(this)
        }
    }

    data class Placeholder(val pos: Int) : ListSnapshotItem() {
        override fun assertReplacement(
            msg: String,
            newListItem: ListSnapshotItem
        ) {
            assertWithMessage(msg).that(
                newListItem
            ).isInstanceOf(
                Placeholder::class.java
            )
            val replacement = newListItem as Placeholder
            // make sure position didn't change. If it did, we would be replaced with a [Changed].
            assertWithMessage(msg).that(
                pos
            ).isEqualTo(replacement.pos)
        }
    }

    /**
     * Inserted into the list when we receive a change notification about an item/placeholder.
     */
    data class Changed(val payload: DiffingChangePayload?) : ListSnapshotItem() {
        override fun assertReplacement(
            msg: String,
            newListItem: ListSnapshotItem
        ) {
            // there are 4 cases for changes.
            // is either placeholder -> placeholder with new position
            // placeholder to item
            // item to placeholder
            // item change from original diffing.
            when (payload) {
                DiffingChangePayload.ITEM_TO_PLACEHOLDER -> {
                    assertWithMessage(msg).that(newListItem)
                        .isInstanceOf(Placeholder::class.java)
                }
                DiffingChangePayload.PLACEHOLDER_TO_ITEM -> {
                    assertWithMessage(msg).that(newListItem)
                        .isInstanceOf(Item::class.java)
                }
                DiffingChangePayload.PLACEHOLDER_POSITION_CHANGE -> {
                    assertWithMessage(msg).that(newListItem)
                        .isInstanceOf(Placeholder::class.java)
                }
                else -> {
                    // item change that came from diffing.
                    assertWithMessage(msg).that(newListItem)
                        .isInstanceOf(Item::class.java)
                }
            }
        }
    }

    /**
     * Used when an item/placeholder is inserted to the list
     */
    object Inserted : ListSnapshotItem() {
        override fun assertReplacement(msg: String, newListItem: ListSnapshotItem) {
            // nothing to assert here, it can represent anything in the new list.
        }
    }
}