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

import android.graphics.pdf.component.StampAnnotation as AospStampAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.models.StampAnnotation

/** Converts a [StampAnnotation] to a [AospStampAnnotation]. */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class StampAnnotationConverter : Converter<StampAnnotation, AospStampAnnotation> {
    override fun convert(from: StampAnnotation, vararg args: Any): AospStampAnnotation {
        val aospStampAnnotation = AospStampAnnotation(from.bounds)
        for (pdfObject in from.pdfObjects) {
            val converter = PdfObjectConvertersFactory.create<PdfObject>(pdfObject)
            aospStampAnnotation.addObject(converter.convert(pdfObject))
        }
        return aospStampAnnotation
    }
}
