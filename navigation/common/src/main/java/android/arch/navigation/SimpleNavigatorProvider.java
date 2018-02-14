/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.navigation;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.HashMap;

/**
 * Simple implementation of a {@link NavigatorProvider} that stores instances of
 * {@link Navigator navigators} by name, using the {@link Navigator.Name} when given a class name.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SimpleNavigatorProvider implements NavigatorProvider {
    private final HashMap<String, Navigator<? extends NavDestination>> mNavigators =
            new HashMap<>();

    @NonNull
    @Override
    public <D extends NavDestination, T extends Navigator<? extends D>> T getNavigator(
            @NonNull Class<T> navigatorClass) {
        Navigator.Name annotation = navigatorClass.getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (!validateName(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigatorClass.getSimpleName());
        }

        return getNavigator(name);
    }

    @NonNull
    @Override
    public <D extends NavDestination, T extends Navigator<? extends D>> T getNavigator(
            @NonNull String name) {
        if (!validateName(name)) {
            throw new IllegalArgumentException("navigator name cannot be an empty string");
        }

        Navigator<? extends NavDestination> navigator = mNavigators.get(name);
        if (navigator == null) {
            throw new IllegalStateException("Could not find Navigator with name \"" + name
                    + "\". You must call NavController.addNavigator() for each navigation type.");
        }
        //noinspection unchecked
        return (T) navigator;
    }

    @Nullable
    @Override
    public Navigator<? extends NavDestination> addNavigator(
            @NonNull Navigator<? extends NavDestination> navigator) {
        Navigator.Name annotation = navigator.getClass().getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (!validateName(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigator.getClass().getSimpleName());
        }

        return addNavigator(name, navigator);
    }

    @Nullable
    @Override
    public Navigator<? extends NavDestination> addNavigator(@NonNull String name,
            @NonNull Navigator<? extends NavDestination> navigator) {
        if (!validateName(name)) {
            throw new IllegalArgumentException("navigator name cannot be an empty string");
        }
        return mNavigators.put(name, navigator);
    }

    private boolean validateName(String name) {
        return name != null && !name.isEmpty();
    }
}
