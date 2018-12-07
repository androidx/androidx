/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.basic

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.RenderParagraph
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RichTextTest {
    @Test
    fun `RichText constructor with text only`() {
        val key = Key.createKey("Hello")
        val textSpan = TextSpan(text = "Hello", style = TextStyle())

        val richText = RichText(key = key, text = textSpan)

        assertThat(richText.key).isSameAs(key)
        assertThat(richText.text).isSameAs(textSpan)
        assertThat(richText.textAlign).isEqualTo(TextAlign.START)
        assertThat(richText.textDirection).isNull()
        assertThat(richText.softWrap).isTrue()
        assertThat(richText.overflow).isEqualTo(TextOverflow.CLIP)
        assertThat(richText.textScaleFactor).isEqualTo(1.0)
        assertThat(richText.maxLines).isNull()
    }

    @Test
    fun `RichText constructor with all customized values`() {
        val key = Key.createKey("Hello")
        val textSpan = TextSpan(text = "Hello", style = TextStyle())
        val textScaleFactor = 3.0
        val maxLines = 5

        val richText = RichText(
            key = key,
            text = textSpan,
            textAlign = TextAlign.CENTER,
            textDirection = TextDirection.LTR,
            softWrap = false,
            overflow = TextOverflow.ELLIPSIS,
            textScaleFactor = textScaleFactor,
            maxLines = 5
        )

        assertThat(richText.key).isSameAs(key)
        assertThat(richText.text).isSameAs(textSpan)
        assertThat(richText.textAlign).isEqualTo(TextAlign.CENTER)
        assertThat(richText.textDirection).isEqualTo(TextDirection.LTR)
        assertThat(richText.softWrap).isFalse()
        assertThat(richText.overflow).isEqualTo(TextOverflow.ELLIPSIS)
        assertThat(richText.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(richText.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `RichText createRenderObject`() {
        val key = Key.createKey("Hello")
        val textSpan = TextSpan(text = "Hello", style = TextStyle())
        val textScaleFactor = 3.0
        val maxLines = 5
        val richText = RichText(
            key = key,
            text = textSpan,
            textAlign = TextAlign.CENTER,
            textDirection = TextDirection.LTR,
            softWrap = false,
            overflow = TextOverflow.ELLIPSIS,
            textScaleFactor = textScaleFactor,
            maxLines = 5
        )

        val paragraph = richText.createRenderObject(richText.createElement())

        assertThat(paragraph.text).isSameAs(textSpan)
        assertThat(paragraph.textAlign).isEqualTo(TextAlign.CENTER)
        assertThat(paragraph.textDirection).isEqualTo(TextDirection.LTR)
        assertThat(paragraph.softWrap).isFalse()
        assertThat(paragraph.overflow).isEqualTo(TextOverflow.ELLIPSIS)
        assertThat(paragraph.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(paragraph.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `RichText updateRenderObject`() {
        val key = Key.createKey("Hello")
        val textSpan = TextSpan(text = "Hello", style = TextStyle())
        val textScaleFactor = 3.0
        val maxLines = 5
        val richText = RichText(
            key = key,
            text = textSpan,
            textAlign = TextAlign.CENTER,
            textDirection = TextDirection.LTR,
            softWrap = false,
            overflow = TextOverflow.ELLIPSIS,
            textScaleFactor = textScaleFactor,
            maxLines = 5
        )
        val paragraph = RenderParagraph(text = TextSpan(), textDirection = TextDirection.RTL)

        richText.updateRenderObject(richText.createElement(), paragraph)

        assertThat(paragraph.text).isSameAs(textSpan)
        assertThat(paragraph.textAlign).isEqualTo(TextAlign.CENTER)
        assertThat(paragraph.textDirection).isEqualTo(TextDirection.LTR)
        assertThat(paragraph.softWrap).isFalse()
        assertThat(paragraph.overflow).isEqualTo(TextOverflow.ELLIPSIS)
        assertThat(paragraph.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(paragraph.maxLines).isEqualTo(maxLines)
    }
}
