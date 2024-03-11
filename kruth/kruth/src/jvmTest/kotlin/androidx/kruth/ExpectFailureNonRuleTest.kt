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

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import org.junit.Test
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.junit.runners.JUnit4

/** Tests for [ExpectFailure] not used as JUnit's TestRule. */
class ExpectFailureNonRuleTest {

    @Test
    fun testExpect_userThrowExceptionInSubject_shouldPropagate() {
        val reportedFailure = mutableListOf<Failure>()
        val runNotifier = RunNotifier()
        runNotifier.addListener(
            object : RunListener() {
                override fun testFailure(failure: Failure) {
                    reportedFailure.add(failure)
                }
            })

        val runner: Runner = JUnit4(ExpectFailureThrowInSubject::class.java)
        runner.run(runNotifier)

        assertThat(reportedFailure).hasSize(2)
        assertThat(reportedFailure[0].exception)
            .hasMessageThat()
            .contains("Throw deliberately")
        assertThat(reportedFailure[1].exception)
            .hasMessageThat()
            .contains("ExpectFailure.whenTesting() invoked, but no failure was caught.")
    }

    @Test
    fun testExpect_userThrowExceptionAfterSubject_shouldPropagate() {
        val reportedFailure = mutableListOf<Failure>()
        val runNotifier = RunNotifier()
        runNotifier.addListener(
            object : RunListener() {
                override fun testFailure(failure: Failure) {
                    reportedFailure.add(failure)
                }
            })

        val runner: Runner = JUnit4(ExpectFailureThrowAfterSubject::class.java)
        runner.run(runNotifier)

        assertThat(reportedFailure).hasSize(2)
        assertThat(reportedFailure[0].exception)
            .hasMessageThat()
            .contains("Throw deliberately")
        assertThat(reportedFailure[1].exception)
            .hasMessageThat()
            .contains("ExpectFailure.whenTesting() invoked, but no failure was caught.")
    }

    /**
     * A test supporting test class which will fail because method in a subject will throw
     * exception.
     */
    @Suppress("IgnoreClassLevelDetector")
    @Ignore("Intentionally failing test that is meant to be run manually above")
    class ExpectFailureThrowInSubject {
        private val expectFailure = ExpectFailure()

        @BeforeTest
        fun setupExpectFailure() {
            expectFailure.enterRuleContext()
        }

        @AfterTest
        fun ensureFailureCaught() {
            expectFailure.ensureFailureCaught()
            expectFailure.leaveRuleContext()
        }

        @Test
        fun testExpect_throwInSubject_shouldPropagate() {
            expectFailure.whenTesting().that(throwingMethod()).isEqualTo(4)
        }
    }

    /**
     * A test supporting test class which will fail because method after a subject will throw
     * exception.
     */
    @Suppress("IgnoreClassLevelDetector")
    @Ignore("Intentionally failing test that is meant to be run manually above")
    class ExpectFailureThrowAfterSubject {
        private val expectFailure: ExpectFailure = ExpectFailure()

        @BeforeTest
        fun setupExpectFailure() {
            expectFailure.enterRuleContext()
        }

        @AfterTest
        fun ensureFailureCaught() {
            expectFailure.ensureFailureCaught()
            expectFailure.leaveRuleContext()
        }

        @Test
        fun testExpect_throwInSubject_shouldPropagate() {
            expectFailure.whenTesting().that(4).isEqualTo(4) // No failure being caught
            throwingMethod()
        }
    }
}

private fun throwingMethod() {
    throw RuntimeException("Throw deliberately")
}
