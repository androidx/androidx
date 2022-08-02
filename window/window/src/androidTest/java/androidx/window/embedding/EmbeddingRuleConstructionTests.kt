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
import android.util.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitRule.Companion.FINISH_ADJACENT
import androidx.window.embedding.SplitRule.Companion.FINISH_ALWAYS
import androidx.window.embedding.SplitRule.Companion.FINISH_NEVER
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
@OptIn(ExperimentalWindowApi::class)
class EmbeddingRuleConstructionTests {
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
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        val application = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(rule.checkParentBounds(minValidWindowBounds(application.resources)))
        assertFalse(rule.checkParentBounds(almostValidWindowBounds(application.resources)))
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
        assertEquals(FINISH_NEVER, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ALWAYS, rule.finishSecondaryWithPrimary)
        assertEquals(false, rule.clearTop)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        assertTrue(rule.checkParentBounds(minValidWindowBounds(application.resources)))
        assertFalse(rule.checkParentBounds(almostValidWindowBounds(application.resources)))
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
        val rule = SplitPairRule.Builder(
            filters,
            123,
            456
        )
            .setFinishPrimaryWithSecondary(FINISH_ADJACENT)
            .setFinishSecondaryWithPrimary(FINISH_ADJACENT)
            .setClearTop(true)
            .setSplitRatio(0.3f)
            .setLayoutDir(LayoutDirection.LTR)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithSecondary)
        assertEquals(FINISH_ADJACENT, rule.finishSecondaryWithPrimary)
        assertEquals(true, rule.clearTop)
        assertEquals(0.3f, rule.splitRatio)
        assertEquals(LayoutDirection.LTR, rule.layoutDirection)
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
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(
                HashSet(),
                minWidth = 123,
                minSmallestWidth = 456
            )
                .setSplitRatio(-1.0f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPairRule.Builder(
                HashSet(),
                minWidth = 123,
                minSmallestWidth = 456
            )
                .setSplitRatio(1.1f)
                .build()
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
        assertEquals(FINISH_ALWAYS, rule.finishPrimaryWithPlaceholder)
        assertEquals(false, rule.isSticky)
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
        val application = ApplicationProvider.getApplicationContext<Application>()
        assertTrue(rule.checkParentBounds(minValidWindowBounds(application.resources)))
        assertFalse(rule.checkParentBounds(almostValidWindowBounds(application.resources)))
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
        assertEquals(0.5f, rule.splitRatio)
        assertEquals(LayoutDirection.LOCALE, rule.layoutDirection)
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
        val rule = SplitPlaceholderRule.Builder(
            filters,
            intent,
            123,
            456
        )
            .setFinishPrimaryWithPlaceholder(FINISH_ADJACENT)
            .setSticky(true)
            .setSplitRatio(0.3f)
            .setLayoutDir(LayoutDirection.LTR)
            .build()
        assertEquals(FINISH_ADJACENT, rule.finishPrimaryWithPlaceholder)
        assertEquals(true, rule.isSticky)
        assertEquals(0.3f, rule.splitRatio)
        assertEquals(LayoutDirection.LTR, rule.layoutDirection)
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
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(
                HashSet(),
                Intent(),
                minWidth = 123,
                minSmallestWidth = 456
            )
                .setSplitRatio(-1.0f)
                .build()
        }
        assertThrows(IllegalArgumentException::class.java) {
            SplitPlaceholderRule.Builder(
                HashSet(),
                Intent(),
                minWidth = 123,
                minSmallestWidth = 456
            )
                .setSplitRatio(1.1f)
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