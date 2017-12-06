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

/**
 * A NavigationProvider stores a set of {@link Navigator}s that are valid ways to navigate
 * to a destination.
 */
public interface NavigatorProvider {
    /**
     * Retrieves a registered {@link Navigator} using the name provided by the
     * {@link Navigator.Name Navigator.Name annotation}.
     *
     * @param navigatorClass class of the navigator to return
     * @return the registered navigator with the given {@link Navigator.Name}
     *
     * @see #addNavigator(Navigator)
     */
    @Nullable
    Navigator<? extends NavDestination> getNavigator(
            @NonNull Class<? extends Navigator> navigatorClass);

    /**
     * Retrieves a registered {@link Navigator} by name.
     *
     * @param name name of the navigator to return
     * @return the registered navigator with the given name
     *
     * @see #addNavigator(String, Navigator)
     */
    @Nullable
    Navigator<? extends NavDestination> getNavigator(@NonNull String name);

    /**
     * Register a navigator using the name provided by the
     * {@link Navigator.Name Navigator.Name annotation}. {@link NavDestination destinations} may
     * refer to any registered navigator by name for inflation. If a navigator by this name is
     * already registered, this new navigator will replace it.
     *
     * @param navigator navigator to add
     */
    void addNavigator(@NonNull Navigator<? extends NavDestination> navigator);

    /**
     * Register a navigator by name. {@link NavDestination destinations} may refer to any
     * registered navigator by name for inflation. If a navigator by this name is already
     * registered, this new navigator will replace it.
     *
     * @param name name for this navigator
     * @param navigator navigator to add
     */
    void addNavigator(@NonNull String name, @NonNull Navigator<? extends NavDestination> navigator);
}
