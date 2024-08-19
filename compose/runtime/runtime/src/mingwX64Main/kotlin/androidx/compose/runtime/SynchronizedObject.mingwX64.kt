/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.compose.runtime

import kotlinx.atomicfu.*

@PublishedApi
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    lock.run {
        lock()
        return try {
            block()
        } finally {
            unlock()
        }
    }
}

/**
 * Re-entrant spin lock implementation.
 *
 * `SynchronizedObject` from `kotlinx-atomicfu` library was used before.
 * However, it is still [experimental](https://github.com/Kotlin/kotlinx-atomicfu?tab=readme-ov-file#locks)
 * and has [a performance problem](https://github.com/Kotlin/kotlinx-atomicfu/issues/412)
 * that seriously affects Compose.
 *
 * Using a posix mutex is [problematic for mingwX64](https://youtrack.jetbrains.com/issue/KT-70449/Posix-declarations-differ-much-for-mingwX64-and-LinuxDarwin-targets),
 * so we just use a simple spin lock for mingwX64 (maybe reconsidered in case of problems).
 */
internal actual class SynchronizedObject actual constructor() {

    companion object {
        private const val NO_OWNER = -1L
    }

    private val owner: AtomicLong = atomic(NO_OWNER)
    private var reEnterCount: Int = 0

    fun lock() {
        if (owner.value == currentThreadId()) {
            reEnterCount += 1
        } else {
            // Busy wait
            while (!owner.compareAndSet(NO_OWNER, currentThreadId())){}
        }
    }

    fun unlock() {
        require (owner.value == currentThreadId())
        if (reEnterCount > 0) {
            reEnterCount -= 1
        } else {
            owner.value = NO_OWNER
        }
    }
}
