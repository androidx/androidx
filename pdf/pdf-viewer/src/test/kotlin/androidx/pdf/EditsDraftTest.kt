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

package androidx.pdf

import androidx.pdf.annotation.models.TestPdfAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class EditsDraftTest {
    private fun createDraft(vararg pages: Int): EditsDraft {
        val builder = MutableEditsDraft()
        pages.forEach { pageNum -> builder.insert(TestPdfAnnotation(pageNum)) }
        return builder.toEditsDraft()
    }

    @Test
    fun splitAt_middleIndex_splitsIntoTwoCorrectDrafts() {
        // Arrange: Creates a draft with 4 insert operations (pages 1, 2, 3, 4)
        val originalDraft = createDraft(1, 2, 3, 4)

        // Act
        val (left, right) = originalDraft.splitAt(2)
        val leftExpected = createDraft(1, 2)

        // Assert
        assertEquals(leftExpected, left)
        assertEquals(createDraft(3, 4), right)
    }

    @Test
    fun splitAt_indexZero_returnsEmptyLeftAndFullRight() {
        // Arrange
        val originalDraft = createDraft(1, 2, 3)

        // Act
        val (left, right) = originalDraft.splitAt(0)

        // Assert
        assertEquals(createDraft(), left) // Empty draft
        assertEquals(createDraft(1, 2, 3), right)
    }

    @Test
    fun splitAt_endIndex_returnsFullLeftAndEmptyRight() {
        // Arrange
        val originalDraft = createDraft(1, 2, 3)

        // Act
        val (left, right) = originalDraft.splitAt(3)

        // Assert
        assertEquals(createDraft(1, 2, 3), left)
        assertEquals(createDraft(), right) // Empty draft
    }

    @Test
    fun splitAt_invalidIndex_throwsIndexOutOfBoundsException() {
        // Arrange
        val originalDraft = createDraft(1, 2, 3)

        // Act & Assert
        assertFailsWith<IllegalArgumentException> { originalDraft.splitAt(-1) }
        assertFailsWith<IllegalArgumentException> { originalDraft.splitAt(4) }
    }
}
