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

import android.view.View
import androidx.slidingpanelayout.test.R
import androidx.slidingpanelayout.widget.helpers.TestActivity
import androidx.slidingpanelayout.widget.helpers.findViewById
import androidx.slidingpanelayout.widget.helpers.findViewX
import androidx.slidingpanelayout.widget.helpers.isTwoPane
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.google.common.truth.Truth.assertThat
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test views split on the fold
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FoldTest {

    @get:Rule
    val rule: WindowLayoutInfoPublisherRule = WindowLayoutInfoPublisherRule()

    @After
    fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    /**
     * Test split views in middle when fold vertically
     */
    @Test
    fun testFoldVertical() {
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                val testFeature = FoldingFeature(activity = this, orientation = VERTICAL)
                val info = WindowLayoutInfo(listOf(testFeature))
                rule.overrideWindowLayoutInfo(info)
                testFeature.bounds
            }
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
        }
    }

    /**
     * Test split views not applicable when fold horizontally.
     */
    @Test
    fun testFoldHorizontal() {
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(isTwoPane()))
            assertThat(findViewX(R.id.list_pane)).isLessThan(findViewX(R.id.detail_pane))
        }
    }

    /**
     * Test split views when fold pane is smaller than required min width
     */
    @Test
    fun testFoldExceedMinWidth() {
        val detailViewExtraWidth = 200
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
            val detailView = activity.findViewById<View>(R.id.detail_pane)
            detailView.minimumWidth = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
                .bounds
                .width() / 2 + detailViewExtraWidth
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            withActivity {
                val feature = FoldingFeature(activity = this, orientation = VERTICAL)
                val info = WindowLayoutInfo(listOf(feature))
                rule.overrideWindowLayoutInfo(info)
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
            }
            Espresso.onView(ViewMatchers.withId(R.id.sliding_pane_fold_layout))
                .check(ViewAssertions.matches(not(isTwoPane())))
        }
    }

    /**
     * Test layout updates when unfold a foldable device
     */
    @Test
    fun testUnfold() {
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            onActivity { activity ->
                val halfOpenFeature = FoldingFeature(
                    activity = activity,
                    state = HALF_OPENED,
                    orientation = VERTICAL
                )
                val flat = FoldingFeature(
                    activity = activity,
                    state = FLAT,
                    orientation = VERTICAL
                )
                val halfOpenInfo = WindowLayoutInfo(listOf(halfOpenFeature))
                val flatInfo = WindowLayoutInfo(listOf(flat))
                rule.overrideWindowLayoutInfo(halfOpenInfo)
                rule.overrideWindowLayoutInfo(flatInfo)
            }
            assertThat(findViewById(R.id.list_pane).width)
                .isLessThan(findViewById(R.id.detail_pane).width)
            assertThat(findViewX(R.id.list_pane)).isLessThan(findViewX(R.id.detail_pane))
        }
    }
}