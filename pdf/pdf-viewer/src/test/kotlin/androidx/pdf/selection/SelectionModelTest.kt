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

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Parcel
import android.util.SparseArray
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.selection.model.ImageSelection
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
    fun testCreate_isOcrFlag() {
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

        val ocrSelection = SelectionModel.create(newPageSelections, isOcr = true)
        assertNotNull(ocrSelection)
        assertEquals(true, ocrSelection?.isOcr)

        val nonOcrSelection = SelectionModel.create(newPageSelections, isOcr = false)
        assertNotNull(nonOcrSelection)
        assertEquals(false, nonOcrSelection?.isOcr)
    }

    @Test
    fun testParcelable_isOcrPreserved() {
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

        val original = SelectionModel.create(newPageSelections, isOcr = true)
        val parcel = Parcel.obtain()
        original?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = SelectionModel.CREATOR.createFromParcel(parcel)
        assertNotNull(restored)
        assertEquals(true, restored.isOcr)
        assertEquals(original?.startBoundary, restored.startBoundary)
        assertEquals(original?.endBoundary, restored.endBoundary)
        parcel.recycle()
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

    @Test
    fun testToPlaceholder_createsLightweightPointsOnlySelection() {
        val newBounds: List<RectF> = listOf(RectF(100f, 100f, 200f, 200f))
        val newPageSelections: List<PageSelection?> =
            listOf(
                PageSelection(
                    1,
                    selectionBoundary,
                    selectionBoundary,
                    listOf(PdfPageTextContent(newBounds, "Large Text Sample")),
                )
            )

        val original = SelectionModel.create(newPageSelections)
        assertNotNull(original)
        assertEquals(false, original?.isPlaceholder)

        val placeholder = original?.toPlaceholder()
        assertNotNull(placeholder)
        assertEquals(true, placeholder?.isPlaceholder)
        assertEquals(original?.startBoundary, placeholder?.startBoundary)
        assertEquals(original?.endBoundary, placeholder?.endBoundary)
        assertEquals(0, placeholder?.documentSelection?.selectedContents?.size())
    }

    @Test
    fun testParcelable_isPlaceholderPreserved() {
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

        val original = SelectionModel.create(newPageSelections)?.toPlaceholder()
        val parcel = Parcel.obtain()
        original?.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = SelectionModel.CREATOR.createFromParcel(parcel)
        assertNotNull(restored)
        assertEquals(true, restored.isPlaceholder)
        assertEquals(original?.startBoundary, restored.startBoundary)
        assertEquals(original?.endBoundary, restored.endBoundary)
        parcel.recycle()
    }

    @Test
    fun testToPlaceholder_withImageSelection_createsPlaceholderImageSelection() {
        val pdfRect = PdfRect(1, 10f, 10f, 100f, 100f)
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        val imageSelection = ImageSelection(bitmap, pdfRect)
        val boundary = UiSelectionBoundary(PdfPoint(1, PointF(10f, 10f)), isRtl = false)
        val selectedContents =
            SparseArray<List<Selection>>().apply { put(1, listOf(imageSelection)) }
        val original =
            SelectionModel(
                documentSelection = DocumentSelection(selectedContents = selectedContents),
                startBoundary = boundary,
                endBoundary = boundary,
                isPlaceholder = false,
            )

        // Since ImageSelection natively strips the bitmap in
        // writeToParcel/imageSelectionFromParcel,
        // toPlaceholder() returns the model as-is without redundant object allocation.
        val placeholder = original.toPlaceholder()
        assertEquals(original, placeholder)

        // Verify that when unparceled across IPC, ImageSelection converts to a lightweight
        // placeholder.
        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val unparceled = SelectionModel.CREATOR.createFromParcel(parcel)
        assertNotNull(unparceled)
        val resultSelection = unparceled.documentSelection.selection as ImageSelection
        assertEquals(true, resultSelection.isPlaceholder)
        assertEquals(pdfRect, resultSelection.bounds.first())
        parcel.recycle()
    }
}
