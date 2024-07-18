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

import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * SingleProcessCoordinator does coordination within a single process. It is used as the default
 * [InterProcessCoordinator] immplementation unless otherwise specified.
 */
internal class SingleProcessCoordinator() : InterProcessCoordinator {
    private val mutex = Mutex()
    private val version = AtomicInt(0)

    override val updateNotifications: Flow<Unit> = flow {}

    // run block with the exclusive lock
    override suspend fun <T> lock(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }

    // run block with an attempt to get the exclusive lock, still run even if
    // attempt fails. Pass a boolean to indicate if the attempt succeeds.
    @OptIn(ExperimentalContracts::class) // withTryLock
    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        return mutex.withTryLock {
            block(it)
        }
    }

    // get the current version
    override suspend fun getVersion(): Int = version.get()

    // increment version and return the new one
    override suspend fun incrementAndGetVersion(): Int = version.incrementAndGet()
}
