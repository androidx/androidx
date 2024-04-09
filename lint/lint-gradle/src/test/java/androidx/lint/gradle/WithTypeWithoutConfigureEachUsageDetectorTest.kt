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
class WithTypeWithoutConfigureEachUsageDetectorTest : GradleLintDetectorTest(
    detector = WithTypeWithoutConfigureEachUsageDetector(),
    issues = listOf(WithTypeWithoutConfigureEachUsageDetector.ISSUE)

) {
    @Test
    fun `Test withType Without ConfigureEach usage`() {

        val input = kotlin(
            """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.withType(Example::class.java) {}
                }
            """.trimIndent()
        )

        val message = "Avoid passing a closure to withType, use withType().configureEach instead"

        val expected = """
            src/test.kt:4: Error: $message [WithTypeWithoutConfigureEach]
                project.tasks.withType(Example::class.java) {}
                              ~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test withType With ConfigureEach usage`() {

        val input = kotlin(
            """
                import org.gradle.api.Project

                fun configure(project: Project) {
                    project.tasks.withType(Example::class.java).configureEach {}
                }
            """.trimIndent()
        )
        check(input).expectClean()
    }
}
