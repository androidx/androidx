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
package androidx.ui.engine.text

import androidx.ui.engine.window.Locale
import androidx.ui.graphics.Color
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphBuilderTest {

    @Test
    fun `build with default values`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = ParagraphBuilder(paragraphStyle).build()

        assertThat(paragraph, not(nullValue()))
        assertThat(paragraph.paragraphStyle, equalTo(paragraphStyle))

        // or should copy
        assertThat(paragraph.text.toString(), equalTo(""))
        assertThat(paragraph.textStyles.size, equalTo(0))
    }

    @Test
    fun `addText`() {
        val paragraphStyle = createParagraphStyle()
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.addText("Test")

        val paragraph = paragraphBuilder.build()
        assertThat(paragraph.text.toString(), equalTo("Test"))
        assertThat(paragraph.textStyles.size, equalTo(0))
    }

    @Test
    fun `addText multiple calls`() {
        val paragraphStyle = createParagraphStyle()
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.addText("Test")
        paragraphBuilder.addText(" me ")
        paragraphBuilder.addText("now")

        val paragraph = paragraphBuilder.build()
        assertThat(paragraph.text.toString(), equalTo("Test me now"))
        assertThat(paragraph.textStyles.size, equalTo(0))
    }

    @Test
    fun `pushStyle`() {
        val paragraphStyle = createParagraphStyle()
        val textStyle = TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4))
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.pushStyle(textStyle)
        paragraphBuilder.addText("Test")
        paragraphBuilder.pop()
        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.text.toString(), equalTo("Test"))
        assertThat(paragraph.textStyles.size, equalTo(1))
        assertThat(paragraph.textStyles[0].textStyle, equalTo(textStyle))
        assertThat(paragraph.textStyles[0].start, equalTo(0))
        assertThat(paragraph.textStyles[0].end, equalTo(paragraph.text.length))
    }

    @Test
    fun `pushStyle without pop`() {
        val paragraphStyle = createParagraphStyle()
        val styles = arrayOf(
            TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4)),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.bold)
        )
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        styles.forEachIndexed { index, textStyle ->
            paragraphBuilder.pushStyle(textStyle)
            paragraphBuilder.addText("Style" + index)
        }
        // pop is intentionally not called here
        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.text.toString(), equalTo("Style0Style1Style2"))
        assertThat(paragraph.textStyles.size, equalTo(3))

        styles.forEachIndexed { index, textStyle ->
            assertThat(paragraph.textStyles[index].textStyle, equalTo(textStyle))
            assertThat(paragraph.textStyles[index].end, equalTo(paragraph.text.length))
        }

        assertThat(paragraph.textStyles[0].start, equalTo(0))
        assertThat(paragraph.textStyles[1].start, equalTo("Style0".length))
        assertThat(paragraph.textStyles[2].start, equalTo("Style0Style1".length))
    }

    @Test
    fun `pushStyle with mutiple styles`() {
        val paragraphStyle = createParagraphStyle()
        val textStyle1 = TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4))
        val textStyle2 =
            TextStyle(fontStyle = FontStyle.Italic)
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.pushStyle(textStyle1)
        paragraphBuilder.addText("Test")
        paragraphBuilder.pushStyle(textStyle2)
        paragraphBuilder.addText(" me")
        paragraphBuilder.pop()
        paragraphBuilder.pop()
        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.text.toString(), equalTo("Test me"))
        assertThat(paragraph.textStyles.size, equalTo(2))

        assertThat(paragraph.textStyles[0].textStyle, equalTo(textStyle1))
        assertThat(paragraph.textStyles[0].start, equalTo(0))
        assertThat(paragraph.textStyles[0].end, equalTo(paragraph.text.length))

        assertThat(paragraph.textStyles[1].textStyle, equalTo(textStyle2))
        assertThat(paragraph.textStyles[1].start, equalTo("Test".length))
        assertThat(paragraph.textStyles[1].end, equalTo(paragraph.text.length))
    }

    @Test
    fun `pushStyle with mutiple styles on top of each other`() {
        val paragraphStyle = createParagraphStyle()
        val styles = arrayOf(
            TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4)),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.bold)
        )
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        styles.forEach { textStyle ->
            paragraphBuilder.pushStyle(textStyle)
        }
        // pop is intentionally not called here
        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.text.toString(), equalTo(""))
        assertThat(paragraph.textStyles.size, equalTo(3))
        styles.forEachIndexed { index, textStyle ->
            assertThat(paragraph.textStyles[index].textStyle, equalTo(textStyle))
            assertThat(paragraph.textStyles[index].start, equalTo(paragraph.text.length))
            assertThat(paragraph.textStyles[index].end, equalTo(paragraph.text.length))
        }
    }

    @Test
    fun `pushStyle with multiple stacks should construct styles in the same order`() {
        val paragraphStyle = createParagraphStyle()
        val styles = arrayOf(
            TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4)),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.bold),
            TextStyle(letterSpacing = 1.0f)
        )

        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.pushStyle(styles[0])
        paragraphBuilder.addText("layer1-1")
        paragraphBuilder.pushStyle(styles[1])
        paragraphBuilder.addText("layer2-1")
        paragraphBuilder.pushStyle(styles[2])
        paragraphBuilder.addText("layer3-1")
        paragraphBuilder.pop()
        paragraphBuilder.pushStyle(styles[3])
        paragraphBuilder.addText("layer3-2")
        paragraphBuilder.pop()
        paragraphBuilder.addText("layer2-2")
        paragraphBuilder.pop()
        paragraphBuilder.addText("layer1-2")

        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.textStyles.size, equalTo(4))
        styles.forEachIndexed { index, textStyle ->
            assertThat(paragraph.textStyles[index].textStyle, equalTo(textStyle))
        }
    }

    @Test
    fun `pushStyle with multiple nested styles should return styles in same order`() {
        val paragraphStyle = createParagraphStyle()
        val styles = arrayOf(
            TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4)),
            TextStyle(fontStyle = FontStyle.Italic),
            TextStyle(fontWeight = FontWeight.bold),
            TextStyle(letterSpacing = 1.0f)
        )

        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.pushStyle(styles[0])
        paragraphBuilder.addText("layer1-1")
        paragraphBuilder.pushStyle(styles[1])
        paragraphBuilder.addText("layer2-1")
        paragraphBuilder.pop()
        paragraphBuilder.pushStyle(styles[2])
        paragraphBuilder.addText("layer2-2")
        paragraphBuilder.pushStyle(styles[3])
        paragraphBuilder.addText("layer3-1")
        paragraphBuilder.pop()
        paragraphBuilder.addText("layer2-3")
        paragraphBuilder.pop()
        paragraphBuilder.addText("layer1-2")
        paragraphBuilder.pop()

        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.textStyles.size, equalTo(4))
        styles.forEachIndexed { index, textStyle ->
            assertThat(paragraph.textStyles[index].textStyle, equalTo(textStyle))
        }
    }

    @Test
    fun `pop when empty does not throw exception`() {
        val paragraphStyle = createParagraphStyle()
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.pop()
        paragraphBuilder.pop()
        val paragraph = paragraphBuilder.build()
        assertThat(paragraph.text.toString(), equalTo(""))
        assertThat(paragraph.textStyles.size, equalTo(0))
    }

    @Test
    fun `pop in the middle`() {
        val paragraphStyle = createParagraphStyle()
        val textStyle1 = TextStyle(color = Color(alpha = 1, red = 2, green = 3, blue = 4))
        val textStyle2 =
            TextStyle(fontStyle = FontStyle.Italic)
        val paragraphBuilder = ParagraphBuilder(paragraphStyle)
        paragraphBuilder.addText("Style0")
        paragraphBuilder.pushStyle(textStyle1)
        paragraphBuilder.addText("Style1")
        paragraphBuilder.pop()
        paragraphBuilder.pushStyle(textStyle2)
        paragraphBuilder.addText("Style2")
        paragraphBuilder.pop()
        paragraphBuilder.addText("Style3")

        val paragraph = paragraphBuilder.build()

        assertThat(paragraph.text.toString(), equalTo("Style0Style1Style2Style3"))
        assertThat(paragraph.textStyles.size, equalTo(2))

        // the order is first applied is in the second
        assertThat(paragraph.textStyles[0].textStyle, equalTo(textStyle1))
        assertThat(paragraph.textStyles[0].start, equalTo("Style0".length))
        assertThat(paragraph.textStyles[0].end, equalTo("Style0Style1".length))

        assertThat(paragraph.textStyles[1].textStyle, equalTo(textStyle2))
        assertThat(paragraph.textStyles[1].start, equalTo("Style0Style1".length))
        assertThat(paragraph.textStyles[1].end, equalTo("Style0Style1Style2".length))
    }

    private fun createParagraphStyle(): ParagraphStyle {
        val textAlign = TextAlign.End
        val textDirection = TextDirection.Rtl
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.Italic
        val maxLines = 2
        val fontSize = 1.0f
        val lineHeight = 2.0f
        val ellipsis = false
        val locale = Locale("en")

        return ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale
        )
    }
}
