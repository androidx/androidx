/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.PositionalDataSource
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executor

@RunWith(JUnit4::class)
class PagedListTest {
    @Test
    fun defaults() {
        val pagedList = PagedList(
                dataSource = dataSource,
                config = config,
                fetchExecutor = executor,
                notifyExecutor = executor
        )

        assertEquals(dataSource, pagedList.dataSource)
        assertEquals(config, pagedList.config)
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

        private val config = Config(10)

        private val executor = Executor { runnable -> runnable.run() }
    }
}
