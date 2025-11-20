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

package androidx.pdf.service

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.FormWidgetInfo
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.ParcelFileDescriptor
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.JetpackAospIdPair
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PaginatedAnnotations
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.models.Dimensions

class FakePdfDocumentRemote : PdfDocumentRemote.Stub() {
    override fun openPdfDocument(pfd: ParcelFileDescriptor?, password: String?): Int {
        TODO("Not yet implemented")
    }

    override fun numPages(): Int {
        TODO("Not yet implemented")
    }

    override fun getPageDimensions(pageNum: Int): Dimensions? {
        TODO("Not yet implemented")
    }

    override fun getPageBitmap(pageNum: Int, width: Int, height: Int): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun getTileBitmap(
        pageNum: Int,
        tilewidth: Int,
        tileHeight: Int,
        pageWidth: Int,
        pageHeight: Int,
        offsetX: Int,
        offsetY: Int,
    ): Bitmap? {
        TODO("Not yet implemented")
    }

    override fun getPageText(pageNum: Int): List<PdfPageTextContent?>? {
        TODO("Not yet implemented")
    }

    override fun searchPageText(pageNum: Int, query: String?): List<PageMatchBounds?>? {
        TODO("Not yet implemented")
    }

    override fun selectPageText(
        pageNum: Int,
        start: SelectionBoundary?,
        stop: SelectionBoundary?,
    ): PageSelection? {
        TODO("Not yet implemented")
    }

    override fun getPageExternalLinks(pageNum: Int): List<PdfPageLinkContent?>? {
        TODO("Not yet implemented")
    }

    override fun getPageGotoLinks(pageNum: Int): List<PdfPageGotoLinkContent?>? {
        TODO("Not yet implemented")
    }

    override fun getPageImageContent(pageNum: Int): List<PdfPageImageContent?>? {
        TODO("Not yet implemented")
    }

    override fun isPdfLinearized(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFormType(): Int {
        TODO("Not yet implemented")
    }

    override fun releasePage(pageNum: Int) {
        TODO("Not yet implemented")
    }

    override fun closePdfDocument() {
        TODO("Not yet implemented")
    }

    override fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo?>? {
        TODO("Not yet implemented")
    }

    override fun getFormWidgetInfosOfType(pageNum: Int, types: IntArray?): List<FormWidgetInfo?>? {
        TODO("Not yet implemented")
    }

    override fun applyEdit(pageNum: Int, editRecord: FormEditRecord?): List<Rect?>? {
        TODO("Not yet implemented")
    }

    override fun write(destination: ParcelFileDescriptor?, removePasswordProtection: Boolean) {
        TODO("Not yet implemented")
    }

    override fun addAnnotations(pfd: ParcelFileDescriptor?): AnnotationResult? {
        TODO("Not yet implemented")
    }

    override fun getPageAnnotations(pageNum: Int): List<PdfAnnotation?>? {
        TODO("Not yet implemented")
    }

    override fun applyEdits(annots: List<PdfAnnotationData>): AnnotationResult {
        val (success, failures) = annots.partition { it.editId.pageNum >= 0 }
        return AnnotationResult(success, failures.map { it.annotation })
    }

    override fun addEdit(annots: List<PdfAnnotationData>): AddEditResult {
        val (success, failures) = annots.partition { it.editId.pageNum >= 0 }
        return AddEditResult(
            success.map { JetpackAospIdPair(it.editId, it.editId) },
            failures.map { it.editId },
        )
    }

    override fun updateEdit(annots: List<PdfAnnotationData>): ModifyEditResult {
        val (success, failures) = annots.partition { it.editId.pageNum >= 0 }
        return ModifyEditResult(success.map { it.editId }, failures.map { it.editId })
    }

    override fun removeEdit(editIds: List<EditId>): ModifyEditResult {
        val (success, failures) = editIds.partition { it.pageNum >= 0 }
        return ModifyEditResult(success.map { it }, failures.map { it })
    }

    override fun getAllPageAnnotations(pageNum: Int): PaginatedAnnotations? {
        TODO("Not yet implemented")
    }

    override fun getBatchedPageAnnotations(pageNum: Int, batchIndex: Int): PaginatedAnnotations? {
        TODO("Not yet implemented")
    }
}
