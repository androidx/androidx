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
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;

import java.util.Iterator;
import java.util.Map;

/**
 * A class for managing saved state.
 */
@SuppressLint("RestrictedApi")
public class SavedStateRegistry implements SavedState {
    private static final String SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.SavedStateStoreImpl.key";
    private SafeIterableMap<String, Callback> mComponents = new SafeIterableMap<>();
    private Bundle mSavedState;
    private boolean mRestored;

    @MainThread
    @Nullable
    @Override
    public Bundle consumeRestoredStateForKey(@NonNull String key) {
        if (!mRestored) {
            throw new IllegalStateException("You can consumeRestoredStateForKey "
                    + "only after super.onCreate of corresponding component");
        }
        Bundle state = null;
        if (mSavedState != null)  {
            state = mSavedState.getBundle(key);
            mSavedState.remove(key);
            if (mSavedState.isEmpty()) {
                mSavedState = null;
            }
        }
        return state;
    }

    @MainThread
    @Override
    public void registerSaveStateCallback(@NonNull String key, @NonNull Callback callback) {
        Callback previousCallback = mComponents.putIfAbsent(key, callback);
        if (previousCallback != null) {
            throw new IllegalArgumentException("Callback with the given key is already registered");
        }
    }

    /**
     * Unregisters a component previously registered by the given {@code key}
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    @Override
    public void unregisterSaveStateCallback(@NonNull String key) {
        mComponents.remove(key);
    }

    /**
     * Returns if state was restored after creation and can be safely consumed
     * with {@link #consumeRestoredStateForKey(String)}
     * @return true if state was restored.
     */
    @MainThread
    @Override
    public boolean isRestored() {
        return mRestored;
    }

    /**
     * An interface for implementations to restore saved state.
     * @param savedState restored state
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    public void performRestore(@Nullable Bundle savedState) {
        mSavedState = savedState != null ? savedState.getBundle(SAVED_COMPONENTS_KEY) : null;
        mRestored = true;
    }

    /**
     * An interface for implementations to perform state saving,
     * it will call all registered callbacks and merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    public void performSave(@NonNull Bundle outBundle) {
        Bundle res = mSavedState == null ? new Bundle() : new Bundle(mSavedState);
        for (Iterator<Map.Entry<String, Callback>> it =
                mComponents.iteratorWithAdditions(); it.hasNext(); ) {
            Map.Entry<String, Callback> entry = it.next();
            res.putBundle(entry.getKey(), entry.getValue().saveState());
        }
        outBundle.putBundle(SAVED_COMPONENTS_KEY, res);
    }
}
