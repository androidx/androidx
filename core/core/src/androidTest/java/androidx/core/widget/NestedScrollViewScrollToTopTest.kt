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

package androidx.core.widget

import android.view.View
import android.view.ViewGroup
import androidx.core.app.TestActivity
import androidx.core.test.R
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NestedScrollViewScrollToTopTest {

    @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var child: View

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity ->
            nestedScrollView =
                NestedScrollView(activity).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
        }
    }

    @Test
    fun testXmlInflation() {
        activityRule.scenario.onActivity { activity ->
            activity.setContentView(R.layout.nested_scroll_view_scrolls_to_top)

            val undefined = activity.findViewById<NestedScrollView>(R.id.scrollToTopUndefined)
            assertTrue(
                "Incorrect default isScrollToTopEnabled value",
                undefined.isScrollToTopEnabled,
            )

            val scrollsToTopTrue = activity.findViewById<NestedScrollView>(R.id.scrollToTopTrue)
            assertTrue(
                "Incorrect explicit isScrollToTopEnabled value",
                scrollsToTopTrue.isScrollToTopEnabled,
            )

            val scrollsToTopFalse = activity.findViewById<NestedScrollView>(R.id.scrollToTopFalse)
            assertFalse(
                "Incorrect explicit isScrollToTopEnabled value",
                scrollsToTopFalse.isScrollToTopEnabled,
            )
        }
    }

    @Test
    fun testAccessScrollToTopEnabled() {
        assertTrue("Default should be true", nestedScrollView.isScrollToTopEnabled)

        activityRule.scenario.onActivity {
            nestedScrollView.isScrollToTopEnabled = true
            assertTrue(nestedScrollView.isScrollToTopEnabled)

            nestedScrollView.isScrollToTopEnabled = false
            assertFalse(nestedScrollView.isScrollToTopEnabled)
        }
    }

    @Test
    fun testOnScrollToTop_scrollsToTop() {
        val childHeight = 5000
        setupNestedScrollView(childHeight = childHeight, enabled = true)

        // Scroll down
        activityRule.scenario.onActivity { nestedScrollView.fullScroll(View.FOCUS_DOWN) }
        assertTrue("Should be scrolled down", nestedScrollView.scrollY > 0)

        // Scroll to top
        waitForScrollToTop {
            val consumed = nestedScrollView.onScrollToTop(0)
            assertTrue("Should consume event when scrolled down and enabled", consumed)
        }

        assertEquals("Should be at top", 0, nestedScrollView.scrollY)
    }

    @Test
    fun testOnScrollToTop_ignoredWhenDisabled() {
        val childHeight = 5000
        setupNestedScrollView(childHeight = childHeight, enabled = false)

        // Scroll down
        activityRule.scenario.onActivity { nestedScrollView.fullScroll(View.FOCUS_DOWN) }
        assertTrue("Should be scrolled down", nestedScrollView.scrollY > 0)

        val startScrollY = nestedScrollView.scrollY

        var consumed = false
        activityRule.scenario.onActivity { consumed = nestedScrollView.onScrollToTop(0) }

        assertFalse("Should not consume event when disabled", consumed)
        assertEquals("Scroll position should not change", startScrollY, nestedScrollView.scrollY)
    }

    @Test
    fun testOnScrollToTop_ignoredWhenAlreadyAtTop() {
        val childHeight = 5000
        setupNestedScrollView(childHeight = childHeight, enabled = true)

        assertEquals(0, nestedScrollView.scrollY)

        var consumed = false
        activityRule.scenario.onActivity { consumed = nestedScrollView.onScrollToTop(0) }

        assertFalse("Should not consume event when already at top", consumed)
    }

    private fun setupNestedScrollView(childHeight: Int, enabled: Boolean) {
        activityRule.scenario.onActivity { activity ->
            nestedScrollView.isScrollToTopEnabled = enabled

            child = View(activity).apply { minimumHeight = childHeight }
            nestedScrollView.addView(child)
            activity.setContentView(nestedScrollView)
        }
    }

    private fun waitForScrollToTop(block: () -> Unit) {
        val scrollToTopLatch = CountDownLatch(1)
        activityRule.scenario.onActivity {
            nestedScrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
                    if (scrollY == 0) {
                        scrollToTopLatch.countDown()
                    }
                }
            )
            block()
        }
        assertTrue(
            "Timed out waiting for scroll to top",
            scrollToTopLatch.await(5, TimeUnit.SECONDS),
        )
    }
}
