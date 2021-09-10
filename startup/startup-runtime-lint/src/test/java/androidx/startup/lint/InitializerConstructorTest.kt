/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.startup.lint

import androidx.startup.lint.Stubs.INITIALIZER
import androidx.startup.lint.Stubs.TEST_INITIALIZER
import androidx.startup.lint.Stubs.TEST_INITIALIZER_JAVA
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Ignore
import org.junit.Test

class InitializerConstructorTest {

    @Test
    fun testSuccessWhenNoArgumentConstructorIsPresent() {
        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER
            )
            .issues(InitializerConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testSuccessWhenNoArgumentConstructorIsPresentJava() {
        lint()
            .files(
                INITIALIZER,
                TEST_INITIALIZER_JAVA
            )
            .issues(InitializerConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Ignore("b/187539166")
    @Test
    fun testFailureWhenZeroNoArgumentConstructorsArePresent() {
        val component: TestFile = kotlin(
            "com/example/TestInitializer.kt",
            """
            package com.example

            import androidx.startup.Initializer

            class TestInitializer(val int: Int): Initializer<Unit> {

            }
        """
        ).indented().within("src")

        lint()
            .files(
                INITIALIZER,
                component
            )
            .issues(InitializerConstructorDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/TestInitializer.kt:5: Error: Missing Initializer no-arg constructor [EnsureInitializerNoArgConstr]
                class TestInitializer(val int: Int): Initializer<Unit> {
                ^
                1 errors, 0 warnings
                """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
