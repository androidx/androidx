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

package androidx.recyclerview.widget

import android.content.Context
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter.Config.Builder
import androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS
import androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.NO_STABLE_IDS
import androidx.recyclerview.widget.ConcatAdapter.Config.StableIdMode.SHARED_STABLE_IDS
import androidx.recyclerview.widget.ConcatAdapterSubject.Companion.assertThat
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.Changed
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.DataSetChanged
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.Inserted
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.Moved
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.Removed
import androidx.recyclerview.widget.ConcatAdapterTest.LoggingAdapterObserver.Event.StateRestorationPolicy
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.ALLOW
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import androidx.recyclerview.widget.RecyclerView.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@RunWith(AndroidJUnit4::class)
@SmallTest
class ConcatAdapterTest {
    private lateinit var recyclerView: RecyclerView

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        recyclerView = RecyclerView(
            context
        ).also {
            it.layoutManager = LinearLayoutManager(context)
            it.itemAnimator = null
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun cannotCallSetStableIds_true() {
        val concatenated = ConcatAdapter()
        concatenated.setHasStableIds(true)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun cannotCallSetStableIds_false() {
        val concatenated = ConcatAdapter()
        concatenated.setHasStableIds(false)
    }

    @UiThreadTest
    @Test
    fun attachAndDetachAll() {
        val concatenated = ConcatAdapter()
        val adapter1 = NestedTestAdapter(
            10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 3)
            }
        )
        concatenated.addAdapter(adapter1)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 50)
        assertThat(recyclerView.childCount).isEqualTo(10)
        assertThat(adapter1.attachedViewHolders()).hasSize(10)
        measureAndLayout(100, 0)
        assertThat(recyclerView.childCount).isEqualTo(0)
        assertThat(adapter1.attachedViewHolders()).isEmpty()

