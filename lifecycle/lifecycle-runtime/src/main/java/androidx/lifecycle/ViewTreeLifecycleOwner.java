/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.lifecycle;

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.runtime.R;

/**
 * Accessors for finding a view tree-local {@link LifecycleOwner} that reports the lifecycle for
 * the given view.
 */
public class ViewTreeLifecycleOwner {
    private ViewTreeLifecycleOwner() {
        // No instances
    }

    /**
     * Set the {@link LifecycleOwner} responsible for managing the given {@link View}.
     * Calls to {@link #get(View)} from this view or descendants will return {@code lifecycleOwner}.
     *
     * <p>This should only be called by constructs such as activities or fragments that manage
     * a view tree and reflect their own lifecycle through a {@link LifecycleOwner}. Callers
     * should only set a {@link LifecycleOwner} that will be <em>stable.</em> The associated
     * lifecycle should report that it is destroyed if the view tree is removed and is not
     * guaranteed to later become reattached to a window.</p>
     *
     * @param view Root view managed by lifecycleOwner
     * @param lifecycleOwner LifecycleOwner representing the manager of the given view
     */
    public static void set(@NonNull View view, @Nullable LifecycleOwner lifecycleOwner) {
        view.setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner);
    }

    /**
     * Retrieve the {@link LifecycleOwner} responsible for managing the given {@link View}.
     * This may be used to scope work or heavyweight resources associated with the view
     * that may span cycles of the view becoming detached and reattached from a window.
     *
     * @param view View to fetch a {@link LifecycleOwner} for
     * @return The {@link LifecycleOwner} responsible for managing this view and/or some subset
     *         of its ancestors
     */
    @Nullable
    public static LifecycleOwner get(@NonNull View view) {
        LifecycleOwner found = (LifecycleOwner) view.getTag(R.id.view_tree_lifecycle_owner);
        if (found != null) return found;
        ViewParent parent = view.getParent();
        while (found == null && parent instanceof View) {
            final View parentView = (View) parent;
            found = (LifecycleOwner) parentView.getTag(R.id.view_tree_lifecycle_owner);
            parent = parentView.getParent();
        }
        return found;
    }
}
