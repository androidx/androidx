/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.Logger
import androidx.work.impl.constraints.ConstraintListener
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/**
 * A base for tracking constraints and notifying listeners of changes.
 *
 * @param T the constraint data type observed by this tracker
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ConstraintTracker<T>
protected constructor(context: Context, private val taskExecutor: TaskExecutor) {
    protected val appContext: Context = context.applicationContext
    private val lock = Any()
    private val listeners = LinkedHashSet<ConstraintListener<T>>()

    private var currentState: T? = null

    /**
     * Add the given listener for tracking. This may cause [.getInitialState] and [.startTracking]
     * to be invoked. If a state is set, this will immediately notify the given listener.
     *
     * @param listener The target listener to start notifying
     */
    public fun addListener(listener: ConstraintListener<T>) {
        synchronized(lock) {
            if (listeners.add(listener)) {
                if (listeners.size == 1) {
                    currentState = readSystemState()
                    Logger.get()
                        .debug(TAG, "${javaClass.simpleName}: initial state = $currentState")
                    startTracking()
                }
                @Suppress("UNCHECKED_CAST") listener.onConstraintChanged(currentState as T)
            }
        }
    }

    /**
     * Remove the given listener from tracking.
     *
     * @param listener The listener to stop notifying.
     */
    public fun removeListener(listener: ConstraintListener<T>) {
        synchronized(lock) {
            if (listeners.remove(listener) && listeners.isEmpty()) {
                stopTracking()
            }
        }
    }

    public var state: T
        get() {
            return currentState ?: readSystemState()
        }
        set(newState) {
            synchronized(lock) {
                if (currentState != null && (currentState == newState)) {
                    return
                }

                currentState = newState

                // onConstraintChanged may lead to calls to addListener or removeListener.
                // This can potentially result in a modification to the set while it is being
                // iterated over, so we handle this by creating a copy and using that for
                // iteration.
                val listenersList = listeners.toList()
                taskExecutor.mainThreadExecutor.execute {
                    listenersList.forEach { listener ->
                        // currentState was initialized by now
                        @Suppress("UNCHECKED_CAST") listener.onConstraintChanged(currentState as T)
                    }
                }
            }
        }

    /**
     * Reads the state of the constraints from source of truth. (e.g. NetworkManager for
     * NetworkTracker). It is always accurate unlike `state` that can be stale after stopTracking
     * call.
     */
    public abstract fun readSystemState(): T

    /** Start tracking for constraint state changes. */
    public abstract fun startTracking()

    /** Stop tracking for constraint state changes. */
    public abstract fun stopTracking()
}

private val TAG = Logger.tagWithPrefix("ConstraintTracker")
