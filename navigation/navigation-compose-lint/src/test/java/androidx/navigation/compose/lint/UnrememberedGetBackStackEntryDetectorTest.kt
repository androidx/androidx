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

package androidx.navigation.compose.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [UnrememberedGetBackStackEntryDetector].
 */
class UnrememberedGetBackStackEntryDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = UnrememberedGetBackStackEntryDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnrememberedGetBackStackEntryDetector.UnrememberedGetBackStackEntry)

    @Test
    fun notRemembered() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.NavController

                @Composable
                fun Test() {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                val lambda = @Composable {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                val lambda2: @Composable () -> Unit = {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    })
                    LambdaParameter {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }
                }

                @Composable
                fun Test4() {
                    val localObject = object {
                        val navController = NavController()
                        val entry = navController.getBackStackEntry("test")
                    }
                }
            """
            ),
            Stubs.Composable,
            NAV_CONTROLLER
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/com/example/{.kt:10: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    navController.getBackStackEntry("test")
                                  ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:15: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    navController.getBackStackEntry("test")
                                  ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:20: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    navController.getBackStackEntry("test")
                                  ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:30: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        navController.getBackStackEntry("test")
                                      ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:34: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        navController.getBackStackEntry("test")
                                      ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:41: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        navController.getBackStackEntry("test")
                                      ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:46: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        navController.getBackStackEntry("test")
                                      ~~~~~~~~~~~~~~~~~
src/com/example/{.kt:54: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        val entry = navController.getBackStackEntry("test")
                                                  ~~~~~~~~~~~~~~~~~
8 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBodyWithoutEntryKey() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.NavController

                @Composable
                fun Test() {
                    val navController = NavController()
                    val entry = remember { navController.getBackStackEntry("test") }
                }

                val lambda = @Composable {
                    val navController = NavController()
                    val entry = remember { navController.getBackStackEntry("test") }
                }

                val lambda2: @Composable () -> Unit = {
                    val navController = NavController()
                    val entry = remember { navController.getBackStackEntry("test") }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val navController = NavController()
                        val entry = remember { navController.getBackStackEntry("test") }
                    })
                    LambdaParameter {
                        val navController = NavController()
                        val entry = remember { navController.getBackStackEntry("test") }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val navController = NavController()
                        val entry = remember { navController.getBackStackEntry("test") }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val navController = NavController()
                        val entry = remember { navController.getBackStackEntry("test") }
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.Remember,
            NAV_CONTROLLER,
            NAV_BACK_STACK_ENTRY
        )
            .run()
            .expect(
                """
src/com/example/test.kt:10: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    val entry = remember { navController.getBackStackEntry("test") }
                                                         ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:15: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    val entry = remember { navController.getBackStackEntry("test") }
                                                         ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:20: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                    val entry = remember { navController.getBackStackEntry("test") }
                                                         ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:30: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        val entry = remember { navController.getBackStackEntry("test") }
                                                             ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:34: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        val entry = remember { navController.getBackStackEntry("test") }
                                                             ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:41: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        val entry = remember { navController.getBackStackEntry("test") }
                                                             ~~~~~~~~~~~~~~~~~
src/com/example/test.kt:46: Error: Calling getBackStackEntry during composition without using remember with a NavBackStackEntry key [UnrememberedGetBackStackEntry]
                        val entry = remember { navController.getBackStackEntry("test") }
                                                             ~~~~~~~~~~~~~~~~~
7 errors, 0 warnings
            """
            )
    }

    @Test
    fun rememberedInsideComposableBodyWithEntryKey() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.NavController
                import androidx.navigation.NavBackStackEntry

                @Composable
                fun Test() {
                    val navController = NavController()
                    val rememberedEntry = NavBackStackEntry()
                    val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                }

                val lambda = @Composable {
                    val navController = NavController()
                    val rememberedEntry = NavBackStackEntry()
                    val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                }

                val lambda2: @Composable () -> Unit = {
                    val navController = NavController()
                    val rememberedEntry = NavBackStackEntry()
                    val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                }

                @Composable
                fun LambdaParameter(content: @Composable () -> Unit) {}

                @Composable
                fun Test2() {
                    LambdaParameter(content = {
                        val navController = NavController()
                        val rememberedEntry = NavBackStackEntry()
                        val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                    })
                    LambdaParameter {
                        val navController = NavController()
                        val rememberedEntry = NavBackStackEntry()
                        val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                    }
                }

                fun test3() {
                    val localLambda1 = @Composable {
                        val navController = NavController()
                        val rememberedEntry = NavBackStackEntry()
                        val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                    }

                    val localLambda2: @Composable () -> Unit = {
                        val navController = NavController()
                        val rememberedEntry = NavBackStackEntry()
                        val entry = remember(rememberedEntry) { navController.getBackStackEntry("test") }
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.Remember,
            NAV_BACK_STACK_ENTRY,
            NAV_CONTROLLER
        )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.NavController

                fun test() {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                val lambda = {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                val lambda2: () -> Unit = {
                    val navController = NavController()
                    navController.getBackStackEntry("test")
                }

                fun LambdaParameter(content: () -> Unit) {}

                fun test2() {
                    LambdaParameter(content = {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    })
                    LambdaParameter {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }
                }

                fun test3() {
                    val localLambda1 = {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }

                    val localLambda2: () -> Unit = {
                        val navController = NavController()
                        navController.getBackStackEntry("test")
                    }
                }

                fun test3() {
                    class Foo {
                        val navController = NavController()
                        val entry = navController.getBackStackEntry("test")
                    }

                    val localObject = object {
                        val navController = NavController()
                        val entry = navController.getBackStackEntry("test")
                    }
                }

                @Composable
                fun Test4() {
                    class Foo {
                        val navController = NavController()
                        val entry = navController.getBackStackEntry("test")
                    }
                }
            """
            ),
            Stubs.Composable,
            Stubs.Remember,
            NAV_CONTROLLER,
            NAV_BACK_STACK_ENTRY
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
