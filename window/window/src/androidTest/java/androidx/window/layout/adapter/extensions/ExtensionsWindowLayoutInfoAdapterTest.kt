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

package androidx.window.layout.adapter.extensions

import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.TestActivity
import androidx.window.WindowTestUtils
import androidx.window.core.Bounds
import androidx.window.extensions.layout.FoldingFeature as OEMFoldingFeature
import androidx.window.extensions.layout.FoldingFeature.STATE_HALF_OPENED
import androidx.window.extensions.layout.FoldingFeature.TYPE_HINGE
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.HardwareFoldingFeature
import androidx.window.layout.HardwareFoldingFeature.Type.Companion.HINGE
import androidx.window.layout.TestFoldingFeatureUtil.invalidNonZeroFoldBounds
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetricsCalculatorCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ExtensionsWindowLayoutInfoAdapterTest {

    @get:Rule
    public val activityScenario: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun testTranslate_foldingFeature() {
        activityScenario.scenario.onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity)
            val bounds = windowMetrics.bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val oemFeature = OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_HALF_OPENED)
            val expected = HardwareFoldingFeature(Bounds(featureBounds), HINGE, HALF_OPENED)

            val actual = ExtensionsWindowLayoutInfoAdapter.translate(windowMetrics, oemFeature)

            assertEquals(expected, actual)
        }
    }

    @Ignore("b/249124046")
    @Test
    fun testTranslate_windowLayoutInfo() {
        activityScenario.scenario.onActivity { activity ->
            val bounds =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity).bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val oemFeature = OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_HALF_OPENED)
            val oemInfo = OEMWindowLayoutInfo(listOf(oemFeature))
            val localFeature = HardwareFoldingFeature(Bounds(featureBounds), HINGE, HALF_OPENED)
            val expected = WindowLayoutInfo(listOf(localFeature))

            val actual = ExtensionsWindowLayoutInfoAdapter.translate(activity, oemInfo)

            assertEquals(expected, actual)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Test
    fun testTranslate_windowLayoutInfoFromContext() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        activityScenario.scenario.onActivity { activity ->
            val bounds =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity).bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val oemFeature = OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_HALF_OPENED)
            val oemInfo = OEMWindowLayoutInfo(listOf(oemFeature))
            val localFeature = HardwareFoldingFeature(Bounds(featureBounds), HINGE, HALF_OPENED)
            val expected = WindowLayoutInfo(listOf(localFeature))

            val windowContext = WindowTestUtils.createOverlayWindowContext()

            val fromContext = ExtensionsWindowLayoutInfoAdapter.translate(windowContext, oemInfo)
            assertEquals(expected, fromContext)
        }
    }

    @Test
    fun testTranslate_foldingFeature_invalidType() {
        activityScenario.scenario.onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity)
            val bounds = windowMetrics.bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val oemFeature = OEMFoldingFeature(featureBounds, -1, STATE_HALF_OPENED)

            val actual = ExtensionsWindowLayoutInfoAdapter.translate(windowMetrics, oemFeature)

            assertNull(actual)
        }
    }

    @Test
    fun testTranslate_foldingFeature_invalidState() {
        activityScenario.scenario.onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity)
            val bounds = windowMetrics.bounds
            val featureBounds = Rect(0, bounds.centerY(), bounds.width(), bounds.centerY())
            val oemFeature = OEMFoldingFeature(featureBounds, TYPE_HINGE, -1)

            val actual = ExtensionsWindowLayoutInfoAdapter.translate(windowMetrics, oemFeature)

            assertNull(actual)
        }
    }

    @Test
    fun testTranslate_foldingFeature_invalidBounds() {
        activityScenario.scenario.onActivity { activity ->
            val windowMetrics =
                WindowMetricsCalculatorCompat().computeCurrentWindowMetrics(activity)
            val windowBounds = windowMetrics.bounds

            val source =
                invalidNonZeroFoldBounds(windowBounds).map { featureBounds ->
                    OEMFoldingFeature(featureBounds, TYPE_HINGE, STATE_HALF_OPENED)
                }

            val invalidFeatures =
                source.mapNotNull { feature ->
                    ExtensionsWindowLayoutInfoAdapter.translate(windowMetrics, feature)
                }

            assertTrue(source.isNotEmpty())
            assertTrue(
                "Expected invalid FoldingFeatures to be filtered but had $invalidFeatures",
                invalidFeatures.isEmpty()
            )
        }
    }
}
