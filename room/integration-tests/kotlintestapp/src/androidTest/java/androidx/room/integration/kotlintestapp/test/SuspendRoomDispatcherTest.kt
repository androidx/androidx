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

package androidx.room.integration.kotlintestapp.test

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.DefaultTaskExecutor
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * A small test to verify Room's executor is used as dispatcher for DAO suspend functions.
 */
@SmallTest
class SuspendRoomDispatcherTest : TestDatabaseTest() {

    val executeCount = AtomicInteger()

    @Before
    fun setup() {
        ArchTaskExecutor.getInstance().setDelegate(object : DefaultTaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                executeCount.incrementAndGet()
                super.executeOnDiskIO(runnable)
            }
        })
    }

    @After
    fun teardown() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    @Test
    fun testIODispatcher() {
        runBlocking {
            booksDao.getBooksSuspend()

            assertThat(executeCount.get(), `is`(1))
        }
    }
}