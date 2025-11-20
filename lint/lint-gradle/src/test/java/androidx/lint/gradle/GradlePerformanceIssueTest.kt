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

package androidx.lint.gradle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GradlePerformanceIssueTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.PERFORMANCE_ISSUE),
    ) {
    @Test
    fun `Test usage of mustRunAfter`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Task

                fun configureTask(task: Task) {
                    task.shouldRunAfter(":oneTask")
                    task.mustRunAfter(":anotherTask")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
                src/test.kt:4: Error: Avoid using method shouldRunAfter [GradlePerformance]
                    task.shouldRunAfter(":oneTask")
                         ~~~~~~~~~~~~~~
                src/test.kt:5: Error: Avoid using method mustRunAfter [GradlePerformance]
                    task.mustRunAfter(":anotherTask")
                         ~~~~~~~~~~~~
                2 errors
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs("")
    }
}
