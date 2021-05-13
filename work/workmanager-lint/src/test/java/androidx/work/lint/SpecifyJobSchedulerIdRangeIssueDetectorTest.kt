/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.work.lint

import androidx.work.lint.Stubs.JOB_SERVICE
import androidx.work.lint.Stubs.WORK_MANAGER_CONFIGURATION_PROVIDER
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class SpecifyJobSchedulerIdRangeIssueDetectorTest {

    @Ignore("b/187541663")
    @Test
    fun failWhenUsingCustomJobServiceAndIdsAreNotSpecified() {
        val service = kotlin(
            "com/example/TestJobService.kt",
            """
            package com.example

            import android.app.job.JobService

            class TestJobService: JobService() {

            }
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            JOB_SERVICE,
            service
        ).issues(SpecifyJobSchedulerIdRangeIssueDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/TestJobService.kt:5: Warning: Specify a valid range of job id's for WorkManager to use. [SpecifyJobSchedulerIdRange]
                class TestJobService: JobService() {
                ^
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun succeedWhenJobSchedulerIdRangeIsSpecified() {
        val service = kotlin(
            "com/example/TestJobService.kt",
            """
            package com.example

            import android.app.job.JobService

            class TestJobService: JobService() {

            }
            """
        ).indented().within("src")

        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.Configuration

            class Test {
                fun buildConfiguration() {
                   val builder = Configuration.Builder()
                   builder.setJobSchedulerJobIdRange(0, 1000)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            JOB_SERVICE,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            service,
            snippet
        ).issues(SpecifyJobSchedulerIdRangeIssueDetector.ISSUE)
            .run()
            .expectClean()
    }
}
