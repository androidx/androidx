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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.fail

@Suppress("TestFunctionName")
internal fun <T : Any> PagingState.Producer<T>.init(
    pages: List<List<T>>,
    placeholdersStart: Int = 0,
    placeholdersEnd: Int = 0,
    indexOfInitialPage: Int = 0
) = processEvent(
    PageEvent.Insert.Refresh(
        pages = pages.toTransformablePages(indexOfInitialPage),
        placeholdersStart = placeholdersStart,
        placeholdersEnd = placeholdersEnd
    )
)

internal fun <T : Any> PagingState.Producer<T>.insertPage(
    isPrepend: Boolean,
    page: List<T>,
    originalPageOffset: Int,
    placeholdersRemaining: Int
) = when (isPrepend) {
    true -> processEvent(
        PageEvent.Insert.Start(
            pages = listOf(
                TransformablePage(
                    originalPageOffset = originalPageOffset,
                    data = page
                )
            ),
            placeholdersStart = placeholdersRemaining
        )
    )
    false -> processEvent(
        PageEvent.Insert.End(
            pages = listOf(
                TransformablePage(
                    originalPageOffset = originalPageOffset,
                    data = page
                )
            ),
            placeholdersEnd = placeholdersRemaining
        )
    )
}

internal fun <T : Any> PagingState.Producer<T>.dropPages(
    isPrepend: Boolean,
    count: Int,
    placeholdersRemaining: Int
) = processEvent(
    PageEvent.Drop(
        loadType = if (isPrepend) LoadType.START else LoadType.END,
        count = count,
        placeholdersRemaining = placeholdersRemaining
    )
)
@Suppress("TestFunctionName")
internal fun <T : Any> PagingState(
    pages: List<List<T>>,
    placeholdersStart: Int = 0,
    placeholdersEnd: Int = 0,
    indexOfInitialPage: Int = 0,
    refresh: LoadState,
    start: LoadState,
    end: LoadState,
    hintReceiver: (ViewportHint) -> Unit
) = PagingState(
    leadingNullCount = placeholdersStart,
    trailingNullCount = placeholdersEnd,
    pages = pages.toTransformablePages(indexOfInitialPage),
    loadStateRefresh = refresh,
    loadStateStart = start,
    loadStateEnd = end,
    hintReceiver = hintReceiver
)

@Suppress("SameParameterValue")
@RunWith(JUnit4::class)
class PagingStateTest {
    @Test
    fun initial() {
        assertEquals(
            PagingState(
                pages = listOf<List<String>>(),
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = PagingState.noopHintReceiver
            ),
            PagingState.initial()
        )
    }

    @Test
    fun simpleInit() {
        val hintReceiver: (ViewportHint) -> Unit = { fail("no load hint expected") }
        val producer = PagingState.Producer<String>(hintReceiver)
        assertEquals(
            PagingState(
                pages = listOf(listOf("a", "b")),
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.init(
                listOf(listOf("a", "b"))
            )
        )
    }

    private fun verifyPrepend(
        initialItems: Int,
        initialPlaceholders: Int,
        newItems: Int,
        newPlaceholders: Int
    ) {
        val hintReceiver: (ViewportHint) -> Unit = { fail("no load hint expected") }
        val producer = PagingState.Producer<Char>(hintReceiver)

        val initialPage = List(initialItems) { 'z' + it - initialItems - 1 }
        assertEquals(
            PagingState(
                pages = listOf(initialPage),
                placeholdersStart = initialPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.init(
                pages = listOf(initialPage),
                placeholdersStart = initialPlaceholders
            )
        )

        val page: List<Char> = List(newItems) { 'a' + it + initialItems }
        assertEquals(
            PagingState(
                pages = listOf(page, initialPage),
                indexOfInitialPage = 1,
                placeholdersStart = newPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.insertPage(
                isPrepend = true,
                page = page,
                originalPageOffset = -1,
                placeholdersRemaining = newPlaceholders
            )
        )
    }

    private fun verifyAppend(
        initialItems: Int,
        initialPlaceholders: Int,
        newItems: Int,
        newPlaceholders: Int
    ) {
        val hintReceiver: (ViewportHint) -> Unit = { fail("no load hint expected") }
        val producer = PagingState.Producer<Char>(hintReceiver)

        val initialPage = List(initialItems) { 'a' + it }
        assertEquals(
            PagingState(
                pages = listOf(initialPage),
                placeholdersEnd = initialPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.init(
                pages = listOf(initialPage),
                placeholdersEnd = initialPlaceholders
            )
        )

        val page: List<Char> = List(newItems) { 'a' + it + initialItems }
        assertEquals(
            PagingState(
                pages = listOf(initialPage, page),
                placeholdersEnd = newPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.insertPage(
                isPrepend = false,
                page = page,
                originalPageOffset = 1,
                placeholdersRemaining = newPlaceholders
            )
        )
    }

    private fun verifyPrependAppend(
        initialItems: Int,
        initialPlaceholders: Int,
        newItems: Int,
        newPlaceholders: Int
    ) {
        verifyPrepend(initialItems, initialPlaceholders, newItems, newPlaceholders)
        verifyAppend(initialItems, initialPlaceholders, newItems, newPlaceholders)
    }

    @Test
    fun insertPageEmpty() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 0,
        newItems = 0,
        newPlaceholders = 0
    )

    @Test
    fun insertPageSimple() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 0,
        newItems = 2,
        newPlaceholders = 0
    )

    @Test
    fun insertPageSimplePlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 4,
        newItems = 2,
        newPlaceholders = 2
    )

    @Test
    fun insertPageInitPlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 0,
        newItems = 2,
        newPlaceholders = 3
    )

    @Test
    fun insertPageInitJustPlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 0,
        newItems = 0,
        newPlaceholders = 3
    )

