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

package androidx.savedstate.bundle;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.savedstate.AbstractSavedStateRegistry;
import androidx.savedstate.SavedStateRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * A default implementation of {@link SavedStateRegistry} backed by {@link Bundle}.
 * <p>
 * An owner of this {@link BundleSavedStateRegistry} must call {@link #performRestore(Bundle)}
 * once previously saved state becomes available to it.
 * <p>
 * To collect saved state supplied by {@link SavedStateRegistry.SavedStateProvider}
 * an owner should call {@link #performSave(Bundle)}
 */
public final class BundleSavedStateRegistry extends AbstractSavedStateRegistry<Bundle> {
    private static final String SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key";

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry} to restore saved state.
     *
     * @param savedState restored state
     */
    @SuppressWarnings("WeakerAccess")
    @MainThread
    public void performRestore(@Nullable Bundle savedState) {
        Bundle componentsState = savedState != null ? savedState.getBundle(SAVED_COMPONENTS_KEY)
                : null;
        if (componentsState == null || componentsState.isEmpty()) {
            restoreSavedState(null);
            return;
        }
        Map<String, Bundle> initialState = new HashMap<>();
        for (String key : componentsState.keySet()) {
            initialState.put(key, componentsState.getBundle(key));
        }
        restoreSavedState(initialState);
    }

    /**
     * An interface for an owner of this @{code {@link SavedStateRegistry}
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    public void performSave(@NonNull Bundle outBundle) {
        Map<String, Bundle> bundleMap = saveState();
        Bundle components = new Bundle();
        for (Map.Entry<String, Bundle> entry : bundleMap.entrySet()) {
            components.putBundle(entry.getKey(), entry.getValue());
        }
        outBundle.putBundle(SAVED_COMPONENTS_KEY, components);
    }
}
