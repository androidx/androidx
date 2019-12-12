/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.START
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import androidx.paging.PageEvent.StateUpdate

/**
 * Create a TransformablePage with separators inside (ignoring edges)
 *
 * Separators between pages are handled outside of the page, see [PageEvent.insertSeparators].
 */
private fun <R : Any, T : R> TransformablePage<T>.insertSeparators(
    transform: (T?, T?) -> R?
): TransformablePage<R> {
    if (data.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        return this as TransformablePage<R>
    }

    val initialCapacity = data.size + 4 // extra space to avoid bigger allocations
    val outputList = ArrayList<R>(initialCapacity)
    val outputIndices = ArrayList<Int>(initialCapacity)

    outputList.add(data.first())
    outputIndices.add(originalIndices?.first() ?: 0)
    for (i in 1 until data.size) {
        val item = data[i]
        val separator = transform(data[i - 1], item)
        if (separator != null) {
            outputList.add(separator)
            outputIndices.add(i)
        }
        outputList.add(item)
        outputIndices.add(i)
    }
    return TransformablePage(
        originalPageOffset = originalPageOffset,
        data = outputList,
        originalPageSize = originalPageSize,
        originalIndices = outputIndices
    )
}

/**
 * Create a TransformablePage with the given separator (or empty, if the separator is null)
 *
 * We create an empty page when a separator is not needed in order to simplify dropping. By
 * ensuring there are always 2N-1 pages in the output event stream, every drop of a M pages in the
 * input event stream can be simply transformed to a drop of 2 * M pages.
 *
 * TODO: consider tracking the separator pages differently, so we don't have to
 *  allocate these empty pages.
 */
private fun <R : Any, T : R> separatorPage(
    adjacentPage: TransformablePage<T>,
    separator: R?,
    originalIndex: Int
): TransformablePage<R> = if (separator != null) {
    // page with just the separator
    TransformablePage(
        originalPageOffset = adjacentPage.originalPageOffset,
        data = listOf(separator),
        originalPageSize = adjacentPage.originalPageSize,
        originalIndices = listOf(originalIndex)
    )
} else {
    // empty page
    TransformablePage(
        originalPageOffset = adjacentPage.originalPageOffset,
        data = emptyList(),
        originalPageSize = adjacentPage.originalPageSize,
        originalIndices = null
    )
}

internal fun <R : Any, T : R> List<TransformablePage<T>>.insertSeparators(
    loadType: LoadType,
    itemAtStart: T?,
    itemAtEnd: T?,
    transform: (T?, T?) -> R?
): List<TransformablePage<R>> {
    if (isEmpty()) {
        return emptyList()
    }

    val outList = ArrayList<TransformablePage<R>>(size)

    var itemBefore = itemAtStart
    forEachIndexed { index, page ->
        // If page is being appended, or if we're in between pages, insert separator page
        if (index != 0 || loadType == END) {
            val separator = transform(itemBefore, page.data.first())
            outList.add(separatorPage(page, separator, originalIndex = 0))
        }

        outList.add(page.insertSeparators(transform))

        itemBefore = page.data.last()
    }

    if (loadType == START) {
        val lastPage = last()
        val separator = transform(lastPage.data.last(), itemAtEnd)
        outList.add(
            separatorPage(lastPage, separator, originalIndex = lastPage.originalPageSize - 1)
        )
    }

    return outList
}

/**
 * State-tracking operation on PageEvent to insert separators
 *
 * State is tracked in the mutable currentPages list, shared by all events in stream
 */
@Suppress("UNCHECKED_CAST")
internal fun <R : Any, T : R> PageEvent<T>.insertSeparators(
    currentPages: MutableList<TransformablePage<T>>,
    transform: (T?, T?) -> R?
): PageEvent<R> = when (this) {
    is Insert<T> -> {
        val newEvent = transformPages {
            it.insertSeparators(
                loadType = loadType,
                itemAtStart = if (loadType == END) currentPages.last().data.last() else null,
                itemAtEnd = if (loadType == START) currentPages.first().data.first() else null,
                transform = transform
            )
        }

        this.applyToList(currentPages)
        newEvent
    }
    is Drop -> {
        this.applyToList(currentPages)
        Drop(
            loadType = loadType,
            count = count * 2,
            placeholdersRemaining = placeholdersRemaining
        )
    }
    is StateUpdate -> this as PageEvent<R>
}