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

package androidx.glance.wear.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WearWidgetBackgroundDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WearWidgetBackgroundDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WearWidgetBackgroundDetector.EMPTY_BACKGROUND_ISSUE)

    private val composableStub: TestFile =
        kotlin(
                "src/androidx/compose/runtime/Composable.kt",
                """
            package androidx.compose.runtime

            annotation class Composable
            """,
            )
            .indented()

    private val remoteComposableStub: TestFile =
        kotlin(
                "src/androidx/compose/remote/creation/compose/layout/RemoteComposable.kt",
                """
            package androidx.compose.remote.creation.compose.layout

            import androidx.compose.runtime.Composable

            annotation class RemoteComposable

            @RemoteComposable
            @Composable
            fun RemoteBox() {}

            @RemoteComposable
            @Composable
            fun RemoteText(text: String) {}
            """,
            )
            .indented()

    private val wearWidgetDocumentStub: TestFile =
        kotlin(
                "src/androidx/glance/wear/WearWidgetDocument.kt",
                """
            package androidx.glance.wear

            import androidx.compose.remote.creation.compose.layout.RemoteComposable
            import androidx.compose.runtime.Composable

            class WearWidgetDocument(
                private val background: (@RemoteComposable @Composable () -> Unit)? = null,
                private val content: @RemoteComposable @Composable () -> Unit,
            )
            """,
            )
            .indented()

    @Test
    fun validComposableBackground_noLintReports() {
        lint()
            .files(
                composableStub,
                remoteComposableStub,
                wearWidgetDocumentStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.layout.RemoteBox
                    import androidx.compose.remote.creation.compose.layout.RemoteText
                    import androidx.glance.wear.WearWidgetDocument

                    fun buildWidget() {
                        WearWidgetDocument(background = { RemoteBox() }) {
                            RemoteText("Hello")
                        }
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }
}
