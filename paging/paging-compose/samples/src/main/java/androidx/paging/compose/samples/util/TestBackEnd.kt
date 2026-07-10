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

package androidx.paging.compose.samples.util

import kotlin.math.ceil

internal class TestBackend<V : Any>(
    private val backendDataList: List<Int>,
    private val loadDelay: Long = 1000,
    private val transform: (Int) -> V,
) {
    val pageSize = 5
    val maxSize = backendDataList.size

    class DesiredLoadResultPageResponse(val data: List<Int>)

    /** Returns [DataBatchSize] items for a key */
    fun getItemsByKey(key: Int, pageSize: Int): DesiredLoadResultPageResponse {
        val maxKey = ceil(backendDataList.size.toFloat() / pageSize).toInt()

        if (key >= maxKey) {
            return DesiredLoadResultPageResponse(emptyList())
        }

        val from = key * pageSize
        val to = minOf((key + 1) * pageSize, backendDataList.size)
        val currentSublist = backendDataList.subList(from, to)

        return DesiredLoadResultPageResponse(currentSublist)
    }

    fun getPagingSource() = TestPagingSource(this, loadDelay, transform)
}
