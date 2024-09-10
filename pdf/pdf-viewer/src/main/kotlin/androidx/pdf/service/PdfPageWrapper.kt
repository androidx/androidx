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
import android.graphics.Matrix
import android.graphics.pdf.RenderParams
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal interface PdfPageWrapper : AutoCloseable {

    val height: Int
    val width: Int

    fun renderPage(bitmap: Bitmap)

    fun renderTile(bitmap: Bitmap, left: Int, top: Int, scaledPageWidth: Int, scaledPageHeight: Int)

    fun getPageTextContents(): List<PdfPageTextContent>

    fun getPageImageContents(): List<PdfPageImageContent>

    fun selectPageText(start: SelectionBoundary, stop: SelectionBoundary): PageSelection?

    fun searchPageText(query: String): List<PageMatchBounds>

    fun getPageLinks(): List<PdfPageLinkContent>

    fun getPageGotoLinks(): List<PdfPageGotoLinkContent>

    fun getTransformationMatrix(
        left: Int,
        top: Int,
        scaledPageWidth: Float,
        scaledPageHeight: Float,
        pageWidth: Int,
        pageHeight: Int
    ): Matrix {
        return Matrix().apply {
            setScale(scaledPageWidth / pageWidth, scaledPageHeight / pageHeight)
            postTranslate(-left.toFloat(), -top.toFloat())
        }
    }

    fun getRenderParams(): RenderParams {
        return RenderParams.Builder(RenderParams.RENDER_MODE_FOR_DISPLAY)
            .setRenderFlags(
                RenderParams.FLAG_RENDER_HIGHLIGHT_ANNOTATIONS or
                    RenderParams.FLAG_RENDER_TEXT_ANNOTATIONS
            )
            .build()
    }
}