    @Test
    fun insertPageInsertPlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 3,
        newItems = 2,
        newPlaceholders = 2
    )

    @Test
    fun insertPageRemovePlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 7,
        newItems = 2,
        newPlaceholders = 0
    )

    @Test
    fun insertPageReducePlaceholders() = verifyPrependAppend(
        initialItems = 2,
        initialPlaceholders = 10,
        newItems = 3,
        newPlaceholders = 4
    )

    private fun verifyDropStart(
        initialPages: List<List<Char>>,
        initialPlaceholders: Int = 0,
        newPlaceholders: Int,
        pagesToDrop: Int
    ) {
        if (initialPages.size < 2) {
            fail("require at least 2 pages")
        }

        val hintReceiver: (ViewportHint) -> Unit = { fail("no load hint expected") }
        val producer = PagingState.Producer<Char>(hintReceiver)

        assertEquals(
            PagingState(
                pages = initialPages,
                placeholdersStart = initialPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.init(
                pages = initialPages,
                placeholdersStart = initialPlaceholders
            )
        )

        assertEquals(
            PagingState(
                pages = initialPages.subList(pagesToDrop, initialPages.size),
                indexOfInitialPage = -pagesToDrop,
                placeholdersStart = newPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.dropPages(
                isPrepend = true,
                count = pagesToDrop,
                placeholdersRemaining = newPlaceholders
            )
        )
    }

    private fun verifyDropEnd(
        initialPages: List<List<Char>>,
        initialPlaceholders: Int = 0,
        newPlaceholders: Int,
        pagesToDrop: Int
    ) {
        if (initialPages.size < 2) {
            fail("require at least 2 pages")
        }
        val hintReceiver: (ViewportHint) -> Unit = { fail("no load hint expected") }
        val producer = PagingState.Producer<Char>(hintReceiver)

        assertEquals(
            PagingState(
                pages = initialPages,
                placeholdersEnd = initialPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.init(
                pages = initialPages,
                placeholdersEnd = initialPlaceholders
            )
        )

        assertEquals(
            PagingState(
                pages = initialPages.subList(0, initialPages.size - pagesToDrop),
                placeholdersEnd = newPlaceholders,
                start = LoadState.Idle,
                end = LoadState.Idle,
                refresh = LoadState.Loading,
                hintReceiver = hintReceiver
            ),
            producer.dropPages(
                isPrepend = false,
                count = pagesToDrop,
                placeholdersRemaining = newPlaceholders
            )
        )
    }

    private fun verifyDrop(
        initialPages: List<List<Char>>,
        initialPlaceholders: Int = 0,
        newPlaceholders: Int,
        pagesToDrop: Int
    ) {
        verifyDropStart(initialPages, initialPlaceholders, newPlaceholders, pagesToDrop)
        verifyDropEnd(initialPages, initialPlaceholders, newPlaceholders, pagesToDrop)
    }

    @Test
    fun dropPageNoop() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd')
        ),
        initialPlaceholders = 0,
        newPlaceholders = 0,
        pagesToDrop = 0
    )

    @Test
    fun dropPageMulti() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 0,
        newPlaceholders = 0,
        pagesToDrop = 2
    )

    @Test
    fun dropPageReturnPlaceholders() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 1,
        newPlaceholders = 4,
        pagesToDrop = 2
    )

    @Test
    fun dropPageFromNoPlaceholdersToHavingPlaceholders() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 0,
        newPlaceholders = 3,
        pagesToDrop = 2
    )

    @Test
    fun dropPageChangeRemovePlaceholders() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 2,
        newPlaceholders = 4,
        pagesToDrop = 2
    )

    @Test
    fun dropPageChangeRemoveItems() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 0,
        newPlaceholders = 1,
        pagesToDrop = 2
    )

    @Test
    fun dropPageChangeDoubleRemove() = verifyDrop(
        initialPages = listOf(
            listOf('a', 'b'),
            listOf('c', 'd'),
            listOf('e')
        ),
        initialPlaceholders = 3,
        newPlaceholders = 1,
        pagesToDrop = 2
    )
}