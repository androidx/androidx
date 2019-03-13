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
import org.mockito.Mockito.reset
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
    fun isTiled_addend_smallerPageIsNotLast() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(0, createPage("a", "a"), 0)
        assertTrue(storage.isTiled)

        storage.appendPage(createPage("a", "a"), callback)
        assertTrue(storage.isTiled)

        storage.appendPage(createPage("a"), callback)
        assertTrue(storage.isTiled)

        // no matter what we append here, we're no longer tiled
        storage.appendPage(createPage("a", "a"), callback)
        assertFalse(storage.isTiled)
    }

    @Test
    fun isTiled_append_growingSizeDisable() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(0, createPage("a", "a"), 0)
        assertTrue(storage.isTiled)

        // page size can't grow from append
        storage.appendPage(createPage("a", "a", "a"), callback)
        assertFalse(storage.isTiled)
    }

    @Test
    fun isTiled_prepend_smallerPage() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(0, createPage("a"), 0)
        assertTrue(storage.isTiled)

        storage.prependPage(createPage("a", "a"), callback)
        assertTrue(storage.isTiled)

        storage.prependPage(createPage("a", "a"), callback)
        assertTrue(storage.isTiled)

        storage.prependPage(createPage("a"), callback)
        assertFalse(storage.isTiled)
    }

    @Test
    fun isTiled_prepend_smallerThanInitialPage() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(0, createPage("a", "a"), 0)
        assertTrue(storage.isTiled)

        storage.prependPage(createPage("a"), callback)
        assertFalse(storage.isTiled)
    }

    @Test
    fun get_tiled() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(1, createPage("a", "b"), 5)
        assertTrue(storage.isTiled)

        storage.appendPage(createPage("c", "d"), callback)
        storage.appendPage(createPage("e", "f"), callback)

        assertTrue(storage.isTiled)
        assertArrayEquals(arrayOf(null, "a", "b", "c", "d", "e", "f", null), storage.toArray())
    }

    @Test
    fun get_nonTiled() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage(1, createPage("a"), 6)
        assertTrue(storage.isTiled)

        storage.appendPage(createPage("b", "c"), callback)
        storage.appendPage(createPage("d", "e", "f"), callback)

        assertFalse(storage.isTiled)
        assertArrayEquals(arrayOf(null, "a", "b", "c", "d", "e", "f", null), storage.toArray())
    }

    @Test
    fun insertOne() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c", "d"), 3, 0, callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf(null, null, "c", "d", null, null, null), storage.toArray())
        verify(callback).onInitialized(7)
        verifyNoMoreInteractions(callback)

        storage.insertPage(4, createPage("e", "f"), callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf(null, null, "c", "d", "e", "f", null), storage.toArray())
        verify(callback).onPageInserted(4, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun insertThree() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c", "d"), 3, 0, callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf(null, null, "c", "d", null, null, null), storage.toArray())
        verify(callback).onInitialized(7)
        verifyNoMoreInteractions(callback)

        // first, insert 1st page
        storage.insertPage(0, createPage("a", "b"), callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf("a", "b", "c", "d", null, null, null), storage.toArray())
        verify(callback).onPageInserted(0, 2)
        verifyNoMoreInteractions(callback)

        // then 3rd page
        storage.insertPage(4, createPage("e", "f"), callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf("a", "b", "c", "d", "e", "f", null), storage.toArray())
        verify(callback).onPageInserted(4, 2)
        verifyNoMoreInteractions(callback)

        // then last, small page
        storage.insertPage(6, createPage("g"), callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf("a", "b", "c", "d", "e", "f", "g"), storage.toArray())
        verify(callback).onPageInserted(6, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun insertLastFirst() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(6, createPage("g"), 0, 0, callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf(null, null, null, null, null, null, "g"), storage.toArray())
        verify(callback).onInitialized(7)
        verifyNoMoreInteractions(callback)

        // insert 1st page
        storage.insertPage(0, createPage("a", "b"), callback)

        assertEquals(7, storage.size)
        assertArrayEquals(arrayOf("a", "b", null, null, null, null, "g"), storage.toArray())
        verify(callback).onPageInserted(0, 2)
        verifyNoMoreInteractions(callback)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertFailure_decreaseLast() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c", "d"), 0, 0, callback)

        // should throw, page too small
        storage.insertPage(0, createPage("a"), callback)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertFailure_increase() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(0, createPage("a", "b"), 3, 0, callback)

        // should throw, page too big
        storage.insertPage(2, createPage("c", "d", "e"), callback)
    }

    @Test
    fun allocatePlaceholders_simple() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c"), 2, 0, callback)

        verify(callback).onInitialized(5)

        storage.allocatePlaceholders(2, 1, 1, callback)

        verify(callback).onPagePlaceholderInserted(1)
        verify(callback).onPagePlaceholderInserted(3)
        verifyNoMoreInteractions(callback)

        assertArrayEquals(arrayOf(null, null, "c", null, null), storage.toArray())
    }

    @Test
    fun allocatePlaceholders_adoptPageSize() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(4, createPage("e"), 0, 0, callback)

        verify(callback).onInitialized(5)

        storage.allocatePlaceholders(0, 2, 2, callback)

        verify(callback).onPagePlaceholderInserted(0)
        verify(callback).onPagePlaceholderInserted(1)
        verifyNoMoreInteractions(callback)

        assertArrayEquals(arrayOf(null, null, null, null, "e"), storage.toArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun allocatePlaceholders_cannotShrinkPageSize() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(4, createPage("e", "f"), 0, 0, callback)

        verify(callback).onInitialized(6)

        storage.allocatePlaceholders(0, 2, 1, callback)
    }

    @Test(expected = IllegalArgumentException::class)
    fun allocatePlaceholders_cannotAdoptPageSize() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c", "d"), 2, 0, callback)

        verify(callback).onInitialized(6)

        storage.allocatePlaceholders(0, 2, 3, callback)
    }

    @Test
    fun get_placeholdersMulti() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(2, createPage("c", "d"), 3, 0, callback)

        assertArrayEquals(arrayOf(null, null, "c", "d", null, null, null), storage.toArray())

        storage.allocatePlaceholders(0, 10, 2, callback)

        // allocating placeholders shouldn't affect result of get
        assertArrayEquals(arrayOf(null, null, "c", "d", null, null, null), storage.toArray())
    }

    @Test
    fun hasPage() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.init(4, createPage("e"), 0, 0, callback)

        assertFalse(storage.hasPage(1, 0))
        assertFalse(storage.hasPage(1, 1))
        assertFalse(storage.hasPage(1, 2))
        assertFalse(storage.hasPage(1, 3))
        assertTrue(storage.hasPage(1, 4))

        assertFalse(storage.hasPage(2, 0))
        assertFalse(storage.hasPage(2, 1))
        assertTrue(storage.hasPage(2, 2))
    }

    @Test
    fun pageWouldBeBoundary_unallocated() {
        val storage = PagedStorage<String>()
        storage.initAndSplit(2, listOf("c", "d", "e", "f"), 1, 0, 2, IGNORED_CALLBACK)

        assertTrue(storage.pageWouldBeBoundary(0, true))
        assertFalse(storage.pageWouldBeBoundary(0, false))
        assertTrue(storage.pageWouldBeBoundary(6, false))
        assertFalse(storage.pageWouldBeBoundary(6, true))
    }

    @Test
    fun pageWouldBeBoundary_front() {
        val storage = PagedStorage<String>()
        storage.initAndSplit(8, listOf("i", "j", "k", "l", "m", "n"), 0, 0, 2, IGNORED_CALLBACK)

        for (i in 0..6 step 2) {
            // any position in leading nulls would be front boundary
            assertTrue(storage.pageWouldBeBoundary(i, true))
            assertFalse(storage.pageWouldBeBoundary(i, false))
        }

        storage.allocatePlaceholders(8, 6, 2, IGNORED_CALLBACK)

        for (i in 0..6 step 2) {
            // 4 / 6 have a placeholder ahead, so they return false
            assertEquals(i < 4, storage.pageWouldBeBoundary(i, true))
            assertFalse(storage.pageWouldBeBoundary(i, false))
        }
    }

    @Test
    fun pageWouldBeBoundary_end() {
        val storage = PagedStorage<String>()
        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e", "f"), 8, 0, 2, IGNORED_CALLBACK)

        for (i in 6..12 step 2) {
            // any position in leading nulls would be front boundary
            assertFalse(storage.pageWouldBeBoundary(i, true))
            assertTrue(storage.pageWouldBeBoundary(i, false))
        }

        storage.allocatePlaceholders(6, 6, 2, IGNORED_CALLBACK)

        for (i in 6..12 step 2) {
            // any position in leading nulls would be front boundary
            assertFalse(storage.pageWouldBeBoundary(i, true))
            assertEquals(i > 10, storage.pageWouldBeBoundary(i, false))
        }
    }

    @Test
    fun trim_noOp() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e"), 0, 0, 1, callback)

        verify(callback).onInitialized(5)
        storage.trimFromFront(true, 5, 5, callback)
        verifyNoMoreInteractions(callback)
        storage.trimFromEnd(true, 5, 5, callback)
        verifyNoMoreInteractions(callback)
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
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()
        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e", "f"), 0, 0, 2, callback)

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
        val storage = PagedStorage<String>()

        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e"), 0, 0, 1, callback)
        verify(callback).onInitialized(5)
        verifyNoMoreInteractions(callback)

        storage.trimFromFront(false, 4, 4, callback)
        verify(callback).onPagesRemoved(0, 1)
        verifyNoMoreInteractions(callback)

        // position is offset, since we've removed one
        assertEquals(1, storage.positionOffset)
    }

    @Test
    fun trimFromFront_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e"), 0, 0, 1, callback)
        verify(callback).onInitialized(5)
        verifyNoMoreInteractions(callback)

        storage.trimFromFront(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(0, 1)
        verifyNoMoreInteractions(callback)

        // position not offset, since data changed but not removed
        assertEquals(0, storage.positionOffset)
    }

    @Test
    fun trimFromFront_complexWithGaps() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(4, listOf("e"), 3, 0, 1, callback)
        storage.insertPage(1, listOf("b"), callback)
        storage.insertPage(3, listOf("d"), callback)
        storage.insertPage(6, listOf("g"), callback)
        storage.insertPage(7, listOf("h"), callback)
        reset(callback)
        assertEquals(7, storage.pageCount) // page for everything but leading null
        assertEquals(listOf(null, "b", null, "d", "e", null, "g", "h"), storage)

        // going from: -b-de-gh
        //         to: ----e-gh
        // we signal this as onPagesSwappedToPlaceholder(1, 3). We could theoretically separate
        // this into two single page drop signals, but it's too rare to be worth it.
        storage.trimFromFront(true, 3, 3, callback)
        verify(callback).onPagesSwappedToPlaceholder(1, 3)
        assertEquals(4, storage.pageCount)
        assertEquals(listOf(null, null, null, null, "e", null, "g", "h"), storage)
    }

    @Test
    fun trimFromEnd_simple() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e"), 0, 0, 1, callback)
        verify(callback).onInitialized(5)
        verifyNoMoreInteractions(callback)

        storage.trimFromEnd(false, 4, 4, callback)
        verify(callback).onPagesRemoved(4, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trimFromEnd_simplePlaceholders() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(0, listOf("a", "b", "c", "d", "e"), 0, 0, 1, callback)
        verify(callback).onInitialized(5)
        verifyNoMoreInteractions(callback)

        storage.trimFromEnd(true, 4, 4, callback)
        verify(callback).onPagesSwappedToPlaceholder(4, 1)
        verifyNoMoreInteractions(callback)
    }

    @Test
    fun trimFromEnd_complexWithGaps() {
        val callback = mock(PagedStorage.Callback::class.java)
        val storage = PagedStorage<String>()

        storage.initAndSplit(3, listOf("d"), 4, 0, 1, callback)
        storage.insertPage(0, listOf("a"), callback)
        storage.insertPage(1, listOf("b"), callback)
        storage.insertPage(5, listOf("f"), callback)
        storage.insertPage(6, listOf("g"), callback)
        reset(callback)
        assertEquals(7, storage.pageCount) // page for everything but trailing null
        assertEquals(listOf("a", "b", null, "d", null, "f", "g", null), storage)

        // going from: ab-d-fg-
        //         to: ab-d----
        // we signal this as onPagesSwappedToPlaceholder(4, 3). We could theoretically separate
        // this into two single page drop signals, but it's too rare to be worth it.
        storage.trimFromEnd(true, 3, 3, callback)
        verify(callback).onPagesSwappedToPlaceholder(4, 3)
        assertEquals(4, storage.pageCount)
        assertEquals(listOf("a", "b", null, "d", null, null, null, null), storage)
    }

    companion object {
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