        val adapter2 = NestedTestAdapter(
            5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 3)
            }
        )
        concatenated.addAdapter(adapter2)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 200)
        assertThat(recyclerView.childCount).isEqualTo(15)
        assertThat(adapter1.attachedViewHolders()).hasSize(10)
        assertThat(adapter2.attachedViewHolders()).hasSize(5)
        concatenated.removeAdapter(adapter1)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 200)
        assertThat(recyclerView.childCount).isEqualTo(5)
        assertThat(adapter1.attachedViewHolders()).isEmpty()
        assertThat(adapter2.attachedViewHolders()).hasSize(5)
        measureAndLayout(100, 0)
        assertThat(adapter2.attachedViewHolders()).isEmpty()
    }

    @Test
    @UiThreadTest
    fun concatInsideConcat() {
        val concatenated = ConcatAdapter()
        val adapter1 = NestedTestAdapter(10)
        concatenated.addAdapter(adapter1)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(10)
        concatenated.removeAdapter(adapter1)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 100)
        assertThat(adapter1.attachedViewHolders()).isEmpty()
    }

    @UiThreadTest
    @Test
    fun recycleOnRemoval() {
        val concatenated = ConcatAdapter()
        val adapter1 = NestedTestAdapter(10)
        concatenated.addAdapter(adapter1)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(10)
        adapter1.removeItems(3, 2)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 100)
        assertThat(adapter1.recycleEvents()).hasSize(2)
        assertThat(adapter1.attachedViewHolders()).hasSize(8)
        assertThat(adapter1.attachedViewHolders()).containsNoneIn(adapter1.recycleEvents())
    }

    @UiThreadTest
    @Test
    fun checkAttachDetach_adapterAdditions() {
        val concatenated = ConcatAdapter()
        val adapter1 = NestedTestAdapter(0)
        concatenated.addAdapter(adapter1)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        adapter1.addItems(0, 3)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 100)
        assertThat(adapter1.attachedViewHolders()).hasSize(3)
        assertThat(adapter1.recycleEvents()).hasSize(0)
    }

    @UiThreadTest
    @Test
    @SdkSuppress(minSdkVersion = 16)
    fun failedToRecycleTest() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 200)
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(12)
        check(viewHolder != null) {
            "should have that view holder for position 12"
        }
        assertThat(adapter2.attachedViewHolders()).contains(viewHolder)
        // give it transient state so that it won't be recycled
        viewHolder.itemView.setHasTransientState(true)
        adapter2.removeItems(2, 2)
        assertThat(recyclerView.isLayoutRequested).isTrue()
        measureAndLayout(100, 200)
        assertThat(adapter2.attachedViewHolders()).hasSize(3)
        assertThat(adapter2.failedToRecycleEvents()).contains(
            RecycledViewHolderEvent(
                itemId = 12,
                absoluteAdapterPosition = NO_POSITION,
                bindingAdapterPosition = NO_POSITION
            )
        )
        assertThat(adapter2.failedToRecycleEvents()).hasSize(1)
        assertThat(adapter2.attachedViewHolders()).doesNotContain(viewHolder)
    }

    @Suppress("DEPRECATION")
    @UiThreadTest
    @Test
    fun localAdapterPositions() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(4)
        val adapter3 = NestedTestAdapter(8)
        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(22)
        (0 until 22).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(recyclerView.getChildAdapterPosition(viewHolder.itemView)).isEqualTo(it)
            assertThat(viewHolder.absoluteAdapterPosition).isEqualTo(it)
        }
        (0 until 10).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter1)
        }

        (10 until 14).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it - 10)
            assertThat(viewHolder.adapterPosition).isEqualTo(it - 10)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter2)
        }

        (14 until 22).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it - 14)
            assertThat(viewHolder.adapterPosition).isEqualTo(it - 14)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter3)
        }
    }

    @Suppress("LocalVariableName")
    @UiThreadTest
    @Test
    fun localAdapterPositions_nested() {
        val adapter1_1 = NestedTestAdapter(10)
        val adapter1_2 = NestedTestAdapter(5)
        val adapter1 =
            ConcatAdapter(adapter1_1, adapter1_2)
        val adapter2_1 = NestedTestAdapter(3)
        val adapter2_2 = NestedTestAdapter(6)
        val adapter2 =
            ConcatAdapter(adapter2_1, adapter2_2)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(24)
        (0 until 24).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.absoluteAdapterPosition).isEqualTo(it)
            assertThat(recyclerView.getChildAdapterPosition(viewHolder.itemView)).isEqualTo(it)
        }
        (0 until 10).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter1_1)
        }
        (10 until 15).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it - 10)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter1_2)
        }
        (15 until 18).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it - 15)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter2_1)
        }
        (18 until 24).forEach {
            val viewHolder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
            assertThat(viewHolder.bindingAdapterPosition).isEqualTo(it - 18)
            assertThat(viewHolder.bindingAdapter).isSameInstanceAs(adapter2_2)
        }
    }

    @UiThreadTest
    @Test
    fun localAdapterPositions_notIncluded() {
        val adapter1 = NestedTestAdapter(10)
        val concatenated = ConcatAdapter(adapter1)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(10)
        val vh = checkNotNull(recyclerView.findViewHolderForAdapterPosition(3))
        assertThat(vh.bindingAdapterPosition).isEqualTo(3)

        val toBeRemoved = checkNotNull(recyclerView.findViewHolderForAdapterPosition(4))
        adapter1.removeItems(4, 1)
        assertThat(toBeRemoved.bindingAdapterPosition).isEqualTo(NO_POSITION)
        assertThat(toBeRemoved.absoluteAdapterPosition).isEqualTo(NO_POSITION)
        measureAndLayout(100, 100)
        assertThat(toBeRemoved.bindingAdapter).isNull()

        recyclerView.adapter = null
        measureAndLayout(100, 100)
        assertThat(vh.bindingAdapterPosition).isEqualTo(NO_POSITION)
        assertThat(vh.absoluteAdapterPosition).isEqualTo(NO_POSITION)
        assertThat(vh.bindingAdapter).isNull()
    }

    @UiThreadTest
    @Test
    fun attachDetachTest() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        assertThat(adapter1.attachedRecyclerViews()).containsExactly(recyclerView)
        assertThat(adapter2.attachedRecyclerViews()).containsExactly(recyclerView)
        val adapter3 = NestedTestAdapter(3)
        concatenated.addAdapter(adapter3)
        assertThat(adapter3.attachedRecyclerViews()).containsExactly(recyclerView)
        concatenated.removeAdapter(adapter3)
        assertThat(adapter3.attachedRecyclerViews()).isEmpty()
        recyclerView.adapter = null
        assertThat(adapter1.attachedRecyclerViews()).isEmpty()
        assertThat(adapter2.attachedRecyclerViews()).isEmpty()
    }

    @UiThreadTest
    @Test
    public fun scrollView() {
        val adapter1 = NestedTestAdapter(
            count = 10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 40)
            }
        )
        val adapter2 = NestedTestAdapter(
            count = 5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 10)
            }
        )
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        recyclerView.scrollBy(0, 90)
        assertThat(adapter1.attachedViewHolders()).hasSize(3)
        assertThat(
            adapter1.attachedViewHolders().map { it.bindingAdapterPosition }
        ).containsExactly(2, 3, 4)
    }

    @UiThreadTest
    @Test
    public fun recycledViewPositions() {
        val adapter1 = NestedTestAdapter(
            count = 5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 40)
            }
        )
        val adapter2 = NestedTestAdapter(
            count = 10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 10)
            }
        )
        val concatenated = ConcatAdapter(adapter1, adapter2)
        recyclerView.setItemViewCacheSize(0)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        // trigger recycle
        recyclerView.scrollBy(0, 90)
        // two views are recycled
        assertThat(adapter1.recycleEvents()).containsExactly(
            RecycledViewHolderEvent(
                itemId = 0,
                absoluteAdapterPosition = 0,
                bindingAdapterPosition = 0
            ),
            RecycledViewHolderEvent(
                itemId = 1,
                absoluteAdapterPosition = 1,
                bindingAdapterPosition = 1
            )
        ).inOrder()
    }

    @UiThreadTest
    @Test
    @SdkSuppress(minSdkVersion = 16)
    public fun recycledViewPositions_failedRecycle() {
        val adapter1 = NestedTestAdapter(
            count = 5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 40)
            }
        )
        val adapter2 = NestedTestAdapter(
            count = 10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 10)
            }
        )
        val concatenated = ConcatAdapter(adapter1, adapter2)
        recyclerView.setItemViewCacheSize(0)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        // give second view transient state
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(1)
        check(viewHolder != null) {
            "should have that view holder for position 1"
        }
        // give it transient state so that it won't be recycled
        viewHolder.itemView.setHasTransientState(true)
        // trigger recycle
        recyclerView.scrollBy(0, 90)
        // two views are recycled but one of them fails to recycle
        assertThat(adapter1.failedToRecycleEvents()).containsExactly(
            RecycledViewHolderEvent(
                itemId = 1,
                absoluteAdapterPosition = 1,
                bindingAdapterPosition = 1
            )
        )
        assertThat(adapter1.recycleEvents()).containsExactly(
            RecycledViewHolderEvent(
                itemId = 0,
                absoluteAdapterPosition = 0,
                bindingAdapterPosition = 0
            )
        )
    }

    @UiThreadTest
    @Test
    public fun recycledViewPositions_withAdapterChanges() {
        val adapter1 = NestedTestAdapter(
            count = 5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 20)
            }
        )
        val adapter2 = NestedTestAdapter(
            count = 10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 10)
            }
        )
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.setItemViewCacheSize(0)
        recyclerView.adapter = concatenated
        val layoutManager = (recyclerView.layoutManager!! as LinearLayoutManager)
        measureAndLayout(100, 100)
        // remove items 3 and 1
        adapter1.removeItems(3, 1)
        adapter1.removeItems(1, 1)

        // scroll to the beginning of the second adapter to trigger recycle of all items in adapter
        // 1
        layoutManager.scrollToPositionWithOffset(3, 0)
        measureAndLayout(100, 100)
        assertThat(adapter1.recycleEvents()).containsExactly(
            RecycledViewHolderEvent(
                itemId = 0,
                bindingAdapterPosition = 0,
                absoluteAdapterPosition = 0
            ),
            RecycledViewHolderEvent(
                itemId = 1,
                bindingAdapterPosition = NO_POSITION,
                absoluteAdapterPosition = NO_POSITION
            ),
            RecycledViewHolderEvent(
                itemId = 2,
                bindingAdapterPosition = 1,
                absoluteAdapterPosition = 1
            ),
            RecycledViewHolderEvent(
                itemId = 3,
                bindingAdapterPosition = NO_POSITION,
                absoluteAdapterPosition = NO_POSITION
            ),
            RecycledViewHolderEvent(
                itemId = 4,
                bindingAdapterPosition = 2,
                absoluteAdapterPosition = 2
            )
        )
    }

    @UiThreadTest
    @Test
    public fun recycledViewPositions_withAdapterChanges_secondAdapter() {
        val adapter1 = NestedTestAdapter(
            count = 5,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 20)
            }
        )
        val adapter2 = NestedTestAdapter(
            count = 10,
            getLayoutParams = {
                LayoutParams(MATCH_PARENT, 10)
            }
        )
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.setItemViewCacheSize(0)
        val layoutManager = (recyclerView.layoutManager!! as LinearLayoutManager)

        recyclerView.adapter = concatenated
        // start from the second adapter
        layoutManager.scrollToPositionWithOffset(adapter1.itemCount, 0)
        measureAndLayout(100, 100)
        assertThat(adapter1.attachedViewHolders()).isEmpty()
        assertThat(adapter2.attachedViewHolders()).hasSize(10)
        // remove items 3 and 1 from first adapter
        adapter1.removeItems(3, 1)
        adapter1.removeItems(1, 1)
        // remove items 2, 4 from the second adapter
        adapter2.removeItems(4, 1)
        adapter2.removeItems(2, 1)
        // scroll to the top of the list
        layoutManager.scrollToPositionWithOffset(0, 0)
        measureAndLayout(100, 100)
        assertThat(adapter1.attachedViewHolders()).hasSize(3)
        assertThat(adapter2.attachedViewHolders()).hasSize(4)
        // the UI will have (item ids)
        // 0, 2, 4, 5, 6
        // nothing is recycled in adapter 1
        assertThat(adapter1.recycleEvents()).isEmpty()
        // all items but first two are recycled in adapter2
        assertThat(adapter2.recycleEvents()).containsExactly(
            // first two items, 5 and 6, are not recycled as they are still visible
            // items 7 and 9 are recycled (removed)
            // items 8, 10 are still visible
            RecycledViewHolderEvent(
                itemId = 7,
                bindingAdapterPosition = NO_POSITION,
                absoluteAdapterPosition = NO_POSITION
            ),
            // pos 4 (item id 9) is removed from adapter 2
            RecycledViewHolderEvent(
                itemId = 9,
                bindingAdapterPosition = NO_POSITION,
                absoluteAdapterPosition = NO_POSITION
            ),
            RecycledViewHolderEvent(
                itemId = 11,
                bindingAdapterPosition = 4,
                absoluteAdapterPosition = 7
            ),
            RecycledViewHolderEvent(
                itemId = 12,
                bindingAdapterPosition = 5,
                absoluteAdapterPosition = 8
            ),
            RecycledViewHolderEvent(
                itemId = 13,
                bindingAdapterPosition = 6,
                absoluteAdapterPosition = 9
            ),
            RecycledViewHolderEvent(
                itemId = 14,
                bindingAdapterPosition = 7,
                absoluteAdapterPosition = 10
            ),
        )
    }

    @UiThreadTest
    @Test
    fun attachDetachTest_multipleRecyclerViews() {
        val recyclerView2 = RecyclerView(ApplicationProvider.getApplicationContext())
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        recyclerView2.adapter = concatenated
        assertThat(adapter1.attachedRecyclerViews()).containsExactly(recyclerView, recyclerView2)
        assertThat(adapter2.attachedRecyclerViews()).containsExactly(recyclerView, recyclerView2)
        val adapter3 = NestedTestAdapter(3)
        concatenated.addAdapter(adapter3)
        assertThat(adapter3.attachedRecyclerViews()).containsExactly(recyclerView, recyclerView2)
        concatenated.removeAdapter(adapter3)
        assertThat(adapter3.attachedRecyclerViews()).isEmpty()
        recyclerView.adapter = null
        assertThat(adapter1.attachedRecyclerViews()).containsExactly(recyclerView2)
        assertThat(adapter2.attachedRecyclerViews()).containsExactly(recyclerView2)
        recyclerView2.adapter = null
        assertThat(adapter1.attachedRecyclerViews()).isEmpty()
        assertThat(adapter2.attachedRecyclerViews()).isEmpty()
        assertThat(adapter3.attachedRecyclerViews()).isEmpty()
    }

    @Test
    @UiThreadTest
    fun adapterRemoval() {
        val adapter1 = NestedTestAdapter(3)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(8)
        assertThat(concatenated.removeAdapter(adapter1)).isTrue()
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(5)
        assertThat(concatenated.removeAdapter(adapter1)).isFalse()
        assertThat(concatenated.removeAdapter(adapter2)).isTrue()
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(0)
    }

    @Test
    @UiThreadTest
    fun boundAdapter() {
        val adapter1 = NestedTestAdapter(3)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        recyclerView.adapter = concatenated
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(8)
        val adapter1ViewHolders = (0 until 3).map {
            checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
        }
        val adapter2ViewHolders = (3 until 8).map {
            checkNotNull(recyclerView.findViewHolderForAdapterPosition(it))
        }
        adapter1ViewHolders.forEach {
            assertThat(it.bindingAdapter).isSameInstanceAs(adapter1)
        }
        adapter2ViewHolders.forEach {
            assertThat(it.bindingAdapter).isSameInstanceAs(adapter2)
        }
        assertThat(concatenated.removeAdapter(adapter1)).isTrue()
        // even when position is invalid, we should still be able to find the bound adapter
        adapter1ViewHolders.forEach {
            assertThat(it.bindingAdapter).isSameInstanceAs(adapter1)
        }
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(5)
        adapter1ViewHolders.forEach {
            assertThat(it.bindingAdapter).isNull()
        }
        assertThat(concatenated.removeAdapter(adapter1)).isFalse()
        assertThat(concatenated.removeAdapter(adapter2)).isTrue()
        measureAndLayout(100, 100)
        assertThat(recyclerView.childCount).isEqualTo(0)
        adapter2ViewHolders.forEach {
            assertThat(it.bindingAdapter).isNull()
        }
    }

    private fun measureAndLayout(@Suppress("SameParameterValue") width: Int, height: Int) {
        measure(width, height)
        layout(width, height)
    }

    private fun measure(width: Int, height: Int) {
        recyclerView.measure(AT_MOST or width, AT_MOST or height)
    }

    private fun layout(width: Int, height: Int) {
        recyclerView.layout(0, 0, width, height)
    }

    @Test
    fun size() {
        val concatenated = ConcatAdapter()
        val observer = LoggingAdapterObserver(concatenated)
        assertThat(concatenated).hasItemCount(0)
        concatenated.addAdapter(NestedTestAdapter(0))
        observer.assertEventsAndClear(
            "Empty adapter shouldn't cause notify"
        )

        val adapter1 = NestedTestAdapter(3)
        concatenated.addAdapter(adapter1)
        assertThat(concatenated).hasItemCount(3)
        observer.assertEventsAndClear(
            "adapter with count should trigger notify",
            Inserted(
                positionStart = 0,
                itemCount = 3
            )
        )

        val adapter2 = NestedTestAdapter(5)
        concatenated.addAdapter(adapter2)
        assertThat(concatenated).hasItemCount(8)
        observer.assertEventsAndClear(
            "appended non-empty adapter should trigger insert event",
            Inserted(
                positionStart = 3,
                itemCount = 5
            )
        )

        val adapter3 = NestedTestAdapter(2)
        concatenated.addAdapter(2, adapter3)
        assertThat(concatenated).hasItemCount(10)
        observer.assertEventsAndClear(
            "appended non-empty adapter should trigger insert event in right index",
            Inserted(
                positionStart = 3,
                itemCount = 2
            )
        )

        concatenated.addAdapter(NestedTestAdapter(0))
        assertThat(concatenated).hasItemCount(10)
        observer.assertEventsAndClear(
            "empty new adapter shouldn't trigger events"
        )
    }

    @Test
    fun nested_addition() {
        val concatenated = ConcatAdapter()
        val observer = LoggingAdapterObserver(concatenated)

        val adapter1 = NestedTestAdapter(0)
        concatenated.addAdapter(adapter1)
        observer.assertEventsAndClear("empty adapter triggers no events")

        adapter1.addItems(positionStart = 0, itemCount = 3)
        observer.assertEventsAndClear(
            "non-empty adapter triggers an event",
            Inserted(
                positionStart = 0,
                itemCount = 3
            )
        )
        assertThat(concatenated).hasItemCount(3)
        adapter1.addItems(positionStart = 1, itemCount = 2)
        observer.assertEventsAndClear(
            "inner adapter change should trigger an event",
            Inserted(
                positionStart = 1,
                itemCount = 2
            )
        )
        assertThat(concatenated).hasItemCount(5)
        val adapter2 = NestedTestAdapter(2)
        concatenated.addAdapter(adapter2)
        observer.assertEventsAndClear(
            "added adapter should trigger an event",
            Inserted(
                positionStart = 5,
                itemCount = 2
            )
        )
        assertThat(concatenated).hasItemCount(7)

        adapter2.addItems(positionStart = 0, itemCount = 3)
        observer.assertEventsAndClear(
            "nested adapter prepends data",
            Inserted(
                positionStart = 5,
                itemCount = 3
            )
        )
        assertThat(concatenated).hasItemCount(10)

        adapter2.addItems(positionStart = 2, itemCount = 4)
        observer.assertEventsAndClear(
            "nested adapter adds items with inner offset",
            Inserted(
                positionStart = 7,
                itemCount = 4
            )
        )
        assertThat(concatenated).hasItemCount(14)
    }

    @Test
    fun nested_removal() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(15)
        val adapter3 = NestedTestAdapter(20)

        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        val observer = LoggingAdapterObserver(concatenated)
        assertThat(concatenated).hasItemCount(45)

        adapter1.removeItems(positionStart = 0, itemCount = 2)
        observer.assertEventsAndClear(
            "removal from first adapter top",
            Removed(
                positionStart = 0,
                itemCount = 2
            )
        )
        assertThat(concatenated).hasItemCount(43)
        adapter1.removeItems(positionStart = 2, itemCount = 1)
        observer.assertEventsAndClear(
            "removal from first adapter inner",
            Removed(
                positionStart = 2,
                itemCount = 1
            )
        )
        assertThat(concatenated).hasItemCount(42)
        // now first adapter has size 7
        adapter2.removeItems(positionStart = 0, itemCount = 3)
        observer.assertEventsAndClear(
            "removal from second adapter should be offset",
            Removed(
                positionStart = adapter1.itemCount,
                itemCount = 3
            )
        )
        assertThat(concatenated).hasItemCount(39)
        adapter2.removeItems(positionStart = 6, itemCount = 4)
        observer.assertEventsAndClear(
            "inner item removal from middle adapter should be offset",
            Removed(
                positionStart = adapter1.itemCount + 6,
                itemCount = 4
            )
        )
        assertThat(concatenated).hasItemCount(35)

        adapter3.removeItems(positionStart = 0, itemCount = 3)
        observer.assertEventsAndClear(
            "removal from last adapter should be offset by adapter 1 and 2",
            Removed(
                positionStart = adapter1.itemCount + adapter2.itemCount,
                itemCount = 3
            )
        )

        adapter3.removeItems(positionStart = 2, itemCount = 5)
        observer.assertEventsAndClear(
            "removal from inner items from last adapter should be offset by adapter 1 & 2",
            Removed(
                positionStart = adapter1.itemCount + adapter2.itemCount + 2,
                itemCount = 5
            )
        )

        concatenated.removeAdapter(adapter2)
        observer.assertEventsAndClear(
            "removing an adapter should trigger removal",
            Removed(
                positionStart = adapter1.itemCount,
                itemCount = adapter2.itemCount
            )
        )
        assertThat(concatenated).hasItemCount(adapter1.itemCount + adapter3.itemCount)
        concatenated.removeAdapter(adapter1)
        observer.assertEventsAndClear(
            "removing first adapter should trigger removal",
            Removed(
                positionStart = 0,
                itemCount = adapter1.itemCount
            )
        )
        assertThat(concatenated).hasItemCount(adapter3.itemCount)
        concatenated.removeAdapter(adapter3)
        observer.assertEventsAndClear(
            "removing last adapter should trigger a removal",
            Removed(
                positionStart = 0,
                itemCount = adapter3.itemCount
            )
        )
        assertThat(concatenated).hasItemCount(0)
    }

    @Test
    fun nested_move() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(15)
        val adapter3 = NestedTestAdapter(20)
        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        val observer = LoggingAdapterObserver(concatenated)
        adapter1.moveItem(fromPosition = 3, toPosition = 5)
        observer.assertEventsAndClear(
            "move from first adapter should come as is",
            Moved(
                fromPosition = 3,
                toPosition = 5
            )
        )
        assertThat(concatenated).hasItemCount(45)
        adapter2.moveItem(fromPosition = 2, toPosition = 4)
        observer.assertEventsAndClear(
            "move in adapter 2 should be offset",
            Moved(
                fromPosition = adapter1.itemCount + 2,
                toPosition = adapter1.itemCount + 4
            )
        )
        adapter3.moveItem(fromPosition = 7, toPosition = 2)
        observer.assertEventsAndClear(
            "move in adapter 3 should be offset by adapter 1 & 2",
            Moved(
                fromPosition = adapter1.itemCount + adapter2.itemCount + 7,
                toPosition = adapter1.itemCount + adapter2.itemCount + 2
            )
        )
        assertThat(concatenated).hasItemCount(45)
    }

    @Test
    fun nested_itemChange_withPayload() = nested_itemChange("payload")

    @Test
    fun nested_itemChange_withoutPayload() = nested_itemChange(null)

    fun nested_itemChange(payload: Any? = null) {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(15)
        val adapter3 = NestedTestAdapter(20)
        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        val observer = LoggingAdapterObserver(concatenated)

        adapter1.changeItems(positionStart = 3, itemCount = 5, payload = payload)
        observer.assertEventsAndClear(
            "change from first adapter should come as is",
            Changed(
                positionStart = 3,
                itemCount = 5,
                payload = payload
            )
        )
        assertThat(concatenated).hasItemCount(45)
        adapter2.changeItems(positionStart = 2, itemCount = 4, payload = payload)
        observer.assertEventsAndClear(
            "change in adapter 2 should be offset",
            Changed(
                positionStart = adapter1.itemCount + 2,
                itemCount = 4,
                payload = payload
            )
        )
        adapter3.changeItems(positionStart = 7, itemCount = 2, payload = payload)
        observer.assertEventsAndClear(
            "change in adapter 3 should be offset by adapter 1 & 2",
            Changed(
                positionStart = adapter1.itemCount + adapter2.itemCount + 7,
                itemCount = 2,
                payload = payload
            )
        )
        assertThat(concatenated).hasItemCount(45)
    }

    @Test
    fun notifyDataSetChanged() {
        // we could add some logic to make data set changes add/remove/itemChange events yet
        // it is very hard to get right and might cause very undesired animations. Not doing it
        // for V1.
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(15)
        val adapter3 = NestedTestAdapter(20)
        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        val observer = LoggingAdapterObserver(concatenated)

        adapter1.changeDataSet(3)
        observer.assertEventsAndClear(
            "data set change should come as is",
            DataSetChanged
        )
        assertThat(concatenated).hasItemCount(38)
        adapter2.changeDataSet(20)
        observer.assertEventsAndClear(
            "data set change in adapter 2 should become full data set change",
            DataSetChanged
        )
        assertThat(concatenated).hasItemCount(43)
        adapter3.changeDataSet(newSize = 0)
        observer.assertEventsAndClear(
            """when an adapter changes size to 0, it should still come as 0 as we cannot
                |rely on itemCount changing immediately. In theory we would but adapter might be
                |faulty and not update its size immediately, which would work fine in RV because
                |everything is delayed but not here if we immediately read the item count
            """.trimMargin(),
            DataSetChanged
        )
        assertThat(concatenated).hasItemCount(23)
    }

    @Test
    fun viewTypeMapping_allViewsHaveDifferentTypes() {
        val adapter1 = NestedTestAdapter(10) { _, position ->
            position
        }
        val concatenated = ConcatAdapter(adapter1)
        val adapter1ViewTypes = (0 until 10).map {
            concatenated.getItemViewType(it)
        }.toSet()

        assertWithMessage("all items have unique types")
            .that(adapter1ViewTypes).hasSize(10)
        repeat(adapter1.itemCount) {
            assertThat(concatenated).bindView(recyclerView, it).verifyBoundTo(
                adapter = adapter1,
                localPosition = it
            )
        }
        val adapter2 = NestedTestAdapter(5) { _, position ->
            position
        }
        concatenated.addAdapter(adapter2)
        repeat(adapter2.itemCount) {
            assertThat(concatenated).bindView(recyclerView, adapter1.itemCount + it).verifyBoundTo(
                adapter = adapter2,
                localPosition = it
            )
        }

        concatenated.removeAdapter(adapter1)
        repeat(adapter2.itemCount) {
            assertThat(concatenated).bindView(recyclerView, it).verifyBoundTo(
                adapter = adapter2,
                localPosition = it
            )
        }
    }

    @Test
    fun viewTypeMapping_shareTypesWithinAdapter() {
        val adapter1 = NestedTestAdapter(10) { item, _ ->
            item.id % 3
        }
        val adapter2 = NestedTestAdapter(20) { item, _ ->
            item.id % 4
        }
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        val adapter1Types = (0 until adapter1.itemCount).map {
            concatenated.getItemViewType(it)
        }.toSet()
        assertThat(adapter1Types).hasSize(3)
        val adapter2Types = (adapter1.itemCount until adapter2.itemCount).map {
            concatenated.getItemViewType(it)
        }.toSet()
        assertThat(adapter2Types).hasSize(4)
        adapter2Types.forEach {
            assertThat(adapter1Types).doesNotContain(it)
        }
        (0 until adapter1.itemCount).forEach {
            assertThat(concatenated).bindView(recyclerView, it)
                .verifyBoundTo(
                    adapter = adapter1,
                    localPosition = it
                )
        }

        (0 until adapter2.itemCount).forEach {
            assertThat(concatenated).bindView(recyclerView, adapter1.itemCount + it)
                .verifyBoundTo(
                    adapter = adapter2,
                    localPosition = it
                )
        }

        concatenated.removeAdapter(adapter1)
        repeat(adapter2.itemCount) {
            assertThat(concatenated).bindView(recyclerView, it).verifyBoundTo(
                adapter = adapter2,
                localPosition = it
            )
        }
    }

    @Test(expected = java.lang.UnsupportedOperationException::class)
    fun stateRestorationTest_callingOnTheConcatAdapterIsNotAllowed() {
        val concatenated = ConcatAdapter()
        concatenated.stateRestorationPolicy = PREVENT
    }

    @Test
    fun stateRestoration_subAdapterAllowsNonEmpty() {
        val adapter1 = NestedTestAdapter(1).also {
            it.stateRestorationPolicy = ALLOW
        }
        val adapter2 = NestedTestAdapter(0).also {
            it.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        }
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        assertThat(concatenated).cannotRestoreState()
        adapter2.addItems(0, 1)
        assertThat(concatenated).canRestoreState()
        adapter2.removeItems(0, 1)
        assertThat(concatenated).cannotRestoreState()
    }

    @Test
    fun stateRestoration_subAdapterAllowsNonEmpty_viaNotifyChange() {
        val adapter1 = NestedTestAdapter(1).also {
            it.stateRestorationPolicy = ALLOW
        }
        val adapter2 = NestedTestAdapter(0).also {
            it.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        }
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        assertThat(concatenated).cannotRestoreState()
        adapter2.changeDataSet(1)
        assertThat(concatenated).canRestoreState()
        adapter2.changeDataSet(0)
        assertThat(concatenated).cannotRestoreState()
    }

    @Test
    fun stateRestoration() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(5)
        val adapter3 = NestedTestAdapter(20)
        val concatenated = ConcatAdapter(
            adapter1,
            adapter2,
            adapter3
        )
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)
        adapter2.stateRestorationPolicy = PREVENT
        assertThat(concatenated).hasStateRestorationPolicy(PREVENT)

        adapter3.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        assertThat(concatenated).hasStateRestorationPolicy(PREVENT)

        adapter2.stateRestorationPolicy = ALLOW
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)

        concatenated.removeAdapter(adapter3)
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)

        val adapter4 = NestedTestAdapter(3).also {
            it.stateRestorationPolicy = PREVENT
            concatenated.addAdapter(it)
        }
        assertThat(concatenated).hasStateRestorationPolicy(PREVENT)
        adapter4.stateRestorationPolicy = PREVENT_WHEN_EMPTY
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)
        concatenated.removeAdapter(adapter1)
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)
        adapter4.stateRestorationPolicy = ALLOW
        assertThat(concatenated).hasStateRestorationPolicy(ALLOW)
    }

    @Test
    fun disposal() {
        val adapter1 = NestedTestAdapter(10)
        val adapter2 = NestedTestAdapter(5)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        assertThat(adapter1.observerCount()).isEqualTo(1)
        assertThat(adapter2.observerCount()).isEqualTo(1)
        concatenated.removeAdapter(adapter1)
        assertThat(adapter1.observerCount()).isEqualTo(0)
        assertThat(adapter2.observerCount()).isEqualTo(1)

        val adapter3 = NestedTestAdapter(2)
        concatenated.addAdapter(adapter3)
        assertThat(adapter3.observerCount()).isEqualTo(1)
        concatenated.adapters.forEach {
            concatenated.removeAdapter(it)
        }
        listOf(adapter1, adapter2, adapter3).forEachIndexed { index, adapter ->
            assertWithMessage("adapter ${index + 1}").apply {
                that(adapter.observerCount()).isEqualTo(0)
                that(adapter.attachedRecyclerViews()).isEmpty()
            }
        }
    }

    /**
     * Running only on 26 due to the getParameters method call and this is not API version
     * dependent test so it is fine to only run it on new devices.
     */
    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun overrideTest() {
        // custom method instead of using toGenericString to avoid having class name
        fun Method.describe() = """
            $name(${parameters.map {
            it.type.canonicalName
        }}) : ${returnType.canonicalName}
        """.trimIndent()

        val excludedMethods = setOf(
            "registerAdapterDataObserver(" +
                "[androidx.recyclerview.widget.RecyclerView.AdapterDataObserver]) : void",
            "unregisterAdapterDataObserver(" +
                "[androidx.recyclerview.widget.RecyclerView.AdapterDataObserver]) : void",
            "canRestoreState([]) : boolean",
            "onBindViewHolder([androidx.recyclerview.widget.RecyclerView.ViewHolder, int, " +
                "java.util.List]) : void"
        )
        val adapterMethods = RecyclerView.Adapter::class.java.declaredMethods.filterNot {
            Modifier.isPrivate(it.modifiers) || Modifier.isFinal(it.modifiers)
        }.map {
            it.describe()
        }.filterNot {
            excludedMethods.contains(it)
        }
        val concatenatedAdapterMethods = ConcatAdapter::class.java.declaredMethods.map {
            it.describe()
        }
        assertWithMessage(
            """
            ConcatAdapter should override all methods in RecyclerView.Adapter for future 
            compatibility. If you want to exclude a method, update the test.
            """.trimIndent()
        ).that(concatenatedAdapterMethods).containsAtLeastElementsIn(adapterMethods)
    }

    @Test
    fun getAdapters() {
        val adapter1 = NestedTestAdapter(1)
        val adapter2 = NestedTestAdapter(2)
        val concatenated =
            ConcatAdapter(adapter1, adapter2)
        assertThat(concatenated.adapters).isEqualTo(listOf(adapter1, adapter2))
        concatenated.removeAdapter(adapter1)
        assertThat(concatenated.adapters).isEqualTo(listOf(adapter2))
    }

    @Test
    fun sharedTypes() {
        val adapter1 = NestedTestAdapter(3) { _, pos ->
            pos % 2
        }
        val adapter2 = NestedTestAdapter(3) { _, pos ->
            pos % 3
        }
        val concatenated = ConcatAdapter(
            Builder()
                .setIsolateViewTypes(false)
                .build(),
            adapter1, adapter2
        )
        assertThat(concatenated).bindView(recyclerView, 2)
            .verifyBoundTo(adapter1, 2)
        assertThat(concatenated).bindView(recyclerView, 3)
            .verifyBoundTo(adapter2, 0)
        assertThat(concatenated.getItemViewType(0)).isEqualTo(0)
        assertThat(concatenated.getItemViewType(1)).isEqualTo(1)
        assertThat(concatenated.getItemViewType(2)).isEqualTo(0)
        // notice that it resets to 0 because type is based on position
        assertThat(concatenated.getItemViewType(3)).isEqualTo(0)
        assertThat(concatenated.getItemViewType(4)).isEqualTo(1)
        assertThat(concatenated.getItemViewType(5)).isEqualTo(2)
        // ensure we bind via the correct adapter when a type is limited to a specific adapter
        assertThat(concatenated).bindView(recyclerView, 5)
            .verifyBoundTo(adapter2, 2)
    }

    @Test
    fun sharedTypes_allUnique() {
        val adapter1 = NestedTestAdapter(3) { item, _ ->
            item.id
        }
        val adapter2 = NestedTestAdapter(3) { item, _ ->
            item.id
        }
        val concatenated = ConcatAdapter(
            Builder()
                .setIsolateViewTypes(false)
                .build(),
            adapter1, adapter2
        )
        assertThat(concatenated).bindView(recyclerView, 0)
            .verifyBoundTo(adapter1, 0)
        assertThat(concatenated).bindView(recyclerView, 1)
            .verifyBoundTo(adapter1, 1)
        assertThat(concatenated).bindView(recyclerView, 2)
            .verifyBoundTo(adapter1, 2)
        assertThat(concatenated).bindView(recyclerView, 3)
            .verifyBoundTo(adapter2, 0)
        assertThat(concatenated).bindView(recyclerView, 4)
            .verifyBoundTo(adapter2, 1)
        assertThat(concatenated).bindView(recyclerView, 5)
            .verifyBoundTo(adapter2, 2)
    }

    @Test
    fun stableIds_noStableId() {
        val concatenatedAdapter = ConcatAdapter(
            Builder().setStableIdMode(NO_STABLE_IDS).build()
        )
        assertThat(concatenatedAdapter).doesNotHaveStableIds()
        // accept adapters with stable ids
        assertThat(concatenatedAdapter.addAdapter(PositionAsIdsNestedTestAdapter(10))).isTrue()
    }

    @Test
    fun stableIds_isolated_addAdapterWithoutStableId() {
        val concatenatedAdapter = ConcatAdapter(
            Builder().setStableIdMode(ISOLATED_STABLE_IDS).build()
        )
        assertThat(concatenatedAdapter).hasStableIds()
        assertThat(concatenatedAdapter).throwsException {
            it.addAdapter(
                NestedTestAdapter(10).also { nested ->
                    nested.setHasStableIds(false)
                }
            )
        }.hasMessageThat().contains(
            "All sub adapters must have stable ids when stable id mode" +
                " is ISOLATED_STABLE_IDS or SHARED_STABLE_IDS"
        )
    }

    @Test
    fun stableIds_shared_addAdapterWithoutStableId() {
        val concatenatedAdapter = ConcatAdapter(
            Builder().setStableIdMode(SHARED_STABLE_IDS).build()
        )
        assertThat(concatenatedAdapter).hasStableIds()
        assertThat(concatenatedAdapter).throwsException {
            it.addAdapter(
                NestedTestAdapter(10).also { nested ->
                    nested.setHasStableIds(false)
                }
            )
        }.hasMessageThat().contains(
            "All sub adapters must have stable ids when stable id mode" +
                " is ISOLATED_STABLE_IDS or SHARED_STABLE_IDS"
        )
    }

    @Test
    fun stableIds_isolated() {
        val concatenatedAdapter = ConcatAdapter(
            Builder().setStableIdMode(ISOLATED_STABLE_IDS).build()
        )
        assertThat(concatenatedAdapter).hasStableIds()
        val adapter1 = PositionAsIdsNestedTestAdapter(10)
        val adapter2 = PositionAsIdsNestedTestAdapter(10)
        concatenatedAdapter.addAdapter(adapter1)
        concatenatedAdapter.addAdapter(adapter2)
        assertThat(concatenatedAdapter).hasItemIds((0..19))
        // call again, ensure we are not popping up new ids
        assertThat(concatenatedAdapter).hasItemIds((0..19))
        concatenatedAdapter.removeAdapter(adapter1)
        assertThat(concatenatedAdapter).hasItemIds((10..19))

        val adapter3 = PositionAsIdsNestedTestAdapter(5)
        concatenatedAdapter.addAdapter(adapter3)
        assertThat(concatenatedAdapter).hasItemIds((10..24))

        // add in between
        val adapter4 = PositionAsIdsNestedTestAdapter(5)
        concatenatedAdapter.addAdapter(1, adapter4)
        assertThat(concatenatedAdapter).hasItemIds(
            (10..19) + (25..29) + (20..24)
        )
    }

    @Test
    fun stableIds_shared() {
        val concatenatedAdapter = ConcatAdapter(
            Builder().setStableIdMode(SHARED_STABLE_IDS).build()
        )
        assertThat(concatenatedAdapter).hasStableIds()
        val adapter1 = UniqueItemIdsNestedTestAdapter(10)
        val adapter2 = UniqueItemIdsNestedTestAdapter(10)
        concatenatedAdapter.addAdapter(adapter1)
        concatenatedAdapter.addAdapter(adapter2)
        assertThat(concatenatedAdapter).hasItemIds(adapter1.itemIds() + adapter2.itemIds())
        // call again, ensure we are not popping up new ids
        assertThat(concatenatedAdapter).hasItemIds(adapter1.itemIds() + adapter2.itemIds())
        concatenatedAdapter.removeAdapter(adapter1)
        assertThat(concatenatedAdapter).hasItemIds(adapter2.itemIds())

        val adapter3 = UniqueItemIdsNestedTestAdapter(5)
        concatenatedAdapter.addAdapter(adapter3)
        assertThat(concatenatedAdapter).hasItemIds(adapter2.itemIds() + adapter3.itemIds())

        // add in between
        val adapter4 = UniqueItemIdsNestedTestAdapter(5)
        concatenatedAdapter.addAdapter(1, adapter4)
        assertThat(concatenatedAdapter).hasItemIds(
            adapter2.itemIds() + adapter4.itemIds() + adapter3.itemIds()
        )
    }

    @Test
    public fun builderDefaults() {
        val defaultBuilder = Builder().build()
        assertThat(defaultBuilder.isolateViewTypes).isEqualTo(
            ConcatAdapter.Config.DEFAULT.isolateViewTypes
        )
        assertThat(defaultBuilder.stableIdMode).isEqualTo(
            ConcatAdapter.Config.DEFAULT.stableIdMode
        )
    }

    private var itemCounter = 0
    private fun produceItem(): TestItem = (itemCounter++).let {
        TestItem(id = it, value = it)
    }

    internal open inner class PositionAsIdsNestedTestAdapter(count: Int) :
        NestedTestAdapter(count) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }
    }

    internal open inner class UniqueItemIdsNestedTestAdapter(count: Int) :
        NestedTestAdapter(count) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long {
            return items[position].id.toLong()
        }

        fun itemIds() = items.map { it.id }
    }

    internal open inner class NestedTestAdapter(
        count: Int = 0,
        val getLayoutParams: ((ConcatAdapterViewHolder) -> LayoutParams)? = null,
        val itemTypeLookup: ((TestItem, position: Int) -> Int)? = null
    ) : RecyclerView.Adapter<ConcatAdapterViewHolder>() {
        private val attachedViewHolders = mutableListOf<ConcatAdapterViewHolder>()
        private val recycledViewHolderEvents = mutableListOf<RecycledViewHolderEvent>()
        private val failedToRecycleEvents = mutableListOf<RecycledViewHolderEvent>()
        private var attachedRecyclerViews = mutableListOf<RecyclerView>()
        private var observers = mutableListOf<RecyclerView.AdapterDataObserver>()

        val items = mutableListOf<TestItem>().also { list ->
            repeat(count) {
                list.add(produceItem())
            }
        }

        fun attachedViewHolders(): List<ConcatAdapterViewHolder> = attachedViewHolders

        override fun onViewAttachedToWindow(holder: ConcatAdapterViewHolder) {
            assertThat(attachedViewHolders).doesNotContain(holder)
            attachedViewHolders.add(holder)
        }

        override fun onViewDetachedFromWindow(holder: ConcatAdapterViewHolder) {
            assertThat(attachedViewHolders).contains(holder)
            holder.onDetached()
            attachedViewHolders.remove(holder)
        }

        override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
            assertThat(observers).doesNotContain(observer)
            observers.add(observer)
            super.registerAdapterDataObserver(observer)
        }

        override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
            assertThat(observers).contains(observer)
            observers.remove(observer)
            super.unregisterAdapterDataObserver(observer)
        }

        fun observerCount() = observers.size

        override fun getItemViewType(position: Int): Int {
            itemTypeLookup?.let {
                return it(items[position], position)
            }
            return super.getItemViewType(position)
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            assertThat(attachedRecyclerViews).doesNotContain(recyclerView)
            attachedRecyclerViews.add(recyclerView)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            assertThat(attachedRecyclerViews).contains(recyclerView)
            attachedRecyclerViews.remove(recyclerView)
        }

        fun attachedRecyclerViews(): List<RecyclerView> = attachedRecyclerViews

        fun addItems(positionStart: Int, itemCount: Int = 1) {
            require(itemCount > 0)
            require(positionStart >= 0 && positionStart <= items.size)
            val newItems = (0 until itemCount).map {
                produceItem()
            }
            items.addAll(positionStart, newItems)
            notifyItemRangeInserted(positionStart, itemCount)
        }

        fun removeItems(positionStart: Int, itemCount: Int = 1) {
            require(positionStart >= 0)
            require(positionStart + itemCount <= items.size)
            require(itemCount > 0)
            repeat(itemCount) {
                items.removeAt(positionStart)
            }
            notifyItemRangeRemoved(positionStart, itemCount)
        }

        fun moveItem(fromPosition: Int, toPosition: Int) {
            require(fromPosition >= 0 && fromPosition < items.size)
            require(toPosition >= 0 && toPosition < items.size)
            if (fromPosition == toPosition) return
            items.add(toPosition, items.removeAt(fromPosition))
            notifyItemMoved(fromPosition, toPosition)
        }

        fun changeDataSet(newSize: Int = items.size) {
            require(newSize >= 0)
            val newItems = (0 until newSize).map {
                produceItem()
            }
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun changeItems(positionStart: Int, itemCount: Int, payload: Any? = null) {
            require(positionStart >= 0 && positionStart < items.size)
            require(positionStart + itemCount <= items.size)
            (positionStart until positionStart + itemCount).forEach {
                val prev = items[it]
                items[it] = prev.copy(value = prev.value + 1)
            }
            if (payload == null) {
                notifyItemRangeChanged(positionStart, itemCount)
            } else {
                notifyItemRangeChanged(positionStart, itemCount, payload)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConcatAdapterViewHolder {
            return ConcatAdapterViewHolder(parent.context, viewType).also { holder ->
                getLayoutParams?.invoke(holder)?.let {
                    holder.itemView.layoutParams = it
                }
            }
        }

        override fun onBindViewHolder(holder: ConcatAdapterViewHolder, position: Int) {
            assertThat(getItemViewType(position)).isEqualTo(holder.localViewType)
            holder.bindTo(this, items[position], position)
        }

        override fun onViewRecycled(holder: ConcatAdapterViewHolder) {
            recycledViewHolderEvents.add(RecycledViewHolderEvent(holder))
            holder.onRecycled()
        }

        override fun getItemCount() = items.size

        override fun onFailedToRecycleView(holder: ConcatAdapterViewHolder): Boolean {
            failedToRecycleEvents.add(RecycledViewHolderEvent(holder))
            return super.onFailedToRecycleView(holder)
        }

        fun getItemAt(localPosition: Int) = items[localPosition]
        fun recycleEvents(): List<RecycledViewHolderEvent> = recycledViewHolderEvents
        fun failedToRecycleEvents(): List<RecycledViewHolderEvent> = failedToRecycleEvents
    }

    internal class ConcatAdapterViewHolder(
        context: Context,
        val localViewType: Int
    ) : RecyclerView.ViewHolder(View(context)) {
        private var boundItem: TestItem? = null
        private var boundAdapter: RecyclerView.Adapter<*>? = null
        private var boundPosition: Int? = null
        fun bindTo(adapter: RecyclerView.Adapter<*>, item: TestItem, position: Int) {
            boundAdapter = adapter
            boundPosition = position
            boundItem = item
        }

        fun boundItem() = boundItem
        fun boundLocalPosition() = boundPosition
        fun boundAdapter() = boundAdapter
        fun onDetached() {
            assertPosition()
        }
        fun onRecycled() {
            assertPosition()
            boundItem = null
            boundPosition = -1
            boundAdapter = null
        }

        private fun assertPosition() {
            val shouldHavePosition = !isRemoved() && isBound() &&
                !isAdapterPositionUnknown() && !isInvalid()
            assertWithMessage(
                "binding adapter position $this"
            ).that(shouldHavePosition).isEqualTo(
                bindingAdapterPosition != NO_POSITION
            )
            assertWithMessage(
                "binding adapter position $this"
            ).that(shouldHavePosition).isEqualTo(
                absoluteAdapterPosition != NO_POSITION
            )
        }
    }

    class LoggingAdapterObserver(
        private val src: RecyclerView.Adapter<*>
    ) : RecyclerView.AdapterDataObserver() {
        init {
            src.registerAdapterDataObserver(this)
        }

        private val events = mutableListOf<Event>()

        fun assertEventsAndClear(
            message: String,
            vararg expected: Event
        ) {
            assertWithMessage(message).that(events).isEqualTo(expected.toList())
            events.clear()
        }

        override fun onChanged() {
            events.add(DataSetChanged)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            events.add(
                Changed(
                    positionStart = positionStart,
                    itemCount = itemCount
                )
            )
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            events.add(
                Changed(
                    positionStart = positionStart,
                    itemCount = itemCount,
                    payload = payload
                )
            )
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            events.add(
                Inserted(
                    positionStart = positionStart,
                    itemCount = itemCount
                )
            )
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            events.add(
                Removed(
                    positionStart = positionStart,
                    itemCount = itemCount
                )
            )
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            require(itemCount == 1) {
                "RV does not support moving more than 1 item at a time"
            }
            events.add(
                Moved(
                    fromPosition = fromPosition,
                    toPosition = toPosition
                )
            )
        }

        override fun onStateRestorationPolicyChanged() {
            events.add(
                StateRestorationPolicy(
                    newValue = src.stateRestorationPolicy
                )
            )
        }

        sealed class Event {
            object DataSetChanged : Event()
            data class Changed(
                val positionStart: Int,
                val itemCount: Int,
                val payload: Any? = null
            ) : Event()

            data class Inserted(
                val positionStart: Int,
                val itemCount: Int
            ) : Event()

            data class Removed(
                val positionStart: Int,
                val itemCount: Int
            ) : Event()

            data class Moved(
                val fromPosition: Int,
                val toPosition: Int
            ) : Event()

            data class StateRestorationPolicy(
                val newValue: RecyclerView.Adapter.StateRestorationPolicy
            ) : Event()
        }
    }

    internal data class TestItem(
        val id: Int,
        val value: Int,
        val viewType: Int = 0
    )

    internal data class RecycledViewHolderEvent(
        val itemId: Int?,
        val absoluteAdapterPosition: Int,
        val bindingAdapterPosition: Int,
    ) {
        constructor(viewHolder: ConcatAdapterViewHolder) : this(
            itemId = viewHolder.boundItem()?.id,
            absoluteAdapterPosition = viewHolder.absoluteAdapterPosition,
            bindingAdapterPosition = viewHolder.bindingAdapterPosition
        )
    }
}