/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress("BanConcurrentHashMap")

package androidx.pdf.adapter

import java.util.concurrent.ConcurrentHashMap

/**
 * A cache for storing rendered [PdfPage] instances. This class uses a [ConcurrentHashMap] to store
 * pages, keyed by their page number.
 *
 * This cache is designed to be thread-safe, allowing concurrent access for efficient page retrieval
 * and updates.
 */
internal class PdfPageCache {
    private val cachedPageMap = ConcurrentHashMap<Int, PdfPage>()

    /**
     * Gets a [PdfPage] for the given [pageNum]. If [useCache] is true and the page is already
     * cached, the cached page is returned. Otherwise, the provided [block] is invoked to generate
     * the page, which is then cached and returned.
     *
     * @param pageNum The page number to retrieve.
     * @param useCache Whether to use the cache or generate a new page.
     * @param block A lambda function that generates a [PdfPage].
     * @return The [PdfPage] for the given page number.
     */
    fun getOrUpdate(pageNum: Int, useCache: Boolean, block: () -> PdfPage): PdfPage {
        if (!useCache) {
            return block()
        }

        return cachedPageMap[pageNum] ?: block().also { cachedPageMap[pageNum] = it }
    }

    /**
     * Removes the [PdfPage] for the given [pageNum] from the cache.
     *
     * @param pageNum The page number to remove.
     * @return The removed [PdfPage] if it was cached, otherwise null.
     */
    fun remove(pageNum: Int): PdfPage? {
        return cachedPageMap.remove(pageNum)
    }

    /** Clears all cached pages and closes them. */
    fun clearAll() {
        for (pdfPage in cachedPageMap.values) {
            pdfPage.close()
        }
    }
}
