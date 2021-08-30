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
package androidx.core.view;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * A class that allows you to host and
 * keep track of {@link MenuProvider}s that will supply
 * {@link android.view.MenuItem}s to the app bar.
 *
 * @see MenuHostHelper
 */
public interface MenuHost {

    /**
     * Adds the given {@link MenuProvider} to this {@link MenuHost}.
     *
     * If using this method, you must manually remove the provider when necessary.
     *
     * @param provider the MenuProvider to be added
     * @see #removeMenuProvider(MenuProvider)
     */
    void addMenuProvider(@NonNull MenuProvider provider);

    /**
     * Adds the given {@link MenuProvider} to this {@link MenuHost}.
     *
     * This {@link MenuProvider} will be removed once the given {@link LifecycleOwner}
     * receives an {@link Lifecycle.Event.ON_DESTROY} event.
     *
     * @param provider the MenuProvider to be added
     * @param owner    the Lifecycle owner whose state will determine the removal of the provider
     */
    void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner);

    /**
     * Adds the given {@link MenuProvider} to this {@link MenuHost} once the given
     * {@link LifecycleOwner} reaches the given {@link Lifecycle.State}.
     *
     * This {@link MenuProvider} will be removed once the given {@link LifecycleOwner}
     * goes down from the given {@link Lifecycle.State}.
     *
     * @param provider the MenuProvider to be added
     * @param state    the Lifecycle.State to check for automated addition/removal
     * @param owner    the Lifecycle owner whose state will be used for automated addition/removal
     */
    @SuppressLint("LambdaLast")
    void addMenuProvider(@NonNull MenuProvider provider, @NonNull LifecycleOwner owner,
            @NonNull Lifecycle.State state);

    /**
     * Removes the given {@link MenuProvider} from this {@link MenuHost}.
     *
     * @param provider the MenuProvider to be removed
     */
    void removeMenuProvider(@NonNull MenuProvider provider);

    /**
     * Invalidates the {@link android.view.Menu} to ensure that what is
     * displayed matches the current internal state of the menu.
     *
     * This should be called whenever the state of the menu is changed,
     * such as items being removed or disabled based on some user event.
     */
    void invalidateMenu();
}
