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

package androidx.wear.watchface

import androidx.annotation.UiThread
import androidx.lifecycle.Observer

/**
 * An observable UI thread only data holder class.
 *
 * @param <T> The type of data hold by this instance
 */
open class WatchData<T> protected constructor(protected var _value: T?) {
    private var iterating = false
    internal val observers = ArrayList<Observer<T>>()
    internal val toBeRemoved = HashSet<Observer<T>>()

    /** Whether or not this WatchData contains a value. */
    fun hasValue() = _value != null

    /** Returns the value contained within this WatchData or default if there isn't one. */
    fun getValueOr(default: T) = if (_value != null) {
        _value!!
    } else {
        default
    }

    open var value: T
        @UiThread
        get() = _value!!

        @UiThread
        protected set(v) {
            require(!iterating)
            iterating = true
            _value = v
            for (observer in observers) {
                // The observer might unregister itself and removing elements from a collection
                // while it's being iterated is not allowed, so check if we should skip over this
                // observer.
                if (!toBeRemoved.contains(observer)) {
                    observer.onChanged(_value)
                }
            }
            iterating = false
            for (observer in toBeRemoved) {
                observers.remove(observer)
            }
        }

    /**
     * Adds the given observer to the observers list. The events are dispatched on the ui thread.
     * If there's any data held within the WatchData it will be immediately delivered to the
     * observer.
     */
    @UiThread
    fun observe(observer: Observer<T>) {
        require(!observers.contains(observer))
        observers.add(observer)
        if (hasValue()) {
            observer.onChanged(_value)
        }
    }

    /** Removes an observer previously added by [observe]. */
    @UiThread
    fun removeObserver(observer: Observer<T>) {
        require(observers.contains(observer))

        if (iterating) {
            toBeRemoved.add(observer)
        } else {
            observers.remove(observer)
        }
    }
}

/**
 * [WatchData] which publicly exposes [setValue(T)] method
 *
 * @param <T> The type of data hold by this instance
 */
@SuppressWarnings("WeakerAccess")
class MutableWatchData<T>(initialValue: T?) : WatchData<T>(initialValue) {
    constructor() : this(null)

    override var value: T
        @UiThread
        get() = _value!!

        @UiThread
        public set(v) {
            super.value = v
        }
}
