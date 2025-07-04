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

import android.graphics.Point
import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.PdfRect
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.view.DocumentSelection
import androidx.pdf.view.Selection
import androidx.pdf.view.SelectionModel
import androidx.pdf.view.TextSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectionModelTest {

    val selectionBoundary = SelectionBoundary(0, Point(100, 100), false)

    @Test
    fun testCombineSelections_returnsNullOnNoSelections() {
        val result =
            SelectionModel.getCombinedSelectionModel(DocumentSelection(SparseArray()), emptyList())
        assertNull(result)
    }

    @Test
    fun testCombineSelections_combineSingleSelection() {

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

        val combinedSelection: SelectionModel? =
            SelectionModel.getCombinedSelectionModel(
                DocumentSelection(SparseArray()),
                newPageSelections,
            )
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
    fun testCombineSelections_multipleNewSelectionsOnMultiplePages() {
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

        val combinedSelection: SelectionModel? =
            SelectionModel.getCombinedSelectionModel(
                DocumentSelection(SparseArray()),
                newPageSelections,
            )

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

    @Test
    fun testCombineSelections_combineWithCurrentAndNewSelections() {
        val currentBounds =
            listOf(
                PdfRect(1, RectF(100f, 100f, 200f, 200f)),
                PdfRect(2, RectF(300f, 300f, 400f, 400f)),
                PdfRect(2, RectF(400f, 400f, 500f, 500f)),
                PdfRect(3, RectF(500f, 500f, 600f, 600f)),
                PdfRect(3, RectF(600f, 600f, 700f, 700f)),
            )

        val newBounds = listOf(RectF(150f, 150f, 200f, 200f), RectF(200f, 200f, 250f, 250f))
        val expectedBounds =
            listOf(
                currentBounds[0],
                PdfRect(2, newBounds[0]),
                PdfRect(2, newBounds[1]),
                currentBounds[3],
                currentBounds[4],
            )

        val currentSelection =
            DocumentSelection(
                SparseArray<List<Selection>>().apply {
                    set(1, listOf(TextSelection("this is page 1", listOf(currentBounds[0]))))
                    set(
                        2,
                        listOf(
                            TextSelection(
                                "this is page 2",
                                listOf(currentBounds[1], currentBounds[2]),
                            )
                        ),
                    )
                    set(
                        3,
                        listOf(
                            TextSelection(
                                "this is page 3",
                                listOf(currentBounds[3], currentBounds[4]),
                            )
                        ),
                    )
                }
            )

        val newPageSelections: List<PageSelection?> =
            listOf(
                PageSelection(
                    2,
                    selectionBoundary,
                    selectionBoundary,
                    listOf(
                        PdfPageTextContent(listOf(newBounds[0]), "New content"),
                        PdfPageTextContent(listOf(newBounds[1]), "for page 2"),
                    ),
                )
            )

        val combinedSelection =
            SelectionModel.getCombinedSelectionModel(currentSelection, newPageSelections)

        assertNotNull(combinedSelection)
        assert(combinedSelection?.documentSelection?.selection is TextSelection)
        val textSelection = combinedSelection?.documentSelection?.selection as TextSelection

        assertNotNull(textSelection.bounds)
        var resultBounds: List<PdfRect> = textSelection.bounds
        resultBounds = resultBounds.sortedWith(compareBy({ it.pageNum }, { it.left }, { it.top }))

        assertNotNull(resultBounds)
        assertEquals(expectedBounds.size, resultBounds.size)
        assertEquals(expectedBounds, resultBounds)
        assertEquals("this is page 1 New content for page 2 this is page 3", textSelection.text)
    }
}
