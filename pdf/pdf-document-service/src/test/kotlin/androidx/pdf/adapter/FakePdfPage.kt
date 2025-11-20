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

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.pdf.RenderParams
import android.graphics.pdf.component.PdfAnnotation
import android.graphics.pdf.component.PdfPageObject
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.FormWidgetInfo
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.util.Pair
import androidx.pdf.annotation.models.EditOperation

class FakePdfPage(private val pageNum: Int, override val height: Int, override val width: Int) :
    PdfPage {

    internal data class AnnotationOperationRecord(
        val aospId: Int,
        val operation: EditOperation.Operation,
    )

    internal val annotationsOperation = mutableListOf<AnnotationOperationRecord>()
    val annotationsMap = mutableMapOf<Int, PdfAnnotation>()

    override fun renderPage(bitmap: Bitmap) {
        TODO("Not yet implemented")
    }

    override fun renderTile(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        scaledPageWidth: Int,
        scaledPageHeight: Int,
    ) {
        TODO("Not yet implemented")
    }

    override fun getPageTextContents(): List<PdfPageTextContent> {
        TODO("Not yet implemented")
    }

    override fun getPageImageContents(): List<PdfPageImageContent> {
        TODO("Not yet implemented")
    }

    override fun getFormWidgetInfos(): List<FormWidgetInfo> {
        TODO("Not yet implemented")
    }

    override fun getFormWidgetInfos(types: IntArray): List<FormWidgetInfo> {
        TODO("Not yet implemented")
    }

    override fun selectPageText(start: SelectionBoundary, stop: SelectionBoundary): PageSelection? {
        TODO("Not yet implemented")
    }

    override fun searchPageText(query: String): List<PageMatchBounds> {
        TODO("Not yet implemented")
    }

    override fun getPageLinks(): List<PdfPageLinkContent> {
        TODO("Not yet implemented")
    }

    override fun getPageGotoLinks(): List<PdfPageGotoLinkContent> {
        TODO("Not yet implemented")
    }

    override fun getRenderParams(): RenderParams {
        TODO("Not yet implemented")
    }

    override fun applyEdit(editRecord: FormEditRecord): List<Rect> {
        TODO("Not yet implemented")
    }

    override fun addPageObject(pageObject: PdfPageObject): Int {
        TODO("Not yet implemented")
    }

    override fun getPageObjects(): List<Pair<Int, PdfPageObject>> {
        TODO("Not yet implemented")
    }

    override fun getTopPageObjectAtPosition(
        point: PointF,
        types: IntArray,
    ): Pair<Int, PdfPageObject>? {
        TODO("Not yet implemented")
    }

    override fun updatePageObject(objectId: Int, pageObject: PdfPageObject): Boolean {
        TODO("Not yet implemented")
    }

    override fun removePageObject(objectId: Int) {
        TODO("Not yet implemented")
    }

    override fun addPageAnnotation(annotation: PdfAnnotation): Int {
        if (pageNum == -1) throw IllegalStateException()
        val fakeAospId = annotationsMap.size
        annotationsOperation.add(AnnotationOperationRecord(fakeAospId, EditOperation.Add))
        annotationsMap[fakeAospId] = annotation
        return fakeAospId
    }

    override fun getPageAnnotations(): List<Pair<Int, PdfAnnotation>> {
        return annotationsMap.map { (aospId, annotation) -> Pair(aospId, annotation) }
    }

    override fun updatePageAnnotation(annotationId: Int, annotation: PdfAnnotation): Boolean {
        if (pageNum < 0) throw IllegalStateException()
        annotationsOperation.add(AnnotationOperationRecord(annotationId, EditOperation.Update))
        annotationsMap[annotationId] = annotation
        return true
    }

    override fun removePageAnnotation(annotationId: Int) {
        if (pageNum < 0) throw IllegalStateException()
        annotationsOperation.add(AnnotationOperationRecord(annotationId, EditOperation.Remove))
        annotationsMap.remove(annotationId)
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
