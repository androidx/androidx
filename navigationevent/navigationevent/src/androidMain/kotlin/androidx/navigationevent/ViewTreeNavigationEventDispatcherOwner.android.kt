/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("ViewTreeNavigationEventDispatcherOwner")

package androidx.navigationevent

import android.view.View
import androidx.core.viewtree.getParentOrViewTreeDisjointParent

/**
 * Set the [NavigationEventDispatcherOwner] associated with the given [View]. Calls to
 * [findViewTreeNavigationEventDispatcherOwner] from this view or descendants will return
 * [NavigationEventDispatcherOwner].
 *
 * This should only be called by constructs such as activities or dialogs that manage a view tree
 * and handle the dispatch of navigation events. Callers should only set a
 * [NavigationEventDispatcherOwner] that will be *stable.*
 *
 * @param navigationEventDispatcherOwner [NavigationEventDispatcherOwner] associated with the [View]
 */
@JvmName("set")
public fun View.setViewTreeNavigationEventDispatcherOwner(
    navigationEventDispatcherOwner: NavigationEventDispatcherOwner
) {
    setTag(R.id.view_tree_navigation_event_dispatcher_owner, navigationEventDispatcherOwner)
}

/**
 * Retrieve the [NavigationEventDispatcherOwner] associated with the given [View]. This may be used
 * to add a callback for navigation events.
 *
 * @return The [NavigationEventDispatcherOwner] associated with this view and/or some subset of its
 *   ancestors
 */
@JvmName("get")
public fun View.findViewTreeNavigationEventDispatcherOwner(): NavigationEventDispatcherOwner? {
    var currentView: View? = this
    while (currentView != null) {
        val dispatchOwner =
            currentView.getTag(R.id.view_tree_navigation_event_dispatcher_owner)
                as? NavigationEventDispatcherOwner
        if (dispatchOwner != null) {
            return dispatchOwner
        }
        currentView = currentView.getParentOrViewTreeDisjointParent() as? View
    }
    return null
}
