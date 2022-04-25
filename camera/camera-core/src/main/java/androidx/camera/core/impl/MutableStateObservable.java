/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A {@link StateObservable} whose state can be set.
 *
 * @param <T> The state type.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class MutableStateObservable<T> extends StateObservable<T> {

    private MutableStateObservable(@Nullable Object initialState, boolean isError) {
        super(initialState, isError);
    }

    /**
     * Creates a mutable state observer with the provided initial state.
     *
     * @param initialState The initial state
     * @param <T>          The state type
     * @return A mutable state observable initialized with the given initial state.
     */
    @NonNull
    public static <T> MutableStateObservable<T> withInitialState(@Nullable T initialState) {
        return new MutableStateObservable<>(initialState, false);
    }

    /**
     * Creates a mutable state observer in an initial error state containing the provided
     * {@link Throwable}.
     *
     * @param initialError The {@link Throwable} contained by the error state.
     * @param <T>          The state type
     * @return A mutable state observable initialized in an error state containing the provided
     * {@link Throwable}.
     */
    @NonNull
    public static <T> MutableStateObservable<T> withInitialError(@NonNull Throwable initialError) {
        return new MutableStateObservable<>(initialError, true);
    }

    /**
     * Posts a new state to be used as the current state of this Observable.
     */
    public void setState(@Nullable T state) {
        updateState(state);
    }

    /**
     * Posts a new {@link Throwable} to be used in the new error state of this Observable.
     */
    public void setError(@NonNull Throwable error) {
        updateStateAsError(error);
    }

}
