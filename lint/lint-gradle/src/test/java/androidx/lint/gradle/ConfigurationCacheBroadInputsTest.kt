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
class ConfigurationCacheBroadInputsTest :
    GradleLintDetectorTest(
        detector = DiscouragedGradleMethodDetector(),
        issues = listOf(DiscouragedGradleMethodDetector.CONFIGURATION_CACHE_BROAD_INPUTS),
    ) {

    @Test
    fun `Test usage of System getenv without key`() {
        val input =
            kotlin(
                """
                import java.lang.System

                fun readSetup() {
                    val ciEnv = System.getenv()
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Avoid using method getenv [GradleConfigurationCacheInputs]
                val ciEnv = System.getenv()
                                   ~~~~~~
            1 error
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of System getenv with key is not an error`() {
        val input =
            kotlin(
                """
                import java.lang.System

                fun readSetup() {
                    val ciEnv = System.getenv("CI")
                }
            """
                    .trimIndent()
            )
        check(input).expect("No warnings.")
    }

    @Test
    fun `Test usage of System getProperties`() {
        val input =
            kotlin(
                """
                import java.lang.System

                fun readSetup() {
                    val ciEnv = System.getProperties()
                }
            """
                    .trimIndent()
            )

        val expected =
            """
            src/test.kt:4: Error: Use getProperty instead of getProperties [GradleConfigurationCacheInputs]
                val ciEnv = System.getProperties()
                                   ~~~~~~~~~~~~~
            1 error
        """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Test usage of System getProperty is not an error`() {
        val input =
            kotlin(
                """
                import java.lang.System

                fun readSetup() {
                    val ciEnv = System.getProperty("CI")
                }
            """
                    .trimIndent()
            )
        check(input).expect("No warnings.")
    }
}
