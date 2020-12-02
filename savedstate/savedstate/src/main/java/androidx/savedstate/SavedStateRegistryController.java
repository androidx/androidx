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

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;

/**
 * An API for {@link SavedStateRegistryOwner} implementations to control {@link SavedStateRegistry}.
 * <p>
 * {@code SavedStateRegistryOwner} should call {@link #performRestore(Bundle)} to restore state of
 * {@link SavedStateRegistry} and {@link #performSave(Bundle)} to gather SavedState from it.
 */
public final class SavedStateRegistryController {
    private final SavedStateRegistryOwner mOwner;
    private final SavedStateRegistry mRegistry;

    private SavedStateRegistryController(SavedStateRegistryOwner owner) {
        mOwner = owner;
        mRegistry = new SavedStateRegistry();
    }

    /**
     * Returns controlled {@link SavedStateRegistry}
     */
    @NonNull
    public SavedStateRegistry getSavedStateRegistry() {
        return mRegistry;
    }

    /**
     * An interface for an owner of this {@link SavedStateRegistry} to restore saved state.
     *
     * @param savedState restored state
     */
    @MainThread
    public void performRestore(@Nullable Bundle savedState) {
        Lifecycle lifecycle = mOwner.getLifecycle();
        if (lifecycle.getCurrentState() != Lifecycle.State.INITIALIZED) {
            throw new IllegalStateException("Restarter must be created only during "
                    + "owner's initialization stage");
        }
        lifecycle.addObserver(new Recreator(mOwner));
        mRegistry.performRestore(lifecycle, savedState);
    }

    /**
     * An interface for an owner of this  {@link SavedStateRegistry}
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    public void performSave(@NonNull Bundle outBundle) {
        mRegistry.performSave(outBundle);
    }

    /**
     * Creates a {@link SavedStateRegistryController}.
     * <p>
     * It should be called during construction time of {@link SavedStateRegistryOwner}
     */
    @NonNull
    public static SavedStateRegistryController create(@NonNull SavedStateRegistryOwner owner) {
        return new SavedStateRegistryController(owner);
    }
}
