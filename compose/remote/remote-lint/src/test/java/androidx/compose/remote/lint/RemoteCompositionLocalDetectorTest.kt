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

@file:Suppress("UnstableApiUsage")

package androidx.compose.remote.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test for [RemoteCompositionLocalDetector]. */
@RunWith(JUnit4::class)
class RemoteCompositionLocalDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RemoteCompositionLocalDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RemoteCompositionLocalDetector.RemoteCompositionLocalUsage)

    @Test
    fun LocalDensityUsageInRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalDensity

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val density = LocalDensity.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.CompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using LocalDensity in a @RemoteComposable will bake static values into the document. Use LocalRemoteDensity.current instead to support dynamic host density. [RemoteCompositionLocalUsage]
                    val density = LocalDensity.current
                                  ~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun LocalContextUsageInRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalContext

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val context = LocalContext.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.AndroidCompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using LocalContext in a @RemoteComposable will bake static values into the document at capture time. The captured context will not represent the host player's context. For loading resources, use host-resolved alternatives like named RemoteString expressions fed with values from app resources by the host device to the player. [RemoteCompositionLocalUsage]
                    val context = LocalContext.current
                                  ~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun LocalConfigurationUsageInRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalConfiguration

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val config = LocalConfiguration.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.AndroidCompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using LocalConfiguration in a @RemoteComposable will bake a static value into the document at capture time. Configuration changes at runtime on the player will not be reflected. Instead of querying configuration dynamically, use responsive layout modifiers (e.g. RemoteModifier.fillMaxSize()) or constraints. [RemoteCompositionLocalUsage]
                    val config = LocalConfiguration.current
                                 ~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun LocalLayoutDirectionUsageInRemoteComposableAllowed() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalLayoutDirection

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val layoutDirection = LocalLayoutDirection.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.CompositionLocals,
            )
            .run()
            .expectClean()
    }

    @Test
    fun LocalDensityUsageWithAliasInRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalDensity as ComposeDensity

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val density = ComposeDensity.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.CompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using LocalDensity in a @RemoteComposable will bake static values into the document. Use LocalRemoteDensity.current instead to support dynamic host density. [RemoteCompositionLocalUsage]
                    val density = ComposeDensity.current
                                  ~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun normalComposableAllowed() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.platform.LocalDensity
                import androidx.compose.ui.platform.LocalContext
                import androidx.compose.ui.platform.LocalConfiguration
                import androidx.compose.ui.platform.LocalLayoutDirection

                @Composable
                fun TestWidget() {
                    val density = LocalDensity.current
                    val context = LocalContext.current
                    val config = LocalConfiguration.current
                    val layoutDirection = LocalLayoutDirection.current
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.CompositionLocals,
                RemoteStubs.AndroidCompositionLocals,
            )
            .run()
            .expectClean()
    }

    @Test
    fun customLocalDensityAllowed() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import custom.LocalDensity

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val density = LocalDensity.current
                }
            """
                ),
                kotlin(
                    """
                package custom

                import androidx.compose.runtime.staticCompositionLocalOf

                val LocalDensity = staticCompositionLocalOf { 1f }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
            )
            .run()
            .expectClean()
    }

    @Test
    fun customLocalDensityInNormalComposableAllowed() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import custom.LocalDensity

                @Composable
                fun TestWidget() {
                    val density = LocalDensity.current
                }
            """
                ),
                kotlin(
                    """
                package custom

                import androidx.compose.runtime.staticCompositionLocalOf

                val LocalDensity = staticCompositionLocalOf { 1f }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
            )
            .run()
            .expectClean()
    }

    @Test
    fun LocalDensityUsageInNestedLambdaInRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalDensity

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    Box {
                        val density = LocalDensity.current
                    }
                }

                @Composable
                fun Box(content: @Composable () -> Unit) {}
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.CompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:12: Warning: Using LocalDensity in a @RemoteComposable will bake static values into the document. Use LocalRemoteDensity.current instead to support dynamic host density. [RemoteCompositionLocalUsage]
                        val density = LocalDensity.current
                                      ~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun LocalDensityReferenceWithoutCurrentFlagged() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable
                import androidx.compose.ui.platform.LocalDensity

                @Composable
                @RemoteComposable
                fun TestWidget() {
                    val density = LocalDensity
                }
            """
                ),
                Stubs.Composable,
                Stubs.CompositionLocal,
                RemoteStubs.RemoteComposable,
                RemoteStubs.CompositionLocals,
            )
            .run()
            .expect(
                """
src/test/test.kt:11: Warning: Using LocalDensity in a @RemoteComposable will bake static values into the document. Use LocalRemoteDensity.current instead to support dynamic host density. [RemoteCompositionLocalUsage]
                    val density = LocalDensity
                                  ~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
    }
}
