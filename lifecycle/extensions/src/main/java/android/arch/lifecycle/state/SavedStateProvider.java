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

package android.arch.lifecycle.state;

import static android.arch.lifecycle.state.StateMap.typeWarning;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.Map;

/**
 * Class that facilitates work with saved state associated with an Activity or Fragment.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SavedStateProvider {

    private static final String TAG = "SavedStateProvider";

    private StateMap mStateMap = new StateMap();

    /**
     * Returns a {@link SavedStateValue} for the given key. If no value with the given key exists,
     * a new SavedStateValue will be returned.
     * If a value with the given key exists but it has a different type,
     * it will be overridden by a new one.
     * <p>
     * Usage of this method is equal to putting value into the Bundle in onSaveInstanceState method.
     * <p>
     * Changes, that were made to returned value after onSaveInstanceState in corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @return {@link SavedStateValue} associated with given key
     */
    @MainThread
    @NonNull
    public <T extends Parcelable> SavedStateValue<T> stateValue(String key) {
        Object o = mStateMap.mMap.get(key);
        SavedStateValue<T> stateValue;
        try {
            //noinspection unchecked
            stateValue = (SavedStateValue<T>) o;
        } catch (ClassCastException e) {
            typeWarning(key, o, SavedStateValue.class.getName(), e);
            stateValue = null;
        }

        if (stateValue == null) {
            T value = null;
            if (mStateMap.mSavedState != null) {
                value = mStateMap.mSavedState.getParcelable(key);
            }
            stateValue = new SavedStateValue<>(value);
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
     * Usage of this method is equal to putting a value into a Bundle in onSaveInstanceState method.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    @NonNull
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
     * Usage of this method is equal to putting a value into a Bundle in onSaveInstanceState method.
     * <p>
     * Changes, that were made to the returned value after onSaveInstanceState in the corresponding
     * activity or fragment, may be lost.
     *
     * @param key a String, or null
     * @param defaultValue a value to initialize with if no {@link IntStateValue} was associated
     *                     with the given key
     * @return {@link IntStateValue} associated with given key
     */
    @MainThread
    @NonNull
    public IntStateValue intStateValue(String key, int defaultValue) {
        return mStateMap.intValue(key, defaultValue);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void restoreState(Bundle savedState) {
        mStateMap.mSavedState = savedState;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void saveState(Bundle outBundle) {
        if (mStateMap.mSavedState != null) {
            outBundle.putAll(mStateMap.mSavedState);
        }
        Map<String, Object> map = mStateMap.mMap;
        for (String key: map.keySet()) {
            ((Saveable) map.get(key)).saveTo(outBundle, key);
        }
    }
}
