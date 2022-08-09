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

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.BOTTOM_TO_TOP
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LEFT_TO_RIGHT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LOCALE
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.TOP_TO_BOTTOM
import androidx.window.embedding.SplitRule.Companion.FINISH_ADJACENT
import androidx.window.embedding.SplitRule.Companion.FINISH_ALWAYS
import androidx.window.embedding.SplitRule.Companion.FINISH_NEVER
import androidx.window.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests creation of all embedding rule types via builders and from XML.
 * @see SplitPairRule
 * @see SplitRule
 * @see ActivityRule
 */
@OptIn(ExperimentalWindowApi::class)
class EmbeddingRuleConstructionTests {
    lateinit var application: Application
    lateinit var validBounds: Rect
    lateinit var invalidBounds: Rect

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        validBounds = minValidWindowBounds(application.resources)
        invalidBounds = almostValidWindowBounds(application.resources)
    }

    /**
     * Verifies that default params are set correctly when reading {@link SplitPairRule} from XML.
     */
    @Test
    fun testDefaults_SplitPairRule_Xml() {
        SplitController.initialize(ApplicationProvider.getApplicationContext(),
            R.xml.test_split_config_default_split_pair_rule)

        val rules = SplitController.getInstance().getSplitRules()
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(validBounds))
        assertFalse(rule.checkParentBounds(invalidBounds))
    }

    /** Verifies that horizontal layout are set correctly when reading [SplitPairRule] from XML. */
    @Test
    fun testHorizontalLayout_SplitPairRule_Xml() {
        SplitController.initialize(ApplicationProvider.getApplicationContext(),
            R.xml.test_split_config_split_pair_rule_horizontal_layout)

        val rules = SplitController.getInstance().getSplitRules()
        assertEquals(1, rules.size)
        val rule: SplitPairRule = rules.first() as SplitPairRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(TOP_TO_BOTTOM)
            .build()
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(validBounds))
        assertFalse(rule.checkParentBounds(invalidBounds))
    }

    /**
     * Verifies that default params are set correctly when creating {@link SplitPairRule} with a
     * builder.
     */
    @Test
    fun testDefaults_SplitPairRule_Builder() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val bounds = minValidWindowBounds(application.resources)
        val rule = SplitPairRule.Builder(
            HashSet(),
            bounds.width(),
            bounds.width()
        ).build()
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(validBounds))
        assertFalse(rule.checkParentBounds(invalidBounds))
    }

    /**
     * Verifies that the params are set correctly when creating {@link SplitPairRule} with a
     * builder.
     */
    @Test
    fun test_SplitPairRule_Builder() {
        val filters = HashSet<SplitPairFilter>()
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(LEFT_TO_RIGHT)
            .build()
        filters.add(
            SplitPairFilter(
                ComponentName("a", "b"),
                ComponentName("c", "d"),
                "ACTION"
            )
        )
        val rule = SplitPairRule.Builder(
            filters,
            123,
            456
        )
            .setFinishPrimaryWithSecondary(FINISH_ADJACENT)
            .setFinishSecondaryWithPrimary(FINISH_ADJACENT)
            .setClearTop(true)
            .setDefaultSplitAttributes(expectedSplitLayout)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ADJACENT, rule.finishSecondaryWithPrimary)
        assertEquals(true, rule.clearTop)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertEquals(filters, rule.filters)
        assertEquals(123, rule.minWidth)
        assertEquals(456, rule.minSmallestWidth)
    }

    /**
     * Verifies that illegal parameter values are not allowed when creating {@link SplitPairRule}
     * with a builder.
     */
    @Test
    fun test_SplitPairRule_Builder_illegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(
                HashSet(),
                minWidth = -1,
                minSmallestWidth = 456
            ).build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(
                HashSet(),
                minWidth = 123,
                minSmallestWidth = -1
            ).build()
        }
    }

    /**
     * Verifies that default params are set correctly when reading {@link SplitPlaceholderRule} from
     * XML.
     */
    @Test
    fun testDefaults_SplitPlaceholderRule_Xml() {
        SplitController.initialize(ApplicationProvider.getApplicationContext(),
            R.xml.test_split_config_default_split_placeholder_rule)

        val rules = SplitController.getInstance().getSplitRules()
        assertEquals(1, rules.size)
        val rule: SplitPlaceholderRule = rules.first() as SplitPlaceholderRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(validBounds))
        assertFalse(rule.checkParentBounds(invalidBounds))
    }

    /**
     * Verifies that horizontal layout are set correctly when reading [SplitPlaceholderRule]
     * from XML.
     */
    @Test
    fun testHorizontalLayout_SplitPlaceholderRule_Xml() {
        SplitController.initialize(ApplicationProvider.getApplicationContext(),
            R.xml.test_split_config_split_placeholder_horizontal_layout)

        val rules = SplitController.getInstance().getSplitRules()
        assertEquals(1, rules.size)
        val rule: SplitPlaceholderRule = rules.first() as SplitPlaceholderRule
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(BOTTOM_TO_TOP)
            .build()
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertTrue(rule.checkParentBounds(validBounds))
        assertFalse(rule.checkParentBounds(invalidBounds))
    }

    /**
     * Verifies that default params are set correctly when creating {@link SplitPlaceholderRule}
     * with a builder.
     */
    @Test
    fun testDefaults_SplitPlaceholderRule_Builder() {
        val rule = SplitPlaceholderRule.Builder(
            HashSet(),
            Intent(),
            123,
            456
        )
            .build()
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.5f))
            .setLayoutDirection(LOCALE)
            .build()
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertEquals(123, rule.minWidth)
        assertEquals(456, rule.minSmallestWidth)
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
        val expectedSplitLayout = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.ratio(0.3f))
            .setLayoutDirection(LEFT_TO_RIGHT)
            .build()
        val rule = SplitPlaceholderRule.Builder(
            filters,
            intent,
            123,
            456
        )
            .setFinishPrimaryWithPlaceholder(FINISH_ADJACENT)
            .setSticky(true)
            .setDefaultSplitAttributes(expectedSplitLayout)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(expectedSplitLayout, rule.defaultSplitAttributes)
        assertEquals(filters, rule.filters)
        assertEquals(intent, rule.placeholderIntent)
        assertEquals(123, rule.minWidth)
        assertEquals(456, rule.minSmallestWidth)
    }

    /**
     * Verifies that illegal parameter values are not allowed when creating
     * {@link SplitPlaceholderRule} with a builder.
     */
    @Test
    fun test_SplitPlaceholderRule_Builder_illegalArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(
                HashSet(),
                Intent(),
                minWidth = -1,
                minSmallestWidth = 456
            ).build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(
                HashSet(),
                Intent(),
                minWidth = 123,
                minSmallestWidth = -1
            ).build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(
                HashSet(),
                Intent(),
                minWidth = 123,
                minSmallestWidth = 456
            )
                .setFinishPrimaryWithPlaceholder(FINISH_NEVER)
                .build()
        }
    }

    /**
     * Verifies that default params are set correctly when reading {@link ActivityRule} from XML.
     */
    @Test
    fun testDefaults_ActivityRule_Xml() {
        SplitController.initialize(ApplicationProvider.getApplicationContext(),
            R.xml.test_split_config_default_activity_rule)

        val rules = SplitController.getInstance().getSplitRules()
        assertEquals(1, rules.size)
        val rule: ActivityRule = rules.first() as ActivityRule
        assertEquals(false, rule.alwaysExpand)
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

    private fun minValidWindowBounds(resources: Resources): Rect {
        val minValidWidthDp = 600
        // Get the screen's density scale
        val scale: Float = resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        val minValidWidthPx = (minValidWidthDp * scale + 0.5f).toInt()

        return Rect(0, 0, minValidWidthPx, minValidWidthPx)
    }

    private fun almostValidWindowBounds(resources: Resources): Rect {
        val minValidWidthDp = 600
        // Get the screen's density scale
        val scale: Float = resources.displayMetrics.density
        // Convert the dps to pixels, based on density scale
        val minValidWidthPx = ((minValidWidthDp - 1) * scale + 0.5f).toInt()

        return Rect(0, 0, minValidWidthPx, minValidWidthPx)
    }
}