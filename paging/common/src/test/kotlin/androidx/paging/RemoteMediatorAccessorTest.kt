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
import androidx.paging.RemoteMediatorMock.LoadEvent
import androidx.paging.TestPagingSource.Companion.LOAD_ERROR
import com.google.common.truth.Truth.assertThat
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

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalPagingApi::class)
@RunWith(JUnit4::class)
class RemoteMediatorAccessorTest {
    private val testScope = TestCoroutineScope()
    private var mockStateId = 0

    // creates a unique state using the anchor position to be able to do equals check in assertions
    private fun createMockState(
        anchorPosition: Int? = mockStateId++
    ): PagingState<Int, Int> {
        return PagingState(
            pages = listOf(),
            anchorPosition = anchorPosition,
            config = PagingConfig(10),
            leadingPlaceholderCount = COUNT_UNDEFINED
        )
    }

    @Test
    fun requestLoad_queuesBoundaryBehindRefresh() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 100)
        val remoteMediatorAccessor = createAccessor(remoteMediator)
        val firstState = createMockState()
        val secondState = createMockState()

        remoteMediatorAccessor.requestLoad(REFRESH, firstState)
        advanceTimeBy(50) // Start remote refresh, but do not let it finish.
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, firstState)
        )
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete
            )
        )

        // Queue a boundary requests, but it should not launch since refresh is running.
        remoteMediatorAccessor.requestLoad(PREPEND, firstState)
        remoteMediatorAccessor.requestLoad(APPEND, firstState)
        assertThat(remoteMediator.newLoadEvents).isEmpty()
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.Loading,
                append = LoadState.Loading
            )
        )

        // Queue more boundary requests, but with an updated PagingState.
        remoteMediatorAccessor.requestLoad(PREPEND, secondState)
        remoteMediatorAccessor.requestLoad(APPEND, secondState)
        assertThat(remoteMediator.newLoadEvents).isEmpty()
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.Loading,
                append = LoadState.Loading
            )
        )

        // Now wait until all queued requests finish running
        advanceUntilIdle()
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, secondState),
            LoadEvent(APPEND, secondState),
        )
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete
            )
        )
    }

    @Test
    fun requestLoad_cancelledBoundaryRetriesAfterRefresh() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val remoteMediatorAccessor = createAccessor(remoteMediator)
        val firstState = createMockState()

        // Launch boundary calls, but do not let them finish.
        remoteMediatorAccessor.requestLoad(PREPEND, firstState)
        // Single runner should prevent append from triggering, but it should still be queued.
        remoteMediatorAccessor.requestLoad(APPEND, firstState)
        advanceTimeBy(50)
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, firstState),
        )

        // Launch refresh, which should cancel running boundary calls
        remoteMediatorAccessor.requestLoad(REFRESH, firstState)
        advanceTimeBy(50)
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, firstState)
        )

        // Let refresh finish, retrying cancelled boundary calls
        advanceUntilIdle()
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, firstState),
            LoadEvent(APPEND, firstState),
        )
    }

    @Test
    fun requestLoad_queuesBoundaryAfterRefreshFails() = testScope.runBlockingTest {
        val firstState = createMockState()
        val secondState = createMockState()
        val remoteMediator = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
            loadCallback = { loadType, state ->
                // Only error out on first refresh.
                if (loadType == REFRESH && state == firstState) {
                    RemoteMediator.MediatorResult.Error(throwable = LOAD_ERROR)
                } else {
                    RemoteMediator.MediatorResult.Success(endOfPaginationReached = false)
                }
            }
        }
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        // Queue up some remote boundary calls, which will not run immediately because they
        // depend on refresh.
        remoteMediatorAccessor.requestLoad(PREPEND, firstState)
        remoteMediatorAccessor.requestLoad(APPEND, firstState)

        // Trigger refresh, letting it fail.
        remoteMediatorAccessor.requestLoad(REFRESH, firstState)
        advanceUntilIdle()
        // Boundary calls should be queued, but not started.
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, firstState),
        )
        // Although boundary calls are queued, they should not trigger or update LoadState since
        // they are waiting for refresh to succeed.
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(LOAD_ERROR),
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete
            )
        )

        // Let refresh finish, triggering queued boundary calls.
        remoteMediatorAccessor.retryFailed(secondState)
        advanceUntilIdle()
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, secondState),
            LoadEvent(PREPEND, firstState),
            LoadEvent(APPEND, firstState),
        )
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete
            )
        )
    }

    @Test
    fun requestLoad_refreshEndOfPaginationReachedClearsBoundaryCalls() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
            loadCallback = { _, _ ->
                RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
            }
        }
        val remoteMediatorAccessor = createAccessor(remoteMediator)
        val firstState = createMockState()

        // Queue up some remote boundary calls, which will not run immediately because they
        // depend on refresh.
        remoteMediatorAccessor.requestLoad(PREPEND, firstState)
        remoteMediatorAccessor.requestLoad(APPEND, firstState)

        // Trigger refresh and let it mark endOfPaginationReached
        remoteMediatorAccessor.requestLoad(REFRESH, firstState)
        advanceUntilIdle()

        // Ensure boundary calls are not triggered since they should be cleared by
        // endOfPaginationReached from refresh.
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, firstState)
        )
        // Although boundary calls are queued, they should not trigger or update LoadState since
        // they are waiting for refresh.
        assertThat(remoteMediatorAccessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Complete,
                prepend = LoadState.NotLoading.Complete,
                append = LoadState.NotLoading.Complete
            )
        )
    }

    @Test
    fun load_reportsPrependLoadState() = testScope.runBlockingTest {
        val emptyState = PagingState<Int, Int>(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        // Assert initial state is NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(prepend = LoadState.NotLoading.Incomplete),
            remoteMediatorAccessor.state.value,
        )

        // Start a PREPEND load.
        remoteMediatorAccessor.requestLoad(
            loadType = PREPEND,
            pagingState = emptyState,
        )

        // Assert state is immediately set to Loading.
        assertEquals(
            LoadStates.IDLE.copy(prepend = LoadState.Loading),
            remoteMediatorAccessor.state.value,
        )

        // Wait for load to finish.
        advanceUntilIdle()

        // Assert state is set to NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(prepend = LoadState.NotLoading.Incomplete),
            remoteMediatorAccessor.state.value,
        )

        // Start a PREPEND load which results in endOfPaginationReached = true.
        remoteMediator.loadCallback = { _, _ ->
            RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
        }
        remoteMediatorAccessor.requestLoad(
            loadType = PREPEND,
            pagingState = emptyState,
        )

        // Wait for load to finish.
        advanceUntilIdle()

        // Assert state is set to NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(prepend = LoadState.NotLoading.Complete),
            remoteMediatorAccessor.state.value,
        )
    }

    @Test
    fun load_reportsAppendLoadState() = testScope.runBlockingTest {
        val emptyState = PagingState<Int, Int>(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        // Assert initial state is NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(prepend = LoadState.NotLoading.Incomplete),
            remoteMediatorAccessor.state.value,
        )

        // Start a APPEND load.
        remoteMediatorAccessor.requestLoad(
            loadType = APPEND,
            pagingState = emptyState,
        )

        // Assert state is immediately set to Loading.
        assertEquals(
            LoadStates.IDLE.copy(append = LoadState.Loading),
            remoteMediatorAccessor.state.value,
        )

        // Wait for load to finish.
        advanceUntilIdle()

        // Assert state is set to NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(append = LoadState.NotLoading.Incomplete),
            remoteMediatorAccessor.state.value,
        )

        // Start a APPEND load which results in endOfPaginationReached = true.
        remoteMediator.loadCallback = { _, _ ->
            RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
        }
        remoteMediatorAccessor.requestLoad(
            loadType = APPEND,
            pagingState = emptyState,
        )

        // Wait for load to finish.
        advanceUntilIdle()

        // Assert state is set to NotLoading.Incomplete.
        assertEquals(
            LoadStates.IDLE.copy(append = LoadState.NotLoading.Complete),
            remoteMediatorAccessor.state.value,
        )
    }

    @Test
    fun load_conflatesPrepend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        remoteMediatorAccessor.requestLoad(
            loadType = PREPEND,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        remoteMediatorAccessor.requestLoad(
            loadType = PREPEND,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)
    }

    @Test
    fun load_conflatesAppend() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        remoteMediatorAccessor.requestLoad(
            loadType = APPEND,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        remoteMediatorAccessor.requestLoad(
            loadType = APPEND,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)
    }

    @Test
    fun load_conflatesRefresh() = testScope.runBlockingTest {
        val remoteMediator = RemoteMediatorMock(loadDelay = 1000)
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        remoteMediatorAccessor.requestLoad(
            loadType = REFRESH,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        remoteMediatorAccessor.requestLoad(
            loadType = REFRESH,
            pagingState = PagingState(listOf(), null, PagingConfig(10), COUNT_UNDEFINED)
        )

        // Assert that exactly one load request was started.
        assertEquals(1, remoteMediator.newLoadEvents.size)

        // Fast-forward time until both load requests jobs complete.
        advanceUntilIdle()

        // Assert that the second load request was skipped since it was launched while the first
        // load request was still running.
        assertEquals(0, remoteMediator.newLoadEvents.size)
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
        val remoteMediatorAccessor = createAccessor(remoteMediator)

        remoteMediatorAccessor.requestLoad(
            loadType = PREPEND,
            pagingState = emptyState
        )

        remoteMediatorAccessor.requestLoad(
            loadType = APPEND,
            pagingState = emptyState
        )

        // Start prependJob and appendJob, but do not let them finish.
        advanceTimeBy(500)

        // Assert that only the PREPEND RemoteMediator.load() call was made.
        assertEquals(
            listOf(LoadEvent(PREPEND, emptyState)),
            remoteMediator.newLoadEvents
        )

        // Start refreshJob
        remoteMediatorAccessor.requestLoad(
            loadType = REFRESH,
            pagingState = emptyState
        )

        // Give prependJob enough time to be cancelled and refresh started due to higher priority
        advanceTimeBy(500)

        assertEquals(
            listOf(LoadEvent(REFRESH, emptyState)),
            remoteMediator.newLoadEvents
        )
        // assert that all of them are in loading state as we don't know if refresh will succeed
        // if refresh fails, we would retry append / prepend
        assertEquals(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            ),
            remoteMediatorAccessor.state.value
        )

        // Wait for all outstanding / queued jobs to finish.
        advanceUntilIdle()

        // Assert all outstanding / queued jobs finished.
        assertEquals(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete
            ),
            remoteMediatorAccessor.state.value
        )

        // Queued boundary requests should be triggered, even though they are out-of-date.
        assertThat(remoteMediator.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, emptyState),
            LoadEvent(APPEND, emptyState),
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

        val remoteMediatorAccessor = createAccessor(remoteMediator)

        remoteMediatorAccessor.requestLoad(loadType = PREPEND, pagingState = emptyState)

        remoteMediatorAccessor.requestLoad(loadType = APPEND, pagingState = emptyState)

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

    @Test
    fun ignoreAppendPrependWhenRefreshIsRequired() {
        val remoteMediatorMock = RemoteMediatorMock().apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        val accessor = testScope.createAccessor(remoteMediatorMock)
        accessor.requestLoad(APPEND, createMockState())
        accessor.requestLoad(PREPEND, createMockState())
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediatorMock.loadEvents
        ).isEmpty()
    }

    @Test
    fun allowAppendPrependWhenRefreshIsNotRequired() {
        val remoteMediatorMock = RemoteMediatorMock().apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val accessor = testScope.createAccessor(remoteMediatorMock)

        val appendState = createMockState(1)
        val prependState = createMockState(2)
        accessor.requestLoad(APPEND, appendState)
        accessor.requestLoad(PREPEND, prependState)
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediatorMock.loadEvents
        ).containsExactly(
            LoadEvent(APPEND, appendState),
            LoadEvent(PREPEND, prependState)
        )
    }

    @Test
    fun ignoreAppendPrependBeforeRefresh() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        val accessor = testScope.createAccessor(remoteMediatorMock)
        testScope.pauseDispatcher()

        val refreshState = createMockState()
        accessor.requestLoad(REFRESH, refreshState)
        // no requests yet since scope is not triggered
        assertThat(remoteMediatorMock.loadEvents).isEmpty()

        // advance enough to trigger the request, not enough to complete
        testScope.advanceTimeBy(50)
        // these should be ignored
        accessor.requestLoad(REFRESH, createMockState())

        testScope.advanceTimeBy(1)
        val appendState = createMockState()
        accessor.requestLoad(APPEND, appendState)

        val prependState = createMockState()
        testScope.advanceTimeBy(1)
        accessor.requestLoad(PREPEND, prependState)

        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, refreshState)
        )

        // now advance enough that we can accept append prepend
        testScope.advanceUntilIdle()
        // queued append/prepend should be executed afterwards.
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(APPEND, appendState),
            LoadEvent(PREPEND, prependState),
        )

        val otherPrependState = createMockState()
        val otherAppendState = createMockState()
        accessor.requestLoad(PREPEND, otherPrependState)
        accessor.requestLoad(APPEND, otherAppendState)

        testScope.advanceTimeBy(50)
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, otherPrependState)
        )
        // while prepend running, any more requests should be ignored
        accessor.requestLoad(PREPEND, createMockState())
        testScope.advanceTimeBy(10)
        assertThat(remoteMediatorMock.newLoadEvents).isEmpty()

        testScope.advanceTimeBy(41)
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(APPEND, otherAppendState)
        )
        accessor.requestLoad(APPEND, createMockState())
        // while append running, any more requests should be ignored
        accessor.requestLoad(APPEND, createMockState())
        testScope.advanceUntilIdle()
        assertThat(remoteMediatorMock.newLoadEvents).isEmpty()

        // now the work is done, we can add more
        val newAppendState = createMockState()
        accessor.requestLoad(APPEND, newAppendState)
        testScope.advanceUntilIdle()
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(APPEND, newAppendState)
        )
    }

    @Test
    fun dropAppendPrependIfRefreshIsTriggered() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val accessor = testScope.createAccessor(remoteMediatorMock)
        testScope.pauseDispatcher()
        val initialAppend = createMockState()
        accessor.requestLoad(APPEND, initialAppend)
        testScope.advanceTimeBy(50)
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(APPEND, initialAppend)
        )
        // now before that append finishes, trigger a refresh
        val newRefresh = createMockState()
        accessor.requestLoad(REFRESH, newRefresh)
        testScope.advanceTimeBy(10)
        // check that we immediately get the new refresh because we'll cancel the append
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(REFRESH, newRefresh)
        )
        assertThat(
            remoteMediatorMock.incompleteEvents
        ).containsExactly(
            LoadEvent(APPEND, initialAppend)
        )
    }

    @Test
    fun loadEvents() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val accessor = testScope.createAccessor(remoteMediatorMock)
        testScope.pauseDispatcher()

        // Initial state
        assertThat(accessor.state.value).isEqualTo(LoadStates.IDLE)

        // Append request should go through since it doesn't require refresh
        val firstAppendState = createMockState()
        accessor.requestLoad(APPEND, firstAppendState)
        testScope.advanceTimeBy(40)
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.Loading
            )
        )
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(APPEND, firstAppendState)
        )

        // Trigger refresh, cancelling remote append
        val firstRefreshState = createMockState()
        accessor.requestLoad(REFRESH, firstRefreshState)
        testScope.advanceTimeBy(1)
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.Loading
            )
        )
        // advance enough to complete refresh
        testScope.advanceUntilIdle()
        // assert that we receive refresh, and append is retried since it was cancelled
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, firstRefreshState),
            LoadEvent(APPEND, firstAppendState),
        )
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete,
                append = LoadState.NotLoading.Incomplete
            )
        )

        val appendState = createMockState()
        accessor.requestLoad(APPEND, appendState)
        val prependState = createMockState()
        accessor.requestLoad(PREPEND, prependState)
        testScope.advanceTimeBy(50)
        // both states should be set to loading even though prepend is not really running
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Loading,
                append = LoadState.Loading
            )
        )
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(APPEND, appendState)
        )
        // advance enough to trigger prepend
        testScope.advanceTimeBy(51)
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Loading,
                append = LoadState.NotLoading.Incomplete
            )
        )
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(PREPEND, prependState)
        )
        testScope.advanceUntilIdle()
        val exception = Throwable()
        remoteMediatorMock.loadCallback = { type, _ ->
            if (type == PREPEND) {
                RemoteMediator.MediatorResult.Error(
                    exception
                )
            } else {
                null
            }
        }
        accessor.requestLoad(APPEND, createMockState())
        accessor.requestLoad(PREPEND, createMockState())
        testScope.advanceUntilIdle()
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Error(exception),
                append = LoadState.NotLoading.Incomplete
            )
        )
        // now complete append, a.k.a. endOfPaginationReached
        remoteMediatorMock.loadCallback = { type, _ ->
            if (type == APPEND) {
                RemoteMediator.MediatorResult.Success(
                    endOfPaginationReached = true
                )
            } else {
                null
            }
        }
        accessor.requestLoad(APPEND, createMockState())
        testScope.advanceUntilIdle()
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Error(exception),
                append = LoadState.NotLoading.Complete
            )
        )
        // clear events
        remoteMediatorMock.newLoadEvents
        // another append request should just be ignored
        accessor.requestLoad(APPEND, createMockState())
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).isEmpty()
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Error(exception),
                append = LoadState.NotLoading.Complete
            )
        )
        val refreshState = createMockState()
        accessor.requestLoad(REFRESH, refreshState)
        testScope.advanceTimeBy(50)
        // prepend error state is still present
        assertThat(accessor.state.value).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.Error(exception),
                append = LoadState.NotLoading.Complete
            )
        )
        testScope.advanceUntilIdle()
        // if refresh succeeds, it will clear the error state for refresh
        assertThat(accessor.state.value).isEqualTo(
            LoadStates.IDLE
        )
        assertThat(remoteMediatorMock.newLoadEvents).containsExactly(
            LoadEvent(REFRESH, refreshState)
        )
    }

    @Test
    fun retry_refresh() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val exception = Exception()
        val accessor = testScope.createAccessor(remoteMediatorMock)
        remoteMediatorMock.loadCallback = { loadType, _ ->
            delay(100)
            if (loadType == REFRESH) {
                RemoteMediator.MediatorResult.Error(exception)
            } else {
                null
            }
        }
        val firstRefreshState = createMockState()
        accessor.requestLoad(REFRESH, firstRefreshState)
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(LoadEvent(REFRESH, firstRefreshState))
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value.refresh
        ).isEqualTo(
            LoadState.Error(exception)
        )
        val retryState = createMockState()
        accessor.retryFailed(retryState)
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(LoadEvent(REFRESH, retryState))
    }

    @Test
    fun failedRefreshShouldNotAllowAppendPrependIfRefreshIsRequired() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        val exception = Exception()
        val accessor = testScope.createAccessor(remoteMediatorMock)
        remoteMediatorMock.loadCallback = { _, _ ->
            delay(100)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val initialState = createMockState()
        accessor.requestLoad(REFRESH, initialState)
        // even though we are sending append prepend, they won't really trigger since refresh will
        // fail

        // ensure that we didn't set append/prepend to loading when refresh is required
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates.IDLE.modifyState(REFRESH, LoadState.Loading)
        )
        testScope.advanceUntilIdle()
        // make sure only refresh has happened
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(REFRESH, initialState)
        )
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates.IDLE.modifyState(REFRESH, LoadState.Error(exception))
        )
    }

    @Test
    fun failedRefreshShouldAllowAppendPrependIfRefreshIsNotRequired() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val exception = Exception()
        val accessor = testScope.createAccessor(remoteMediatorMock)
        remoteMediatorMock.loadCallback = { loadType, _ ->
            delay(100)
            if (loadType != APPEND) {
                RemoteMediator.MediatorResult.Error(exception)
            } else {
                null // let append succeed
            }
        }
        val initialState = createMockState()
        accessor.requestLoad(REFRESH, initialState)
        accessor.requestLoad(PREPEND, initialState)
        accessor.requestLoad(APPEND, initialState)
        // make sure we optimistically updated append prepend states
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        testScope.advanceUntilIdle()
        // make sure all requests did happen eventually
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(REFRESH, initialState),
            LoadEvent(PREPEND, initialState),
            LoadEvent(APPEND, initialState)

        )
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                append = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Error(exception)
            )
        )
    }

    @Test
    fun retry_retryBothAppendAndPrepend() {
        val remoteMediatorMock = RemoteMediatorMock(loadDelay = 100).apply {
            initializeResult = RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH
        }
        val exception = Exception()
        val accessor = testScope.createAccessor(remoteMediatorMock)
        remoteMediatorMock.loadCallback = { _, _ ->
            delay(100)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val appendState = createMockState()
        val prependState = createMockState()
        accessor.requestLoad(PREPEND, prependState)
        accessor.requestLoad(APPEND, appendState)
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(PREPEND, prependState),
            LoadEvent(APPEND, appendState)
        )
        // now retry, ensure both runs
        val retryState = createMockState()
        accessor.retryFailed(retryState)
        // make sure they both move to loading
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        testScope.advanceUntilIdle()
        // ensure they both got called
        assertThat(
            remoteMediatorMock.newLoadEvents
        ).containsExactly(
            LoadEvent(PREPEND, retryState),
            LoadEvent(APPEND, retryState)
        )
        // make sure new loading states are correct
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                append = LoadState.Error(exception),
                prepend = LoadState.Error(exception)
            )
        )
    }

    @Test
    fun retry_multipleTriggersOnlyRefresh() {
        val remoteMediator = object : RemoteMediatorMock(100) {
            override suspend fun initialize(): InitializeAction {
                return InitializeAction.SKIP_INITIAL_REFRESH
            }
        }
        val exception = Exception()
        remoteMediator.loadCallback = { _, _ ->
            // fail all
            delay(60)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val accessor = testScope.createAccessor(remoteMediator)
        testScope.pauseDispatcher()
        accessor.requestLoad(REFRESH, createMockState())
        accessor.requestLoad(APPEND, createMockState())
        accessor.requestLoad(PREPEND, createMockState())
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        // let refresh start but don't let it finish
        testScope.advanceUntilIdle()
        // get all errors
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                append = LoadState.Error(exception),
                prepend = LoadState.Error(exception)
            )
        )
        // let requests succeed
        remoteMediator.loadCallback = null
        val retryState = createMockState()
        accessor.retryFailed(retryState)
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete
            )
        )
    }

    @Test
    fun failingRefreshRetriesAppendPrepend_refreshNotRequired() {
        val remoteMediator = object : RemoteMediatorMock(100) {
            override suspend fun initialize(): InitializeAction {
                return InitializeAction.SKIP_INITIAL_REFRESH
            }
        }
        val exception = Exception()
        remoteMediator.loadCallback = { type, _ ->
            // only fail for refresh
            if (type == REFRESH) {
                delay(60)
                RemoteMediator.MediatorResult.Error(exception)
            } else {
                null
            }
        }
        val accessor = testScope.createAccessor(remoteMediator)
        testScope.pauseDispatcher()
        accessor.requestLoad(REFRESH, createMockState())
        accessor.requestLoad(APPEND, createMockState())
        accessor.requestLoad(PREPEND, createMockState())
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        // let refresh start but don't let it finish
        testScope.advanceTimeBy(50)
        // make sure refresh does not revert the append / prepend states
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        // let refresh fail, it should retry append prepend
        testScope.advanceTimeBy(20)
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                append = LoadState.Loading,
                prepend = LoadState.Loading
            )
        )
        // let the prepend retry start
        testScope.advanceTimeBy(100)
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                append = LoadState.NotLoading.Incomplete,
                prepend = LoadState.Loading
            )
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                append = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete
            )
        )
    }

    @Test
    fun loadMoreRefreshShouldRetryRefresh() {
        // see: b/173438474
        val remoteMediator = RemoteMediatorMock(loadDelay = 100)
        val exception = Exception()
        remoteMediator.loadCallback = { _, _ ->
            delay(60)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val accessor = testScope.createAccessor(remoteMediator)
        val state1 = createMockState()
        accessor.requestLoad(REFRESH, state1)
        assertThat(
            accessor.state.value.refresh
        ).isEqualTo(
            LoadState.Loading
        )
        // run to get the error
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value.refresh
        ).isEqualTo(
            LoadState.Error(exception)
        )
        // now send another load type refresh, should trigger another load
        remoteMediator.loadCallback = null // let it succeed
        val state2 = createMockState()
        accessor.requestLoad(REFRESH, state2)
        assertThat(
            accessor.state.value.refresh
        ).isEqualTo(
            LoadState.Loading
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value.refresh
        ).isEqualTo(
            LoadState.NotLoading.Incomplete
        )
    }

    @Test
    fun loadMoreRefreshShouldRetryRefresh_withAppendPrependErrors() {
        // see: b/173438474
        val remoteMediator = RemoteMediatorMock(loadDelay = 100)
        val exception = Exception()
        remoteMediator.loadCallback = { _, _ ->
            delay(60)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val accessor = testScope.createAccessor(remoteMediator)
        val state1 = createMockState()
        accessor.requestLoad(REFRESH, state1)
        accessor.requestLoad(APPEND, state1)
        accessor.requestLoad(PREPEND, state1)
        // run to get the error
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception),
                prepend = LoadState.Error(exception),
                append = LoadState.Error(exception),
            )
        )
        // now send another load type refresh, should trigger another load
        remoteMediator.loadCallback = null // let it succeed
        val state2 = createMockState()
        accessor.requestLoad(REFRESH, state2)
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.Error(exception), // keep errors for these for now
                append = LoadState.Error(exception),
            )
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.NotLoading.Incomplete,
                prepend = LoadState.NotLoading.Incomplete, // clear errors
                append = LoadState.NotLoading.Incomplete,
            )
        )
    }

    @Test
    fun loadMoreRefreshShouldRetryRefresh_withAppendPrependErrors_secondRefreshFails() {
        // see: b/173438474
        val remoteMediator = RemoteMediatorMock(loadDelay = 100)
        val exception1 = Exception("1")
        remoteMediator.loadCallback = { _, _ ->
            delay(60)
            RemoteMediator.MediatorResult.Error(exception1)
        }
        val accessor = testScope.createAccessor(remoteMediator)
        val state1 = createMockState()
        accessor.requestLoad(REFRESH, state1)
        accessor.requestLoad(APPEND, state1)
        accessor.requestLoad(PREPEND, state1)
        // run to get the error
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception1),
                prepend = LoadState.Error(exception1),
                append = LoadState.Error(exception1),
            )
        )
        // now send another load type refresh, should trigger another load
        val exception2 = Exception("2")
        remoteMediator.loadCallback = { _, _ ->
            delay(60)
            RemoteMediator.MediatorResult.Error(exception2)
        }
        val state2 = createMockState()
        accessor.requestLoad(REFRESH, state2)
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Loading,
                prepend = LoadState.Error(exception1), // keep errors for these for now
                append = LoadState.Error(exception1),
            )
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value
        ).isEqualTo(
            LoadStates(
                refresh = LoadState.Error(exception2),
                prepend = LoadState.Error(exception1), // these keep their original exceptions
                append = LoadState.Error(exception1),
            )
        )
    }

    @Test
    fun requireRetry_append() {
        requireRetry(APPEND)
    }

    @Test
    fun requireRetry_prepend() {
        requireRetry(PREPEND)
    }

    private fun requireRetry(loadType: LoadType) {
        // ensure that we don't retry a failed request until a retry arrives.
        val remoteMediator = RemoteMediatorMock(100)
        val exception = Exception()
        remoteMediator.loadCallback = { _, _ ->
            delay(60)
            RemoteMediator.MediatorResult.Error(exception)
        }
        val accessor = testScope.createAccessor(remoteMediator)
        testScope.pauseDispatcher()
        val state1 = createMockState()
        accessor.requestLoad(loadType, state1)
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.Loading
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.Error(exception)
        )
        assertThat(
            remoteMediator.newLoadEvents
        ).containsExactly(
            LoadEvent(loadType, state1)
        )
        // subsequent add calls shouldn't do anything
        accessor.requestLoad(loadType, createMockState())
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.Error(exception)
        )
        testScope.advanceUntilIdle()
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.Error(exception)
        )
        assertThat(remoteMediator.newLoadEvents).isEmpty()

        // if we send a retry, then it will work
        remoteMediator.loadCallback = null
        val retryState = createMockState()
        accessor.retryFailed(retryState)
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.Loading
        )
        testScope.advanceUntilIdle()
        assertThat(
            remoteMediator.newLoadEvents
        ).containsExactly(
            LoadEvent(loadType, retryState)
        )
        assertThat(
            accessor.state.value.get(loadType)
        ).isEqualTo(
            LoadState.NotLoading.Incomplete
        )
    }

    private fun TestCoroutineScope.createAccessor(
        mediator: RemoteMediatorMock
    ): RemoteMediatorAccessor<Int, Int> {
        val accessor = RemoteMediatorAccessor(
            scope = this,
            delegate = mediator
        )
        TestCoroutineScope().launch {
            accessor.initialize()
        }
        return accessor
    }
}
