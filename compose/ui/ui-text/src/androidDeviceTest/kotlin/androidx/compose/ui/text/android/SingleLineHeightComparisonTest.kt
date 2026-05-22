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

package androidx.compose.ui.text.android

import android.util.Log
import androidx.compose.ui.text.AndroidComposeUiTextFlags
import androidx.compose.ui.text.AndroidParagraph
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontTestData
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// In CI run only a few tests, this is used for local validation
internal const val DoFullValidation = false

/** Delete when [AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled] is removed */
@OptIn(ExperimentalTextApi::class)
@RunWith(Parameterized::class)
class SingleLineHeightComparisonTest(
    private val scriptName: String,
    private val text: String,
    private val trimInt: Int,
    private val alignFloat: Float,
    private val modeInt: Int,
    private val styleName: String,
    private val fontSizeSp: Float,
    private val lineHeightSp: Float,
    private val letterSpacingSp: Float,
    private val maxWidthParam: Int,
    private val isLineHeightStyleNull: Boolean,
) {
    private val fontFamilyMeasureFont = FontTestData.BASIC_MEASURE_FONT.toFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)

    private val trim: Trim
        get() = Trim(trimInt)

    private val alignment: Alignment
        get() = Alignment(alignFloat)

    private val mode: LineHeightStyle.Mode
        get() = LineHeightStyle.Mode(modeInt)

    private val fontSize: TextUnit
        get() = fontSizeSp.sp

    private val lineHeight: TextUnit
        get() = if (lineHeightSp.isNaN()) TextUnit.Unspecified else lineHeightSp.sp

    private val letterSpacing: TextUnit
        get() = letterSpacingSp.sp

    companion object {
        data class TypographyStyle(
            val name: String,
            val fontSize: TextUnit,
            val lineHeight: TextUnit,
            val letterSpacing: TextUnit,
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}_{5}_w={9}_nullStyle={10}_trim={2}_align={3}_mode={4}")
        fun data(): Collection<Array<Any>> {
            val testCases =
                listOf(
                    "English text" to "Hello World",
                    "Arabic text" to "مرحبا بالعالم",
                    "Burmese text" to "မင်္ဂလာပါကမ္ဘာလောက",
                )
            val trims = listOf(Trim.Both, Trim.None, Trim.FirstLineTop, Trim.LastLineBottom)
            val alignments =
                listOf(Alignment.Center, Alignment.Top, Alignment.Bottom, Alignment.Proportional)
            val modes =
                listOf(
                    LineHeightStyle.Mode.Fixed,
                    LineHeightStyle.Mode.Minimum,
                    LineHeightStyle.Mode.Tight,
                )

            val typographyStyles =
                listOf(
                    // Default values after resolution. Note that unspecified line height is
                    // resolved
                    // to Float.NaN. It's evaluated on the LineHeightStyleSpan level.
                    TypographyStyle("default", 14.sp, TextUnit.Unspecified, 0.sp),
                    // Material 3 styles
                    TypographyStyle("displayLarge", 57.sp, 64.sp, (-0.25f).sp),
                    TypographyStyle("displayMedium", 45.sp, 52.sp, 0.sp),
                    TypographyStyle("displaySmall", 36.sp, 44.sp, 0.sp),
                    TypographyStyle("headlineLarge", 32.sp, 40.sp, 0.sp),
                    TypographyStyle("headlineMedium", 28.sp, 36.sp, 0.sp),
                    TypographyStyle("headlineSmall", 24.sp, 32.sp, 0.sp),
                    TypographyStyle("titleLarge", 22.sp, 28.sp, 0.sp),
                    TypographyStyle("titleMedium", 16.sp, 24.sp, 0.15f.sp),
                    TypographyStyle("titleSmall", 14.sp, 20.sp, 0.1f.sp),
                    TypographyStyle("bodyLarge", 16.sp, 24.sp, 0.5f.sp),
                    TypographyStyle("bodyMedium", 14.sp, 20.sp, 0.25f.sp),
                    TypographyStyle("bodySmall", 12.sp, 16.sp, 0.4f.sp),
                    TypographyStyle("labelLarge", 14.sp, 20.sp, 0.1f.sp),
                    TypographyStyle("labelMedium", 12.sp, 16.sp, 0.5f.sp),
                    TypographyStyle("labelSmall", 11.sp, 16.sp, 0.5f.sp),

                    // Material 2 styles
                    TypographyStyle("h1", 96.sp, 112.sp, (-1.5f).sp),
                    TypographyStyle("h2", 60.sp, 72.sp, (-0.5f).sp),
                    TypographyStyle("h3", 48.sp, 56.sp, 0.sp),
                    TypographyStyle("h4", 34.sp, 40.sp, 0.25f.sp),
                    TypographyStyle("h5", 24.sp, 32.sp, 0.sp),
                    TypographyStyle("h6", 20.sp, 28.sp, 0.15f.sp),
                    TypographyStyle("subtitle1", 16.sp, 24.sp, 0.15f.sp),
                    TypographyStyle("subtitle2", 14.sp, 20.sp, 0.1f.sp),
                    TypographyStyle("body1", 16.sp, 24.sp, 0.5f.sp),
                    TypographyStyle("body2", 14.sp, 20.sp, 0.25f.sp),
                    TypographyStyle("button", 14.sp, 20.sp, 1.25f.sp),
                    TypographyStyle("caption", 12.sp, 16.sp, 0.4f.sp),
                    TypographyStyle("overline", 10.sp, 16.sp, 1.5f.sp),
                )

            val widths = listOf(5000, 1000, 200)

            val list = mutableListOf<Array<Any>>()
            for ((script, txt) in testCases) {
                for (style in typographyStyles) {
                    for (w in widths) {
                        // pass null line height style
                        val defaultLineHeightStyle = LineHeightStyle.Default
                        list.add(
                            arrayOf(
                                script,
                                txt,
                                defaultLineHeightStyle.trim.value,
                                defaultLineHeightStyle.alignment.topRatio,
                                defaultLineHeightStyle.mode.value,
                                style.name,
                                style.fontSize.value,
                                style.lineHeight.value,
                                style.letterSpacing.value,
                                w,
                                true,
                            )
                        )
                        // all line height styles
                        for (t in trims) {
                            for (a in alignments) {
                                for (m in modes) {
                                    list.add(
                                        arrayOf(
                                            script,
                                            txt,
                                            t.value,
                                            a.topRatio,
                                            m.value,
                                            style.name,
                                            style.fontSize.value,
                                            style.lineHeight.value,
                                            style.letterSpacing.value,
                                            w,
                                            false,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return if (DoFullValidation) list else list.subList(0, 200)
        }
    }

    @Test
    fun compareSingleLineHeightBehavior() {
        val style =
            TextStyle(
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = fontFamilyMeasureFont,
                lineHeightStyle =
                    if (isLineHeightStyleNull) {
                        null
                    } else {
                        LineHeightStyle(alignment = alignment, trim = trim, mode = mode)
                    },
                letterSpacing = letterSpacing,
            )

        // Test with the new behavior (spans removed, layout heights adjusted when softWrap is
        // false)
        AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled = true
        val newIntrinsics =
            ParagraphIntrinsics(
                text = text,
                style = style,
                annotations = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = createFontFamilyResolver(context),
                softWrap = false,
                placeholders = emptyList(),
            )
        val newParagraph =
            Paragraph(
                paragraphIntrinsics = newIntrinsics,
                constraints = Constraints(maxWidth = maxWidthParam),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
                as AndroidParagraph

        val newHeight = newParagraph.height
        val newFirstBaseline = newParagraph.firstBaseline
        val newLineTop = newParagraph.getLineTop(0)
        val newLineBottom = newParagraph.getLineBottom(0)

        // Test with the old behavior (spans kept, internal font metrics mutated when softWrap is
        // true)
        AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled = false
        val oldIntrinsics =
            ParagraphIntrinsics(
                text = text,
                style = style,
                annotations = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = createFontFamilyResolver(context),
                softWrap = false,
                placeholders = emptyList(),
            )
        val oldParagraph =
            Paragraph(
                paragraphIntrinsics = oldIntrinsics,
                constraints = Constraints(maxWidth = maxWidthParam),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
                as AndroidParagraph

        val oldHeight = oldParagraph.height
        val oldFirstBaseline = oldParagraph.firstBaseline
        val oldLineTop = oldParagraph.getLineTop(0)
        val oldLineBottom = oldParagraph.getLineBottom(0)

        val heightMatches = abs(newHeight - oldHeight) <= 1f
        val baselineMatches = abs(newFirstBaseline - oldFirstBaseline) <= 1f

        if (!heightMatches || !baselineMatches) {
            Log.d("SingleLineTest", "    MISMATCH FOUND!")
            Log.d(
                "SingleLineTest",
                "      style=$styleName fontSize=$fontSize lineHeight=$lineHeight",
            )
            Log.d(
                "SingleLineTest",
                "    NEW BEHAVIOR: Height: $newHeight, FirstBaseline: $newFirstBaseline, LineTop: $newLineTop, LineBottom: $newLineBottom",
            )
            Log.d(
                "SingleLineTest",
                "    OLD BEHAVIOR: Height: $oldHeight, FirstBaseline: $oldFirstBaseline, LineTop: $oldLineTop, LineBottom: $oldLineBottom",
            )
        } else {
            Log.d(
                "SingleLineTest",
                "    MATCH (or very close): Height ~$newHeight, Baseline ~$newFirstBaseline",
            )
        }

        assertWithMessage(
                "Height mismatch for $scriptName style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newHeight)
            .isWithin(1f)
            .of(oldHeight)

        assertWithMessage(
                "Baseline mismatch for $scriptName style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newFirstBaseline)
            .isWithin(1f)
            .of(oldFirstBaseline)
    }
}
