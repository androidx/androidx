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

import androidx.work.lint.Stubs.LISTENABLE_WORKER
import androidx.work.lint.Stubs.WORKER_FACTORY
import androidx.work.lint.Stubs.WORK_MANAGER_CONFIGURATION_PROVIDER
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class WorkerHasPublicModifierDetectorTest {
    @Ignore("b/187541663")
    @Test
    fun testWithPrivateWorker() {
        val worker = kotlin(
            "com/example/Worker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            private class Worker: ListenableWorker()
            """
        ).indented().within("src")

        /* ktlint-disable max-line-length */
        lint().files(
            // Source files
            LISTENABLE_WORKER,
            worker
        ).issues(WorkerHasPublicModifierDetector.ISSUE)
            .run()
            .expect(
                """
                src/com/example/Worker.kt:5: Error: com.example.Worker needs to be public [WorkerHasAPublicModifier]
                private class Worker: ListenableWorker()
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }

    @Test
    fun testWithPublicWorker() {
        val worker = kotlin(
            "com/example/Worker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            class Worker: ListenableWorker()
            """
        ).indented().within("src")

        lint().files(
            // Source files
            LISTENABLE_WORKER,
            worker
        ).issues(WorkerHasPublicModifierDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testWithPrivateWorkerAndCustomFactory() {
        val worker = kotlin(
            "com/example/Worker.kt",
            """
            package com.example

            import androidx.work.ListenableWorker

            private class Worker: ListenableWorker()
            """
        ).indented().within("src")

        val snippet = kotlin(
            "com/example/Test.kt",
            """
            package com.example

            import androidx.work.Configuration

            class Test {
                fun buildConfiguration() {
                   val factory: WorkerFactory = TODO()
                   val builder = Configuration.Builder(factory)
                   builder.setWorkerFactory(factory)
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Source files
            WORKER_FACTORY,
            LISTENABLE_WORKER,
            WORK_MANAGER_CONFIGURATION_PROVIDER,
            worker,
            snippet
        ).issues(WorkerHasPublicModifierDetector.ISSUE)
            .run()
            .expectClean()
    }
}
