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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DisplayFeatureTestingTest {

    @get:Rule
    val activityRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    private val windowBounds = Rect(0, 0, 320, 640)

    @Test
    fun testFold_emptyWidthIsFold() {
        val center = windowBounds.centerX()
        val actual =
            FoldingFeature(windowBounds = windowBounds, state = FLAT, orientation = VERTICAL)
        val expectedBounds = Rect(center, 0, center, windowBounds.height())

        assertEquals(expectedBounds, actual.bounds)
        assertFalse(actual.isSeparating)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(VERTICAL, actual.orientation)
        assertEquals(FLAT, actual.state)
    }

    @Test
    fun testFold_boundsMatchOrientation() {
        val bounds = Rect(0, 0, 320, 640)
        val center = bounds.centerY()
        val actual = FoldingFeature(windowBounds = bounds, state = FLAT, orientation = HORIZONTAL)
        val expectedBounds = Rect(0, center, bounds.width(), center)

        assertEquals(expectedBounds, actual.bounds)
        assertFalse(actual.isSeparating)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(HORIZONTAL, actual.orientation)
        assertEquals(FLAT, actual.state)
    }

    @Test
    fun testFold_centerMatchOrientation_vertical() {
        val center = windowBounds.centerX()
        val actual = FoldingFeature(windowBounds = windowBounds, orientation = VERTICAL)
        val expectedBounds = Rect(center, 0, center, windowBounds.height())

        assertEquals(expectedBounds, actual.bounds)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(VERTICAL, actual.orientation)
    }

    @Test
    fun testFold_centerMatchOrientation_horizontal() {
        val center = windowBounds.centerY()
        val actual = FoldingFeature(windowBounds = windowBounds, orientation = HORIZONTAL)
        val expectedBounds = Rect(0, center, windowBounds.width(), center)

        assertEquals(expectedBounds, actual.bounds)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(HORIZONTAL, actual.orientation)
    }

    @Test
    fun testFold_nonEmptyWidthIsFold() {
        val center = windowBounds.centerX()
        val width = 20
        val actual =
            FoldingFeature(
                windowBounds = windowBounds,
                size = width,
                state = FLAT,
                orientation = VERTICAL
            )
        val expectedBounds = Rect(center - width / 2, 0, center + width / 2, windowBounds.height())

        assertEquals(expectedBounds, actual.bounds)
        assertTrue(actual.isSeparating)
        assertEquals(FULL, actual.occlusionType)
        assertEquals(VERTICAL, actual.orientation)
        assertEquals(FLAT, actual.state)
    }

    @Test
    fun testFold_windowBoundsFromActivity() {
        activityRule.scenario.onActivity { activity ->
            val windowBounds =
                WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
            val actual = FoldingFeature(activity = activity)
            val expected = FoldingFeature(windowBounds = windowBounds)
            val expectedBounds = expected.bounds

            assertEquals(expectedBounds, actual.bounds)
            assertEquals(expected.isSeparating, actual.isSeparating)
            assertEquals(expected.occlusionType, actual.occlusionType)
            assertEquals(expected.orientation, actual.orientation)
            assertEquals(expected.state, actual.state)
        }
    }

    @Test
    fun testFold_windowBoundsAreRelative_vertical() {
        val bounds = Rect(200, 300, 400, 600)
        val center = bounds.centerX()
        val actual = FoldingFeature(windowBounds = bounds, orientation = VERTICAL)
        val expectedBounds = Rect(center, bounds.top, center, bounds.bottom)

        assertEquals(expectedBounds, actual.bounds)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(VERTICAL, actual.orientation)
    }

    @Test
    fun testFold_windowBoundsAreRelative_horizontal() {
        val bounds = Rect(200, 300, 400, 600)
        val center = bounds.centerY()
        val actual = FoldingFeature(windowBounds = bounds, orientation = HORIZONTAL)
        val expectedBounds = Rect(bounds.left, center, bounds.right, center)

        assertEquals(expectedBounds, actual.bounds)
        assertEquals(NONE, actual.occlusionType)
        assertEquals(HORIZONTAL, actual.orientation)
    }
}
