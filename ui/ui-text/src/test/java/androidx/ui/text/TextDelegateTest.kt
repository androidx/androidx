/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text

import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.painting.Canvas
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDirection
import androidx.ui.text.style.TextOverflow
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

@RunWith(JUnit4::class)
class TextDelegateTest() {
    private val density = Density(density = 1f)
    private val resourceLoader = mock<Font.ResourceLoader>()

    @Test
    fun `constructor with default values`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        assertThat(textDelegate.text).isNull()
        assertThat(textDelegate.textAlign).isEqualTo(TextAlign.Start)
        assertThat(textDelegate.textDirection).isEqualTo(TextDirection.Ltr)
        assertThat(textDelegate.maxLines).isNull()
        assertThat(textDelegate.overflow).isEqualTo(TextOverflow.Clip)
        assertThat(textDelegate.locale).isNull()
    }

    @Test
    fun `constructor with customized text(TextSpan)`() {
        val text = AnnotatedString("Hello")
        val textDelegate = TextDelegate(
            text = text,
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.text).isEqualTo(text)
    }

    @Test
    fun `constructor with customized textAlign`() {
        val textDelegate = TextDelegate(
            paragraphStyle = ParagraphStyle(textAlign = TextAlign.Left),
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.textAlign).isEqualTo(TextAlign.Left)
    }

    @Test
    fun `constructor with customized textDirection`() {
        val textDelegate = TextDelegate(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl),
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun `constructor with customized maxLines`() {
        val maxLines = 8

        val textDelegate = TextDelegate(
            maxLines = maxLines,
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor with customized overflow`() {
        val overflow = TextOverflow.Ellipsis

        val textDelegate = TextDelegate(
            overflow = overflow,
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.overflow).isEqualTo(overflow)
    }

    @Test
    fun `constructor with customized locale`() {
        val locale = Locale("en", "US")

        val textDelegate = TextDelegate(
            locale = locale,
            density = density,
            resourceLoader = resourceLoader
        )

        assertThat(textDelegate.locale).isEqualTo(locale)
    }

    @Test
    fun `createParagraphStyle without TextStyle in AnnotatedText`() {
        val maxLines = 5
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")
        val text = AnnotatedString(text = "Hello")
        val textDelegate = TextDelegate(
            text = text,
            paragraphStyle = ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            maxLines = maxLines,
            overflow = overflow,
            locale = locale,
            density = density,
            resourceLoader = resourceLoader
        )

        val paragraphStyle = textDelegate.createParagraphStyle()

        assertThat(paragraphStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraphStyle.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun `applyFloatingPointHack with value is integer toDouble`() {
        assertThat(applyFloatingPointHack(2f)).isEqualTo(2.0f)
    }

    @Test
    fun `applyFloatingPointHack with value smaller than half`() {
        assertThat(applyFloatingPointHack(2.2f)).isEqualTo(3.0f)
    }

    @Test
    fun `applyFloatingPointHack with value larger than half`() {
        assertThat(applyFloatingPointHack(2.8f)).isEqualTo(3.0f)
    }

    @Test(expected = AssertionError::class)
    fun `minIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        textDelegate.minIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        textDelegate.maxIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `width without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        textDelegate.width
    }

    @Test(expected = AssertionError::class)
    fun `height without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        textDelegate.height
    }

    @Test(expected = AssertionError::class)
    fun `size without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)

        textDelegate.size
    }

    @Test(expected = AssertionError::class)
    fun `layout without text assertion should fail`() {
        val textDelegate = TextDelegate(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr),
            density = density,
            resourceLoader = resourceLoader
        )

        textDelegate.layout(Constraints())
    }

    @Test(expected = AssertionError::class)
    fun `paint without layout assertion should fail`() {
        val textDelegate = TextDelegate(density = density, resourceLoader = resourceLoader)
        val canvas = mock<Canvas>()

        textDelegate.paint(canvas)
    }
}
