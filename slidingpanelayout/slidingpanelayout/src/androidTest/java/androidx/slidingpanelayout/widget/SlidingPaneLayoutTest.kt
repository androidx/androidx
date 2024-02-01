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

package androidx.slidingpanelayout.widget

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.View.MeasureSpec.EXACTLY
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.MeasureCountingView
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.isTwoPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SlidingPaneLayoutTest {

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    @Test
    fun testLayoutRoot() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }
    }

    @Test
    fun testLayoutWidthSpecExact() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutWidthSpecAtMost() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutWidthSpecUnspecific() {
        TestActivity.onActivityCreated = { activity ->
            val container = LinearLayout(activity)
            val sideButton = Button(activity).apply { text = "button" }
            container.addView(sideButton, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1F))
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_fold_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                LinearLayout.LayoutParams(0, MATCH_PARENT, 1F)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    @Test
    fun testLayoutHeightSpecExact() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check((ViewAssertions.matches(not(isTwoPane()))))
        }
    }

    @Test
    fun testLayoutHeightSpecAtMost() {
        TestActivity.onActivityCreated = { activity ->
            val container = FrameLayout(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(not(isTwoPane())))
        }
    }

    @Test
    fun testLayoutHeightSpecUnspecific() {
        TestActivity.onActivityCreated = { activity ->
            val container = ScrollView(activity)
            val slidingPaneLayout = activity.layoutInflater.inflate(
                R.layout.activity_test_layout, null, false
            )
            container.addView(
                slidingPaneLayout,
                ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            activity.setContentView(container)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_layout))
                .check(ViewAssertions.matches(not(isTwoPane())))
        }
    }

    @Test
    fun testRemoveDetailView() {
        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                val slidingPaneLayout = findViewById<SlidingPaneLayout>(R.id.sliding_pane_layout)
                assertThat(slidingPaneLayout.childCount).isEqualTo(2)
                val detailView = findViewById<View>(R.id.detail_pane)
                runOnUiThread {
                    slidingPaneLayout.removeView(detailView)
                }
                assertThat(slidingPaneLayout.childCount).isEqualTo(1)
            }
        }
    }

    @Test
    fun testSingleLayoutPassLpWidthAndWeight() {
        testSingleLayoutPass(
            SlidingPaneLayout.LayoutParams(100, MATCH_PARENT),
            SlidingPaneLayout.LayoutParams(0, MATCH_PARENT).apply {
                weight = 1f
            }
        )
    }

    @Test
    fun testSingleLayoutPassLpWidthAndMatch() {
        testSingleLayoutPass(
            SlidingPaneLayout.LayoutParams(100, MATCH_PARENT),
            SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        )
    }

    @Test
    fun testSingleLayoutPassMinWidthAndMatch() {
        testSingleLayoutPass(
            SlidingPaneLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT),
            SlidingPaneLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        ) {
            minimumWidth = 100
        }
    }
    @Test
    fun testSingleLayoutPassMinWidthAndWeight() {
        testSingleLayoutPass(
            SlidingPaneLayout.LayoutParams(WRAP_CONTENT, MATCH_PARENT),
            SlidingPaneLayout.LayoutParams(0, MATCH_PARENT).apply { weight = 1f }
        ) {
            minimumWidth = 100
        }
    }

    @Test
    fun userResizingConfiguration() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val view = SlidingPaneLayout(context)
        val drawable = object : Drawable() {
            var stateChanged = false
                private set

            override fun draw(canvas: Canvas) {}
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            @Suppress("DeprecatedCallableAddReplaceWith")
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

            override fun isStateful(): Boolean = true
            override fun onStateChange(state: IntArray): Boolean {
                stateChanged = true
                return true
            }
        }

        // Precondition - this should be false for a detached view
        assertWithMessage("isSlideable")
            .that(view.isSlideable)
            .isFalse()

        view.setUserResizingDividerDrawable(drawable)
        assertWithMessage("drawable state changed")
            .that(drawable.stateChanged)
            .isTrue()

        assertWithMessage("isUserResizable with drawable but not enabled")
            .that(view.isUserResizable)
            .isFalse()

        view.isUserResizingEnabled = true

        assertWithMessage("isUserResizable with drawable and enabled")
            .that(view.isUserResizable)
            .isTrue()

        view.setUserResizingDividerDrawable(null)

        assertWithMessage("isUserResizable with null drawable and enabled")
            .that(view.isUserResizable)
            .isFalse()
    }

    @Test
    fun userResizingConfigurationInflated() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.user_resize_config, null) as SlidingPaneLayout
        assertWithMessage("isUserResizingEnabled")
            .that(view.isUserResizingEnabled)
            .isTrue()
        assertWithMessage("isUserResizable")
            .that(view.isUserResizable)
            .isTrue()
    }
}

private fun View.measureAndLayout(
    width: Int,
    height: Int
) {
    measure(
        MeasureSpec.makeMeasureSpec(width, EXACTLY),
        MeasureSpec.makeMeasureSpec(height, EXACTLY)
    )
    layout(0, 0, measuredWidth, measuredHeight)
}

private fun testSingleLayoutPass(
    firstLayoutParams: SlidingPaneLayout.LayoutParams,
    secondLayoutParams: SlidingPaneLayout.LayoutParams,
    configFirst: MeasureCountingView.() -> Unit = {}
) {
    val context = InstrumentationRegistry.getInstrumentation().context
    val firstChild = MeasureCountingView(context).apply(configFirst)
    val secondChild = MeasureCountingView(context)
    val spl = SlidingPaneLayout(context).apply {
        isOverlappingEnabled = false
        addView(firstChild, firstLayoutParams)
        addView(secondChild, secondLayoutParams)
    }

    spl.measureAndLayout(300, 300)

    firstChild.assertReportingMeasureCallTraces {
        assertWithMessage("first child measure count")
            .that(measureCount)
            .isEqualTo(1)
    }
    secondChild.assertReportingMeasureCallTraces {
        assertWithMessage("second child measure count")
            .that(measureCount)
            .isEqualTo(1)
    }
}
