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

package androidx.pdf.annotation.repository

import android.os.RemoteException
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.util.ExceptionUtils.isHandledRemoteException
import java.util.Collections
import kotlin.math.abs
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A concrete implementation of [AnnotationsRepository] that retrieves data directly from a
 * [PdfDocument].
 *
 * **Concurrency & Performance:**
 * - **Caching:** Results are stored in a thread-safe [Collections.synchronizedMap].
 * - **Lock Striping:** Uses a fixed pool of [Mutex] locks (based on page number modulo) to allow
 *   multiple pages to be fetched in parallel, while preventing the same page from being processed
 *   concurrently by different threads.
 *
 * @param document The source [PdfDocument] from which to parse annotations.
 */
internal class PdfDocumentAnnotationsRepository(private val document: PdfDocument) :
    AnnotationsRepository {
    private val cachedAnnotationsPerPage: MutableMap<Int, List<KeyedPdfAnnotation>> =
        Collections.synchronizedMap(HashMap())

    private val mutexPool = Array(MAX_MUTEX_POOL_SIZE) { Mutex() }

    override suspend fun getAnnotation(pageNum: Int, annotationId: String): KeyedPdfAnnotation? {
        val annotationsPerPage = getAnnotationsForPage(pageNum)

        // TODO: Can be optimized by maintaining a separate map from id -> index
        return annotationsPerPage.find { it.key == annotationId }
    }

    override suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> {
        val cachedData = cachedAnnotationsPerPage[pageNum]
        if (cachedData != null) {
            return cachedData
        }

        val lockIndex = abs(pageNum % mutexPool.size)
        val lock = mutexPool[lockIndex]

        return lock.withLock {
            // After acquiring the lock, another coroutine might have
            // already fetched the data while this one was waiting. This check
            // prevents a redundant fetch.
            if (cachedAnnotationsPerPage[pageNum] == null) {
                try {
                    cachedAnnotationsPerPage[pageNum] = document.getAnnotationsForPage(pageNum)
                } catch (e: RemoteException) {
                    if (!e.isHandledRemoteException) throw e
                    // Gracefully recover from known remote failures (e.g., service crashes or
                    // IPC call rejection) by treating annotations as empty for this page.
                }
            }
            cachedAnnotationsPerPage[pageNum] ?: emptyList()
        }
    }

    override fun clear() {
        cachedAnnotationsPerPage.clear()
    }

    @VisibleForTesting fun isCacheEmpty(): Boolean = cachedAnnotationsPerPage.isEmpty()

    private companion object {
        private const val MAX_MUTEX_POOL_SIZE = 16
    }
}
