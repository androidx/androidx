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

package androidx.pdf.annotation

import android.graphics.pdf.component.PdfAnnotation
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.annotation.converters.PdfAnnotationConvertersFactory

/** Implementation of [PageAnnotationsProvider] that fetches annotations for a specific page. */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class PageAnnotationsProviderImpl(private val documentRenderer: PdfDocumentRenderer) :
    PageAnnotationsProvider {
    override fun getPageAnnotations(pageNum: Int): List<KeyedPdfAnnotation> {
        return documentRenderer.withPage(pageNum) { page ->
            val pageAnnotations = page.getPageAnnotations()
            val result =
                pageAnnotations.map { annotationData ->
                    val aospId = annotationData.first.toString()
                    val aospAnnotation = annotationData.second
                    val converter =
                        PdfAnnotationConvertersFactory.create<PdfAnnotation>(aospAnnotation)
                    val jetpackAnnotation = converter.convert(aospAnnotation, pageNum)

                    KeyedPdfAnnotation(key = aospId, jetpackAnnotation)
                }
            return@withPage result
        } ?: emptyList()
    }
}
