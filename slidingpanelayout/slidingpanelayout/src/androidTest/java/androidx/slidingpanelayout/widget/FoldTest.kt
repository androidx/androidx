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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.window.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.WindowLayoutInfo
import androidx.window.WindowMetricsCalculator
import androidx.window.testing.FoldingFeature
import androidx.window.testing.WindowLayoutInfoPublisherRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test views split on the fold
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
public class FoldTest {

    @get:Rule
    val rule: WindowLayoutInfoPublisherRule = WindowLayoutInfoPublisherRule()

    @After
    public fun tearDown() {
        TestActivity.onActivityCreated = {}
    }

    /**
     * Test split views in middle when fold vertically
     */
    @FlakyTest(bugId = 190609880)
    @Test
    public fun testFoldVertical() {
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val foldPosition = withActivity {
                val testFeature = FoldingFeature(activity = this, orientation = VERTICAL)
                val info = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(testFeature))
                    .build()
                rule.overrideWindowLayoutInfo(info)
                testFeature.bounds
            }
            assertThat(findViewById(R.id.list_pane).width)
                .isEqualTo(findViewById(R.id.detail_pane).width)
            assertThat(findViewX(R.id.detail_pane)).isEqualTo(foldPosition.left)
        }
    }

    /**
     * Test split views not applicable when fold horizontally.
     */
    @Test
    public fun testFoldHorizontal() {
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            assertThat(findViewById(R.id.list_pane).width)
                .isLessThan(findViewById(R.id.detail_pane).width)
            assertThat(findViewX(R.id.list_pane)).isLessThan(findViewX(R.id.detail_pane))
        }
    }

    /**
     * Test split views when fold pane is smaller than required min width
     */
    @FlakyTest(bugId = 190609880)
    @Test
    public fun testFoldExceedMinWidth() {
        val detailViewExtraWidth = 200
        TestActivity.onActivityCreated = { activity ->
            activity.setContentView(R.layout.activity_test_fold_layout)
            val detailView = activity.findViewById<View>(R.id.detail_pane)
            detailView.minimumWidth = WindowMetricsCalculator.create()
                .computeCurrentWindowMetrics(activity)
                .bounds
                .width() / 2 + detailViewExtraWidth
            val window = WindowMetricsCalculator.create()
                .computeCurrentWindowMetrics(activity).bounds
            detailView.minimumWidth = window.width() / 2 + detailViewExtraWidth
        }

        with(ActivityScenario.launch(TestActivity::class.java)) {
            val window = withActivity {
                val feature = FoldingFeature(activity = this, orientation = VERTICAL)
                val info = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(feature)).build()
                rule.overrideWindowLayoutInfo(info)
                WindowMetricsCalculator.create().computeCurrentWindowMetrics(this).bounds
            }
            assertThat(findViewById(R.id.detail_pane).width).isEqualTo(window.width())
        }
    }

    /**
     * Test layout updates when unfold a foldable device
     */
    @Test
    public fun testUnfold() {
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
                val halfOpenInfo = WindowLayoutInfo.Builder()
                    .setDisplayFeatures(listOf(halfOpenFeature)).build()
                val flatInfo = WindowLayoutInfo.Builder().setDisplayFeatures(listOf(flat)).build()
                rule.overrideWindowLayoutInfo(halfOpenInfo)
                rule.overrideWindowLayoutInfo(flatInfo)
            }
            assertThat(findViewById(R.id.list_pane).width)
                .isLessThan(findViewById(R.id.detail_pane).width)
            assertThat(findViewX(R.id.list_pane)).isLessThan(findViewX(R.id.detail_pane))
        }
    }
}