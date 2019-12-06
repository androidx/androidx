/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.compose.ObserverMap
import androidx.compose.TestOnly
import androidx.compose.ThreadLocal
import androidx.compose.frames.FrameCommitObserver
import androidx.compose.frames.FrameReadObserver
import androidx.compose.frames.registerCommitObserver
import androidx.compose.frames.temporaryReadObserver

/**
 * Allows for easy model read observation. To begin observe a change, you must pass a
 * non-lambda `onCommit` listener to the [observeReads] method:
 *
 * @sample androidx.ui.core.samples.modelObserverExample
 *
 * When a `@Model` class change has been committed, the `onCommit` listener will be called
 * with the `targetObject` as the argument. There are no order guarantees for
 * `onCommit` listener calls.
 *
 * All `onCommit` calls will be made on the thread that the ModelObserver was created on, if
 * it is a Looper thread or on the main thread if not.
 */
class ModelObserver() {
    private val commitObserver: FrameCommitObserver = { committed ->
        val calls = mutableMapOf<OnCommitCaller<*>, List<Any>>()
        synchronized(observerMaps) {
            observerMaps.entries.forEach { (onCommit, map) ->
                val list = map.get(committed)
                if (list.isNotEmpty()) {
                    calls.put(onCommitCalls[onCommit]!!, list)
                }
            }
        }

        if (calls.isNotEmpty()) {
            if (Looper.myLooper() === handler.looper) {
                callOnCommit(calls)
            } else {
                handler.post { callOnCommit(calls) }
            }
        }
    }

    // map from onCommit to ObserverMap (key = model, value = target)
    private val observerMaps = mutableMapOf<Any, ObserverMap<Any, Any>>()

    // method to call when unsubscribing from the commit observer
    private var commitUnsubscribe: (() -> Unit)? = null

    // The FrameReadObserver currently being used to observe
    private val currentReadObserver = ThreadLocal<FrameReadObserver>()

    // A trick to be able to call the onCommit() without knowing the target type,
    // this is a map from the onCommit to a class designed to call that method.
    private val onCommitCalls = mutableMapOf<Any, OnCommitCaller<*>>()

    // The handler on the thread that this ModelObserver was created on.
    private val handler: Handler

    init {
        if (Looper.myLooper() == null) {
            handler = Handler(Looper.getMainLooper())
        } else {
            handler = Handler()
        }
    }

    /**
     * Test-only access to the internal commit listener. This is used for benchmarking
     * the commit notification callback.
     *
     * @hide
     */
    val frameCommitObserver: FrameCommitObserver
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @TestOnly
        get() = commitObserver

    /**
     * Executes [block], observing model reads during its execution.
     * The [target] is stored as a weak reference to be passed to [onCommit] when a change to the
     * model has been detected.
     *
     * Observation for [target] will be paused when a new [observeReads] call is made or when
     * [pauseObservingReads] is called.
     *
     * Any previous observation with the given [target] and [onCommit] will be
     * cleared and only the new observation on [block] will be stored. It is important that
     * the same instance of [onCommit] is used between calls or previous references will
     * not be cleared.
     *
     * The [onCommit] will be called when a model that was accessed during [block] has been
     * committed, and it will be run on  the thread that the [ModelObserver] was created on.
     * If the ModelObserver was created on a non-[Looper] thread, [onCommit] will be called on
     * the [main thread][Looper.getMainLooper] instead.
     */
    fun <T : Any> observeReads(target: T, onCommit: (T) -> Unit, block: () -> Unit) {
        val map: ObserverMap<Any, Any>
        synchronized(observerMaps) {
            map = observerMaps.getOrPut(onCommit) { ObserverMap() }
            onCommitCalls.getOrPut(onCommit) { OnCommitCaller(onCommit) }
            // clear all current observations for the target
            map.removeValue(target)
        }
        observeWithObserver(ReadObserver(observerMaps, target, map), block)
    }

    /**
     * Stops observing model reads while executing [block]. Model reads may be restarted
     * by calling [observeReads] inside [block].
     */
    fun pauseObservingReads(block: () -> Unit) {
        observeWithObserver(PausedReadObserver, block)
    }

    private fun observeWithObserver(newObserver: FrameReadObserver, block: () -> Unit) {
        val oldObserver = currentReadObserver.get()
        currentReadObserver.set(newObserver)
        temporaryReadObserver(newObserver, oldObserver, block)
        currentReadObserver.set(oldObserver)
    }

    /**
     * Clears all model read observations for a given [target]. This clears values for all
     * `onCommit` methods passed in [observeReads].
     */
    fun clear(target: Any) {
        synchronized(observerMaps) {
            observerMaps.values.forEach { map ->
                map.removeValue(target)
            }
        }
    }

    /**
     * Starts or stops watching for model commits based on [enabled].
     */
    fun enableModelUpdatesObserving(enabled: Boolean) {
        require(enabled == (commitUnsubscribe == null)) {
            "Called twice with the same enabled value: $enabled"
        }
        if (enabled) {
            commitUnsubscribe = registerCommitObserver(commitObserver)
        } else {
            commitUnsubscribe?.invoke()
            commitUnsubscribe = null
        }
    }

    private fun callOnCommit(calls: Map<OnCommitCaller<*>, List<Any>>) {
        calls.entries.forEach { (caller, targets) ->
            caller.callOnCommit(targets)
        }
    }

    /**
     * A [FrameReadObserver] that adds the read value to the observer map. This is used when
     * [observeReads] is called.
     */
    private class ReadObserver<T : Any>(
        val lock: Any,
        val target: T,
        val map: ObserverMap<Any, T>
    ) : FrameReadObserver {
        override fun invoke(readValue: Any) {
            synchronized(lock) {
                map.add(readValue, target)
            }
        }
    }

    internal companion object {
        /**
         * The [FrameReadObserver] used [pauseObservingReads] is called.
         */
        private val PausedReadObserver: FrameReadObserver = { _ -> }
    }

    /**
     * Used to tie an `onCommit` to its target by type. This works around some difficulties in
     * unchecked casts with kotlin.
     */
    @Suppress("UNCHECKED_CAST")
    private class OnCommitCaller<T>(val onCommit: (T) -> Unit) {
        fun callOnCommit(targets: Iterable<Any>) {
            targets.forEach { target ->
                onCommit(target as T)
            }
        }
    }
}