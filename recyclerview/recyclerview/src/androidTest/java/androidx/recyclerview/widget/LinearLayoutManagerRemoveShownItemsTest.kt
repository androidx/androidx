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
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests that we're looking at the right item(s) after removing the current item(s) in the case
 * where all item views have the same size as RecyclerViews view port.
 */

@MediumTest
@RunWith(Parameterized::class)
class LinearLayoutManagerRemoveShownItemsTest(
    private val config: Config,
    private val extraLayoutSpaceItems: Int
) : BaseLinearLayoutManagerTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0},extraLayoutSpaceItems={1}")
        fun spec(): List<Array<Any>> =
            listOf(VERTICAL, HORIZONTAL).flatMap { orientation ->
                listOf(false, true).flatMap { stackFromEnd ->
                    listOf(false, true).flatMap { reverseLayout ->
                        listOf(0, 1, 2).map { extraLayoutSpaceItems ->
                            arrayOf(
                                Config(orientation, reverseLayout, stackFromEnd).apply {
                                    mItemCount = Config.DEFAULT_ITEM_COUNT
                                },
                                extraLayoutSpaceItems
                            )
                        }
                    }
                }
            }
    }

    /**
     * 1 item removed.
     * Middle of the set of items.
     */
    @Test
    fun notifyItemRangeRemoved_1_onlyCorrectItemVisible() {
        val llm = MyLayoutManager(activity, 500)
        config.mTestLayoutManager = llm
        setupByConfig(
            config,
            true,
            RecyclerView.LayoutParams(500, 500),
            RecyclerView.LayoutParams(500, 500)
        )

        val startingAdapterPosition = 10 // Item with label 11

        // Expected behavior when removing the shown view is
        // that the view(s) after it are moved into the gap.
        val expectedResultingAdapterPosition =
            startingAdapterPosition - if (config.mStackFromEnd) 1 else 0

        // Given an RV showing the 11th view that is as big as RV itself ..
        mLayoutManager.expectLayouts(1)
        scrollToPosition(startingAdapterPosition)
        mLayoutManager.waitForLayout(2)

        // .. when we remove that view ..
        mLayoutManager.expectLayouts(2)
        mTestAdapter.deleteAndNotify(startingAdapterPosition, 1)
        mLayoutManager.waitForLayout(2)

        // .. then the views after the removed view are moved into the gap
        mActivityRule.runOnUiThread {
            assertEquals(expectedResultingAdapterPosition, llm.findFirstVisibleItemPosition())
            assertEquals(
                expectedResultingAdapterPosition,
                llm.findFirstCompletelyVisibleItemPosition()
            )
            assertEquals(
                expectedResultingAdapterPosition,
                llm.findLastCompletelyVisibleItemPosition()
            )
            assertEquals(expectedResultingAdapterPosition, llm.findLastVisibleItemPosition())
        }
    }

    /**
     * 2 items removed.
     * Middle of the set of items.
     */
    @Test
    fun notifyItemRangeRemoved_2_onlyCorrectItemsVisible() {
        val llm = MyLayoutManager(activity, 500)
        config.mTestLayoutManager = llm
        val rvLayoutParams =
            if (config.mOrientation == VERTICAL) {
                RecyclerView.LayoutParams(500, 1000)
            } else {
                RecyclerView.LayoutParams(1000, 500)
            }
        setupByConfig(
            config,
            true,
            RecyclerView.LayoutParams(500, 500),
            rvLayoutParams
        )

        val startingAdapterPosition = 10 // Items 10 and 11 (with labels 11 and 12) will show.

        // Expected behavior when removing the shown view is
        // that the view(s) after it are moved into the gap.
        val expectedResultingAdapterPosition =
            startingAdapterPosition - if (config.mStackFromEnd) 2 else 0

        // This will make sure that items 10 and 11 are shown
        val adapterPositionToScrollTo = startingAdapterPosition + if (config.mStackFromEnd) 0 else 1
        mLayoutManager.expectLayouts(1)
        scrollToPosition(adapterPositionToScrollTo)
        mLayoutManager.waitForLayout(2)

        // .. when we remove that view ..
        mLayoutManager.expectLayouts(2)
        mTestAdapter.deleteAndNotify(startingAdapterPosition, 2)
        mLayoutManager.waitForLayout(2)

        // .. then the views after the removed view are moved into the gap
        mActivityRule.runOnUiThread {
            assertEquals(expectedResultingAdapterPosition, llm.findFirstVisibleItemPosition())
            assertEquals(
                expectedResultingAdapterPosition,
                llm.findFirstCompletelyVisibleItemPosition()
            )
            assertEquals(
                expectedResultingAdapterPosition + 1,
                llm.findLastCompletelyVisibleItemPosition()
            )
            assertEquals(expectedResultingAdapterPosition + 1, llm.findLastVisibleItemPosition())
        }
    }

    /**
     * All attached items removed.
     * Middle of the set of items.
     */
    @Test
    fun notifyItemRangeRemoved_all_onlyCorrectItemVisible() {
        val llm = MyLayoutManager(activity, 500)
        config.mTestLayoutManager = llm
        setupByConfig(
            config,
            true,
            RecyclerView.LayoutParams(500, 500),
            RecyclerView.LayoutParams(500, 500)
        )

        val startingAdapterPosition = 10 // Item with label 11

        // Expected behavior when removing the shown view is
        // that the view(s) after it are moved into the gap.
        val expectedResultingAdapterPosition =
            (startingAdapterPosition - extraLayoutSpaceItems) - if (config.mStackFromEnd) 1 else 0

        // Given an RV showing the 11th view that is as big as RV itself ..
        mLayoutManager.expectLayouts(1)
        scrollToPosition(startingAdapterPosition)
        mLayoutManager.waitForLayout(2)

        // .. when we remove all laid out items ..
        val removeFrom = llm.run {
            List(childCount) {
                getPosition(getChildAt(it)!!)
            }.minOrNull()
        }!!
        mLayoutManager.expectLayouts(2)
        mTestAdapter.deleteAndNotify(removeFrom, llm.childCount)
        mLayoutManager.waitForLayout(2)

        // .. then the views after the removed view are moved into the gap
        mActivityRule.runOnUiThread {
            assertEquals(expectedResultingAdapterPosition, llm.findFirstVisibleItemPosition())
            assertEquals(
                expectedResultingAdapterPosition,
                llm.findFirstCompletelyVisibleItemPosition()
            )
            assertEquals(
                expectedResultingAdapterPosition,
                llm.findLastCompletelyVisibleItemPosition()
            )
            assertEquals(expectedResultingAdapterPosition, llm.findLastVisibleItemPosition())
        }
    }

    private inner class MyLayoutManager(context: Context, val itemSize: Int) :
        WrappedLinearLayoutManager(context, config.mOrientation, config.mReverseLayout) {

        override fun calculateExtraLayoutSpace(
            state: RecyclerView.State,
            extraLayoutSpace: IntArray
        ) {
            when (extraLayoutSpaceItems) {
                0 -> super.calculateExtraLayoutSpace(state, extraLayoutSpace)
                else -> {
                    extraLayoutSpace[0] = itemSize * extraLayoutSpaceItems
                    extraLayoutSpace[1] = itemSize * extraLayoutSpaceItems
                }
            }
        }
    }
}
