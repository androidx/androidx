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
        mutableListOf(
            WearWidgetBackgroundDetector.EMPTY_BACKGROUND_ISSUE,
            WearWidgetBackgroundDetector.INVALID_BACKGROUND_ISSUE,
        )

    private val wearWidgetStub: TestFile =
        kotlin(
                "src/androidx/glance/wear/WearWidgetBrush.kt",
                """
            package androidx.glance.wear

            import androidx.compose.remote.creation.compose.state.RemoteColor

            sealed class WearWidgetBrush {
                companion object : WearWidgetBrush()
            }

            fun WearWidgetBrush.color(color: RemoteColor): WearWidgetBrush = this
            fun WearWidgetBrush.verticalGradient(colors: Any): WearWidgetBrush = this
            fun WearWidgetBrush.image(resId: Int): WearWidgetBrush = this

            class WearWidgetDocument(
                private val background: WearWidgetBrush,
                private val content: () -> Unit = {}
            )
            """,
            )
            .indented()

    private val composeColorStub: TestFile =
        kotlin(
                "src/androidx/compose/ui/graphics/Color.kt",
                """
        package androidx.compose.ui.graphics

        @JvmInline
        value class Color(val value: ULong) {
            companion object {
                val Black = Color(0xFF000000)
                val Red = Color(0xFFFF0000)
                val White = Color(0xFFFFFFFF)
                val Transparent = Color(0x00000000)
            }
        }

        fun Color(color: Long): Color = Color(color.toULong())
        fun Color(color: Int): Color = Color(color.toLong())
        fun Color(red: Float, green: Float, blue: Float, alpha: Float = 1f): Color = Color(0xFFFFFFFF)
        """,
            )
            .indented()

    private val composeRemoteColorStub: TestFile =
        kotlin(
                "src/androidx/compose/remote/creation/compose/state/RemoteColor.kt",
                """
            package androidx.compose.remote.creation.compose.state

            import androidx.compose.ui.graphics.Color

            class RemoteColor {
                fun copy(alpha: Float? = null, red: Float? = null, green: Float? = null, blue: Float? = null): RemoteColor = this

                companion object {
                    fun rgb(red: Float, green: Float, blue: Float, alpha: Float = 1.0f): RemoteColor = RemoteColor()
                    fun hsv(hue: Float, saturation: Float, value: Float, alpha: Float = 1.0f): RemoteColor = RemoteColor()
                    operator fun invoke(value: Color): RemoteColor = RemoteColor()
                    operator fun invoke(value: Int): RemoteColor = RemoteColor()
                    operator fun invoke(value: Long): RemoteColor = RemoteColor()
                }
            }

            val Color.rc: RemoteColor get() = RemoteColor()
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
                    import androidx.glance.wear.color

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
    fun testValidRgbCall_noLintReports() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.rgb(1f, 1f, 1f))) // White
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testValidColorFloatArgs_noLintReports() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.invoke(Color(1f, 1f, 1f)))) // White
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testValidHsvCall_noLintReports() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.hsv(0f, 1f, 1f, 1f))) // Red
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testValidHexConstant_noLintReports() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.invoke(Color(0xFFFFFFFF))))
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testValidLocalVariableTracing_noLintReports() {
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
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        val myColor = Color.Red
                        val myBrush = WearWidgetBrush.color(myColor.rc)
                        WearWidgetDocument(background = myBrush)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testValidGradientBackground_noLintReports() {
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
                    import androidx.glance.wear.verticalGradient

                    fun buildWidget() {
                        val colors = listOf(Color.Black.rc, Color.White.rc)
                        WearWidgetDocument(background = WearWidgetBrush.verticalGradient(colors))
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
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
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
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
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
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
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

    @Test
    fun testSolidBlackReference_reportsError() {
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
                import androidx.glance.wear.color

                fun buildWidget() {
                    WearWidgetDocument(background = WearWidgetBrush.color(Color.Black.rc))
                }
                """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = WearWidgetBrush.color(Color.Black.rc))
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testTransparentReference_reportsError() {
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
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(Color.Transparent.rc))
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = WearWidgetBrush.color(Color.Transparent.rc))
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testBlackRgbCall_reportsError() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.rgb(0f, 0f, 0f)))
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:9: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.rgb(0f, 0f, 0f)))
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testTransparentHsvCall_reportsError() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.hsv(180f, 1f, 1f, 0f)))
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:9: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.hsv(180f, 1f, 1f, 0f)))
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testHexConstantBlack_reportsError() {
        lint()
            .files(
                composeColorStub,
                composeRemoteColorStub,
                wearWidgetStub,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.remote.creation.compose.state.RemoteColor
                    import androidx.glance.wear.WearWidgetBrush
                    import androidx.glance.wear.WearWidgetDocument
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.invoke(Color(0xFF000000))))
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = WearWidgetBrush.color(RemoteColor.invoke(Color(0xFF000000))))
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }

    @Test
    fun testLocalVariableTracing_reportsError() {
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
                    import androidx.glance.wear.color

                    fun buildWidget() {
                        val myColor = Color.Black
                        val myBrush = WearWidgetBrush.color(myColor.rc)
                        WearWidgetDocument(background = myBrush)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:12: Error: WearWidgetDocument background cannot be black or transparent [WearWidgetInvalidBackground]
                    WearWidgetDocument(background = myBrush)
                                                    ~~~~~~~
                1 errors, 0 warnings
                """
                    .trimIndent()
            )
    }
}
