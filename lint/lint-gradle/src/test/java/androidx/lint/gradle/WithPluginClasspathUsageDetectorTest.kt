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
class WithPluginClasspathUsageDetectorTest :
    GradleLintDetectorTest(
        detector = WithPluginClasspathUsageDetector(),
        issues = listOf(WithPluginClasspathUsageDetector.ISSUE)
    ) {
    @Test
    fun `Test withPluginClassPath usage`() {

        val input =
            kotlin(
                """
                import org.gradle.testkit.runner.GradleRunner
                import java.io.File

                class TestClass {
                    fun testMethod() {
                        GradleRunner.create()
                            .withProjectDir(File("path/to/project"))
                            .withPluginClasspath()
                            .build()
                    }
                }
                """
                    .trimIndent()
            )

        val message =
            "Avoid usage of GradleRunner#withPluginClasspath, which is broken. " +
                "Instead use something like https://github.com/autonomousapps/" +
                "dependency-analysis-gradle-plugin/tree/main/testkit#gradle-testkit-support-plugin"

        val expected =
            """
            src/TestClass.kt:8: Error: $message [WithPluginClasspathUsage]
                        .withPluginClasspath()
                         ~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(input).expect(expected)
    }
}
