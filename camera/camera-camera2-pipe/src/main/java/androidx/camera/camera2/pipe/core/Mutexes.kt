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

package androidx.camera.camera2.pipe.core

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * [CoroutineMutex] is a shared [Mutex] instance with extension functions to allow callers to lock
 * the mutex from a non-coroutine function, while ensuring that the locked execution block is always
 * suspended (Which avoids synchronously running code that should be executed on a different
 * thread). This guaranteed suspension, plus the additional extension functions to make it easier to
 * correctly lock and run suspending code from a non-suspending function, is the primary difference
 * from using a [Mutex] object directly.
 *
 * This should not be used to run long running operations, since the lock will be held for the
 * entire duration. In addition, kotlin [Mutex] objects are non-reentrant, unlike standard java
 * `synchronized` locks.
 */
public class CoroutineMutex {
    internal val mutex = Mutex()
}

/**
 * Execute the provided [block] within the current [CoroutineMutex] while ensuring that only one
 * operation is executed at a time.
 */
public fun <T> CoroutineMutex.withLockAsync(
    scope: CoroutineScope,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    // Using CoroutineStart.UNDISPATCHED ensures that the mutex.lock call is invoked synchronously
    // on the current thread. This prevents two operations from being mis-ordered since the lock
    // is acquired before the block is scheduled for execution.
    return scope.async(start = CoroutineStart.UNDISPATCHED) {
        ensureActive()

        // The block is called within a new CoroutineScope, while holding the lock. This ensures
        // that any child coroutines started via `block` are completed before `lock` gets released
        // and the next block starts, which includes other async/launch calls invoked on the
        // scope.
        mutex.withLockSuspend { coroutineScope(block) }
    }
}

/**
 * Execute the provided [block] after acquiring a lock to the [CoroutineMutex] in the provided
 * [CoroutineScope].
 */
public fun CoroutineMutex.withLockLaunch(
    scope: CoroutineScope,
    block: suspend CoroutineScope.() -> Unit
): Job {
    // Using CoroutineStart.UNDISPATCHED ensures that the mutex.lock call is invoked synchronously
    // on the current thread. This prevents two operations from being mis-ordered since the lock
    // is acquired before the block is scheduled for execution.
    return scope.launch(start = CoroutineStart.UNDISPATCHED) {
        ensureActive()

        // The block is called within a new CoroutineScope, while holding the lock. This ensures
        // that any child coroutines started via `block` are completed before `lock` gets released
        // and the next block starts.
        mutex.withLockSuspend { coroutineScope(block) }
    }
}

/**
 * Acquire a lock on the provided mutex, suspending if the lock could not be immediately acquired.
 */
internal suspend inline fun Mutex.acquireToken(): Token {
    lock()
    return MutexToken(this)
}

/**
 * Acquire a lock on the provided mutex and suspend. This can be used with coroutines that are are
 * started as `UNDISPATCHED` to ensure they are dispatched onto the correct context.
 */
internal suspend inline fun Mutex.acquireTokenAndSuspend(): Token {
    lockAndSuspend()
    return MutexToken(this)
}

/**
 * Acquire a lock on the provided mutex without suspending. Returns null if the lock could not be
 * immediately acquired.
 */
internal fun Mutex.tryAcquireToken(): Token? {
    if (tryLock()) {
        return MutexToken(this)
    }
    return null
}

internal class MutexToken(private val mutex: Mutex) : Token {
    private val _released = atomic(false)
    override val released: Boolean
        get() = _released.value

    override fun release(): Boolean {
        if (_released.compareAndSet(expect = false, update = true)) {
            mutex.unlock()
            return true
        }
        return false
    }
}

/**
 * This function allows the implementation of [Mutex.lockAndSuspend] to call the overload of
 * [startCoroutineUninterceptedOrReturn] that takes a receiver, which saves an allocation and allows
 * the compiler to skip the coroutine state machine logic in [Mutex.lockAndSuspend].
 */
private suspend fun Mutex.lockWithoutOwner() = lock(owner = null)

/**
 * Same as [kotlinx.coroutines.sync.withLock], but guarantees that the coroutine is suspended before
 * the lock is acquired whether or not the lock is locked at the time this function is called.
 */
private suspend inline fun <T> Mutex.withLockSuspend(action: () -> T): T {
    lockAndSuspend()
    try {
        return action()
    } finally {
        unlock()
    }
}

/**
 * Same as [Mutex.lock], but guarantees that the coroutine is *always* suspended before the lock is
 * acquired, regardless of if the lock is locked at the time this function is called. This ensures
 * consistent behavior when calling lock from within a [CoroutineStart.UNDISPATCHED] coroutine.
 */
private suspend fun Mutex.lockAndSuspend() {
    val lockFn = Mutex::lockWithoutOwner
    return suspendCoroutineUninterceptedOrReturn { continuation ->
        if (
            lockFn.startCoroutineUninterceptedOrReturn(this, continuation) !== COROUTINE_SUSPENDED
        ) {
            // If the mutex.lock call did *not* suspend (likely because the lock was acquired
            // immediately), intercept the continuation block, which will schedule it for execution.
            continuation.intercepted().resume(Unit)
        }

        // The result is always that the continuation is always suspending.
        COROUTINE_SUSPENDED
    }
}
