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
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.window.embedding.EmbeddingAspectRatio.Companion.ALWAYS_DISALLOW
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.BOTTOM_TO_TOP
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.RIGHT_TO_LEFT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.TOP_TO_BOTTOM
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ADJACENT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER
import androidx.window.test.R
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests creation of all embedding rule types and from XML.
 * @see SplitPairRule
 * @see SplitRule
 * @see ActivityRule
 * @see RuleParser
 */
class RuleParserTests {
    private val application = ApplicationProvider.getApplicationContext<Context>()
    private val ruleController = RuleController.getInstance(application)
    private val density = application.resources.displayMetrics.density
    private lateinit var validBounds: Rect
    private lateinit var invalidBounds: Rect

    @Before
    fun setUp() {
        validBounds = minValidWindowBounds()
        invalidBounds = almostValidWindowBounds()
        ruleController.clearRules()
    }

    /**
     * Verifies that default params are set correctly when reading {@link SplitPairRule} from XML.
     */
    @Test
    fun testDefaults_SplitPairRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_default_split_pair_rule)
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertNull(rule.tag)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minHeightDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(density, validBounds))
        assertFalse(rule.checkParentBounds(density, invalidBounds))
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
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.1f))
            .setLayoutDirection(RIGHT_TO_LEFT)
            .build()
        assertEquals("rule2", rule.tag)
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minHeightDp)
        assertEquals(789, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(ALWAYS_DISALLOW, rule.maxAspectRatioInLandscape)
        assertEquals(ALWAYS, rule.finishPrimaryWithSecondary)
        assertEquals(NEVER, rule.finishSecondaryWithPrimary)
        assertEquals(true, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
    }

    /** Verifies that horizontal layout are set correctly when reading [SplitPairRule] from XML. */
    @Test
    fun testHorizontalLayout_SplitPairRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_split_pair_rule_horizontal_layout)
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(TOP_TO_BOTTOM)
            .build()
        assertEquals(TEST_TAG, rule.tag)
        assertEquals(NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(density, validBounds))
        assertFalse(rule.checkParentBounds(density, invalidBounds))
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
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertNull(rule.tag)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minWidthDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minHeightDp)
        assertEquals(SPLIT_MIN_DIMENSION_DP_DEFAULT, rule.minSmallestWidthDp)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT, rule.maxAspectRatioInPortrait)
        assertEquals(SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT, rule.maxAspectRatioInLandscape)
        assertEquals(ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(density, validBounds))
        assertFalse(rule.checkParentBounds(density, invalidBounds))
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
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.1f))
            .setLayoutDirection(RIGHT_TO_LEFT)
            .build()
        assertEquals("rule3", rule.tag)
        assertEquals(123, rule.minWidthDp)
        assertEquals(456, rule.minHeightDp)
        assertEquals(789, rule.minSmallestWidthDp)
        assertEquals(1.23f, rule.maxAspectRatioInPortrait.value)
        assertEquals(ALWAYS_DISALLOW, rule.maxAspectRatioInLandscape)
        assertEquals(ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
    }

    /**
     * Verifies that horizontal layout are set correctly when reading [SplitPlaceholderRule]
     * from XML.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @Test
    fun testHorizontalLayout_SplitPlaceholderRule_Xml() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_split_placeholder_horizontal_layout)
        assertEquals(1, rules.size)
        val rule: SplitPlaceholderRule = rules.first() as SplitPlaceholderRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(BOTTOM_TO_TOP)
            .build()
        assertEquals(TEST_TAG, rule.tag)
        assertEquals(ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(density, validBounds))
        assertFalse(rule.checkParentBounds(density, invalidBounds))
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
        assertNull(rule.tag)
        assertFalse(rule.alwaysExpand)
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
        assertEquals("rule1", rule.tag)
        assertTrue(rule.alwaysExpand)
    }

    /**
     * Verifies that [ActivityRule.tag] and [ActivityRule.alwaysExpand] are set correctly when
     * reading [ActivityRule] from XML.
     */
    @Test
    fun testSetTagAndAlwaysExpand_ActivityRule_Xml() {
        val rules = RuleController
            .parseRules(application, R.xml.test_split_config_activity_rule_with_tag)
        assertEquals(1, rules.size)
        val rule: ActivityRule = rules.first() as ActivityRule
        assertEquals(TEST_TAG, rule.tag)
        assertTrue(rule.alwaysExpand)
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

    @Test
    fun testIllegalTag_XML() {
        assertThrows(IllegalArgumentException::class.java) {
            RuleController.parseRules(application, R.xml.test_split_config_duplicated_tag)
        }
    }

    @Test
    fun testReplacingRuleWithTag() {
        var rules = RuleController
            .parseRules(application, R.xml.test_split_config_activity_rule_with_tag)
        assertEquals(1, rules.size)
        var rule = rules.first()
        assertEquals(TEST_TAG, rule.tag)
        val staticRule = rule as ActivityRule
        assertTrue(staticRule.alwaysExpand)
        ruleController.setRules(rules)

        val filters = HashSet<ActivityFilter>()
        filters.add(
            ActivityFilter(
                ComponentName("a", "b"),
                "ACTION"
            )
        )
        val rule1 = ActivityRule.Builder(filters)
            .setAlwaysExpand(true)
            .setTag(TEST_TAG)
            .build()
        ruleController.addRule(rule1)

        rules = ruleController.getRules()
        assertEquals(1, rules.size)
        rule = rules.first()
        assertEquals(rule1, rule)

        val intent = Intent("ACTION")
        val rule2 = SplitPlaceholderRule.Builder(filters, intent)
            .setMinWidthDp(123)
            .setMinHeightDp(456)
            .setMinSmallestWidthDp(789)
            .setTag(TEST_TAG)
            .build()

        ruleController.addRule(rule2)

        rules = ruleController.getRules()
        assertEquals(1, rules.size)
        rule = rules.first()
        assertEquals(rule2, rule)
    }

    companion object {
        const val TEST_TAG = "test"
    }
}