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
import androidx.ui.foundation.Key
import androidx.ui.painting.TextStyle
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DefaultTextStyleTest {
    @Test
    fun `primary constructor only with customized TextStyle`() {
        val textStyle = TextStyle()

        val defaultTextStyle = DefaultTextStyle(style = textStyle, child = null)

        assertThat(defaultTextStyle.key).isNull()
        assertThat(defaultTextStyle.style).isSameAs(textStyle)
        assertThat(defaultTextStyle.textAlign).isNull()
        assertThat(defaultTextStyle.softWrap).isTrue()
        assertThat(defaultTextStyle.overflow).isEqualTo(TextOverflow.CLIP)
        assertThat(defaultTextStyle.maxLines).isNull()
        assertThat(defaultTextStyle.child).isNull()
    }

    @Test
    fun `primary constructor with customized values`() {
        val key = Key.createKey("Hello")
        val textStyle = TextStyle()
        val maxLines = 5
        val childDefaultTextStyle = DefaultTextStyle(style = textStyle, child = null)

        val defaultTextStyle = DefaultTextStyle(
            key = key,
            style = textStyle,
            textAlign = TextAlign.CENTER,
            softWrap = false,
            overflow = TextOverflow.ELLIPSIS,
            maxLines = maxLines,
            child = childDefaultTextStyle
        )

        assertThat(defaultTextStyle.key).isSameAs(key)
        assertThat(defaultTextStyle.style).isSameAs(textStyle)
        assertThat(defaultTextStyle.textAlign).isEqualTo(TextAlign.CENTER)
        assertThat(defaultTextStyle.softWrap).isFalse()
        assertThat(defaultTextStyle.overflow).isEqualTo(TextOverflow.ELLIPSIS)
        assertThat(defaultTextStyle.maxLines).isEqualTo(maxLines)
        assertThat(defaultTextStyle.child).isSameAs(childDefaultTextStyle)
    }

    @Test
    fun `secondary constructor`() {
        val defaultTextStyle = DefaultTextStyle()

        assertThat(defaultTextStyle.key).isNull()
        assertThat(defaultTextStyle.style).isEqualTo(TextStyle())
        assertThat(defaultTextStyle.textAlign).isNull()
        assertThat(defaultTextStyle.softWrap).isTrue()
        assertThat(defaultTextStyle.overflow).isEqualTo(TextOverflow.CLIP)
        assertThat(defaultTextStyle.maxLines).isNull()
        assertThat(defaultTextStyle.child).isNull()
    }

    @Test
    fun `updateShouldNotify when nothing is updated should return false`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = oldDefaultTextStyle.style,
            textAlign = oldDefaultTextStyle.textAlign,
            softWrap = oldDefaultTextStyle.softWrap,
            overflow = oldDefaultTextStyle.overflow,
            maxLines = oldDefaultTextStyle.maxLines,
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isFalse()
    }

    @Test
    fun `updateShouldNotify when style is updated should return true`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = TextStyle(fontSize = 15.0), // style is different.
            textAlign = oldDefaultTextStyle.textAlign,
            softWrap = oldDefaultTextStyle.softWrap,
            overflow = oldDefaultTextStyle.overflow,
            maxLines = oldDefaultTextStyle.maxLines,
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isTrue()
    }

    @Test
    fun `updateShouldNotify when textAlign is updated should return true`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = oldDefaultTextStyle.style,
            textAlign = TextAlign.RIGHT, // textAlign is different.
            softWrap = oldDefaultTextStyle.softWrap,
            overflow = oldDefaultTextStyle.overflow,
            maxLines = oldDefaultTextStyle.maxLines,
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isTrue()
    }

    @Test
    fun `updateShouldNotify when softWrap is updated should return true`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = oldDefaultTextStyle.style,
            textAlign = oldDefaultTextStyle.textAlign,
            softWrap = !oldDefaultTextStyle.softWrap, // softWrap is different.
            overflow = oldDefaultTextStyle.overflow,
            maxLines = oldDefaultTextStyle.maxLines,
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isTrue()
    }

    @Test
    fun `updateShouldNotify when overflow is updated should return true`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = oldDefaultTextStyle.style,
            textAlign = oldDefaultTextStyle.textAlign,
            softWrap = oldDefaultTextStyle.softWrap,
            overflow = TextOverflow.FADE, // overflow is different.
            maxLines = oldDefaultTextStyle.maxLines,
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isTrue()
    }

    @Test
    fun `updateShouldNotify when maxLines is updated should return true`() {
        val oldDefaultTextStyle = DefaultTextStyle()
        val newDefaultTextStyle = DefaultTextStyle(
            key = oldDefaultTextStyle.key,
            style = oldDefaultTextStyle.style,
            textAlign = oldDefaultTextStyle.textAlign,
            softWrap = oldDefaultTextStyle.softWrap,
            overflow = oldDefaultTextStyle.overflow,
            maxLines = 3, // maxLines is different.
            child = oldDefaultTextStyle.child
        )

        assertThat(newDefaultTextStyle.updateShouldNotify(oldDefaultTextStyle)).isTrue()
    }
}
