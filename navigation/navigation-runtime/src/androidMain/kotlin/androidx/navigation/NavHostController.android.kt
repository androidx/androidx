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
package androidx.navigation

import android.content.Context
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore

public actual open class NavHostController
/**
 * Construct a new controller for a given [Context] suitable for use in a [NavHost]. Controllers
 * should not be used outside of their context and retain a hard reference to the context supplied.
 * If you need a global controller, pass [Context.getApplicationContext].
 *
 * Note that controllers that are not constructed with an [Activity] context (or a wrapped activity
 * context) will only be able to navigate to
 * [new tasks][android.content.Intent.FLAG_ACTIVITY_NEW_TASK] or
 * [new document tasks][android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT] when navigating to new
 * activities.
 *
 * @param context context for this controller
 */
(context: Context) : NavController(context) {
    public actual final override fun setLifecycleOwner(owner: LifecycleOwner) {
        super.setLifecycleOwner(owner)
    }

    /**
     * Sets the host's [OnBackPressedDispatcher]. If set, NavController will register a
     * [onBackPressedCallback] to handle system Back button events.
     *
     * You must explicitly called [setLifecycleOwner] before calling this method as the owner set
     * there will be used as the [LifecycleOwner] for registering the [onBackPressedCallback].
     *
     * You can dynamically enable and disable whether the NavController should handle the system
     * Back button events by calling [enableOnBackPressed].
     *
     * @param dispatcher The [OnBackPressedDispatcher] associated with the containing [NavHost].
     * @throws IllegalStateException if you have not called [setLifecycleOwner] before calling this
     *   method.
     * @see NavHostController.setLifecycleOwner
     */
    public final override fun setOnBackPressedDispatcher(dispatcher: OnBackPressedDispatcher) {
        super.setOnBackPressedDispatcher(dispatcher)
    }

    /**
     * Set whether the NavController should handle the system Back button events via the registered
     * [OnBackPressedDispatcher].
     *
     * @param enabled True if the NavController should handle system Back button events.
     */
    public final override fun enableOnBackPressed(enabled: Boolean) {
        super.enableOnBackPressed(enabled)
    }

    public actual final override fun setViewModelStore(viewModelStore: ViewModelStore) {
        super.setViewModelStore(viewModelStore)
    }
}
