/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file    except in compliance with the License.
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

package androidx.pdf.service

import android.graphics.Bitmap
import android.graphics.pdf.PdfRendererPreV
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfPageWrapperPreV(private val page: PdfRendererPreV.Page) : PdfPageWrapper {

    override val height = page.height
    override val width = page.width

    override fun renderPage(bitmap: Bitmap) {
        page.render(bitmap, null, null, getRenderParams())
    }

    override fun renderTile(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        scaledPageWidth: Int,
        scaledPageHeight: Int
    ) {
        val transformationMatrix =
            getTransformationMatrix(
                left,
                top,
                scaledPageWidth.toFloat(),
                scaledPageHeight.toFloat(),
                width,
                height
            )
        page.render(bitmap, null, transformationMatrix, getRenderParams())
    }

    override fun getPageTextContents(): List<PdfPageTextContent> {
        return page.textContents
    }

    override fun getPageImageContents(): List<PdfPageImageContent> {
        return page.imageContents
    }

    override fun selectPageText(start: SelectionBoundary, stop: SelectionBoundary): PageSelection? {
        return page.selectContent(start, stop)
    }

    override fun searchPageText(query: String): List<PageMatchBounds> {
        return page.searchText(query)
    }

    override fun getPageLinks(): List<PdfPageLinkContent> {
        return page.linkContents
    }

    override fun getPageGotoLinks(): List<PdfPageGotoLinkContent> {
        return page.gotoLinks
    }

    override fun close() {
        page.close()
    }
}
