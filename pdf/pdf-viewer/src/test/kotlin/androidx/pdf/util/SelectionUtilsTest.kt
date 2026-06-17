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

package androidx.pdf.util

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Parcel
import androidx.pdf.Dimension
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.annotation.content.ImagePdfObject
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.selection.model.TextSelection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SelectionUtilsTest {

    @Test
    fun pdfRect_writeToParcel_readsCorrectly() {
        val pdfRect = PdfRect(1, 10f, 20f, 30f, 40f)
        val parcel = Parcel.obtain()
        try {
            pdfRect.writeToParcel(parcel)
            parcel.setDataPosition(0)

            val pageNum = parcel.readInt()
            val left = parcel.readFloat()
            val top = parcel.readFloat()
            val right = parcel.readFloat()
            val bottom = parcel.readFloat()

            assertThat(pageNum).isEqualTo(1)
            assertThat(left).isEqualTo(10f)
            assertThat(top).isEqualTo(20f)
            assertThat(right).isEqualTo(30f)
            assertThat(bottom).isEqualTo(40f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pdfPoint_writeToParcel_readsCorrectly() {
        val pdfPoint = PdfPoint(2, 50f, 60f)
        val parcel = Parcel.obtain()
        try {
            pdfPoint.writeToParcel(parcel)
            parcel.setDataPosition(0)

            val pageNum = parcel.readInt()
            val x = parcel.readFloat()
            val y = parcel.readFloat()

            assertThat(pageNum).isEqualTo(2)
            assertThat(x).isEqualTo(50f)
            assertThat(y).isEqualTo(60f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun toImagePoint_returnsCorrectPixelCoordinates() {
        // PDF image at (10, 10) with size 100x100
        val imageRect = RectF(10f, 10f, 110f, 110f)
        // Image bitmap is 1000x1000 pixels
        val bitmapSize = Dimension(1000, 1000)

        // Point at (20, 30) in PDF space
        // Relative to image: X = ((20-10)/100)*1000 = 100, Y = ((30-10)/100)*1000 = 200
        val pdfPoint = PdfPoint(pageNum = 0, x = 20f, y = 30f)

        val imagePoint = pdfPoint.toImagePoint(imageRect, bitmapSize)

        assertThat(imagePoint.x).isEqualTo(100)
        assertThat(imagePoint.y).isEqualTo(200)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toImagePoint_withZeroWidth_throwsException() {
        val imageRect = RectF(10f, 10f, 10f, 110f)
        val bitmapSize = Dimension(1000, 1000)
        val pdfPoint = PdfPoint(pageNum = 0, x = 20f, y = 30f)

        pdfPoint.toImagePoint(imageRect, bitmapSize)
    }

    @Test
    fun pageSelection_toViewSelection_returnsCorrectList() {
        val pageNum = 5
        val rect1 = RectF(0f, 0f, 10f, 10f)
        val rect2 = RectF(20f, 20f, 30f, 30f)
        val textContent = PdfPageTextContent(listOf(rect1, rect2), "sample text")
        val pageSelection =
            PageSelection(pageNum, SelectionBoundary(), SelectionBoundary(), listOf(textContent))

        val viewSelections = pageSelection.toViewSelection()

        assertThat(viewSelections).hasSize(1)
        val textSelection = viewSelections[0] as TextSelection
        assertThat(textSelection.text.toString()).isEqualTo("sample text")
        assertThat(textSelection.bounds).hasSize(2)
        assertThat(textSelection.bounds[0].pageNum).isEqualTo(pageNum)
        assertThat(textSelection.bounds[0].left).isEqualTo(0f)
        assertThat(textSelection.bounds[1].pageNum).isEqualTo(pageNum)
        assertThat(textSelection.bounds[1].top).isEqualTo(20f)
    }

    @Test
    fun imagePdfObject_toImageSelection_returnsCorrectSelection() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val bounds = RectF(10f, 20f, 30f, 40f)
        val imagePdfObject = ImagePdfObject(bitmap, bounds)
        val pageNum = 3

        val imageSelection = imagePdfObject.toImageSelection(pageNum)

        assertThat(imageSelection.bitmap).isEqualTo(bitmap)
        assertThat(imageSelection.bounds).hasSize(1)
        assertThat(imageSelection.bounds[0].pageNum).isEqualTo(pageNum)
        assertThat(imageSelection.bounds[0].left).isEqualTo(10f)
        assertThat(imageSelection.bounds[0].top).isEqualTo(20f)
    }

    @Test
    fun imagePdfObject_bitmapSize_returnsCorrectSize() {
        val bitmap = Bitmap.createBitmap(50, 100, Bitmap.Config.ARGB_8888)
        val imagePdfObject = ImagePdfObject(bitmap, RectF())

        val size = imagePdfObject.bitmapSize

        assertThat(size.x).isEqualTo(50)
        assertThat(size.y).isEqualTo(100)
    }

    @Test
    fun imageSelectionFromParcel_returnsPlaceholderSelection() {
        val parcel = Parcel.obtain()
        try {
            // Write ImageSelection data to parcel
            parcel.writeInt(1) // bounds size
            parcel.writeInt(1) // pageNum
            parcel.writeFloat(10f) // left
            parcel.writeFloat(20f) // top
            parcel.writeFloat(30f) // right
            parcel.writeFloat(40f) // bottom
            parcel.setDataPosition(0)

            val imageSelection = imageSelectionFromParcel(parcel)

            assertThat(imageSelection.isPlaceholder).isTrue()
            assertThat(imageSelection.bitmap).isEqualTo(PLACEHOLDER_BITMAP)
            assertThat(imageSelection.bounds).hasSize(1)
            assertThat(imageSelection.bounds[0].pageNum).isEqualTo(1)
            assertThat(imageSelection.bounds[0].left).isEqualTo(10f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun textSelectionFromParcel_returnsCorrectSelection() {
        val parcel = Parcel.obtain()
        try {
            // TextSelection writes text then bounds size then bounds
            val text = "test text"
            android.text.TextUtils.writeToParcel(text, parcel, 0)
            parcel.writeInt(1) // bounds size
            parcel.writeInt(2) // pageNum
            parcel.writeFloat(5f) // left
            parcel.writeFloat(15f) // top
            parcel.writeFloat(25f) // right
            parcel.writeFloat(35f) // bottom
            parcel.setDataPosition(0)

            val textSelection = textSelectionFromParcel(parcel)

            assertThat(textSelection.text.toString()).isEqualTo(text)
            assertThat(textSelection.bounds).hasSize(1)
            assertThat(textSelection.bounds[0].pageNum).isEqualTo(2)
            assertThat(textSelection.bounds[0].left).isEqualTo(5f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pdfPointFromParcel_returnsCorrectPoint() {
        val parcel = Parcel.obtain()
        try {
            parcel.writeInt(7)
            parcel.writeFloat(1.5f)
            parcel.writeFloat(2.5f)
            parcel.setDataPosition(0)

            val pdfPoint = pdfPointFromParcel(parcel)

            assertThat(pdfPoint.pageNum).isEqualTo(7)
            assertThat(pdfPoint.x).isEqualTo(1.5f)
            assertThat(pdfPoint.y).isEqualTo(2.5f)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun pdfRectFromParcel_returnsCorrectRect() {
        val parcel = Parcel.obtain()
        try {
            parcel.writeInt(9)
            parcel.writeFloat(10f)
            parcel.writeFloat(20f)
            parcel.writeFloat(30f)
            parcel.writeFloat(40f)
            parcel.setDataPosition(0)

            val pdfRect = pdfRectFromParcel(parcel)

            assertThat(pdfRect.pageNum).isEqualTo(9)
            assertThat(pdfRect.left).isEqualTo(10f)
            assertThat(pdfRect.top).isEqualTo(20f)
            assertThat(pdfRect.right).isEqualTo(30f)
            assertThat(pdfRect.bottom).isEqualTo(40f)
        } finally {
            parcel.recycle()
        }
    }
}
