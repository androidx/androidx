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

package androidx.pdf.models

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import androidx.pdf.DraftEditOperation as ParcelableDraftEditOperation
import androidx.pdf.InsertDraftEditOperation as ParcelableInsertDraftEditOperation
import androidx.pdf.RemoveDraftEditOperation as ParcelableRemoveDraftEditOperation
import androidx.pdf.UpdateDraftEditOperation as ParcelableUpdateDraftEditOperation
import androidx.pdf.annotation.content.DraftEditOperation
import androidx.pdf.annotation.content.HighlightAnnotation
import androidx.pdf.annotation.content.ImagePdfObject
import androidx.pdf.annotation.content.InsertDraftEditOperation
import androidx.pdf.annotation.content.KeyedPdfAnnotation
import androidx.pdf.annotation.content.KeyedPdfObject
import androidx.pdf.annotation.content.PathPdfObject
import androidx.pdf.annotation.content.RemoveDraftEditOperation
import androidx.pdf.annotation.content.StampAnnotation
import androidx.pdf.annotation.content.UpdateDraftEditOperation
import androidx.pdf.annotation.models.HighlightAnnotation as ParcelableHighlight
import androidx.pdf.annotation.models.ImagePdfObject as ParcelableImage
import androidx.pdf.annotation.models.KeyedPdfAnnotation as ParcelableKeyedPdfAnnotation
import androidx.pdf.annotation.models.KeyedPdfObject as ParcelableKeyedObject
import androidx.pdf.annotation.models.PathPdfObject as ParcelablePath
import androidx.pdf.annotation.models.PdfAnnotation as ParcelableAnnotation
import androidx.pdf.annotation.models.StampAnnotation as ParcelableStamp
import androidx.pdf.models.PdfModelMapper.toContent
import androidx.pdf.models.PdfModelMapper.toParcelable
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PdfModelMapperTest {

    @Test
    fun highlightAnnotation_toParcelable_toContent() {
        val bounds = listOf(RectF(10f, 10f, 20f, 20f))
        val original = HighlightAnnotation(pageNum = 1, bounds = bounds, color = Color.YELLOW)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableHighlight::class.java)
        assertThat(parcelable.pageNum).isEqualTo(1)
        assertThat((parcelable as ParcelableHighlight).bounds).isEqualTo(bounds)
        assertThat(parcelable.color).isEqualTo(Color.YELLOW)

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun stampAnnotation_toParcelable_toContent() {
        val bounds = RectF(0f, 0f, 100f, 100f)
        val pdfObjects =
            listOf(
                PathPdfObject(
                    brushColor = Color.RED,
                    brushWidth = 5f,
                    inputs =
                        listOf(PathPdfObject.PathInput(0f, 0f, PathPdfObject.PathInput.MOVE_TO)),
                )
            )
        val original = StampAnnotation(pageNum = 2, bounds = bounds, pdfObjects = pdfObjects)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableStamp::class.java)
        assertThat(parcelable.pageNum).isEqualTo(2)
        assertThat((parcelable as ParcelableStamp).bounds).isEqualTo(bounds)
        assertThat(parcelable.pdfObjects).hasSize(1)

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun pathPdfObject_toParcelable_toContent() {
        val inputs =
            listOf(
                PathPdfObject.PathInput(10f, 10f, PathPdfObject.PathInput.MOVE_TO),
                PathPdfObject.PathInput(20f, 20f, PathPdfObject.PathInput.LINE_TO),
            )
        val original = PathPdfObject(brushColor = Color.BLUE, brushWidth = 2f, inputs = inputs)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelablePath::class.java)
        val p = parcelable as ParcelablePath
        assertThat(p.brushColor).isEqualTo(Color.BLUE)
        assertThat(p.brushWidth).isEqualTo(2f)
        assertThat(p.inputs).hasSize(2)
        assertThat(p.inputs[0].x).isEqualTo(10f)
        assertThat(p.inputs[1].command).isEqualTo(PathPdfObject.PathInput.LINE_TO)

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun imagePdfObject_toParcelable_toContent() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val bounds = RectF(0f, 0f, 50f, 50f)
        val original = ImagePdfObject(bitmap, bounds)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableImage::class.java)
        val p = parcelable as ParcelableImage
        assertThat(p.bitmap).isEqualTo(bitmap)
        assertThat(p.bounds).isEqualTo(bounds)

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun insertDraftEditOperation_toParcelable() {
        val annotation = HighlightAnnotation(0, emptyList(), Color.RED)
        val original = InsertDraftEditOperation(annotation)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableInsertDraftEditOperation::class.java)
        val p = parcelable as ParcelableInsertDraftEditOperation
        assertThat(p.annotation.pageNum).isEqualTo(0)
        assertThat((p.annotation as ParcelableHighlight).color).isEqualTo(Color.RED)
    }

    @Test
    fun updateDraftEditOperation_toParcelable() {
        val annotation = HighlightAnnotation(0, emptyList(), Color.RED)
        val original = UpdateDraftEditOperation("key1", annotation)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableUpdateDraftEditOperation::class.java)
        val p = parcelable as ParcelableUpdateDraftEditOperation
        assertThat(p.id).isEqualTo("key1")
        assertThat(p.annotation.pageNum).isEqualTo(0)
    }

    @Test
    fun removeDraftEditOperation_toParcelable() {
        val original = RemoveDraftEditOperation("key2", 5)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableRemoveDraftEditOperation::class.java)
        val p = parcelable as ParcelableRemoveDraftEditOperation
        assertThat(p.id).isEqualTo("key2")
        assertThat(p.pageNum).isEqualTo(5)
    }

    @Test
    fun keyedPdfAnnotation_toParcelable_toContent() {
        val annotation = HighlightAnnotation(0, emptyList(), Color.GREEN)
        val original = KeyedPdfAnnotation("key3", annotation)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableKeyedPdfAnnotation::class.java)
        assertThat(parcelable.key).isEqualTo("key3")

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack.key).isEqualTo("key3")
        assertThat(convertedBack.annotation).isEqualTo(annotation)
    }

    @Test
    fun keyedPdfObject_toParcelable_toContent() {
        val pdfObject =
            ImagePdfObject(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                RectF(0f, 0f, 1f, 1f),
            )
        val original = KeyedPdfObject("key4", pdfObject)

        val parcelable = original.toParcelable()
        assertThat(parcelable).isInstanceOf(ParcelableKeyedObject::class.java)
        assertThat(parcelable.key).isEqualTo("key4")

        val convertedBack = parcelable.toContent()
        assertThat(convertedBack.key).isEqualTo("key4")
        assertThat(convertedBack.pdfObject).isEqualTo(pdfObject)
    }

    @Test
    fun draftEditOperation_alreadyParcelable_toParcelable_returnsSame() {
        class Both : DraftEditOperation, ParcelableDraftEditOperation {
            override val pageNum: Int = 0

            override fun writeToParcel(dest: android.os.Parcel, flags: Int) {}

            override fun describeContents(): Int = 0
        }
        val both = Both()
        val result = both.toParcelable()
        assertThat(result === both).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownDraftEditOperation_toParcelable_throwsException() {
        val unknown =
            object : DraftEditOperation {
                override val pageNum: Int = 0
            }
        unknown.toParcelable()
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownParcelableAnnotation_toContent_throwsException() {
        val unknown =
            object : ParcelableAnnotation(pageNum = 0) {
                override fun writeToParcel(dest: android.os.Parcel, flags: Int) {}

                override fun describeContents(): Int = 0
            }
        unknown.toContent()
    }
}
