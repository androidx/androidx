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
package androidx.activity.contextaware

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A `ContextAware` class is associated with a [Context] sometime after
 * the class is instantiated. By adding a [OnContextAvailableListener], you can
 * receive a callback for that event.
 *
 * Classes implementing [ContextAware] are strongly recommended to also implement
 * [androidx.lifecycle.LifecycleOwner] for providing a more general purpose API for
 * listening for creation and destruction events.
 *
 * @see ContextAwareHelper
 */
interface ContextAware {
    /**
     * Get the [Context] if it is currently available. If this returns
     * `null`, you can use [addOnContextAvailableListener] to receive
     * a callback for when it available.
     *
     * @return the Context if it is currently available.
     */
    fun peekAvailableContext(): Context?

    /**
     * Add a new [OnContextAvailableListener] for receiving a callback for when
     * this class is associated with a [android.content.Context].
     *
     * Listeners are triggered in the order they are added when added before the Context is
     * available. Listeners added after the context has been made available will have the Context
     * synchronously delivered to them as part of this call.
     *
     * @param listener The listener that should be added.
     * @see removeOnContextAvailableListener
     */
    fun addOnContextAvailableListener(listener: OnContextAvailableListener)

    /**
     * Remove a [OnContextAvailableListener] previously added via
     * [addOnContextAvailableListener].
     *
     * @param listener The listener that should be removed.
     * @see addOnContextAvailableListener
     */
    fun removeOnContextAvailableListener(listener: OnContextAvailableListener)
}

/**
 * Run [onContextAvailable] when the [Context] becomes available and
 * resume with the result.
 *
 * If the [Context] is already available, [onContextAvailable] will be
 * synchronously called on the current coroutine context. Otherwise,
 * [onContextAvailable] will be called on the UI thread immediately when
 * the Context becomes available.
 */
suspend inline fun <R> ContextAware.withContextAvailable(
    crossinline onContextAvailable: (@JvmSuppressWildcards Context) -> @JvmSuppressWildcards R
): @JvmSuppressWildcards R {
    val availableContext = peekAvailableContext()
    return if (availableContext != null) {
        onContextAvailable(availableContext)
    } else {
        suspendCancellableCoroutine { co ->
            val listener = object : OnContextAvailableListener {
                override fun onContextAvailable(context: Context) {
                    co.resumeWith(runCatching { onContextAvailable(context) })
                }
            }
            addOnContextAvailableListener(listener)
            co.invokeOnCancellation {
                removeOnContextAvailableListener(listener)
            }
        }
    }
}
