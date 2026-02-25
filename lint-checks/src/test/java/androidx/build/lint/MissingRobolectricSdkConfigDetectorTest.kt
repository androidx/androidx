/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.tools.lint.checks.infrastructure.TestFiles
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MissingRobolectricSdkConfigDetectorTest :
    AbstractLintDetectorTest(
        useDetector = MissingRobolectricSdkConfigDetector(),
        useIssues = listOf(MissingRobolectricSdkConfigDetector.ISSUE),
        stubs = STUBS,
    ) {

    @Test
    fun `Robolectric test without Config is flagged and has fixes`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.junit.Test

            @RunWith(RobolectricTestRunner::class)
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        val expected =
            """
            src/test/com/example/MyRobolectricTest.kt:8: Error: Robolectric tests require an @Config annotation to explicitly specify an SDK level. [RobolectricSdkConfigRequired]
                        @RunWith(RobolectricTestRunner::class)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        // Although no line separator is added between the two annotations in this test,
        // in practice,they do get separated to different lines, probably due to reformatting.
        val expectedFix =
            """
            Fix for src/test/com/example/MyRobolectricTest.kt line 8: Add @Config(sdk = [Config.TARGET_SDK]):
            @@ -6,0 +7 @@
            +import org.robolectric.annotation.Config
            @@ -8,0 +10 @@
            +@Config(sdk = [Config.TARGET_SDK])
            Fix for src/test/com/example/MyRobolectricTest.kt line 8: Add @Config(sdk = [Config.ALL_SDKS]):
            @@ -6,0 +7 @@
            +import org.robolectric.annotation.Config
            @@ -8,0 +10 @@
            +@Config(sdk = [Config.ALL_SDKS])
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Robolectric test with minSdk is not flagged`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.robolectric.annotation.Config
            import org.junit.Test

            @RunWith(RobolectricTestRunner::class)
            @Config(minSdk = 23)
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        check(input).expectClean()
    }

    @Test
    fun `Robolectric test with sdk is not flagged`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.robolectric.annotation.Config
            import org.junit.Test

            @RunWith(RobolectricTestRunner::class)
            @Config(sdk = [23])
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        check(input).expectClean()
    }

    @Test
    fun `Robolectric test with maxSdk is not flagged`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.robolectric.annotation.Config
            import org.junit.Test

            @RunWith(RobolectricTestRunner::class)
            @Config(maxSdk = 23)
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        check(input).expectClean()
    }

    @Test
    fun `Robolectric test with empty Config is flagged and has fixes`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.robolectric.annotation.Config
            import org.junit.Test

            @Config
            @RunWith(RobolectricTestRunner::class)
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        val expected =
            """
            src/test/com/example/MyRobolectricTest.kt:9: Error: Robolectric @Config must specify an SDK level (e.g., sdk, minSdk, or maxSdk). [RobolectricSdkConfigRequired]
                        @Config
                        ~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFix =
            """
            Fix for src/test/com/example/MyRobolectricTest.kt line 9: Add @Config(sdk = [Config.TARGET_SDK]):
            @@ -9 +9 @@
            -            @Config
            +            @Config(sdk = [Config.TARGET_SDK])
            Fix for src/test/com/example/MyRobolectricTest.kt line 9: Add @Config(sdk = [Config.ALL_SDKS]):
            @@ -9 +9 @@
            -            @Config
            +            @Config(sdk = [Config.ALL_SDKS])
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Fixes retain the original attributes in Robolectric Config annotation`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.robolectric.RobolectricTestRunner
            import org.robolectric.annotation.Config
            import org.junit.Test

            @RunWith(RobolectricTestRunner::class)
            @Config(fontScale = 0.85f)
            class MyRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        val expected =
            """
            src/test/com/example/MyRobolectricTest.kt:10: Error: Robolectric @Config must specify an SDK level (e.g., sdk, minSdk, or maxSdk). [RobolectricSdkConfigRequired]
                        @Config(fontScale = 0.85f)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFix =
            """
            Fix for src/test/com/example/MyRobolectricTest.kt line 10: Add @Config(sdk = [Config.TARGET_SDK]):
            @@ -10 +10 @@
            -            @Config(fontScale = 0.85f)
            +            @Config(fontScale = 0.85f, sdk = [Config.TARGET_SDK])
            Fix for src/test/com/example/MyRobolectricTest.kt line 10: Add @Config(sdk = [Config.ALL_SDKS]):
            @@ -10 +10 @@
            -            @Config(fontScale = 0.85f)
            +            @Config(fontScale = 0.85f, sdk = [Config.ALL_SDKS])
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Non-Robolectric test is not flagged`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import org.junit.runners.JUnit4
            import org.junit.Test

            @RunWith(JUnit4::class)
            class MyJUnit4Test {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        check(input).expectClean()
    }

    @Test
    fun `Custom Robolectric test runner subclass flags issue`() {
        val input =
            kotlin(
                    """
            package com.example

            import org.junit.runner.RunWith
            import com.example.CustomRobolectricTestRunner
            import org.junit.Test

            @RunWith(CustomRobolectricTestRunner::class)
            class MyCustomRobolectricTest {
                @Test
                fun testSomething() { }
            }
            """
                )
                .within("src/test")

        val expected =
            """
            src/test/com/example/MyCustomRobolectricTest.kt:8: Error: Robolectric tests require an @Config annotation to explicitly specify an SDK level. [RobolectricSdkConfigRequired]
                        @RunWith(CustomRobolectricTestRunner::class)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        check(input).expect(expected)
    }

    @Test
    fun `Java Robolectric test without config is flagged and uses Java-specific fixes`() {
        val input =
            java(
                    """
            package com.example;

            import org.junit.runner.RunWith;
            import org.robolectric.RobolectricTestRunner;
            import org.junit.Test;

            @RunWith(RobolectricTestRunner.class)
            public class MyRobolectricTest {
                @Test
                public void testSomething() { }
            }
            """
                )
                .within("src/test")

        val expected =
            """
            src/test/com/example/MyRobolectricTest.java:8: Error: Robolectric tests require an @Config annotation to explicitly specify an SDK level. [RobolectricSdkConfigRequired]
                        @RunWith(RobolectricTestRunner.class)
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            1 errors, 0 warnings
            """
                .trimIndent()

        val expectedFix =
            """
            Fix for src/test/com/example/MyRobolectricTest.java line 8: Add @Config(sdk = {Config.TARGET_SDK}):
            @@ -6,0 +7 @@
            +import org.robolectric.annotation.Config;
            @@ -8,0 +10 @@
            +@Config(sdk = {Config.TARGET_SDK})
            Fix for src/test/com/example/MyRobolectricTest.java line 8: Add @Config(sdk = {Config.ALL_SDKS}):
            @@ -6,0 +7 @@
            +import org.robolectric.annotation.Config;
            @@ -8,0 +10 @@
            +@Config(sdk = {Config.ALL_SDKS})
            """
                .trimIndent()

        check(input).expect(expected).expectFixDiffs(expectedFix)
    }

    companion object {
        private val ROBO_CONFIG_STUB =
            TestFiles.kotlin(
                    """
                package org.robolectric.annotation
                annotation class Config(
                    val sdk: IntArray = [],
                    val minSdk: Int = -1,
                    val maxSdk: Int = -1,
                    val fontScale: Float = 1.0f,
                )
                """
                )
                .indented()

        private val ROBO_TEST_RUNNER_STUB =
            TestFiles.kotlin(
                    """
                package org.robolectric
                open class RobolectricTestRunner
                """
                )
                .indented()

        private val CUSTOM_ROBO_TEST_RUNNER_STUB =
            TestFiles.kotlin(
                    """
                package com.example
                import org.robolectric.RobolectricTestRunner
                class CustomRobolectricTestRunner : RobolectricTestRunner()
                """
                )
                .indented()

        private val STUBS =
            arrayOf(
                ROBO_CONFIG_STUB,
                ROBO_TEST_RUNNER_STUB,
                CUSTOM_ROBO_TEST_RUNNER_STUB,
                Stubs.RunWith,
                Stubs.JUnit4Runner,
                Stubs.TestAnnotation,
            )
    }
}
