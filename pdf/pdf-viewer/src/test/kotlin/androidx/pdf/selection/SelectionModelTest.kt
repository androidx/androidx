/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.selection

import android.graphics.Point
import android.graphics.RectF
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.selection.model.TextSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SelectionModelTest {

    val selectionBoundary = SelectionBoundary(0, Point(100, 100), false)

    @Test
    fun testCreate_returnsNullOnNoSelections() {
        val result = SelectionModel.create(emptyList())
        assertNull(result)
    }

    @Test
    fun testCreate_singleSelection() {

        val newBounds: List<RectF> = listOf(RectF(100f, 100f, 200f, 200f))
        val newPageSelections: List<PageSelection?> =
            listOf(
                PageSelection(
                    1,
                    selectionBoundary,
                    selectionBoundary,
                    listOf(PdfPageTextContent(newBounds, "Hello")),
                )
            )

        val combinedSelection: SelectionModel? = SelectionModel.create(newPageSelections)
        assertNotNull(combinedSelection?.documentSelection?.selection?.bounds)
        assertEquals(
            combinedSelection?.documentSelection?.selection?.bounds?.map {
                (RectF(it.left, it.top, it.right, it.bottom))
            },
            newBounds,
        )
        assertEquals(
            "Hello",
            (combinedSelection?.documentSelection?.selection as TextSelection).text,
        )
    }

    @Test
    fun testCreate_multipleNewSelectionsOnMultiplePages() {
        val newBoundsPage1: List<RectF> =
            listOf(RectF(100f, 100f, 200f, 200f), RectF(200f, 200f, 300f, 300f))
        val newBoundsPage2: List<RectF> =
            listOf(RectF(300f, 300f, 400f, 400f), RectF(400f, 400f, 500f, 500f))
        val newPageSelections: List<PageSelection?> =
            listOf(
                PageSelection(
                    1,
                    selectionBoundary,
                    selectionBoundary,
                    listOf(PdfPageTextContent(newBoundsPage1, "Hello")),
                ),
                PageSelection(
                    2,
                    selectionBoundary,
                    selectionBoundary,
                    listOf(PdfPageTextContent(newBoundsPage2, "World")),
                ),
            )

        val combinedSelection: SelectionModel? = SelectionModel.create(newPageSelections)

        assertNotNull(combinedSelection?.documentSelection?.selection?.bounds)

        val expectedBounds = newBoundsPage1 + newBoundsPage2
        val resultBounds =
            combinedSelection?.documentSelection?.selection?.bounds?.map {
                (RectF(it.left, it.top, it.right, it.bottom))
            }
        assertNotNull(resultBounds)
        assertEquals(expectedBounds.size, resultBounds?.size)
        assertEquals(expectedBounds, resultBounds)
        assertEquals(
            "Hello World",
            (combinedSelection?.documentSelection?.selection as TextSelection).text,
        )
    }
}
