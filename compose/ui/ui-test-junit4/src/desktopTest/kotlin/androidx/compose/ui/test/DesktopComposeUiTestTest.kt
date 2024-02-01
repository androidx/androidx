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

package androidx.compose.ui.test

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement

@RunWith(JUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DesktopComposeUiTestTest {

    private lateinit var testDescription: Description

    /**
     * Records the current [testDescription] for tests that need to invoke the compose test rule
     * directly.
     */
    @get:Rule
    val testWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            testDescription = description
        }
    }

    @Test
    fun effectContextPropagatedToComposition_createComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createComposeRule(testElement)
        val baseStatement = object : Statement() {
            override fun evaluate() {
                rule.setContent {
                    compositionScope = rememberCoroutineScope()
                }
                rule.waitForIdle()
            }
        }
        rule.apply(baseStatement, testDescription)
            .evaluate()

        val elementFromComposition =
            compositionScope.coroutineContext[TestCoroutineContextElement]
        assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    private class TestCoroutineContextElement : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*> get() = Key

        companion object Key : CoroutineContext.Key<TestCoroutineContextElement>
    }
}
