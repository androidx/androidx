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

package androidx.window.testing

import android.graphics.Rect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.FoldingFeature
import androidx.window.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.FoldingFeature.State.Companion.FLAT
import androidx.window.FoldingFeature.Type.Companion.FOLD
import androidx.window.FoldingFeature.Type.Companion.HINGE
import androidx.window.WindowMetricsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

public class DisplayFeatureTestingTest {

    @get:Rule
    public val activityRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_emptyWidthIsFold() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.create().computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val actual = FoldingFeature(activity = activity, state = FLAT, orientation = VERTICAL)
            val expected = FoldingFeature(Rect(center, 0, center, bounds.height()), FOLD, FLAT)
            assertEquals(expected, actual)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_boundsMatchOrientation() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.create().computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val actual = FoldingFeature(activity = activity, state = FLAT, orientation = HORIZONTAL)
            val expected = FoldingFeature(Rect(0, center, bounds.width(), center), FOLD, FLAT)
            assertEquals(expected, actual)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_nonEmptyWidthIsFold() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.create().computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val width = 20
            val actual = FoldingFeature(
                activity = activity,
                size = width,
                state = FLAT,
                orientation = VERTICAL
            )
            val expected = FoldingFeature(
                Rect(center - width / 2, 0, center + width / 2, bounds.height()),
                HINGE,
                FLAT
            )
            assertEquals(expected, actual)
        }
    }
}