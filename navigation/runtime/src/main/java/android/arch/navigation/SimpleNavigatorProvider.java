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

import java.util.HashMap;

/**
 * Simple implementation of a {@link NavigatorProvider} that stores instances of
 * {@link Navigator navigators} by name, using the {@link Navigator.Name} when given a class name.
 */
class SimpleNavigatorProvider implements NavigatorProvider {
    private final HashMap<String, Navigator<? extends NavDestination>> mNavigators =
            new HashMap<>();

    @Override
    public Navigator<? extends NavDestination> getNavigator(
            Class<? extends Navigator> navigatorClass) {
        Navigator.Name annotation = navigatorClass.getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (!validateName(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigatorClass.getSimpleName());
        }

        return getNavigator(name);
    }

    @Override
    public Navigator<? extends NavDestination> getNavigator(String name) {
        if (!validateName(name)) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        return mNavigators.get(name);
    }

    @Override
    public void addNavigator(Navigator<? extends NavDestination> navigator) {
        Navigator.Name annotation = navigator.getClass().getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (!validateName(name)) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigator.getClass().getSimpleName());
        }

        addNavigator(name, navigator);
    }

    @Override
    public void addNavigator(String name, Navigator<? extends NavDestination> navigator) {
        if (!validateName(name)) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }
        mNavigators.put(name, navigator);
    }

    private boolean validateName(String name) {
        return name != null && !name.isEmpty();
    }
}
