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

import androidx.annotation.RestrictTo
import kotlin.coroutines.CoroutineContext

/**
 * Container of thread-local data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // Public + lib restricted so we can typealias in JVM.
expect class ThreadLocal<T>() {
    fun get(): T?
    fun set(value: T?)
}

/**
 * Creates a [CoroutineContext.Element] from this thread local that will transfer the data to
 * threads resumed by the coroutine context.
 */
internal expect fun <T> ThreadLocal<T>.asContextElement(value: T): CoroutineContext.Element

/**
 * Gets the current thread id.
 */
internal expect fun currentThreadId(): Long
