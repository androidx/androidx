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

import android.graphics.pdf.component.PdfPagePathObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.utils.getPathInputsFromPath

/** Converts a [PdfPagePathObject] to a AOSP [PathPdfObject]. */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class AospPathPdfObjectConverter : Converter<PdfPagePathObject, PathPdfObject> {
    override fun convert(from: PdfPagePathObject, vararg args: Any): PathPdfObject {
        val pathInputs = from.toPath().getPathInputsFromPath()
        return PathPdfObject(
            brushColor = from.fillColor,
            brushWidth = from.strokeWidth,
            inputs = pathInputs,
        )
    }
}
