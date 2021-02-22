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

import androidx.work.lint.Stubs.RX_WORKER
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class RxWorkerSetProgressDetectorTest {
    @Test
    fun setProgressDetectorTest() {
        val application = kotlin(
            "com/example/App.kt",
            """
            package com.example

            import androidx.work.RxWorker

            class App {
                fun onCreate() {
                    val worker = RxWorker()
                    worker.setProgress()
                }
            }
            """
        ).indented().within("src")

        lint().files(
            // Source files
            RX_WORKER,
            application
        ).issues(RxWorkerSetProgressDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/App.kt:8: Error: setProgress is deprecated. Use setCompletableProgress instead. [UseRxSetProgress2]
                        worker.setProgress()
                        ~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/App.kt line 8: Use setCompletableProgress instead:
                @@ -8 +8
                -         worker.setProgress()
                +         worker.setCompletableProgress()
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
