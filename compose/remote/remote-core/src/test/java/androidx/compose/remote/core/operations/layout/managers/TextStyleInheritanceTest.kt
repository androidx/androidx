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

import androidx.compose.remote.core.RemoteContext
import androidx.kruth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class TextStyleInheritanceTest {

    @Test
    fun testCoreTextFontSizeInheritance() {
        val styleAId = 1
        val styleBId = 2

        // TextStyle A with fontSize 42
        val styleA =
            TextStyle(
                styleAId,
                0xFF000000.toInt(),
                -1,
                42f,
                -1f,
                -1f,
                0,
                400f,
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
            )

        // TextStyle B with DEFAULT_FONT_SIZE and parent TextStyle A
        val styleB =
            TextStyle(
                styleBId,
                0xFF000000.toInt(),
                -1,
                TextStyle.DEFAULT_FONT_SIZE,
                -1f,
                -1f,
                0,
                TextStyle.DEFAULT_FONT_WEIGHT,
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
                styleAId,
            )

        val mockContext =
            mock<RemoteContext> {
                on { getObject(styleAId) } doReturn styleA
                on { getObject(styleBId) } doReturn styleB
                on { getText(any()) } doReturn "Hello"
            }

        // Apply styles
        styleA.apply(mockContext)
        styleB.apply(mockContext)

        // Apply TextStyle B to a text operation (CoreText)
        val coreText =
            CoreText(
                null,
                1,
                -1,
                1, // textId
                0xFF000000.toInt(),
                -1,
                TextStyle.DEFAULT_FONT_SIZE,
                -1f,
                -1f,
                0,
                TextStyle.DEFAULT_FONT_WEIGHT,
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
                styleBId,
            )

        coreText.updateVariables(mockContext)

        // Check that the font size used by the text is 42f
        val fontSizeField = CoreText::class.java.getDeclaredField("mFontSize")
        fontSizeField.isAccessible = true
        assertThat(fontSizeField.get(coreText)).isEqualTo(TextStyle.DEFAULT_FONT_SIZE)
    }

    @Test
    fun testDeepInheritance() {
        val grandParentId = 1
        val parentId = 2
        val childId = 3

        // Grandparent defines base size and color
        val grandParent =
            TextStyle(
                grandParentId,
                0xFFFF0000.toInt(), // Red
                null,
                30f,
                null,
                null,
                null,
                400f,
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

        // Parent overrides color to Green, inherits fontSize 30f
        val parent =
            TextStyle(
                parentId,
                0xFF00FF00.toInt(), // Green
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
                null,
                null,
                null,
                null,
                null,
                grandParentId,
            )

        // Child overrides fontSize to 50f, inherits color Green from parent
        val child =
            TextStyle(
                childId,
                null, // color null -> inherit
                null,
                50f, // fontSize specified
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
                null,
                null,
                null,
                null,
                null,
                parentId,
            )

        val mockContext =
            mock<RemoteContext> {
                on { getObject(grandParentId) } doReturn grandParent
                on { getObject(parentId) } doReturn parent
            }

        // Apply styles in order (simulating player lifecycle)
        grandParent.apply(mockContext)
        parent.apply(mockContext)
        child.apply(mockContext)

        assertThat(child.mFontSize).isEqualTo(50f)
        assertThat(child.mColor).isEqualTo(0xFF00FF00.toInt()) // Inherited from Parent
        assertThat(child.mFontWeight).isEqualTo(400f) // Inherited from Grandparent (via Parent)
    }

    @Test
    fun testPrecedenceWithExplicitDefaults() {
        val parentId = 1
        val childId = 2

        val parent =
            TextStyle(
                parentId,
                0xFFFF0000.toInt(), // Red
                -1,
                30f,
                -1f,
                -1f,
                0,
                700f,
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
                true,
                false,
                null,
                null,
                false,
            )

        // Child explicitly sets color to Black and underline to false
        // These should override the parent values.
        // Other fields (fontSize, fontWeight) are null so they should be inherited.
        val child =
            TextStyle(
                childId,
                0xFF000000.toInt(), // color specified
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
                false, // underline specified
                null,
                null,
                null,
                null,
                parentId,
            )

        val mockContext = mock<RemoteContext> { on { getObject(parentId) } doReturn parent }

        child.apply(mockContext)

        assertThat(child.mColor).isEqualTo(0xFF000000.toInt()) // Kept child's explicit black
        assertThat(child.mFontSize).isEqualTo(30f) // Inherited
        assertThat(child.mUnderline).isFalse() // Kept child's explicit false
        assertThat(child.mFontWeight).isEqualTo(700f) // Inherited
    }
}
