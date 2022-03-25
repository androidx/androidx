/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Accessors for finding a view tree-local {@link SavedStateRegistryOwner} that allows managing the
 * saving state using {@link SavedStateRegistry} for the given view.
 */
public final class ViewTreeSavedStateRegistryOwner {
    private ViewTreeSavedStateRegistryOwner() {
        // No instances
    }

    /**
     * Set the {@link SavedStateRegistryOwner} responsible for managing the saved state for the
     * given {@link View}.
     * Calls to {@link #get(View)} from this view or descendants will return {@code owner}.
     *
     * This is is automatically set for you in the common cases of using fragments or
     * ComponentActivity.
     *
     * <p>This should only be called by constructs such as activities or fragments that manage
     * a view tree and their saved state through a {@link SavedStateRegistryOwner}. Callers
     * should only set a {@link SavedStateRegistryOwner} that will be <em>stable.</em> The
     * associated {@link SavedStateRegistry} should be cleared if the view tree is removed and is
     * not guaranteed to later become reattached to a window.</p>
     *
     * @param view Root view managed by {@link SavedStateRegistryOwner}
     * @param owner The {@link SavedStateRegistryOwner} responsible for managing the
     *              saved state for the given view
     */
    public static void set(@NonNull View view, @Nullable SavedStateRegistryOwner owner) {
        view.setTag(R.id.view_tree_saved_state_registry_owner, owner);
    }

    /**
     * Retrieve the {@link SavedStateRegistryOwner} responsible for managing the saved state
     * for the given {@link View}.
     * This may be used to save or restore the state associated with the view.
     *
     * The returned {@link SavedStateRegistryOwner} is managing all the Views within the Fragment
     * or Activity the given {@link View} is added to.
     *
     * @param view View to fetch a {@link SavedStateRegistryOwner} for
     * @return The {@link SavedStateRegistryOwner} responsible for managing the saved state for
     *         the given view and/or some subset of its ancestors
     */
    @Nullable
    public static SavedStateRegistryOwner get(@NonNull View view) {
        SavedStateRegistryOwner found = (SavedStateRegistryOwner) view.getTag(
                R.id.view_tree_saved_state_registry_owner);
        if (found != null) return found;
        ViewParent parent = view.getParent();
        while (found == null && parent instanceof View) {
            final View parentView = (View) parent;
            found = (SavedStateRegistryOwner) parentView.getTag(
                    R.id.view_tree_saved_state_registry_owner);
            parent = parentView.getParent();
        }
        return found;
    }
}
