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

package androidx.window.testing.emedding

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.testing.TestActivity
import androidx.window.testing.embedding.ActivityEmbeddingTestRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

/** Test for [ActivityEmbeddingTestRule]. */
@OptIn(ExperimentalWindowApi::class)
class ActivityEmbeddingTestRuleTest {
    private val activityRule = ActivityScenarioRule(TestActivity::class.java)
    private val activityEmbeddingTestRule = ActivityEmbeddingTestRule()

    @get:Rule
    val testRule: TestRule

    init {
        testRule = RuleChain.outerRule(activityEmbeddingTestRule).around(activityRule)
    }

    @Test
    fun testActivityEmbeddingController_overrideIsActivityEmbedded() {
        activityRule.scenario.onActivity { activity ->
            assertFalse(ActivityEmbeddingController.getInstance(activity)
                .isActivityEmbedded(activity))

            activityEmbeddingTestRule.overrideIsActivityEmbedded(
                activity,
                true /* isActivityEmbedded*/
            )
            assertTrue(ActivityEmbeddingController.getInstance(activity)
                .isActivityEmbedded(activity))

            activityEmbeddingTestRule.overrideIsActivityEmbedded(
                activity,
                false /* isActivityEmbedded*/
            )
            assertFalse(ActivityEmbeddingController.getInstance(activity)
                .isActivityEmbedded(activity))
        }
    }
}