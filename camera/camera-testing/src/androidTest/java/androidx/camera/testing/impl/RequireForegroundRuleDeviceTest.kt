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

package androidx.camera.testing.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/** Instrumented tests for [RequireForegroundRule]. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RequireForegroundRuleDeviceTest {

    @Test
    fun deferCleanup_executedOnSuccess() {
        val rule = RequireForegroundRule()
        val description =
            Description.createTestDescription(
                RequireForegroundRuleDeviceTest::class.java,
                "deferCleanup_executedOnSuccess",
            )
        var cleanedUp = false

        val successStatement =
            object : Statement() {
                override fun evaluate() {
                    rule.deferCleanup { cleanedUp = true }
                }
            }

        rule.apply(successStatement, description).evaluate()
        assertThat(cleanedUp).isTrue()
    }

    @Test
    fun deferCleanup_executedOnFailure() {
        val rule = RequireForegroundRule()
        val description =
            Description.createTestDescription(
                RequireForegroundRuleDeviceTest::class.java,
                "deferCleanup_executedOnFailure",
            )
        var cleanedUp = false

        val failingStatement =
            object : Statement() {
                override fun evaluate() {
                    rule.deferCleanup { cleanedUp = true }
                    throw AssertionError("Simulated test failure")
                }
            }

        var caught: Throwable? = null
        try {
            rule.apply(failingStatement, description).evaluate()
        } catch (t: Throwable) {
            caught = t
        }

        assertThat(caught).isInstanceOf(AssertionError::class.java)
        assertThat(caught).hasMessageThat().contains("Simulated test failure")
        assertThat(cleanedUp).isTrue()
    }

    @Test
    fun deferCleanup_executedInLifoOrder() {
        val rule = RequireForegroundRule()
        val description =
            Description.createTestDescription(
                RequireForegroundRuleDeviceTest::class.java,
                "deferCleanup_executedInLifoOrder",
            )
        val executionOrder = mutableListOf<Int>()

        val statement =
            object : Statement() {
                override fun evaluate() {
                    rule.deferCleanup { executionOrder.add(1) }
                    rule.deferCleanup { executionOrder.add(2) }
                    rule.deferCleanup { executionOrder.add(3) }
                }
            }

        rule.apply(statement, description).evaluate()
        assertThat(executionOrder).containsExactly(3, 2, 1).inOrder()
    }
}
