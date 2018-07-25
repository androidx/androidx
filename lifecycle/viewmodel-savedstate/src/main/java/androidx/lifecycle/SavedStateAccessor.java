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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * All data stored in this class will be saved as saved state,
 * once {@code onSaveInstanceState} method is called on the corresponding component
 * (Fragment/Activity).
 *
 * There is no guarantees if the last saved value is actually kept,
 * onSaveInstance can actually occur before latest mutations and never happen afterwards.
 */
public class SavedStateAccessor {
    final Map<String, Object> mRegular;
    private final Map<String, SavingStateLiveData<?>> mLiveDatas = new HashMap<>();

    /**
     * Creates {@code SavedStateAccessor} with an empty state.
     */
    @MainThread
    public SavedStateAccessor() {
        mRegular = new HashMap<>();
    }

    /**
     * Creates {@code SavedStateAccessor} with the given initial state.
     *
     * @param initialState state to initialize with
     */
    @MainThread
    public SavedStateAccessor(Map<String, Object> initialState) {
        mRegular = new HashMap<>(initialState);
    }

    /**
     * @return true if there is value associated with the given key.
     */
    @MainThread
    public boolean contains(@NonNull String key) {
        return mRegular.containsKey(key);
    }

    /**
     * Returns a {@link LiveData} that access data associated with the given key,.
     */
    @SuppressWarnings("unchecked")
    @MainThread
    public <T> MutableLiveData<T> getLiveData(@Nullable String key) {
        if (mLiveDatas.containsKey(key)) {
            //noinspection unchecked
            return (MutableLiveData<T>) mLiveDatas.get(key);
        }
        SavingStateLiveData<T> mutableLd = new SavingStateLiveData<>(this, key);
        if (mRegular.containsKey(key)) {
            mutableLd.setValue((T) mRegular.get(key));
        }
        mLiveDatas.put(key, mutableLd);
        return mutableLd;
    }

    /**
     * Returns all keys contained in this {@link SavedStateAccessor}
     */
    @MainThread
    @NonNull
    public Set<String> keys() {
        return Collections.unmodifiableSet(mRegular.keySet());
    }

    /**
     * Returns a value associated with the given key.
     */
    @SuppressWarnings("unchecked")
    @MainThread
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
    public <T> void set(@NonNull String key, T value) {
        validateValue(value);
        if (mLiveDatas.containsKey(key)) {
            @SuppressWarnings("unchecked")
            MutableLiveData<T> mutableLiveData = (MutableLiveData<T>) mLiveDatas.get(key);
            // it will set value;
            mutableLiveData.setValue(value);
        } else {
            mRegular.put(key, value);
        }
    }

    private void validateValue(Object value) {
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
     *
     * All changes to {@link LiveData} previously
     * returned by {@link SavedStateAccessor#getLiveData(String)} won't be reflected in
     * the saved state. Also that {@code LiveData} won't receive any updates about new values
     * associated by the given key.
     *
     * @param key a key
     * @return a value that was previously associated with the given key.
     */
    @MainThread
    public <T> T remove(@NonNull String key) {
        if (!mRegular.containsKey(key)) {
            return null;
        }
        T latestValue = get(key);
        if (mLiveDatas.containsKey(key)) {
            mLiveDatas.remove(key).detach();
        }
        mRegular.remove(key);
        //noinspection unchecked
        return latestValue;
    }

    static class SavingStateLiveData<T> extends MutableLiveData<T> {
        private String mKey;
        private SavedStateAccessor mAccessor;

        SavingStateLiveData(SavedStateAccessor accessor, String key) {
            mKey = key;
            mAccessor = accessor;
        }

        @Override
        public void setValue(T value) {
            if (mAccessor != null) {
                mAccessor.mRegular.put(mKey, value);
            }
            super.setValue(value);
        }

        void detach() {
            mAccessor = null;
        }
    }

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
