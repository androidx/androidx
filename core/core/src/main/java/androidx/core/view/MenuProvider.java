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

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;

/**
 * Interface for indicating that a component will be supplying
 * {@link MenuItem}s to the component owning the app bar.
 */
public interface MenuProvider {

    /**
     * Called by the {@link MenuHost} to allow the {@link MenuProvider}
     * to inflate {@link MenuItem}s into the menu.
     *
     * @param menu         the menu to inflate the new menu items into
     * @param menuInflater the inflater to be used to inflate the updated menu
     */
    void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater);

    /**
     * Called by the {@link MenuHost} when a {@link MenuItem} is selected from the menu.
     *
     * @param menuItem the menu item that was selected
     * @return {@code true} if the given menu item is handled by this menu provider,
     *         {@code false} otherwise
     */
    boolean onMenuItemSelected(@NonNull MenuItem menuItem);
}
