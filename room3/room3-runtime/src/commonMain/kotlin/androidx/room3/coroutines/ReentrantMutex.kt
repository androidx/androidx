/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.coroutines

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A reentrant version of [Mutex.withLock] that allows the same coroutine to enter the critical
 * region.
 *
 * See also the relevant [GitHub Issue](https://github.com/Kotlin/kotlinx.coroutines/issues/1686).
 */
internal suspend fun <T> Mutex.withReentrantLock(block: suspend () -> T): T {
    val key = ReentrantMutexContextKey(this)
    return if (currentCoroutineContext()[key] != null) {
        // Call block directly when this mutex is already locked in the context
        block()
    } else {
        // Otherwise add it to the context and lock the mutex
        withContext(ReentrantMutexContextElement(key)) { withLock { block() } }
    }
}

private class ReentrantMutexContextElement(override val key: ReentrantMutexContextKey) :
    CoroutineContext.Element

private data class ReentrantMutexContextKey(val mutex: Mutex) :
    CoroutineContext.Key<ReentrantMutexContextElement>
