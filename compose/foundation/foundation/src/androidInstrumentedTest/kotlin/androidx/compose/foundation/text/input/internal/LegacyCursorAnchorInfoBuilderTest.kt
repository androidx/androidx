/*
 * Copyright 2023 The Android Open Source Project
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
import android.view.inputmethod.CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
import android.view.inputmethod.CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
import android.view.inputmethod.CursorAnchorInfo.FLAG_IS_RTL
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
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
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
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LegacyCursorAnchorInfoBuilderTest {

    private val fontFamilyMeasureFont =
        Font(resId = R.font.sample_font, weight = FontWeight.Normal, style = FontStyle.Normal)
            .toFontFamily()

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val matrix = Matrix()

    @Test
    fun testSelectionDefault() {
        val textFieldValue = TextFieldValue()

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(0)
    }

    @Test
    fun testSelectionCursor() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(2))

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(2)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testSelectionRange() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1, 2))

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.selectionStart).isEqualTo(1)
        assertThat(cursorAnchorInfo.selectionEnd).isEqualTo(2)
    }

    @Test
    fun testCompositionNone() {
        val textFieldValue = TextFieldValue(composition = null)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testCompositionCoveringAllString() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(0)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
    }

    @Test
    fun testCompositionCoveringPortionOfString() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val textFieldValue =
            TextFieldValue(
                word1 + word2 + word3,
                composition = TextRange(word1.length, (word1 + word2).length)
            )

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(word1.length)
        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(word2)
    }

    @Test
    fun testCompositionNotIncludedWhenIncludeCharacterBoundsFalse() {
        val word1 = "123 "
        val word2 = "456"
        val word3 = " 789"
        val textFieldValue =
            TextFieldValue(
                word1 + word2 + word3,
                composition = TextRange(word1.length, (word1 + word2).length)
            )

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    getTextLayoutResult(textFieldValue.text),
                    matrix,
                    includeCharacterBounds = false
                )

        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(-1)
        assertThat(cursorAnchorInfo.composingText).isNull()
    }

    @Test
    fun testResetsBetweenExecutions() {
        val text = "abc"
        val textFieldValue = TextFieldValue(text, composition = TextRange(0, text.length))
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo =
            builder.build(textFieldValue, getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo.composingText.toString()).isEqualTo(text)
        assertThat(cursorAnchorInfo.composingTextStart).isEqualTo(textFieldValue.composition!!.min)

        val cursorAnchorInfo1 =
            builder.build(TextFieldValue("abcd"), getTextLayoutResult(textFieldValue.text), matrix)

        assertThat(cursorAnchorInfo1.composingText).isNull()
        assertThat(cursorAnchorInfo1.composingTextStart).isEqualTo(-1)
    }

    @Test
    fun testInsertionMarkerCursor() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(textFieldValue, textLayoutResult, matrix)

        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(FLAG_HAS_VISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerSelectionIsSameWithCursor() {
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1, 2))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text)
        val builder = CursorAnchorInfo.Builder()

        val cursorAnchorInfo1 = builder.build(textFieldValue, textLayoutResult, matrix)

        val cursorAnchorInfo2 =
            builder.build(textFieldValue.copy(selection = TextRange(1)), textLayoutResult, matrix)

        assertThat(cursorAnchorInfo1.insertionMarkerHorizontal)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerHorizontal)
        assertThat(cursorAnchorInfo1.insertionMarkerTop)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerTop)
        assertThat(cursorAnchorInfo1.insertionMarkerBottom)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerBottom)
        assertThat(cursorAnchorInfo1.insertionMarkerBaseline)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerBaseline)
        assertThat(cursorAnchorInfo1.insertionMarkerFlags)
            .isEqualTo(cursorAnchorInfo2.insertionMarkerFlags)
    }

    @Test
    fun testInsertionMarkerCursorClamped() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textFieldValue = TextFieldValue("abc   ", selection = TextRange(5))
        val width = 4 * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(textFieldValue, textLayoutResult, matrix)

        // The cursor position is clamped to the width of the layout.
        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(width)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(FLAG_HAS_VISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerRtl() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }

        val textFieldValue = TextFieldValue("\u05D0\u05D1\u05D2", selection = TextRange(0))
        val width = 3 * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(textFieldValue, textLayoutResult, matrix)

        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(width)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(FLAG_HAS_VISIBLE_REGION or FLAG_IS_RTL)
    }

    @Test
    fun testInsertionMarkerWithVisualTransformation() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abcde", selection = TextRange(2))
        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) =
                    if (offset < 2) offset else offset + 3

                override fun transformedToOriginal(offset: Int) = throw NotImplementedError()
            }
        val textLayoutResult = getTextLayoutResult("ab---cde", fontSize = fontSize)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, textLayoutResult, matrix, offsetMapping = offsetMapping)

        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(5 * fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(FLAG_HAS_VISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerNotVisible() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // insertionMarkerHorizontal = fontSizeInPx, so the insertion marker is completely outside
        // this rectangle.
        val innerTextFieldBounds =
            Rect(0f, 0f, fontSizeInPx - 1f, textLayoutResult.size.height.toFloat())

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    OffsetMapping.Identity,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(FLAG_HAS_INVISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerPartiallyVisible() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // insertionMarkerTop = 0 and insertionMarkerBottom = fontSizeInPx, so this rectangle covers
        // the top of the insertion marker but not the bottom.
        val innerTextFieldBounds = Rect(fontSizeInPx - 1f, 0f, fontSizeInPx + 1f, fontSizeInPx - 1f)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    OffsetMapping.Identity,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerTop).isEqualTo(0f)
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isEqualTo(fontSizeInPx)
        assertThat(cursorAnchorInfo.insertionMarkerFlags)
            .isEqualTo(FLAG_HAS_VISIBLE_REGION or FLAG_HAS_INVISIBLE_REGION)
    }

    @Test
    fun testInsertionMarkerNotIncludedWhenIncludeInsertionMarkerFalse() {
        val fontSize = 10.sp
        val textFieldValue = TextFieldValue("abc", selection = TextRange(1))
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, textLayoutResult, matrix, includeInsertionMarker = false)

        assertThat(cursorAnchorInfo.insertionMarkerHorizontal).isNaN()
        assertThat(cursorAnchorInfo.insertionMarkerTop).isNaN()
        assertThat(cursorAnchorInfo.insertionMarkerBottom).isNaN()
        assertThat(cursorAnchorInfo.insertionMarkerBaseline).isNaN()
        assertThat(cursorAnchorInfo.insertionMarkerFlags).isEqualTo(0)
    }

    @Test
    fun testCharacterBoundsLtr() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val textFieldValue = TextFieldValue(text, composition = composition)
        val width = text.length * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(textFieldValue, textLayoutResult, matrix)

        for (index in text.indices) {
            if (index in composition) {
                assertThat(cursorAnchorInfo.getCharacterBounds(index))
                    .isEqualTo(
                        RectF(index * fontSizeInPx, 0f, (index + 1) * fontSizeInPx, fontSizeInPx)
                    )
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index))
                    .isEqualTo(FLAG_HAS_VISIBLE_REGION)
            } else {
                assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
            }
        }
    }

    @Test
    fun testCharacterBoundsRtl() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "\u05D0 \u05D1\u05D2 \u05D3"
        // Composition is on "\u05D1\u05D2"
        val composition = TextRange(2, 4)
        val textFieldValue = TextFieldValue(text, composition = composition)
        val width = text.length * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder().build(textFieldValue, textLayoutResult, matrix)

        for (index in text.indices) {
            if (index in composition) {
                assertThat(cursorAnchorInfo.getCharacterBounds(index))
                    .isEqualTo(
                        RectF(
                            width - ((index + 1) * fontSizeInPx),
                            0f,
                            width - (index * fontSizeInPx),
                            fontSizeInPx
                        )
                    )
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index))
                    .isEqualTo(FLAG_HAS_VISIBLE_REGION or FLAG_IS_RTL)
            } else {
                assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
            }
        }
    }

    @Test
    fun testCharacterBoundsWithVisualTransformation() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "abcd"
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = 2 * offset

                override fun transformedToOriginal(offset: Int) = throw NotImplementedError()
            }
        val transformedText = "a-b-c-d-"
        val textFieldValue = TextFieldValue(text, composition = composition)
        val width = transformedText.length * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(transformedText, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, textLayoutResult, matrix, offsetMapping = offsetMapping)

        for (index in text.indices) {
            if (index in composition) {
                assertThat(cursorAnchorInfo.getCharacterBounds(index))
                    .isEqualTo(
                        RectF(
                            2 * index * fontSizeInPx,
                            0f,
                            (2 * index + 1) * fontSizeInPx,
                            fontSizeInPx
                        )
                    )
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index))
                    .isEqualTo(FLAG_HAS_VISIBLE_REGION)
            } else {
                assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
                assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
            }
        }
    }

    @Test
    fun testCharacterBoundsVisibility() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val textFieldValue = TextFieldValue(text, composition = composition)
        val width = text.length * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)
        val innerTextFieldBounds = Rect(3.5f * fontSizeInPx, 0f, 4f * fontSizeInPx, fontSizeInPx)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    OffsetMapping.Identity,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        // Character at index 2 spans horizontal range [2 * fontSizeInPx, 3 * fontSizeInPx], so it
        // is completely outside innerTextFieldBounds.
        assertThat(cursorAnchorInfo.getCharacterBoundsFlags(2)).isEqualTo(FLAG_HAS_INVISIBLE_REGION)
        // Character at index 3 spans horizontal range [3 * fontSizeInPx, 4 * fontSizeInPx], so it
        // is partially inside innerTextFieldBounds.
        assertThat(cursorAnchorInfo.getCharacterBoundsFlags(3))
            .isEqualTo(FLAG_HAS_VISIBLE_REGION or FLAG_HAS_INVISIBLE_REGION)
    }

    @Test
    fun testCharacterBoundsNotIncludedWhenIncludeCharacterBoundsFalse() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        val text = "a bc d"
        // Composition is on "bc"
        val composition = TextRange(2, 4)
        val textFieldValue = TextFieldValue(text, composition = composition)
        val width = text.length * fontSizeInPx
        val textLayoutResult =
            getTextLayoutResult(textFieldValue.text, fontSize = fontSize, width = width)

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(textFieldValue, textLayoutResult, matrix, includeCharacterBounds = false)

        for (index in text.indices) {
            assertThat(cursorAnchorInfo.getCharacterBounds(index)).isNull()
            assertThat(cursorAnchorInfo.getCharacterBoundsFlags(index)).isEqualTo(0)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testEditorBounds() {
        val textFieldValue = TextFieldValue()

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    OffsetMapping.Identity,
                    getTextLayoutResult(textFieldValue.text),
                    matrix,
                    innerTextFieldBounds = Rect(1f, 2f, 3f, 4f),
                    decorationBoxBounds = Rect(5f, 6f, 7f, 8f)
                )

        assertThat(cursorAnchorInfo.editorBoundsInfo?.editorBounds).isEqualTo(RectF(5f, 6f, 7f, 8f))
        assertThat(cursorAnchorInfo.editorBoundsInfo?.handwritingBounds)
            .isEqualTo(RectF(5f, 6f, 7f, 8f))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun testEditorBoundsNotIncludedWhenIncludeEditorBoundsFalse() {
        val textFieldValue = TextFieldValue()

        val cursorAnchorInfo =
            CursorAnchorInfo.Builder()
                .build(
                    textFieldValue,
                    OffsetMapping.Identity,
                    getTextLayoutResult(textFieldValue.text),
                    matrix,
                    innerTextFieldBounds = Rect(1f, 2f, 3f, 4f),
                    decorationBoxBounds = Rect(5f, 6f, 7f, 8f),
                    includeEditorBounds = false
                )

        assertThat(cursorAnchorInfo.editorBoundsInfo).isNull()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testLineBounds() {
        val fontSize = 10.sp
        val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
        // 6 lines of text
        val textFieldValue = TextFieldValue("a\nbb\nccc\ndddd\neeeee\nffffff")
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)
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
                    textFieldValue,
                    OffsetMapping.Identity,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds
                )

        assertThat(cursorAnchorInfo.visibleLineBounds.size).isEqualTo(3)
        // Line 2 "ccc" has 3 characters
        assertThat(cursorAnchorInfo.visibleLineBounds[0])
            .isEqualTo(
                RectF(
                    0f,
                    textLayoutResult.getLineTop(2),
                    3 * fontSizeInPx,
                    textLayoutResult.getLineBottom(2)
                )
            )
        // Line 3 "dddd" has 4 characters
        assertThat(cursorAnchorInfo.visibleLineBounds[1])
            .isEqualTo(
                RectF(
                    0f,
                    textLayoutResult.getLineTop(3),
                    4 * fontSizeInPx,
                    textLayoutResult.getLineBottom(3)
                )
            )
        // Line 4 "eeeee" has 5 characters
        assertThat(cursorAnchorInfo.visibleLineBounds[2])
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
        val textFieldValue = TextFieldValue("a\nbb\nccc\ndddd\neeeee\nffffff")
        val textLayoutResult = getTextLayoutResult(textFieldValue.text, fontSize = fontSize)
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
                    textFieldValue,
                    OffsetMapping.Identity,
                    textLayoutResult,
                    matrix,
                    innerTextFieldBounds = innerTextFieldBounds,
                    decorationBoxBounds = innerTextFieldBounds,
                    includeLineBounds = false
                )

        assertThat(cursorAnchorInfo.visibleLineBounds.size).isEqualTo(0)
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
    textFieldValue: TextFieldValue,
    textLayoutResult: TextLayoutResult,
    matrix: Matrix,
    offsetMapping: OffsetMapping = OffsetMapping.Identity,
    includeInsertionMarker: Boolean = true,
    includeCharacterBounds: Boolean = true,
    includeEditorBounds: Boolean = true,
    includeLineBounds: Boolean = true
): CursorAnchorInfo {
    val innerTextFieldBounds =
        Rect(0f, 0f, textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat())
    return build(
        textFieldValue,
        offsetMapping,
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
