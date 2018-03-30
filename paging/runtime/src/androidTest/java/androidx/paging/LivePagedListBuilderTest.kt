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

package androidx.paging

import android.support.test.filters.SmallTest
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class LivePagedListBuilderTest {
    private val backgroundExecutor = TestExecutor()
    private val lifecycleOwner = object : LifecycleOwner {
        private val lifecycle = LifecycleRegistry(this)

        override fun getLifecycle(): Lifecycle {
            return lifecycle
        }

        fun handleEvent(event: Lifecycle.Event) {
            lifecycle.handleLifecycleEvent(event)
        }
    }

    @Before
    fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                fail("IO executor should be overwritten")
            }

            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }

            override fun isMainThread(): Boolean {
                return true
            }
        })
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_START)
    }

    @After
    fun teardown() {
        lifecycleOwner.handleEvent(Lifecycle.Event.ON_STOP)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private class MockDataSource : PositionalDataSource<String>() {
        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<String>) {
            assertEquals(2, params.pageSize)
            callback.onResult(listOf("a", "b"), 0, 4)
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<String>) {
            callback.onResult(listOf("c", "d"))
        }

        class Factory : DataSource.Factory<Int, String>() {
            override fun create(): DataSource<Int, String> {
                return MockDataSource()
            }
        }
    }

    @Test
    fun executorBehavior() {
        // specify a background executor via builder, and verify it gets used for all loads,
        // overriding default arch IO executor
        val livePagedList = LivePagedListBuilder(
                MockDataSource.Factory(), 2)
                .setFetchExecutor(backgroundExecutor)
                .build()

        val pagedListHolder: Array<PagedList<String>?> = arrayOfNulls(1)

        livePagedList.observe(lifecycleOwner, Observer<PagedList<String>> { newList ->
            pagedListHolder[0] = newList
        })

        // won't compute until we flush...
        assertNull(pagedListHolder[0])

        // flush loadInitial, done with passed executor
        backgroundExecutor.executeAll()

        val pagedList = pagedListHolder[0]
        assertNotNull(pagedList)
        assertEquals(listOf("a", "b", null, null), pagedList)

        // flush loadRange
        pagedList!!.loadAround(2)
        backgroundExecutor.executeAll()

        assertEquals(listOf("a", "b", "c", "d"), pagedList)
    }
}
