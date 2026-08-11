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

package androidx.pdf.signature.model

import android.graphics.Bitmap
import android.graphics.Path
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Config.TARGET_SDK])
@RunWith(RobolectricTestRunner::class)
class SignatureTest {

    private val defaultId = "test_sig_id"
    private val defaultPageNum = 1
    private val defaultX = 100f
    private val defaultY = 200f
    private val defaultWidth = 300f
    private val defaultHeight = 150f

    @Test
    fun drawnSignature_setSelection_updatesSelectionCorrectly() {
        val path =
            Path().apply {
                moveTo(10f, 20f)
                lineTo(30f, 40f)
            }

        val signature =
            Signature.DrawnSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = false,
                drawnPath = path,
            )

        val selectedSignature = signature.setSelection(true) as Signature.DrawnSignature

        assertThat(selectedSignature.isSelected).isTrue()
        assertThat(selectedSignature.id).isEqualTo(signature.id)
        assertThat(selectedSignature.drawnPath).isEqualTo(signature.drawnPath)
    }

    @Test
    fun drawnSignature_updateBounds_updatesBoundsCorrectly() {
        val path = Path().apply { moveTo(10f, 20f) }
        val signature =
            Signature.DrawnSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = false,
                drawnPath = path,
            )

        val newPageNum = 2
        val newX = 10f
        val newY = 20f
        val newWidth = 30f
        val newHeight = 40f

        val updatedSignature =
            signature.updateBounds(
                pageNum = newPageNum,
                xCoord = newX,
                yCoord = newY,
                width = newWidth,
                height = newHeight,
            ) as Signature.DrawnSignature

        assertThat(updatedSignature.pageNum).isEqualTo(newPageNum)
        assertThat(updatedSignature.xCoord).isEqualTo(newX)
        assertThat(updatedSignature.yCoord).isEqualTo(newY)
        assertThat(updatedSignature.width).isEqualTo(newWidth)
        assertThat(updatedSignature.height).isEqualTo(newHeight)

        // Verify unchanged fields
        assertThat(updatedSignature.id).isEqualTo(signature.id)
        assertThat(updatedSignature.isSelected).isEqualTo(signature.isSelected)
        assertThat(updatedSignature.drawnPath).isEqualTo(signature.drawnPath)
    }

    @Test
    fun drawnSignature_equalsAndHashCode() {
        val path = Path().apply { moveTo(1f, 1f) }

        val sig1 = Signature.DrawnSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, path)
        val sig2 = Signature.DrawnSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, path)
        val sigDifferent = Signature.DrawnSignature("other", 0, 0f, 0f, 10f, 10f, false, path)

        // Test Equals
        assertThat(sig1).isEqualTo(sig2)
        assertThat(sig1).isNotEqualTo(sigDifferent)

        // Test HashCode
        assertThat(sig1.hashCode()).isEqualTo(sig2.hashCode())
        assertThat(sig1.hashCode()).isNotEqualTo(sigDifferent.hashCode())
    }

    @Test
    fun typedSignature_setSelection_updatesSelectionCorrectly() {
        val signature =
            Signature.TypedSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = true,
                typedText = "Jane Doe",
                typedFont = Signature.TypedSignature.FONT_SERIF,
            )

        val deselectedSignature = signature.setSelection(false) as Signature.TypedSignature

        assertThat(deselectedSignature.isSelected).isFalse()
        assertThat(deselectedSignature.typedText).isEqualTo(signature.typedText)
        assertThat(deselectedSignature.typedFont).isEqualTo(signature.typedFont)
    }

    @Test
    fun typedSignature_updateBounds_updatesBoundsCorrectly() {
        val signature =
            Signature.TypedSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = true,
                typedText = "Jane Doe",
                typedFont = Signature.TypedSignature.FONT_SERIF,
            )

        val newPageNum = 3
        val newX = 50f
        val newY = 60f
        val newWidth = 70f
        val newHeight = 80f

        val updatedSignature =
            signature.updateBounds(
                pageNum = newPageNum,
                xCoord = newX,
                yCoord = newY,
                width = newWidth,
                height = newHeight,
            ) as Signature.TypedSignature

        assertThat(updatedSignature.pageNum).isEqualTo(newPageNum)
        assertThat(updatedSignature.xCoord).isEqualTo(newX)
        assertThat(updatedSignature.yCoord).isEqualTo(newY)
        assertThat(updatedSignature.width).isEqualTo(newWidth)
        assertThat(updatedSignature.height).isEqualTo(newHeight)

        // Verify unchanged fields
        assertThat(updatedSignature.id).isEqualTo(signature.id)
        assertThat(updatedSignature.isSelected).isEqualTo(signature.isSelected)
        assertThat(updatedSignature.typedText).isEqualTo(signature.typedText)
        assertThat(updatedSignature.typedFont).isEqualTo(signature.typedFont)
    }

    @Test
    fun typedSignature_equalsAndHashCode() {
        val sig1 =
            Signature.TypedSignature(
                defaultId,
                0,
                0f,
                0f,
                10f,
                10f,
                false,
                "Text",
                Signature.TypedSignature.FONT_MONOSPACE,
            )
        val sig2 =
            Signature.TypedSignature(
                defaultId,
                0,
                0f,
                0f,
                10f,
                10f,
                false,
                "Text",
                Signature.TypedSignature.FONT_MONOSPACE,
            )
        val sigDifferent =
            Signature.TypedSignature(
                defaultId,
                0,
                0f,
                0f,
                10f,
                10f,
                false,
                "Other Text",
                Signature.TypedSignature.FONT_MONOSPACE,
            )

        // Test Equals
        assertThat(sig1).isEqualTo(sig2)
        assertThat(sig1).isNotEqualTo(sigDifferent)

        // Test HashCode
        assertThat(sig1.hashCode()).isEqualTo(sig2.hashCode())
        assertThat(sig1.hashCode()).isNotEqualTo(sigDifferent.hashCode())
    }

    @Test
    fun uploadedSignature_setSelection_updatesSelectionCorrectly() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val signature =
            Signature.UploadedSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = false,
                imageBitmap = bitmap,
            )

        val selectedSignature = signature.setSelection(true) as Signature.UploadedSignature

        assertThat(selectedSignature.isSelected).isTrue()
        assertThat(selectedSignature.imageBitmap.sameAs(signature.imageBitmap)).isTrue()
    }

    @Test
    fun uploadedSignature_updateBounds_updatesBoundsCorrectly() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val signature =
            Signature.UploadedSignature(
                id = defaultId,
                pageNum = defaultPageNum,
                xCoord = defaultX,
                yCoord = defaultY,
                width = defaultWidth,
                height = defaultHeight,
                isSelected = false,
                imageBitmap = bitmap,
            )

        val newPageNum = 4
        val newX = 200f
        val newY = 300f
        val newWidth = 400f
        val newHeight = 500f

        val updatedSignature =
            signature.updateBounds(
                pageNum = newPageNum,
                xCoord = newX,
                yCoord = newY,
                width = newWidth,
                height = newHeight,
            ) as Signature.UploadedSignature

        assertThat(updatedSignature.pageNum).isEqualTo(newPageNum)
        assertThat(updatedSignature.xCoord).isEqualTo(newX)
        assertThat(updatedSignature.yCoord).isEqualTo(newY)
        assertThat(updatedSignature.width).isEqualTo(newWidth)
        assertThat(updatedSignature.height).isEqualTo(newHeight)

        // Verify unchanged fields
        assertThat(updatedSignature.id).isEqualTo(signature.id)
        assertThat(updatedSignature.isSelected).isEqualTo(signature.isSelected)
        assertThat(updatedSignature.imageBitmap.sameAs(signature.imageBitmap)).isTrue()
    }

    @Test
    fun uploadedSignature_equalsAndHashCode() {
        val bitmap1 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val bitmap2 = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888) // Identical empty bitmap
        val bitmapDifferent = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888) // Different size

        val sig1 = Signature.UploadedSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, bitmap1)
        val sig2 = Signature.UploadedSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, bitmap2)
        val sigDifferent =
            Signature.UploadedSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, bitmapDifferent)

        // Test Equals
        assertThat(sig1).isEqualTo(sig2)
        assertThat(sig1).isNotEqualTo(sigDifferent)

        // Test HashCode
        val sigSameRef = Signature.UploadedSignature(defaultId, 0, 0f, 0f, 10f, 10f, false, bitmap1)
        assertThat(sigSameRef.hashCode()).isEqualTo(sig1.hashCode())
    }
}
