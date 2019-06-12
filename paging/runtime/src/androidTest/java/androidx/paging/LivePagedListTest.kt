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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class LivePagedListTest {
    @JvmField
    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun toLiveData_config() {
        val livePagedList = dataSourceFactory.toLiveData(config)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(config, livePagedList.value!!.config)
    }

    @Test
    fun toLiveData_pageSize() {
        val livePagedList = dataSourceFactory.toLiveData(24)
        livePagedList.observeForever {}
        assertNotNull(livePagedList.value)
        assertEquals(24, livePagedList.value!!.config.pageSize)
    }

    companion object {
        private val dataSource = object : PositionalDataSource<String>() {
            override fun loadInitial(
                params: LoadInitialParams,
                callback: LoadInitialCallback<String>
            ) {
            }

            override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
            }
        }

        private val dataSourceFactory = object : DataSource.Factory<Int, String>() {
            override fun create(): DataSource<Int, String> {
                return dataSource
            }
        }

        private val config = Config(10)
    }
}
