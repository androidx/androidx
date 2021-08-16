/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.work.lint

import androidx.work.lint.Stubs.LISTENABLE_WORKER
import androidx.work.lint.Stubs.ONE_TIME_WORK_REQUEST
import androidx.work.lint.Stubs.PERIODIC_WORK_REQUEST
import androidx.work.lint.Stubs.WORK_MANAGER
import androidx.work.lint.Stubs.WORK_REQUEST
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class PeriodicEnqueueIssueDetectorTest {

    @Test
    fun warnWhenEnqueueingPeriodicWork() {
        val snippet = kotlin(
            "com/example/Snippet.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import androidx.work.WorkManager

            class Test {
                fun enqueueWork() {
                    val request = PeriodicWorkRequest()
                    val workManager: WorkManager = TODO("Get an implementation of WorkManager")
                    workManager.enqueue(request)
                }
            }
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            WORK_MANAGER,
            WORK_REQUEST,
            ONE_TIME_WORK_REQUEST,
            PERIODIC_WORK_REQUEST,
            LISTENABLE_WORKER,
            snippet
        ).issues(PeriodicEnqueueIssueDetector.ISSUE)
            .run()
            .expect(
                """
                src/androidx/work/WorkManager.kt:4: Warning: Use enqueueUniquePeriodicWork() instead of enqueue() [BadPeriodicWorkRequestEnqueue]
                   fun enqueue(request: WorkRequest)
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun warnWhenEnqueuingListOfPeriodicWork() {
        val snippet = kotlin(
            "com/example/Snippet.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import androidx.work.WorkManager

            class Test {
                fun enqueueWork() {
                    val requests = listOf(PeriodicWorkRequest())
                    val workManager: WorkManager = TODO("Get an implementation of WorkManager")
                    workManager.enqueue(requests)
                }
            }
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            WORK_MANAGER,
            WORK_REQUEST,
            ONE_TIME_WORK_REQUEST,
            PERIODIC_WORK_REQUEST,
            LISTENABLE_WORKER,
            snippet
        ).issues(PeriodicEnqueueIssueDetector.ISSUE)
            .run()
            .expect(
                """
                src/androidx/work/WorkManager.kt:5: Warning: Use enqueueUniquePeriodicWork() instead of enqueue() [BadPeriodicWorkRequestEnqueue]
                   fun enqueue(requests: List<WorkRequest>)
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun warnWhenEnqueuingListOfPeriodicWorkAndOneTimeWork() {
        val snippet = kotlin(
            "com/example/Snippet.kt",
            """
            package com.example

            import androidx.work.WorkRequest
            import androidx.work.OneTimeWorkRequest
            import androidx.work.PeriodicWorkRequest
            import androidx.work.WorkManager

            class Test {
                fun enqueueWork() {
                    val requests = listOf(OneTimeWorkRequest(), PeriodicWorkRequest())
                    val workManager: WorkManager = TODO("Get an implementation of WorkManager")
                    workManager.enqueue(requests)
                }
            }
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            WORK_MANAGER,
            WORK_REQUEST,
            ONE_TIME_WORK_REQUEST,
            PERIODIC_WORK_REQUEST,
            LISTENABLE_WORKER,
            snippet
        ).issues(PeriodicEnqueueIssueDetector.ISSUE)
            .run()
            .expect(
                """
                src/androidx/work/WorkManager.kt:5: Warning: Use enqueueUniquePeriodicWork() instead of enqueue() [BadPeriodicWorkRequestEnqueue]
                   fun enqueue(requests: List<WorkRequest>)
                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun noWarningsWhenUsingOneTimeWorkRequest() {
        val snippet = kotlin(
            "com/example/Snippet.kt",
            """
            package com.example

            import androidx.work.OneTimeWorkRequest
            import androidx.work.PeriodicWorkRequest
            import androidx.work.WorkManager

            class Test {
                fun enqueueWork() {
                    val request = OneTimeWorkRequest()
                    val workManager: WorkManager = TODO("Get an implementation of WorkManager")
                    workManager.enqueue(request)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            WORK_MANAGER,
            WORK_REQUEST,
            ONE_TIME_WORK_REQUEST,
            PERIODIC_WORK_REQUEST,
            LISTENABLE_WORKER,
            snippet
        ).issues(PeriodicEnqueueIssueDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun noWarningsWhenEnqueuingListOfOneTimeWorkRequests() {
        val snippet = kotlin(
            "com/example/Snippet.kt",
            """
            package com.example

            import androidx.work.OneTimeWorkRequest
            import androidx.work.PeriodicWorkRequest
            import androidx.work.WorkManager

            class Test {
                fun enqueueWork() {
                    val requests = listOf(OneTimeWorkRequest())
                    val workManager: WorkManager = TODO("Get an implementation of WorkManager")
                    workManager.enqueue(requests)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            WORK_MANAGER,
            WORK_REQUEST,
            ONE_TIME_WORK_REQUEST,
            PERIODIC_WORK_REQUEST,
            LISTENABLE_WORKER,
            snippet
        ).issues(PeriodicEnqueueIssueDetector.ISSUE)
            .run()
            .expectClean()
    }
}
