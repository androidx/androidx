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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A NavigationProvider stores a set of {@link Navigator}s that are valid ways to navigate
 * to a destination.
 */
@SuppressLint("TypeParameterUnusedInFormals")
public class NavigatorProvider {
    private static final HashMap<Class<?>, String> sAnnotationNames = new HashMap<>();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean validateName(String name) {
        return name != null && !name.isEmpty();
    }

    @NonNull
    static String getNameForNavigator(@NonNull Class<? extends Navigator> navigatorClass) {
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

    private final HashMap<String, Navigator<? extends NavDestination>> mNavigators =
            new HashMap<>();

    /**
     * Retrieves a registered {@link Navigator} using the name provided by the
     * {@link Navigator.Name Navigator.Name annotation}.
     *
     * @param navigatorClass class of the navigator to return
     * @return the registered navigator with the given {@link Navigator.Name}
     *
     * @throws IllegalArgumentException if the Navigator does not have a
     * {@link Navigator.Name Navigator.Name annotation}
     * @throws IllegalStateException if the Navigator has not been added
     *
     * @see #addNavigator(Navigator)
     */
    @NonNull
    public final <T extends Navigator<?>> T getNavigator(@NonNull Class<T> navigatorClass) {
        String name = getNameForNavigator(navigatorClass);
        return getNavigator(name);
    }

    /**
     * Retrieves a registered {@link Navigator} by name.
     *
     * @param name name of the navigator to return
     * @return the registered navigator with the given name
     *
     * @throws IllegalStateException if the Navigator has not been added
     *
     * @see #addNavigator(String, Navigator)
     */
    @SuppressWarnings("unchecked")
    @CallSuper
    @NonNull
    public <T extends Navigator<?>> T getNavigator(@NonNull String name) {
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

    /**
     * Register a navigator using the name provided by the
     * {@link Navigator.Name Navigator.Name annotation}. {@link NavDestination destinations} may
     * refer to any registered navigator by name for inflation. If a navigator by this name is
     * already registered, this new navigator will replace it.
     *
     * @param navigator navigator to add
     * @return the previously added Navigator for the name provided by the
     * {@link Navigator.Name Navigator.Name annotation}, if any
     */
    @Nullable
    public final Navigator<? extends NavDestination> addNavigator(
            @NonNull Navigator<? extends NavDestination> navigator) {
        String name = getNameForNavigator(navigator.getClass());

        return addNavigator(name, navigator);
    }

    /**
     * Register a navigator by name. {@link NavDestination destinations} may refer to any
     * registered navigator by name for inflation. If a navigator by this name is already
     * registered, this new navigator will replace it.
     *
     * @param name name for this navigator
     * @param navigator navigator to add
     * @return the previously added Navigator for the given name, if any
     */
    @CallSuper
    @Nullable
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
}
