/*
 * Copyright (C) 2023 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.capture

import android.view.View
import androidx.annotation.RestrictTo
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/** Needed to be provided for the lifecycle execution */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    public fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    public fun onResume() {
        //        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    public fun onPause() {
        //        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    public fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }

    /**
     * Compose uses the Window's decor view to locate the Lifecycle/ViewModel/SavedStateRegistry
     * owners. Therefore, we need to set this class as the "owner" for the decor view.
     */
    public fun attachToDecorView(decorView: View?) {
        if (decorView == null) return

        decorView.setViewTreeLifecycleOwner(this)
        decorView.setViewTreeSavedStateRegistryOwner(this)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    // LifecycleOwner methods
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    // ViewModelStore methods
    override val viewModelStore: ViewModelStore = ViewModelStore()

    // SavedStateRegistry methods
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() {
            return savedStateRegistryController.savedStateRegistry
        }
}
