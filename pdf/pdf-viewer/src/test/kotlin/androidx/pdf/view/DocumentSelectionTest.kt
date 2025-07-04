/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.PdfRect
import androidx.pdf.view.DocumentSelection
import androidx.pdf.view.Selection
import androidx.pdf.view.TextSelection
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentSelectionTest {

    @Test
    fun testDocumentSelection_returnsNullOnNoSelections() {

        val bounds =
            listOf(
                PdfRect(1, RectF(1f, 1f, 1f, 1f)),
                PdfRect(2, RectF(2f, 2f, 2f, 2f)),
                PdfRect(3, RectF(3f, 3f, 3f, 3f)),
                PdfRect(4, RectF(4f, 4f, 4f, 4f)),
            )
        val selectedContents =
            SparseArray<List<Selection>>().apply {
                set(2, listOf(TextSelection("is the", listOf(bounds[1]))))
                set(1, listOf(TextSelection("This", listOf(bounds[0]))))
                set(4, listOf(TextSelection("of text", listOf(bounds[3]))))
                set(3, listOf(TextSelection("right order", listOf(bounds[2]))))
            }

        val expectedText = "This is the right order of text"

        val documentSelection = DocumentSelection(selectedContents)

        assert(documentSelection.selection is TextSelection)
        assertEquals((documentSelection.selection as TextSelection).text, expectedText)
        assertEquals((documentSelection.selection as TextSelection).bounds, bounds)
    }
}
