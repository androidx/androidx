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

import android.app.Activity;
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
public abstract class SavedStateStore {
    private static final String SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.SavedStateStoreImpl.key";
    private SafeIterableMap<String, SavedStateCallback> mComponents = new SafeIterableMap<>();
    private Bundle mSavedState;
    private boolean mRestored;

    /**
     * Arguments of the owning component.
     * <p>
     * Currently it doesn't track updates of underlying arguments,
     * but it will fixed before beta stage.
     * <p>
     * For a Fragment, it is arguments that were set once this fragment was created. It doesn't
     * track later changes done with {@code setArguments}.
     *
     * For an Activity, it is extras of intent that started that Activity
     * ({@link Activity#getIntent()})
     *
     * @return the arguments supplied when the owning was instantiated,
     * if any.
     */
    @Nullable
    @MainThread
    public abstract Bundle getArguments();

    /**
     * Consumes saved state previously supplied by {@link SavedStateCallback} registered via
     * {@link #registerSavedStateCallback(String, SavedStateCallback)} with the given {@code key}.
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return null.
     * <p>
     * All unconsumed values will be saved during {#code onSaveInstanceState(Bundle savedState)}
     * <p>
     * This method can be called after {@code super.onCreate(savedStateBundle)} of the corresponding
     * component.
     *
     * @param key a key with which {@link SavedStateCallback} was previously registered.
     * @return {@code Bundle} with the previously saved state or {@code null}
     */
    @MainThread
    @Nullable
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

    /**
     * This interface marks a component that contributes to saved state.
     */
    public interface SavedStateCallback {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from {@link #consumeRestoredStateForKey(String)}
         *
         * @return Bundle with your saved state.
         */
        @NonNull
        Bundle getSavedState();
    }

    /**
     * Registers a {@link SavedStateCallback} by the given {@code key}. This callback will be called
     * during state saving phase, returned bundle will be associated with the given {@code key}
     * and can be used after the restoration via {@link #consumeRestoredStateForKey(String)}.
     *
     * <p>
     * If there is unconsumed value with the same {@code key},
     * the value supplied by {@code callback} will be override and will be written to resulting
     * saved state bundle.
     *
     * @param key a key with which returned saved state will be associated
     * @param callback callback to get saved state.
     */
    @MainThread
    public void registerSavedStateCallback(@NonNull String key,
            @NonNull SavedStateCallback callback) {
        SavedStateCallback previousCallback = mComponents.putIfAbsent(key, callback);
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
    public void unregisterSaveStateCallback(@NonNull String key) {
        mComponents.remove(key);
    }

    /**
     * Returns if state was restored after creation and can be safely consumed
     * with {@link #consumeRestoredStateForKey(String)}
     * @return true if state was restored.
     */
    @MainThread
    public boolean isRestored() {
        return mRestored;
    }

    /**
     * An interface for implementations to restore saved state.
     * @param savedState restored state
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    protected void performRestoreState(@Nullable Bundle savedState) {
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
    protected void performSaveState(@NonNull Bundle outBundle) {
        Bundle res = mSavedState == null ? new Bundle() : new Bundle(mSavedState);
        for (Iterator<Map.Entry<String, SavedStateCallback>> it =
                mComponents.iteratorWithAdditions(); it.hasNext(); ) {
            Map.Entry<String, SavedStateCallback> entry = it.next();
            res.putBundle(entry.getKey(), entry.getValue().getSavedState());
        }
        outBundle.putBundle(SAVED_COMPONENTS_KEY, res);
    }
}
