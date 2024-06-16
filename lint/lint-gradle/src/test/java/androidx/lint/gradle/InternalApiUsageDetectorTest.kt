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

import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InternalApiUsageDetectorTest :
    GradleLintDetectorTest(
        detector = InternalApiUsageDetector(),
        issues =
            listOf(
                InternalApiUsageDetector.INTERNAL_GRADLE_ISSUE,
                InternalApiUsageDetector.INTERNAL_AGP_ISSUE,
            )
    ) {
    @Test
    fun `Test usage of internal Gradle API`() {
        val input =
            kotlin(
                """
                import org.gradle.api.component.SoftwareComponent
                import org.gradle.api.internal.component.SoftwareComponentInternal

                fun getSoftwareComponent() : SoftwareComponent {
                    return object : SoftwareComponentInternal {
                        override fun getUsages(): Set<out UsageContext> {
                            TODO()
                        }
                    }
                }
            """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Adding import aliases adds new warnings and that is working as intended.
            .skipTestModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    1 errors, 0 warnings
                """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Adding import aliases adds new warnings and that is working as intended.
            .testModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                    src/test.kt:2: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    src/test.kt:4: Error: Avoid using internal Gradle APIs [InternalGradleApiUsage]
                    import org.gradle.api.internal.component.SoftwareComponentInternal as IMPORT_ALIAS_2_SOFTWARECOMPONENTINTERNAL
                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    2 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of internal Android Gradle API`() {
        val input =
            kotlin(
                """
                import com.android.build.gradle.internal.lint.VariantInputs
            """
                    .trimIndent()
            )

        lint()
            .files(*STUBS, input)
            // Import aliases mode is covered by other tests
            .skipTestModes(TestMode.IMPORT_ALIAS)
            .run()
            .expect(
                """
                src/test.kt:1: Error: Avoid using internal Android Gradle Plugin APIs [InternalAgpApiUsage]
                import com.android.build.gradle.internal.lint.VariantInputs
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun `Test usage of Internal annotation`() {
        val input =
            kotlin(
                """
                import java.io.File
                import org.gradle.api.Task
                import org.gradle.api.tasks.Internal

                class MyTask : Task {
                    @get:Internal
                    val notInput: File
                }
            """
                    .trimIndent()
            )
        check(input).expectClean()
    }
}
