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

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class for managing saved state.
 */
public interface SavedState {
    /**
     * Consumes saved state previously supplied by {@link SavedState.Callback} registered
     * via {@link #registerSaveStateCallback(String, Callback)} with the given {@code key}.
     * <p>
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return null.
     * <p>
     * All unconsumed values will be saved during {@code onSaveInstanceState(Bundle savedState)}
     * <p>
     * This method can be called after {@code super.onCreate(savedStateBundle)} of the corresponding
     * component. Calling it before that will result in {@code IllegalArgumentException}.
     * {@link Lifecycle.Event#ON_CREATE} can be used as a signal
     * that a saved state can be consumed.
     *
     * @param key a key with which {@link SavedState.Callback} was previously registered.
     * @return {@code Bundle} with the previously saved state or {@code null}
     */
    @MainThread
    @Nullable
    Bundle consumeRestoredStateForKey(@NonNull String key);

    /**
     * Returns if a state was restored and can be safely consumed
     * with {@link #consumeRestoredStateForKey(String)}
     *
     * @return true if state was restored.
     */
    @MainThread
    boolean isRestored();

    /**
     * This interface marks a component that contributes to saved state.
     */
    interface Callback {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from {@link #consumeRestoredStateForKey(String)}
         *
         * @return Bundle with your saved state.
         */
        @NonNull
        Bundle saveState();
    }

    /**
     * Registers a {@link Callback} by the given {@code key}. This callback will be called
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
    void registerSaveStateCallback(@NonNull String key, @NonNull Callback callback);

    /**
     * Unregisters a component previously registered by the given {@code key}
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    void unregisterSaveStateCallback(@NonNull String key);
}
