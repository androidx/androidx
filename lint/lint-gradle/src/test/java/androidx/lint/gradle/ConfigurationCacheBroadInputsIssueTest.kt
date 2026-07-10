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

package androidx.lint.gradle

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConfigurationCacheBroadInputsIssueTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.CONFIGURATION_CACHE_BROAD_INPUTS_ISSUE),
    ) {
    @Test
    fun `Test usage of System#getenv`() {
        val input =
            kotlin(
                """
                import java.lang.System

                fun configure() {
                    val values = System.getenv()
                    val singleValue = System.getenv("single")
                }
                """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use Project.providers.environmentVariable instead of getenv [GradleConfigurationCacheBroadInputs]
                val values = System.getenv()
                                    ~~~~~~
            src/test.kt:5: Error: Use Project.providers.environmentVariable instead of getenv [GradleConfigurationCacheBroadInputs]
                val singleValue = System.getenv("single")
                                         ~~~~~~
            2 errors
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs("")
    }
}
