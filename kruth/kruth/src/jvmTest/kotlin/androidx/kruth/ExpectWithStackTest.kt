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

package androidx.kruth

import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ExpectWithStackTest {
    private val expectWithTrace: Expect = Expect.create()

    @get:Rule
    internal val verifyAssertionError: TestRuleVerifier = TestRuleVerifier(expectWithTrace)

    @Test
    fun testExpectTrace_simpleCase() {
        verifyAssertionError.errorVerifier = ErrorVerifier { expected ->
            assertThat(expected.getStackTrace()).hasLength(0)
            assertThat(expected).hasMessageThat().startsWith("3 expectations failed:")
        }

        expectWithTrace.that(true).isFalse()
        expectWithTrace.that("Hello").isNull()
        expectWithTrace.that(1).isEqualTo(2)
    }

    @Test
    fun testExpectTrace_loop() {
        verifyAssertionError.errorVerifier = ErrorVerifier { expected ->
            assertThat(expected.getStackTrace()).hasLength(0)
            assertThat(expected).hasMessageThat().startsWith("4 expectations failed:")
            assertWithMessage("test method name should only show up once with following omitted")
                .that(expected.message!!.split("testExpectTrace_loop").toTypedArray())
                .hasLength(2)
        }

        repeat(4) { expectWithTrace.that(true).isFalse() }
    }

    @Test
    fun testExpectTrace_callerException() {
        verifyAssertionError.errorVerifier = ErrorVerifier { expected ->
            assertThat(expected.getStackTrace()).hasLength(0)
            assertThat(expected).hasMessageThat().startsWith("2 expectations failed:")
        }

        expectWithTrace.that(true).isFalse()
        expectWithTrace
            .that(alwaysFailWithCause())
            .isEqualTo(5)
    }

    @Test
    fun testExpectTrace_onlyCallerException() {
        verifyAssertionError.errorVerifier = ErrorVerifier { expected ->
            assertWithMessage("Should throw exception as it is if only caller exception")
                .that(expected.getStackTrace().size)
                .isAtLeast(2)
        }

        expectWithTrace
            .that(alwaysFailWithCause())
            .isEqualTo(5)
    }
}

internal fun interface ErrorVerifier {
    fun verify(error: AssertionError)
}

internal class TestRuleVerifier(private val ruleToVerify: TestRule) : TestRule {
    var errorVerifier: ErrorVerifier = ErrorVerifier {}

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    ruleToVerify.apply(base, description).evaluate()
                } catch (caught: AssertionError) {
                    errorVerifier.verify(caught)
                }
            }
        }
    }
}

private fun alwaysFailWithCause() {
    throw AssertionError("Always fail", RuntimeException("First", RuntimeException("Second", null)))
}
