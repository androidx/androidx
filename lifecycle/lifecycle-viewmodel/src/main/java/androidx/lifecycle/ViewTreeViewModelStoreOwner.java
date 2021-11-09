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

package androidx.lifecycle;

import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.viewmodel.R;

/**
 * Accessors for finding a view tree-local {@link ViewModelStoreOwner} that allows access to a
 * {@link ViewModelStore} for the given view.
 */
public class ViewTreeViewModelStoreOwner {
    private ViewTreeViewModelStoreOwner() {
        // No instances
    }

    /**
     * Set the {@link ViewModelStoreOwner} associated with the given {@link View}.
     * Calls to {@link #get(View)} from this view or descendants will return
     * {@code viewModelStoreOwner}.
     *
     * <p>This should only be called by constructs such as activities or fragments that manage
     * a view tree and retain state through a {@link ViewModelStoreOwner}. Callers
     * should only set a {@link ViewModelStoreOwner} that will be <em>stable.</em> The associated
     * {@link ViewModelStore} should be cleared if the view tree is removed and is not
     * guaranteed to later become reattached to a window.</p>
     *
     * @param view Root view associated with the viewModelStoreOwner
     * @param viewModelStoreOwner ViewModelStoreOwner associated with the given view
     */
    public static void set(@NonNull View view, @Nullable ViewModelStoreOwner viewModelStoreOwner) {
        view.setTag(R.id.view_tree_view_model_store_owner, viewModelStoreOwner);
    }

    /**
     * Retrieve the {@link ViewModelStoreOwner} associated with the given {@link View}.
     * This may be used to retain state associated with this view across configuration changes.
     *
     * @param view View to fetch a {@link ViewModelStoreOwner} for
     * @return The {@link ViewModelStoreOwner} associated with this view and/or some subset
     *         of its ancestors
     */
    @Nullable
    public static ViewModelStoreOwner get(@NonNull View view) {
        ViewModelStoreOwner found = (ViewModelStoreOwner) view.getTag(
                R.id.view_tree_view_model_store_owner);
        if (found != null) return found;
        ViewParent parent = view.getParent();
        while (found == null && parent instanceof View) {
            final View parentView = (View) parent;
            found = (ViewModelStoreOwner) parentView.getTag(R.id.view_tree_view_model_store_owner);
            parent = parentView.getParent();
        }
        return found;
    }
}
