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
 * Associates a [PdfAnnotation] with a unique key.
 *
 * @property key The unique string identifier for the annotation.
 * @property annotation The [PdfAnnotation] object.
 */
public class KeyedPdfAnnotation(public val key: String, public val annotation: PdfAnnotation) {
    override fun equals(other: Any?): Boolean {
        return (other is KeyedPdfAnnotation) && other.key == key && other.annotation == annotation
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + annotation.hashCode()
        return result
    }
}
