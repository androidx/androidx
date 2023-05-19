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

package androidx.window.layout.adapter.extensions

import android.app.Activity
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.UiContext
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.window.core.ConsumerAdapter
import androidx.window.core.ExtensionsUtil
import androidx.window.extensions.WindowExtensions
import androidx.window.extensions.core.util.function.Consumer as OEMConsumer
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.adapter.WindowBackend
import androidx.window.layout.adapter.extensions.ExtensionsWindowLayoutInfoAdapter.translate
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A wrapper around [WindowLayoutComponent] that ensures
 * [WindowLayoutComponent.addWindowLayoutInfoListener] is called at most once per context while
 * there are active listeners. Context has to be an [Activity] or a [UiContext] created with
 * [Context#createWindowContext] or InputMethodService.
 */
internal class ExtensionWindowLayoutInfoBackend(
    private val component: WindowLayoutComponentWrapper,
    private val consumerAdapter: ConsumerAdapter
) : WindowBackend {

    private val extensionWindowBackendLock = ReentrantLock()
    @GuardedBy("lock")
    private val contextToListeners = mutableMapOf<Context, MulticastConsumer>()

    @GuardedBy("lock")
    private val listenerToContext = mutableMapOf<Consumer<WindowLayoutInfo>, Context>()

    @GuardedBy("lock")
    private val consumerToToken = mutableMapOf<MulticastConsumer, ConsumerAdapter.Subscription>()

    /**
     * The mapping from [MulticastConsumer] to Extensions Core version [Consumer]. This is used
     * to translate [MulticastConsumer] to Extensions APIs after
     * [WindowExtensions.VENDOR_API_LEVEL_2].
     *
     * @see WindowLayoutComponent.addWindowLayoutInfoListener
     * @see WindowLayoutComponent.removeWindowLayoutInfoListener
     */
    @GuardedBy("lock")
    private val consumerToOemConsumer =
        mutableMapOf<MulticastConsumer, OEMConsumer<OEMWindowLayoutInfo>>()

    /**
     * Registers a listener to consume new values of [WindowLayoutInfo]. If there was a listener
     * registered for a given [Context] then the new listener will receive a replay of the last
     * known value.
     * @param context the host of a [android.view.Window] or an area on the screen. Has to be an
     * [Activity] or a [UiContext] created with [Context#createWindowContext] or InputMethodService.
     * @param executor an executor from the parent interface
     * @param callback the listener that will receive new values
     */
    @OptIn(androidx.window.core.ExperimentalWindowApi::class)
    override fun registerLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        extensionWindowBackendLock.withLock {
            contextToListeners[context]?.let { listener ->
                listener.addListener(callback)
                listenerToContext[callback] = context
            } ?: run {
                val consumer = MulticastConsumer(context)
                contextToListeners[context] = consumer
                listenerToContext[callback] = context
                consumer.addListener(callback)
                if (ExtensionsUtil.safeVendorApiLevel < WindowExtensions.VENDOR_API_LEVEL_2) {
                    val consumeWindowLayoutInfo: (OEMWindowLayoutInfo) -> Unit = { value ->
                        consumer.accept(value)
                    }
                    // The registrations above maintain 1-many mapping of Context-Listeners across
                    // different subscription implementations.
                    val disposableToken = if (context is Activity) {
                        consumerAdapter.createSubscription(
                            component,
                            OEMWindowLayoutInfo::class,
                            "addWindowLayoutInfoListener",
                            "removeWindowLayoutInfoListener",
                            context,
                            consumeWindowLayoutInfo
                        )
                    } else {
                        // Prior to WM Extensions v2 addWindowLayoutInfoListener only
                        // supports Activities. Return empty WindowLayoutInfo if the
                        // provided Context is not an Activity.
                        consumer.accept(OEMWindowLayoutInfo(emptyList()))
                        return@registerLayoutChangeCallback
                    }
                    consumerToToken[consumer] = disposableToken
                } else {
                    val oemConsumer = OEMConsumer<OEMWindowLayoutInfo> { info ->
                        consumer.accept(info)
                    }
                    consumerToOemConsumer[consumer] = oemConsumer
                    component.addWindowLayoutInfoListener(context,
                        oemConsumer)
                }
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
        extensionWindowBackendLock.withLock {
            val context = listenerToContext[callback] ?: return
            val multicastListener = contextToListeners[context] ?: return
            multicastListener.removeListener(callback)
            listenerToContext.remove(callback)
            if (multicastListener.isEmpty()) {
                contextToListeners.remove(context)
                if (ExtensionsUtil.safeVendorApiLevel < WindowExtensions.VENDOR_API_LEVEL_2) {
                    consumerToToken.remove(multicastListener)?.dispose()
                } else {
                    val oemConsumer = consumerToOemConsumer.remove(multicastListener)
                    if (oemConsumer != null) {
                        component.removeWindowLayoutInfoListener(oemConsumer)
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if all the collections are empty, {@code false} otherwise
     */
    @VisibleForTesting
    fun hasRegisteredListeners(): Boolean {
        return !(contextToListeners.isEmpty() && listenerToContext.isEmpty() &&
            consumerToToken.isEmpty())
    }

    /**
     * A class that implements [Consumer] by aggregating multiple instances of [Consumer]
     * and multicasting each value that is consumed. [MulticastConsumer] also replays the last known
     * value whenever a new consumer registers.
     */
    private class MulticastConsumer(
        private val context: Context
    ) : Consumer<OEMWindowLayoutInfo> {
        private val multicastConsumerLock = ReentrantLock()
        @GuardedBy("lock")
        private var lastKnownValue: WindowLayoutInfo? = null
        @GuardedBy("lock")
        private val registeredListeners = mutableSetOf<Consumer<WindowLayoutInfo>>()

        override fun accept(value: OEMWindowLayoutInfo) {
            multicastConsumerLock.withLock {
                lastKnownValue = translate(context, value)
                registeredListeners.forEach { consumer -> consumer.accept(lastKnownValue) }
            }
        }

        fun addListener(listener: Consumer<WindowLayoutInfo>) {
            multicastConsumerLock.withLock {
                lastKnownValue?.let { value -> listener.accept(value) }
                registeredListeners.add(listener)
            }
        }

        fun removeListener(listener: Consumer<WindowLayoutInfo>) {
            multicastConsumerLock.withLock {
                registeredListeners.remove(listener)
            }
        }

        fun isEmpty(): Boolean {
            return registeredListeners.isEmpty()
        }
    }
}