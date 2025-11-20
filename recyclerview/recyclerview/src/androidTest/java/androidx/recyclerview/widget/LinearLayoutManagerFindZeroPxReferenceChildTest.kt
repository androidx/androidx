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
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Number of items that fit in RecyclerView's viewport
private const val n = 4
// Size of children in direction of orientation
private const val childSize = 150
// Size of both the width and height of RecyclerView
private const val size = childSize * n

/**
 * This tests if [LinearLayoutManager.findReferenceChild] is able to find zero pixel sized items
 * that are laid out on the edge of RecyclerView. It sets up an RV that fits exactly 4 children in
 * its viewport, with a list of 250 items. On both ends there will be an empty view, so we can
 * easily test with [LinearLayoutManager.mReverseLayout] and [LinearLayoutManager.mStackFromEnd].
 *
 * Two scenarios are tested:
 * 1. The empty view is the first/last item, and we don't touch RV (keep it at the start).
 * 2. The empty view is the second/penultimate item, and we scroll RV by one position.
 *
 * Scenario 2 has one problem: with mStackFromEnd, it is not possible to layout the empty view at
 * the edge using [LinearLayoutManager.scrollToPositionWithOffset], so we first need to scroll far
 * enough that the empty view is out of bounds, and then scroll back to the empty view with
 * [LinearLayoutManager.scrollToPosition]. That positions the empty view on the edge in all cases in
 * scenario 2, regardless of the parameters.
 *
 * All scenarios are tested with all combinations of horizontal/vertical,
 * stackFromEnd/dontStackFromEnd, reverseLayout/dontReverseLayout and
 * extraLayoutSpace/noExtraLayoutSpace.
 */
@MediumTest
@RunWith(Parameterized::class)
class LinearLayoutManagerFindZeroPxReferenceChildTest(
    private val config: Config,
    addExtraLayoutSpace: Boolean,
    zeroPxItemPosition: Int,
) : BaseLinearLayoutManagerTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0},addExtraLayoutSpace={1},zeroPxItemPosition={2}")
        fun spec(): List<Array<Any>> =
            listOf(VERTICAL, HORIZONTAL).flatMap { orientation ->
                listOf(false, true).flatMap { stackFromEnd ->
                    listOf(false, true).flatMap { reverseLayout ->
                        listOf(false, true).flatMap { addExtraLayoutSpace ->
                            listOf(0, 1).map { zeroPxItemPosition -> // position of empty item
                                arrayOf(
                                    Config(orientation, reverseLayout, stackFromEnd).apply {
                                        mItemCount = Config.DEFAULT_ITEM_COUNT
                                    },
                                    addExtraLayoutSpace,
                                    zeroPxItemPosition,
                                )
                            }
                        }
                    }
                }
            }
    }

    private val firstEmptyItemPosition = zeroPxItemPosition
    private val lastEmptyItemPosition = config.mItemCount - 1 - zeroPxItemPosition

    private val extraLayoutSpace = if (addExtraLayoutSpace) size else 0

    private val itemLayoutParams =
        RecyclerView.LayoutParams(
            if (config.mOrientation == HORIZONTAL) childSize else size,
            if (config.mOrientation == VERTICAL) childSize else size,
        )

    private val emptyItemLayoutParams =
        RecyclerView.LayoutParams(
            if (config.mOrientation == HORIZONTAL) 0 else size,
            if (config.mOrientation == VERTICAL) 0 else size,
        )

    @Test
    fun test() {
        val llm = MyLayoutManager(activity)
        config.mTestLayoutManager = llm
        config.mTestAdapter = MyTestAdapter()
        setupByConfig(config, true, null, RecyclerView.LayoutParams(size, size))

        val targetPosition =
            if (config.mStackFromEnd) {
                lastEmptyItemPosition
            } else {
                firstEmptyItemPosition
            }

        // Given an RV that either didn't scroll, or scrolled to the second view ..
        if (firstEmptyItemPosition > 0) {
            // First scroll away from the target position
            mLayoutManager.expectLayouts(1)
            scrollToPosition(config.mItemCount - 1 - targetPosition)
            mLayoutManager.waitForLayout(2)

            // Then scroll towards the target position
            mLayoutManager.expectLayouts(1)
            scrollToPosition(targetPosition)
            mLayoutManager.waitForLayout(2)
        }

        // .. when we trigger a layout ..
        llm.clearRecordedReferenceChildren()
        mLayoutManager.expectLayouts(1)
        requestLayoutOnUIThread(mRecyclerView)
        mLayoutManager.waitForLayout(2)

        // .. then LinearLayoutManager has searched for
        // a reference child, and found the first child
        Assert.assertEquals(listOf(targetPosition), llm.recordedReferenceChildren)
    }

    private inner class MyLayoutManager internal constructor(context: Context) :
        WrappedLinearLayoutManager(context, config.mOrientation, config.mReverseLayout) {

        internal val recordedReferenceChildren: MutableList<Int> = ArrayList()

        fun clearRecordedReferenceChildren() {
            recordedReferenceChildren.clear()
        }

        override fun calculateExtraLayoutSpace(
            state: RecyclerView.State,
            extraLayoutSpaceArray: IntArray,
        ) {
            extraLayoutSpaceArray[0] = extraLayoutSpace
            extraLayoutSpaceArray[1] = extraLayoutSpace
        }

        internal override fun findReferenceChild(
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?,
            layoutFromEnd: Boolean,
            traverseChildrenInReverseOrder: Boolean,
        ): View {
            val referenceChild =
                super.findReferenceChild(
                    recycler,
                    state,
                    layoutFromEnd,
                    traverseChildrenInReverseOrder,
                )
            recordedReferenceChildren.add(getPosition(referenceChild))
            return referenceChild
        }
    }

    private inner class MyTestAdapter : TestAdapter(config.mItemCount, itemLayoutParams) {
        override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
            // First and last item should be zero pixels
            if (position == firstEmptyItemPosition || position == lastEmptyItemPosition) {
                val item = mItems[position]
                getTextViewInHolder(holder).text = null
                holder.mBoundItem = item
                holder.itemView.layoutParams = RecyclerView.LayoutParams(emptyItemLayoutParams)
            } else {
                super.onBindViewHolder(holder, position)
            }
        }
    }
}
