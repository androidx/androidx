/*
 * Copyright 2017 The Android Open Source Project
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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PageKeyedDataSourceTest {
    private val mMainThread = TestExecutor()
    private val mBackgroundThread = TestExecutor()

    internal data class Item(val name: String)

    internal data class Page(val prev: String?, val data: List<Item>, val next: String?)

    internal class ItemDataSource(val data: Map<String, Page> = PAGE_MAP)
            : PageKeyedDataSource<String, Item>() {

        private fun getPage(key: String): Page = data[key]!!

        override fun loadInitial(
                params: LoadInitialParams<String>,
                callback: LoadInitialCallback<String, Item>) {
            val page = getPage(INIT_KEY)
            callback.onResult(page.data, page.prev, page.next)
        }

        override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, Item>) {
            val page = getPage(params.key)
            callback.onResult(page.data, page.prev)
        }

        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, Item>) {
            val page = getPage(params.key)
            callback.onResult(page.data, page.next)
        }
    }

    @Test
    fun loadFullVerify() {
        // validate paging entire ItemDataSource results in full, correctly ordered data
        val pagedList = ContiguousPagedList<String, Item>(ItemDataSource(),
                mMainThread, mBackgroundThread,
                null, PagedList.Config.Builder().setPageSize(100).build(), null)

        // validate initial load
        assertEquals(PAGE_MAP[INIT_KEY]!!.data, pagedList)

        // flush the remaining loads
        for (i in 0..PAGE_MAP.keys.size) {
            pagedList.loadAround(0)
            pagedList.loadAround(pagedList.size - 1)
            drain()
        }

        // validate full load
        assertEquals(ITEM_LIST, pagedList)
    }

    private fun performLoadInitial(callbackInvoker:
            (callback: PageKeyedDataSource.LoadInitialCallback<String, String>) -> Unit) {
        val dataSource = object : PageKeyedDataSource<String, String>() {
            override fun loadInitial(
                    params: LoadInitialParams<String>,
                    callback: LoadInitialCallback<String, String>) {
                callbackInvoker(callback)
            }

            override fun loadBefore(
                    params: LoadParams<String>,
                    callback: LoadCallback<String, String>) {
                fail("loadBefore not expected")
            }

            override fun loadAfter(
                    params: LoadParams<String>,
                    callback: LoadCallback<String, String>) {
                fail("loadAfter not expected")
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
    fun loadInitialCallbackSuccess() = performLoadInitial {
        // LoadInitialCallback correct usage
        it.onResult(listOf("a", "b"), 0, 2, null, null)
    }

    @Test
    fun loadInitialCallbackNotPageSizeMultiple() = performLoadInitial {
        // Keyed LoadInitialCallback *can* accept result that's not a multiple of page size
        val elevenLetterList = List(11) { "" + 'a' + it }
        it.onResult(elevenLetterList, 0, 12, null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackListTooBig() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b", "c"), 0, 2, null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackPositionTooLarge() = performLoadInitial {
        // LoadInitialCallback can't accept pos + list > totalCount
        it.onResult(listOf("a", "b"), 1, 2, null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackPositionNegative() = performLoadInitial {
        // LoadInitialCallback can't accept negative position
        it.onResult(listOf("a", "b", "c"), -1, 2, null, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun loadInitialCallbackEmptyCannotHavePlaceholders() = performLoadInitial {
        // LoadInitialCallback can't accept empty result unless data set is empty
        it.onResult(emptyList(), 0, 2, null, null)
    }

    companion object {
        // first load is 2nd page to ensure we test prepend as well as append behavior
        private val INIT_KEY: String = "key 2"
        private val PAGE_MAP: Map<String, Page>
        private val ITEM_LIST: List<Item>

        init {
            val map = HashMap<String, Page>()
            val list = ArrayList<Item>()
            val pageCount = 5
            for (i in 1..pageCount) {
                val data = List(4) { Item("name $i $it") }
                list.addAll(data)

                val key = "key $i"
                val prev = if (i > 1) ("key " + (i - 1)) else null
                val next = if (i < pageCount) ("key " + (i + 1)) else null
                map.put(key, Page(prev, data, next))
            }
            PAGE_MAP = map
            ITEM_LIST = list
        }
    }

    private fun drain() {
        var executed: Boolean
        do {
            executed = mBackgroundThread.executeAll()
            executed = mMainThread.executeAll() || executed
        } while (executed)
    }
}
