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

private const val METHOD_NAME = "ExpectFailureWithStackTraceTest.expectTwoFailures"

/** Test that stack traces are included in the error message created by Expect. */
class ExpectFailureWithStackTraceTest {

    @get:Rule
    internal val failToExpect: FailingExpect = FailingExpect()

    @Test
    fun expectTwoFailures() {
        failToExpect.delegate.that(4).isNotEqualTo(4)
        failToExpect.delegate.that("abc").contains("x")
    }
}

/** Expect class that can examine the error message */
internal class FailingExpect : TestRule {
    val delegate: Expect = Expect.create()

    override fun apply(base: Statement, description: Description): Statement {
        val s = delegate.apply(base, description)
        return object : Statement() {
            override fun evaluate() {
                var failureMessage = ""
                try {
                    s.evaluate()
                } catch (e: AssertionError) {
                    failureMessage = e.message!!
                }
                // Check that error message contains stack traces. Method name should appear twice,
                // once for each expect error.
                val firstIndex = failureMessage.indexOf(METHOD_NAME)
                assertThat(firstIndex).isGreaterThan(0)
                val secondIndex = failureMessage.indexOf(METHOD_NAME, firstIndex + 1)
                assertThat(secondIndex).isGreaterThan(firstIndex)
            }
        }
    }
}
