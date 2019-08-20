/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.paging.integration.testapp.custom

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.paging.PageLoadType
import androidx.paging.PagedSource
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

val dataSourceError = AtomicBoolean(false)

/**
 * Sample data source with artificial data.
 */
internal class ItemDataSource : PagedSource<Int, Item>() {
    override val keyProvider = KeyProvider.Positional

    override suspend fun load(params: LoadParams<Int>) = when (params.loadType) {
        PageLoadType.REFRESH -> loadInitial(params)
        else -> loadRange(params)
    }

    class RetryableItemError : Exception()

    private val mGenerationId = sGenerationId++

    private fun loadRangeInternal(startPosition: Int, loadCount: Int): List<Item> {
        val items = ArrayList<Item>()
        val end = minOf(COUNT, startPosition + loadCount)
        val bgColor = COLORS[mGenerationId % COLORS.size]

        Thread.sleep(1000)

        if (end < startPosition) {
            throw IllegalStateException()
        }
        for (i in startPosition until end) {
            items.add(Item(i, "item $i", bgColor))
        }
        if (dataSourceError.compareAndSet(true, false)) {
            throw RetryableItemError()
        }
        return items
    }

    override fun isRetryableError(error: Throwable): Boolean {
        return error is RetryableItemError
    }

    companion object {
        private const val COUNT = 60

        @ColorInt
        private val COLORS = intArrayOf(Color.RED, Color.BLUE, Color.BLACK)
        private var sGenerationId: Int = 0
    }

    // TODO: Clean up logic only pertinent to tiling.
    private fun computeStartPosition(params: LoadParams<Int>): Int {
        val requestedStartPosition = params.key?.let { key ->
            var initialPosition = key

            if (params.placeholdersEnabled) {
                // snap load size to page multiple (minimum two)
                val initialLoadSize = maxOf(params.loadSize / params.pageSize, 2) * params.pageSize

                // move start so the load is centered around the key, not starting at it
                val idealStart = initialPosition - initialLoadSize / 2
                initialPosition = maxOf(0, idealStart / params.pageSize * params.pageSize)
            } else {
                // not tiled, so don't try to snap or force multiple of a page size
                initialPosition -= params.loadSize / 2
            }

            initialPosition
        } ?: 0

        var pageStart = requestedStartPosition / params.pageSize * params.pageSize

        // maximum start pos is that which will encompass end of list
        val maximumLoadPage =
            (COUNT - params.loadSize + params.pageSize - 1) / params.pageSize * params.pageSize
        pageStart = minOf(maximumLoadPage, pageStart)

        // minimum start position is 0
        return maxOf(0, pageStart)
    }

    private fun loadInitial(params: LoadParams<Int>): LoadResult<Int, Item> {
        val position = computeStartPosition(params)
        val loadSize = minOf(COUNT - position, params.loadSize)
        val data = loadRangeInternal(position, loadSize)
        return LoadResult(
            data = data,
            itemsBefore = position,
            itemsAfter = COUNT - data.size - position
        )
    }

    private fun loadRange(params: LoadParams<Int>): LoadResult<Int, Item> {
        val data = loadRangeInternal(params.key ?: 0, params.loadSize)
        return LoadResult(data)
    }
}
