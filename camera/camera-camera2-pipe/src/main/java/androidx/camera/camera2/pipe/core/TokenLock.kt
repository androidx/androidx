/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")
@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.core

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.atomicfu.atomic
import java.io.Closeable
import java.util.ArrayDeque
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.io.use

/**
 * Provides fair access to a resources by acquiring and releasing variable sized [Token] objects.
 *
 * A [TokenLock] has a fixed maximum size that it will issue [Token] objects for. Additional
 * requests beyond the maximum capacity of the lock will wait until enough of the outstanding
 * tokens have been closed to fulfill the next request in the queue.
 *
 * This object behaves like a lock or mutex, which means that it's possible to deadlock if a
 * function or sequence attempts to acquire or hold multiple tokens. For this reason, it's
 * recommended to request tokens using a range of values in a single call if multiple tokens are
 * needed instead of incrementally acquiring tokens.
 *
 * Access the methods and properties of the [TokenLock] are ThreadSafe, and closing this object
 * multiple times has no effect.
 */
internal interface TokenLock : AutoCloseable, Closeable {
    val capacity: Long
    val available: Long
    val size: Long

    /**
     * Acquire a token or suspend until a token is available. Canceling the request will remove the
     * request from the queue, and closing the [TokenLock] will cause this method to throw a
     * CancellationException.
     *
     * @throws CancellationException if the TokenLock is closed while this function is suspended.
     * @throws IllegalArgumentException if min > capacity
     */
    suspend fun acquire(min: Long, max: Long): Token

    /**
     * Immediately acquire a token or return null if a token cannot be immediately acquired.
     *
     * @throws IllegalArgumentException if min > capacity
     */
    fun acquireOrNull(min: Long, max: Long): Token?

    /**
     * Tokens are Thread-safe objects that hold onto the acquired value. Closing this object returns
     * its value to the parent TokenLock.
     *
     * Closing this object multiple times has no effect.
     */
    interface Token : AutoCloseable, Closeable {
        val value: Long

        /**
         * Close this token and return true if this call successfully released the value to the
         * parent [TokenLock]. This method call is atomic, and can be used to guard shutdown calls
         * that must only be run once.
         */
        fun release(): Boolean
    }
}

/** Shorthand for "acquire(value, value)" */
internal suspend inline fun TokenLock.acquire(value: Long): TokenLock.Token =
    this.acquire(value, value)

/** Shorthand for "acquireOrNull(value, value)" */
internal inline fun TokenLock.acquireOrNull(value: Long): TokenLock.Token? = this.acquireOrNull(
    value,
    value
)

/**
 * Executes the given action while holding a token.
 */
internal suspend inline fun <T> TokenLock.withToken(
    value: Long,
    crossinline action: (token: TokenLock.Token) -> T
): T {
    this.acquire(value).use {
        return action(it)
    }
}

/**
 * Executes the given action while holding a token.
 */
internal suspend inline fun <T> TokenLock.withToken(
    min: Long,
    max: Long,
    crossinline action: (token: TokenLock.Token) -> T
): T {
    this.acquire(min, max).use {
        return action(it)
    }
}

internal class TokenLockImpl(override val capacity: Long) : TokenLock {
    companion object {
        val closedException = CancellationException()
    }

    private val pending = ArrayDeque<TokenRequest>()

    @GuardedBy("pending")
    private var closed = false

    @GuardedBy("pending")
    private var _available: Long = capacity

    override val available: Long
        get() = synchronized(pending) {
            return if (closed || pending.isNotEmpty()) {
                0
            } else {
                _available
            }
        }

    override val size: Long
        get() = synchronized(pending) {
            return if (closed || pending.isNotEmpty()) {
                capacity
            } else {
                capacity - _available
            }
        }

    override fun acquireOrNull(min: Long, max: Long): TokenLock.Token? {
        if (min > capacity)
            throw IllegalArgumentException("Attempted to acquire $min / $capacity")

        synchronized(pending) {
            if (closed) return null

            if (pending.isEmpty()) {
                val value = min(_available, max)
                if (value >= min) {
                    _available -= value
                    return TokenImpl(value)
                }
            }
        }
        return null
    }

    override suspend fun acquire(min: Long, max: Long): TokenLock.Token =
        suspendCancellableCoroutine { continuation ->
            if (min > capacity) {
                continuation.resumeWithException(
                    IllegalArgumentException("Attempted to acquire $min / $capacity")
                )
                return@suspendCancellableCoroutine
            }

            synchronized(pending) {
                if (closed) throw closedException
                if (pending.isEmpty()) {
                    val value = min(_available, max)
                    if (value >= min) {
                        _available -= value
                        continuation.resume(TokenImpl(value))
                        return@suspendCancellableCoroutine
                    }
                }
                pending.add(TokenRequest(continuation, min, max))
            }

            // WARNING: This may invoke the release method **synchronously** if the continuation
            //   was canceled while this method was executing.
            continuation.invokeOnCancellation { release(0) }
        }

    override fun close() {
        synchronized(pending) {
            if (closed) {
                return
            }
            closed = true
        }

        // Make sure all suspended functions that are waiting for a token are canceled, then clear
        // the list. This access is safe because all other interactions with the pending list occur
        // within a synchronized block that's guarded by a closed check.
        pending.forEach {
            it.continuation.cancel()
        }
        pending.clear()
    }

    /**
     * WARNING: This is an internal function to avoid creating synthetic accessors but it should
     *  ONLY be called by TokenImpl.close()
     */
    internal fun release(qty: Long) {
        var requestsToComplete: List<TokenRequest>? = null
        synchronized(pending) {
            if (closed) return

            _available += qty

            // Slower path: If we have pending requests, then we need figure out which ones we
            // should complete, in order, and to update the internal state.
            //
            // The CompletableDeferred is _ONLY_ completed outside of the synchronized block to
            // avoid reentrant behavior.
            if (!pending.isEmpty()) {
                val requests = mutableListOf<TokenRequest>()

                // Loop through the pending queue. If we can fulfil the pending request without
                // going over capacity, update the capacity and add the request to a list of
                // requests that must be completed.
                while (!pending.isEmpty()) {
                    // This will always be safe since we never insert non-null values and because
                    // the loop checks that the pending queue is not empty.
                    val next = pending.peek()!!
                    if (next.continuation.isCancelled || next.continuation.isCompleted) {
                        pending.remove()
                    } else {
                        val value = min(_available, next.max)
                        if (value >= next.min) {
                            _available -= value
                            next.token = TokenImpl(value)
                            requests.add(pending.remove())
                        } else {
                            break
                        }
                    }
                }

                // If we fulfilled 1 or more requests, then create and pass tokens to the
                // continuation outside of the syncronized block.
                if (requests.isNotEmpty()) {
                    requestsToComplete = requests
                }
            }
        }

        requestsToComplete?.forEach {
            it.continuation.resume(it.token!!)
        }
    }

    private class TokenRequest(
        val continuation: CancellableContinuation<TokenLock.Token>,
        val min: Long,
        val max: Long,
        var token: TokenImpl? = null
    )

    inner class TokenImpl(override val value: Long) : TokenLock.Token {
        private val closed = atomic(false)

        override fun close() {
            release()
        }

        override fun release(): Boolean {
            if (closed.compareAndSet(expect = false, update = true)) {
                release(value)
                return true
            }
            return false
        }
    }
}
