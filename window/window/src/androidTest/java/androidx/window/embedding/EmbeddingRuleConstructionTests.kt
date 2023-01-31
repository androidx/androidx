/*
 * Copyright 2022 The Android Open Source Project
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
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.util.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.window.embedding.EmbeddingAspectRatio.Companion.alwaysAllow
import androidx.window.embedding.EmbeddingAspectRatio.Companion.alwaysDisallow
import androidx.window.embedding.EmbeddingAspectRatio.Companion.ratio
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.Companion.FINISH_ADJACENT
import androidx.window.embedding.SplitRule.Companion.FINISH_ALWAYS
import androidx.window.embedding.SplitRule.Companion.FINISH_NEVER
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests creation of all embedding rule types via builders and from XML.
 * @see SplitPairRule
 * @see SplitRule
 * @see ActivityRule
 */
class EmbeddingRuleConstructionTests {
    private val application = ApplicationProvider.getApplicationContext<Context>()
    private val density = application.resources.displayMetrics.density

    /**
     * Verifies that default params are set correctly when reading {@link SplitPairRule} from XML.
     */
    @Test
    fun testDefaults_SplitPairRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_default_split_pair_rule)
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        assertTrue(rule.checkParentBounds(density, minValidWindowBounds()))
        assertFalse(rule.checkParentBounds(density, almostValidWindowBounds()))
    }

    /**
     * Verifies that params are set correctly when reading {@link SplitPairRule} from XML.
     * @see R.xml.test_split_config_custom_split_pair_rule for customized value.
     */
    @Test
    fun testCustom_SplitPairRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_custom_split_pair_rule)
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(alwaysDisallow(), rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_NEVER, rule.finishSecondaryWithPrimary)
        assertEquals(true, rule.clearTop)
        assertEquals(0.1f, rule.splitRatio)
        assertEquals(LayoutDirection.RTL, rule.layoutDirection)
    }

    /**
     * Verifies that default params are set correctly when creating {@link SplitPairRule} with a
     * builder.
     */
    @Test
    fun testDefaults_SplitPairRule_Builder() {
        val rule = SplitPairRule.Builder(HashSet()).build()
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        assertTrue(rule.checkParentBounds(density, minValidWindowBounds()))
        assertFalse(rule.checkParentBounds(density, almostValidWindowBounds()))
    }

    /**
     * Verifies that the params are set correctly when creating {@link SplitPairRule} with a
     * builder.
     */
    @Test
    fun test_SplitPairRule_Builder() {
        val filters = HashSet<SplitPairFilter>()
        filters.add(
            SplitPairFilter(
                ComponentName("a", "b"),
                ComponentName("c", "d"),
                "ACTION"
            )
        )
        val rule = SplitPairRule.Builder(filters)
            .setMinWidthDp(123)
            .setMinSmallestWidthDp(456)
            .setMaxAspectRatioInPortrait(ratio(1.23f))
            .setMaxAspectRatioInLandscape(ratio(4.56f))
            .setFinishPrimaryWithSecondary(FINISH_ADJACENT)
            .setFinishSecondaryWithPrimary(FINISH_ADJACENT)
            .setClearTop(true)
            .setSplitRatio(0.3f)
            .setLayoutDirection(LayoutDirection.LTR)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ADJACENT, rule.finishSecondaryWithPrimary)
        assertEquals(true, rule.clearTop)
        assertEquals(0.3f, rule.splitRatio)
        assertEquals(LayoutDirection.LTR, rule.layoutDirection)
        assertEquals(filters, rule.filters)
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(4.56f, rule.maxAspectRatioInLandscape.value)
    }

    /**
     * Verifies that illegal parameter values are not allowed when creating {@link SplitPairRule}
     * with a builder.
     */
    @Test
    fun test_SplitPairRule_Builder_illegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMinWidthDp(-1)
                .setMinSmallestWidthDp(456)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(-1)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(456)
                .setSplitRatio(-1.0f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(456)
                .setSplitRatio(1.1f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInPortrait(ratio(-1f))
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInLandscape(ratio(-1f))
                .build()
        }
    }

    /**
     * Verifies that the SplitPairRule verifies that the parent bounds satisfy
     * maxAspectRatioInPortrait.
     */
    @Test
    fun testSplitPairRule_maxAspectRatioInPortrait() {
        // Always allow split
        var rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .build()
        var width = 100
        var height = 1000
        var bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Always disallow split in portrait
        rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(alwaysDisallow())
            .build()
        width = 100
        height = 101
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in landscape
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Compare the aspect ratio in portrait
        rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(ratio(1.1f))
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
     * Verifies that the SplitPairRule verifies that the parent bounds satisfy
     * maxAspectRatioInLandscape.
     */
    @Test
    fun testSplitPairRule_maxAspectRatioInLandscape() {
        // Always allow split
        var rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .build()
        var width = 1000
        var height = 100
        var bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Always disallow split in landscape
        rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(alwaysDisallow())
            .build()
        width = 101
        height = 100
        bounds = Rect(0, 0, width, height)
        assertFalse(rule.checkParentBounds(density, bounds))
        // Ignore if the bounds in portrait
        bounds = Rect(0, 0, height, width)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Compare the aspect ratio in landscape
        rule = SplitPairRule.Builder(HashSet())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(ratio(1.1f))
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

    /**
     * Verifies that default params are set correctly when reading {@link SplitPlaceholderRule} from
     * XML.
     */
    @Test
    fun testDefaults_SplitPlaceholderRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_default_split_placeholder_rule)
        assertEquals(1, rules.size)
        val rule: SplitPlaceholderRule = rules.first() as SplitPlaceholderRule
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        assertTrue(rule.checkParentBounds(density, minValidWindowBounds()))
        assertFalse(rule.checkParentBounds(density, almostValidWindowBounds()))
    }

    /**
     * Verifies that params are set correctly when reading {@link SplitPlaceholderRule} from XML.
     * @see R.xml.test_split_config_custom_split_placeholder_rule for customized value.
     */
    @Test
    fun testCustom_SplitPlaceholderRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_custom_split_placeholder_rule)
        assertEquals(1, rules.size)
        val rule: SplitPlaceholderRule = rules.first() as SplitPlaceholderRule
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(alwaysDisallow(), rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(0.1f, rule.splitRatio)
        assertEquals(LayoutDirection.RTL, rule.layoutDirection)
    }

    /**
     * Verifies that default params are set correctly when creating {@link SplitPlaceholderRule}
     * with a builder.
     */
    @Test
    fun testDefaults_SplitPlaceholderRule_Builder() {
        val rule = SplitPlaceholderRule.Builder(HashSet(), Intent()).build()
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        assertTrue(rule.checkParentBounds(density, minValidWindowBounds()))
        assertFalse(rule.checkParentBounds(density, almostValidWindowBounds()))
    }

    /**
     * Verifies that the params are set correctly when creating {@link SplitPlaceholderRule} with a
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
        val rule = SplitPlaceholderRule.Builder(filters, intent)
            .setMinWidthDp(123)
            .setMinSmallestWidthDp(456)
            .setMaxAspectRatioInPortrait(ratio(1.23f))
            .setMaxAspectRatioInLandscape(ratio(4.56f))
            .setFinishPrimaryWithPlaceholder(FINISH_ADJACENT)
            .setSticky(true)
            .setSplitRatio(0.3f)
            .setLayoutDirection(LayoutDirection.LTR)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(0.3f, rule.splitRatio)
        assertEquals(LayoutDirection.LTR, rule.layoutDirection)
        assertEquals(filters, rule.filters)
        assertEquals(intent, rule.placeholderIntent)
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(4.56f, rule.maxAspectRatioInLandscape.value)
    }

    /**
     * Verifies that illegal parameter values are not allowed when creating
     * {@link SplitPlaceholderRule} with a builder.
     */
    @Test
    fun test_SplitPlaceholderRule_Builder_illegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(-1)
                .setMinSmallestWidthDp(456)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(-1)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(456)
                .setFinishPrimaryWithPlaceholder(FINISH_NEVER)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(456)
                .setSplitRatio(-1.0f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(HashSet(), Intent())
                .setMinWidthDp(123)
                .setMinSmallestWidthDp(456)
                .setSplitRatio(1.1f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInPortrait(ratio(-1f))
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(HashSet())
                .setMaxAspectRatioInLandscape(ratio(-1f))
                .build()
        }
    }

    /**
     * Verifies that the SplitPlaceholderRule verifies that the parent bounds satisfy
     * maxAspectRatioInPortrait.
     */
    @Test
    fun testSplitPlaceholderRule_maxAspectRatioInPortrait() {
        // Always allow split
        var rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .build()
        var width = 100
        var height = 1000
        var bounds = Rect(0, 0, width, height)
        assertTrue(rule.checkParentBounds(density, bounds))

        // Always disallow split in portrait
        rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(alwaysDisallow())
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
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInLandscape(alwaysAllow())
            .setMaxAspectRatioInPortrait(ratio(1.1f))
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
     * Verifies that the SplitPlaceholderRule verifies that the parent bounds satisfy
     * maxAspectRatioInLandscape.
     */
    @Test
    fun testSplitPlaceholderRule_maxAspectRatioInLandscape() {
        // Always allow split
        var rule = SplitPlaceholderRule.Builder(HashSet(), Intent())
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(alwaysAllow())
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
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(alwaysDisallow())
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
            .setMinWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMinSmallestWidthDp(SPLIT_MIN_DIMENSION_ALWAYS_ALLOW)
            .setMaxAspectRatioInPortrait(alwaysAllow())
            .setMaxAspectRatioInLandscape(ratio(1.1f))
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

    /**
     * Verifies that default params are set correctly when reading {@link ActivityRule} from XML.
     */
    @Test
    fun testDefaults_ActivityRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_default_activity_rule)
        assertEquals(1, rules.size)
        val rule: ActivityRule = rules.first() as ActivityRule
        assertEquals(false, rule.alwaysExpand)
    }

    /**
     * Verifies that params are set correctly when reading {@link ActivityRule} from XML.
     * @see R.xml.test_split_config_custom_activity_rule for customized value.
     */
    @Test
    fun testCustom_ActivityRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_custom_activity_rule)
        assertEquals(1, rules.size)
        val rule: ActivityRule = rules.first() as ActivityRule
        assertEquals(true, rule.alwaysExpand)
    }

    /**
     * Verifies that default params are set correctly when creating {@link ActivityRule} with a
     * builder.
     */
    @Test
    fun testDefaults_ActivityRule_Builder() {
        val rule = ActivityRule.Builder(HashSet()).build()
        assertEquals(false, rule.alwaysExpand)
    }

    /**
     * Verifies that the params are set correctly when creating {@link ActivityRule} with a builder.
     */
    @Test
    fun test_ActivityRule_Builder() {
        val filters = HashSet<ActivityFilter>()
        filters.add(
            ActivityFilter(
                ComponentName("a", "b"),
                "ACTION"
            )
        )
        val rule = ActivityRule.Builder(filters)
            .setAlwaysExpand(true)
            .build()
        assertEquals(true, rule.alwaysExpand)
        assertEquals(filters, rule.filters)
    }

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