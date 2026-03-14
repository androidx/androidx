/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.managers

import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.WireBuffer
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class TextStyleTest {

    @Test
    fun testTextStyleSerialization() {
        val buffer = WireBuffer()
        val id = 42
        val color = 0xFFFF0000.toInt()
        val fontSize = 24f
        val fontWeight = 700f
        val textAlign = CoreText.TEXT_ALIGN_CENTER
        val underline = true

        TextStyle.apply(
            buffer,
            id,
            color,
            null,
            fontSize,
            null,
            null,
            null,
            fontWeight,
            null,
            textAlign,
            CoreText.OVERFLOW_ELLIPSIS,
            10,
            0.5f,
            2f,
            1.5f,
            CoreText.BREAK_STRATEGY_HIGH_QUALITY,
            CoreText.HYPHENATION_FREQUENCY_FULL,
            CoreText.JUSTIFICATION_MODE_INTER_CHARACTER,
            underline,
            null,
            null,
            null,
            null,
            null,
        )

        val operations = mutableListOf<Operation>()
        buffer.reset(0)
        val opCode = buffer.readByte()
        Truth.assertThat(opCode).isEqualTo(242) // Operations.TEXT_STYLE

        TextStyle.read(buffer, operations)
        Truth.assertThat(operations).hasSize(1)
        val readStyle = operations[0] as TextStyle

        assertThat(readStyle.mId).isEqualTo(id)
        assertThat(readStyle.mColor).isEqualTo(color)
        assertThat(readStyle.mFontSize).isEqualTo(fontSize)
        assertThat(readStyle.mFontWeight).isEqualTo(fontWeight)
        assertThat(readStyle.mTextAlign).isEqualTo(textAlign)
        assertThat(readStyle.mUnderline).isEqualTo(underline)
        assertThat(readStyle.mLetterSpacing).isEqualTo(0.5f)
        assertThat(readStyle.mLineHeightAdd).isEqualTo(2f)
        assertThat(readStyle.mLineHeightMultiplier).isEqualTo(1.5f)
        assertThat(readStyle.mLineBreakStrategy).isEqualTo(CoreText.BREAK_STRATEGY_HIGH_QUALITY)
        assertThat(readStyle.mHyphenationFrequency).isEqualTo(CoreText.HYPHENATION_FREQUENCY_FULL)
        assertThat(readStyle.mJustificationMode)
            .isEqualTo(CoreText.JUSTIFICATION_MODE_INTER_CHARACTER)
        assertThat(readStyle.mParentId).isNull()
    }

    @Test
    fun testTextStyleDefaults() {
        val buffer = WireBuffer()
        TextStyle.apply(
            buffer,
            42, // Use a real id
            0xFF000000.toInt(),
            null,
            TextStyle.DEFAULT_FONT_SIZE,
            null,
            null,
            null,
            TextStyle.DEFAULT_FONT_WEIGHT,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        )

        val operations = mutableListOf<Operation>()
        buffer.reset(0)
        buffer.readByte() // skip opcode
        TextStyle.read(buffer, operations)
        val readStyle = operations[0] as TextStyle

        assertThat(readStyle.mId).isEqualTo(42)
        assertThat(readStyle.mColor).isEqualTo(0xFF000000.toInt())
        assertThat(readStyle.mFontSize).isEqualTo(TextStyle.DEFAULT_FONT_SIZE)
        assertThat(readStyle.mFontWeight).isEqualTo(TextStyle.DEFAULT_FONT_WEIGHT)
        assertThat(readStyle.mTextAlign).isNull()
        assertThat(readStyle.mUnderline).isNull()
        assertThat(readStyle.mParentId).isNull()
    }

    @Test
    fun testTextStyleInheritance() {
        val parentId = 1
        val childId = 2

        val parentStyle =
            TextStyle(
                parentId,
                0xFFFF0000.toInt(), // Red
                null,
                24f,
                null,
                null,
                null,
                700f,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true, // underline
                null,
                null,
                null,
                null,
                null,
            )

        // Child specifies Green, but leaves fontSize and weight to be inherited
        val childStyle =
            TextStyle(
                childId,
                0xFF00FF00.toInt(), // Green specified
                null,
                null, // fontSize null -> inherit
                null,
                null,
                null,
                null, // fontWeight null -> inherit
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, // underline null -> inherit
                null,
                null,
                null,
                null,
                parentId,
            )

        val mockContext = mock<RemoteContext> { on { getObject(parentId) } doReturn parentStyle }

        childStyle.apply(mockContext)

        // color should be Green (specified in child)
        assertThat(childStyle.mColor).isEqualTo(0xFF00FF00.toInt())
        // fontSize should be 24f (inherited from parent)
        assertThat(childStyle.mFontSize).isEqualTo(24f)
        // fontWeight should be 700f (inherited from parent)
        assertThat(childStyle.mFontWeight).isEqualTo(700f)
        // underline should be true (inherited from parent)
        assertThat(childStyle.mUnderline).isTrue()
    }

    @Test
    fun testCoreTextIntegration() {
        val styleId = 100
        val testStyle =
            TextStyle(
                styleId,
                0xFF00FF00.toInt(), // color
                null, // colorId
                32f, // fontSize
                null,
                null, // min/max fontSize
                1, // fontStyle (Italic)
                900f, // fontWeight
                null, // fontFamilyId
                CoreText.TEXT_ALIGN_RIGHT,
                CoreText.OVERFLOW_VISIBLE,
                5, // maxLines
                1.2f, // letterSpacing
                4f, // lineHeightAdd
                2.0f, // lineHeightMultiplier
                CoreText.BREAK_STRATEGY_BALANCED,
                CoreText.HYPHENATION_FREQUENCY_NORMAL,
                CoreText.JUSTIFICATION_MODE_INTER_WORD,
                true, // underline
                true, // strikethrough
                null,
                null, // fontAxis
                false, // autosize
                null,
            )

        val mockContext =
            mock<RemoteContext> {
                on { getObject(styleId) } doReturn testStyle
                on { getText(any()) } doReturn "TestString"
            }

        val coreText =
            CoreText(
                null, // parent
                1, // componentId
                -1, // animationId
                0f,
                0f, // x, y
                100f,
                100f, // width, height
                1, // textId
                0xFF000000.toInt(), // color
                -1, // colorId
                TextStyle.DEFAULT_FONT_SIZE, // fontSize
                -1f,
                -1f, // minFontSize, maxFontSize
                0, // fontStyle
                TextStyle.DEFAULT_FONT_WEIGHT, // fontWeight
                -1, // fontFamilyId
                1, // textAlign
                1, // overflow
                Int.MAX_VALUE, // maxLines
                0f, // letterSpacing
                0f, // lineHeightAdd
                1f, // lineHeightMultiplier
                0, // lineBreakStrategy
                0, // hyphenationFrequency
                0, // justificationMode
                false, // underline
                false, // strikethrough
                null, // fontAxis
                null, // fontAxisValues
                false, // autosize
                0, // flags
                styleId,
            )

        coreText.updateVariables(mockContext)

        // Using reflection to check private fields
        val colorField = CoreText::class.java.getDeclaredField("mColor")
        colorField.isAccessible = true
        assertThat(colorField.get(coreText)).isEqualTo(0xFF00FF00.toInt())

        val fontSizeField = CoreText::class.java.getDeclaredField("mFontSize")
        fontSizeField.isAccessible = true
        assertThat(fontSizeField.get(coreText)).isEqualTo(32f)

        val fontWeightField = CoreText::class.java.getDeclaredField("mFontWeight")
        fontWeightField.isAccessible = true
        assertThat(fontWeightField.get(coreText)).isEqualTo(900f)

        val textAlignField = CoreText::class.java.getDeclaredField("mTextAlign")
        textAlignField.isAccessible = true
        assertThat(textAlignField.get(coreText)).isEqualTo(CoreText.TEXT_ALIGN_RIGHT)
    }

    @Test
    fun testTextStylePrecedence() {
        val styleId = 101
        val testStyle =
            TextStyle(
                styleId,
                0xFF00FF00.toInt(), // Green
                null,
                50f, // Large font size
                null,
                null,
                null,
                900f, // Heavy weight
                null,
                CoreText.TEXT_ALIGN_CENTER,
                CoreText.OVERFLOW_ELLIPSIS,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
            )

        val mockContext =
            mock<RemoteContext> {
                on { getObject(styleId) } doReturn testStyle
                on { getText(any()) } doReturn "TestString"
            }

        // CoreText specifies RED, but leaves fontSize and weight as default
        val coreText =
            CoreText(
                null,
                1,
                -1,
                0f,
                0f,
                100f,
                100f,
                1,
                0xFFFF0000.toInt(), // RED (Specified)
                -1,
                TextStyle.DEFAULT_FONT_SIZE, // Default
                -1f,
                -1f,
                0,
                TextStyle.DEFAULT_FONT_WEIGHT, // Default
                -1,
                1,
                1,
                Int.MAX_VALUE,
                0f,
                0f,
                1f,
                0,
                0,
                0,
                false,
                false,
                null,
                null,
                false,
                0,
                styleId,
            )

        coreText.updateVariables(mockContext)

        // Color should stay RED because it was specified in CoreText
        val colorField = CoreText::class.java.getDeclaredField("mColor")
        colorField.isAccessible = true
        assertThat(colorField.get(coreText)).isEqualTo(0xFFFF0000.toInt())

        // FontSize should be 50f from TextStyle because it was NOT specified in CoreText
        val fontSizeField = CoreText::class.java.getDeclaredField("mFontSize")
        fontSizeField.isAccessible = true
        assertThat(fontSizeField.get(coreText)).isEqualTo(50f)

        // FontWeight should be 900f from TextStyle because it was NOT specified in CoreText
        val fontWeightField = CoreText::class.java.getDeclaredField("mFontWeight")
        fontWeightField.isAccessible = true
        assertThat(fontWeightField.get(coreText)).isEqualTo(900f)
    }
}
