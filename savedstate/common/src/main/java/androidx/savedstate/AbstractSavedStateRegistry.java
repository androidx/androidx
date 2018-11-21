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

package androidx.savedstate;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class provides a skeletal implementation of the {@link SavedStateRegistry}.
 * <p>
 * Implementations simply need to call {@link #restoreSavedState(Map)} to initialize restored state
 * and call {@link #saveState()} once system requests saved state.
 *
 * @param <S> represents a class for saving a state, typically it is {@link android.os.Bundle}
 *
 * @see androidx.activity.BundleSavedStateRegistry
 */
public abstract class AbstractSavedStateRegistry<S> implements SavedStateRegistry<S> {
    private SafeIterableMap<String, SavedStateProvider<S>> mComponents =
            new SafeIterableMap<>();
    private Map<String, S> mSavedState;
    private boolean mRestored;

    @MainThread
    @Nullable
    @Override
    public final S consumeRestoredStateForKey(@NonNull String key) {
        if (!mRestored) {
            throw new IllegalStateException("You can consumeRestoredStateForKey "
                    + "only after super.onCreate of corresponding component");
        }
        S state = null;
        if (mSavedState != null) {
            state = mSavedState.remove(key);
            if (mSavedState.isEmpty()) {
                mSavedState = null;
            }
        }
        return state;
    }

    @MainThread
    @Override
    public final void registerSavedStateProvider(@NonNull String key,
            @NonNull SavedStateProvider<S> provider) {
        SavedStateProvider<S> previous = mComponents.putIfAbsent(key, provider);
        if (previous != null) {
            throw new IllegalArgumentException("SavedStateProvider with the given key is"
                    + " already registered");
        }
    }

    /**
     * Unregisters a component previously registered by the given {@code key}
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    @Override
    public final void unregisterSavedStateProvider(@NonNull String key) {
        mComponents.remove(key);
    }

    /**
     * Returns if state was restored after creation and can be safely consumed
     * with {@link #consumeRestoredStateForKey(String)}
     *
     * @return true if state was restored.
     */
    @MainThread
    @Override
    public final boolean isRestored() {
        return mRestored;
    }

    /**
     * Subclasses of this {@code AbstractSavedStateRegistry} should call this
     * method to initialize restored state.
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    protected final void restoreSavedState(@Nullable Map<String, S> initialState) {
        if (initialState != null) {
            mSavedState = new HashMap<>(initialState);
        }
        mRestored = true;
    }

    /**
     * Subclasses of this {@code AbstractSavedStateRegistry} should call this
     * method to perform state saving, this method will call all registered providers and
     * merge a state provided by them with all unconsumed values since previous restoration.
     *
     * @return state that should be saved.
     */
    @MainThread
    @NonNull
    protected final Map<String, S> saveState() {
        Map<String, S> savedState = new HashMap<>();
        if (mSavedState != null) {
            savedState.putAll(mSavedState);
        }
        for (Iterator<Map.Entry<String, SavedStateProvider<S>>> it =
                mComponents.iteratorWithAdditions(); it.hasNext(); ) {
            Map.Entry<String, SavedStateProvider<S>> entry = it.next();
            savedState.put(entry.getKey(), entry.getValue().saveState());
        }
        return savedState;
    }
}
