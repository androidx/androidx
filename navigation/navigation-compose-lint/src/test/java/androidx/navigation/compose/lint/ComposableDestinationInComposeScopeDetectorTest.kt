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
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [ComposableDestinationInComposeScopeDetector]. */
class ComposableDestinationInComposeScopeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ComposableDestinationInComposeScopeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            ComposableDestinationInComposeScopeDetector.ComposableDestinationInComposeScope,
            ComposableDestinationInComposeScopeDetector.ComposableNavGraphInComposeScope
        )

    @Test
    fun expectPass() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.compose.composable
                import androidx.navigation.compose.navigation
                import androidx.navigation.compose.NavHost
                import androidx.navigation.NavGraphBuilder

                @Composable
                fun Test() {
                    NavHost("host") {
                        composable("right") { }
                        navigation("fine") {
                            composable("okay") { }
                            navigation("sure") { }
                        }
                    }
                }
            """
                ),
                Stubs.Composable,
                NAV_BACK_STACK_ENTRY,
                NAV_CONTROLLER,
                NAV_GRAPH_BUILDER,
                NAV_GRAPH_COMPOSABLE,
                NAV_HOST
            )
            .run()
            .expectClean()
    }

    @Test
    fun nestedComposableBuilders() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.compose.composable
                import androidx.navigation.compose.NavHost
                import androidx.navigation.NavGraphBuilder

                @Composable
                fun Test() {
                    NavHost("host") {
                        composable("right") {
                            composable("wrong") { }
                        }
                    }
                }
            """
                ),
                Stubs.Composable,
                NAV_BACK_STACK_ENTRY,
                NAV_CONTROLLER,
                NAV_GRAPH_BUILDER,
                NAV_GRAPH_COMPOSABLE,
                NAV_HOST
            )
            .run()
            .expect(
                """
src/com/example/test.kt:13: Error: Using composable inside of a compose scope [ComposableDestinationInComposeScope]
                            composable("wrong") { }
                            ~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun navigationBuilderInsideComposable() {
        lint()
            .files(
                kotlin(
                    """
                package com.example

                import androidx.compose.runtime.*
                import androidx.navigation.compose.composable
                import androidx.navigation.compose.navigation
                import androidx.navigation.compose.NavHost
                import androidx.navigation.NavGraphBuilder

                @Composable
                fun Test() {
                    NavHost("host") {
                        composable("right") {
                            navigation("wrong") { }
                        }
                    }
                }
            """
                ),
                Stubs.Composable,
                NAV_BACK_STACK_ENTRY,
                NAV_CONTROLLER,
                NAV_GRAPH_BUILDER,
                NAV_GRAPH_COMPOSABLE,
                NAV_HOST
            )
            .run()
            .expect(
                """
src/com/example/test.kt:14: Error: Using navigation inside of a compose scope [ComposableNavGraphInComposeScope]
                            navigation("wrong") { }
                            ~~~~~~~~~~
1 errors, 0 warnings
            """
            )
    }
}
