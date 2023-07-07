/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.core

import kotlinx.coroutines.flow.Flow

/**
 * InterProcessCoordinator provides functionalities that support DataStore instances to coordinate
 * the concurrent work running on multiple threads and multiple processes to guarantee its data
 * consistency. Typically users should use default coordinators provided by the library, including
 * [SingleProcessCoordinator] for use cases where DataStore is only used in a single process, and
 * [MultiProcessCoordinator] for a DataStore that needs to be accessed in multiple processes.
 */
interface InterProcessCoordinator {

    /**
     * A flow that emits a Unit when the data for the DataStore changes. [DataStore] collects this
     * flow to signal the action to invalidate cache and re-read data from disk.
     */
    val updateNotifications: Flow<Unit>

    /**
     * Get the exclusive lock shared by the coordinators from DataStore instances (even from
     * different processes) to run a suspending code [block] that returns type `T`. It guarantees
     * one-at-a-time execution for all the [block] called with this method. If some other process
     * or thread is holding the lock, it will wait until the lock is available.
     *
     * @param block The block of code that is performed with the lock resource.
     */
    suspend fun <T> lock(block: suspend () -> T): T

    /**
     * Attempt to get the exclusive lock shared by the coordinators from DataStore instances (even
     * from different processes) and run the code [block] regardless of the attempt result. Pass a
     * boolean to [block] to indicate if the attempt succeeds. If the attempt fails, [block] will
     * run immediately after the attempt, without waiting for the lock to become available.
     *
     * @param block The block of code that is performed after attempting to get the lock resource.
     * Block will receive a Boolean parameter which is true if the try lock succeeded.
     */
    suspend fun <T> tryLock(block: suspend (Boolean) -> T): T

    /**
     * Atomically get the current version. [DataStore] instances for the same data use this method
     * to access the shared version for its cached data and internal state. Notice concurrent access
     * to the version should guarantee data consistency.
     */
    suspend fun getVersion(): Int

    /**
     * Atomically increment version and return the new version. [DataStore] instances for the same
     * data use this method to access the shared version for its cached data and internal state.
     * Notice concurrent access to the version should guarantee data consistency.
     *
     * Note that the number of calls to the `incrementAndGetVersion` is an internal implementation
     * detail for DataStore and implementers of this API should not make any assumption based on the
     * number of version increments.
     */
    suspend fun incrementAndGetVersion(): Int
}

/**
 * Create a coordinator for single process use cases.
 */
fun createSingleProcessCoordinator(): InterProcessCoordinator = SingleProcessCoordinator()
