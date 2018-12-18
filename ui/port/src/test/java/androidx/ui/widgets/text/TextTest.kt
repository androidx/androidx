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

package androidx.ui.widgets.text

import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.foundation.Key
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextTest {
    @Test
    fun `constructor containing string only with string`() {
        val string = "Hello"

        val text = Text(data = string)

        assertThat(text.data).isEqualTo(string)
        assertThat(text.textSpan).isNull()
        assertThat(text.key).isNull()
        assertThat(text.style).isNull()
        assertThat(text.textAlign).isNull()
        assertThat(text.textDirection).isNull()
        assertThat(text.softWrap).isNull()
        assertThat(text.overflow).isNull()
        assertThat(text.textScaleFactor).isNull()
        assertThat(text.maxLines).isNull()
    }

    @Test
    fun `constructor containing string with customized values`() {
        val string = "Hello"
        val key = Key.createKey("Hello")
        val style = TextStyle()
        val textScaleFactor = 5.0
        val maxLines = 5

        val text = Text(
            data = string,
            key = key,
            style = style,
            textAlign = TextAlign.RIGHT,
            textDirection = TextDirection.RTL,
            softWrap = true,
            overflow = TextOverflow.FADE,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines
        )

        assertThat(text.data).isEqualTo(string)
        assertThat(text.textSpan).isNull()
        assertThat(text.key).isSameAs(key)
        assertThat(text.style).isSameAs(style)
        assertThat(text.textAlign).isEqualTo(TextAlign.RIGHT)
        assertThat(text.textDirection).isEqualTo(TextDirection.RTL)
        assertThat(text.softWrap).isTrue()
        assertThat(text.overflow).isEqualTo(TextOverflow.FADE)
        assertThat(text.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(text.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor containing textSpan with only textSpan`() {
        val textSpan = TextSpan(text = "Hello")

        val text = Text(textSpan = textSpan)

        assertThat(text.data).isNull()
        assertThat(text.textSpan).isSameAs(textSpan)
        assertThat(text.key).isNull()
        assertThat(text.style).isNull()
        assertThat(text.textAlign).isNull()
        assertThat(text.textDirection).isNull()
        assertThat(text.softWrap).isNull()
        assertThat(text.overflow).isNull()
        assertThat(text.textScaleFactor).isNull()
        assertThat(text.maxLines).isNull()
    }

    @Test
    fun `constructor containing textSpan with customized values`() {
        val textSpan = TextSpan(text = "Hello")
        val key = Key.createKey("Hello")
        val style = TextStyle()
        val textScaleFactor = 5.0
        val maxLines = 5

        val text = Text(
            textSpan = textSpan,
            key = key,
            style = style,
            textAlign = TextAlign.RIGHT,
            textDirection = TextDirection.RTL,
            softWrap = true,
            overflow = TextOverflow.FADE,
            textScaleFactor = textScaleFactor,
            maxLines = maxLines
        )

        assertThat(text.data).isNull()
        assertThat(text.textSpan).isSameAs(textSpan)
        assertThat(text.key).isSameAs(key)
        assertThat(text.style).isSameAs(style)
        assertThat(text.textAlign).isEqualTo(TextAlign.RIGHT)
        assertThat(text.textDirection).isEqualTo(TextDirection.RTL)
        assertThat(text.softWrap).isTrue()
        assertThat(text.overflow).isEqualTo(TextOverflow.FADE)
        assertThat(text.textScaleFactor).isEqualTo(textScaleFactor)
        assertThat(text.maxLines).isEqualTo(maxLines)
    }
}