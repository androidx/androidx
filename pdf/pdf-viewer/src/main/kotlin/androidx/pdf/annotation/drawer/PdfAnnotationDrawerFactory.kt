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

package androidx.pdf.annotation.drawer

import androidx.pdf.annotation.models.PdfAnnotation

/** Factory interface for creating [PdfAnnotationDrawer] instances. */
internal interface PdfAnnotationDrawerFactory {

    /**
     * Creates a [PdfAnnotationDrawer] for the given [pdfAnnotation].
     *
     * @param pdfAnnotation The PDF annotation for which to create a drawer.
     * @return A [PdfAnnotationDrawer] capable of drawing the specified annotation.
     * @throws IllegalArgumentException if the annotation type is unsupported.
     */
    fun create(pdfAnnotation: PdfAnnotation): PdfAnnotationDrawer<out PdfAnnotation>
}
