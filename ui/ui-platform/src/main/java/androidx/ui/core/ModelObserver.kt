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
        // This array is in the same order as commitMaps
        val targetsArray: Array<List<Any>>
        var hasValues = false
        synchronized(commitMaps) {
            targetsArray = Array(commitMaps.size) { index ->
                val commitMap = commitMaps[index]
                val map = commitMap.map
                val targets = map.get(committed)
                if (targets.isNotEmpty()) {
                    hasValues = true
                }
                targets
            }
        }
        if (hasValues) {
            if (Looper.myLooper() === handler.looper) {
                callOnCommit(targetsArray)
            } else {
                handler.post { callOnCommit(targetsArray) }
            }
        }
    }

    // list of CommitMaps
    private val commitMaps = mutableListOf<CommitMap<*>>()

    // method to call when unsubscribing from the commit observer
    private var commitUnsubscribe: (() -> Unit)? = null

    // The FrameReadObserver currently being used to observe
    private val currentReadObserver = ThreadLocal<FrameReadObserver>()

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
        synchronized(commitMaps) {
            map = ensureMap(onCommit)
            // clear all current observations for the target
            map.removeValue(target)
        }
        observeWithObserver(ReadObserver(commitMaps, target, map), block)
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
        synchronized(commitMaps) {
            commitMaps.forEach { commitMap ->
                commitMap.map.removeValue(target)
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

    /**
     * Calls the `onCommit` callback for the given targets.
     */
    private fun callOnCommit(targetsArray: Array<List<Any>>) {
        for (i in 0..targetsArray.lastIndex) {
            val targets = targetsArray[i]
            if (targets.isNotEmpty()) {
                val commitCaller = synchronized(commitMaps) { commitMaps[i] }
                commitCaller.callOnCommit(targets)
            }
        }
    }

    /**
     * Returns the [ObserverMap] within [commitMaps] associated with [onCommit] or a newly-
     * inserted one if it doesn't exist.
     */
    private fun <T : Any> ensureMap(onCommit: (T) -> Unit): ObserverMap<Any, Any> {
        val index = commitMaps.indexOfFirst { it.onCommit === onCommit }
        if (index == -1) {
            val commitMap = CommitMap(onCommit)
            commitMaps.add(commitMap)
            return commitMap.map
        }
        return commitMaps[index].map
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
    private class CommitMap<T : Any>(val onCommit: (T) -> Unit) {
        /**
         * ObserverMap (key = model, value = target). These are the models that have been
         * read during the target's [ModelObserver.observeReads].
         */
        val map = ObserverMap<Any, Any>()

        /**
         * Calls the `onCommit` callback for targets affected by the given committed values.
         */
        fun callOnCommit(targets: List<Any>) {
            targets.forEach { target ->
                onCommit(target as T)
            }
        }
    }
}
