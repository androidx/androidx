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

import android.content.Context;

import java.util.HashMap;

/**
 * Simple NavigatorProvider that only supports &lt;navigation&gt; and &lt;test&gt; navigation
 * elements.
 */
public class TestNavigatorProvider implements NavigatorProvider {
    private final HashMap<String, Navigator> mNavigators = new HashMap<>();

    TestNavigatorProvider(Context context) {
        addNavigator(new NavGraphNavigator(context));
        addNavigator(new TestNavigator());
    }

    @Override
    public Navigator getNavigator(Class<? extends Navigator> navigatorClass) {
        Navigator.Name annotation = navigatorClass.getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigatorClass.getSimpleName());
        }

        return getNavigator(name);
    }

    @Override
    public Navigator getNavigator(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        return mNavigators.get(name);
    }

    @Override
    public void addNavigator(Navigator navigator) {
        Navigator.Name annotation = navigator.getClass().getAnnotation(Navigator.Name.class);
        String name = annotation != null ? annotation.value() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("No @Navigator.Name annotation found for "
                    + navigator.getClass().getSimpleName());
        }

        addNavigator(name, navigator);
    }

    @Override
    public void addNavigator(String name, Navigator navigator) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("navigator name cannot be null");
        }

        mNavigators.put(name, navigator);
    }
}
