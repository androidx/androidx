/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.input.InputState
import androidx.ui.input.OffsetMap
import androidx.ui.text.AnnotatedString
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.TextPainter
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.font.Font
import androidx.ui.text.font.ResourceFont
import androidx.ui.unit.Density
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class TextFieldDelegateIntegrationTest {
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)

    private class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
        override fun load(font: Font): Typeface {
            return when (font) {
                is ResourceFont -> ResourcesCompat.getFont(context, font.resId)!!
                else -> throw IllegalArgumentException("Unknown font type: $font")
            }
        }
    }

    @Test
    fun draw_selection_test() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        val selection = TextRange(0, 1)
        val selectionColor = Color.Blue
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024.ipx))

        val expectedBitmap = layoutResult.toBitmap()
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))
        textDelegate.paintBackground(
            0,
            1,
            selectionColor,
            expectedCanvas,
            layoutResult
        )
        TextPainter.paint(expectedCanvas, layoutResult)

        val actualBitmap = layoutResult.toBitmap()
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))
        TextFieldDelegate.draw(
            canvas = actualCanvas,
            value = InputState(text = "Hello, World", selection = selection),
            selectionColor = selectionColor,
            hasFocus = true,
            offsetMap = OffsetMap.identityOffsetMap,
            textLayoutResult = layoutResult
        )

        assertThat(actualBitmap.sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun draw_cursor_test() {
        val cursor = TextRange(1, 1)

        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024.ipx))

        val expectedBitmap = layoutResult.toBitmap()
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))

        val cursorRect = layoutResult.getCursorRect(cursor.min)
        expectedCanvas.drawRect(cursorRect, Paint().apply { this.color = Color.Black })
        TextPainter.paint(expectedCanvas, layoutResult)

        val actualBitmap = layoutResult.toBitmap()
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))
        TextFieldDelegate.draw(
            canvas = actualCanvas,
            value = InputState(text = "Hello, World", selection = cursor),
            selectionColor = Color.Black,
            hasFocus = true,
            offsetMap = OffsetMap.identityOffsetMap,
            textLayoutResult = layoutResult
        )

        assertThat(actualBitmap.sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun dont_draw_cursor_test() {
        val cursor = TextRange(1, 1)

        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024.ipx))

        val expectedBitmap = layoutResult.toBitmap()
        val expectedCanvas = Canvas(android.graphics.Canvas(expectedBitmap))

        TextPainter.paint(expectedCanvas, layoutResult)

        val actualBitmap = layoutResult.toBitmap()
        val actualCanvas = Canvas(android.graphics.Canvas(actualBitmap))
        TextFieldDelegate.draw(
            canvas = actualCanvas,
            value = InputState(text = "Hello, World", selection = cursor),
            selectionColor = Color.Black,
            hasFocus = false,
            offsetMap = OffsetMap.identityOffsetMap,
            textLayoutResult = layoutResult
        )

        assertThat(actualBitmap.sameAs(expectedBitmap)).isTrue()
    }

    @Test
    fun layout_height_constraint_max_height() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024.ipx))
        val requestHeight = layoutResult.size.height / 2

        val (_, height, _) = TextFieldDelegate.layout(
            textDelegate,
            Constraints.fixedHeight(requestHeight)
        )

        assertThat(height).isEqualTo(requestHeight)
    }

    @Test
    fun layout_height_constraint_min_height() {
        val textDelegate = TextDelegate(
            text = AnnotatedString("Hello, World"),
            style = TextStyle.Default,
            maxLines = 2,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )
        val layoutResult = textDelegate.layout(Constraints.fixedWidth(1024.ipx))
        val requestHeight = layoutResult.size.height * 2

        val (_, height, _) = TextFieldDelegate.layout(
            textDelegate,
            Constraints.fixedHeight(requestHeight)
        )

        assertThat(height).isEqualTo(requestHeight)
    }
}

private fun TextLayoutResult.toBitmap() = Bitmap.createBitmap(
    size.width.value,
    size.height.value,
    Bitmap.Config.ARGB_8888
)