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

package androidx.compose.ui.platform

import androidx.compose.ui.InternalComposeUiApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

/**
 * Provides platform-specific component owners.
 */
@InternalComposeUiApi
interface PlatformArchitectureComponentsOwner {
    val lifecycleOwner: LifecycleOwner
    val navigationEventDispatcherOwner: NavigationEventDispatcherOwner
    val viewModelStoreOwner: ViewModelStoreOwner?

    // TODO: Add SavedStateRegistryOwner
}

/**
 * Default implementation of [PlatformArchitectureComponentsOwner].
 */
@InternalComposeUiApi
open class DefaultArchitectureComponentsOwner(
    enforceMainThread: Boolean = true,
) : PlatformArchitectureComponentsOwner,
    LifecycleOwner,
    ViewModelStoreOwner,
    NavigationEventDispatcherOwner {
    override val lifecycleOwner get() = this
    override val navigationEventDispatcherOwner get() = this
    override val viewModelStoreOwner get() = this
    override val lifecycle = if (enforceMainThread) {
        LifecycleRegistry(this)
    } else {
        LifecycleRegistry.createUnsafe(this)
    }
    override val viewModelStore = ViewModelStore()
    override val navigationEventDispatcher = NavigationEventDispatcher()
}
