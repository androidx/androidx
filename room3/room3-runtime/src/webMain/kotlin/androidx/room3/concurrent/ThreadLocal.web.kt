/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.concurrent

import androidx.annotation.RestrictTo
import kotlin.coroutines.CoroutineContext

private object ThreadLocalData {
    val threadLocalMap = mutableMapOf<Long, Any>()
}

/** Container of thread-local data. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public actual class ThreadLocal<T> {
    private val threadId = currentThreadId()

    public actual fun get(): T? {
        @Suppress("UNCHECKED_CAST")
        return ThreadLocalData.threadLocalMap[threadId] as? T
    }

    public actual fun set(value: T?) {
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

/** Gets the current thread id. */
internal actual fun currentThreadId(): Long {
    return 1L // There is no multi-threading in web
}
