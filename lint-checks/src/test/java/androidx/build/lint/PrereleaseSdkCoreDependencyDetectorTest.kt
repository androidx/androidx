/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.lint

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrereleaseSdkCoreDependencyDetectorTest :
    AbstractLintDetectorTest(
        useDetector = PrereleaseSdkCoreDependencyDetector(),
        useIssues = listOf(PrereleaseSdkCoreDependencyDetector.ISSUE),
        stubs =
            arrayOf(
                Stubs.BuildCompat,
                Stubs.ChecksSdkIntAtLeast,
                Stubs.JetpackRequiresOptIn,
                Stubs.RestrictTo
            )
    ) {
    @Test
    fun `Versioned dependency with isAtLeastU is flagged`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package androidx.test

                    import androidx.core.os.BuildCompat

                    fun callIsAtLeastU() {
                        return BuildCompat.isAtLeastU()
                    }
                """
                        .trimIndent()
                ),
                gradle(
                    """
                dependencies {
                    implementation("androidx.core:core:1.9.0")
                }
            """
                        .trimIndent()
                ),
            )

        val expected =
            """
            src/main/kotlin/androidx/test/test.kt:6: Error: Prelease SDK check isAtLeastU cannot be called as this project has a versioned dependency on androidx.core:core [PrereleaseSdkCoreDependency]
                return BuildCompat.isAtLeastU()
                       ~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Tip-of-tree dependency with isAtLeastU is not flagged`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package androidx.test

                    import androidx.core.os.BuildCompat

                    fun callIsAtLeastU() {
                        return BuildCompat.isAtLeastU()
                    }
                """
                        .trimIndent()
                ),
                gradle(
                    """
                dependencies {
                    implementation(project(":core:core"))
                }
            """
                        .trimIndent()
                ),
            )

        check(*input).expectClean()
    }

    @Test
    fun `Versioned dependency with isAtLeastSv2 is flagged`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package androidx.test

                    import androidx.core.os.BuildCompat

                    fun callIsAtLeastSv2() {
                        return BuildCompat.isAtLeastSv2()
                    }
                """
                        .trimIndent()
                ),
                gradle(
                    """
                dependencies {
                    implementation("androidx.core:core:1.9.0")
                }
            """
                        .trimIndent()
                ),
            )

        val expected =
            """
            src/main/kotlin/androidx/test/test.kt:6: Error: Prelease SDK check isAtLeastSv2 cannot be called as this project has a versioned dependency on androidx.core:core [PrereleaseSdkCoreDependency]
                return BuildCompat.isAtLeastSv2()
                       ~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """
                .trimIndent()

        check(*input).expect(expected)
    }

    @Test
    fun `Versioned dependency with non-annotated isAtLeastN is not flagged`() {
        val input =
            arrayOf(
                kotlin(
                    """
                    package androidx.test

                    import androidx.core.os.BuildCompat

                    fun callIsAtLeastN() {
                        return BuildCompat.isAtLeastN()
                    }
                """
                        .trimIndent()
                ),
                gradle(
                    """
                dependencies {
                    implementation("androidx.core:core:1.9.0")
                }
            """
                        .trimIndent()
                ),
            )

        check(*input).expectClean()
    }
}
