/*
 * Copyright 2023 The Android Open Source Project
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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Test

// This subclass exists to allow porting individual tests in this class to Kotlin
// without porting the whole (very large) class
class RecyclerViewLayoutTestKotlin : RecyclerViewLayoutTest() {
    @Test
    @Throws(Throwable::class)
    fun duplicateAdapterPositionTest() = runBlocking(Dispatchers.Main) {
        val testAdapter = TestAdapter(10)
        val tlm: TestLayoutManager = object : TestLayoutManager() {
            override fun onLayoutChildren(
                recycler: RecyclerView.Recycler,
                state: RecyclerView.State
            ) {
                detachAndScrapAttachedViews(recycler)
                layoutRange(recycler, 0, state.itemCount)
                if (!state.isPreLayout) {
                    while (!recycler.scrapList.isEmpty()) {
                        val viewHolder = recycler.scrapList[0]
                        addDisappearingView(viewHolder.itemView, 0)
                    }
                }
            }

            override fun supportsPredictiveItemAnimations(): Boolean {
                return true
            }
        }
        val animator = DefaultItemAnimator()
        animator.supportsChangeAnimations = true
        animator.changeDuration = 10000
        testAdapter.setHasStableIds(true)
        val recyclerView = TestRecyclerView(activity)
        recyclerView.setLayoutManager(tlm)
        recyclerView.setAdapter(testAdapter)
        recyclerView.setItemAnimator(animator)
        setRecyclerView(recyclerView)
        awaitFrame()
        awaitFrame()

        testAdapter.mItems[2].mType += 2
        val itemId = testAdapter.mItems[2].mId
        testAdapter.changeAndNotify(2, 1)
        awaitFrame()
        awaitFrame()
        MatcherAssert.assertThat("Assumption check", recyclerView.childCount, CoreMatchers.`is`(11))

        // now mangle the order and run the test
        var hidden: RecyclerView.ViewHolder? = null
        var updated: RecyclerView.ViewHolder? = null
        for (i in 0 until recyclerView.childCount) {
            val view = recyclerView.getChildAt(i)
            val vh = recyclerView.getChildViewHolder(view)
            if (vh.getAbsoluteAdapterPosition() == 2) {
                if (mRecyclerView.mChildHelper.isHidden(view)) {
                    MatcherAssert.assertThat(hidden, CoreMatchers.nullValue())
                    hidden = vh
                } else {
                    MatcherAssert.assertThat(updated, CoreMatchers.nullValue())
                    updated = vh
                }
            }
        }
        MatcherAssert.assertThat(hidden, CoreMatchers.notNullValue())
        MatcherAssert.assertThat(updated, CoreMatchers.notNullValue())
        mRecyclerView.startInterceptRequestLayout()

        // first put the hidden child back
        val index1: Int = mRecyclerView.indexOfChild(hidden!!.itemView)
        val index2: Int = mRecyclerView.indexOfChild(updated!!.itemView)
        if (index1 < index2) {
            // swap views
            swapViewsAtIndices(recyclerView, index1, index2)
        }
        MatcherAssert.assertThat(
            tlm.findViewByPosition(2),
            CoreMatchers.sameInstance(updated.itemView)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForAdapterPosition(2),
            CoreMatchers.sameInstance(updated)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForLayoutPosition(2),
            CoreMatchers.sameInstance(updated)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForItemId(itemId.toLong()),
            CoreMatchers.sameInstance(updated)
        )

        // now swap back
        swapViewsAtIndices(recyclerView, index1, index2)
        MatcherAssert.assertThat(
            tlm.findViewByPosition(2),
            CoreMatchers.sameInstance(updated.itemView)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForAdapterPosition(2),
            CoreMatchers.sameInstance(updated)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForLayoutPosition(2),
            CoreMatchers.sameInstance(updated)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForItemId(itemId.toLong()),
            CoreMatchers.sameInstance(updated)
        )

        // now remove updated. re-assert fallback to the hidden one
        tlm.removeView(updated.itemView)
        MatcherAssert.assertThat(tlm.findViewByPosition(2), CoreMatchers.nullValue())
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForAdapterPosition(2),
            CoreMatchers.sameInstance(hidden)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForLayoutPosition(2),
            CoreMatchers.sameInstance(hidden)
        )
        MatcherAssert.assertThat(
            recyclerView.findViewHolderForItemId(itemId.toLong()),
            CoreMatchers.sameInstance(hidden)
        )
    }
}
