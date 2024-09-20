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

package androidx.compose.runtime

import androidx.compose.runtime.internal.PlatformOptimizedCancellationException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

private class CancelledCoroutineContext : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<CancelledCoroutineContext>
}

private class ForgottenCoroutineScopeException :
    PlatformOptimizedCancellationException("rememberCoroutineScope left the composition")

internal actual class RememberedCoroutineScope
actual constructor(
    private val parentContext: CoroutineContext,
    private val overlayContext: CoroutineContext,
) : CoroutineScope, RememberObserver {
    // The goal of this implementation is to make cancellation as cheap as possible if the
    // coroutineContext property was never accessed, consisting only of taking a monitor lock and
    // setting a volatile field.

    @Volatile private var _coroutineContext: CoroutineContext? = null

    override val coroutineContext: CoroutineContext
        get() {
            var localCoroutineContext = _coroutineContext
            if (
                localCoroutineContext == null || localCoroutineContext === CancelledCoroutineContext
            ) {
                // Yes, we're leaking our lock here by using the instance of the object
                // that also gets handled by user code as a CoroutineScope as an intentional
                // tradeoff for avoiding the allocation of a dedicated lock object.
                // Since we only use it here for this lazy initialization and control flow
                // does not escape the creation of the CoroutineContext while holding the lock,
                // the splash damage should be acceptable.
                kotlin.synchronized(this) {
                    localCoroutineContext = _coroutineContext
                    if (localCoroutineContext == null) {
                        val parentContext = parentContext
                        val childJob = Job(parentContext[Job])
                        localCoroutineContext = parentContext + childJob + overlayContext
                    } else if (localCoroutineContext === CancelledCoroutineContext) {
                        // Lazily initialize the child job here, already cancelled.
                        // Assemble the CoroutineContext exactly as otherwise expected.
                        val parentContext = parentContext
                        val cancelledChildJob =
                            Job(parentContext[Job]).apply {
                                cancel(ForgottenCoroutineScopeException())
                            }
                        localCoroutineContext = parentContext + cancelledChildJob + overlayContext
                    }
                    _coroutineContext = localCoroutineContext
                }
            }
            return localCoroutineContext!!
        }

    actual fun cancelIfCreated() {
        // Take the lock unconditionally; this is internal API only used by internal
        // RememberObserver implementations that are not leaked to user code; we can assume
        // this won't be called repeatedly. If this assumption is violated we'll simply create a
        // redundant exception.
        kotlin.synchronized(this) {
            val context = _coroutineContext
            if (context == null) {
                _coroutineContext = CancelledCoroutineContext
            } else {
                // Ignore optimizing the case where we might be cancelling an already cancelled job;
                // only internal callers such as RememberObservers will invoke this method.
                context.cancel(ForgottenCoroutineScopeException())
            }
        }
    }

    override fun onRemembered() {
        // Do nothing
    }

    override fun onForgotten() {
        cancelIfCreated()
    }

    override fun onAbandoned() {
        cancelIfCreated()
    }

    companion object {
        @JvmField val CancelledCoroutineContext: CoroutineContext = CancelledCoroutineContext()
    }
}
