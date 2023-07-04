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

package androidx.work.lint

import androidx.work.lint.Stubs.LISTENABLE_WORKER
import androidx.work.lint.Stubs.PERIODIC_WORK_REQUEST
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class InvalidPeriodicWorkRequestIntervalDetectorTest {
    @Test
    fun testWithInvalidDurationTimeUnits() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker()
            """
        ).indented().within("src")
        /* ktlint-disable max-line-length */
        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.util.concurrent.TimeUnit

            class Test {
                fun enqueue() {
                    val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, 15L, TimeUnit.MILLISECONDS)
                }
            }
            """
        ).indented().within("src")
        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Test.kt:9: Error: Interval duration for `PeriodicWorkRequest`s must be at least 15 minutes. [InvalidPeriodicWorkRequestInterval]
                        val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, 15L, TimeUnit.MILLISECONDS)
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWithValidDurationTimeUnits() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker()
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.util.concurrent.TimeUnit

            class Test {
                fun enqueue() {
                    val worker = TestWorker()
                    val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, 15L, TimeUnit.MINUTES)
                }
            }
            """
        ).indented().within("src")
        /* ktlint-enable max-line-length */

        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testWithInvalidDurationTypeStaticImport() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker()
            """
        ).indented().within("src")
        /* ktlint-disable max-line-length */
        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.time.Duration.ofNanos

            class Test {
                fun enqueue() {
                    val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, ofNanos(10L))
                }
            }
            """
        ).indented().within("src")

        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Test.kt:9: Error: Interval duration for `PeriodicWorkRequest`s must be at least 15 minutes. [InvalidPeriodicWorkRequestInterval]
                        val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, ofNanos(10L))
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWithInvalidDurationType() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker()
            """
        ).indented().within("src")
        /* ktlint-disable max-line-length */
        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.time.Duration

            class Test {
                fun enqueue() {
                    val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, Duration.ofSeconds(10L))
                }
            }
            """
        ).indented().within("src")

        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Test.kt:9: Error: Interval duration for `PeriodicWorkRequest`s must be at least 15 minutes. [InvalidPeriodicWorkRequestInterval]
                        val builder = PeriodicWorkRequest.Builder(TestWorker::class.java, Duration.ofSeconds(10L))
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWithValidDurationType() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker() {

            }
            """
        ).indented().within("src")

        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.time.Duration

            class Test {
                fun enqueue() {
                    val worker = TestWorker()
                    val builder = PeriodicWorkRequest.Builder(worker, Duration.ofMinutes(15))
                }
            }
            """
        ).indented().within("src")

        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testWithPeriodicRequestHelper() {
        val worker = kotlin(
            "com/example/TestWorker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class TestWorker: ListenableWorker() {

            }
            """
        ).indented().within("src")

        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.PeriodicWorkRequest
            import com.example.TestWorker
            import java.util.concurrent.TimeUnit

            class Test {
                fun buildPeriodicRequest(interval: Long) {
                    val worker = TestWorker()
                    val builder = PeriodicWorkRequest.Builder(worker, interval, TimeUnit.MINUTES)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            LISTENABLE_WORKER,
            PERIODIC_WORK_REQUEST,
            worker,
            snippet
        ).issues(InvalidPeriodicWorkRequestIntervalDetector.ISSUE)
            .run()
            .expectClean()
    }
}
