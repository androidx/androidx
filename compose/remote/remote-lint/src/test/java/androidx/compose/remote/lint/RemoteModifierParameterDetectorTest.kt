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

/** Test for [RemoteModifierParameterDetector]. */
@RunWith(JUnit4::class)
class RemoteModifierParameterDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RemoteModifierParameterDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RemoteModifierParameterDetector.RemoteModifierParameter)

    @Test
    fun remoteModifierParameterNaming() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    buttonModifier: RemoteModifier = RemoteModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expect(
                """
src/androidx/compose/remote/creation/compose/modifier/foo/test.kt:12: Warning: RemoteModifier parameter should be named modifier [RemoteModifierParameter]
                    buttonModifier: RemoteModifier = RemoteModifier,
                    ~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/test.kt line 12: Change name to modifier:
@@ -12 +12 @@
-                    buttonModifier: RemoteModifier = RemoteModifier,
+                    modifier: RemoteModifier = RemoteModifier,
            """
            )
    }

    @Test
    fun remoteModifierParameterType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    modifier: RemoteModifier.Element,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expect(
                """
src/androidx/compose/remote/creation/compose/modifier/foo/test.kt:12: Warning: RemoteModifier parameter should have a type of RemoteModifier [RemoteModifierParameter]
                    modifier: RemoteModifier.Element,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/test.kt line 12: Change type to RemoteModifier:
@@ -12 +12 @@
-                    modifier: RemoteModifier.Element,
+                    modifier: RemoteModifier,
            """
            )
    }

    @Test
    fun remoteModifierParameterDefaultValue() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                object TestModifier : RemoteModifier.Element

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    modifier: RemoteModifier = TestModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expect(
                """
src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt:14: Warning: Optional RemoteModifier parameter should have a default value of RemoteModifier [RemoteModifierParameter]
                    modifier: RemoteModifier = TestModifier,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt line 14: Change default value to RemoteModifier:
@@ -14 +14 @@
-                    modifier: RemoteModifier = TestModifier,
+                    modifier: RemoteModifier = RemoteModifier,
            """
            )
    }

    @Test
    fun remoteModifierParameterOrdering() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    modifier: RemoteModifier = RemoteModifier,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expect(
                """
src/androidx/compose/remote/creation/compose/modifier/foo/test.kt:13: Warning: RemoteModifier parameter should be the first optional parameter [RemoteModifierParameter]
                    modifier: RemoteModifier = RemoteModifier,
                    ~~~~~~~~
0 errors, 1 warnings
            """
            )
    }

    @Test
    fun multipleErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                object TestModifier : RemoteModifier.Element

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    buttonModifier: RemoteModifier.Element = TestModifier,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expect(
                """
src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt:15: Warning: Optional RemoteModifier parameter should have a default value of RemoteModifier [RemoteModifierParameter]
                    buttonModifier: RemoteModifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt:15: Warning: RemoteModifier parameter should be named modifier [RemoteModifierParameter]
                    buttonModifier: RemoteModifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt:15: Warning: RemoteModifier parameter should be the first optional parameter [RemoteModifierParameter]
                    buttonModifier: RemoteModifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt:15: Warning: RemoteModifier parameter should have a type of RemoteModifier [RemoteModifierParameter]
                    buttonModifier: RemoteModifier.Element = TestModifier,
                    ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
            .expectFixDiffs(
                """
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt line 15: Change default value to RemoteModifier:
@@ -15 +15 @@
-                    buttonModifier: RemoteModifier.Element = TestModifier,
+                    buttonModifier: RemoteModifier.Element = RemoteModifier,
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt line 15: Change name to modifier:
@@ -15 +15 @@
-                    buttonModifier: RemoteModifier.Element = TestModifier,
+                    modifier: RemoteModifier.Element = TestModifier,
Autofix for src/androidx/compose/remote/creation/compose/modifier/foo/TestModifier.kt line 15: Change type to RemoteModifier:
@@ -15 +15 @@
-                    buttonModifier: RemoteModifier.Element = TestModifier,
+                    buttonModifier: RemoteModifier = TestModifier,
            """
            )
    }

    @Test
    fun ignoreNonComposableFunctions() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier

                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    buttonModifier: RemoteModifier = RemoteModifier,
                ) {}
            """
                ),
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignoreOrderingIfNoDefaultValue() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    elevation: Float = 5,
                    modifier: RemoteModifier,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expectClean()
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable
                import androidx.compose.remote.creation.compose.layout.RemoteComposable

                @Composable
                @RemoteComposable
                fun Button(
                    onClick: () -> Unit,
                    modifier: RemoteModifier = RemoteModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}

                @Composable
                @RemoteComposable
                fun Button2(
                    onClick: () -> Unit,
                    modifier: RemoteModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteComposable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expectClean()
    }

    @Test
    fun ignoreNormalComposablesWithoutRemoteComposable() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.remote.creation.compose.modifier.foo

                import androidx.compose.remote.creation.compose.modifier.RemoteModifier
                import androidx.compose.runtime.Composable

                @Composable
                fun Button(
                    onClick: () -> Unit,
                    someModifier: RemoteModifier = RemoteModifier,
                    elevation: Float = 5,
                    content: @Composable () -> Unit
                ) {}
            """
                ),
                Stubs.Composable,
                RemoteStubs.RemoteModifier,
            )
            .run()
            .expectClean()
    }
}
