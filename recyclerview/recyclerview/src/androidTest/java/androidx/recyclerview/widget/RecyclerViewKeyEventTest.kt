/*
 * Copyright 2022 The Android Open Source Project
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

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewKeyEventTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val mActivityTestRule = androidx.test.rule.ActivityTestRule(TestContentViewActivity::class.java)

    private val viewHeight = 1024
    private val viewWidth = 1024
    private val context = mActivityTestRule.activity
    private val testItemCount = 10

    enum class InitialPosition { FIRST, LAST }

    private fun setupRecyclerViewAndScrollByKeyEvent(
        viewHeight: Int,
        initialPosition: InitialPosition,
        keyEvent: Int,
        layoutManager: RecyclerView.LayoutManager
    ): Pair<Int, Int> {
        val context = mActivityTestRule.activity
        val recyclerView = RecyclerView(context)

        recyclerView.layoutParams = ViewGroup.LayoutParams(viewWidth, viewHeight)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ) = object : RecyclerView.ViewHolder(
                TextView(parent.context).apply {
                    minWidth = 1024
                    minHeight = 256
                }
            ) {}

            override fun getItemCount() = testItemCount

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        }
        val scrollPos = when (initialPosition) {
            InitialPosition.FIRST -> 0
            InitialPosition.LAST -> testItemCount - 1
        }
        recyclerView.scrollToPosition(scrollPos)

        val testContentView = mActivityTestRule.activity.contentView
        testContentView.expectLayouts(1)
        mActivityTestRule.runOnUiThread { testContentView.addView(recyclerView) }
        testContentView.awaitLayouts(2)

        // Arrange
        val latch = CountDownLatch(1)
        var scrollHeight = 0
        var scrollWidth = 0

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    latch.countDown()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                scrollWidth += dx
                scrollHeight += dy
            }
        })

        // Act
        mActivityTestRule.runOnUiThread {
            recyclerView.dispatchKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, keyEvent)
            )
            recyclerView.dispatchKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, keyEvent)
            )
        }

        MatcherAssert.assertThat(latch.await(2, TimeUnit.SECONDS), CoreMatchers.`is`(true))
        return Pair(scrollWidth, scrollHeight)
    }

    @Test
    fun vertical_pageDown() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_PAGE_DOWN, layoutManager)
        // PageUp should scroll down one-page i.e. view height.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(viewHeight))
    }

    @Test
    fun vertical_pageUp() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_PAGE_UP, layoutManager)
        // PageUp should scroll up one-page i.e. view height.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(-viewHeight))
    }

    @Test
    fun horizontal_pageUp() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_PAGE_UP, layoutManager)
        // No scroll for horizontal layout.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(-viewWidth))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
    }

    @Test
    fun horizontal_pageDown() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_PAGE_DOWN, layoutManager)
        // No scroll for horizontal layout.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(viewWidth))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
    }

    @Test
    fun vertical_home() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_MOVE_HOME, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.not(0))
        MatcherAssert.assertThat(layoutManager.findFirstCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(0))
    }

    @Test
    fun vertical_end() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_MOVE_END, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.not(0))
        MatcherAssert.assertThat(layoutManager.findLastCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(testItemCount - 1))
    }

    @Test
    fun horizontal_home() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_MOVE_HOME, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.not(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(layoutManager.findFirstCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(0))
    }

    @Test
    fun horizontal_end() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_MOVE_END, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.not(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(layoutManager.findLastCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(testItemCount - 1))
    }

    @Test
    fun vertical_pageDown_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_PAGE_DOWN, layoutManager)
        // PageUp should scroll down one-page i.e. view height.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(viewHeight))
    }

    @Test
    fun vertical_pageUp_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_PAGE_UP, layoutManager)
        // PageUp should scroll up one-page i.e. view height.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(-viewHeight))
    }

    @Test
    fun horizontal_pageUp_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_PAGE_UP, layoutManager)
        // No scroll for horizontal layout.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(-viewWidth))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
    }

    @Test
    fun horizontal_pageDown_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_PAGE_DOWN, layoutManager)
        // No scroll for horizontal layout.
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(viewWidth))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
    }

    @Test
    fun vertical_home_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_MOVE_HOME, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.not(0))
        MatcherAssert.assertThat(layoutManager.findLastCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(testItemCount - 1))
    }

    @Test
    fun vertical_end_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_MOVE_END, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.not(0))
        MatcherAssert.assertThat(layoutManager.findFirstCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(0))
    }

    @Test
    fun horizontal_home_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.FIRST, KeyEvent.KEYCODE_MOVE_HOME, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.not(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(layoutManager.findFirstCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(testItemCount - 1))
    }

    @Test
    fun horizontal_end_reversed() {
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, true)
        val (scrollWidth, scrollHeight) = setupRecyclerViewAndScrollByKeyEvent(
            viewHeight, InitialPosition.LAST, KeyEvent.KEYCODE_MOVE_END, layoutManager)
        MatcherAssert.assertThat(scrollWidth, CoreMatchers.not(0))
        MatcherAssert.assertThat(scrollHeight, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(layoutManager.findFirstCompletelyVisibleItemPosition(),
            CoreMatchers.`is`(0))
    }

    @Test
    fun dispatchKeyEvent_withNoLayoutManager_doesNotCrash() {
        val context = mActivityTestRule.activity
        val recyclerView = RecyclerView(context)

        recyclerView.layoutParams = ViewGroup.LayoutParams(viewWidth, viewHeight)
        val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN)
        recyclerView.dispatchKeyEvent(event)
    }
}
