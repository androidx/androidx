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

package androidx.pdf.annotation.content

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.pdf.Dimension
import androidx.pdf.PdfRect
import androidx.pdf.selection.model.ImageSelection

/**
 * Represents an image object within a PDF document.
 *
 * @property bitmap The [Bitmap] data of the image.
 * @property bounds The rectangular boundaries of its position and size on the PDF page.
 */
public class ImagePdfObject(public val bitmap: Bitmap, public val bounds: RectF) : PdfObject {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ImagePdfObject) return false
        return bitmap == other.bitmap && bounds == other.bounds
    }

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }
}

internal fun ImagePdfObject.toImageSelection(pageNum: Int): ImageSelection =
    ImageSelection(bitmap, PdfRect(pageNum, bounds.left, bounds.top, bounds.right, bounds.bottom))

internal val ImagePdfObject.bitmapSize: Dimension
    get() = Dimension(bitmap.width, bitmap.height)
