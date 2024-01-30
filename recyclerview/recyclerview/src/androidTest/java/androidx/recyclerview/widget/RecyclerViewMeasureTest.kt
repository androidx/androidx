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

import android.graphics.Rect
import android.view.View
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.UNSPECIFIED
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@SmallTest
@RunWith(Parameterized::class)
class RecyclerViewMeasureTest(
    private val childWidth: Int,
    private val childHeight: Int,
    private val firstWidthMode: Int,
    private val firstWidth: Int,
    private val firstHeightMode: Int,
    private val firstHeight: Int,
    private val secondWidthMode: Int,
    private val secondWidth: Int,
    private val secondHeightMode: Int,
    private val secondHeight: Int,
    private val expectedWidthMode: Int,
    private val expectedWidth: Int,
    private val expectedHeightMode: Int,
    private val expectedHeight: Int
) {
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mMockLayoutManager: MockLayoutManager

    companion object {

        @JvmStatic
        @Parameterized.Parameters(
            name = "childWidth = {0}, childHeight = {1}, firstWidthMode = {2}, firstWidth = {3}, " +
                "firstHeightMode = {4}, firstHeight = {5}, secondWidthMode = {6}, " +
                "secondWidth = {7}, secondHeightMode = {8}, secondHeight = {9}, " +
                "expectedWidthMode = {10}, expectedWidth = {11}, expectedHeightMode = {12}, " +
                "expectedHeight = {13}"
        )
        fun data(): List<Array<Any>> {

            val tests = mutableListOf<Array<Any>>()

            // Verifies that when measurements are first AT_MOST and then EXACTLY, if the
            // children cause the RV to be smaller during AT_MOST, the children are laid out
            // again using the EXACTLY measurements.

            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    val expectedMode =
                        if (resultingWidth >= 500 && resultingHeight >= 1000) {
                            AT_MOST
                        } else {
                            EXACTLY
                        }
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            AT_MOST, 500, AT_MOST, 1000,
                            EXACTLY, 500, EXACTLY, 1000,
                            expectedMode, 500, expectedMode, 1000
                        )
                    )
                }
            }
            // Same as above but height is always EXACTLY.
            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    val expectedMode =
                        if (resultingWidth >= 500) {
                            AT_MOST
                        } else {
                            EXACTLY
                        }
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            AT_MOST, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000,
                            expectedMode, 500, EXACTLY, 1000
                        )
                    )
                }
            }
            // Same as above but width is always EXACTLY instead of height.
            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    val expectedMode =
                        if (resultingHeight >= 1000) {
                            AT_MOST
                        } else {
                            EXACTLY
                        }
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            EXACTLY, 500, AT_MOST, 1000,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, expectedMode, 1000
                        )
                    )
                }
            }

            // Verifies that when measurements are first UNSPECIFIED and then EXACTLY, the
            // EXACTLY measurements are always used, even if the children cause the RV already
            // be the same size as the EXACTLY measurements.
            //
            // This shouldn't actually be the case.  It should be that if the children cause the
            // RV to be smaller or larger during UNSPECIFIED, then the children are laid out again
            // using the EXACTLY measurements, but only then.  This is just a minor hit to
            // performance, but worth noting.

            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            UNSPECIFIED, 0, UNSPECIFIED, 0,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000
                        )
                    )
                }
            }
            // Same as above but height is always EXACTLY.
            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            UNSPECIFIED, 0, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000
                        )
                    )
                }
            }
            // Same as above but width is always EXACTLY instead of height.
            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            EXACTLY, 500, UNSPECIFIED, 0,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000
                        )
                    )
                }
            }

            // Verifies same stuff as above, but first measure is UNSPECIFIED for width and
            // AT_MOST for height.
            //
            // Just like above, this behavior could be improved to be more efficient where if the
            // children end up getting measured to be the same size as the EXACTLY measurements,
            // then the EXACTLY measurements can be totally skipped.

            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            UNSPECIFIED, 0, AT_MOST, 1000,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000
                        )
                    )
                }
            }

            // Verifies same stuff as above, but first measure is AT_MOST for width and UNSPECIFIED
            // for height.
            //
            // Just like above, this behavior could be improved to be more efficient where if the
            // children end up getting measured to be the same size as the EXACTLY measurements,
            // then the EXACTLY measurements can be totally skipped.

            listOf(499, 500, 501).forEach { resultingWidth ->
                listOf(999, 1000, 1001).forEach { resultingHeight ->
                    tests.add(
                        arrayOf(
                            resultingWidth, resultingHeight,
                            AT_MOST, 500, UNSPECIFIED, 0,
                            EXACTLY, 500, EXACTLY, 1000,
                            EXACTLY, 500, EXACTLY, 1000
                        )
                    )
                }
            }

            return tests
        }
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mRecyclerView = RecyclerView(ApplicationProvider.getApplicationContext())
        mMockLayoutManager = MockLayoutManager()
        mRecyclerView.layoutManager = mMockLayoutManager
        mRecyclerView.adapter = MockAdapter()
    }

    @Test
    fun autoMeasure_test() {
        mMockLayoutManager.measurementsToReport(childWidth, childHeight)
        mRecyclerView.measure(
            firstWidthMode or firstWidth,
            firstHeightMode or firstHeight
        )
        mRecyclerView.measure(
            secondWidthMode or secondWidth,
            secondHeightMode or secondHeight
        )
        mRecyclerView.layout(0, 0, secondWidth, secondHeight)
        verifyMeasureSpecsAtLastLayoutChildren(
            expectedWidthMode, expectedWidth,
            expectedHeightMode, expectedHeight
        )
    }

    private fun verifyMeasureSpecsAtLastLayoutChildren(
        widthMeasureMode: Int,
        width: Int,
        heightMeasureMode: Int,
        height: Int
    ) {
        Assert.assertEquals(
            widthMeasureMode.toLong(),
            mMockLayoutManager.mWidthModeAtLastLayoutChildren.toLong()
        )
        Assert.assertEquals(
            width.toLong(),
            mMockLayoutManager.mWidthAtLastLayoutChildren.toLong()
        )
        Assert.assertEquals(
            heightMeasureMode.toLong(),
            mMockLayoutManager.mHeightModeAtLastLayoutChildren.toLong()
        )
        Assert.assertEquals(
            height.toLong(),
            mMockLayoutManager.mHeightAtLastLayoutChildren.toLong()
        )
    }

    internal class MockLayoutManager :
        RecyclerView.LayoutManager() {
        private var mTotalWidthOfAllChildrenToReport = 0
        private var mTotalHeightOfAllChildrenToReport = 0
        var mWidthAtLastLayoutChildren = 0
        var mHeightAtLastLayoutChildren = 0
        var mWidthModeAtLastLayoutChildren = 0
        var mHeightModeAtLastLayoutChildren = 0

        fun measurementsToReport(width: Int, height: Int) {
            mTotalWidthOfAllChildrenToReport = width
            mTotalHeightOfAllChildrenToReport = height
        }

        override fun isAutoMeasureEnabled(): Boolean {
            return true
        }

        override fun onLayoutChildren(
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State
        ) {
            mWidthModeAtLastLayoutChildren = widthMode
            mWidthAtLastLayoutChildren = width
            mHeightModeAtLastLayoutChildren = heightMode
            mHeightAtLastLayoutChildren = height
            removeAndRecycleAllViews(recycler)
            for (i in 0..9) {
                addView(recycler.getViewForPosition(i))
            }
        }

        public override fun setMeasureSpecs(wSpec: Int, hSpec: Int) {
            super.setMeasureSpecs(wSpec, hSpec)
        }

        override fun getDecoratedBoundsWithMargins(view: View, outBounds: Rect) {
            outBounds.left = 0
            outBounds.top = 0
            outBounds.right = mTotalWidthOfAllChildrenToReport
            outBounds.bottom = mTotalHeightOfAllChildrenToReport
        }

        override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
            return RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    internal class MockAdapter : RecyclerView.Adapter<MockViewHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MockViewHolder {
            return MockViewHolder(
                TextView(
                    parent.context
                )
            )
        }

        override fun onBindViewHolder(
            holder: MockViewHolder,
            position: Int
        ) {
        }

        override fun getItemCount(): Int {
            return 10
        }
    }

    internal class MockViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
}
