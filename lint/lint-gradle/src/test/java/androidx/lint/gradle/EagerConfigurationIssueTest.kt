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
class EagerConfigurationIssueTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.EAGER_CONFIGURATION_ISSUE)
    ) {
    @Test
    fun `Test usage of TaskContainer#create`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.create("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use register instead of create [EagerGradleConfiguration]
                project.tasks.create("example")
                              ~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with register:
            @@ -4 +4
            -     project.tasks.create("example")
            +     project.tasks.register("example")
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of unrelated create method`() {
        val input =
            kotlin(
                """
                interface Bar
                class Foo : Bar {
                    fun create() = Unit
                }
                fun foo() {
                    Foo().create()
                }
            """
                    .trimIndent()
            )
        check(input).expectClean()
    }

    @Test
    fun `Test usage of TaskContainer#getByName`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.getByName("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use named instead of getByName [EagerGradleConfiguration]
                project.tasks.getByName("example")
                              ~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with named:
            @@ -4 +4
            -     project.tasks.getByName("example")
            +     project.tasks.named("example")
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of DomainObjectCollection#all`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.all(action)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:6: Error: Use configureEach instead of all [EagerGradleConfiguration]
                project.tasks.all(action)
                              ~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.all(action)
            +     project.tasks.configureEach(action)
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskContainer#whenTaskAdded`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.whenTaskAdded(action)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:6: Error: Use configureEach instead of whenTaskAdded [EagerGradleConfiguration]
                project.tasks.whenTaskAdded(action)
                              ~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.whenTaskAdded(action)
            +     project.tasks.configureEach(action)
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskContainer with kotlin extension functions`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.any()
                    project.tasks.any { it.enabled }
                    project.tasks.map { it.name }
                    project.tasks.mapNotNull { it.name }
                    project.tasks.groupBy { it.group }
                    project.tasks.forEach { it.enabled = true }
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:6: Error: Avoid using method any [EagerGradleConfiguration]
                project.tasks.any()
                              ~~~
            src/test.kt:7: Error: Avoid using method any [EagerGradleConfiguration]
                project.tasks.any { it.enabled }
                              ~~~
            src/test.kt:8: Error: Avoid using method map [EagerGradleConfiguration]
                project.tasks.map { it.name }
                              ~~~
            src/test.kt:9: Error: Avoid using method mapNotNull [EagerGradleConfiguration]
                project.tasks.mapNotNull { it.name }
                              ~~~~~~~~~~
            src/test.kt:10: Error: Avoid using method groupBy [EagerGradleConfiguration]
                project.tasks.groupBy { it.group }
                              ~~~~~~~
            src/test.kt:11: Error: Avoid using method forEach [EagerGradleConfiguration]
                project.tasks.forEach { it.enabled = true }
                              ~~~~~~~
            6 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs = ""

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of DomainObjectCollection#whenObjectAdded`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Action
                import org.gradle.api.Project
                import org.gradle.api.Task

                fun configure(project: Project, action: Action<Task>) {
                    project.tasks.whenObjectAdded(action)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:6: Error: Use configureEach instead of whenObjectAdded [EagerGradleConfiguration]
                project.tasks.whenObjectAdded(action)
                              ~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 6: Replace with configureEach:
            @@ -6 +6
            -     project.tasks.whenObjectAdded(action)
            +     project.tasks.configureEach(action)
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskCollection#getAt`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.getAt("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use named instead of getAt [EagerGradleConfiguration]
                project.tasks.getAt("example")
                              ~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with named:
            @@ -4 +4
            -     project.tasks.getAt("example")
            +     project.tasks.named("example")
        """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of TaskContainer#getByPath`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.getByPath("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method getByPath [EagerGradleConfiguration]
                project.tasks.getByPath("example")
                              ~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskContainer#findByName`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.findByName("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method findByName [EagerGradleConfiguration]
                project.tasks.findByName("example")
                              ~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskContainer#findByPath`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.findByPath("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method findByPath [EagerGradleConfiguration]
                project.tasks.findByPath("example")
                              ~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskContainer#replace`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.replace("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method replace [EagerGradleConfiguration]
                project.tasks.replace("example")
                              ~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskContainer#remove`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project, task: Task) {
                    project.tasks.remove(task)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method remove [EagerGradleConfiguration]
                project.tasks.remove(task)
                              ~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskContainer#iterator`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.findByPath("example")
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method findByPath [EagerGradleConfiguration]
                project.tasks.findByPath("example")
                              ~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of NamedDomainObjectCollection#findAll`() {
        val input =
            kotlin(
                """
                import groovy.lang.Closure
                import org.gradle.api.Project

                fun configure(project: Project, closure: Closure) {
                    project.tasks.findAll(closure)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:5: Error: Avoid using method findAll [EagerGradleConfiguration]
                project.tasks.findAll(closure)
                              ~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskCollection#matching`() {
        val input =
            kotlin(
                """
                import groovy.lang.Closure
                import org.gradle.api.Project

                fun configure(project: Project, closure: Closure) {
                    project.tasks.matching(closure)
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:5: Error: Avoid using method matching [EagerGradleConfiguration]
                project.tasks.matching(closure)
                              ~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of TaskProvider#get`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.register("example").get()
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method get [EagerGradleConfiguration]
                project.tasks.register("example").get()
                                                  ~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }
}
