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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore

/**
 * Subclass of [NavController] that offers additional APIs for use by a [NavHost] to connect the
 * NavController to external dependencies.
 *
 * Apps should generally not construct controllers, instead obtain a relevant controller directly
 * from a navigation host via [NavHost.navController] or by using one of the utility methods on the
 * [Navigation] class.
 */
public expect class NavHostController : NavController {
    /**
     * Sets the host's [LifecycleOwner].
     *
     * @param owner The [LifecycleOwner] associated with the containing [NavHost].
     */
    public final override fun setLifecycleOwner(owner: LifecycleOwner)

    /**
     * Sets the host's ViewModelStore used by the NavController to store ViewModels at the
     * navigation graph level. This is required to call `getViewModelStoreOwner` and should
     * generally be called for you by your [NavHost].
     *
     * You must call this method before [setGraph] or similar methods, because the [ViewModelStore]
     * set here will be used by the created [NavBackStackEntry] items.
     *
     * @param viewModelStore ViewModelStore used to store ViewModels at the navigation graph level
     * @throws IllegalStateException if this method is called when graph was already set via
     *   [setGraph] or similar methods.
     */
    public final override fun setViewModelStore(viewModelStore: ViewModelStore)
}
