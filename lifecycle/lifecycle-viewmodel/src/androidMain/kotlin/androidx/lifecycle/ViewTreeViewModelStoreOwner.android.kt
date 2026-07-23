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
@file:JvmName("ViewTreeViewModelStoreOwner")

package androidx.lifecycle

import android.view.View
import androidx.core.viewtree.getParentOrViewTreeDisjointParent
import androidx.lifecycle.viewmodel.R

/**
 * Sets the [ViewModelStoreOwner] associated with the given [View].
 *
 * Calls to [findViewTreeViewModelStoreOwner] from this view or its descendants will return
 * [viewModelStoreOwner].
 *
 * This should only be called by constructs such as `Activity`s or `Fragment`s that manage a view
 * tree and retain state through a [ViewModelStoreOwner]. Callers should only set a
 * [ViewModelStoreOwner] that is stable. The associated [ViewModelStore] should be cleared if the
 * view tree is removed and is not guaranteed to later become reattached to a window.
 *
 * @param viewModelStoreOwner [ViewModelStoreOwner] to associate with this [View]
 */
@JvmName("set")
public fun View.setViewTreeViewModelStoreOwner(viewModelStoreOwner: ViewModelStoreOwner?) {
    setTag(R.id.view_tree_view_model_store_owner, viewModelStoreOwner)
}

/**
 * Retrieves the [ViewModelStoreOwner] associated with the given [View].
 *
 * Used to retain state associated with this view across configuration changes.
 *
 * @return [ViewModelStoreOwner] associated with this view or its ancestors
 */
@JvmName("get")
public fun View.findViewTreeViewModelStoreOwner(): ViewModelStoreOwner? {
    var currentView: View? = this
    while (currentView != null) {
        val storeOwner =
            currentView.getTag(R.id.view_tree_view_model_store_owner) as? ViewModelStoreOwner
        if (storeOwner != null) {
            return storeOwner
        }
        currentView = currentView.getParentOrViewTreeDisjointParent() as? View
    }
    return null
}
