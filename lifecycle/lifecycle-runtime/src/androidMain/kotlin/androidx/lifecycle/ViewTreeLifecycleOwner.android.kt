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
@file:JvmName("ViewTreeLifecycleOwner")

package androidx.lifecycle

import android.view.View
import androidx.lifecycle.runtime.R

/**
 * Set the [LifecycleOwner] responsible for managing the given [View]. Calls to [get] from this view
 * or descendants will return `lifecycleOwner`.
 *
 * This should only be called by constructs such as activities or fragments that manage a view tree
 * and reflect their own lifecycle through a [LifecycleOwner]. Callers should only set a
 * [LifecycleOwner] that will be *stable.* The associated lifecycle should report that it is
 * destroyed if the view tree is removed and is not guaranteed to later become reattached to a
 * window.
 *
 * @param lifecycleOwner LifecycleOwner representing the manager of the given view
 */
@JvmName("set")
public fun View.setViewTreeLifecycleOwner(lifecycleOwner: LifecycleOwner?) {
    setTag(R.id.view_tree_lifecycle_owner, lifecycleOwner)
}

/**
 * Retrieve the [LifecycleOwner] responsible for managing the given [View]. This may be used to
 * scope work or heavyweight resources associated with the view that may span cycles of the view
 * becoming detached and reattached from a window.
 *
 * @return The [LifecycleOwner] responsible for managing this view and/or some subset of its
 *   ancestors
 */
@JvmName("get")
public fun View.findViewTreeLifecycleOwner(): LifecycleOwner? {
    var currentView: View? = this
    while (currentView != null) {
        val lifecycleOwner = currentView.getTag(R.id.view_tree_lifecycle_owner) as? LifecycleOwner
        if (lifecycleOwner != null) {
            return lifecycleOwner
        }
        currentView = currentView.parent as? View
    }
    return null
}
