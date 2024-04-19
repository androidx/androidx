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

package androidx.window.layout.adapter.extensions

import android.app.Activity
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.UiContext
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.WindowBackend
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ExtensionWindowBackendApi2(
    private val component: WindowLayoutComponent
) : WindowBackend {

    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock")
    private val contextToListeners = mutableMapOf<Context, MulticastConsumer>()

    @GuardedBy("globalLock")
    private val listenerToContext = mutableMapOf<Consumer<WindowLayoutInfo>, Context>()

    /**
     * Registers a listener to consume new values of [WindowLayoutInfo]. If there was a listener
     * registered for a given [Context] then the new listener will receive a replay of the last
     * known value.
     * @param context the host of a [android.view.Window] or an area on the screen. Has to be an
     * [Activity] or a [UiContext] created with [Context#createWindowContext] or InputMethodService.
     * @param executor an executor from the parent interface
     * @param callback the listener that will receive new values
     */
    // TODO(b/289377381) remove duplicate code.
    override fun registerLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        globalLock.withLock {
            contextToListeners[context]?.let { listener ->
                listener.addListener(callback)
                listenerToContext[callback] = context
            } ?: run {
                val consumer = MulticastConsumer(context)
                contextToListeners[context] = consumer
                listenerToContext[callback] = context
                consumer.addListener(callback)

                component.addWindowLayoutInfoListener(context, consumer)
            }
        }
    }

    /**
     * Unregisters a listener, if this is the last listener for a [UiContext] then the listener is
     * removed from the [WindowLayoutComponent]. Calling with the same listener multiple times in a
     * row does not have an effect.
     * @param callback a listener that may have been registered
     */
    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        globalLock.withLock {
            val context = listenerToContext[callback] ?: return
            val multicastListener = contextToListeners[context] ?: return
            multicastListener.removeListener(callback)
            listenerToContext.remove(callback)
            if (multicastListener.isEmpty()) {
                contextToListeners.remove(context)
                component.removeWindowLayoutInfoListener(multicastListener)
            }
        }
    }

    /**
     * Returns {@code true} if all the collections are empty, {@code false} otherwise
     */
    @VisibleForTesting
    override fun hasRegisteredListeners(): Boolean {
        return !(contextToListeners.isEmpty() && listenerToContext.isEmpty())
    }
}
