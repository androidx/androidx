/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle.state;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;

import static com.android.support.lifecycle.state.StateMap.typeWarning;

/**
 * This class facilities work with retained state associated with corresponding fragment or
 * activity.
 * <p>
 * "Retained state" is the state which is retained across Activity or Fragment
 * re-creation (such as from a configuration change).
 * <p>
 * You should <b>never</b> reference the Activity or Fragment from a value you retain.
 */
@SuppressWarnings("WeakerAccess")
public class RetainedStateProvider {

    private StateMap mStateMap = new StateMap();
    /**
     * Returns a {@link StateValue} for the given key. If no value with the given key exists,
     * a new StateValue will be returned.
     * <p>
     * If a value with the given key exists but it has a different type,
     * it will be overridden by a new one.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost if the application process is kill after the change.
     *
     * @param key The unique identifier of the value.
     *
     * @return {@link StateValue} associated with the given key
     */
    @MainThread
    @NonNull
    public <T> StateValue<T> stateValue(String key) {
        Object o = mStateMap.mMap.get(key);
        StateValue<T> stateValue;
        try {
            //noinspection unchecked
            stateValue = (StateValue<T>) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, StateValue.class.getName(), e);
            stateValue = null;
        }

        if (stateValue == null) {
            stateValue = new StateValue<>();
            mStateMap.mMap.put(key, stateValue);
        }
        return stateValue;
    }

    /**
     * Returns a {@link IntStateValue} for the given key. If no value with the given key exists,
     * a new {@link IntStateValue} will be returned.
     * <p>
     * If a value with the given key exists but it has a different type,
     * it will be overridden by the new one.
     * <p>
     * Resulting {@link IntStateValue} will be initialized with {@code 0}.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment is called, may be lost if the application process is killed.
     *
     * @param key The unique identifier of the value.
     *
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    @NonNull
    public IntStateValue intStateValue(String key) {
        return mStateMap.intValue(key, 0);
    }

    /**
     * Returns a {@link IntStateValue} for the given key. If no value with the given key exists,
     * a new {@link IntStateValue} will be returned.
     * <p>
     * If a value with the given key exists but it has a different type,
     * it will be overridden by the new one.
     * <p>
     * Resulting {@link IntStateValue} will be initialized with {@code defaultValue}.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment is called, may be lost if the application process is killed.
     *
     * @param key The unique identifier of the value.
     *
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    @NonNull
    public IntStateValue intStateValue(String key, int defaultValue) {
        return mStateMap.intValue(key, defaultValue);
    }

}
