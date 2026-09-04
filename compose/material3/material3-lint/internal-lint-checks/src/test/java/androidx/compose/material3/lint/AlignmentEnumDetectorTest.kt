/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AlignmentEnumDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = AlignmentEnumDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(AlignmentEnumDetector.ISSUE)

    @Test
    fun invalid_customAlignment() {
        lint()
            .files(
                kotlin(
                        "src/androidx/compose/material3/CustomAlignment.kt",
                        """
                    package androidx.compose.material3

                    enum class CustomAlignment {
                        Top, Center, Bottom
                    }
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CustomAlignment.kt:3: Error: Avoid custom alignment enums; use standard Layout/Alignment APIs instead [AvoidCustomAlignmentEnum]
                enum class CustomAlignment {
                           ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun valid_otherEnum() {
        lint()
            .files(
                kotlin(
                        "src/androidx/compose/material3/Direction.kt",
                        """
                    package androidx.compose.material3

                    enum class Direction {
                        Left, Right
                    }
                    """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }

    @Test
    fun invalid_customAlignmentClass() {
        lint()
            .files(
                kotlin(
                        "src/androidx/compose/material3/CustomAlignment.kt",
                        """
                    package androidx.compose.material3

                    class CustomAlignment {
                        val value = "center"
                    }
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CustomAlignment.kt:3: Error: Avoid custom alignment enums; use standard Layout/Alignment APIs instead [AvoidCustomAlignmentEnum]
                class CustomAlignment {
                      ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun invalid_customAlignmentValueClass() {
        lint()
            .files(
                kotlin(
                        "src/androidx/compose/material3/CustomAlignment.kt",
                        """
                    package androidx.compose.material3

                    @JvmInline
                    value class CustomAlignment private constructor(val value: Int) {
                        companion object {
                            val Center = CustomAlignment(0)
                        }
                    }
                    """,
                    )
                    .indented()
            )
            .run()
            .expect(
                """
                src/androidx/compose/material3/CustomAlignment.kt:4: Error: Avoid custom alignment enums; use standard Layout/Alignment APIs instead [AvoidCustomAlignmentEnum]
                value class CustomAlignment private constructor(val value: Int) {
                            ~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun valid_nonPublicAlignmentClass() {
        lint()
            .files(
                kotlin(
                        "src/androidx/compose/material3/CustomAlignment.kt",
                        """
                    package androidx.compose.material3

                    internal class InternalAlignment {
                        val value = 1
                    }

                    private class PrivateAlignment {
                        val value = 2
                    }
                    """,
                    )
                    .indented()
            )
            .run()
            .expectClean()
    }
}
