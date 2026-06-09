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
 * Factory for creating [PdfObject] instances from a [Parcel].
 *
 * This factory handles polymorphic un-parceling of different PDF object types.
 */
internal object PdfObjectFactory {
    /**
     * Creates a [PdfObject] instance from the given [Parcel].
     *
     * @param parcel The parcel to read from.
     * @return The created [PdfObject], or null if the type is unknown.
     */
    internal fun createFromParcel(parcel: Parcel): PdfObject? {
        val type = parcel.readInt()
        return when (type) {
            PathPdfObject.TYPE -> PathPdfObject(parcel)
            ImagePdfObject.TYPE -> ImagePdfObject(parcel)
            // TODO: Add other pdf object types here
            else -> null
        }
    }
}
