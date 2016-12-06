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

import static com.android.support.lifecycle.state.StateMap.typeWarning;

/**
 * This class facilities work with retained state associated with corresponding
 * fragment or activity.
 *
 * "Retained state" is state which is retained across Activity or Fragment
 * re-creation (such as from a configuration change).
 */
public class RetainedStateProvider {

    private StateMap mStateMap = new StateMap();
    /**
     * Returns a {@link StateValue} for the given key. If no value with the given key exists,
     * a new StateValue will be returned.
     * If a value with the given key exists but it has a different type,
     * it will be overridden by a new one.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @return {@link StateValue} associated with given key
     */
    @MainThread
    public <T> StateValue<T> stateValue(String key) {
        Object o = mStateMap.mMap.get(key);
        StateValue<T> stateValue;
        try {
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
     * a new IntStateValue will be returned.
     * If a value with the given key exists but it has a different type,
     * it will be overridden by a new one.
     * <p>
     * Resulted {@link IntStateValue} will be initialized with 0.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    public IntStateValue intStateValue(String key) {
        return mStateMap.intValue(key, 0);
    }

    /**
     * Returns a {@link IntStateValue} for the given key. If no value with the given key exists,
     * a new IntStateValue will be returned.
     * If a value with the given key exists but it has a different type,
     * it will be overridden by a new one.
     * <p>
     * Resulted {@link IntStateValue} will be initialized with defaultValue.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    public IntStateValue intStateValue(String key, int defaultValue) {
        return mStateMap.intValue(key, defaultValue);
    }

}
