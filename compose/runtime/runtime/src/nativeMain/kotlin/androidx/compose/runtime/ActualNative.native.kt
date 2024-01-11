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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode
import kotlin.system.getTimeNanos
import kotlin.time.ExperimentalTime
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.yield

private val threadCounter = atomic(0L)

@kotlin.native.concurrent.ThreadLocal
private var threadId: Long = threadCounter.addAndGet(1)

@OptIn(ExperimentalNativeApi::class)
internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    val kotlinNativeReference = kotlin.native.ref.WeakReference<T>(reference)
    actual fun get(): T? = kotlinNativeReference.get()
}

@OptIn(ExperimentalNativeApi::class)
@InternalComposeApi
actual fun identityHashCode(instance: Any?): Int =
    instance.identityHashCode()

actual annotation class TestOnly

actual typealias CompositionContextLocal = kotlin.native.concurrent.ThreadLocal

actual val DefaultMonotonicFrameClock: MonotonicFrameClock = MonotonicClockImpl()

@OptIn(ExperimentalTime::class)
private class MonotonicClockImpl : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(
        onFrame: (Long) -> R
    ): R {
        yield()
        return onFrame(getTimeNanos())
    }
}

internal actual object Trace {
    actual fun beginSection(name: String): Any? {
        return null
    }

    actual fun endSection(token: Any?) {
    }
}

actual annotation class CheckResult actual constructor(actual val suggest: String)

@ExperimentalComposeApi
internal actual class SnapshotContextElementImpl actual constructor(
    private val snapshot: Snapshot
) : SnapshotContextElement {

    init {
        error("provide SnapshotContextElementImpl when coroutines lib has necessary APIs")
    }

    override val key: CoroutineContext.Key<*>
        get() = SnapshotContextElement
}

internal actual fun logError(message: String, e: Throwable) {
    println(message)
    e.printStackTrace()
}

internal actual fun currentThreadId(): Long = threadId

internal actual fun currentThreadName(): String = "thread@$threadId"