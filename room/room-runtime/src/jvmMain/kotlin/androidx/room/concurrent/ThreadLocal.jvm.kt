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
import kotlinx.coroutines.asContextElement

/**
 * Container of thread-local data.
 */
actual typealias ThreadLocal<T> = java.lang.ThreadLocal<T>

/**
 * Creates a [CoroutineContext.Element] from this thread local that will transfer the data to
 * threads resumed by the coroutine context.
 */
internal actual fun <T> ThreadLocal<T>.asContextElement(value: T): CoroutineContext.Element =
    this.asContextElement(value)

/**
 * Gets the current thread id.
 */
internal actual fun currentThreadId(): Long = Thread.currentThread().id
