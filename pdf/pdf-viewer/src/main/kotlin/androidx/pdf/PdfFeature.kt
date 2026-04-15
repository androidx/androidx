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

package androidx.pdf

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmInline

/**
 * Represents a specific feature supported by a PDF document. New features may be added in future
 * versions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@JvmInline
public value class PdfFeature private constructor(public val value: Int) {
    public companion object {
        public val TEXT_SELECTION: PdfFeature = PdfFeature(0)
        public val SEARCH: PdfFeature = PdfFeature(1)
        public val FORM_FILLING: PdfFeature = PdfFeature(2)
        public val ANNOTATIONS: PdfFeature = PdfFeature(3)
        public val LINKS: PdfFeature = PdfFeature(4)
        public val TEXT_EXTRACTION: PdfFeature = PdfFeature(5)
        public val IMAGE_EXTRACTION: PdfFeature = PdfFeature(6)
    }

    override fun toString(): String {
        return when (this) {
            TEXT_SELECTION -> "TEXT_SELECTION"
            SEARCH -> "SEARCH"
            FORM_FILLING -> "FORM_FILLING"
            ANNOTATIONS -> "ANNOTATIONS"
            LINKS -> "LINKS"
            TEXT_EXTRACTION -> "TEXT_EXTRACTION"
            IMAGE_EXTRACTION -> "IMAGE_EXTRACTION"
            else -> "UNKNOWN"
        }
    }
}
