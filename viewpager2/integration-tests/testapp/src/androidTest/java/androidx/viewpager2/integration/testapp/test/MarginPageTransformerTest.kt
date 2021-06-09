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

package androidx.viewpager2.integration.testapp.test

import android.util.TypedValue
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.viewpager2.integration.testapp.PageTransformerActivity
import androidx.viewpager2.integration.testapp.R
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val LABEL_NONE = "None"
private const val LABEL_50_PX = "Margin 50px"
private const val LABEL_32_DP = "Margin 32dp"

@LargeTest
@RunWith(AndroidJUnit4::class)
class MarginPageTransformerTest :
    BaseTest<PageTransformerActivity>(PageTransformerActivity::class.java) {

    @Test
    fun testMargin_offscreenLimit_noChange() {
        testMargin(null)
    }

    @FlakyTest(bugId = 190611098)
    @Test
    fun testMargin_offscreenLimit_default() {
        testMargin(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT)
    }

    @Ignore("Test logic needs updating (visible pages detection).") // TODO: re-enable
    @Test
    fun testMargin_offscreenLimit_1() {
        testMargin(1)
    }

    @Ignore("Test logic needs updating (visible pages detection).") // TODO: re-enable
    @Test
    fun testMargin_offscreenLimit_4() {
        testMargin(4)
    }

    // TODO: break down / refactor
    /**
     * @param offscreenLimit <code>null</code> for no change. Otherwise an explicit value.
     */
    private fun testMargin(offscreenLimit: Int?) {

        // given
        setOffscreenLimit(offscreenLimit)
        val recyclerView = viewPager.getChildAt(0) as RecyclerView
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager

        val swipeCount = 3
        assertThat(viewPager.adapter!!.itemCount, greaterThan(swipeCount))

        var marginPx: Double = Double.MIN_VALUE
        var firstPass = true

        val height = viewPager.measuredHeight.toDouble()
        val width = viewPager.measuredWidth.toDouble()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                // then
                val pageA = layoutManager.getChildAt(layoutManager.findFirstVisibleItemPosition())
                val pageB = layoutManager.getChildAt(layoutManager.findLastVisibleItemPosition())
                if (pageA == null || pageB == null || pageA == pageB) return

                if (firstPass) { // first pass is before a transformer has a chance to be applied
                    firstPass = false
                    return
                }

                when (viewPager.orientation) {
                    ORIENTATION_HORIZONTAL -> {
                        // TODO: move assertions from UiThread to TestThread
                        assertThat(Math.abs(pageB.x - pageA.x) - width, closeTo(marginPx, 1.0))
                        assertThat(pageA.translationY, equalTo(0f))
                        assertThat(pageB.translationY, equalTo(0f))
                    }
                    ORIENTATION_VERTICAL -> {
                        assertThat(pageB.y - pageA.y - height, closeTo(marginPx, 1.0))
                        assertThat(pageA.translationX, equalTo(0f))
                        assertThat(pageB.translationX, equalTo(0f))
                    }
                    else -> throw IllegalArgumentException()
                }
            }
        })

        // when
        listOf(LABEL_NONE, LABEL_50_PX, LABEL_32_DP).forEach { transformer ->
            listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).forEach { orientation ->
                firstPass = true
                marginPx = selectPageTransformer(transformer)
                selectOrientation(orientation)
                repeat(swipeCount) { swipeToNextPage() }
                repeat(swipeCount) { swipeToPreviousPage() }
            }
        }
    }

    private fun setOffscreenLimit(offscreenLimit: Int?) {
        if (offscreenLimit != null) {
            // TODO: consider replacing by activityTestRule.runOnUiThread
            val latch = CountDownLatch(1)
            viewPager.post {
                viewPager.offscreenPageLimit = offscreenLimit
                latch.countDown()
            }
            assertThat(latch.await(5, TimeUnit.SECONDS), equalTo(true))
        }

        val expected = offscreenLimit ?: ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
        assertThat(viewPager.offscreenPageLimit, equalTo(expected))
    }

    /** @return margin in px */
    private fun selectPageTransformer(marginLabel: String): Double {
        onView(ViewMatchers.withId(R.id.transformer_spinner)).perform(click())
        onData(CoreMatchers.equalTo(marginLabel)).perform(click())
        return marginLabelToPx(marginLabel)
    }

    private fun marginLabelToPx(label: String): Double {
        return when (label) {
            LABEL_NONE -> 0.0
            LABEL_50_PX -> 50.0
            LABEL_32_DP -> TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                32f,
                viewPager.resources.displayMetrics
            ).toDouble()
            else -> throw IllegalArgumentException()
        }
    }

    override val layoutId: Int
        get() = R.id.view_pager
}
