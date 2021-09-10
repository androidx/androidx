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

/**
 * An observable UI thread only data holder class (see [Observer]).
 *
 * @param T The type of data held by this instance.
 * @param _value The initial value or `null` if there isn't an initial value.
 */
public sealed class ObservableWatchData<T : Any> constructor(internal var _value: T?) {

    private var iterating = false
    private val observers = ArrayList<Observer<T>>()
    private val toBeRemoved = HashSet<Observer<T>>()

    /** Whether or not this ObservableWatchData contains a value. */
    @UiThread
    public fun hasValue(): Boolean = _value != null

    /**
     * Returns the value contained within this ObservableWatchData or default if there isn't one.
     */
    @UiThread
    public fun getValueOr(default: T): T = if (_value != null) {
        _value!!
    } else {
        default
    }

    /** The observable value. */
    public open var value: T
        @UiThread
        get() = _value!!
        @UiThread
        protected set(v) {
            require(!iterating)
            iterating = true
            _value = v

            var index = 0
            while (index < observers.size) {
                val observer = observers[index++]
                // The observer might unregister itself.
                if (!toBeRemoved.contains(observer)) {
                    observer.onChanged(v)
                }
            }
            iterating = false
            for (observer in toBeRemoved) {
                observers.remove(observer)
            }
            toBeRemoved.clear()
        }

    /**
     * Adds the given [Observer] to the observers list. If [hasValue] would return true then
     * [Observer.onChanged] will be called. Subsequently [Observer.onChanged] will also be called
     * any time [value] changes. All of these callbacks are assumed to occur on the UI thread.
     */
    @UiThread
    public fun addObserver(observer: Observer<T>) {
        require(!observers.contains(observer))
        observers.add(observer)
        // We want to dispatch a callback when added, and if we're iterating then adding to the end
        // of the list is sufficient.
        if (!iterating && _value != null) {
            observer.onChanged(_value!!)
        }
    }

    /** Removes an observer previously added by [addObserver]. */
    @UiThread
    public fun removeObserver(observer: Observer<T>) {
        require(observers.contains(observer))

        if (iterating) {
            toBeRemoved.add(observer)
        } else {
            observers.remove(observer)
        }
    }

    override fun toString(): String {
        return if (hasValue()) {
            value.toString()
        } else {
            "<unset>"
        }
    }

    /**
     * [ObservableWatchData] which publicly exposes [setValue(T)] method.
     *
     * @param T The type of data held by this instance
     */
    public class MutableObservableWatchData<T : Any>(initialValue: T?) :
        ObservableWatchData<T>(initialValue) {
        public constructor() : this(null)

        /**
         * Mutable observable value. Assigning a different value will trigger [Observer.onChanged]
         * callbacks.
         */
        override var value: T
            @UiThread
            get() = _value!!
            @UiThread
            public set(v) {
                super.value = v
            }
    }
}
