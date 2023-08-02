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
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Helper class for implementing [ContextAware]. Classes using this helper should
 * call [addOnContextAvailableListener] and [removeOnContextAvailableListener] as the respective
 * methods of [ContextAware] are called.
 *
 * You must call [dispatchOnContextAvailable] once the
 * [Context] is available to dispatch the callbacks to all registered listeners.
 *
 * Listeners added after the context has been made available via
 * [dispatchOnContextAvailable] will have the Context synchronously
 * delivered to them up until [clearAvailableContext] is called.
 */
class ContextAwareHelper {
    private val listeners: MutableSet<OnContextAvailableListener> = CopyOnWriteArraySet()

    @Volatile
    private var context: Context? = null

    /**
     * Get the [Context] if it is currently available. If this returns
     * `null`, you can use [addOnContextAvailableListener] to receive
     * a callback for when it available.
     *
     * @return the Context if it is currently available.
     */
    fun peekAvailableContext(): Context? {
        return context
    }

    /**
     * Add a new [OnContextAvailableListener] for receiving a callback for when
     * this class is associated with a [android.content.Context].
     *
     * @param listener The listener that should be added.
     * @see removeOnContextAvailableListener
     */
    fun addOnContextAvailableListener(listener: OnContextAvailableListener) {
        context?.let {
            listener.onContextAvailable(it)
        }
        listeners.add(listener)
    }

    /**
     * Remove a [OnContextAvailableListener] previously added via
     * [addOnContextAvailableListener].
     *
     * @param listener The listener that should be removed.
     * @see addOnContextAvailableListener
     */
    fun removeOnContextAvailableListener(listener: OnContextAvailableListener) {
        listeners.remove(listener)
    }

    /**
     * Dispatch the callback of [OnContextAvailableListener.onContextAvailable] to
     * all currently added listeners in the order they were added.
     *
     * @param context The [Context] the [ContextAware] object is now associated with.
     */
    fun dispatchOnContextAvailable(context: Context) {
        this.context = context
        for (listener in listeners) {
            listener.onContextAvailable(context)
        }
    }

    /**
     * Clear any [Context] previously made available via
     * [dispatchOnContextAvailable].
     */
    fun clearAvailableContext() {
        context = null
    }
}
