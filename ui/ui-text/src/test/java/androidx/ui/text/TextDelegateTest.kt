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

import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.painting.Canvas
import androidx.ui.text.font.Font
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
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

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
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        assertThat(textDelegate.text).isEqualTo(text)
    }

    @Test
    fun `constructor with customized maxLines`() {
        val maxLines = 8

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            maxLines = maxLines,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        assertThat(textDelegate.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor with customized overflow`() {
        val overflow = TextOverflow.Ellipsis

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            overflow = overflow,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        assertThat(textDelegate.overflow).isEqualTo(overflow)
    }

    @Test
    fun `constructor with customized locale`() {
        val locale = Locale("en", "US")

        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            locale = locale,
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr
        )

        assertThat(textDelegate.locale).isEqualTo(locale)
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
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr)

        textDelegate.minIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr)

        textDelegate.maxIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `width without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr)

        textDelegate.width
    }

    @Test(expected = AssertionError::class)
    fun `height without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr)

        textDelegate.height
    }

    @Test(expected = AssertionError::class)
    fun `paint without layout assertion should fail`() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = ""),
            density = density,
            resourceLoader = resourceLoader,
            layoutDirection = LayoutDirection.Ltr)
        val canvas = mock<Canvas>()

        textDelegate.paint(canvas)
    }
}
