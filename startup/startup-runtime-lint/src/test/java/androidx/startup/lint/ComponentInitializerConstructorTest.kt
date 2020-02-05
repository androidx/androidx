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

package androidx.startup.lint

import androidx.startup.lint.Stubs.COMPONENT_INITIALIZER
import androidx.startup.lint.Stubs.TEST_COMPONENT
import androidx.startup.lint.Stubs.TEST_COMPONENT_JAVA
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class ComponentInitializerConstructorTest {

    @Test
    fun testSuccessWhenNoArgumentConstructorIsPresent() {
        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT
            )
            .issues(ComponentInitializerConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testSuccessWhenNoArgumentConstructorIsPresentJava() {
        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT_JAVA
            )
            .issues(ComponentInitializerConstructorDetector.ISSUE)
            .run()
            .expectClean()
    }

    @Test
    fun testFailureWhenZeroNoArgumentConstructorsArePresent() {
        val component: TestFile = kotlin(
            "com/example/TestComponentInitializer.kt",
            """
            package com.example

            import androidx.startup.ComponentInitializer

            class TestComponentInitializer(val int: Int): ComponentInitializer<Unit> {

            }
        """
        ).indented().within("src")

        lint()
            .files(
                COMPONENT_INITIALIZER,
                TEST_COMPONENT,
                component
            )
            .issues(ComponentInitializerConstructorDetector.ISSUE)
            .run()
            /* ktlint-disable max-line-length */
            .expect(
                """
                src/com/example/TestComponentInitializer.kt:5: Error: Missing ComponentInitializer no-arg constructor [EnsureComponentInitializerNoArgConstructor]
                class TestComponentInitializer(val int: Int): ComponentInitializer<Unit> {
                ^
                1 errors, 0 warnings
            """.trimIndent()
            )
        /* ktlint-enable max-line-length */
    }
}
