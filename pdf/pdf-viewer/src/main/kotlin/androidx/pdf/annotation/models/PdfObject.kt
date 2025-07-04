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

package androidx.pdf.annotation.models

import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * This sealed interface represents specific types of PDF objects, such as [PathPdfObject]. It is
 * also [Parcelable] to allow for seamless communication between different android components.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public sealed interface PdfObject : Parcelable {

    /** Companion object holding constants related to [PdfObject] types. */
    public companion object {
        /** Constant representing an unknown PDF object type. Used as a default or error value. */
        public const val TYPE_UNKNOWN: Int = 0

        /**
         * Constant representing a path PDF object type.
         *
         * @see PathPdfObject
         */
        public const val TYPE_PATH_PDF_OBJECT: Int = 1
    }

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0
}
