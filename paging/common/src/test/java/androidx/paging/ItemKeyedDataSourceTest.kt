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
class ItemKeyedDataSourceTest {

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
        val callback = mock(ItemKeyedDataSource.LoadCallback::class.java)
                as ItemKeyedDataSource.LoadCallback<Item>

        dataSource.loadBefore(
                ItemKeyedDataSource.LoadParams(dataSource.getKey(ITEMS_BY_NAME_ID[5]), 5), callback)

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
            : ItemKeyedDataSource<Key, Item>() {

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

    private fun performLoadInitial(
            invalidateDataSource: Boolean = false,
            callbackInvoker: (callback: ItemKeyedDataSource.LoadInitialCallback<String>) -> Unit) {
        val dataSource = object : ItemKeyedDataSource<String, String>() {
            override fun getKey(item: String): String {
                return ""
            }

            override fun loadInitial(
                    params: LoadInitialParams<String>,
                    callback: LoadInitialCallback<String>) {
                if (invalidateDataSource) {
                    // invalidate data source so it's invalid when onResult() called
                    invalidate()
                }
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
                "",
                ContiguousPagedList.LAST_LOAD_UNSPECIFIED)
    }

    @Test
    fun loadInitialCallbackSuccess() = performLoadInitial {
        // LoadInitialCallback correct usage
        it.onResult(listOf("a", "b"), 0, 2)
    }

    @Test
    fun loadInitialCallbackNotPageSizeMultiple() = performLoadInitial {
        // Keyed LoadInitialCallback *can* accept result that's not a multiple of page size
        val elevenLetterList = List(11) { "" + 'a' + it }
        it.onResult(elevenLetterList, 0, 12)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackListTooBig() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b", "c"), 0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackPositionTooLarge() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b"), 1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackPositionNegative() = performLoadInitial {
        // LoadInitialCallback can't accept negative position
        it.onResult(listOf("a", "b", "c"), -1, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackEmptyCannotHavePlaceholders() = performLoadInitial {
        // LoadInitialCallback can't accept empty result unless data set is empty
        it.onResult(emptyList(), 0, 2)
    }

    @Test
    fun initialLoadCallbackInvalidThreeArg() = performLoadInitial(invalidateDataSource = true) {
        // LoadInitialCallback doesn't throw on invalid args if DataSource is invalid
        it.onResult(emptyList(), 0, 1)
    }

    private abstract class WrapperDataSource<K, A, B>(private val source: ItemKeyedDataSource<K, A>)
            : ItemKeyedDataSource<K, B>() {
        override fun addInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.addInvalidatedCallback(onInvalidatedCallback)
        }

        override fun removeInvalidatedCallback(onInvalidatedCallback: InvalidatedCallback) {
            source.removeInvalidatedCallback(onInvalidatedCallback)
        }

        override fun invalidate() {
            source.invalidate()
        }

        override fun isInvalid(): Boolean {
            return source.isInvalid
        }

        override fun loadInitial(params: LoadInitialParams<K>, callback: LoadInitialCallback<B>) {
            source.loadInitial(params, object : LoadInitialCallback<A>() {
                override fun onResult(data: List<A>, position: Int, totalCount: Int) {
                    callback.onResult(convert(data), position, totalCount)
                }

                override fun onResult(data: MutableList<A>) {
                    callback.onResult(convert(data))
                }
            })
        }

        override fun loadAfter(params: LoadParams<K>, callback: LoadCallback<B>) {
            source.loadAfter(params, object : LoadCallback<A>() {
                override fun onResult(data: MutableList<A>) {
                    callback.onResult(convert(data))
                }
            })
        }

        override fun loadBefore(params: LoadParams<K>, callback: LoadCallback<B>) {
            source.loadBefore(params, object : LoadCallback<A>() {
                override fun onResult(data: MutableList<A>) {
                    callback.onResult(convert(data))
                }
            })
        }

        protected abstract fun convert(source: List<A>): List<B>
    }

    private data class DecoratedItem(val item: Item)

    private class DecoratedWrapperDataSource(private val source: ItemKeyedDataSource<Key, Item>)
            : WrapperDataSource<Key, Item, DecoratedItem>(source) {
        override fun convert(source: List<Item>): List<DecoratedItem> {
            return source.map { DecoratedItem(it) }
        }

        override fun getKey(item: DecoratedItem): Key {
            return source.getKey(item.item)
        }
    }

    private fun verifyWrappedDataSource(createWrapper:
            (ItemKeyedDataSource<Key, Item>) -> ItemKeyedDataSource<Key, DecoratedItem>) {
        // verify that it's possible to wrap an ItemKeyedDataSource, and add info to its data

        val orig = ItemDataSource(items = ITEMS_BY_NAME_ID)
        val wrapper = createWrapper(orig)

        // load initial
        @Suppress("UNCHECKED_CAST")
        val loadInitialCallback = mock(ItemKeyedDataSource.LoadInitialCallback::class.java)
                as ItemKeyedDataSource.LoadInitialCallback<DecoratedItem>
        val initKey = orig.getKey(ITEMS_BY_NAME_ID.first())
        wrapper.loadInitial(ItemKeyedDataSource.LoadInitialParams(initKey, 10, false),
                loadInitialCallback)
        verify(loadInitialCallback).onResult(
                ITEMS_BY_NAME_ID.subList(0, 10).map { DecoratedItem(it) })
        verifyNoMoreInteractions(loadInitialCallback)

        @Suppress("UNCHECKED_CAST")
        val loadCallback = mock(ItemKeyedDataSource.LoadCallback::class.java)
                as ItemKeyedDataSource.LoadCallback<DecoratedItem>
        val key = orig.getKey(ITEMS_BY_NAME_ID[20])
        // load after
        wrapper.loadAfter(ItemKeyedDataSource.LoadParams(key, 10), loadCallback)
        verify(loadCallback).onResult(ITEMS_BY_NAME_ID.subList(21, 31).map { DecoratedItem(it) })
        verifyNoMoreInteractions(loadCallback)

        // load before
        wrapper.loadBefore(ItemKeyedDataSource.LoadParams(key, 10), loadCallback)
        verify(loadCallback).onResult(ITEMS_BY_NAME_ID.subList(10, 20).map { DecoratedItem(it) })
        verifyNoMoreInteractions(loadCallback)

        // verify invalidation
        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testManualWrappedDataSource() = verifyWrappedDataSource {
        DecoratedWrapperDataSource(it)
    }

    @Test
    fun testListConverterWrappedDataSource() = verifyWrappedDataSource {
        it.mapByPage { it.map { DecoratedItem(it) } }
    }

    @Test
    fun testItemConverterWrappedDataSource() = verifyWrappedDataSource {
        it.map { DecoratedItem(it) }
    }

    @Test
    fun testInvalidateToWrapper() {
        val orig = ItemDataSource()
        val wrapper = orig.map { DecoratedItem(it) }

        orig.invalidate()
        assertTrue(wrapper.isInvalid)
    }

    @Test
    fun testInvalidateFromWrapper() {
        val orig = ItemDataSource()
        val wrapper = orig.map { DecoratedItem(it) }

        wrapper.invalidate()
        assertTrue(orig.isInvalid)
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
