/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import platform.Foundation.NSNotificationCenter

// TODO: Rename and move to androidx.compose.ui.platform
internal class ViewControllerBasedLifecycleOwner(
    notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
) : LifecycleOwner, ViewModelStoreOwner {
    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()

    private var isViewAppeared = false
    private var isAppForeground = ApplicationStateListener.isApplicationActive
    private var isDisposed = false

    private val applicationStateListener = ApplicationStateListener(notificationCenter) { isForeground ->
        isAppForeground = isForeground
        updateLifecycleState()
    }

    init {
        updateLifecycleState()
    }

    fun dispose() {
        applicationStateListener.dispose()
        viewModelStore.clear()
        isDisposed = true
        updateLifecycleState()
    }

    fun handleViewWillAppear() {
        isViewAppeared = true
        updateLifecycleState()
    }

    fun handleViewDidDisappear() {
        isViewAppeared = false
        updateLifecycleState()
    }

    private fun updateLifecycleState() {
        lifecycle.currentState = when {
            isDisposed -> State.DESTROYED
            isViewAppeared && isAppForeground -> State.RESUMED
            isViewAppeared -> State.STARTED
            else -> State.CREATED
        }
    }
}