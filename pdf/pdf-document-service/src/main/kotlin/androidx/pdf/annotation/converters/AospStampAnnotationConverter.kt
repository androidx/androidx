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

package androidx.pdf.annotation.converters

import android.graphics.pdf.component.PdfPageObject
import android.graphics.pdf.component.StampAnnotation as AospStampAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.models.StampAnnotation

/** Converts a [AospStampAnnotation] to a [StampAnnotation]. */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class AospStampAnnotationConverter : Converter<AospStampAnnotation, StampAnnotation> {
    override fun convert(from: AospStampAnnotation, vararg args: Any): StampAnnotation {
        require(args.isNotEmpty() && args[0] is Int) {
            "First parameter is required to be pagenum."
        }
        val pdfObjects = mutableListOf<PdfObject>()
        for (aospPdfObject in from.objects) {
            val converter = PdfObjectConvertersFactory.create<PdfPageObject>(aospPdfObject)
            converter.convert(aospPdfObject).let { pdfObject -> pdfObjects.add(pdfObject) }
        }
        val pageNum = args[0] as Int
        return StampAnnotation(pageNum, from.bounds, pdfObjects)
    }
}
