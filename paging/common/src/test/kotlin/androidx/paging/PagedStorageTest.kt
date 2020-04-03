/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.paging.PagingSource.LoadResult.Page
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import kotlin.test.assertNotNull

@RunWith(JUnit4::class)
class PagedStorageTest {
    private fun pageOf(vararg strings: String): Page<Any, String> = Page(
        data = strings.asList(),
        prevKey = null,
        nextKey = null
    )

    @Test
    fun construct() {
        val storage = PagedStorage(2, pageOf("a", "b"), 2)

        assertArrayEquals(arrayOf(null, null, "a", "b", null, null), storage.toArray())
        assertEquals(6, storage.size)
    }

    @Test
    fun appendFill() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, pageOf("a", "b"), 2)
        storage.appendPage(pageOf("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 2, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, pageOf("a", "b"), 0)
        storage.appendPage(pageOf("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendFillAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, pageOf("a", "b"), 2)

        // change 2 nulls into c, d
        storage.appendPage(pageOf("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 2, 0)
        verifyNoMoreInteractions(callback)

        // append e, f
        storage.appendPage(pageOf("e", "f"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d", "e", "f"), storage.toArray())
        verify(callback).onPageAppended(6, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependFill() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, pageOf("c", "d"), 2)
        storage.prependPage(pageOf("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 2, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(0, pageOf("c", "d"), 2)
        storage.prependPage(pageOf("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependFillAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, pageOf("e", "f"), 2)

        // change 2 nulls into c, d
        storage.prependPage(pageOf("c", "d"), callback)

        assertArrayEquals(arrayOf("c", "d", "e", "f", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 2, 0)
        verifyNoMoreInteractions(callback)

        // prepend a, b
        storage.prependPage(pageOf("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", "e", "f", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun get() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(1, pageOf("a"), 6)
        storage.appendPage(pageOf("b", "c"), callback)
        storage.appendPage(pageOf("d", "e", "f"), callback)
        assertArrayEquals(arrayOf(null, "a", "b", "c", "d", "e", "f", null), storage.toArray())
    }

    @Test
    fun trim_twoPagesNoOp() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()
        storage.init(0, pageOf("a", "b", "c"), 3, 0, callback, true)
        verify(callback).onInitialized(6)
        storage.appendPage(pageOf("d", "e", "f"), callback)
        verify(callback).onPageAppended(3, 3, 0)

        storage.trimFromFront(true, 4, 4, callback)
        verifyNoMoreInteractions(callback)
        storage.trimFromEnd(true, 4, 4, callback)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trim_remainderPreventsNoOp() {
        val storage = PagedStorage(
            listOf(pageOf("a", "b"), pageOf("c", "d"), pageOf("d", "e"))
        )

        // can't trim, since removing a page would mean fewer items than required
        assertFalse(storage.needsTrimFromFront(5, 5))
        assertFalse(storage.needsTrimFromEnd(5, 5))

        // can trim, since stops cleanly at page boundary
        assertTrue(storage.needsTrimFromFront(4, 4))
        assertTrue(storage.needsTrimFromEnd(4, 4))
    }

    @Test
    fun trimFromFront_simple() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { pageOf("$it") })

        storage.trimFromFront(false, 4, 4, callback)
        verify(callback).onPagesRemoved(0, 1)
        verifyNoMoreInteractions(callback)

        // position is offset, since we've removed one
        assertEquals(1, storage.positionOffset)
    }

    @Test
    fun trimFromFront_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { pageOf("$it") })

        storage.trimFromFront(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(0, 1)
        verifyNoMoreInteractions(callback)

        // position not offset, since data changed but not removed
        assertEquals(0, storage.positionOffset)
    }

    @Test
    fun trimFromEnd_simple() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { pageOf("$it") })

        storage.trimFromEnd(false, 4, 4, callback)
        verify(callback).onPagesRemoved(4, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trimFromEnd_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { pageOf("$it") })

        storage.trimFromEnd(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(4, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun getRefreshKeyInfo_withoutPlaceholders() {
        val page = pageOf("a", "b", "c")
        val storage = PagedStorage(0, page, 0)

        storage.lastLoadAroundIndex = -5
        var pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(0, pagingState.anchorPosition)

        storage.lastLoadAroundIndex = 1
        pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(1, pagingState.anchorPosition)

        storage.lastLoadAroundIndex = 5
        pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(2, pagingState.anchorPosition)
    }

    @Test
    fun getRefreshKeyInfo_withPlaceholders() {
        val page = pageOf("a", "b", "c")
        val storage = PagedStorage(10, page, 10)

        storage.lastLoadAroundIndex = 1
        var pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(10, pagingState.anchorPosition)

        storage.lastLoadAroundIndex = 11
        pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(11, pagingState.anchorPosition)

        storage.lastLoadAroundIndex = 21
        pagingState = storage.getRefreshKeyInfo(
            @Suppress("DEPRECATION")
            PagedList.Config(
                pageSize = 3,
                prefetchDistance = 0,
                enablePlaceholders = true,
                initialLoadSizeHint = 3,
                maxSize = 3
            )
        )
        assertNotNull(pagingState)
        assertEquals(12, pagingState.anchorPosition)
    }

    companion object {
        @Suppress("TestFunctionName")
        private fun PagedStorage(pages: List<Page<*, String>>): PagedStorage<String> {
            val storage = PagedStorage<String>()
            val totalPageCount = pages.map { it.data.size }.sum()
            storage.init(
                leadingNulls = 0,
                page = pages[0],
                trailingNulls = totalPageCount - pages[0].data.size,
                positionOffset = 0,
                callback = IGNORED_CALLBACK,
                counted = true
            )
            pages.forEachIndexed { index, page ->
                if (index != 0) {
                    storage.appendPage(page, IGNORED_CALLBACK)
                }
            }
            return storage
        }

        private val IGNORED_CALLBACK = object : PagedStorage.Callback {
            override fun onInitialized(count: Int) {}
            override fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int) {}
            override fun onPageAppended(endPosition: Int, changed: Int, added: Int) {}
            override fun onPagesRemoved(startOfDrops: Int, count: Int) {}
            override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) {}
        }
    }
}