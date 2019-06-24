/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle;

import android.annotation.SuppressLint;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseArray;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.savedstate.SavedStateRegistry.SavedStateProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A handle to saved state passed down to {@link androidx.lifecycle.ViewModel}. You should use
 * {@link SavedStateVMFactory} if you want to receive this object in {@code ViewModel}'s
 * constructor.
 * <p>
 * This is a key-value map that will let you write and retrieve objects to and from the saved state.
 * These values will persist after the process is killed by the system
 * and remain available via the same object.
 * <p>
 * You can read a value from it via {@link #get(String)} or observe it via
 * {@link androidx.lifecycle.LiveData} returned
 * by {@link #getLiveData(String)}.
 * <p>
 * You can write a value to it via {@link #set(String, Object)} or setting a value to
 * {@link androidx.lifecycle.MutableLiveData} returned by {@link #getLiveData(String)}.
 */
public final class SavedStateHandle {
    final Map<String, Object> mRegular;
    private final Map<String, SavingStateLiveData<?>> mLiveDatas = new HashMap<>();

    private static final String VALUES = "values";
    private static final String KEYS = "keys";

    private final SavedStateProvider mSavedStateProvider = new SavedStateProvider() {
        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public Bundle saveState() {
            Set<String> keySet = mRegular.keySet();
            ArrayList keys = new ArrayList(keySet.size());
            ArrayList value = new ArrayList(keys.size());
            for (String key : keySet) {
                keys.add(key);
                value.add(mRegular.get(key));
            }

            Bundle res = new Bundle();
            // "parcelable" arraylists - lol
            res.putParcelableArrayList("keys", keys);
            res.putParcelableArrayList("values", value);
            return res;
        }
    };


    /**
     * Creates a handle with the given initial arguments.
     */
    public SavedStateHandle(@NonNull Map<String, Object> initialState) {
        mRegular = new HashMap<>(initialState);
    }

    /**
     * Creates a handle with the empty state.
     */
    public SavedStateHandle() {
        mRegular = new HashMap<>();
    }

    static SavedStateHandle createHandle(@Nullable Bundle restoredState,
            @Nullable Bundle defaultState) {
        if (restoredState == null && defaultState == null) {
            return new SavedStateHandle();
        }

        Map<String, Object> state = new HashMap<>();
        if (defaultState != null) {
            for (String key : defaultState.keySet()) {
                state.put(key, defaultState.get(key));
            }
        }

        if (restoredState == null) {
            return new SavedStateHandle(state);
        }

        ArrayList keys = restoredState.getParcelableArrayList(KEYS);
        ArrayList values = restoredState.getParcelableArrayList(VALUES);
        if (keys == null || values == null || keys.size() != values.size()) {
            throw new IllegalStateException("Invalid bundle passed as restored state");
        }
        for (int i = 0; i < keys.size(); i++) {
            state.put((String) keys.get(i), values.get(i));
        }
        return new SavedStateHandle(state);
    }

    @NonNull
    SavedStateProvider savedStateProvider() {
        return mSavedStateProvider;
    }

    /**
     * @return true if there is value associated with the given key.
     */
    @MainThread
    public boolean contains(@NonNull String key) {
        return mRegular.containsKey(key);
    }

    /**
     * Returns a {@link androidx.lifecycle.LiveData} that access data associated with the given key.
     *
     * @see #getLiveData(String, Object)
     */
    @SuppressWarnings("unchecked")
    @MainThread
    @NonNull
    public <T> MutableLiveData<T> getLiveData(@NonNull String key) {
        return getLiveDataInternal(key, false, null);
    }

    /**
     * Returns a {@link androidx.lifecycle.LiveData} that access data associated with the given key.
     *
     * <pre>{@code
     *     LiveData<String> liveData = savedStateHandle.get(KEY, "defaultValue");
     * }</pre
     *
     * Keep in mind that {@link LiveData} can have {@code null} as a valid value. If the
     * {@code initialValue} is {@code null} and the data does not already exist in the
     * {@link SavedStateHandle}, the value of the returned {@link LiveData} will be set to
     * {@code null} and observers will be notified. You can call {@link #getLiveData(String)} if
     * you want to avoid dispatching {@code null} to observers.
     * <pre>{@code
     *     String defaultValue = ...; // nullable
     *     LiveData<String> liveData;
     *     if (defaultValue != null) {
     *         liveData = savedStateHandle.get(KEY, defaultValue);
     *     } else {
     *         liveData = savedStateHandle.get(KEY);
     *     }
     * }</pre>
     *
     * @param key          The identifier for the value
     * @param initialValue If no value exists with the given {@code key}, a new one is created
     *                     with the given {@code initialValue}. Note that passing {@code null} will
     *                     create a {@link LiveData} with {@code null} value.
     */
    @MainThread
    @NonNull
    public <T> MutableLiveData<T> getLiveData(@NonNull String key,
            @SuppressLint("UnknownNullness") T initialValue) {
        return getLiveDataInternal(key, true, initialValue);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private <T> MutableLiveData<T> getLiveDataInternal(
            @NonNull String key,
            boolean hasInitialValue,
            @Nullable T initialValue) {
        MutableLiveData<T> liveData = (MutableLiveData<T>) mLiveDatas.get(key);
        if (liveData != null) {
            return liveData;
        }
        SavingStateLiveData<T> mutableLd;
        // double hashing but null is valid value
        if (mRegular.containsKey(key)) {
            mutableLd = new SavingStateLiveData<>(this, key, (T) mRegular.get(key));
        } else if (hasInitialValue) {
            mutableLd = new SavingStateLiveData<>(this, key, initialValue);
        } else {
            mutableLd = new SavingStateLiveData<>(this, key);
        }
        mLiveDatas.put(key, mutableLd);
        return mutableLd;
    }

    /**
     * Returns all keys contained in this {@link SavedStateHandle}
     */
    @MainThread
    @NonNull
    public Set<String> keys() {
        return Collections.unmodifiableSet(mRegular.keySet());
    }

    /**
     * Returns a value associated with the given key.
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    @MainThread
    @Nullable
    public <T> T get(@NonNull String key) {
        return (T) mRegular.get(key);
    }

    /**
     * Associate the given value with the key. The value must have a type that could be stored in
     * {@link android.os.Bundle}
     *
     * @param <T> any type that can be accepted by Bundle.
     */
    @MainThread
    public <T> void set(@NonNull String key, @Nullable T value) {
        validateValue(value);
        @SuppressWarnings("unchecked")
        MutableLiveData<T> mutableLiveData = (MutableLiveData<T>) mLiveDatas.get(key);
        if (mutableLiveData != null) {
            // it will set value;
            mutableLiveData.setValue(value);
        } else {
            mRegular.put(key, value);
        }
    }

    private static void validateValue(Object value) {
        if (value == null) {
            return;
        }
        for (Class<?> cl : ACCEPTABLE_CLASSES) {
            if (cl.isInstance(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("Can't put value with type " + value.getClass()
                + " into saved state");
    }

    /**
     * Removes a value associated with the given key. If there is a {@link LiveData} associated
     * with the given key, it will be removed as well.
     * <p>
     * All changes to {@link androidx.lifecycle.LiveData} previously
     * returned by {@link SavedStateHandle#getLiveData(String)} won't be reflected in
     * the saved state. Also that {@code LiveData} won't receive any updates about new values
     * associated by the given key.
     *
     * @param key a key
     * @return a value that was previously associated with the given key.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @MainThread
    @Nullable
    public <T> T remove(@NonNull String key) {
        @SuppressWarnings("unchecked")
        T latestValue = (T) mRegular.remove(key);
        SavingStateLiveData<?> liveData = mLiveDatas.remove(key);
        if (liveData != null) {
            liveData.detach();
        }
        return latestValue;
    }

    static class SavingStateLiveData<T> extends MutableLiveData<T> {
        private String mKey;
        private SavedStateHandle mHandle;

        SavingStateLiveData(SavedStateHandle handle, String key, T value) {
            super(value);
            mKey = key;
            mHandle = handle;
        }

        SavingStateLiveData(SavedStateHandle handle, String key) {
            super();
            mKey = key;
            mHandle = handle;
        }

        @Override
        public void setValue(T value) {
            if (mHandle != null) {
                mHandle.mRegular.put(mKey, value);
            }
            super.setValue(value);
        }

        void detach() {
            mHandle = null;
        }
    }

    // doesn't have Integer, Long etc box types because they are "Serializable"
    private static final Class[] ACCEPTABLE_CLASSES = new Class[]{
            //baseBundle
            boolean.class,
            boolean[].class,
            double.class,
            double[].class,
            int.class,
            int[].class,
            long.class,
            long[].class,
            String.class,
            String[].class,
            //bundle
            Binder.class,
            Bundle.class,
            byte.class,
            byte[].class,
            char.class,
            char[].class,
            CharSequence.class,
            CharSequence[].class,
            // type erasure ¯\_(ツ)_/¯, we won't eagerly check elements contents
            ArrayList.class,
            float.class,
            float[].class,
            Parcelable.class,
            Parcelable[].class,
            Serializable.class,
            short.class,
            short[].class,
            SparseArray.class,
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? Size.class : int.class),
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? SizeF.class : int.class),
    };
}
