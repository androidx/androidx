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

package androidx.test.ext.junitgtesttest

import androidx.test.ext.junitgtest.GtestRunner
import androidx.test.ext.junitgtest.TargetLibrary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

/**
 * Tests the GtestRunner
 *
 * These specific tests would be more appropriate to put in junit-gtest itself, and have an
 * integration test app run the tests more like an actual app consuming the library would (basically
 * the [NativeTests] class), but due to b/236913987 it is currently difficult or impossible to
 * make the test libraries ('apptest') available to the tests without including them in the release
 * AAR.
 */
class GtestRunnerTest {
    private val runListener = CountingRunListener()
    private val runNotifier = RunNotifier().apply {
        addListener(runListener)
    }
    companion object {
        private val runner = GtestRunner(NativeTests::class.java)
    }

    @Test
    fun runsTheTest() {
        runner.run(runNotifier)
        assertThat(runListener.failures).hasSize(2)
        val adderFailure = runListener.failures[0]
        assertThat(adderFailure.message.normalizeWhitespace()).contains(
            """
                Expected equality of these values:
                42
                add(42, 1)
                Which is: 43
            """.normalizeWhitespace()
        )

        val uncaughtExceptionFailure = runListener.failures[1]
        assertThat(uncaughtExceptionFailure.message.normalizeWhitespace()).contains(
            """
                unknown file:-1
                Unknown C++ exception thrown in the test body.
            """.normalizeWhitespace()
        )
    }

    @Test
    fun reportsAllResults() {
        runner.run(runNotifier)
        assertThat(runListener.descriptions.map { it.displayName }).isEqualTo(
            listOf(
                "adder_pass(androidx.test.ext.junitgtesttest.GtestRunnerTest\$NativeTests)",
                "foo_fail(androidx.test.ext.junitgtesttest.GtestRunnerTest\$NativeTests)",
                "JUnitNotifyingListener_handles_null_file_names(androidx.test.ext.junitgtesttest." +
                    "GtestRunnerTest\$NativeTests)"
            )

        )
    }

    fun String.normalizeWhitespace(): String {
        return replace("\\s+".toRegex(), " ").trim()
    }

    class CountingRunListener : RunListener() {
        val failures = mutableListOf<Failure>()
        val descriptions = mutableListOf<Description>()
        override fun testFailure(failure: Failure) {
            failures.add(failure)
        }

        override fun testFinished(description: Description) {
            descriptions.add(description)
        }
    }

    @RunWith(GtestRunner::class)
    @TargetLibrary(libraryName = "apptest")
    class NativeTests
}
