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

package androidx.pdf

import android.graphics.PointF
import android.net.Uri
import android.util.SparseArray
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.models.PdfEdit
import androidx.pdf.annotation.models.PdfEditEntry
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import java.util.UUID
import java.util.concurrent.Executor

/** Fake implementation of [EditablePdfDocument] for testing. */
internal class FakeEditablePdfDocument(
    override val uri: Uri,
    override val pageCount: Int,
    override val isLinearized: Boolean = false,
    override val renderParams: RenderParams = RenderParams(RenderParams.RENDER_MODE_FOR_DISPLAY),
    override val formType: Int = -1,
) : EditablePdfDocument() {
    private val annotationsByPage = mutableMapOf<Int, MutableList<PdfEditEntry<out PdfEdit>>>()

    val getAnnotationsForPageCallCount = mutableMapOf<Int, Int>()

    fun addAnnotationToPage(pageNum: Int, annotation: PdfAnnotation) {
        val editId = EditId(pageNum, UUID.randomUUID().toString())
        val data = PdfAnnotationData(editId, annotation)
        annotationsByPage.getOrPut(pageNum) { mutableListOf() }.add(data)
    }

    override fun addOnPdfContentInvalidatedListener(
        executor: Executor,
        listener: PdfDocument.OnPdfContentInvalidatedListener,
    ) {
        TODO("Not yet implemented")
    }

    override fun removeOnPdfContentInvalidatedListener(
        listener: PdfDocument.OnPdfContentInvalidatedListener
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun applyEdits(editsDraft: EditsDraft): List<String> {
        TODO("Not yet implemented")
    }

    override fun createWriteHandle(): PdfWriteHandle {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfo(pageNumber: Int, pageInfoFlags: Long): PdfDocument.PageInfo {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: Long,
    ): List<PdfDocument.PageInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> {
        TODO("Not yet implemented")
    }

    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection? {
        TODO("Not yet implemented")
    }

    override suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection? {
        TODO("Not yet implemented")
    }

    override suspend fun getPageContent(pageNumber: Int): PdfDocument.PdfPageContent? {
        TODO("Not yet implemented")
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        TODO("Not yet implemented")
    }

    override suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> {
        TODO("Not yet implemented")
    }

    override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
        TODO("Not yet implemented")
    }

    override suspend fun getFormWidgetInfos(pageNum: Int, types: Long): List<FormWidgetInfo> {
        TODO("Not yet implemented")
    }

    override suspend fun getTopPageObjectAtPosition(pageNum: Int, point: PointF): PdfObject? {
        return null
    }

    override suspend fun applyEdit(record: FormEditInfo) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}
