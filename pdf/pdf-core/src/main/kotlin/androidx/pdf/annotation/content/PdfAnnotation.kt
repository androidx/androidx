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

package androidx.pdf.annotation.content

/**
 * Represents an annotation on a PDF page.
 *
 * This abstract class serves as the base for different types of PDF annotations. It handles common
 * properties and behaviors.
 *
 * @param pageNum The page number (0-indexed) where this annotation is located.
 */
public abstract class PdfAnnotation internal constructor(public open val pageNum: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PdfAnnotation
        return pageNum == other.pageNum
    }

    override fun hashCode(): Int = pageNum
}
