/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.sample

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.PositionAssertions.isBottomAlignedWith
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyAbove
import androidx.test.espresso.assertion.PositionAssertions.isCompletelyLeftOf
import androidx.test.espresso.assertion.PositionAssertions.isLeftAlignedWith
import androidx.test.espresso.assertion.PositionAssertions.isRightAlignedWith
import androidx.test.espresso.assertion.PositionAssertions.isTopAlignedWith
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SplitLayoutActivityTest {
    private val activityRule = ActivityScenarioRule(SplitLayoutActivity::class.java)
    private val publisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule
    val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(publisherRule).around(activityRule)
    }

    @Test
    fun testDeviceOpen_Flat() {
        activityRule.scenario.onActivity {
            val expected = TestWindowLayoutInfo(listOf())
            publisherRule.overrideWindowLayoutInfo(expected)
        }

        // Checks that the two views are overlapped if there's no FoldingFeature.
        onView(withId(R.id.start_layout)).check(isBottomAlignedWith(withId(R.id.end_layout)))
        onView(withId(R.id.start_layout)).check(isTopAlignedWith(withId(R.id.end_layout)))
        onView(withId(R.id.start_layout)).check(isLeftAlignedWith(withId(R.id.end_layout)))
        onView(withId(R.id.start_layout)).check(isRightAlignedWith(withId(R.id.end_layout)))
    }

    @Test
    fun testDeviceOpen_Vertical() {
        var isWindowBigEnoughForTest = false
        activityRule.scenario.onActivity { activity ->
            val feature = FoldingFeature(
                activity = activity,
                orientation = VERTICAL,
                state = HALF_OPENED
            )
            val expected = TestWindowLayoutInfo(listOf(feature))
            publisherRule.overrideWindowLayoutInfo(expected)

            val layout = activity.findViewById<LinearLayout>(R.id.rootLayout)
            val startView = activity.findViewById<View>(R.id.start_layout)
            val endView = activity.findViewById<View>(R.id.end_layout)

            isWindowBigEnoughForTest = isWindowBigEnough(feature, layout, startView, endView)
        }

        if (isWindowBigEnoughForTest) {
            // Checks that start_layout is on the left of end_layout with a vertical folding feature.
            // This requires to run the test on a big enough screen to fit both views on screen
            onView(withId(R.id.start_layout)).check(isCompletelyLeftOf(withId(R.id.end_layout)))
        } else {
            // Checks that the two views are overlapped if the test is running in a Window not
            // big enough to allow having the two views side by side.
            onView(withId(R.id.start_layout)).check(isBottomAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isTopAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isLeftAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isRightAlignedWith(withId(R.id.end_layout)))
        }
    }

    @Test
    fun testDeviceOpen_Horizontal() {
        var isWindowBigEnoughForTest = false

        activityRule.scenario.onActivity { activity ->
            val feature = FoldingFeature(
                activity = activity,
                orientation = HORIZONTAL,
                state = HALF_OPENED
            )
            val expected = TestWindowLayoutInfo(listOf(feature))
            publisherRule.overrideWindowLayoutInfo(expected)

            val layout = activity.findViewById<LinearLayout>(R.id.rootLayout)
            val startView = activity.findViewById<View>(R.id.start_layout)
            val endView = activity.findViewById<View>(R.id.end_layout)

            isWindowBigEnoughForTest = isWindowBigEnough(feature, layout, startView, endView)
        }

        if (isWindowBigEnoughForTest) {
            // Checks that start_layout is above of end_layout with a horizontal folding feature.
            // This requires to run the test on a big enough screen to fit both views on screen
            onView(withId(R.id.start_layout)).check(isCompletelyAbove(withId(R.id.end_layout)))
        } else {
            // Checks that the two views are overlapped if the test is running in a Window not
            // big enough to allow having the two views side by side.
            onView(withId(R.id.start_layout)).check(isBottomAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isTopAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isLeftAlignedWith(withId(R.id.end_layout)))
            onView(withId(R.id.start_layout)).check(isRightAlignedWith(withId(R.id.end_layout)))
        }
    }

    /**
     * Check if the Window is big enough to fit {@code startView} and {@code endView} in the two
     * display areas defined by the {@code feature} in the {@code layout}.
     * @return A Boolean that defines if the window is big enough.
     */
    private fun isWindowBigEnough(
        feature: FoldingFeature?,
        layout: ViewGroup?,
        startView: View?,
        endView: View?
    ): Boolean {
        if (feature == null || layout == null || startView == null || endView == null) {
            return false
        }

        // Calculate the area for view's content with padding
        with(layout) {
            val paddedWidth = width - paddingLeft - paddingRight
            val paddedHeight = height - paddingTop - paddingBottom

            val featureBounds = adjustFeaturePositionOffset(feature, this)
            if (feature.orientation == HORIZONTAL) { // Horizontal layout
                val topRect = Rect(
                    paddingLeft, paddingTop,
                    paddingLeft + paddedWidth, featureBounds.top
                )
                val bottomRect = Rect(
                    paddingLeft, featureBounds.bottom,
                    paddingLeft + paddedWidth, paddingTop + paddedHeight
                )

                if (measureAndCheckMinSize(topRect, startView) &&
                    measureAndCheckMinSize(bottomRect, endView)
                ) {
                    return true
                }
            } else if (feature.orientation == VERTICAL) { // Vertical layout
                val leftRect = Rect(
                    paddingLeft, paddingTop,
                    featureBounds.left, paddingTop + paddedHeight
                )
                val rightRect = Rect(
                    featureBounds.right, paddingTop,
                    paddingLeft + paddedWidth, paddingTop + paddedHeight
                )

                if (measureAndCheckMinSize(leftRect, startView) &&
                    measureAndCheckMinSize(rightRect, endView)
                ) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Measures a child view and sees if it will fit in the provided rect.
     * <p>Note: This method calls [View.measure] on the child view, which updates
     * its stored values for measured with and height. If the view will end up with different
     * values, it should be measured again.
     */
    private fun measureAndCheckMinSize(rect: Rect, childView: View): Boolean {
        val widthSpec =
            View.MeasureSpec.makeMeasureSpec(rect.width(), View.MeasureSpec.AT_MOST)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            rect.height(),
            View.MeasureSpec.AT_MOST
        )
        childView.measure(widthSpec, heightSpec)
        return childView.measuredWidthAndState and FrameLayout.MEASURED_STATE_TOO_SMALL == 0 &&
            childView.measuredHeightAndState and FrameLayout.MEASURED_STATE_TOO_SMALL == 0
    }
}
