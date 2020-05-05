/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.paging.RemoteMediator.MediatorResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Usage of [RemoteMediator] within [PageFetcher] and [PageFetcherSnapshot] should always be
 * accessed behind this class, which handles state tracking of active remote jobs.
 */
@OptIn(ExperimentalPagingApi::class)
internal class RemoteMediatorAccessor<Key : Any, Value : Any>(
    private val remoteMediator: RemoteMediator<Key, Value>
) {
    private val jobsByLoadTypeLock = Mutex()
    private val jobsByLoadType = HashMap<LoadType, Deferred<MediatorResult>>()

    suspend fun initialize(): RemoteMediator.InitializeAction {
        return remoteMediator.initialize()
    }

    /**
     * Launches a remote load request with the backing [MediatorResult] if no current
     * [kotlinx.coroutines.Job] for the passed [LoadType] is running, otherwise returns the
     * result of the existing [kotlinx.coroutines.Job].
     */
    internal suspend fun load(
        scope: CoroutineScope,
        loadType: LoadType,
        state: PagingState<Key, Value>
    ): MediatorResult {
        val deferred = jobsByLoadTypeLock.withLock {
            if (jobsByLoadType[loadType]?.isActive != true) {
                // List of RemoteMediator.load jobs that were registered prior to this one.
                val existingJobs = jobsByLoadType.values.toList() // Immutable copy.
                val existingBoundaryJobs = listOfNotNull(
                    jobsByLoadType[LoadType.PREPEND],
                    jobsByLoadType[LoadType.APPEND]
                )

                // Launch the actual call to RemoteMediator.load asynchronously to release
                // jobsByLoadTypeLock.
                jobsByLoadType[loadType] = scope.async {
                    doLoad(loadType, state, existingJobs, existingBoundaryJobs)
                }
            }

            jobsByLoadType.getValue(loadType)
        }

        return deferred.await()
    }

    private suspend fun doLoad(
        loadType: LoadType,
        state: PagingState<Key, Value>,
        existingJobs: List<Job>,
        existingBoundaryJobs: List<Job>
    ): MediatorResult {
        if (loadType == LoadType.REFRESH) {
            // Since RemoteMediator is expected to perform writes to the local DB
            // in the common case, it's not safe to just cancel and proceed with
            // REFRESH here. If we do that, the REFRESH could race with e.g. the
            // START job, and it's unsafe for an old START to land in the DB after
            // a newer REFRESH. Due to cooperative cancellation, the START job may
            // not actually realize it's cancelled before performing its write.
            existingBoundaryJobs.forEach { it.cancel() }
            existingBoundaryJobs.joinAll()
        }

        // Only allow one active RemoteMediator.load at a time, by joining all jobs
        // registered in jobsByLoadType before this one.
        existingJobs.forEach { it.join() }

        return remoteMediator.load(loadType, state)
    }
}
