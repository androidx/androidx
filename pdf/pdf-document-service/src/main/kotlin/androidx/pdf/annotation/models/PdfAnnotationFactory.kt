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

package androidx.pdf.annotation.models

import android.os.Parcel

/**
 * Factory for creating [PdfAnnotation] instances from a [Parcel].
 *
 * This factory handles polymorphic un-parceling of different annotation types.
 */
internal object PdfAnnotationFactory {
    /**
     * Creates a [PdfAnnotation] instance from the given [Parcel].
     *
     * @param parcel The parcel to read from.
     * @return The created [PdfAnnotation], or null if the type is unknown.
     */
    internal fun createFromParcel(parcel: Parcel): PdfAnnotation? {
        return when (val type = parcel.readInt()) {
            StampAnnotation.TYPE -> StampAnnotation(parcel)
            HighlightAnnotation.TYPE -> HighlightAnnotation(parcel)
            else -> null
        }
    }
}
