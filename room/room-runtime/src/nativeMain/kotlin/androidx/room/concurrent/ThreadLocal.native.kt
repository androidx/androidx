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

package androidx.room.concurrent

import kotlin.coroutines.CoroutineContext
import kotlinx.atomicfu.atomic

private val globalThreadCounter = atomic(1L)

// This annotation is not experimental but has a very big warning saying 'it might go away'...
// however, it works as expected. 🙂
@kotlin.native.concurrent.ThreadLocal
private object ThreadLocalData {
    val threadLocalMap = mutableMapOf<Long, Any>()
}

/**
 * Container of thread-local data.
 */
actual class ThreadLocal<T> {
    private val threadId = currentThreadId()
    actual fun get(): T? {
        @Suppress("UNCHECKED_CAST")
        return ThreadLocalData.threadLocalMap[threadId] as? T
    }

    actual fun set(value: T?) {
        if (value == null) {
            ThreadLocalData.threadLocalMap.remove(threadId)
        } else {
            ThreadLocalData.threadLocalMap[threadId] = value
        }
    }
}

internal actual fun <T> ThreadLocal<T>.asContextElement(value: T): CoroutineContext.Element =
    ThreadContextElement()

// A fake ThreadContextElement, see https://github.com/Kotlin/kotlinx.coroutines/issues/3326
private class ThreadContextElement : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ThreadContextElement>

    override val key: CoroutineContext.Key<ThreadContextElement>
        get() = ThreadContextElement
}

@kotlin.native.concurrent.ThreadLocal
private var localThreadId: Long = 0

/**
 * Gets the current thread id.
 */
internal actual fun currentThreadId(): Long {
    if (localThreadId == 0L) {
        localThreadId = globalThreadCounter.getAndIncrement()
    }
    return localThreadId
}
