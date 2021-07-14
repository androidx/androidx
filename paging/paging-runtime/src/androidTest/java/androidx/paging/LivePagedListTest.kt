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

import android.view.View
import android.view.ViewGroup
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.TestDispatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@SmallTest
@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
class LivePagedListTest {
    @JvmField
    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testScope = TestCoroutineScope()

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun invalidPagingSourceOnInitialLoadTriggersInvalidation() {
        var pagingSourcesCreated = 0
        val pagingSourceFactory = {
            when (pagingSourcesCreated++) {
                0 -> TestPagingSource().apply {
                    invalidate()
                }
                else -> TestPagingSource()
            }
        }

        val livePagedList = LivePagedList(
            coroutineScope = GlobalScope,
            initialKey = null,
            config = PagedList.Config.Builder().setPageSize(10).build(),
            boundaryCallback = null,
            pagingSourceFactory = pagingSourceFactory,
            notifyDispatcher = ArchTaskExecutor.getMainThreadExecutor().asCoroutineDispatcher(),
            fetchDispatcher = ArchTaskExecutor.getIOThreadExecutor().asCoroutineDispatcher(),
        )

        livePagedList.observeForever { }
        assertThat(pagingSourcesCreated).isEqualTo(2)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun instantiatesPagingSourceOnFetchDispatcher() {
        var pagingSourcesCreated = 0
        val pagingSourceFactory = {
            pagingSourcesCreated++
            TestPagingSource()
        }
        val testDispatcher = TestDispatcher()
        val livePagedList = LivePagedList(
            coroutineScope = GlobalScope,
            initialKey = null,
            config = PagedList.Config.Builder().setPageSize(10).build(),
            boundaryCallback = null,
            pagingSourceFactory = pagingSourceFactory,
            notifyDispatcher = ArchTaskExecutor.getMainThreadExecutor().asCoroutineDispatcher(),
            fetchDispatcher = testDispatcher,
        )

        assertTrue { testDispatcher.queue.isEmpty() }
        assertEquals(0, pagingSourcesCreated)

        livePagedList.observeForever { }

        assertTrue { testDispatcher.queue.isNotEmpty() }
        assertEquals(0, pagingSourcesCreated)

        testDispatcher.executeAll()
        assertEquals(1, pagingSourcesCreated)
    }

    @Test
    fun toLiveData_dataSourceConfig() {
        val livePagedList = dataSourceFactory.toLiveData(config)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(config, livePagedList.value!!.config)
    }

    @Test
    fun toLiveData_dataSourcePageSize() {
        val livePagedList = dataSourceFactory.toLiveData(24)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(24, livePagedList.value!!.config.pageSize)
    }

    @Test
    fun toLiveData_pagingSourceConfig() {
        val livePagedList = pagingSourceFactory.toLiveData(config)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(config, livePagedList.value!!.config)
    }

    @Test
    fun toLiveData_pagingSourcePageSize() {
        val livePagedList = pagingSourceFactory.toLiveData(24)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(24, livePagedList.value!!.config.pageSize)
    }

    /**
     * Some paging2 tests might be using InstantTaskExecutor and expect first page to be loaded
     * immediately. This test replicates that by checking observe forever receives the value in
     * its own call stack.
     */
    @Test
    fun instantExecutionWorksWithLegacy() {
        val totalSize = 300
        val data = (0 until totalSize).map { "$it/$it" }
        val factory = object : DataSource.Factory<Int, String>() {
            override fun create(): DataSource<Int, String> {
                return TestPositionalDataSource(data)
            }
        }

        class TestAdapter : PagedListAdapter<String, RecyclerView.ViewHolder>(
            DIFF_STRING
        ) {
            // open it up by overriding
            public override fun getItem(position: Int): String? {
                return super.getItem(position)
            }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                return object : RecyclerView.ViewHolder(View(parent.context)) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            }
        }

        val livePagedList = LivePagedListBuilder(
            factory,
            PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(30)
                .build()
        ).build()

        val adapter = TestAdapter()
        livePagedList.observeForever { pagedList ->
            // make sure observeForever worked sync where it did load the data immediately
            assertThat(
                Throwable().stackTraceToString()
            ).contains("observeForever")
            assertThat(pagedList.loadedCount).isEqualTo(90)
        }
        adapter.submitList(checkNotNull(livePagedList.value))
        assertThat(adapter.itemCount).isEqualTo(90)

        (0 until totalSize).forEach {
            // getting that item will trigger load around which should load the item immediately
            assertThat(adapter.getItem(it)).isEqualTo("$it/$it")
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun initialLoad_loadResultInvalid() = testScope.runBlockingTest {
        val dispatcher = coroutineContext[CoroutineDispatcher.Key]!!
        val pagingSources = mutableListOf<TestPagingSource>()
        val factory = {
            TestPagingSource().also {
                if (pagingSources.size == 0) it.nextLoadResult = PagingSource.LoadResult.Invalid()
                pagingSources.add(it)
            }
        }
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPageSize(3)
            .build()

        val livePagedList = LivePagedList(
            coroutineScope = testScope,
            initialKey = null,
            config = config,
            boundaryCallback = null,
            pagingSourceFactory = factory,
            notifyDispatcher = dispatcher,
            fetchDispatcher = dispatcher,
        )

        val pagedLists = mutableListOf<PagedList<Int>>()
        livePagedList.observeForever {
            pagedLists.add(it)
        }

        advanceUntilIdle()

        assertThat(pagedLists.size).isEqualTo(2)
        assertThat(pagingSources.size).isEqualTo(2)
        assertThat(pagedLists.size).isEqualTo(2)
        assertThat(pagedLists[1]).containsExactly(
            0, 1, 2, 3, 4, 5, 6, 7, 8
        )
    }

    companion object {
        @Suppress("DEPRECATION")
        private val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {}
        }

        private val dataSourceFactory = object : DataSource.Factory<Int, String>() {
            override fun create(): DataSource<Int, String> = dataSource
        }

        private val pagingSourceFactory = dataSourceFactory.asPagingSourceFactory(
            fetchDispatcher = Dispatchers.Main
        )

        private val config = Config(10)
        private val DIFF_STRING = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    }
}
