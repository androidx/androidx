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

package androidx.pdf.ocr

import android.os.RemoteException
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument
import androidx.pdf.annotation.content.ImagePdfObject
import androidx.pdf.util.ExceptionUtils.isHandledRemoteException
import androidx.pdf.util.bitmapSize
import java.util.Collections
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository responsible for managing [OcrContext] for PDF pages. Handles fetching OCR results from
 * [OcrProvider] and caching them for subsequent use.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class OcrContextRepository(
    private val pdfDocument: PdfDocument,
    private val ocrProvider: OcrProvider,
) {
    private val pageOcrCache: MutableMap<Int, List<OcrContext>> =
        Collections.synchronizedMap(HashMap())
    private val mutexPool = Array(MAX_MUTEX_POOL_SIZE) { Mutex() }

    /**
     * Retrieves [OcrContext] for the specified page. Returns cached results if available, otherwise
     * performs OCR and caches the results.
     */
    public suspend fun getOcrContexts(pageNum: Int): List<OcrContext> {
        pageOcrCache[pageNum]?.let {
            return it
        }

        val lockIndex = abs(pageNum % mutexPool.size)
        val lock = mutexPool[lockIndex]

        return lock.withLock {
            if (pageOcrCache[pageNum] == null) {
                val keyedPdfObjects =
                    try {
                        pdfDocument.getPageObjects(pageNum, PdfDocument.INCLUDE_IMAGE_PAGE_OBJECT)
                    } catch (e: RemoteException) {
                        if (!e.isHandledRemoteException) throw e
                        // Gracefully recover from known remote failures (e.g., service crashes or
                        // IPC call rejection).
                        return@withLock emptyList()
                    }

                val contexts = mutableListOf<OcrContext>()
                for (keyedObject in keyedPdfObjects) {
                    val imageObject = keyedObject.pdfObject as? ImagePdfObject ?: continue
                    val ocrResult = ocrProvider.recognizeText(imageObject.bitmap) ?: continue

                    contexts.add(
                        OcrContext(ocrResult, pageNum, imageObject.bounds, imageObject.bitmapSize)
                    )
                }

                pageOcrCache[pageNum] = contexts
            }
            pageOcrCache[pageNum] ?: emptyList()
        }
    }

    private companion object {
        private const val MAX_MUTEX_POOL_SIZE = 2
    }
}
