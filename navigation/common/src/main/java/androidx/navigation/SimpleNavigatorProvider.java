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

package androidx.navigation;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple implementation of a {@link NavigatorProvider} that stores instances of
 * {@link Navigator navigators} by name, using the {@link Navigator.Name} when given a class name.
 *
 * @hide
 */
@SuppressLint("TypeParameterUnusedInFormals")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SimpleNavigatorProvider implements NavigatorProvider {
    private static final HashMap<Class, String> sAnnotationNames = new HashMap<>();

    private final HashMap<String, Navigator<? extends NavDestination>> mNavigators =
            new HashMap<>();

    @NonNull
    private String getNameForNavigator(@NonNull Class<? extends Navigator> navigatorClass) {
        String name = sAnnotationNames.get(navigatorClass);
        if (name == null) {
            Navigator.Name annotation = navigatorClass.getAnnotation(Navigator.Name.class);
            name = annotation != null ? annotation.value() : null;
            if (!validateName(name)) {
                throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                        + navigatorClass.getSimpleName());
            }
            sAnnotationNames.put(navigatorClass, name);
        }
        return name;
    }

    @NonNull
    @Override
    public <D extends NavDestination, T extends Navigator<? extends D>> T getNavigator(
            @NonNull Class<T> navigatorClass) {
        String name = getNameForNavigator(navigatorClass);
        return getNavigator(name);
    }

    @SuppressWarnings("unchecked")
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
        return (T) navigator;
    }

    @Nullable
    @Override
    public Navigator<? extends NavDestination> addNavigator(
            @NonNull Navigator<? extends NavDestination> navigator) {
        String name = getNameForNavigator(navigator.getClass());

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

    Map<String, Navigator<? extends NavDestination>> getNavigators() {
        return mNavigators;
    }

    private boolean validateName(String name) {
        return name != null && !name.isEmpty();
    }
}
