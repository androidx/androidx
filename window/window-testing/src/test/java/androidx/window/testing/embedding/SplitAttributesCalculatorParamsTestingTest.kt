/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.testing.embedding

import android.content.res.Configuration
import android.graphics.Rect
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_EXPAND
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import androidx.window.testing.layout.FoldingFeature as testFoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test class to verify [TestSplitAttributesCalculatorParams]. */
@RunWith(RobolectricTestRunner::class)
class SplitAttributesCalculatorParamsTestingTest {

    /** Verifies if the default values of [TestSplitAttributesCalculatorParams] are as expected. */
    @Test
    fun testDefaults() {
        val params = TestSplitAttributesCalculatorParams(TEST_METRICS)

        assertEquals(TEST_METRICS, params.parentWindowMetrics)
        assertEquals(0, params.parentConfiguration.diff(Configuration()))
        assertEquals(DEFAULT_SPLIT_ATTRIBUTES, params.defaultSplitAttributes)
        assertTrue(params.areDefaultConstraintsSatisfied)
        assertEquals(WindowLayoutInfo(Collections.emptyList()), params.parentWindowLayoutInfo)
        assertNull(params.splitRuleTag)

        assertEquals(DEFAULT_SPLIT_ATTRIBUTES, testSplitAttributesCalculator(params))
    }

    @OptIn(ExperimentalWindowApi::class)
    @Test
    fun testParamsWithTabletopFoldingFeature() {
        val tabletopFoldingFeature = testFoldingFeature(TEST_BOUNDS)
        val parentWindowLayoutInfo = TestWindowLayoutInfo(listOf(tabletopFoldingFeature))
        val params = TestSplitAttributesCalculatorParams(
                parentWindowMetrics = TEST_METRICS,
                parentWindowLayoutInfo = parentWindowLayoutInfo
            )

        assertEquals(TEST_METRICS, params.parentWindowMetrics)
        assertEquals(0, params.parentConfiguration.diff(Configuration()))
        assertEquals(DEFAULT_SPLIT_ATTRIBUTES, params.defaultSplitAttributes)
        assertTrue(params.areDefaultConstraintsSatisfied)
        assertEquals(parentWindowLayoutInfo, params.parentWindowLayoutInfo)
        assertNull(params.splitRuleTag)

        assertEquals(TABLETOP_HINGE_ATTRIBUTES, testSplitAttributesCalculator(params))
    }

    private fun testSplitAttributesCalculator(
        params: SplitAttributesCalculatorParams
    ): SplitAttributes {
        val foldingFeatures = params.parentWindowLayoutInfo.displayFeatures
            .filterIsInstance<FoldingFeature>()
        val foldingFeature: FoldingFeature? =
            if (foldingFeatures.size == 1) {
                foldingFeatures.first()
            } else {
                null
            }
        if (foldingFeature?.state == FoldingFeature.State.HALF_OPENED &&
            foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
        ) {
            return TABLETOP_HINGE_ATTRIBUTES
        }
        return if (params.areDefaultConstraintsSatisfied) {
            params.defaultSplitAttributes
        } else {
            SplitAttributes.Builder()
                .setSplitType(SPLIT_TYPE_EXPAND)
                .build()
        }
    }

    companion object {
        private val TEST_BOUNDS = Rect(0, 0, 2000, 2000)
        private val TEST_METRICS = WindowMetrics(TEST_BOUNDS)
        private val DEFAULT_SPLIT_ATTRIBUTES = SplitAttributes.Builder().build()
        private val TABLETOP_HINGE_ATTRIBUTES = SplitAttributes.Builder()
            .setSplitType(SPLIT_TYPE_HINGE)
            .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
            .build()
    }
}
