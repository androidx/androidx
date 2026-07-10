/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore

import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Helper class to fire events and manage listener:executor pairs in a thread-safe manner.
 *
 * @param fireFunc a function implementation that invokes the given ListenerType with the given
 *   EventType object as an argument. ListenerMap will handle firing it on the appropriate executor
 *   and ensuring the underlying set of listener:executor pairs are not mutated while iterating over
 *   the set of listeners.
 */
internal open class ListenerMap<ListenerType, EventType>(
    private val fireFunc: (ListenerType, EventType) -> Unit
) {

    // synchronizedMap automatically handles synchronization for add/remove/clear.
    private val map: MutableMap<ListenerType, Executor> =
        Collections.synchronizedMap(HashMap<ListenerType, Executor>())

    val isEmpty: Boolean
        get() = map.isEmpty()

    val size: Int
        get() = map.size

    fun add(executor: Executor, listener: ListenerType) {
        map[listener] = executor
    }

    fun remove(listener: ListenerType) {
        map.remove(listener)
    }

    /** Removes all listeners from this ListenerMap. */
    fun clear() {
        map.clear()
    }

    /** Iterates over all ListenerTypes in this ListenerMap, invoking fireFunc on each. */
    fun fire(event: EventType) {
        // synchronizedMap does not automatically synchronize iteration, so make a defensive copy
        // to iterate. This is preferable to synchronizing this entire method, to avoid blocking
        // add/remove/clear on any long-running fireFunc calls on a direct executor.
        val mapCopy = map.toMap()
        mapCopy.forEach { (listener, executor) -> executor.execute { fireFunc(listener, event) } }
    }
}

/** ListenerMap for listeners that are of type Consumer<EventType>. */
internal class ConsumerListenerMap<EventType> :
    ListenerMap<Consumer<EventType>, EventType>({ consumer, event -> consumer.accept(event) })

/** ListenerMap for listeners that are of type Runnable. */
internal class RunnableListenerMap : ListenerMap<Runnable, Unit>({ runnable, _ -> runnable.run() })

/**
 * A [Runnable] implementation that holds a [WeakReference] to a target object and executes a block
 * only if the target is still alive.
 */
internal class WeakRunnable<T>(target: T, private val block: (T) -> Unit) : Runnable {
    private val weakTarget = WeakReference(target)

    override fun run() {
        weakTarget.get()?.let { block(it) }
    }
}

/**
 * A generic functional interface implementation that holds a [WeakReference] to a target object and
 * executes a block only if the target is still alive. This is useful for implementing SAM
 * interfaces with weak references.
 */
internal class WeakListener<T, L>(target: T, private val block: (T, L) -> Unit) {
    private val weakTarget = WeakReference(target)

    fun invoke(value: L) {
        weakTarget.get()?.let { block(it, value) }
    }
}
