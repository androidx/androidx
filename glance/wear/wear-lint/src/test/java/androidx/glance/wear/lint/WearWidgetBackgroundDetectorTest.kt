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

    private val wearWidgetStub: TestFile =
        kotlin(
                "src/androidx/glance/wear/WearWidgetDocument.kt",
                """
        package androidx.glance.wear

        sealed class WearWidgetBrush {
            companion object {
                fun color(color: Long): WearWidgetBrush = WearWidgetBrush()
                fun color(color: Any): WearWidgetBrush = WearWidgetBrush()
                fun verticalGradient(colors: Any): WearWidgetBrush = WearWidgetBrush()
                fun image(resId: Int): WearWidgetBrush = WearWidgetBrush()
            }
        }

            class WearWidgetDocument(
                private val background: WearWidgetBrush,
                private val content: () -> Unit
            )
        """,
            )
            .indented()

    private val composeColorStub: TestFile =
        kotlin(
                "src/androidx/compose/ui/graphics/Color.kt",
                """
        package androidx.compose.ui.graphics

        class Color(val value: Long) {
            companion object {
                val Black = Color(0xFF000000)
                val Red = Color(0xFFFF0000)
                val White = Color(0xFFFFFFFF)
            }
        }
        """,
            )
            .indented()

    private val composeRemoteColorStub: TestFile =
        kotlin(
                "src/main/java/androidx/compose/remote/creation/compose/state/RemoteColor.kt",
                """
        package androidx.compose.remote.creation.compose.state

        class RemoteColor
        val Any.rc: RemoteColor get() = RemoteColor()
        """,
            )
            .indented()

    @Test
    fun testValidBackground_noLintReports() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.state.rc
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(Color.Red.rc))
                    }
                """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testEmptyCompanionObject_reportsWarning() {
        lint()
            .files(
                wearWidgetStub,
                composeColorStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: WearWidgetDocument background cannot be an empty WearWidgetBrush reference [WearWidgetEmptyBackground]
                    WearWidgetDocument(background = WearWidgetBrush)
                                                    ~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testEmptyCompanionReference_reportsWarning() {
        lint()
            .files(
                wearWidgetStub,
                composeColorStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.Companion)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: WearWidgetDocument background cannot be an empty WearWidgetBrush reference [WearWidgetEmptyBackground]
                    WearWidgetDocument(background = WearWidgetBrush.Companion)
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testEmptyCompanionVariableReference_reportsWarning() {
        lint()
            .files(
                wearWidgetStub,
                composeColorStub,
                kotlin(
                        """
                    package com.example

                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument

                    fun buildWidget() {
                        val myBackground = WearWidgetBrush
                        WearWidgetDocument(background = myBackground)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:8: Warning: WearWidgetDocument background cannot be an empty WearWidgetBrush reference [WearWidgetEmptyBackground]
                    WearWidgetDocument(background = myBackground)
                                                    ~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
    }
}
