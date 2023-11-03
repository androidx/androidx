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

package androidx.window.embedding

import android.content.res.Configuration
import android.graphics.Rect
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit tests for [SplitAttributesCalculatorParams] */
@RunWith(RobolectricTestRunner::class) // Used for initializing Android instance (ex: Configuration)
class SplitAttributesCalculatorParamsTest {

    @Test
    fun testSplitAttributesCalculatorParams() {
        val parentWindowMetrics = WindowMetrics(Rect())
        val parentConfiguration = Configuration()
        val parentWindowLayoutInfo = WindowLayoutInfo(emptyList())
        val defaultSplitAttributes = SplitAttributes.Builder().build()
        val areDefaultConstraintsSatisfied = true
        val splitRuleTag = "test"

        val params = SplitAttributesCalculatorParams(
            parentWindowMetrics,
            parentConfiguration,
            parentWindowLayoutInfo,
            defaultSplitAttributes,
            areDefaultConstraintsSatisfied,
            splitRuleTag
        )

        assertEquals(parentWindowMetrics, params.parentWindowMetrics)
        assertEquals(parentConfiguration, params.parentConfiguration)
        assertEquals(parentWindowLayoutInfo, params.parentWindowLayoutInfo)
        assertEquals(defaultSplitAttributes, params.defaultSplitAttributes)
        assertEquals(areDefaultConstraintsSatisfied, params.areDefaultConstraintsSatisfied)
        assertEquals(splitRuleTag, params.splitRuleTag)
    }

    @Test
    fun testToString() {
        val parentWindowMetrics = WindowMetrics(Rect())
        val parentConfiguration = Configuration()
        val parentWindowLayoutInfo = WindowLayoutInfo(emptyList())
        val defaultSplitAttributes = SplitAttributes.Builder().build()
        val areDefaultConstraintsSatisfied = true
        val splitRuleTag = "test"

        val paramsString = SplitAttributesCalculatorParams(
            parentWindowMetrics,
            parentConfiguration,
            parentWindowLayoutInfo,
            defaultSplitAttributes,
            areDefaultConstraintsSatisfied,
            splitRuleTag
        ).toString()

        assertTrue(paramsString.contains(parentWindowMetrics.toString()))
        assertTrue(paramsString.contains(parentConfiguration.toString()))
        assertTrue(paramsString.contains(parentWindowLayoutInfo.toString()))
        assertTrue(paramsString.contains(defaultSplitAttributes.toString()))
        assertTrue(paramsString.contains(areDefaultConstraintsSatisfied.toString()))
        assertTrue(paramsString.contains(splitRuleTag))
    }
}
