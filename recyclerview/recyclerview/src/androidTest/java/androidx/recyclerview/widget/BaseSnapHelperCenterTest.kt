/*
 * Copyright 2019 The Android Open Source Project
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
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Small integration test base that helps verify that [SnapHelper]s that implement "center" snapping
 * define "center" correctly.
 *
 * For now, all the test actually does is verify that [SnapHelper.calculateDistanceToFinalSnap] and
 * [SnapHelper.findSnapView] always take padding into account when determining what "center" is and
 * that the value of [RecyclerView.getClipToPadding] is irrelevant in making that determination.
 *
 * The test sets up padding on the RecyclerView such that if the padding were ignored when
 * clipToPadding is false, the results of the tests would be wrong.
 *
 * The test tests in both orientations, where padding is added to either, both, or neither side of
 * the RV's orientation, and turns clipToPadding on and off.
 */
@RunWith(Parameterized::class)
@LargeTest
abstract class BaseSnapHelperCenterTest(
    private val vertical: Boolean,
    startPadded: Boolean,
    endPadded: Boolean,
    private val clipToPadding: Boolean,
) {
    abstract val snapHelper: SnapHelper

    private val numItems = 3
    private val mainAxisItemSize = 100
    private val startPadding = if (startPadded) 200 else 0
    private val endPadding = if (endPadded) 200 else 0
    private val mainAxisRvSize = numItems * mainAxisItemSize + startPadding + endPadding
    private val crossAxisRvSize = 1000

    private val rvWidth = if (vertical) crossAxisRvSize else mainAxisRvSize
    private val rvHeight = if (vertical) mainAxisRvSize else crossAxisRvSize
    private val rvPaddingLeft = if (vertical) 0 else startPadding
    private val rvPaddingTop = if (vertical) startPadding else 0
    private val rvPaddingRight = if (vertical) 0 else endPadding
    private val rvPaddingBottom = if (vertical) endPadding else 0

    private lateinit var recyclerView: RecyclerView

    @Before
    fun setup() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create views

        recyclerView = RecyclerView(context)
        recyclerView.minimumWidth = rvWidth
        recyclerView.minimumHeight = rvHeight
        recyclerView.setPadding(rvPaddingLeft, rvPaddingTop, rvPaddingRight, rvPaddingBottom)
        recyclerView.clipToPadding = clipToPadding

        // Setup RecyclerView
        val orientation = if (vertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
        recyclerView.layoutManager = LinearLayoutManager(context, orientation, false)
        recyclerView.adapter = TestAdapter(context, mainAxisItemSize, numItems, vertical)

        //  Measure and layout
        val measureSpecWidth = View.MeasureSpec.makeMeasureSpec(rvWidth, View.MeasureSpec.EXACTLY)
        val measureSpecHeight = View.MeasureSpec.makeMeasureSpec(rvHeight, View.MeasureSpec.EXACTLY)
        recyclerView.measure(measureSpecWidth, measureSpecHeight)
        recyclerView.layout(0, 0, rvWidth, rvHeight)

        snapHelper.attachToRecyclerView(recyclerView)
    }

    @Test
    fun calculateDistanceToFinalSnap_isCorrect() {
        for (i in 0 until numItems) {

            val mainAxisOffset = -mainAxisItemSize + i * mainAxisItemSize
            val xOffset = if (vertical) 0 else mainAxisOffset
            val yOffset = if (vertical) mainAxisOffset else 0

            val actualResult =
                snapHelper.calculateDistanceToFinalSnap(
                    recyclerView.layoutManager!!,
                    recyclerView.getChildAt(i),
                )

            assertThat(actualResult, `is`(intArrayOf(xOffset, yOffset)))
        }
    }

    @Test
    fun findCenterView_isCorrect() {
        val actualResult = snapHelper.findSnapView(recyclerView.layoutManager!!)
        assertThat(actualResult, `is`(recyclerView.getChildAt(1)))
    }

    companion object {

        private const val ParameterNames =
            "vertical:{0},startPadded:{1},endPadded:{2},clipToPadding:{3}"
        private val trueFalse = booleanArrayOf(true, false)

        @JvmStatic
        val params: List<Array<Any>>
            @Parameterized.Parameters(name = ParameterNames)
            get() {
                val result = ArrayList<Array<Any>>()
                for (vertical in trueFalse) {
                    for (startPadding in trueFalse) {
                        for (endPadding in trueFalse) {
                            for (clipToPadding in trueFalse) {
                                result.add(
                                    arrayOf(vertical, startPadding, endPadding, clipToPadding)
                                )
                            }
                        }
                    }
                }
                return result
            }
    }
}

private class TestAdapter(
    private val context: Context,
    private val orientationItemSize: Int,
    private val itemCount: Int,
    private val orientationVertical: Boolean,
) : RecyclerView.Adapter<TestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val view = View(context)

        val width: Int
        val height: Int
        if (orientationVertical) {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = orientationItemSize
        } else {
            width = orientationItemSize
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        view.layoutParams = ViewGroup.LayoutParams(width, height)
        return TestViewHolder(view)
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {}

    override fun getItemCount() = itemCount
}

private class TestViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
