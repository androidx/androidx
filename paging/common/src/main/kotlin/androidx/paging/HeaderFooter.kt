/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging

import androidx.annotation.VisibleForTesting
import androidx.paging.LoadState.Done
import androidx.paging.LoadType.END
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Insert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@VisibleForTesting
internal fun <T : Any> PageEvent<T>.addHeader(item: T): PageEvent<T> {
    if (this !is Insert) return this
    if (loadType == END) return this
    if (loadStates[START] != Done) return this

    return transformPages {
        it.modifyItem(0) { page ->
            TransformablePage(
                originalPageOffset = page.originalPageOffset,
                data = mutableListOf(item).apply { addAll(page.data) },
                originalPageSize = page.originalPageSize,
                originalIndices = ArrayList<Int>(page.data.size + 1).apply {
                    add(page.originalIndices?.firstOrNull() ?: 0)

                    // Append original indices if non-null, otherwise append 0, 1, 2, ..
                    if (page.originalIndices != null) {
                        addAll(page.originalIndices)
                    } else {
                        for (i in page.data.indices) {
                            add(i)
                        }
                    }
                }
            )
        }
    }
}

@VisibleForTesting
internal fun <T : Any> PageEvent<T>.addFooter(item: T): PageEvent<T> {
    if (this !is Insert) return this
    if (loadType == START) return this
    if (loadStates[END] != Done) return this

    return transformPages {
        it.modifyItem(it.lastIndex) { page ->
            TransformablePage(
                originalPageOffset = page.originalPageOffset,
                data = page.data.toMutableList().apply { add(item) },
                originalPageSize = page.originalPageSize,
                originalIndices = ArrayList<Int>(page.data.size + 1).apply {
                    // Append original indices if non-null, otherwise append 0, 1, 2, ..
                    if (page.originalIndices != null) {
                        addAll(page.originalIndices)
                    } else {
                        for (i in page.data.indices) {
                            add(i)
                        }
                    }

                    add(
                        page.originalIndices?.lastOrNull()
                            ?: (page.originalPageSize - 1).coerceAtLeast(0)
                    )
                }
            )
        }
    }
}

private inline fun <T> List<T>.modifyItem(index: Int, block: (T) -> T) = let {
    toMutableList().apply { set(index, block(get(index))) }
}

internal fun <T : Any> Flow<PageEvent<T>>.addHeader(item: T): Flow<PageEvent<T>> {
    return map { it.addHeader(item) }
}

internal fun <T : Any> Flow<PageEvent<T>>.addFooter(item: T): Flow<PageEvent<T>> {
    return map { it.addFooter(item) }
}
