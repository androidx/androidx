/*
 * Copyright 2026 The Android Open Source Project
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

import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RecyclerViewScrollToTopTest : BaseRecyclerViewInstrumentationTest() {

    @Before
    fun setUp() {
        mRecyclerView =
            RecyclerView(activity).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }
        mActivityRule.runOnUiThread { activity.container.addView(mRecyclerView) }
    }

    @Test
    fun testAccessScrollToTopEnabled() {
        assertTrue("Default should be true", mRecyclerView.isScrollToTopEnabled)

        mRecyclerView.isScrollToTopEnabled = true
        assertTrue(mRecyclerView.isScrollToTopEnabled)

        mRecyclerView.isScrollToTopEnabled = false
        assertFalse(mRecyclerView.isScrollToTopEnabled)
    }

    @Test
    fun testOnScrollToTop_scrollsToTop() {
        val itemCount = 100
        setupRecyclerView(itemCount = itemCount, enabled = true)

        // Scroll to bottom
        scrollToPosition(itemCount - 1)
        waitForIdleScroll(mRecyclerView)
        assertTrue("Should be scrolled down", mRecyclerView.computeVerticalScrollOffset() > 0)

        mActivityRule.runOnUiThread {
            val consumed = mRecyclerView.onScrollToTop(0)
            assertTrue("Should consume event when scrolled down and enabled", consumed)
        }

        // Wait for the animation to start and then wait for it to finish
        waitForDraw(2)
        waitForIdleScroll(mRecyclerView)

        assertEquals("Should be at top", 0, mRecyclerView.computeVerticalScrollOffset())
    }

    @Test
    fun testOnScrollToTop_ignoredWhenDisabled() {
        val itemCount = 100
        setupRecyclerView(itemCount = itemCount, enabled = false)

        // Scroll to bottom
        scrollToPosition(itemCount - 1)
        waitForIdleScroll(mRecyclerView)

        val startScrollY = mRecyclerView.computeVerticalScrollOffset()
        assertTrue("Should be scrolled down", startScrollY > 0)

        mActivityRule.runOnUiThread {
            val consumed = mRecyclerView.onScrollToTop(0)
            assertFalse("Should not consume event when disabled", consumed)
        }

        assertEquals(
            "Scroll position should not change",
            startScrollY,
            mRecyclerView.computeVerticalScrollOffset(),
        )
    }

    @Test
    fun testOnScrollToTop_ignoredWhenAlreadyAtTop() {
        setupRecyclerView(itemCount = 100, enabled = true)

        scrollToPosition(0)
        waitForIdleScroll(mRecyclerView)
        assertEquals(0, mRecyclerView.computeVerticalScrollOffset())

        mActivityRule.runOnUiThread {
            val consumed = mRecyclerView.onScrollToTop(0)
            assertFalse("Should not consume event when already at top", consumed)
        }
    }

    private fun setupRecyclerView(itemCount: Int, enabled: Boolean) {
        val adapter = TestAdapter(itemCount)
        val lm = LinearLayoutManager(activity)
        mActivityRule.runOnUiThread {
            mRecyclerView.isScrollToTopEnabled = enabled
            mRecyclerView.layoutManager = lm
            mRecyclerView.adapter = adapter
        }
        instrumentation.waitForIdleSync()
    }
}
