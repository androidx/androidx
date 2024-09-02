/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.view.inputmethod.CursorAnchorInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class CursorAnchorInfoBuilderTest {

    private val fontFamilyMeasureFont =
        Font(resId = R.font.sample_font, weight = FontWeight.Normal, style = FontStyle.Normal)
            .toFontFamily()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val matrix = Matrix()

    @Test
    fun testSelectionDefault() {
        val text = ""
        val selection = TextRange(0)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.selectionStart).isEqualTo(0)
        Truth.assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(0)
    }

    @Test
    fun testSelectionCursor() {
        val text = "abc"
        val selection = TextRange(2)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.selectionStart).isEqualTo(2)
        Truth.assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testSelectionRange() {
        val text = "abc"
        val selection = TextRange(1, 2)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.selectionStart).isEqualTo(1)
        Truth.assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testCompositionNone() {
        val text = ""
        val selection = TextRange(0)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        Truth.assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testCompositionCoveringAllString() {
        val text = "abc"
        val selection = TextRange(0)
        val composition = TextRange(0, text.length)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(0)
        Truth.assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
    }

    @Test
    fun testCompositionCoveringPortionOfString() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val text = word1 + word2 + word3
        val selection = TextRange(0)
        val composition = TextRange(word1.length, (word1 + word2).length)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(word1.length)
        Truth.assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(word2)
    }

    @Test
    fun testCompositionNotIncludedWhenIncludeCharacterBoundsFalse() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val text = word1 + word2 + word3
        val selection = TextRange(0)
        val composition = TextRange(word1.length, (word1 + word2).length)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    getTextLayoutResult(text),
                    matrix,
                    includeCharacterBounds = false
                )

        Truth.assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        Truth.assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testResetsBetweenExecutions() {
        val text = "abc"
        val selection = TextRange(0)
        val composition = TextRange(0, text.length)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo =
            builder.build(text, selection, composition, getTextLayoutResult(text), matrix)

        Truth.assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
        Truth.assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(composition.min)

        val cursorAnchorInfo1 =
            builder.build(
                "abcd",
                selection = TextRange(0),
                composition = null,
                getTextLayoutResult(text),
                matrix
            )

        Truth.assertThat(cursorAnchorInfo1.composingText).isNull()
        Truth.assertThat(cursorAnchorInfo1.composingTextStart).isEqualTo(-1)
    }

    @Test
    fun testInsertionMarkerCursor() {
        val fontSize = 10.sp
        val text = "abc"
        val selection = TextRange(1)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(text, selection, composition, textLayoutResult, matrix)

        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerSelectionIsSameWithCursor() {
        val text = "abc"
        val selection = TextRange(1, 2)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo1 =
            builder.build(text, selection, composition, textLayoutResult, matrix)

        val cursorAnchorInfo2 =
            builder.build(text, selection = TextRange(1), composition, textLayoutResult, matrix)

        Truth.assertThat(cursorAnchorInfo1.insertionMarkerHorizontal)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerHorizontal)
        Truth.assertThat(cursorAnchorInfo1.insertionMarkerTop)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerTop)
        Truth.assertThat(cursorAnchorInfo1.insertionMarkerBottom)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerBottom)
        Truth.assertThat(cursorAnchorInfo1.insertionMarkerBaseline)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerBaseline)
        Truth.assertThat(cursorAnchorInfo1.insertionMarkerFlags)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerFlags)
    }

    @Test
    fun testInsertionMarkerCursorClamped() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val text = "abc   "
        val selection = TextRange(5)
        val composition: TextRange? = null
        val width = 4 * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(text, selection, composition, textLayoutResult, matrix)

        // The cursor position is clamped to the width of the layout.
        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(width)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerRtl() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val text = "\u05D0\u05D1\u05D2"
        val selection = TextRange(0)
        val composition: TextRange? = null
        val width = 3 * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(text, selection, composition, textLayoutResult, matrix)

        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(width)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION or CursorAnchorInfo.FLAG_IS_RTL)
    }

    @Test
    fun testInsertionMarkerNotVisible() {
        val fontSize = 10.sp
        val text = "abc"
        val selection = TextRange(1)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // insertionMarkerHorizontal = fontSizeInPx, so the insertion marker is completely outside
        // this rectangle.
        val innerTextFieldBounds =
            Rect(0f, 0f, fontSizeInPx - 1f, textLayoutResult.size.height.toFloat())

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerPartiallyVisible() {
        val fontSize = 10.sp
        val text = "abc"
        val selection = TextRange(1)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // insertionMarkerTop = 0 and insertionMarkerBottom = fontSizeInPx, so this rectangle covers
        // the top of the insertion marker but not the bottom.
        val innerTextFieldBounds = Rect(fontSizeInPx - 1f, 0f, fontSizeInPx + 1f, fontSizeInPx - 1f)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION or
                    CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
            )
    }

    @Test
    fun testInsertionMarkerNotIncludedWhenIncludeInsertionMarkerFalse() {
        val fontSize = 10.sp
        val text = "abc"
        val selection = TextRange(1)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    includeInsertionMarker = false
                )

        Truth.assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isNaN()
        Truth.assertThat(cursorAnchorInfo.insertionMarkerTop).isNaN()
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBottom).isNaN()
        Truth.assertThat(cursorAnchorInfo.insertionMarkerBaseline).isNaN()
        Truth.assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(0)
    }

    @Test
    fun testCharacterBoundsLtr() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        val selection = TextRange(0)
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val width = text.length * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(text, selection, composition, textLayoutResult, matrix)

        for (index in text.indices) {
            if (index in composition) {
                Truth.assertThat(cursorAnchorInfo.getCharacterBounds(index))
                    .isEqualTo(
                        RectF(index * fontSizeInPx, 0f, (index + 1) * fontSizeInPx, fontSizeInPx)
                    )
                Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index))
                    .isEqualTo(CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION)
            } else {
                Truth.assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
                Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
            }
        }
    }

    @Test
    fun testCharacterBoundsRtl() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "\u05D0 \u05D1\u05D2 \u05D3"
        val selection = TextRange(0)
        // Composition is on "\u05D1\u05D2"
        val composition = TextRange(2, 4)
        val width = text.length * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(text, selection, composition, textLayoutResult, matrix)

        for (index in text.indices) {
            if (index in composition) {
                Truth.assertThat(cursorAnchorInfo.getCharacterBounds(index))
                    .isEqualTo(
                        RectF(
                            width - ((index + 1) * fontSizeInPx),
                            0f,
                            width - (index * fontSizeInPx),
                            fontSizeInPx
                        )
                    )
                Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index))
                    .isEqualTo(
                        CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION or CursorAnchorInfo.FLAG_IS_RTL
                    )
            } else {
                Truth.assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
                Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
            }
        }
    }

    @Test
    fun testCharacterBoundsVisibility() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        val selection = TextRange(0)
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val width = text.length * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)
        val innerTextFieldBounds = Rect(3.5f * fontSizeInPx, 0f, 4f * fontSizeInPx, fontSizeInPx)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        // Character at index 2 spans horizontal range [2 * fontSizeInPx, 3 * fontSizeInPx], so it
        // is completely outside innerTextFieldBounds.
        Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(2))
            .isEqualTo(CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION)
        // Character at index 3 spans horizontal range [3 * fontSizeInPx, 4 * fontSizeInPx], so it
        // is partially inside innerTextFieldBounds.
        Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(3))
            .isEqualTo(
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION or
                    CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
            )
    }

    @Test
    fun testCharacterBoundsNotIncludedWhenIncludeCharacterBoundsFalse() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        val selection = TextRange(0)
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val width = text.length * fontSizeInPx
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    includeCharacterBounds = false
                )

        for (index in text.indices) {
            Truth.assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
            Truth.assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testEditorBounds() {
        val text = ""
        val selection = TextRange(0)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    getTextLayoutResult(text),
                    matrix,
                    innerTextFieldBounds = Rect(1f, 2f, 3f, 4f),
                    decorationBoxBounds = Rect(5f, 6f, 7f, 8f)
                )

        Truth.assertThat(cursorAnchorInfo.editorBoundsInfo?.editorBounds)
            .isEqualTo(RectF(5f, 6f, 7f, 8f))
        Truth.assertThat(cursorAnchorInfo.editorBoundsInfo?.handwritingBounds)
            .isEqualTo(RectF(5f, 6f, 7f, 8f))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testEditorBoundsNotIncludedWhenIncludeEditorBoundsFalse() {
        val text = ""
        val selection = TextRange(0)
        val composition: TextRange? = null

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    getTextLayoutResult(text),
                    matrix,
                    innerTextFieldBounds = Rect(1f, 2f, 3f, 4f),
                    decorationBoxBounds = Rect(5f, 6f, 7f, 8f),
                    includeEditorBounds = false
                )

        Truth.assertThat(cursorAnchorInfo.editorBoundsInfo).isNull()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testLineBounds() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // 6 lines of text
        val text = "a\nbb\nccc\ndddd\neeeee\nffffff"
        val selection = TextRange(0)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)
        // Lines 2, 3, 4 are visible
        val innerTextFieldBounds =
            Rect(
                0f,
                textLayoutResult.getLineTop(2) + 1f,
                fontSizeInPx,
                textLayoutResult.getLineBottom(4) - 1f
            )

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        Truth.assertThat(cursorAnchorInfo.visibleLineBounds.size).isEqualTo(3)
        // Line 2 "ccc" has 3 characters
        Truth.assertThat(cursorAnchorInfo.visibleLineBounds[0])
            .isEqualTo(
                RectF(
                    0f,
                    textLayoutResult.getLineTop(2),
                    3 * fontSizeInPx,
                    textLayoutResult.getLineBottom(2)
                )
            )
        // Line 3 "dddd" has 4 characters
        Truth.assertThat(cursorAnchorInfo.visibleLineBounds[1])
            .isEqualTo(
                RectF(
                    0f,
                    textLayoutResult.getLineTop(3),
                    4 * fontSizeInPx,
                    textLayoutResult.getLineBottom(3)
                )
            )
        // Line 4 "eeeee" has 5 characters
        Truth.assertThat(cursorAnchorInfo.visibleLineBounds[2])
            .isEqualTo(
                RectF(
                    0f,
                    textLayoutResult.getLineTop(4),
                    5 * fontSizeInPx,
                    textLayoutResult.getLineBottom(4)
                )
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testLineBoundsNotIncludedWhenIncludeLineBoundsFalse() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // 6 lines of text
        val text = "a\nbb\nccc\ndddd\neeeee\nffffff"
        val selection = TextRange(0)
        val composition: TextRange? = null
        val textLayoutResult = getTextLayoutResult(text, fontSize = fontSize)
        // Lines 2, 3, 4 are visible
        val innerTextFieldBounds =
            Rect(
                0f,
                textLayoutResult.getLineTop(2) + 1f,
                fontSizeInPx,
                textLayoutResult.getLineBottom(4) - 1f
            )

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    text,
                    selection,
                    composition,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds,
                    includeLineBounds = false
                )

        Truth.assertThat(cursorAnchorInfo.visibleLineBounds.size).isEqualTo(0)
    }

    private fun getTextLayoutResult(
        text: String,
        fontSize: TextUnit = 12.sp,
        width: Float = with(defaultDensity) { 1000.dp.toPx() }
    ): TextLayoutResult {
        val intWidth = ceil(width).toInt()

        val fontFamilyResolver = createFontFamilyResolver(context)

        val input =
            TextLayoutInput(
                text = AnnotatedString(text),
                style = TextStyle(fontFamily = fontFamilyMeasureFont, fontSize = fontSize),
                placeholders = listOf(),
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible,
                density = defaultDensity,
                layoutDirection = LayoutDirection.Ltr,
                fontFamilyResolver = fontFamilyResolver,
                constraints = Constraints(maxWidth = intWidth)
            )

        val paragraph =
            MultiParagraph(
                annotatedString = input.text,
                style = input.style,
                constraints = Constraints(maxWidth = ceil(width).toInt()),
                density = input.density,
                fontFamilyResolver = fontFamilyResolver,
                overflow = TextOverflow.Clip
            )

        return TextLayoutResult(input, paragraph, IntSize(intWidth, ceil(paragraph.height).toInt()))
    }
}

private fun CursorAnchorInfo.Builder.build(
    text: CharSequence,
    selection: TextRange,
    composition: TextRange?,
    textLayoutResult: TextLayoutResult,
    matrix: Matrix,
    includeInsertionMarker: Boolean = true,
    includeCharacterBounds: Boolean = true,
    includeEditorBounds: Boolean = true,
    includeLineBounds: Boolean = true
): CursorAnchorInfo {
    val innerTextFieldBounds =
        Rect(0f, 0f, textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat())
    return build(
        text,
        selection,
        composition,
        textLayoutResult,
        matrix,
        innerTextFieldBounds = innerTextFieldBounds,
        decorationBoxBounds = innerTextFieldBounds,
        includeInsertionMarker = includeInsertionMarker,
        includeCharacterBounds = includeCharacterBounds,
        includeEditorBounds = includeEditorBounds,
        includeLineBounds = includeLineBounds
    )
}
