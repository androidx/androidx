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

package androidx.compose.ui.test.junit4

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/**
 * Tests for passing a custom CoroutineContext to one of [ComposeTestRule]s. Similar tests are
 * available for [runComposeUiTest] in compose:ui:ui-test
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class CustomEffectContextRuleTest {

    private lateinit var testDescription: Description

    /**
     * Records the current [testDescription] so it can be used when manually applying the
     * [ComposeTestRule].
     */
    @get:Rule
    val testWatcher =
        object : TestWatcher() {
            override fun starting(description: Description) {
                testDescription = description
            }
        }

    @Test
    fun effectContextPropagatedToComposition_createComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createComposeRule(testElement)
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    rule.setContent { compositionScope = rememberCoroutineScope() }
                    rule.waitForIdle()
                }
            }
        rule.apply(baseStatement, testDescription).evaluate()

        val elementFromComposition = compositionScope.coroutineContext[TestCoroutineContextElement]
        Truth.assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun effectContextPropagatedToComposition_createAndroidComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createAndroidComposeRule<ComponentActivity>(testElement)
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    rule.setContent { compositionScope = rememberCoroutineScope() }
                    rule.waitForIdle()
                }
            }
        rule.apply(baseStatement, testDescription).evaluate()

        val elementFromComposition = compositionScope.coroutineContext[TestCoroutineContextElement]
        Truth.assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun effectContextPropagatedToComposition_createEmptyComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val composeRule = createEmptyComposeRule(testElement)
        val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
        val baseStatement =
            object : Statement() {
                override fun evaluate() {
                    activityRule.scenario.onActivity {
                        it.setContent { compositionScope = rememberCoroutineScope() }
                    }
                    composeRule.waitForIdle()
                }
            }
        activityRule
            .apply(composeRule.apply(baseStatement, testDescription), testDescription)
            .evaluate()

        val elementFromComposition = compositionScope.coroutineContext[TestCoroutineContextElement]
        Truth.assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    private class TestCoroutineContextElement : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*>
            get() = Key

        companion object Key : CoroutineContext.Key<TestCoroutineContextElement>
    }
}
