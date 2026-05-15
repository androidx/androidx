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
class LocalTextStyleDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LocalTextStyleDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(LocalTextStyleDetector.LocalTextStyleUsage)

    @Test
    fun wearMaterialLocalTextStyleInsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.TextStyle
                    import androidx.compose.remote.creation.compose.layout.RemoteComposable
                    import androidx.wear.compose.material.LocalTextStyle

                    @Composable
                    @RemoteComposable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(background = Color.Red)
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                RemoteStubs.RemoteComposableStub,
                ComposeStubs.ProvidableCompositionLocalStub,
                ComposeStubs.TextStyleStub,
                WearComposeStubs.WearMaterialLocalTextStyleStub,
            )
            .run()
            .expect(
                """
                src/test/test.kt:14: Warning: Using androidx.wear.compose.material.LocalTextStyle inside a @RemoteComposable function has no effect. [LocalTextStyleUsage]
                        LocalTextStyle provides TextStyle(background = Color.Red)
                        ~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 14: Replace with LocalRemoteTextStyle:
                @@ -8,0 +9 @@
                +import androidx.wear.compose.remote.material3.LocalRemoteTextStyle
                @@ -14 +15 @@
                -        LocalTextStyle provides TextStyle(background = Color.Red)
                +        LocalRemoteTextStyle provides TextStyle(background = Color.Red)
                """
                    .trimIndent()
            )
    }

    @Test
    fun wearMaterial3LocalTextStyleInsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.TextStyle
                    import androidx.compose.remote.creation.compose.layout.RemoteComposable
                    import androidx.wear.compose.material3.LocalTextStyle

                    @Composable
                    @RemoteComposable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(background = Color.Red)
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                RemoteStubs.RemoteComposableStub,
                ComposeStubs.ProvidableCompositionLocalStub,
                ComposeStubs.TextStyleStub,
                WearComposeStubs.WearMaterial3LocalTextStyleStub,
            )
            .run()
            .expect(
                """
                src/test/test.kt:14: Warning: Using androidx.wear.compose.material3.LocalTextStyle inside a @RemoteComposable function has no effect. [LocalTextStyleUsage]
                        LocalTextStyle provides TextStyle(background = Color.Red)
                        ~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Autofix for src/test/test.kt line 14: Replace with LocalRemoteTextStyle:
                @@ -8,0 +9 @@
                +import androidx.wear.compose.remote.material3.LocalRemoteTextStyle
                @@ -14 +15 @@
                -        LocalTextStyle provides TextStyle(background = Color.Red)
                +        LocalRemoteTextStyle provides TextStyle(background = Color.Red)
                """
                    .trimIndent()
            )
    }

    @Test
    fun wearMaterialLocalTextStyleOutsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.TextStyle
                    import androidx.wear.compose.material.LocalTextStyle

                    @Composable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(background = Color.Red)
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                ComposeStubs.ProvidableCompositionLocalStub,
                ComposeStubs.TextStyleStub,
                WearComposeStubs.WearMaterialLocalTextStyleStub,
            )
            .run()
            .expectClean()
    }

    @Test
    fun wearMaterial3LocalTextStyleOutsideRemoteComposable() {
        lint()
            .files(
                kotlin(
                        """
                    package test

                    import androidx.compose.runtime.Composable
                    import androidx.compose.runtime.CompositionLocalProvider
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.text.TextStyle
                    import androidx.wear.compose.material3.LocalTextStyle

                    @Composable
                    fun TestComponent() {
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(background = Color.Red)
                        ) { }
                    }
                    """
                    )
                    .indented(),
                Stubs.Composable,
                Stubs.Color,
                ComposeStubs.ProvidableCompositionLocalStub,
                ComposeStubs.TextStyleStub,
                WearComposeStubs.WearMaterial3LocalTextStyleStub,
            )
            .run()
            .expectClean()
    }
}
