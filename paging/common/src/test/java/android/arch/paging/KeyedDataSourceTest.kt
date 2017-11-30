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

package android.arch.paging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

@RunWith(JUnit4::class)
class KeyedDataSourceTest {

    // ----- STANDARD -----

    private fun loadInitial(dataSource: ItemDataSource, key: Key?, initialLoadSize: Int,
            enablePlaceholders: Boolean): PageResult<Item> {
        @Suppress("UNCHECKED_CAST")
        val receiver = mock(PageResult.Receiver::class.java) as PageResult.Receiver<Item>
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(PageResult::class.java)
                as ArgumentCaptor<PageResult<Item>>

        dataSource.dispatchLoadInitial(key, initialLoadSize,
                /* ignored pageSize */ 10, enablePlaceholders, FailExecutor(), receiver)

        verify(receiver).onPageResult(anyInt(), captor.capture())
        verifyNoMoreInteractions(receiver)
        assertNotNull(captor.value)
        return captor.value
    }

    @Test
    fun loadInitial() {
        val dataSource = ItemDataSource()
        val result = loadInitial(dataSource, dataSource.getKey(ITEMS_BY_NAME_ID[49]), 10, true)

        assertEquals(45, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), result.page)
        assertEquals(45, result.trailingNulls)
    }

    @Test
    fun loadInitial_keyMatchesSingleItem() {
        val dataSource = ItemDataSource(items = ITEMS_BY_NAME_ID.subList(0, 1))

        // this is tricky, since load after and load before with the passed key will fail
        val result = loadInitial(dataSource, dataSource.getKey(ITEMS_BY_NAME_ID[0]), 20, true)

        assertEquals(0, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 1), result.page)
        assertEquals(0, result.trailingNulls)
    }

    @Test
    fun loadInitial_keyMatchesLastItem() {
        val dataSource = ItemDataSource()

        // tricky, because load after key is empty, so another load before and load after required
        val key = dataSource.getKey(ITEMS_BY_NAME_ID.last())
        val result = loadInitial(dataSource, key, 20, true)

        assertEquals(90, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(90, 100), result.page)
        assertEquals(0, result.trailingNulls)
    }

    @Test
    fun loadInitial_nullKey() {
        val dataSource = ItemDataSource()

        // dispatchLoadInitial(null, count) == dispatchLoadInitial(count)
        val result = loadInitial(dataSource, null, 10, true)

        assertEquals(0, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), result.page)
        assertEquals(90, result.trailingNulls)
    }

    @Test
    fun loadInitial_keyPastEndOfList() {
        val dataSource = ItemDataSource()

        // if key is past entire data set, should return last items in data set
        val key = Key("fz", 0)
        val result = loadInitial(dataSource, key, 10, true)

        // NOTE: ideally we'd load 10 items here, but it adds complexity and unpredictability to
        // do: load after was empty, so pass full size to load before, since this can incur larger
        // loads than requested (see keyMatchesLastItem test)
        assertEquals(95, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(95, 100), result.page)
        assertEquals(0, result.trailingNulls)
    }

    // ----- UNCOUNTED -----

    @Test
    fun loadInitial_disablePlaceholders() {
        val dataSource = ItemDataSource()

        // dispatchLoadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val result = loadInitial(dataSource, key, 10, false)

        assertEquals(0, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), result.page)
        assertEquals(0, result.trailingNulls)
    }

    @Test
    fun loadInitial_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // dispatchLoadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val result = loadInitial(dataSource, key, 10, true)

        assertEquals(0, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(45, 55), result.page)
        assertEquals(0, result.trailingNulls)
    }

    @Test
    fun loadInitial_nullKey_uncounted() {
        val dataSource = ItemDataSource(counted = false)

        // dispatchLoadInitial(null, count) == dispatchLoadInitial(count)
        val result = loadInitial(dataSource, null, 10, true)

        assertEquals(0, result.leadingNulls)
        assertEquals(ITEMS_BY_NAME_ID.subList(0, 10), result.page)
        assertEquals(0, result.trailingNulls)
    }

    // ----- EMPTY -----

    @Test
    fun loadInitial_empty() {
        val dataSource = ItemDataSource(items = ArrayList())

        // dispatchLoadInitial(key, count) == null padding, loadAfter(key, count), null padding
        val key = dataSource.getKey(ITEMS_BY_NAME_ID[49])
        val result = loadInitial(dataSource, key, 10, true)

        assertEquals(0, result.leadingNulls)
        assertTrue(result.page.isEmpty())
        assertEquals(0, result.trailingNulls)
    }

    @Test
    fun loadInitial_nullKey_empty() {
        val dataSource = ItemDataSource(items = ArrayList())
        val result = loadInitial(dataSource, null, 10, true)

        assertEquals(0, result.leadingNulls)
        assertTrue(result.page.isEmpty())
        assertEquals(0, result.trailingNulls)
    }

    // ----- Other behavior -----

    @Test
    fun loadBefore() {
        val dataSource = ItemDataSource()
        @Suppress("UNCHECKED_CAST")
        val callback = mock(KeyedDataSource.LoadCallback::class.java)
                as KeyedDataSource.LoadCallback<Item>

        dataSource.loadBefore(
                KeyedDataSource.LoadParams(dataSource.getKey(ITEMS_BY_NAME_ID[5]), 5), callback)

        @Suppress("UNCHECKED_CAST")
        val argument = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Item>>
        verify(callback).onResult(argument.capture())
        verifyNoMoreInteractions(callback)

        val observed = argument.value

        assertEquals(ITEMS_BY_NAME_ID.subList(0, 5), observed)
    }

    internal data class Key(val name: String, val id: Int)

    internal data class Item(
            val name: String, val id: Int, val balance: Double, val address: String)

    internal class ItemDataSource(private val counted: Boolean = true,
                                  private val items: List<Item> = ITEMS_BY_NAME_ID)
            : KeyedDataSource<Key, Item>() {

        override fun loadInitial(
                params: LoadInitialParams<Key>,
                callback: LoadInitialCallback<Item>) {
            val key = params.requestedInitialKey ?: Key("", Integer.MAX_VALUE)
            val start = Math.max(0, findFirstIndexAfter(key) - params.requestedLoadSize / 2)
            val endExclusive = Math.min(start + params.requestedLoadSize, items.size)

            if (params.placeholdersEnabled && counted) {
                callback.onResult(items.subList(start, endExclusive), start, items.size)
            } else {
                callback.onResult(items.subList(start, endExclusive))
            }
        }

        override fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Item>) {
            val start = findFirstIndexAfter(params.key)
            val endExclusive = Math.min(start + params.requestedLoadSize, items.size)

            callback.onResult(items.subList(start, endExclusive))
        }

        override fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Item>) {
            val firstIndexBefore = findFirstIndexBefore(params.key)
            val endExclusive = Math.max(0, firstIndexBefore + 1)
            val start = Math.max(0, firstIndexBefore - params.requestedLoadSize + 1)

            callback.onResult(items.subList(start, endExclusive))
        }

        override fun getKey(item: Item): Key {
            return Key(item.name, item.id)
        }

        private fun findFirstIndexAfter(key: Key): Int {
            return items.indices.firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(items[it])) < 0
            } ?: items.size
        }

        private fun findFirstIndexBefore(key: Key): Int {
            return items.indices.reversed().firstOrNull {
                KEY_COMPARATOR.compare(key, getKey(items[it])) > 0
            } ?: -1
        }
    }

    private fun performInitialLoad(
            callbackInvoker: (callback: KeyedDataSource.LoadInitialCallback<String>) -> Unit) {
        val dataSource = object : KeyedDataSource<String, String>() {
            override fun getKey(item: String): String {
                return ""
            }

            override fun loadInitial(
                    params: LoadInitialParams<String>,
                    callback: LoadInitialCallback<String>) {
                callbackInvoker(callback)
            }

            override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String>) {
                fail("loadAfter not expected")
            }

            override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String>) {
                fail("loadBefore not expected")
            }
        }

        ContiguousPagedList<String, String>(
                dataSource, FailExecutor(), FailExecutor(), null,
                PagedList.Config.Builder()
                        .setPageSize(10)
                        .build(),
                "")
    }

    @Test
    fun initialLoadCallbackSuccess() = performInitialLoad {
        // LoadInitialCallback correct usage
        it.onResult(listOf("a", "b"), 0, 2)
    }

    @Test
    fun initialLoadCallbackNotPageSizeMultiple() = performInitialLoad {
        // Keyed LoadInitialCallback *can* accept result that's not a multiple of page size
        val elevenLetterList = List(11) { "" + 'a' + it }
        it.onResult(elevenLetterList, 0, 12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackListTooBig() = performInitialLoad {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b", "c"), 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackPositionTooLarge() = performInitialLoad {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b"), 1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackPositionNegative() = performInitialLoad {
        // LoadInitialCallback can't accept negative position
        it.onResult(listOf("a", "b", "c"), -1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialLoadCallbackEmptyCannotHavePlaceholders() = performInitialLoad {
        // LoadInitialCallback can't accept empty result unless data set is empty
        it.onResult(emptyList(), 0, 2)
    }

    companion object {
        private val ITEM_COMPARATOR = compareBy<Item>({ it.name }).thenByDescending({ it.id })
        private val KEY_COMPARATOR = compareBy<Key>({ it.name }).thenByDescending({ it.id })

        private val ITEMS_BY_NAME_ID = List(100) {
            val names = Array(10) { "f" + ('a' + it) }
            Item(names[it % 10],
                    it,
                    Math.random() * 1000,
                    (Math.random() * 200).toInt().toString() + " fake st.")
        }.sortedWith(ITEM_COMPARATOR)
    }
}
