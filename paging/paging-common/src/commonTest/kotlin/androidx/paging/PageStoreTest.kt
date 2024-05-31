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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("TestFunctionName")
internal fun <T : Any> PageStore(
    pages: List<List<T>>,
    leadingNullCount: Int = COUNT_UNDEFINED,
    trailingNullCount: Int = COUNT_UNDEFINED,
    indexOfInitialPage: Int = 0
) =
    PageStore(
        localRefresh(
            pages =
                pages.mapIndexed { index, list ->
                    TransformablePage(originalPageOffset = index - indexOfInitialPage, data = list)
                },
            placeholdersBefore = leadingNullCount,
            placeholdersAfter = trailingNullCount,
        )
    )

internal fun <T : Any> PageStore<T>.insertPage(
    isPrepend: Boolean,
    page: List<T>,
    placeholdersRemaining: Int,
) =
    processEvent(
        adjacentInsertEvent(
            isPrepend = isPrepend,
            page = page,
            originalPageOffset = 0,
            placeholdersRemaining = placeholdersRemaining
        )
    )

internal fun <T : Any> PageStore<T>.dropPages(
    isPrepend: Boolean,
    minPageOffset: Int,
    maxPageOffset: Int,
    placeholdersRemaining: Int,
) =
    processEvent(
        PageEvent.Drop(
            loadType = if (isPrepend) PREPEND else APPEND,
            minPageOffset = minPageOffset,
            maxPageOffset = maxPageOffset,
            placeholdersRemaining = placeholdersRemaining
        )
    )

internal fun <T : Any> PageStore<T>.asList() = List(size) { get(it) }

@Suppress("SameParameterValue")
class PageStoreTest {
    private fun verifyAppend(
        initialItems: Int,
        initialNulls: Int,
        newItems: Int,
        newNulls: Int = COUNT_UNDEFINED,
    ) {
        val data =
            PageStore(
                pages = mutableListOf(List(initialItems) { 'a' + it }),
                leadingNullCount = 0,
                trailingNullCount = initialNulls,
                indexOfInitialPage = 0
            )

        val inserted = List(newItems) { 'a' + it + initialItems }
        val event =
            data.insertPage(
                isPrepend = false,
                page = inserted,
                placeholdersRemaining = newNulls,
            )

        // Assert list contents first (since this shows more obvious errors)...
        val expectedNulls =
            if (newNulls != COUNT_UNDEFINED) {
                newNulls
            } else {
                maxOf(initialNulls - newItems, 0)
            }
        val expectedData = List(initialItems + newItems) { 'a' + it } + List(expectedNulls) { null }
        assertEquals(expectedData, data.asList())

        // Then assert we got the right PagingDataEvent
        assertEquals(
            PagingDataEvent.Append(
                startIndex = initialItems,
                inserted = inserted,
                newPlaceholdersAfter = newNulls,
                oldPlaceholdersAfter = initialNulls
            ),
            event
        )
    }

    private fun verifyPrepend(
        initialItems: Int,
        initialNulls: Int,
        newItems: Int,
        newNulls: Int,
    ) {
        val data =
            PageStore(
                pages = mutableListOf(List(initialItems) { 'z' + it - initialItems - 1 }),
                leadingNullCount = initialNulls,
                trailingNullCount = 0,
                indexOfInitialPage = 0
            )

        val endItemCount = newItems + initialItems
        val inserted = List(newItems) { 'z' + it - endItemCount - 1 }
        val event =
            data.insertPage(
                isPrepend = true,
                page = inserted,
                placeholdersRemaining = newNulls,
            )

        // Assert list contents first (since this shows more obvious errors)...
        val expectedNulls =
            if (newNulls != COUNT_UNDEFINED) {
                newNulls
            } else {
                maxOf(initialNulls - newItems, 0)
            }
        val expectedData =
            List(expectedNulls) { null } + List(endItemCount) { 'z' + it - endItemCount - 1 }
        assertEquals(expectedData, data.asList())

        // Then assert we got the right PagingDataEvent
        assertEquals(
            PagingDataEvent.Prepend(
                inserted = inserted,
                newPlaceholdersBefore = newNulls,
                oldPlaceholdersBefore = initialNulls
            ),
            event
        )
    }

    private fun verifyPrependAppend(
        initialItems: Int,
        initialNulls: Int,
        newItems: Int,
        newNulls: Int,
    ) {
        verifyPrepend(initialItems, initialNulls, newItems, newNulls)
        verifyAppend(initialItems, initialNulls, newItems, newNulls)
    }

