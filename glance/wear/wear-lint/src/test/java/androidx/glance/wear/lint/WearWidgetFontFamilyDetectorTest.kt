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
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WearWidgetFontFamilyDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = WearWidgetFontFamilyDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(WearWidgetFontFamilyDetector.CUSTOM_FONT_FAMILY_ISSUE)

    override fun lint(): TestLintTask = super.lint().skipTestModes(TestMode.JVM_OVERLOADS)

    private val remoteFontFamilyStub: TestFile =
        kotlin(
                """
        package androidx.compose.remote.creation.compose.text

        abstract class RemoteFontFamily {
            companion object {
                val Default = object : RemoteFontFamily() {}
                val SansSerif = object : RemoteFontFamily() {}
                val Serif = object : RemoteFontFamily() {}
                val Monospace = object : RemoteFontFamily() {}
                val Cursive = object : RemoteFontFamily() {}
            }
        }
        """
            )
            .indented()

    private val colorStub: TestFile =
        kotlin(
                """
        package androidx.compose.ui.graphics

        @JvmInline
        value class Color(val value: ULong) {
            companion object {
                val Red = Color(0xFFFF0000)
                val Blue = Color(0xFF0000FF)
            }
        }
        """
            )
            .indented()

    private val unitStub: TestFile =
        kotlin(
                """
        package androidx.compose.ui.unit

        @JvmInline
        value class TextUnit(val value: Long)

        val Int.sp: TextUnit get() = TextUnit(this.toLong())
        """
            )
            .indented()

    private val remoteTextStyleStub: TestFile =
        kotlin(
                """
        package androidx.compose.remote.creation.compose.text

        import androidx.compose.ui.graphics.Color
        import androidx.compose.ui.unit.TextUnit

        class RemoteTextStyle(
            val color: Color = Color.Red,
            val fontSize: TextUnit = TextUnit(0),
            val fontFamily: RemoteFontFamily? = null,
        ) {
            fun copy(
                color: Color = this.color,
                fontSize: TextUnit = this.fontSize,
                fontFamily: RemoteFontFamily? = this.fontFamily,
            ): RemoteTextStyle = this

            fun merge(
                other: RemoteTextStyle? = null,
                fontFamily: RemoteFontFamily? = null,
            ): RemoteTextStyle = this

            companion object {
                val Default = RemoteTextStyle()
            }
        }
        """
            )
            .indented()

    private val remoteLayoutRemoteTextStub: TestFile =
        kotlin(
                """
        package androidx.compose.remote.creation.compose.layout

        import androidx.compose.remote.creation.compose.text.RemoteFontFamily
        import androidx.compose.remote.creation.compose.text.RemoteTextStyle

        fun RemoteText(
            text: String,
            style: RemoteTextStyle = RemoteTextStyle.Default,
            fontFamily: RemoteFontFamily? = null,
        ) {}
        """
            )
            .indented()

    private val wearMaterial3RemoteTextStub: TestFile =
        kotlin(
                """
        package androidx.wear.compose.remote.material3

        import androidx.compose.remote.creation.compose.text.RemoteFontFamily
        import androidx.compose.remote.creation.compose.text.RemoteTextStyle

        fun RemoteText(
            text: String,
            style: RemoteTextStyle = RemoteTextStyle.Default,
            fontFamily: RemoteFontFamily? = null,
        ) {}
        """
            )
            .indented()

    private val unrelatedRemoteTextStub: TestFile =
        kotlin(
                """
        package com.example.unrelated

        import androidx.compose.remote.creation.compose.text.RemoteFontFamily

        fun RemoteText(
            text: String,
            fontFamily: RemoteFontFamily? = null,
        ) {}
        """
            )
            .indented()

    private val commonStubs =
        arrayOf(
            remoteFontFamilyStub,
            colorStub,
            unitStub,
            remoteTextStyleStub,
            remoteLayoutRemoteTextStub,
            wearMaterial3RemoteTextStub,
            unrelatedRemoteTextStub,
        )

    // 1. Clean Cases (No warnings)

    @Test
    fun testRemoteText_material3_noFontFamily_clean() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.wear.compose.remote.material3.RemoteText

                    fun MyWidget() {
                        RemoteText(
                            text = "Hello Wear",
                            style = RemoteTextStyle.Default,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRemoteText_layout_noFontFamily_clean() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.layout.RemoteText
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle

                    fun MyWidget() {
                        RemoteText(
                            text = "Hello Wear",
                            style = RemoteTextStyle.Default,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRemoteTextStyle_constructor_noFontFamily_clean() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.sp
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle

                    fun getStyle(): RemoteTextStyle {
                        return RemoteTextStyle(
                            color = Color.Red,
                            fontSize = 16.sp,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRemoteTextStyle_copyAndMerge_noFontFamily_clean() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.sp
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle

                    fun transformStyle(base: RemoteTextStyle): RemoteTextStyle {
                        val copied = base.copy(color = Color.Blue, fontSize = 20.sp)
                        return copied.merge(other = base)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun testUnrelatedRemoteText_withFontFamily_ignored_clean() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import com.example.unrelated.RemoteText

                    fun MyCustomUi() {
                        RemoteText(
                            text = "Custom Text",
                            fontFamily = RemoteFontFamily.SansSerif,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expectClean()
    }

    // 2. Warning Cases and Quick Fix Verifications

    @Test
    fun testRemoteText_wearMaterial3_withFontFamily_reportsAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.wear.compose.remote.material3.RemoteText

                    fun MyWidget() {
                        RemoteText(
                            text = "Hello Wear",
                            fontFamily = RemoteFontFamily.SansSerif,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:9: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                        fontFamily = RemoteFontFamily.SansSerif,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 9: Remove fontFamily parameter:
                @@ -9 +8,0 @@
                -        fontFamily = RemoteFontFamily.SansSerif,
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteText_remoteLayout_withFontFamily_reportsAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.layout.RemoteText
                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily

                    fun MyWidget() {
                        RemoteText(text = "Hello Wear", fontFamily = RemoteFontFamily.SansSerif)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                    RemoteText(text = "Hello Wear", fontFamily = RemoteFontFamily.SansSerif)
                                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 7: Remove fontFamily parameter:
                @@ -7 +7
                -     RemoteText(text = "Hello Wear", fontFamily = RemoteFontFamily.SansSerif)
                +     RemoteText(text = "Hello Wear")
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_constructor_withFontFamily_reportsAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.compose.ui.graphics.Color

                    fun getStyle(): RemoteTextStyle {
                        return RemoteTextStyle(
                            color = Color.Red,
                            fontFamily = RemoteFontFamily.SansSerif,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                        fontFamily = RemoteFontFamily.SansSerif,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 10: Remove fontFamily parameter:
                @@ -10 +9,0 @@
                -        fontFamily = RemoteFontFamily.SansSerif,
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_copy_withFontFamily_reportsAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.compose.ui.graphics.Color

                    fun updateStyle(base: RemoteTextStyle): RemoteTextStyle {
                        return base.copy(
                            color = Color.Blue,
                            fontFamily = RemoteFontFamily.Monospace,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:10: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                        fontFamily = RemoteFontFamily.Monospace,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 10: Remove fontFamily parameter:
                @@ -10 +9,0 @@
                -        fontFamily = RemoteFontFamily.Monospace,
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_merge_withFontFamily_reportsAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle

                    fun mergeStyle(base: RemoteTextStyle): RemoteTextStyle {
                        return base.merge(fontFamily = RemoteFontFamily.Cursive)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:7: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                    return base.merge(fontFamily = RemoteFontFamily.Cursive)
                                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 7: Remove fontFamily parameter:
                @@ -7 +7
                -     return base.merge(fontFamily = RemoteFontFamily.Cursive)
                +     return base.merge()
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_firstArgument_removesCommaAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.compose.ui.graphics.Color

                    fun createStyle(): RemoteTextStyle {
                        return RemoteTextStyle(fontFamily = RemoteFontFamily.SansSerif, color = Color.Red)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:8: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                    return RemoteTextStyle(fontFamily = RemoteFontFamily.SansSerif, color = Color.Red)
                                           ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 8: Remove fontFamily parameter:
                @@ -8 +8
                -     return RemoteTextStyle(fontFamily = RemoteFontFamily.SansSerif, color = Color.Red)
                +     return RemoteTextStyle(color = Color.Red)
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_middleArgument_removesCommaAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.compose.ui.graphics.Color
                    import androidx.compose.ui.unit.sp

                    fun createStyle(): RemoteTextStyle {
                        return RemoteTextStyle(
                            color = Color.Red,
                            fontFamily = RemoteFontFamily.SansSerif,
                            fontSize = 14.sp,
                        )
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:11: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                        fontFamily = RemoteFontFamily.SansSerif,
                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 11: Remove fontFamily parameter:
                @@ -11 +11
                -         fontFamily = RemoteFontFamily.SansSerif,
                """
                    .trimIndent()
            )
    }

    @Test
    fun testRemoteTextStyle_lastArgumentNoTrailingComma_removesPrecedingCommaAndFixes() {
        lint()
            .files(
                *commonStubs,
                kotlin(
                        """
                    package com.example

                    import androidx.compose.remote.creation.compose.text.RemoteFontFamily
                    import androidx.compose.remote.creation.compose.text.RemoteTextStyle
                    import androidx.compose.ui.graphics.Color

                    fun createStyle(): RemoteTextStyle {
                        return RemoteTextStyle(color = Color.Red, fontFamily = RemoteFontFamily.SansSerif)
                    }
                    """
                    )
                    .indented(),
            )
            .run()
            .expect(
                """
                src/com/example/test.kt:8: Warning: The fontFamily parameter is a no-op in Wear widgets and should be removed. [WearWidgetCustomFontFamily]
                    return RemoteTextStyle(color = Color.Red, fontFamily = RemoteFontFamily.SansSerif)
                                                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                0 errors, 1 warnings
                """
                    .trimIndent()
            )
            .expectFixDiffs(
                """
                Fix for src/com/example/test.kt line 8: Remove fontFamily parameter:
                @@ -8 +8
                -     return RemoteTextStyle(color = Color.Red, fontFamily = RemoteFontFamily.SansSerif)
                +     return RemoteTextStyle(color = Color.Red)
                """
                    .trimIndent()
            )
    }
}
