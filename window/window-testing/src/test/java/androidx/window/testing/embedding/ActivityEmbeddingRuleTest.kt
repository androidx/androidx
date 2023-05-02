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

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.embedding.ActivityRule
import androidx.window.embedding.EmbeddingBackend
import androidx.window.embedding.EmbeddingRule
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.SplitType.Companion.SPLIT_TYPE_HINGE
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitPairRule
import androidx.window.embedding.SplitPlaceholderRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Test for [ActivityEmbeddingRule]. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityEmbeddingRuleTest {

    @get:Rule
    val testRule: ActivityEmbeddingRule = ActivityEmbeddingRule()

    private val mockActivity: Activity = mock()
    private val testScope = TestScope()

    private lateinit var activityEmbeddingController: ActivityEmbeddingController
    private lateinit var ruleController: RuleController
    private lateinit var splitController: SplitController

    init {
        whenever(mockActivity.applicationContext).thenReturn(mock<Application>())
    }

    @Before
    fun setUp() {
        activityEmbeddingController = ActivityEmbeddingController.getInstance(mockActivity)
        ruleController = RuleController.getInstance(mockActivity)
        splitController = SplitController.getInstance(mockActivity)
    }

    @Test
    fun testActivityEmbeddingController_overrideIsActivityEmbedded() {
        assertFalse(
            activityEmbeddingController.isActivityEmbedded(mockActivity)
        )
        testRule.overrideIsActivityEmbedded(
            activity = mockActivity,
            isActivityEmbedded = true
        )
        assertTrue(
            activityEmbeddingController.isActivityEmbedded(mockActivity)
        )

        testRule.overrideIsActivityEmbedded(
            activity = mockActivity,
            isActivityEmbedded = false
        )
        assertFalse(
            activityEmbeddingController.isActivityEmbedded(mockActivity)
        )
    }

    @Test
    fun testRuleController_getRules() {
        assertTrue(ruleController.getRules().isEmpty())
    }

    @Test
    fun testRuleController_addRule() {
        val splitPairRule = createSplitPairRule()
        val splitPlaceholderRule = createSplitPlaceholderRule()
        val activityRule = createActivityRule()

        ruleController.addRule(splitPairRule)

        assertEquals(1, ruleController.getRules().size)
        assertTrue(ruleController.getRules().contains(splitPairRule))

        ruleController.addRule(splitPlaceholderRule)

        assertEquals(2, ruleController.getRules().size)
        assertTrue(ruleController.getRules().contains(splitPlaceholderRule))

        ruleController.addRule(activityRule)

        assertEquals(3, ruleController.getRules().size)
        assertTrue(ruleController.getRules().contains(activityRule))
    }

    @Test
    fun testRuleController_addRule_updateRuleWithSameTag() {
        val splitPairRule1 = createSplitPairRule("Tag1")
        val splitPairRule2 = createSplitPairRule("Tag2")

        ruleController.addRule(splitPairRule1)
        ruleController.addRule(splitPairRule2)

        assertEquals(2, ruleController.getRules().size)

        val splitPairRule3 = SplitPairRule.Builder(emptySet())
            .setMinWidthDp(splitPairRule1.minWidthDp + 1)
            .setTag(splitPairRule1.tag)
            .build()

        ruleController.addRule(splitPairRule3)

        assertEquals(2, ruleController.getRules().size)
        assertFalse(ruleController.getRules().contains(splitPairRule1))
        assertTrue(ruleController.getRules().contains(splitPairRule3))
    }

    @Test
    fun testRuleController_removeRule() {
        val rules = HashSet<EmbeddingRule>()
        val splitPairRule = createSplitPairRule()
        val splitPlaceholderRule = createSplitPlaceholderRule()
        val activityRule = createActivityRule()
        rules.add(splitPairRule)
        rules.add(splitPlaceholderRule)
        rules.add(activityRule)
        ruleController.setRules(rules)

        assertEquals(3, ruleController.getRules().size)

        ruleController.removeRule(splitPairRule)

        assertEquals(2, ruleController.getRules().size)
        assertFalse(ruleController.getRules().contains(splitPairRule))

        ruleController.removeRule(splitPlaceholderRule)

        assertEquals(1, ruleController.getRules().size)
        assertFalse(ruleController.getRules().contains(splitPlaceholderRule))

        ruleController.removeRule(activityRule)

        assertTrue(ruleController.getRules().isEmpty())
    }

    @Test
    fun testRuleController_setRules() {
        val rules = HashSet<EmbeddingRule>()

        ruleController.setRules(rules)

        assertTrue(ruleController.getRules().isEmpty())

        val splitPairRule = createSplitPairRule()
        val splitPlaceholderRule = createSplitPlaceholderRule()
        val activityRule = createActivityRule()
        rules.add(splitPairRule)
        rules.add(splitPlaceholderRule)
        rules.add(activityRule)
        ruleController.setRules(rules)

        assertEquals(3, ruleController.getRules().size)
        assertTrue(ruleController.getRules().contains(splitPairRule))
        assertTrue(ruleController.getRules().contains(splitPlaceholderRule))
        assertTrue(ruleController.getRules().contains(activityRule))

        ruleController.setRules(emptySet())

        assertTrue(ruleController.getRules().isEmpty())
    }

    @Test
    fun testRuleController_setRules_throwForDuplicateTag() {
        val rules = HashSet<EmbeddingRule>()
        val splitPairRule = createSplitPairRule("Tag1")
        val splitPlaceholderRule = createSplitPlaceholderRule("Tag2")
        rules.add(splitPairRule)
        rules.add(splitPlaceholderRule)
        ruleController.setRules(rules)

        assertEquals(2, ruleController.getRules().size)

        val activityRule = createActivityRule("Tag1")
        rules.add(activityRule)

        assertThrows(IllegalArgumentException::class.java) {
            ruleController.setRules(rules)
        }
    }

    @Test
    fun testRuleController_clearRules() {
        ruleController.clearRules()

        assertTrue(ruleController.getRules().isEmpty())

        val rules = HashSet<EmbeddingRule>()
        val splitPairRule = createSplitPairRule()
        val splitPlaceholderRule = createSplitPlaceholderRule()
        val activityRule = createActivityRule()
        rules.add(splitPairRule)
        rules.add(splitPlaceholderRule)
        rules.add(activityRule)
        ruleController.setRules(rules)

        assertEquals(3, ruleController.getRules().size)

        ruleController.clearRules()

        assertTrue(ruleController.getRules().isEmpty())
    }

    @Test
    fun testRuleResetsOnException() {
        EmbeddingBackend.reset()
        try {
            ActivityEmbeddingRule().apply(
                object : Statement() {
                    override fun evaluate() {
                        throw TestException
                    }
                },
                Description.EMPTY
            ).evaluate()
        } catch (e: TestException) {
            // Throw unexpected exception
        }
        assertFalse(EmbeddingBackend.getInstance(mockActivity) is StubEmbeddingBackend)
    }

    @Test
    fun testOverrideSplitSupportStatus() {
        val expected = SplitController.SplitSupportStatus.SPLIT_ERROR_PROPERTY_NOT_DECLARED
        testRule.overrideSplitSupportStatus(expected)

        val actual = splitController.splitSupportStatus

        assertEquals(expected, actual)
    }

    @Test
    fun testOverrideSplitInfo() = testScope.runTest {
        val expected = listOf(
            TestSplitInfo(
                TestActivityStack(listOf(mockActivity), isEmpty = false),
                TestActivityStack(listOf(mockActivity), isEmpty = false),
            )
        )

        testRule.overrideSplitInfo(mockActivity, expected)

        val actual = splitController.splitInfoList(mockActivity).first().toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testOverrideSplitInfo_updatesExistingListeners() = testScope.runTest {
        val expected1 = listOf(
            TestSplitInfo(
                TestActivityStack(listOf(mockActivity), isEmpty = false),
                TestActivityStack(listOf(mockActivity), isEmpty = false),
            )
        )
        val expected2 = listOf(
            TestSplitInfo(
                TestActivityStack(listOf(mockActivity), isEmpty = false),
                TestActivityStack(listOf(mockActivity), isEmpty = false),
                SplitAttributes(splitType = SPLIT_TYPE_HINGE),
            )
        )

        val value = testScope.async(Dispatchers.Unconfined) {
            splitController.splitInfoList(mockActivity).take(3).toList()
        }
        testRule.overrideSplitInfo(mockActivity, expected1)
        testRule.overrideSplitInfo(mockActivity, expected2)
        runTest(UnconfinedTestDispatcher(testScope.testScheduler)) {
            assertEquals(
                listOf(emptyList(), expected1, expected2),
                value.await()
            )
        }
    }

    private fun createSplitPairRule(tag: String? = null): SplitPairRule {
        return SplitPairRule.Builder(emptySet())
            .setTag(tag)
            .build()
    }

    private fun createSplitPlaceholderRule(tag: String? = null): SplitPlaceholderRule {
        return SplitPlaceholderRule.Builder(emptySet(), Intent())
            .setTag(tag)
            .build()
    }

    private fun createActivityRule(tag: String? = null): ActivityRule {
        return ActivityRule.Builder(emptySet())
            .setTag(tag)
            .build()
    }

    private object TestException : Exception("TEST EXCEPTION")
}