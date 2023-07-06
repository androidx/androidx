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
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.sync.Mutex

/**
 * Provide similar functionality of {@link kotlinx.coroutines.sync.Mutex#withLock} but don't
 * wait for the lock if unavailable, instead it passes a Boolean into the [block] lambda to
 * indicate if it is able to get the lock and run [block] immediately.
 *
 * [block] is guaranteed to be called once and only once by this function.
 */
@ExperimentalContracts
internal inline fun <R> Mutex.withTryLock(owner: Any? = null, block: (Boolean) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val locked: Boolean = tryLock(owner)
    try {
        return block(locked)
    } finally {
        if (locked) {
            unlock(owner)
        }
    }
}
