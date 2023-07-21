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
import androidx.window.core.ActivityComponentInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityRuleTest {

    @Test
    fun testAddingActivityRule() {
        val rule = ActivityRule.Builder(setOf()).build() + FILTER_WITH_ACTIVITY

        assertEquals(setOf(FILTER_WITH_ACTIVITY), rule.filters)
    }

    @Test
    fun testBuildActivityRule() {
        val rule = ActivityRule.Builder(setOf(FILTER_WITH_ACTIVITY)).build()

        assertEquals(setOf(FILTER_WITH_ACTIVITY), rule.filters)
        assertFalse(rule.alwaysExpand)
    }

    @Test
    fun testBuildActivityRule_enableAlwaysExpanded() {
        val rule = ActivityRule.Builder(setOf(FILTER_WITH_ACTIVITY)).setAlwaysExpand(true).build()

        assertEquals(setOf(FILTER_WITH_ACTIVITY), rule.filters)
        assertTrue(rule.alwaysExpand)
    }

    @Test
    fun equalsImpliesHashCode() {
        val firstRule = ActivityRule.Builder(setOf(FILTER_WITH_ACTIVITY))
            .setAlwaysExpand(true)
            .build()
        val secondRule = ActivityRule.Builder(setOf(FILTER_WITH_ACTIVITY))
            .setAlwaysExpand(true)
            .build()

        assertEquals(firstRule, secondRule)
        assertEquals(firstRule.hashCode(), secondRule.hashCode())
    }

    /**
     * Verifies that default params are set correctly when creating [ActivityRule] with a
     * builder.
     */
    @Test
    fun testDefaults_ActivityRule_Builder() {
        val rule = ActivityRule.Builder(HashSet()).build()
        assertFalse(rule.alwaysExpand)
    }

    /**
     * Verifies that the params are set correctly when creating [ActivityRule] with a builder.
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
            .setTag(TEST_TAG)
            .build()
        assertTrue(rule.alwaysExpand)
        assertEquals(TEST_TAG, rule.tag)
        assertEquals(filters, rule.filters)
    }

    companion object {
        private const val TEST_TAG = "test"
        val FILTER_WITH_ACTIVITY = ActivityFilter(
            ActivityComponentInfo("package", "className"),
            null
        )
    }
}