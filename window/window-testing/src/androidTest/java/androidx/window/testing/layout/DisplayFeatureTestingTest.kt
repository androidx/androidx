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

package androidx.window.testing.layout

import android.graphics.Rect
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.layout.FoldingFeature.OcclusionType.Companion.FULL
import androidx.window.layout.FoldingFeature.OcclusionType.Companion.NONE
import androidx.window.layout.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.FoldingFeature.State.Companion.FLAT
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.TestActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val actual = FoldingFeature(activity = activity, state = FLAT, orientation = VERTICAL)
            val expectedBounds = Rect(center, 0, center, bounds.height())
            assertEquals(expectedBounds.left, actual.bounds.left)
            assertEquals(expectedBounds.right, actual.bounds.right)
            assertEquals(expectedBounds.top, actual.bounds.top)
            assertEquals(expectedBounds.bottom, actual.bounds.bottom)
            assertFalse(actual.isSeparating)
            assertEquals(NONE, actual.occlusionType)
            assertEquals(VERTICAL, actual.orientation)
            assertEquals(FLAT, actual.state)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_boundsMatchOrientation() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerY()
            val actual = FoldingFeature(activity = activity, state = FLAT, orientation = HORIZONTAL)
            val expectedBounds = Rect(0, center, bounds.width(), center)
            assertEquals(expectedBounds.left, actual.bounds.left)
            assertEquals(expectedBounds.right, actual.bounds.right)
            assertEquals(expectedBounds.top, actual.bounds.top)
            assertEquals(expectedBounds.bottom, actual.bounds.bottom)
            assertFalse(actual.isSeparating)
            assertEquals(NONE, actual.occlusionType)
            assertEquals(HORIZONTAL, actual.orientation)
            assertEquals(FLAT, actual.state)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_centerMatchOrientation_vertical() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val actual = FoldingFeature(activity = activity, orientation = VERTICAL)
            val expectedBounds = Rect(center, 0, center, bounds.height())
            assertEquals(expectedBounds.left, actual.bounds.left)
            assertEquals(expectedBounds.right, actual.bounds.right)
            assertEquals(expectedBounds.top, actual.bounds.top)
            assertEquals(expectedBounds.bottom, actual.bounds.bottom)
            assertEquals(NONE, actual.occlusionType)
            assertEquals(VERTICAL, actual.orientation)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_centerMatchOrientation_horizontal() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerY()
            val actual = FoldingFeature(activity = activity, orientation = HORIZONTAL)
            val expectedBounds = Rect(0, center, bounds.width(), center)
            assertEquals(expectedBounds.left, actual.bounds.left)
            assertEquals(expectedBounds.right, actual.bounds.right)
            assertEquals(expectedBounds.top, actual.bounds.top)
            assertEquals(expectedBounds.bottom, actual.bounds.bottom)
            assertEquals(NONE, actual.occlusionType)
            assertEquals(HORIZONTAL, actual.orientation)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    public fun testFold_nonEmptyWidthIsFold() {
        activityRule.scenario.onActivity { activity ->
            val metrics = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(activity)
            val bounds = metrics.bounds
            val center = bounds.centerX()
            val width = 20
            val actual = FoldingFeature(
                activity = activity,
                size = width,
                state = FLAT,
                orientation = VERTICAL
            )
            val expectedBounds = Rect(center - width / 2, 0, center + width / 2, bounds.height())
            assertEquals(expectedBounds.left, actual.bounds.left)
            assertEquals(expectedBounds.right, actual.bounds.right)
            assertEquals(expectedBounds.top, actual.bounds.top)
            assertEquals(expectedBounds.bottom, actual.bounds.bottom)
            assertTrue(actual.isSeparating)
            assertEquals(FULL, actual.occlusionType)
            assertEquals(VERTICAL, actual.orientation)
            assertEquals(FLAT, actual.state)
        }
    }
}