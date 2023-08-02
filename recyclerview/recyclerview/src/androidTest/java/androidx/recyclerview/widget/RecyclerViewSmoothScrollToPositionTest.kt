/*
 * Copyright 2018 The Android Open Source Project
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
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewSmoothScrollToPositionTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(TestContentViewActivity::class.java)

    @Test
    @Throws(Throwable::class)
    fun smoothScrollToPosition_calledDuringScrollJustBeforeStop_scrollStateCallbacksCorrect() {

        val recyclerView =
            setup(
                500 to 500,
                500 to 200,
                100
            )

        val called2ndTime = -1

        // Arrange

        val target = 3
        val log: MutableList<Int> = mutableListOf()
        val latch = CountDownLatch(1)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                log.add(newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    latch.countDown()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                recyclerView.findChildWithTag(target)?.let {
                    if (it.bottom == 500) {
                        log.add(called2ndTime)
                        recyclerView.smoothScrollToPosition(target)
                    }
                }
            }
        })

        // Act
        mActivityTestRule.runOnUiThread { recyclerView.smoothScrollToPosition(target) }
        assertThat(latch.await(2, TimeUnit.SECONDS), `is`(true))

        // Assert
        assertThat(log.size, `is`(3))
        assertThat(log[0], `is`(RecyclerView.SCROLL_STATE_SETTLING))
        assertThat(log[1], `is`(called2ndTime))
        assertThat(log[2], `is`(RecyclerView.SCROLL_STATE_IDLE))
    }

    @Test
    @Throws(Throwable::class)
    fun smoothScroll_whenSmoothScrollerStops_destinationReached() {

        // Arrange

        val calledOnStart = CountDownLatch(1)
        val calledOnStop = CountDownLatch(1)

        val layoutManager =
            object : LinearLayoutManager(mActivityTestRule.activity) {

                override fun smoothScrollToPosition(
                    recyclerView: RecyclerView,
                    state: RecyclerView.State,
                    position: Int
                ) {
                    val linearSmoothScroller: LinearSmoothScroller =
                        object : LinearSmoothScroller(recyclerView.context) {
                            override fun onStart() {
                                super.onStart()
                                calledOnStart.countDown()
                            }

                            override fun onStop() {
                                super.onStop()
                                calledOnStop.countDown()
                            }
                        }
                    linearSmoothScroller.targetPosition = position
                    startSmoothScroll(linearSmoothScroller)
                }
            }

        // We are going to traverse through 5 of 10 total screens worth of items to find the
        // target view.
        val itemHeight = 100
        val itemsPerScreen = 5
        val screensToTraverse = 5
        val totalScreens = 10

        val targetPosition = itemsPerScreen * screensToTraverse

        val recyclerView =
            setup(
                500 to itemHeight * itemsPerScreen,
                500 to itemHeight,
                itemsPerScreen * totalScreens,
                layoutManager = layoutManager
            )

        // Act

        BaseRecyclerViewInstrumentationTest.mActivityRule.runOnUiThread(
            Runnable {
                recyclerView.smoothScrollToPosition(
                    targetPosition
                )
            }
        )

        // Assert

        Assert.assertTrue(
            "onStart should be called quickly ",
            calledOnStart.await(2, TimeUnit.SECONDS)
        )
        Assert.assertTrue(
            "onStop should be called eventually",
            calledOnStop.await(30, TimeUnit.SECONDS)
        )

        // This needs to be run on the UI thread 1) due to inspecting the results of operations
        // (such as layout) that may occur after the latch is counted down, and 2) in order to
        // ensure that it doesn't run concurrently with operations on the UI thread that might
        // affect the state.
        mActivityTestRule.runOnUiThread {
            Assert.assertNotNull(
                "smoothScrollToPosition should succeed " +
                    "(first visible item: " + layoutManager.findFirstVisibleItemPosition() +
                    ", last visible item: " + layoutManager.findLastVisibleItemPosition() + ")",
                recyclerView.findViewHolderForLayoutPosition(targetPosition)
            )
        }
    }

    private fun setup(
        rvDimensions: Pair<Int, Int>,
        itemDimensions: Pair<Int, Int>,
        numItems: Int,
        context: Context = mActivityTestRule.activity,
        layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
    ): RecyclerView {

        val recyclerView = RecyclerView(context)

        recyclerView.layoutParams = ViewGroup.LayoutParams(rvDimensions.first, rvDimensions.second)
        recyclerView.setBackgroundColor(0x7FFF0000)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MyAdapter(itemDimensions, numItems)

        val testContentView = mActivityTestRule.activity.contentView
        testContentView.expectLayouts(1)
        mActivityTestRule.runOnUiThread { testContentView.addView(recyclerView) }
        testContentView.awaitLayouts(2)

        return recyclerView
    }
}

private fun ViewGroup.findChildWithTag(tag: Int): View? {
    for (i in 0 until this.childCount) {
        this.getChildAt(i).also {
            if (it.tag == tag) return it
        }
    }
    return null
}

private class MyAdapter(
    val itemDimensions: Pair<Int, Int>,
    val numItems: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object : RecyclerView.ViewHolder(
            TextView(parent.context).apply {
                minWidth = itemDimensions.first
                minHeight = itemDimensions.second
            }
        ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).apply {
            text = position.toString()
            tag = position
        }
    }

    override fun getItemCount() = numItems
}
