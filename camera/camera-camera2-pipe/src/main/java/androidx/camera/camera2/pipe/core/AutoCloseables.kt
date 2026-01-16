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

package androidx.camera.camera2.pipe.core

import androidx.camera.camera2.pipe.core.AutoCloseables.useEachIndexedAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.yield

/**
 * This contains a set of utility functions for safely iterating on sets of [AutoCloseable] objects.
 *
 * TODO: Once we allow API's to be exposed, mark these as internal + published API.
 */
public object AutoCloseables {
    /**
     * For each item in the list, call [action], then close it. All items in the list will be closed
     * even [action] throws an exception or if an individual close call throws an exception.
     */
    public inline fun <T : AutoCloseable> useEach(closeables: List<T>, action: (T) -> Unit) {
        useEachIndexed(closeables) { _, it -> action(it) }
    }

    /**
     * For each item in the list, call [action], then close it. All items in the list will be closed
     * even [action] throws an exception or if an individual close call throws an exception.
     */
    public inline fun <T : AutoCloseable> useEachIndexed(
        closeables: List<T>,
        action: (index: Int, T) -> Unit,
    ) {
        var exception: Throwable? = null
        var i = 0
        try {
            while (i < closeables.size) {
                closeables[i].use {
                    // NOTE: i is incremented after accessing the list and calling action so that if
                    // an exception is thrown the finally clause will still close the un-accessed
                    // resources.
                    action(i++, it)
                }
            }
        } catch (e: Throwable) {
            exception = e
            throw e
        } finally {
            while (i < closeables.size) {
                try {
                    closeables[i++].close()
                } catch (e: Throwable) {
                    // Suppress exceptions during close, and keep the original exception that
                    // caused the failure in the first place.
                    exception?.addSuppressed(e)
                }
            }
        }
    }

    /** @see [useEachIndexedAsync] */
    public suspend inline fun <T : AutoCloseable, R> useEachAsync(
        scope: CoroutineScope,
        closeables: List<T>,
        crossinline action: suspend CoroutineScope.(T) -> R,
    ): List<Deferred<R>> = useEachIndexedAsync(scope, closeables) { _, it -> action(it) }

    /**
     * For each closable in the list, launch and execute the provided [action] and then close the
     * resource. Each item in [closeables] is guaranteed to be closed, even if [scope] is canceled
     * or if [action] throws an exception. Ordering is preserved.
     */
    public inline fun <T : AutoCloseable, R> useEachIndexedAsync(
        scope: CoroutineScope,
        closeables: List<T>,
        crossinline action: suspend CoroutineScope.(index: Int, T) -> R,
    ): List<Deferred<R>> {
        val results = ArrayList<Deferred<R>>(closeables.size)
        for (i in closeables.indices) {
            val closeable = closeables[i]
            // UNDISPATCHED ensures the block synchronously begins execution.
            val deferred =
                scope.async(start = CoroutineStart.UNDISPATCHED) {
                    closeable.use {
                        // Yield ensures the action is dispatched onto the scope, even if the action
                        // does not internally suspend. Yield must be within the `use` block so that
                        // if the coroutine is canceled the element will still be closed.
                        yield()

                        // Invoke the action inside its own `coroutineScope`. This ensures all
                        // actions and sub-launch actions complete before the resource is closed.
                        coroutineScope { action(i, it) }
                    }
                }
            results.add(deferred)
        }
        return results
    }
}
