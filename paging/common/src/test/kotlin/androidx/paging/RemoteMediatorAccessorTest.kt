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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.paging

import androidx.paging.LoadType.END
import androidx.paging.LoadType.START
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class RemoteMediatorAccessorTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun load_concurrentJobsShouldRunSerially() = testScope.runBlockingTest {
        val remoteMediator = object : RemoteMediatorMock() {
            var loading = AtomicBoolean(false)
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                if (!loading.compareAndSet(false, true)) {
                    fail("Concurrent load")
                }

                val result = super.load(loadType, state)

                delay(1000)
                loading.set(false)

                return result
            }
        }
        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        launch {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = START,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        launch {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = END,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        // Assert that only one job runs due to second job joining the first before starting.
        assertEquals(1, remoteMediator.loadEvents.size)

        // Advance some time, but not enough to finish first load.
        advanceTimeBy(500)
        assertEquals(1, remoteMediator.loadEvents.size)

        // Assert that second job starts after first finishes.
        advanceTimeBy(500)
        assertEquals(2, remoteMediator.loadEvents.size)

        // Allow second job to finish.
        advanceTimeBy(1000)
    }
}