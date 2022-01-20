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

package androidx.window.layout

import android.app.Activity
import androidx.annotation.GuardedBy
import androidx.core.util.Consumer
import androidx.window.core.ConsumerAdapter
import androidx.window.extensions.layout.WindowLayoutComponent
import androidx.window.layout.ExtensionsWindowLayoutInfoAdapter.translate
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import androidx.window.extensions.layout.WindowLayoutInfo as OEMWindowLayoutInfo

/**
 * A wrapper around [WindowLayoutComponent] that ensures
 * [WindowLayoutComponent.addWindowLayoutInfoListener] is called at most once per activity while
 * there are active listeners.
 */
internal class ExtensionWindowLayoutInfoBackend(
    private val component: WindowLayoutComponent,
    private val consumerAdapter: ConsumerAdapter
) : WindowBackend {

    private val extensionWindowBackendLock = ReentrantLock()
    @GuardedBy("lock")
    private val activityToListeners = mutableMapOf<Activity, MulticastConsumer>()
    @GuardedBy("lock")
    private val listenerToActivity = mutableMapOf<Consumer<WindowLayoutInfo>, Activity>()
    @GuardedBy("lock")
    private val consumerToToken = mutableMapOf<MulticastConsumer, ConsumerAdapter.Subscription>()

    /**
     * Registers a listener to consume new values of [WindowLayoutInfo]. If there was a listener
     * registered for a given [Activity] then the new listener will receive a replay of the last
     * known value.
     * @param activity the host of a [android.view.Window]
     * @param executor an executor from the parent interface
     * @param callback the listener that will receive new values
     */
    override fun registerLayoutChangeCallback(
        activity: Activity,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    ) {
        extensionWindowBackendLock.withLock {
            activityToListeners[activity]?.let { listener ->
                listener.addListener(callback)
                listenerToActivity[callback] = activity
            } ?: run {
                val consumer = MulticastConsumer(activity)
                activityToListeners[activity] = consumer
                listenerToActivity[callback] = activity
                consumer.addListener(callback)
                val disposableToken = consumerAdapter.createSubscription(
                    component,
                    OEMWindowLayoutInfo::class,
                    "addWindowLayoutInfoListener",
                    "removeWindowLayoutInfoListener",
                    activity
                ) { value ->
                    consumer.accept(value)
                }
                consumerToToken[consumer] = disposableToken
            }
        }
    }

    /**
     * Unregisters a listener, if this is the last listener for an [Activity] then the listener is
     * removed from the [WindowLayoutComponent]. Calling with the same listener multiple times in a
     * row does not have an effect. @param callback a listener that may have been registered
     */
    override fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>) {
        extensionWindowBackendLock.withLock {
            val activity = listenerToActivity[callback] ?: return
            val multicastListener = activityToListeners[activity] ?: return
            multicastListener.removeListener(callback)
            if (multicastListener.isEmpty()) {
                consumerToToken.remove(multicastListener)?.dispose()
            }
        }
    }

    /**
     * A class that implements [Consumer] by aggregating multiple instances of [Consumer]
     * and multicasting each value that is consumed. [MulticastConsumer] also replays the last known
     * value whenever a new consumer registers.
     */
    private class MulticastConsumer(
        private val activity: Activity
    ) : Consumer<OEMWindowLayoutInfo> {
        private val multicastConsumerLock = ReentrantLock()
        @GuardedBy("lock")
        private var lastKnownValue: WindowLayoutInfo? = null
        @GuardedBy("lock")
        private val registeredListeners = mutableSetOf<Consumer<WindowLayoutInfo>>()

        override fun accept(value: OEMWindowLayoutInfo) {
            multicastConsumerLock.withLock {
                lastKnownValue = translate(activity, value)
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