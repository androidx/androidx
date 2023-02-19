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

import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import androidx.window.core.ActivityComponentInfo
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER
import junit.framework.TestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit test for [SplitPlaceholderRule] to check the construction is correct by using it's builder.
 */
@RunWith(RobolectricTestRunner::class)
internal class SplitPlaceHolderRuleTest {

    /*------------------------------Class Test------------------------------*/
    /**
     * Test hashcode and equals are properly calculated for 2 equal [SplitPlaceholderRule]
     */
    @Test
    fun equalsImpliesHashCode() {
        val filter = ActivityFilter(ActivityComponentInfo("a", "b"), "ACTION")
        val placeholderIntent = Intent()
        val firstRule = SplitPlaceholderRule.Builder(setOf(filter), placeholderIntent).build()
        val secondRule = SplitPlaceholderRule.Builder(setOf(filter), placeholderIntent).build()
        assertEquals(firstRule, secondRule)
        assertEquals(firstRule.hashCode(), secondRule.hashCode())
    }

    /*------------------------------Builder Test------------------------------*/
    /**
     * Verifies that default params are set correctly when creating [SplitPlaceholderRule]
     * with a builder.
     */
    @Test
    fun testDefaults_SplitPlaceholderRule_Builder() {
        val rule = SplitPlaceholderRule.Builder(HashSet(), Intent()).build()
        TestCase.assertNull(rule.tag)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minHeightDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
            .build()
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(density, validBounds))
        assertFalse(rule.checkParentBounds(density, invalidBounds))
    }

    /**
     * Verifies that the params are set correctly when creating [SplitPlaceholderRule] with a
     * builder.
     */
    @Test
    fun test_SplitPlaceholderRule_Builder() {
        val filters = HashSet<ActivityFilter>()
        filters.add(
            ActivityFilter(
                ComponentName("a", "b"),
                "ACTION"
            )
        )
        val intent = Intent("ACTION")
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(SplitAttributes.LayoutDirection.LEFT_TO_RIGHT)
            .build()
        val rule = SplitPlaceholderRule.Builder(filters, intent)
            .setMinWidthDp(123)
            .setMinHeightDp(456)
            .setMinSmallestWidthDp(789)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ratio(1.23f))
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ratio(4.56f))
            .setFinishPrimaryWithPlaceholder(SplitRule.FinishBehavior.ADJACENT)
            .setSticky(true)
            .setDefaultSplitAttributes(expectedSplitLayout)
            .setTag(TEST_TAG)
            .build()
        assertEquals(SplitRule.FinishBehavior.ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertEquals(filters, rule.filters)
        assertEquals(intent, rule.placeholderIntent)
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minHeightDp)
        assertEquals(789, rule.minSmallestWidthDp)
        assertEquals(TEST_TAG, rule.tag)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(4.56f, rule.maxAspectRatioInLandscape.value)
    }

    /**
     * Verifies that illegal parameter values are not allowed when creating
     * [SplitPlaceholderRule] with a builder.
     */
    @Test
    fun test_SplitPlaceholderRule_Builder_illegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(-1)
                .setMinHeightDp(456)
                .setMinSmallestWidthDp(789)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinHeightDp(-1)
                .setMinSmallestWidthDp(789)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinHeightDp(456)
                .setMinSmallestWidthDp(-1)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinHeightDp(456)
                .setMinSmallestWidthDp(789)
                .setFinishPrimaryWithPlaceholder(NEVER)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ratio(-1f))
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ratio(-1f))
                .build()
        }
    }

    /*------------------------------Functional Test------------------------------*/
    /**
     * Verifies that the [SplitPlaceholderRule] verifies that the parent bounds satisfy
     * maxAspectRatioInPortrait.
     */
    @Test
    fun testSplitPlaceholderRule_maxAspectRatioInPortrait() {
        // Always allow split
        var rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .build()
        var width = 100
        var height = 1000
        var bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Always disallow split in portrait
        rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_DISALLOW)
            .build()
        width = 100
        height = 101
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in landscape
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Compare the aspect ratio in portrait
        rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ratio(1.1f))
            .build()
        // Equals to the max aspect ratio
        width = 100
        height = 110
        bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))
        // Greater than the max aspect ratio
        width = 100
        height = 111
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in landscape
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))
    }

    /**
     * Verifies that the [SplitPlaceholderRule] verifies that the parent bounds satisfy
     * maxAspectRatioInLandscape.
     */
    @Test
    fun testSplitPlaceholderRule_maxAspectRatioInLandscape() {
        // Always allow split
        var rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .build()
        var width = 1000
        var height = 100
        var bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in portrait
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Always disallow split in landscape
        rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ALWAYS_DISALLOW)
            .build()
        width = 101
        height = 100
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in portrait
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Compare the aspect ratio in landscape
        rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinHeightDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SplitRule.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(EmbeddingAspectRatio.ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(EmbeddingAspectRatio.ratio(1.1f))
            .build()
        // Equals to the max aspect ratio
        width = 110
        height = 100
        bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))
        // Greater than the max aspect ratio
        width = 111
        height = 100
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in portrait
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))
    }

    companion object {

        private const val density = 2f
        private const val TEST_TAG = "test"
        private val validBounds: Rect = minValidWindowBounds()
        private val invalidBounds: Rect = almostValidWindowBounds()

        private fun minValidWindowBounds(): Rect {
            // Get the screen's density scale
            val scale: Float = density
            // Convert the dps to pixels, based on density scale
            val minValidWidthPx = (SPLIT_MIN_DIMENSION_DP_DEFAULT * scale + 0.5f).toInt()
            return Rect(0, 0, minValidWidthPx, minValidWidthPx)
        }

        private fun almostValidWindowBounds(): Rect {
            // Get the screen's density scale
            val scale: Float = density
            // Convert the dps to pixels, based on density scale
            val minValidWidthPx = ((SPLIT_MIN_DIMENSION_DP_DEFAULT) - 1 * scale + 0.5f).toInt()
            return Rect(0, 0, minValidWidthPx, minValidWidthPx)
        }
    }
}