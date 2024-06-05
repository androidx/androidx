/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test for [ComposedModifierDetector]. */
@RunWith(JUnit4::class)
class ComposedModifierDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposedModifierDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ComposedModifierDetector.UnnecessaryComposedModifier)

    @Test
    fun noComposableCalls() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.Composable

                fun Modifier.test(): Modifier = composed {
                    this@test
                }

                fun Modifier.test2(): Modifier {
                    return composed {
                        this@test
                    }
                }

                fun Modifier.test3(): Modifier = composed(factory = {
                    this@test3
                })

                fun Modifier.test4(): Modifier = composed({}, { this@test4})
            """
                ),
                UiStubs.composed,
                Stubs.Composable,
                Stubs.Modifier
            )
            .skipTestModes(TestMode.WHITESPACE) // b/202187519, remove when upgrading to 7.1.0
            .run()
            .expect(
                """
src/test/test.kt:8: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test(): Modifier = composed {
                                                ~~~~~~~~
src/test/test.kt:13: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                    return composed {
                           ~~~~~~~~
src/test/test.kt:18: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test3(): Modifier = composed(factory = {
                                                 ~~~~~~~~
src/test/test.kt:22: Warning: Unnecessary use of Modifier.composed [UnnecessaryComposedModifier]
                fun Modifier.test4(): Modifier = composed({}, { this@test4})
                                                 ~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun composableCalls() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.composed
                import androidx.compose.runtime.*

                inline fun <T> scopingFunction(lambda: () -> T): T {
                    return lambda()
                }

                fun Modifier.test1(): Modifier = composed {
                    val foo = remember { true }
                    this@test1
                }

                @Composable
                fun composableFunction() {}

                fun Modifier.test2(): Modifier = composed {
                    composableFunction()
                    this@test2
                }

                fun Modifier.test3(): Modifier = composed {
                    scopingFunction {
                        val foo = remember { true }
                        this@test3
                    }
                }

                @Composable
                fun <T> composableScopingFunction(lambda: @Composable () -> T): T {
                    return lambda()
                }

                fun Modifier.test4(): Modifier = composed {
                    composableScopingFunction {
                        this@test4
                    }
                }

                val composableProperty: Boolean
                    @Composable
                    get() = true

                fun Modifier.test5(): Modifier = composed {
                    composableProperty
                    this@test5
                }

                // Test for https://youtrack.jetbrains.com/issue/KT-46795
                fun Modifier.test6(): Modifier = composed {
                    val nullable: Boolean? = null
                    val foo = nullable ?: false
                    val bar = remember { true }
                    this@test6
                }
            """
                ),
                UiStubs.composed,
                Stubs.Composable,
                Stubs.Modifier,
                Stubs.Remember
            )
            .skipTestModes(TestMode.WHITESPACE) // b/202187519, remove when upgrading to 7.1.0
            .run()
            .expectClean()
    }
}
