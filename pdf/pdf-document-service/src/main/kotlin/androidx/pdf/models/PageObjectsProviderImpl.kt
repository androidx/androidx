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
package androidx.pdf.models

import android.graphics.pdf.component.PdfPageImageObject
import android.graphics.pdf.component.PdfPageObject
import android.graphics.pdf.component.PdfPagePathObject
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.PdfDocument
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.annotation.models.KeyedPdfObject
import androidx.pdf.utils.toPdfObject

/** Implementation of [PageObjectsProvider] that fetches objects for a specific page. */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class PageObjectsProviderImpl(private val documentRenderer: PdfDocumentRenderer) :
    PageObjectsProvider {
    override fun getPageObjects(pageNum: Int, types: Long): List<KeyedPdfObject> {
        return documentRenderer.withPage(pageNum) { page ->
            page.getPageObjects().mapNotNull { pair ->
                val id = pair.first
                val pageObject = pair.second
                if (pageObject.matchesTypes(types)) {
                    pageObject.toPdfObject()?.let { KeyedPdfObject(key = id.toString(), it) }
                } else {
                    null
                }
            }
        } ?: emptyList()
    }

    private fun PdfPageObject.matchesTypes(types: Long): Boolean {
        return when (this) {
            is PdfPageImageObject -> (types and PdfDocument.INCLUDE_IMAGE_PAGE_OBJECT) != 0L
            is PdfPagePathObject -> (types and PdfDocument.INCLUDE_PATH_PAGE_OBJECT) != 0L
            else -> false
        }
    }
}
