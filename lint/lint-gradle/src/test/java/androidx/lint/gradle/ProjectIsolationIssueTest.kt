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
class ProjectIsolationIssueTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.PROJECT_ISOLATION_ISSUE)
    ) {
    @Test
    fun `Test usage of TaskContainer#create`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.findProperty("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use providers.gradleProperty instead of findProperty [GradleProjectIsolation]
                project.findProperty("example")
                        ~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with providers.gradleProperty:
            @@ -4 +4
            -     project.findProperty("example")
            +     project.providers.gradleProperty("example")
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
