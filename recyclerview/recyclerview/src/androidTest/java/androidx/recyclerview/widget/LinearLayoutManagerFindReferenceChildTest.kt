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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Size of both the width and height of both RecyclerView and it's children
private const val size = 500

@MediumTest
@RunWith(Parameterized::class)
class LinearLayoutManagerFindReferenceChildTest(
    private val config: Config,
    private val addExtraLayoutSpace: Boolean,
    private val childOffset: Int,
) : BaseLinearLayoutManagerTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0},addExtraLayoutSpace={1},childOffset={2}")
        fun spec(): List<Array<Any>> =
            listOf(VERTICAL, HORIZONTAL).flatMap { orientation ->
                listOf(false, true).flatMap { stackFromEnd ->
                    listOf(false, true).flatMap { reverseLayout ->
                        listOf(false, true).flatMap { addExtraLayoutSpace ->
                            listOf(0, 1).map { childOffset ->
                                arrayOf(
                                    Config(orientation, reverseLayout, stackFromEnd).apply {
                                        mItemCount = Config.DEFAULT_ITEM_COUNT
                                    },
                                    addExtraLayoutSpace,
                                    childOffset,
                                )
                            }
                        }
                    }
                }
            }
    }

    @Test
    fun test() {
        val llm = MyLayoutManager(activity)
        config.mTestLayoutManager = llm
        setupByConfig(config, true, createLayoutParams(), createLayoutParams())

        val targetPosition = if (config.mStackFromEnd) config.mItemCount - 2 else 1
        val expectedReferenceChildPosition =
            if (childOffset <= 0) {
                // With no childOffset, the first view is completely out of bounds and the second
                // view should be the reference child
                targetPosition
            } else {
                // With a childOffset, the last few pixels of the first view are in bounds, so the
                // first view should be the reference child
                if (config.mStackFromEnd) config.mItemCount - 1 else 0
            }

        // Given an RV that is scrolled to start with the second view,
        // or if childOffset > 0, scrolled to start with the last pixels of the first view ..
        val resolvedOffset = if (config.mStackFromEnd) -childOffset else childOffset
        mLayoutManager.expectLayouts(1)
        scrollToPositionWithOffset(targetPosition, resolvedOffset)
        mLayoutManager.waitForLayout(2)

        // .. when we trigger a layout ..
        llm.clearRecordedReferenceChildren()
        mLayoutManager.expectLayouts(1)
        requestLayoutOnUIThread(mRecyclerView)
        mLayoutManager.waitForLayout(2)

        // .. then LinearLayoutManager has searched for a reference child, and found either the
        // first or the second child, respectively when childOffset was >0 or ==0
        assertEquals(listOf(expectedReferenceChildPosition), llm.recordedReferenceChildren)
    }

    private fun createLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(size, size)
    }

    private inner class MyLayoutManager internal constructor(context: Context) :
        WrappedLinearLayoutManager(context, config.mOrientation, config.mReverseLayout) {

        internal val recordedReferenceChildren: MutableList<Int> = ArrayList()

        fun clearRecordedReferenceChildren() {
            recordedReferenceChildren.clear()
        }

        override fun calculateExtraLayoutSpace(
            state: RecyclerView.State,
            extraLayoutSpace: IntArray,
        ) {
            if (!addExtraLayoutSpace) {
                super.calculateExtraLayoutSpace(state, extraLayoutSpace)
            } else {
                extraLayoutSpace[0] = size
                extraLayoutSpace[1] = size
            }
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
}
