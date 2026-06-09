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

import android.graphics.pdf.component.HighlightAnnotation as AospHighlightAnnotation
import android.graphics.pdf.component.PdfAnnotation as AospPdfAnnotation
import android.graphics.pdf.component.StampAnnotation as AospStampAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.Converter
import androidx.pdf.annotation.models.HighlightAnnotation as ParcelableHighlightAnnotation
import androidx.pdf.annotation.models.PdfAnnotation as ParcelablePdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation as ParcelableStampAnnotation

/**
 * Responsible for creating [Converter] instances that can transform specific subtypes of
 * [ParcelablePdfAnnotation] into their corresponding AOSP framework [AospPdfAnnotation]
 * representations and vice-versa.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal object PdfAnnotationConvertersFactory {
    // Jetpack to AOSP converters
    private val stampAnnotationConverter = StampAnnotationConverter()

    // AOSP to jetpack converters
    private val aospStampAnnotationConverter = AospStampAnnotationConverter()

    private val aospHighlightAnnotationConverter = AospHighlightAnnotationConverter()
    private val highlightAnnotationConverter = HighlightAnnotationConverter()

    /**
     * Creates and returns a [Converter] for the given [ParcelablePdfAnnotation].
     *
     * @param F The specific subtype of [ParcelablePdfAnnotation] for which to create a converter.
     * @param annot The [ParcelablePdfAnnotation] instance for which a converter is needed.
     * @return A [Converter] capable of converting the input [annot] to an [AospPdfAnnotation].
     * @throws UnsupportedOperationException if a converter for the provided [annot] type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST")
    fun <F : ParcelablePdfAnnotation> create(
        annot: ParcelablePdfAnnotation
    ): Converter<F, AospPdfAnnotation> {
        val value =
            when (annot) {
                is ParcelableStampAnnotation -> stampAnnotationConverter
                is ParcelableHighlightAnnotation -> highlightAnnotationConverter
                else ->
                    throw UnsupportedOperationException(
                        "PdfAnnotation :: ${annot.javaClass.simpleName} is not supported!"
                    )
            }
        return value as Converter<F, AospPdfAnnotation>
    }

    /**
     * Creates and returns a [Converter] for the given [AospPdfAnnotation].
     *
     * @param F The specific subtype of [AospPdfAnnotation] for which to create a converter.
     * @param annot The [AospPdfAnnotation] instance for which a converter is needed.
     * @return A [Converter] capable of converting the input [annot] to an
     *   [ParcelablePdfAnnotation].
     * @throws UnsupportedOperationException if a converter for the provided [annot] type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST")
    fun <F : AospPdfAnnotation> create(
        annot: AospPdfAnnotation
    ): Converter<F, ParcelablePdfAnnotation> {
        val value =
            when (annot) {
                is AospStampAnnotation -> aospStampAnnotationConverter
                is AospHighlightAnnotation -> {
                    aospHighlightAnnotationConverter
                }
                else ->
                    throw UnsupportedOperationException(
                        "PdfAnnotation :: ${annot.javaClass.simpleName} is not supported!"
                    )
            }
        return value as Converter<F, ParcelablePdfAnnotation>
    }
}
