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

@file:JvmName("ViewTreeOnBackPressedDispatcherOwner")

package androidx.activity

import android.view.View

/**
 * Set the [OnBackPressedDispatcherOwner] associated with the given [View].
 * Calls to [findViewTreeOnBackPressedDispatcherOwner] from this view or descendants will
 * return [onBackPressedDispatcherOwner].
 *
 * This should only be called by constructs such as activities or dialogs that manage
 * a view tree and handle the dispatch of the system back button. Callers
 * should only set a [OnBackPressedDispatcherOwner] that will be *stable.*
 *
 * @param onBackPressedDispatcherOwner [OnBackPressedDispatcherOwner] associated with the [View]
 */
@JvmName("set")
fun View.setViewTreeOnBackPressedDispatcherOwner(
    onBackPressedDispatcherOwner: OnBackPressedDispatcherOwner
) {
    setTag(R.id.view_tree_on_back_pressed_dispatcher_owner, onBackPressedDispatcherOwner)
}

/**
 * Retrieve the [OnBackPressedDispatcherOwner] associated with the given [View].
 * This may be used to add a callback for the system back button.
 *
 * @return The [OnBackPressedDispatcherOwner] associated with this view and/or some subset
 * of its ancestors
 */
@JvmName("get")
fun View.findViewTreeOnBackPressedDispatcherOwner(): OnBackPressedDispatcherOwner? {
    return generateSequence(this) {
        it.parent as? View
    }.mapNotNull {
        it.getTag(R.id.view_tree_on_back_pressed_dispatcher_owner) as? OnBackPressedDispatcherOwner
    }.firstOrNull()
}
