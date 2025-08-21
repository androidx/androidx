/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import junit.framework.TestCase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Unit test for [SplitPinRule] to check the construction is correct by using it's builder. */
@RunWith(RobolectricTestRunner::class)
class SplitPinRuleTest {

    /*------------------------------Class Test------------------------------*/
    /** Test hashcode and equals are properly calculated for 2 equal [SplitPinRule] */
    @Test
    fun equalsImpliesHashCode() {
        val firstRule = SplitPinRule.Builder().setSticky(true).build()
        val secondRule = SplitPinRule.Builder().setSticky(true).build()
        assertEquals(firstRule, secondRule)
        assertEquals(firstRule.hashCode(), secondRule.hashCode())

        // The hashCode should be consistent to the predetermined value.
        // Note that the value should be updated whenever hashCode calculation is changed.
        assertEquals(1678620995, firstRule.hashCode())
    }

    /*------------------------------Builder Test------------------------------*/
    /**
     * Verifies that default params are set correctly when creating [SplitPinRule] with a builder.
     */
    @Test
    fun testDefaults_SplitPinRule_Builder() {
        val rule = SplitPinRule.Builder().build()
        val expectedSplitLayout =
            SplitAttributes.Builder()
                .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
                .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
                .setAnimationParams(EmbeddingAnimationParams.Builder().build())
                .build()

        TestCase.assertNull(rule.tag)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minHeightDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(false, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
    }
}
