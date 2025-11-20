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

package androidx.pdf.annotation.processor

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.pdf.annotation.PageAnnotationsProvider
import androidx.pdf.annotation.models.PaginatedAnnotations
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor.Companion.unflatten

/**
 * Responsible for fetching and managing annotations for a specific page of a PDF document.
 *
 * @property pageNum The 0-based index of the page for which to fetch annotations.
 * @property annotationsProvider The [PageAnnotationsProvider] to fetch the annotations.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
internal class PageAnnotationsPaginator(
    internal val pageNum: Int,
    private val annotationsProvider: PageAnnotationsProvider,
) {

    private val allAnnotationBatches: List<List<PdfAnnotationData>> by lazy {
        annotationsProvider
            .getPageAnnotations(pageNum)
            .unflatten(BatchPdfAnnotationsProcessor.MAX_BATCH_SIZE_IN_BYTES)
    }

    fun getPageAnnotations(nextBatchIndex: Int = 0): PaginatedAnnotations? {
        return try {
            val batches = allAnnotationBatches
            if (batches.isEmpty() || nextBatchIndex >= batches.size) {
                null
            } else {
                PaginatedAnnotations(
                    annotations = batches[nextBatchIndex],
                    currentBatchIndex = nextBatchIndex,
                    totalBatchCount = batches.size,
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
