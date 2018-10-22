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
 * An interface for plugging components that consumes and contributes to the saved state.
 *
 * @param <S> represents a class for saving a state, typically it is {@link Bundle}
 */
public interface SavedStateRegistry<S> {
    /**
     * Consumes saved state previously supplied by {@link SavedStateProvider} registered
     * via {@link #registerSavedStateProvider(String, SavedStateProvider)}
     * with the given {@code key}.
     * <p>
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return {@code null}.
     * <p>
     * All unconsumed values will be saved during {@code onSaveInstanceState(Bundle savedState)}
     * <p>
     * This method can be called after {@code super.onCreate(savedStateBundle)} of the corresponding
     * component. Calling it before that will result in {@code IllegalArgumentException}.
     * {@link Lifecycle.Event#ON_CREATE} can be used as a signal
     * that a saved state can be safely consumed.
     *
     * @param key a key with which {@link SavedStateProvider} was previously registered.
     * @return {@code S} with the previously saved state or {@code null}
     */
    @MainThread
    @Nullable
    S consumeRestoredStateForKey(@NonNull String key);

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
     *
     * @param <S> represents a class for saving a state, typically it is {@link Bundle}
     */
    interface SavedStateProvider<S> {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from {@link #consumeRestoredStateForKey(String)}
         *
         * @return Bundle with your saved state.
         */
        @NonNull
        S saveState();
    }

    /**
     * Registers a {@link SavedStateProvider} by the given {@code key}. This
     * {@code savedStateProvider} will be called
     * during state saving phase, returned bundle will be associated with the given {@code key}
     * and can be used after the restoration via {@link #consumeRestoredStateForKey(String)}.
     * <p>
     * If there is unconsumed value with the same {@code key},
     * the value supplied by {@code savedStateProvider} will be override and
     * will be written to resulting saved state bundle.
     * <p> if a provider was already registered with the given {@code key}, an implementation should
     * throw an {@link IllegalArgumentException}
     * @param key a key with which returned saved state will be associated
     * @param savedStateProvider savedStateProvider to get saved state.
     */
    @MainThread
    void registerSavedStateProvider(@NonNull String key,
            @NonNull SavedStateProvider<S> savedStateProvider);

    /**
     * Unregisters a component previously registered by the given {@code key}
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    void unregisterSavedStateProvider(@NonNull String key);
}
