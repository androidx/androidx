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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val RV_WIDTH = 500
private const val RV_HEIGHT = 500
private const val ITEM_WIDTH = 500
private const val ITEM_HEIGHT = 200
private const val NUM_ITEMS = 100

// TODO: This probably isn't a small test
@LargeTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewSmoothScrollToPositionTest {

    private lateinit var recyclerView: RecyclerView
    private lateinit var testContentView: TestContentView

    @get:Rule
    val mActivityTestRule = ActivityTestRule(TestContentViewActivity::class.java)

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val context = mActivityTestRule.activity

        recyclerView = RecyclerView(context)

        recyclerView.layoutParams = ViewGroup.LayoutParams(RV_WIDTH, RV_HEIGHT)
        recyclerView.setBackgroundColor(0x7FFF0000)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = MyAdapter()

        testContentView = mActivityTestRule.activity.contentView
        testContentView.expectLayouts(1)
        mActivityTestRule.runOnUiThread { testContentView.addView(recyclerView) }
        testContentView.awaitLayouts(2)
    }

    @Test
    @Throws(Throwable::class)
    fun smoothScrollToPosition_calledDuringScrollJustBeforeStop_scrollStateCallbacksCorrect() {
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
                    if (it.bottom == RV_HEIGHT) {
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
}

private fun ViewGroup.findChildWithTag(tag: Int): View? {
    for (i in 0 until this.childCount) {
        this.getChildAt(i).also {
            if (it.tag == tag) return it
        }
    }
    return null
}

private class MyAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object : RecyclerView.ViewHolder(
            TextView(parent.context).apply {
                minWidth = ITEM_WIDTH
                minHeight = ITEM_HEIGHT
            }
        ) {}

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder.itemView as TextView).apply {
            text = Integer.toString(position)
            tag = position
        }
    }

    override fun getItemCount() = NUM_ITEMS
}