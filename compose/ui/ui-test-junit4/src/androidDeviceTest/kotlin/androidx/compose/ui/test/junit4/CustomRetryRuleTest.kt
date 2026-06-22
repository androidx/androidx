/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.material.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class CustomRetryRuleTest {

    var failCount = 0
    private val composeRule = createComposeRule()
    private val retryRule = RetryRule(maxRetries = 5)

    @get:Rule val testRuleChain: RuleChain = RuleChain.outerRule(retryRule).around(composeRule)

    @Test
    fun testThatFailsWithCoroutinesException() {
        failCount++
        composeRule.setContent { Text("Hello Compose") }
        composeRule.onNodeWithText("Hello Compose").assertExists()
        if (failCount < 5) fail("###: Fail count = $failCount")
    }
}

class RetryRule(private val maxRetries: Int) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                var throwable: Throwable? = null
                repeat(maxRetries) {
                    try {
                        base.evaluate()
                        return
                    } catch (ex: Throwable) {
                        throwable = ex
                    }
                }
                throw throwable ?: Throwable("Test Failed")
            }
        }
    }
}