    @Test
    fun insertPageEmpty() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 0,
            newItems = 0,
            newNulls = 0,
        )

    @Test
    fun insertPageSimple() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 0,
            newItems = 2,
            newNulls = 0,
        )

    @Test
    fun insertPageSimplePlaceholders() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 4,
            newItems = 2,
            newNulls = 2,
        )

    @Test
    fun insertPageInitPlaceholders() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 0,
            newItems = 2,
            newNulls = 3,
        )

    @Test
    fun insertPageInitJustPlaceholders() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 0,
            newItems = 0,
            newNulls = 3,
        )

    @Test
    fun insertPageInsertNulls() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 3,
            newItems = 2,
            newNulls = 2,
        )

    @Test
    fun insertPageRemoveNulls() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 7,
            newItems = 2,
            newNulls = 0,
        )

    @Test
    fun insertPageReduceNulls() =
        verifyPrependAppend(
            initialItems = 2,
            initialNulls = 10,
            newItems = 3,
            newNulls = 4,
        )

    private fun verifyDropEnd(
        initialPages: List<List<Char>>,
        initialNulls: Int = 0,
        newNulls: Int,
        dropCount: Int,
        pagesToDrop: Int,
    ) {
        if (initialPages.size < 2) {
            fail("require at least 2 pages")
        }

        val data =
            PageStore(
                pages = initialPages.toMutableList(),
                leadingNullCount = 0,
                trailingNullCount = initialNulls,
                indexOfInitialPage = 0
            )

        assertEquals(initialPages.flatten() + List<Char?>(initialNulls) { null }, data.asList())

        val event =
            data.dropPages(
                isPrepend = false,
                minPageOffset = initialPages.lastIndex - (pagesToDrop - 1),
                maxPageOffset = initialPages.lastIndex,
                placeholdersRemaining = newNulls,
            )

        // assert PagingDataEvent and final list state
        assertEquals(
            PagingDataEvent.DropAppend(
                startIndex = initialPages.flatten().size - dropCount,
                dropCount = dropCount,
                newPlaceholdersAfter = newNulls,
                oldPlaceholdersAfter = initialNulls
            ),
            event
        )
        val finalData = initialPages.subList(0, initialPages.size - pagesToDrop).flatten()
        assertEquals(finalData + List<Char?>(newNulls) { null }, data.asList())
    }

    private fun verifyDropStart(
        initialPages: List<List<Char>>,
        initialNulls: Int = 0,
        newNulls: Int,
        dropCount: Int,
        pagesToDrop: Int,
    ) {
        if (initialPages.size < 2) {
            fail("require at least 2 pages")
        }

        val data =
            PageStore(
                pages = initialPages.reversed().toMutableList(),
                leadingNullCount = initialNulls,
                trailingNullCount = 0,
                indexOfInitialPage = 0
            )

        assertEquals(
            List<Char?>(initialNulls) { null } + initialPages.reversed().flatten(),
            data.asList()
        )

        val event =
            data.dropPages(
                isPrepend = true,
                minPageOffset = 0,
                maxPageOffset = pagesToDrop - 1,
                placeholdersRemaining = newNulls,
            )

        // assert PagingDataEvent and final list state
        assertEquals(
            PagingDataEvent.DropPrepend(
                dropCount = dropCount,
                oldPlaceholdersBefore = initialNulls,
                newPlaceholdersBefore = newNulls
            ),
            event
        )
        val finalData = initialPages.take(initialPages.size - pagesToDrop).reversed().flatten()
        assertEquals(List<Char?>(newNulls) { null } + finalData, data.asList())
    }

    private fun verifyDrop(
        initialPages: List<List<Char>>,
        initialNulls: Int = 0,
        newNulls: Int,
        pagesToDrop: Int,
    ) {
        val dropCount = initialPages.reversed().take(pagesToDrop).flatten().size
        verifyDropStart(initialPages, initialNulls, newNulls, dropCount, pagesToDrop)
        verifyDropEnd(initialPages, initialNulls, newNulls, dropCount, pagesToDrop)
    }

    @Test
    fun dropPageMulti() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 0,
            newNulls = 0,
            pagesToDrop = 2,
        )

    @Test
    fun dropPageReturnNulls() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 1,
            newNulls = 4,
            pagesToDrop = 2,
        )

    @Test
    fun dropPageFromNoNullsToHavingNulls() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 0,
            newNulls = 3,
            pagesToDrop = 2,
        )

    @Test
    fun dropPageChangeRemovePlaceholders() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 2,
            newNulls = 4,
            pagesToDrop = 2,
        )

    @Test
    fun dropPageChangeRemoveItems() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 0,
            newNulls = 1,
            pagesToDrop = 2,
        )

    @Test
    fun dropPageChangeDoubleRemove() =
        verifyDrop(
            initialPages = listOf(listOf('a', 'b'), listOf('c', 'd'), listOf('e')),
            initialNulls = 3,
            newNulls = 1,
            pagesToDrop = 2,
        )

    @Test
    fun getOutOfBounds() {
        val storage =
            PageStore(
                pages = mutableListOf(listOf('a')),
                leadingNullCount = 1,
                trailingNullCount = 1,
                indexOfInitialPage = 0
            )
        assertFailsWith<IndexOutOfBoundsException> { storage.get(-1) }
        assertFailsWith<IndexOutOfBoundsException> { storage.get(4) }
    }

    // TODO storageCount test

    @Test
    fun snapshot_uncounted() {
        val pageStore =
            PageStore(
                insertEvent =
                    localRefresh(
                        pages = listOf(TransformablePage(listOf('a'))),
                    )
            )

        assertEquals<List<Char?>>(listOf('a'), pageStore.snapshot())
    }

    @Test
    fun snapshot_counted() {
        val pageStore =
            PageStore(
                insertEvent =
                    localRefresh(
                        pages = listOf(TransformablePage(listOf('a'))),
                        placeholdersBefore = 1,
                        placeholdersAfter = 3,
                    )
            )

        assertEquals(listOf(null, 'a', null, null, null), pageStore.snapshot())
    }
}
