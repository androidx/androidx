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
class EagerTaskConfigurationDetectorTest : GradleLintDetectorTest(
    detector = EagerTaskConfigurationDetector(),
    issues = listOf(EagerTaskConfigurationDetector.ISSUE)
) {
    @Test
    fun `Test usage of TaskContainer#create`() {
        val input = kotlin(
            """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.create("example")
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:4: Error: Use register instead of create [EagerGradleTaskConfiguration]
                project.tasks.create("example")
                              ~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 4: Replace with register:
            @@ -4 +4
            -     project.tasks.create("example")
            +     project.tasks.register("example")
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskContainer#getByName`() {
        val input = kotlin(
            """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.getByName("example")
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:4: Error: Use named instead of getByName [EagerGradleTaskConfiguration]
                project.tasks.getByName("example")
                              ~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 4: Replace with named:
            @@ -4 +4
            -     project.tasks.getByName("example")
            +     project.tasks.named("example")
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of DomainObjectCollection#all`() {
        val input = kotlin(
            """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.all(action)
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:6: Error: Use configureEach instead of all [EagerGradleTaskConfiguration]
                project.tasks.all(action)
                              ~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.all(action)
            +     project.tasks.configureEach(action)
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskContainer#whenTaskAdded`() {
        val input = kotlin(
            """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.whenTaskAdded(action)
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:6: Error: Use configureEach instead of whenTaskAdded [EagerGradleTaskConfiguration]
                project.tasks.whenTaskAdded(action)
                              ~~~~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.whenTaskAdded(action)
            +     project.tasks.configureEach(action)
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of DomainObjectCollection#whenObjectAdded`() {
        val input = kotlin(
            """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.whenObjectAdded(action)
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:6: Error: Use configureEach instead of whenObjectAdded [EagerGradleTaskConfiguration]
                project.tasks.whenObjectAdded(action)
                              ~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.whenObjectAdded(action)
            +     project.tasks.configureEach(action)
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskCollection#getAt`() {
        val input = kotlin(
            """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.getAt("example")
                }
            """.trimIndent()
        )

        val expected = """
            src/test.kt:4: Error: Use named instead of getAt [EagerGradleTaskConfiguration]
                project.tasks.getAt("example")
                              ~~~~~
            1 errors, 0 warnings
        """.trimIndent()
        val expectedFixDiffs = """
            Fix for src/test.kt line 4: Replace with named:
            @@ -4 +4
            -     project.tasks.getAt("example")
            +     project.tasks.named("example")
        """.trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }
}
