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

package androidx.ui.painting

import androidx.ui.graphics.Color
import androidx.ui.painting.basictypes.RenderComparison
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class TextSpanTest {
    @Test
    fun `constructor with default values`() {
        val textSpan = TextSpan()

        assertThat(textSpan.style).isNull()
        assertThat(textSpan.text).isNull()
        assertThat(textSpan.children).isEqualTo(mutableListOf<TextSpan>())
        // assertThat(textSpan.recognizer).isNull()
    }

    @Test
    fun `constructor with customized style`() {
        val textStyle = TextStyle(fontSize = 10.0f, letterSpacing = 1.5f)
        val textSpan = TextSpan(style = textStyle)

        assertThat(textSpan.style).isEqualTo(textStyle)
    }

    @Test
    fun `constructor with customized text`() {
        val string = "Hello"
        val textSpan = TextSpan(text = string)

        assertThat(textSpan.text).isEqualTo(string)
    }

    @Test
    fun `constructor with customized children`() {
        val string1 = "Hello"
        val string2 = "World"
        val textSpan1 = TextSpan(text = string1)
        val textSpan2 = TextSpan(text = string2)
        val textSpan = TextSpan(children = mutableListOf(textSpan1, textSpan2))

        assertThat(textSpan.children.size).isEqualTo(2)
        assertThat(textSpan.children.get(0).text).isEqualTo(string1)
        assertThat(textSpan.children.get(1).text).isEqualTo(string2)
    }

    @Test
    fun `visitTextSpan with neither text nor children should return true`() {
        val textSpan = TextSpan()

        val result = textSpan.visitTextSpan { _: TextSpan -> false }

        assertThat(result).isTrue()
    }

    @Test
    fun `visitTextSpan with text and visitor always returns true`() {
        val textSpan = TextSpan(text = "Hello")

        val result = textSpan.visitTextSpan { _: TextSpan -> true }

        assertThat(result).isTrue()
    }

    @Test
    fun `visitTextSpan with text and visitor always returns false`() {
        val textSpan = TextSpan(text = "Hello")

        val result = textSpan.visitTextSpan { _: TextSpan -> false }

        assertThat(result).isFalse()
    }

    @Test
    fun `visitTextSpan with children and visitor always returns true`() {
        val textSpan1 = spy(TextSpan(text = "Hello"))
        val textSpan2 = spy(TextSpan(text = "World"))
        val textSpan = spy(TextSpan(children = mutableListOf(textSpan1, textSpan2)))
        val returnTrueFunction = { _: TextSpan -> true }

        val result = textSpan.visitTextSpan(returnTrueFunction)

        assertThat(result).isTrue()
        verify(textSpan1, times(1)).visitTextSpan(returnTrueFunction)
        verify(textSpan2, times(1)).visitTextSpan(returnTrueFunction)
    }

    @Test
    fun `visitTextSpan with children and visitor always returns false`() {
        val textSpan1 = spy(TextSpan(text = "Hello"))
        val textSpan2 = spy(TextSpan(text = "World"))
        val textSpan = TextSpan(children = mutableListOf(textSpan1, textSpan2))
        val returnFalseFunction = { _: TextSpan -> false }

        val result = textSpan.visitTextSpan(returnFalseFunction)

        assertThat(result).isFalse()
        verify(textSpan1, times(1)).visitTextSpan(returnFalseFunction)
        verify(textSpan2, times(0)).visitTextSpan(returnFalseFunction)
    }

    @Test
    fun `toPlainText with neither text nor children`() {
        val textSpan = TextSpan()

        assertThat(textSpan.toPlainText()).isEmpty()
    }

    @Test
    fun `toPlainText with text`() {
        val string = "Hello"
        val textSpan = TextSpan(text = string)

        assertThat(textSpan.toPlainText()).isEqualTo(string)
    }

    @Test
    fun `toPlainText with children`() {
        val string1 = "Hello"
        val string2 = "World"
        val textSpan1 = TextSpan(text = string1)
        val textSpan2 = TextSpan(text = string2)
        val textSpan = TextSpan(children = mutableListOf(textSpan1, textSpan2))

        assertThat(textSpan.toPlainText()).isEqualTo(string1 + string2)
    }

    // TODO(Migration/qqd): Figure out what to do with codeUnitAt.
//    @Test
//    fun `codeUnitAt with negative index`() {
//        val textSpan = TextSpan(text = string1)
//
//        assertThat(textSpan.codeUnitAt(-1)).isNull()
//    }
//
//    @Test
//    fun `codeUnitAt with index larger than text length`() {
//        val textSpan = TextSpan(text = string1)
//
//        assertThat(textSpan.codeUnitAt(string1.length + 1)).isNull()
//    }
//
//    @Test
//    fun `codeUnitAt with valid text and index`() {
//        val textSpan = TextSpan(text = string1)
//        val index = 0
//
//        assertThat(textSpan.codeUnitAt(index)).isEqualTo(string1[index].toInt())
//    }

    @Test
    fun `compareTo self should return IDENTICAL`() {
        val textSpan = TextSpan()

        assertThat(textSpan.compareTo(textSpan)).isEqualTo(RenderComparison.IDENTICAL)
    }

    @Test
    fun `compareTo with different text should return LAYOUT`() {
        val textSpan1 = TextSpan(text = "Hello")
        val textSpan2 = TextSpan(text = "World")

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo with different children list length should return LAYOUT`() {
        val childTextSpan1 = TextSpan(text = "Hello")
        val childTextSpan2 = TextSpan(text = "World")
        val textSpan1 = TextSpan(children = mutableListOf(childTextSpan1))
        val textSpan2 = TextSpan(children = mutableListOf(childTextSpan1, childTextSpan2))

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo with one null style should return LAYOUT`() {
        val textSpan1 = TextSpan()
        val textSpan2 = TextSpan(style = TextStyle(letterSpacing = 1.5f))

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    /*@Test
    fun `compareTo with same recognizer should return IDENTICAL`() {
        val recognizer1 = MultiTapGestureRecognizer(longTapDelay = Duration.zero)
        val textSpan1 = TextSpan(recognizer = recognizer1)
        val textSpan2 = TextSpan(recognizer = recognizer1)

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.IDENTICAL)
    }

    @Test
    fun `compareTo with different recognizer should return METADATA`() {
        val recognizer1 = MultiTapGestureRecognizer(longTapDelay = Duration.zero)
        val recognizer2 = MultiTapGestureRecognizer(longTapDelay = Duration.create(seconds = 1L))
        val textSpan1 = TextSpan(recognizer = recognizer1)
        val textSpan2 = TextSpan(recognizer = recognizer2)

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.METADATA)
    }*/

    @Test
    fun `compareTo with different TextStyle with different fontSize should return LAYOUT`() {
        val textStyle1 = TextStyle()
        val textStyle2 = TextStyle(fontSize = 10.0f)
        val textSpan1 = TextSpan(style = textStyle1)
        val textSpan2 = TextSpan(style = textStyle2)

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo with different TextStyle with different color should return PAINT`() {
        val textStyle1 = TextStyle(color = Color(0))
        val textStyle2 = TextStyle(color = Color(1))
        val textSpan1 = TextSpan(style = textStyle1)
        val textSpan2 = TextSpan(style = textStyle2)

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.PAINT)
    }

    @Test
    fun `compareTo with different children with different text should return LAYOUT`() {
        val childTextSpan1 = TextSpan(text = "Hello")
        val childTextSpan2 = TextSpan(text = "World")
        val textSpan1 = TextSpan(children = mutableListOf(childTextSpan1))
        val textSpan2 = TextSpan(children = mutableListOf(childTextSpan2))

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo with different children with different fontSize should return LAYOUT`() {
        val textStyle1 = TextStyle()
        val textStyle2 = TextStyle(fontSize = 10.0f)
        val childTextSpan1 = TextSpan(style = textStyle1)
        val childTextSpan2 = TextSpan(style = textStyle2)
        val textSpan1 = TextSpan(children = mutableListOf(childTextSpan1))
        val textSpan2 = TextSpan(children = mutableListOf(childTextSpan2))

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.LAYOUT)
    }

    @Test
    fun `compareTo with different children with different color should return PAINT`() {
        val textStyle1 = TextStyle(color = Color(0))
        val textStyle2 = TextStyle(color = Color(1))
        val childTextSpan1 = TextSpan(style = textStyle1)
        val childTextSpan2 = TextSpan(style = textStyle2)
        val textSpan1 = TextSpan(children = mutableListOf(childTextSpan1))
        val textSpan2 = TextSpan(children = mutableListOf(childTextSpan2))

        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.PAINT)
    }

//    @Test
//    fun `compareTo with different children with different recognizer should return METADATA`() {
//        val recognizer1 = MultiTapGestureRecognizer(longTapDelay = Duration.zero)
//        val recognizer2 = MultiTapGestureRecognizer(longTapDelay = Duration.create(seconds = 1L))
//        val childTextSpan1 = TextSpan(recognizer = recognizer1)
//        val childTextSpan2 = TextSpan(recognizer = recognizer2)
//        val textSpan1 = TextSpan(children = mutableListOf(childTextSpan1))
//        val textSpan2 = TextSpan(children = mutableListOf(childTextSpan2))
//
//        assertThat(textSpan1.compareTo(textSpan2)).isEqualTo(RenderComparison.METADATA)
//    }

    @Test
    fun `toAnnotatedString with includeRootStyle default value`() {
        val textStyle = TextStyle(fontSize = 10f)
        val text = "Hello"
        val textSpan = TextSpan(style = textStyle, text = text)
        val annotatedString = textSpan.toAnnotatedString()

        // By default includeRootStyle = true and TextStyle on root node should be converted.
        assertThat(annotatedString.textStyles.size).isEqualTo(1)
        assertThat(annotatedString.textStyles[0].style).isEqualTo(textStyle)
        assertThat(annotatedString.textStyles[0].start).isEqualTo(0)
        assertThat(annotatedString.textStyles[0].end).isEqualTo(text.length)
    }

    @Test
    fun `toAnnotatedString with nested TextSpan plain text`() {
        val text1 = "Hello"
        val text2 = "World"

        val textSpan = TextSpan(
            text = text1,
            children = mutableListOf(TextSpan(text = text2))
        )
        val annotatedString = textSpan.toAnnotatedString()

        assertThat(annotatedString.text).isEqualTo(text1 + text2)
        assertThat(annotatedString.textStyles.size).isEqualTo(0)
    }

    @Test
    fun `toAnnotatedString with nested TextSpan with TextStyle`() {
        val textStyle1 = TextStyle(fontSize = 10f)
        val text1 = "Hello"

        val textStyle2 = TextStyle(color = Color.Red)
        val text2 = "World"

        val textSpan = TextSpan(
            style = textStyle1,
            text = text1,
            children = mutableListOf(TextSpan(style = textStyle2, text = text2))
        )
        val annotatedString = textSpan.toAnnotatedString()

        assertThat(annotatedString.text).isEqualTo(text1 + text2)
        assertThat(annotatedString.textStyles.size).isEqualTo(2)

        assertThat(annotatedString.textStyles[0].style).isEqualTo(textStyle1)
        assertThat(annotatedString.textStyles[0].start).isEqualTo(0)
        assertThat(annotatedString.textStyles[0].end).isEqualTo((text1 + text2).length)

        assertThat(annotatedString.textStyles[1].style).isEqualTo(textStyle2)
        assertThat(annotatedString.textStyles[1].start).isEqualTo(text1.length)
        assertThat(annotatedString.textStyles[1].end).isEqualTo((text1 + text2).length)
    }

    @Test
    fun `toAnnotatedString with complex nested TextSpan`() {
        /*
          Case
               _________Root_________
              /                      \
               Leaf1    ____Inner____
              /    \   /             \
              Lorem    leaf2     leaf3
                      /    \    /    \
                      ipsum     dolor

          Output
          AnnotatedString(
            text = "Lorem ipsum dolor"
            textStyles = listOf(Root[0, 17], Leaf1[0, 6], Inner[6, 17], leaf2[6, 12], leaf3[12, 17])
          )
         */
        val textStyleRoot = TextStyle(fontSize = 10f)
        val textStyleLeaf1 = TextStyle(color = Color.Blue)
        val text1 = "Lorem "

        val textStyleInner = TextStyle(color = Color.Blue)
        val textStyleLeaf2 = TextStyle(color = Color.Red)
        val text2 = "ipsum "

        val textStyleLeaf3 = TextStyle(color = Color.Blue)
        val text3 = "dolor"

        val textSpan = TextSpan(
            style = textStyleRoot,
            children = mutableListOf(
                TextSpan(text = text1, style = textStyleLeaf1),
                TextSpan(
                    style = textStyleInner,
                    children = mutableListOf(
                        TextSpan(text = text2, style = textStyleLeaf2),
                        TextSpan(text = text3, style = textStyleLeaf3)
                    )
                )
            )
        )
        val annotatedString = textSpan.toAnnotatedString()

        assertThat(annotatedString.text).isEqualTo(text1 + text2 + text3)
        assertThat(annotatedString.textStyles.size).isEqualTo(5)

        assertThat(annotatedString.textStyles[0].style).isEqualTo(textStyleRoot)
        assertThat(annotatedString.textStyles[0].start).isEqualTo(0)
        assertThat(annotatedString.textStyles[0].end)
            .isEqualTo((text1 + text2 + text3).length)

        assertThat(annotatedString.textStyles[1].style).isEqualTo(textStyleLeaf1)
        assertThat(annotatedString.textStyles[1].start).isEqualTo(0)
        assertThat(annotatedString.textStyles[1].end).isEqualTo(text1.length)

        assertThat(annotatedString.textStyles[2].style).isEqualTo(textStyleInner)
        assertThat(annotatedString.textStyles[2].start).isEqualTo(text1.length)
        assertThat(annotatedString.textStyles[2].end)
            .isEqualTo((text1 + text2 + text3).length)

        assertThat(annotatedString.textStyles[3].style).isEqualTo(textStyleLeaf2)
        assertThat(annotatedString.textStyles[3].start).isEqualTo(text1.length)
        assertThat(annotatedString.textStyles[3].end).isEqualTo((text1 + text2).length)

        assertThat(annotatedString.textStyles[4].style).isEqualTo(textStyleLeaf3)
        assertThat(annotatedString.textStyles[4].start).isEqualTo((text1 + text2).length)
        assertThat(annotatedString.textStyles[4].end)
            .isEqualTo((text1 + text2 + text3).length)
    }
}
