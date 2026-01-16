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

package androidx.pdf.utils

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.component.PdfPageImageObject
import android.graphics.pdf.component.PdfPageObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.PdfObject

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal fun PdfPageObject.toPdfObject(): PdfObject? {
    return when (this) {
        is PdfPageImageObject -> {
            this.toImagePdfObject()
        }
        else -> null
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal fun PdfPageImageObject.toImagePdfObject(): ImagePdfObject {
    val matrixArray = this.matrix
    val androidMatrix = Matrix()
    androidMatrix.setValues(matrixArray)

    // Define the unit rectangle (0,0 to 1,1)
    val unitRect = RectF(0f, 0f, 1f, 1f)
    val transformedBounds = RectF()

    // mapRect transforms the src rect (unitRect) by the matrix
    // and stores the axis-aligned bounding box of the result in dst rect (transformedBounds).
    androidMatrix.mapRect(transformedBounds, unitRect)

    return ImagePdfObject(this.bitmap, transformedBounds)
}
