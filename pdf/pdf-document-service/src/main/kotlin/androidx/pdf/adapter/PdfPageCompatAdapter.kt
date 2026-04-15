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

package androidx.pdf.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.component.PdfAnnotation
import android.graphics.pdf.component.PdfPageObject
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.Build
import android.util.Pair
import androidx.annotation.RequiresApi
import androidx.pdf.RenderParams

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PdfPageCompatAdapter(private val page: PdfRenderer.Page) : PdfPage {
    override val height = page.height
    override val width = page.width
    override var isClosed = false

    override fun renderPage(bitmap: Bitmap, renderParams: RenderParams) {
        page.render(
            /* destination = */ bitmap,
            /* destClip = */ null,
            /* transform = */ null,
            /* renderMode = */ PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
        )
    }

    override fun renderTile(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        scaledPageWidth: Int,
        scaledPageHeight: Int,
        renderParams: RenderParams,
    ) {
        val matrix =
            androidx.pdf.utils.getTransformationMatrix(
                left = left,
                top = top,
                scaledPageWidth = scaledPageWidth.toFloat(),
                scaledPageHeight = scaledPageHeight.toFloat(),
                pageWidth = width,
                pageHeight = height,
            )
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    }

    override fun close() {
        page.close()
    }

    // All methods below are unsupported pre-S
    override fun getPageTextContents() = throw OperationSupportedAboveSAndSdkExt13

    override fun getPageImageContents() = throw OperationSupportedAboveSAndSdkExt13

    override fun getFormWidgetInfos() = throw OperationSupportedAboveSAndSdkExt13

    override fun getFormWidgetInfos(types: IntArray) = throw OperationSupportedAboveSAndSdkExt13

    override fun selectPageText(start: SelectionBoundary, stop: SelectionBoundary): PageSelection? =
        throw OperationSupportedAboveSAndSdkExt13

    override fun searchPageText(query: String) = throw OperationSupportedAboveSAndSdkExt13

    override fun getPageLinks() = throw OperationSupportedAboveSAndSdkExt13

    override fun getPageGotoLinks() = throw OperationSupportedAboveSAndSdkExt13

    override fun applyEdit(editRecord: FormEditRecord): List<Rect> =
        throw OperationSupportedAboveSAndSdkExt18

    override fun addPageObject(pageObject: PdfPageObject): Int {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun getPageObjects(): List<Pair<Int, PdfPageObject>> {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun updatePageObject(objectId: Int, pageObject: PdfPageObject): Boolean {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun removePageObject(objectId: Int) {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun addPageAnnotation(annotation: PdfAnnotation): Int {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun getPageAnnotations(): List<Pair<Int, PdfAnnotation>> {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun updatePageAnnotation(annotationId: Int, annotation: PdfAnnotation): Boolean {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun removePageAnnotation(annotationId: Int) {
        throw OperationSupportedAboveSAndSdkExt18
    }

    override fun getTopPageObjectAtPosition(
        point: PointF,
        types: IntArray,
    ): Pair<Int, PdfPageObject>? {
        throw OperationSupportedAboveSAndSdkExt19
    }

    companion object {
        private val OperationSupportedAboveSAndSdkExt13 =
            UnsupportedOperationException("Operation supported above S + SDK extension >= 13")

        private val OperationSupportedAboveSAndSdkExt18 =
            UnsupportedOperationException("Operation supported above S + SDK extension >= 18")

        private val OperationSupportedAboveSAndSdkExt19 =
            UnsupportedOperationException("Operation supported above S + SDK extension >= 19")
    }
}
