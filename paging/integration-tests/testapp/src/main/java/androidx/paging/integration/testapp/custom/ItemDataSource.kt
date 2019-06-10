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
import androidx.paging.PositionalDataSource
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean

val dataSourceError = AtomicBoolean(false)
/**
 * Sample data source with artificial data.
 */
internal class ItemDataSource : PositionalDataSource<Item>() {
    class RetryableItemError : Exception()

    private val mGenerationId = sGenerationId++

    private fun loadRangeInternal(startPosition: Int, loadCount: Int): List<Item>? {
        val items = ArrayList<Item>()
        val end = Math.min(COUNT, startPosition + loadCount)
        val bgColor = COLORS[mGenerationId % COLORS.size]

        Thread.sleep(1000)

        if (end < startPosition) {
            throw IllegalStateException()
        }
        for (i in startPosition until end) {
            items.add(Item(i, "item $i", bgColor))
        }
        if (dataSourceError.compareAndSet(true, false)) {
            return null
        }
        return items
    }

    companion object {
        private const val COUNT = 60

        @ColorInt
        private val COLORS = intArrayOf(Color.RED, Color.BLUE, Color.BLACK)

        private var sGenerationId: Int = 0
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<Item>) {
        val position = computeInitialLoadPosition(params, COUNT)
        val loadSize = computeInitialLoadSize(params, position, COUNT)
        val data = loadRangeInternal(position, loadSize)
        if (data == null) {
            callback.onError(RetryableItemError())
        } else {
            callback.onResult(data, position, COUNT)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<Item>) {
        val data = loadRangeInternal(params.startPosition, params.loadSize)
        if (data == null) {
            callback.onError(RetryableItemError())
        } else {
            callback.onResult(data)
        }
    }

    override fun isRetryableError(error: Throwable): Boolean {
        return error is RetryableItemError
    }
}
