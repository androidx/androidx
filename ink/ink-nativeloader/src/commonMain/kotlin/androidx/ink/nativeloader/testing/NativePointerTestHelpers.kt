/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:JvmName("NativePointerTestHelpers")

package androidx.ink.nativeloader.testing

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.nativeloader.NativePointerObserver
import kotlin.jvm.JvmName
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Runs the given block and waits for all native pointers allocated by
 * [androidx.ink.nativeloader.NativePointer] instances in the block to be cleaned up. Checks that
 * those pointers are all unique, that at least one is allocated if the block completes
 * successfully, and that all are cleaned up within the given timeout.
 *
 * This only observes pointer allocation and cleanup done through the `NativePointer` wrapper. So it
 * doesn't assert that all relevant native pointer allocation is done through that wrapper. What it
 * does check is that those wrappers are used somewhere in the code under test, and they're not
 * misconfigured in a way that inadvertently prevents the object from being GC'd (a common hazard
 * with [kotlin.native.ref.createCleaner]).
 *
 * @param timeoutMillis The maximum time to wait for all native pointers to be cleaned up.
 * @param onCleanup Callback called when a `NativePointer` cleanup is observed, called with the
 *   allocated pointer value or 0 if no pointer was allocated. This is mainly to observe that
 *   cleanup logic is set up before allocation is attempted. (That cannot be observed with the
 *   `pointerFree` callback passed as a parameter to the `NativePointer` constructor, since that is
 *   not called if allocation fails.)
 * @param block The block to run.
 * @throws IllegalStateException if the block completed successfully but no pointers were allocated,
 *   or if the same pointer was allocated twice. Also if the same pointer was cleaned up twice or if
 *   a pointer was cleaned up without being allocated, though `NativePointer` should ensure these
 *   conditions are not met.
 * @throws kotlinx.coroutines.TimeoutCancellationException if any pointer was not cleaned up within
 *   the timeout specified by [timeoutMillis].
 */
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun awaitNativePointerCleanupAfter(
    onCleanup: ((Long) -> Unit)? = null,
    timeoutMillis: Long = 1000L,
    block: () -> Unit,
) {
    val cleanupsMapMutex = Mutex()
    val cleanupsToAwait = mutableMapOf<Long, CompletableDeferred<Unit>>()
    observingNativePointers(
        onAlloc = { pointer ->
            runBlocking {
                cleanupsMapMutex.withLock {
                    check(!cleanupsToAwait.contains(pointer)) {
                        "The same pointer ($pointer) was allocated twice."
                    }
                    cleanupsToAwait[pointer] = CompletableDeferred()
                }
            }
        },
        onCleanup = { pointer ->
            onCleanup?.invoke(pointer)
            val cleanup = runBlocking { cleanupsMapMutex.withLock { cleanupsToAwait[pointer] } }
            check(cleanup != null) {
                "Attempted to clean up pointer ($pointer) which was not allocated."
            }
            check(cleanup.complete(Unit)) { "The same pointer ($pointer) was cleaned up twice." }
        },
    ) {
        try {
            block()
            check(!cleanupsToAwait.isEmpty()) { "No pointers were allocated." }
        } finally {
            GarbageCollectorController.collect()
            runBlocking { withTimeout(timeoutMillis) { cleanupsToAwait.values.awaitAll() } }
        }
    }
}

@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun awaitNativePointerCleanupSupported(): Boolean =
    GarbageCollectorController.canCollectSynchronously()

internal fun observingNativePointers(
    onAlloc: (Long) -> Unit,
    onCleanup: (Long) -> Unit,
    block: () -> Unit,
) {
    try {
        NativePointerObserver.onAlloc = onAlloc
        NativePointerObserver.onCleanup = onCleanup
        block()
    } finally {
        NativePointerObserver.onAlloc = null
        NativePointerObserver.onCleanup = null
    }
}
