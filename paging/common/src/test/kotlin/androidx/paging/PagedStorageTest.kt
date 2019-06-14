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

@RunWith(JUnit4::class)
class PagedStorageTest {
    private fun createPage(vararg strings: String): List<String> {
        return strings.asList()
    }

    @Test
    fun construct() {
        val storage = PagedStorage(2, createPage("a", "b"), 2)

        assertArrayEquals(arrayOf(null, null, "a", "b", null, null), storage.toArray())
        assertEquals(6, storage.size)
    }

    @Test
    fun appendFill() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, createPage("a", "b"), 2)
        storage.appendPage(createPage("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 2, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, createPage("a", "b"), 0)
        storage.appendPage(createPage("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun appendFillAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, createPage("a", "b"), 2)

        // change 2 nulls into c, d
        storage.appendPage(createPage("c", "d"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d"), storage.toArray())
        verify(callback).onPageAppended(4, 2, 0)
        verifyNoMoreInteractions(callback)

        // append e, f
        storage.appendPage(createPage("e", "f"), callback)

        assertArrayEquals(arrayOf(null, null, "a", "b", "c", "d", "e", "f"), storage.toArray())
        verify(callback).onPageAppended(6, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependFill() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, createPage("c", "d"), 2)
        storage.prependPage(createPage("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 2, 0)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(0, createPage("c", "d"), 2)
        storage.prependPage(createPage("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun prependFillAdd() {
        val callback = mock(PagedStorage.Callback::class.java)

        val storage = PagedStorage(2, createPage("e", "f"), 2)

        // change 2 nulls into c, d
        storage.prependPage(createPage("c", "d"), callback)

        assertArrayEquals(arrayOf("c", "d", "e", "f", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 2, 0)
        verifyNoMoreInteractions(callback)

        // prepend a, b
        storage.prependPage(createPage("a", "b"), callback)

        assertArrayEquals(arrayOf("a", "b", "c", "d", "e", "f", null, null), storage.toArray())
        verify(callback).onPagePrepended(0, 0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun get() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(1, createPage("a"), 6)
        storage.appendPage(createPage("b", "c"), callback)
        storage.appendPage(createPage("d", "e", "f"), callback)
        assertArrayEquals(arrayOf(null, "a", "b", "c", "d", "e", "f", null), storage.toArray())
    }

    @Test
    fun trim_twoPagesNoOp() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()
        storage.init(0, listOf("a", "b", "c"), 3, 0, callback)
        verify(callback).onInitialized(6)
        storage.appendPage(listOf("d", "e", "f"), callback)
        verify(callback).onPageAppended(3, 3, 0)

        storage.trimFromFront(true, 4, 4, callback)
        verifyNoMoreInteractions(callback)
        storage.trimFromEnd(true, 4, 4, callback)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trim_remainderPreventsNoOp() {
        val storage = PagedStorage(listOf(listOf("a", "b"), listOf("c", "d"), listOf("d", "e")))

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
        val storage = PagedStorage(('a'..'e').map { listOf("$it") })

        storage.trimFromFront(false, 4, 4, callback)
        verify(callback).onPagesRemoved(0, 1)
        verifyNoMoreInteractions(callback)

        // position is offset, since we've removed one
        assertEquals(1, storage.positionOffset)
    }

    @Test
    fun trimFromFront_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { listOf("$it") })

        storage.trimFromFront(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(0, 1)
        verifyNoMoreInteractions(callback)

        // position not offset, since data changed but not removed
        assertEquals(0, storage.positionOffset)
    }

    @Test
    fun trimFromEnd_simple() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { listOf("$it") })

        storage.trimFromEnd(false, 4, 4, callback)
        verify(callback).onPagesRemoved(4, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trimFromEnd_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(('a'..'e').map { listOf("$it") })

        storage.trimFromEnd(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(4, 1)
        verifyNoMoreInteractions(callback)
    }

    companion object {
        @Suppress("TestFunctionName")
        private fun PagedStorage(pages: List<List<String>>): PagedStorage<String> {
            val storage = PagedStorage<String>()
            val totalPageCount = pages.map { it.size }.reduce { a, b -> a + b }
            storage.init(0, pages[0], totalPageCount - pages[0].size, 0, IGNORED_CALLBACK)
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
            override fun onPagePlaceholderInserted(pageIndex: Int) {}
            override fun onPageInserted(start: Int, count: Int) {}
            override fun onPagesRemoved(startOfDrops: Int, count: Int) {}
            override fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int) {}
        }
    }
}