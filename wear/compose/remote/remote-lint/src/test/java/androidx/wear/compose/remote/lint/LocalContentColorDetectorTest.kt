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

package androidx.wear.compose.remote.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocalContentColorDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LocalContentColorDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(LocalContentColorDetector.LocalContentColorUsage)

    @Test
    fun wearMaterialLocalContentColorInsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.layout.RemoteComposable
                    import androidx.wear.compose.material.LocalContentColor

                    @Composable
                    @RemoteComposable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalContentColor provides Color.Red
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                RemoteStubs.RemoteComposableStub,
                ComposeStubs.ProvidableCompositionLocalStub,
                WearComposeStubs.WearMaterialLocalContentColorStub,
            )
            .run()
            .expect(
                """
                src/test/test.kt:13: Warning: Using androidx.wear.compose.material.LocalContentColor inside a @RemoteComposable function has no effect. [LocalContentColorUsage]
                        LocalContentColor provides Color.Red
                        ~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 13: Replace with LocalRemoteContentColor:
                @@ -7,0 +8 @@
                +import androidx.wear.compose.remote.material3.LocalRemoteContentColor
                @@ -13 +14 @@
                -        LocalContentColor provides Color.Red
                +        LocalRemoteContentColor provides Color.Red
                """
                    .trimIndent()
            )
    }

    @Test
    fun wearMaterial3LocalContentColorInsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.layout.RemoteComposable
                    import androidx.wear.compose.material3.LocalContentColor

                    @Composable
                    @RemoteComposable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalContentColor provides Color.Red
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                RemoteStubs.RemoteComposableStub,
                ComposeStubs.ProvidableCompositionLocalStub,
                WearComposeStubs.WearMaterial3LocalContentColorStub,
            )
            .run()
            .expect(
                """
                src/test/test.kt:13: Warning: Using androidx.wear.compose.material3.LocalContentColor inside a @RemoteComposable function has no effect. [LocalContentColorUsage]
                        LocalContentColor provides Color.Red
                        ~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 13: Replace with LocalRemoteContentColor:
                @@ -7,0 +8 @@
                +import androidx.wear.compose.remote.material3.LocalRemoteContentColor
                @@ -13 +14 @@
                -        LocalContentColor provides Color.Red
                +        LocalRemoteContentColor provides Color.Red
                """
                    .trimIndent()
            )
    }

    @Test
    fun wearMaterialLocalContentColorOutsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.wear.compose.material.LocalContentColor

                    @Composable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalContentColor provides Color.Red
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                ComposeStubs.ProvidableCompositionLocalStub,
                WearComposeStubs.WearMaterialLocalContentColorStub,
            )
            .run()
            .expectClean()
    }

    @Test
    fun wearMaterial3LocalContentColorOutsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.wear.compose.material3.LocalContentColor

                    @Composable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalContentColor provides Color.Red
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                ComposeStubs.ProvidableCompositionLocalStub,
                WearComposeStubs.WearMaterial3LocalContentColorStub,
            )
            .run()
            .expectClean()
    }
}
