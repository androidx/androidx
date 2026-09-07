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

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.AndroidComposeUiTextFlags
import androidx.compose.ui.text.AndroidParagraph
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
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
    private val hasPlaceholder: Boolean,
) {
    private val context = InstrumentationRegistry.getInstrumentation().context

    @get:Rule val composeTestRule = createComposeRule()

    private val defaultDensity = composeTestRule.density

    private var originalLineHeightOptimizationEnabled = true

    @Before
    fun setup() {
        originalLineHeightOptimizationEnabled =
            AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled
    }

    @After
    fun cleanup() {
        AndroidComposeUiTextFlags.isSingleLineLineHeightOptimizationEnabled =
            originalLineHeightOptimizationEnabled
    }

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
        @Parameterized.Parameters(
            name = "{0}_{5}_w={9}_nullStyle={10}_trim={2}_align={3}_mode={4}_placeholder={11}"
        )
        fun data(): Collection<Array<Any>> {
            val testCases =
                listOf(
                    "English" to "Hello World",
                    "Arabic" to "مرحبا بالعالم",
                    "Burmese" to "မင်္ဂလာပါကမ္ဘာလောက",
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

                    // Additional compact typography styles
                    TypographyStyle("compact30_30", 30.sp, 30.sp, 0.sp),
                    TypographyStyle("numeralSmall", 40.sp, 46.sp, 0.sp),
                    TypographyStyle("displaySmallCompact", 34.sp, 40.sp, 0.sp),
                    TypographyStyle("titleLargeCompact", 24.sp, 28.sp, 0.sp),
                    TypographyStyle("titleMediumCompact", 16.sp, 20.sp, 0.sp),
                    TypographyStyle("titleSmallCompact", 14.sp, 18.sp, 0.sp),
                    TypographyStyle("bodySmallCompact", 12.sp, 16.sp, 0.sp),
                    TypographyStyle("bodyExtraSmallCompact", 10.sp, 14.sp, 0.sp),
                )

            val widths = listOf(5000, 1000)

            val list = mutableListOf<Array<Any>>()
            val placeholderOptions = listOf(true, false)
            for ((script, txt) in testCases) {
                for (style in typographyStyles) {
                    for (w in widths) {
                        for (hasPl in placeholderOptions) {
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
                                    hasPl,
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
                                                hasPl,
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return if (DoFullValidation) list else list.shuffled().take(500)
        }
    }

    @Ignore("Validation tests")
    @Test
    fun compareSingleLineHeightBehavior() {
        val style =
            TextStyle(
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontFamily = FontFamily.Default,
                lineHeightStyle =
                    if (isLineHeightStyleNull) {
                        null
                    } else {
                        LineHeightStyle(alignment = alignment, trim = trim, mode = mode)
                    },
                letterSpacing = letterSpacing,
            )

        val placeholders =
            if (hasPlaceholder) {
                listOf(
                    AnnotatedString.Range(
                        Placeholder(
                            width = fontSize * 2,
                            height = fontSize * 2,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                        ),
                        start = 0,
                        end = 1,
                    )
                )
            } else {
                emptyList()
            }

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
                placeholders = placeholders,
            )
        val newParagraph =
            Paragraph(
                paragraphIntrinsics = newIntrinsics,
                constraints = Constraints(maxWidth = maxWidthParam),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
                as AndroidParagraph

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
                placeholders = placeholders,
            )
        val oldParagraph =
            Paragraph(
                paragraphIntrinsics = oldIntrinsics,
                constraints = Constraints(maxWidth = maxWidthParam),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
                as AndroidParagraph

        // This is our baseline because it doesn't force any line height and uses BoringLayout
        val noLineHeightStyle =
            TextStyle(
                fontSize = fontSize,
                lineHeight = TextUnit.Unspecified,
                fontFamily = FontFamily.Default,
                lineHeightStyle = null,
                letterSpacing = letterSpacing,
            )

        val noLineHeightIntrinsics =
            ParagraphIntrinsics(
                text = text,
                style = noLineHeightStyle,
                annotations = emptyList(),
                density = defaultDensity,
                fontFamilyResolver = createFontFamilyResolver(context),
                softWrap = false,
                placeholders = placeholders,
            )
        val noLineHeightParagraph =
            Paragraph(
                paragraphIntrinsics = noLineHeightIntrinsics,
                constraints = Constraints(maxWidth = maxWidthParam),
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
                as AndroidParagraph

        try {
            runComparisonAssertions(
                newParagraph = newParagraph,
                oldParagraph = oldParagraph,
                noLineHeightParagraph = noLineHeightParagraph,
            )
        } catch (e: Throwable) {
            if (Build.VERSION.SDK_INT >= 26) {
                captureSideBySideDemo(style, oldParagraph, newParagraph, noLineHeightParagraph)
            }
            throw e
        }
    }

    @RequiresApi(26)
    private fun captureSideBySideDemo(
        style: TextStyle,
        oldParagraph: Paragraph,
        newParagraph: Paragraph,
        noLineHeightParagraph: Paragraph,
    ) {
        try {
            composeTestRule.setContent {
                Column(modifier = Modifier.padding(8.dp)) {
                    val textColor =
                        if (style.color != Color.Unspecified) style.color else Color.Black

                    // Draw oldParagraph on a Yellow background
                    val oldWidth = with(defaultDensity) { oldParagraph.width.toDp() }
                    val oldHeight = with(defaultDensity) { oldParagraph.height.toDp() }
                    Canvas(modifier = Modifier.size(oldWidth, oldHeight).background(Color.Yellow)) {
                        val oldSelectionPath = oldParagraph.getPathForRange(0, text.length)
                        drawPath(oldSelectionPath, color = Color(0xFF2196F3).copy(alpha = 0.3f))
                        oldParagraph.paint(drawContext.canvas, color = textColor)

                        // Paragraph's own first baseline
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, oldParagraph.firstBaseline),
                            end = Offset(size.width, oldParagraph.firstBaseline),
                            strokeWidth = 1f,
                        )

                        // Reference (noLineHeight) first baseline
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, noLineHeightParagraph.firstBaseline),
                            end = Offset(size.width, noLineHeightParagraph.firstBaseline),
                            strokeWidth = 1f,
                        )

                        // Reference (noLineHeight) height
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, noLineHeightParagraph.height),
                            end = Offset(size.width, noLineHeightParagraph.height),
                            strokeWidth = 1f,
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Draw newParagraph on a Cyan background
                    val newWidth = with(defaultDensity) { newParagraph.width.toDp() }
                    val newHeight = with(defaultDensity) { newParagraph.height.toDp() }
                    Canvas(modifier = Modifier.size(newWidth, newHeight).background(Color.Cyan)) {
                        val newSelectionPath = newParagraph.getPathForRange(0, text.length)
                        drawPath(newSelectionPath, color = Color(0xFF2196F3).copy(alpha = 0.3f))
                        newParagraph.paint(drawContext.canvas, color = textColor)

                        // Paragraph's own first baseline
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, newParagraph.firstBaseline),
                            end = Offset(size.width, newParagraph.firstBaseline),
                            strokeWidth = 1f,
                        )

                        // Reference (noLineHeight) first baseline
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, noLineHeightParagraph.firstBaseline),
                            end = Offset(size.width, noLineHeightParagraph.firstBaseline),
                            strokeWidth = 1f,
                        )

                        // Reference (noLineHeight) height
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, noLineHeightParagraph.height),
                            end = Offset(size.width, noLineHeightParagraph.height),
                            strokeWidth = 1f,
                        )
                    }
                }
            }
            composeTestRule.waitForIdle()

            val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
            val fileName =
                "api_34_SingleLineMismatch_${scriptName}_${styleName}_trim${trim}_mode${mode}.png"
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(
                "SingleLineTest",
                "Saved side-by-side mismatch screenshot to: ${file.absolutePath}",
            )
        } catch (t: Throwable) {
            Log.e("SingleLineTest", "Failed to capture side-by-side demo screenshot", t)
        }
    }

    private fun runComparisonAssertions(
        newParagraph: AndroidParagraph,
        oldParagraph: AndroidParagraph,
        noLineHeightParagraph: AndroidParagraph,
    ) {
        val newHeight = newParagraph.height
        val newFirstBaseline = newParagraph.firstBaseline
        val newLineTop = newParagraph.getLineTop(0)
        val newLineBottom = newParagraph.getLineBottom(0)
        val newCursorRect = newParagraph.getCursorRect(0)
        val newCursorRectAtEnd = newParagraph.getCursorRect(text.length)
        val newSelectionPathBounds = newParagraph.getPathForRange(0, text.length).getBounds()
        val newBoundingBoxes = FloatArray(text.length * 4)
        newParagraph.fillBoundingBoxes(TextRange(0, text.length), newBoundingBoxes, 0)
        val newPlaceholderRects = newParagraph.placeholderRects
        val newBoundingBoxesList = List(text.length) { i -> newParagraph.getBoundingBox(i) }

        val oldHeight = oldParagraph.height
        val oldFirstBaseline = oldParagraph.firstBaseline
        val oldLineTop = oldParagraph.getLineTop(0)
        val oldLineBottom = oldParagraph.getLineBottom(0)
        val oldCursorRect = oldParagraph.getCursorRect(0)
        val oldCursorRectAtEnd = oldParagraph.getCursorRect(text.length)
        val oldSelectionPathBounds = oldParagraph.getPathForRange(0, text.length).getBounds()
        val oldBoundingBoxes = FloatArray(text.length * 4)
        oldParagraph.fillBoundingBoxes(TextRange(0, text.length), oldBoundingBoxes, 0)
        val oldPlaceholderRects = oldParagraph.placeholderRects
        val oldBoundingBoxesList = List(text.length) { i -> oldParagraph.getBoundingBox(i) }

        val heightMatches = abs(newHeight - oldHeight) <= 1f
        val baselineMatches = abs(newFirstBaseline - oldFirstBaseline) <= 1f
        val selectionTopMatches = (newSelectionPathBounds.top - oldSelectionPathBounds.top) <= 1f
        val selectionBottomMatches =
            (newSelectionPathBounds.bottom - oldSelectionPathBounds.bottom) <= 1f

        if (!heightMatches || !baselineMatches || !selectionTopMatches || !selectionBottomMatches) {
            Log.d("SingleLineTest", "    MISMATCH FOUND!")
            Log.d(
                "SingleLineTest",
                "      style=$styleName fontSize=$fontSize lineHeight=$lineHeight",
            )
            Log.d(
                "SingleLineTest",
                "    NEW BEHAVIOR: Height: $newHeight, FirstBaseline: $newFirstBaseline, LineTop: $newLineTop, LineBottom: $newLineBottom, TopSelection: ${newSelectionPathBounds.top}, BottomSelection: ${newSelectionPathBounds.bottom}",
            )
            Log.d(
                "SingleLineTest",
                "    OLD BEHAVIOR: Height: $oldHeight, FirstBaseline: $oldFirstBaseline, LineTop: $oldLineTop, LineBottom: $oldLineBottom, TopSelection: ${oldParagraph.getPathForRange(0, text.length).getBounds().top}, BottomSelection: ${oldSelectionPathBounds.bottom}",
            )
            Log.d(
                "SingleLineTest",
                "    NO LINE HEIGHT: Height: ${noLineHeightParagraph.height}, FirstBaseline: ${noLineHeightParagraph.firstBaseline}, LineTop: ${noLineHeightParagraph.getLineTop(0)}, LineBottom: ${noLineHeightParagraph.getLineBottom(0)}, TopSelection: ${noLineHeightParagraph.getPathForRange(0, text.length).getBounds().top}, BottomSelection: ${noLineHeightParagraph.getPathForRange(0, text.length).getBounds().bottom}",
            )
        } else {
            Log.d(
                "SingleLineTest",
                "    MATCH (or very close): Height ~$newHeight, Baseline ~$newFirstBaseline",
            )
        }

        assertWithMessage(
                "Height mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newHeight)
            .isWithin(1f)
            .of(oldHeight)

        assertWithMessage(
                "Baseline mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newFirstBaseline)
            .isWithin(1f)
            .of(oldFirstBaseline)

        assertWithMessage(
                "LineTop mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newLineTop)
            .isWithin(1f)
            .of(oldLineTop)

        assertWithMessage(
                "LineBottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newLineBottom)
            .isWithin(1f)
            .of(oldLineBottom)

        assertWithMessage(
                "CursorRect(0) top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRect.top)
            .isWithin(1f)
            .of(oldCursorRect.top)

        assertWithMessage(
                "CursorRect(0) bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRect.bottom)
            .isWithin(1f)
            .of(oldCursorRect.bottom)

        assertWithMessage(
                "CursorRect(0) left mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRect.left)
            .isWithin(1f)
            .of(oldCursorRect.left)

        assertWithMessage(
                "CursorRect(0) right mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRect.right)
            .isWithin(1f)
            .of(oldCursorRect.right)

        assertWithMessage(
                "CursorRect(end) top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRectAtEnd.top)
            .isWithin(1f)
            .of(oldCursorRectAtEnd.top)

        assertWithMessage(
                "CursorRect(end) bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRectAtEnd.bottom)
            .isWithin(1f)
            .of(oldCursorRectAtEnd.bottom)

        assertWithMessage(
                "CursorRect(end) left mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRectAtEnd.left)
            .isWithin(1f)
            .of(oldCursorRectAtEnd.left)

        assertWithMessage(
                "CursorRect(end) right mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newCursorRectAtEnd.right)
            .isWithin(1f)
            .of(oldCursorRectAtEnd.right)

        assertWithMessage(
                "SelectionPath top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newSelectionPathBounds.top)
            .isWithin(1f)
            .of(oldSelectionPathBounds.top)

        assertWithMessage(
                "SelectionPath bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newSelectionPathBounds.bottom)
            .isWithin(1f)
            .of(oldSelectionPathBounds.bottom)

        assertWithMessage(
                "SelectionPath left mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newSelectionPathBounds.left)
            .isWithin(1f)
            .of(oldSelectionPathBounds.left)

        assertWithMessage(
                "SelectionPath right mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newSelectionPathBounds.right)
            .isWithin(1f)
            .of(oldSelectionPathBounds.right)

        assertWithMessage(
                "placeholderRects.size mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
            )
            .that(newPlaceholderRects.size)
            .isEqualTo(oldPlaceholderRects.size)

        for (i in newPlaceholderRects.indices) {
            val newRect = newPlaceholderRects[i]
            val oldRect = oldPlaceholderRects[i]
            if (newRect != null && oldRect != null) {
                assertWithMessage(
                        "placeholderRects[$i] top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                    )
                    .that(newRect.top)
                    .isWithin(1f)
                    .of(oldRect.top)

                assertWithMessage(
                        "placeholderRects[$i] bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                    )
                    .that(newRect.bottom)
                    .isWithin(1f)
                    .of(oldRect.bottom)
            } else {
                assertWithMessage(
                        "placeholderRects[$i] null mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                    )
                    .that(newRect)
                    .isEqualTo(oldRect)
            }
        }

        for (i in text.indices) {
            val newBox = newBoundingBoxesList[i]
            val oldBox = oldBoundingBoxesList[i]
            assertWithMessage(
                    "getBoundingBox($i) top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                )
                .that(newBox.top)
                .isWithin(1f)
                .of(oldBox.top)

            assertWithMessage(
                    "getBoundingBox($i) bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                )
                .that(newBox.bottom)
                .isWithin(1f)
                .of(oldBox.bottom)

            val arrayIndex = i * 4
            assertWithMessage(
                    "fillBoundingBoxes($i) top mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                )
                .that(newBoundingBoxes[arrayIndex + 1])
                .isWithin(1f)
                .of(oldBoundingBoxes[arrayIndex + 1])

            assertWithMessage(
                    "fillBoundingBoxes($i) bottom mismatch for $scriptName fontSize=${fontSizeSp} lineHeight=${lineHeightSp} style=$styleName trim=$trim align=$alignment mode=$mode"
                )
                .that(newBoundingBoxes[arrayIndex + 3])
                .isWithin(1f)
                .of(oldBoundingBoxes[arrayIndex + 3])
        }
    }
}
