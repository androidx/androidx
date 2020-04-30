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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.LoadType.REFRESH
import androidx.paging.PagingSource.LoadResult.Page.Companion.COUNT_UNDEFINED
import androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.yield
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(JUnit4::class)
class RemoteMediatorAccessorTest {
    private val testScope = TestCoroutineScope()

    @Test
    fun load_runsToCompletion() {
        val remoteMediator = object : RemoteMediatorMock() {
            override suspend fun initialize(): InitializeAction {
                return LAUNCH_INITIAL_REFRESH
            }

            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                yield() // Force yield on scope.async.
                return super.load(loadType, state)
            }
        }

        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        // Assert that a load call doesn't infinitely block on itself.
        runBlocking {
            remoteMediatorAccessor.load(
                // testScope is purposely not passed here to avoid DelayController trivializing
                // this test, as it would immediately resolve the async call to
                // RemoteMediator.load before it has a chance to write to RemoteMediatorAccessor
                // state.
                scope = CoroutineScope(EmptyCoroutineContext),
                loadType = REFRESH,
                state = PagingState(
                    pages = listOf(),
                    anchorPosition = null,
                    config = PagingConfig(10),
                    placeholdersBefore = COUNT_UNDEFINED
                )
            )
        }
    }

    @Test
    fun load_conflatesPrepend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        val result1 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = PREPEND,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        val result2 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = PREPEND,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)

        // Assert that both jobs resolve to the same result.
        assertEquals(result1.await(), result2.await())
    }

    @Test
    fun load_conflatesAppend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        val result1 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = APPEND,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        val result2 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = APPEND,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)

        // Assert that both jobs resolve to the same result.
        assertEquals(result1.await(), result2.await())
    }

    @Test
    fun load_conflatesRefresh() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        val result1 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = REFRESH,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        val result2 = async {
            remoteMediatorAccessor.load(
                scope = testScope,
                loadType = REFRESH,
                state = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
            )
        }

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)

        // Assert that both jobs resolve to the same result.
        assertEquals(result1.await(), result2.await())
    }

    @Test
    fun load_concurrentInitializeJobCancelsBoundaryJobs() = testScope.runBlockingTest {
        val emptyState = PagingState<Int, Int>(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        val remoteMediator = object : RemoteMediatorMock(loadDelay = 1000) {
            var loading = AtomicBoolean(false)
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                if (!loading.compareAndSet(false, true)) fail("Concurrent load")

                return try {
                    super.load(loadType, state)
                } finally {
                    loading.set(false)
                }
            }
        }
        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        val prependJob = launch {
            pauseDispatcher {
                remoteMediatorAccessor.load(
                    scope = testScope,
                    loadType = PREPEND,
                    state = emptyState
                )
            }
        }

        val appendJob = launch {
            pauseDispatcher {
                remoteMediatorAccessor.load(
                    scope = testScope,
                    loadType = APPEND,
                    state = emptyState
                )
            }
        }

        // Start prependJob and appendJob, but do not let them finish.
        advanceTimeBy(500)

        // Assert that only the PREPEND RemoteMediator.load() call was made.
        assertEquals(
            listOf(RemoteMediatorMock.LoadEvent(PREPEND, emptyState)),
            remoteMediator.newLoadEvents
        )

        // Start refreshJob
        val refreshJob = launch {
            pauseDispatcher {
                remoteMediatorAccessor.load(
                    scope = testScope,
                    loadType = REFRESH,
                    state = emptyState
                )
            }
        }

        // Give prependJob enough time to finish.
        advanceTimeBy(500)

        // Assert that both prependJob and appendJob were cancelled by refreshJob.
        assertTrue { prependJob.isCancelled }
        assertTrue { appendJob.isCancelled }

        // Finish refreshJob.
        advanceUntilIdle()

        // Assert refreshJob finishes successfully and no further interactions with
        // RemoteMediator.load() are made.
        assertFalse { refreshJob.isCancelled }
        assertTrue { refreshJob.isCompleted }
        assertEquals(
            listOf(RemoteMediatorMock.LoadEvent(REFRESH, emptyState)),
            remoteMediator.newLoadEvents
        )
    }

    @Test
    fun load_concurrentBoundaryJobsRunsSerially() = testScope.runBlockingTest {
        val emptyState = PagingState<Int, Int>(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        val remoteMediator = object : RemoteMediatorMock(loadDelay = 1000) {
            var loading = AtomicBoolean(false)
            override suspend fun load(
                loadType: LoadType,
                state: PagingState<Int, Int>
            ): MediatorResult {
                if (!loading.compareAndSet(false, true)) fail("Concurrent load")

                return try {
                    super.load(loadType, state)
                } finally {
                    loading.set(false)
                }
            }
        }

        val remoteMediatorAccessor = RemoteMediatorAccessor(remoteMediator)

        launch {
            remoteMediatorAccessor.load(scope = testScope, loadType = PREPEND, state = emptyState)
        }

        launch {
            remoteMediatorAccessor.load(scope = testScope, loadType = APPEND, state = emptyState)
        }

        // Assert that only one job runs due to second job joining the first before starting.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Advance some time, but not enough to finish first load.
        advanceTimeBy(500)
        assertEquals(0, remoteMediator.newLoadEvents.size)

        // Assert that second job starts after first finishes.
        advanceTimeBy(500)
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Allow second job to finish.
        advanceTimeBy(1000)
    }
}