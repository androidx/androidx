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
        issues = listOf(DiscouragedGradleMethodDetector.PROJECT_ISOLATION_ISSUE),
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

    @Test
    fun `Test direct rootProject access should fail`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    val root = project.getRootProject().tasks
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use isolated.rootProject instead of getRootProject [GradleProjectIsolation]
                val root = project.getRootProject().tasks
                                   ~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with isolated.rootProject:
            @@ -4 +4
            -     val root = project.getRootProject().tasks
            +     val root = project.isolated.rootProject().tasks
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test rootProject access via project rootProject isolated should fail`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    val root = project.getRootProject().getIsolated().tasks
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use isolated.rootProject instead of getRootProject [GradleProjectIsolation]
                val root = project.getRootProject().getIsolated().tasks
                                   ~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with isolated.rootProject:
            @@ -4 +4
            -     val root = project.getRootProject().getIsolated().tasks
            +     val root = project.isolated.rootProject().getIsolated().tasks
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test safe rootProject isolated access via project isolated`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    val root = project.getIsolated().getRootProject()
                }
                """
                    .trimIndent()
            )
        check(input).expectClean()
    }

    @Test
    fun `Test usage of evaluationDependsOn`() {
        val input =
            kotlin(
                """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.evaluationDependsOn(":foo:bar")
                    project.evaluationDependsOnChildren()
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method evaluationDependsOn [GradleProjectIsolation]
                project.evaluationDependsOn(":foo:bar")
                        ~~~~~~~~~~~~~~~~~~~
            src/test.kt:5: Error: Avoid using method evaluationDependsOnChildren [GradleProjectIsolation]
                project.evaluationDependsOnChildren()
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~
            2 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of getAt on build service registrations`() {
        val input =
            kotlin(
                """
                import org.gradle.api.invocation.Gradle

                fun getService(gradle: Gradle) {
                    gradle.sharedServices.registrations.getAt("example")
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use findByName instead of getAt on the build service registrations collection [GradleProjectIsolation]
                gradle.sharedServices.registrations.getAt("example")
                                                    ~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with findByName:
            @@ -4 +4
            -     gradle.sharedServices.registrations.getAt("example")
            +     gradle.sharedServices.registrations.findByName("example")
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of named on build service registrations`() {
        val input =
            kotlin(
                """
                import org.gradle.api.invocation.Gradle

                fun getService(gradle: Gradle) {
                    gradle.sharedServices.registrations.named("example")
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use findByName instead of named on the build service registrations collection [GradleProjectIsolation]
                gradle.sharedServices.registrations.named("example")
                                                    ~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with findByName:
            @@ -4 +4
            -     gradle.sharedServices.registrations.named("example")
            +     gradle.sharedServices.registrations.findByName("example")
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of getByName on build service registrations`() {
        val input =
            kotlin(
                """
                import org.gradle.api.invocation.Gradle

                fun getService(gradle: Gradle) {
                    gradle.sharedServices.registrations.getByName("example")
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use findByName instead of getByName on the build service registrations collection [GradleProjectIsolation]
                gradle.sharedServices.registrations.getByName("example")
                                                    ~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()
        val expectedFixDiffs =
            """
            Fix for src/test.kt line 4: Replace with findByName:
            @@ -4 +4
            -     gradle.sharedServices.registrations.getByName("example")
            +     gradle.sharedServices.registrations.findByName("example")
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Test usage of findAll on build service registrations`() {
        val input =
            kotlin(
                """
                import groovy.lang.Closure
                import org.gradle.api.invocation.Gradle

                fun findServices(gradle: Gradle, closure: Closure) {
                    gradle.sharedServices.registrations.findAll(closure)
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:5: Error: Avoid using method findAll on the build service registrations collection [GradleProjectIsolation]
                gradle.sharedServices.registrations.findAll(closure)
                                                    ~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test allowed usages of build service registrations`() {
        val input =
            kotlin(
                """
                import org.gradle.api.invocation.Gradle

                fun configureServices(gradle: Gradle) {
                    gradle.sharedServices.registerIfAbsent("example", Any::class.java)
                    gradle.sharedServices.registrations.findByName("example")
                    gradle.sharedServices.registrations.let { }
                }
                """
                    .trimIndent()
            )

        check(input).expectClean()
    }

    @Test
    fun `Test getAt on other collections is not a project isolation violation`() {
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

        check(input).expectClean()
    }
}
