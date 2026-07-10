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
import android.graphics.pdf.component.PdfPagePathObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfObject as ParcelablePdfObject

/**
 * Responsible for creating [Converter] instances that can transform specific subtypes of
 * [ParcelablePdfObject] into their corresponding AOSP framework [PdfPageObject] representations and
 * vice versa.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal object PdfObjectConvertersFactory {
    // Jetpack to AOSP converters
    private val pathPdfObjectConverter = PathPdfObjectConverter()

    // AOSP to jetpack converters
    private val aospPathPdfObjectConverter = AospPathPdfObjectConverter()

    /**
     * Creates and returns a [Converter] for the given [ParcelablePdfObject].
     *
     * @param F The specific subtype of [ParcelablePdfObject] for which to create a converter.
     * @param obj The [ParcelablePdfObject] instance for which a converter is needed.
     * @return A [Converter] capable of converting the input [obj] to a [PdfPageObject].
     * @throws UnsupportedOperationException if a converter for the provided [obj] type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST", "REDUNDANT_ELSE_IN_WHEN")
    fun <F : ParcelablePdfObject> create(obj: ParcelablePdfObject): Converter<F, PdfPageObject> {
        val value =
            when (obj) {
                is PathPdfObject -> pathPdfObjectConverter
                else ->
                    throw UnsupportedOperationException(
                        "PdfObject :: ${obj.javaClass.simpleName} is not supported!"
                    )
            }

        return value as Converter<F, PdfPageObject>
    }

    /**
     * Creates and returns a [Converter] for the given [PdfPageObject].
     *
     * @param F The specific subtype of [PdfPageObject] for which to create a converter.
     * @param obj The [PdfPageObject] instance for which a converter is needed.
     * @return A [Converter] capable of converting the input [obj] to a [ParcelablePdfObject].
     * @throws UnsupportedOperationException if a converter for the provided [obj] type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST", "REDUNDANT_ELSE_IN_WHEN")
    fun <F : PdfPageObject> create(obj: PdfPageObject): Converter<F, ParcelablePdfObject> {
        val value =
            when (obj) {
                is PdfPagePathObject -> aospPathPdfObjectConverter
                else ->
                    throw UnsupportedOperationException(
                        "PdfPageObject :: ${obj.javaClass.simpleName} is not supported!"
                    )
            }

        return value as Converter<F, ParcelablePdfObject>
    }
}
