/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.runtime.internal.ThreadMap
import androidx.compose.runtime.internal.emptyThreadMap
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotContextElement
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias AtomicReference<V> = java.util.concurrent.atomic.AtomicReference<V>

internal actual class ThreadLocal<T> actual constructor(
    private val initialValue: () -> T
) : java.lang.ThreadLocal<T>() {
    @Suppress("UNCHECKED_CAST")
    actual override fun get(): T {
        return super.get() as T
    }

    actual override fun set(value: T) {
        super.set(value)
    }

    override fun initialValue(): T? {
        return initialValue.invoke()
    }

    actual override fun remove() {
        super.remove()
    }
}

internal actual class SnapshotThreadLocal<T> {
    private val map = AtomicReference<ThreadMap>(emptyThreadMap)
    private val writeMutex = Any()

    private var mainThreadValue: T? = null

    @Suppress("UNCHECKED_CAST")
    actual fun get(): T? {
        val threadId = Thread.currentThread().id
        return if (threadId == MainThreadId) {
            mainThreadValue
        } else {
            map.get().get(threadId) as T?
        }
    }

    actual fun set(value: T?) {
        val key = Thread.currentThread().id
        if (key == MainThreadId) {
            mainThreadValue = value
        } else {
            synchronized(writeMutex) {
                val current = map.get()
                if (current.trySet(key, value)) return
                map.set(current.newWith(key, value))
            }
        }
    }
}

internal expect val MainThreadId: Long

internal actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

@PublishedApi
internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    return kotlin.synchronized(lock, block)
}

internal actual typealias TestOnly = org.jetbrains.annotations.TestOnly

internal actual fun invokeComposable(composer: Composer, composable: @Composable () -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val realFn = composable as Function2<Composer, Int, Unit>
    realFn(composer, 1)
}

internal actual fun <T> invokeComposableForResult(
    composer: Composer,
    composable: @Composable () -> T
): T {
    @Suppress("UNCHECKED_CAST")
    val realFn = composable as Function2<Composer, Int, T>
    return realFn(composer, 1)
}

internal actual class AtomicInt actual constructor(value: Int) : AtomicInteger(value) {
    actual fun add(amount: Int): Int = addAndGet(amount)

    // These are implemented by Number, but Kotlin fails to resolve them
    override fun toByte(): Byte = toInt().toByte()
    override fun toShort(): Short = toInt().toShort()
    override fun toChar(): Char = toInt().toChar()
}

internal actual fun ensureMutable(it: Any) { /* NOTHING */ }

internal actual class WeakReference<T : Any> actual constructor(reference: T) :
    java.lang.ref.WeakReference<T>(reference)

internal actual fun currentThreadId(): Long = Thread.currentThread().id

internal actual fun currentThreadName(): String = Thread.currentThread().name

/**
 * Implementation of [SnapshotContextElement] that enters a single given snapshot when updating
 * the thread context of a resumed coroutine.
 */
@ExperimentalComposeApi
internal actual class SnapshotContextElementImpl actual constructor(
    private val snapshot: Snapshot
) : SnapshotContextElement, ThreadContextElement<Snapshot?> {
    override val key: CoroutineContext.Key<*>
        get() = SnapshotContextElement

    override fun updateThreadContext(context: CoroutineContext): Snapshot? =
        snapshot.unsafeEnter()

    override fun restoreThreadContext(context: CoroutineContext, oldState: Snapshot?) {
        snapshot.unsafeLeave(oldState)
    }
}
