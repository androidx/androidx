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

import androidx.compose.ui.window.ApplicationForegroundStateListener
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

internal class UIKitArchitectureComponentsOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    NavigationEventDispatcherOwner {
    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()
    override val navigationEventDispatcher = NavigationEventDispatcher()

    var isViewAppeared = false
        set(value) {
            field = value
            updateLifecycleState()
        }
    var isAppForeground = ApplicationForegroundStateListener.isApplicationForeground
        set(value) {
            field = value
            updateLifecycleState()
        }
    var isAppActive = isAppForeground
        set(value) {
            field = value
            updateLifecycleState()
        }

    private var isDisposed = false

    init {
        updateLifecycleState()
    }

    fun dispose() {
        isDisposed = true
        viewModelStore.clear()
        updateLifecycleState()
    }

    private fun updateLifecycleState() {
        lifecycle.currentState = when {
            isDisposed -> State.DESTROYED
            isViewAppeared && isAppForeground && isAppActive -> State.RESUMED
            isViewAppeared && isAppForeground && !isAppActive -> State.STARTED
            else -> State.CREATED
        }
    }
}