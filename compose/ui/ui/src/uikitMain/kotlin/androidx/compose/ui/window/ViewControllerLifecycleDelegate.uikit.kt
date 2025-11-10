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

import androidx.compose.ui.uikit.utils.CMPViewControllerLifecycleDelegateProtocol
import androidx.lifecycle.Lifecycle
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

internal class ViewControllerLifecycleDelegate(
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter
): NSObject(), CMPViewControllerLifecycleDelegateProtocol {
    private var isViewAppeared = false
        set(value) {
            field = value
            updateLifecycleState()
        }
    private var isSceneInForeground = false
        set(value) {
            field = value
            updateLifecycleState()
        }
    private var isSceneActive = false
        set(value) {
            field = value
            updateLifecycleState()
        }

    private var isDisposed = false
        set(value) {
            field = value
            updateLifecycleState()
        }

    var onLifecycleStateUpdated: ((Lifecycle.State) -> Unit)? = null
        set(value) {
            field = value
            updateLifecycleState()
        }
    
    private val activeStateListener = SceneActiveStateListener(
        notificationCenter = notificationCenter,
        getScene = ::windowScene
    ) { isSceneActive ->
        this.isSceneActive = isSceneActive
    }
    private val foregroundStateListener = SceneForegroundStateListener(
        notificationCenter = notificationCenter,
        getScene = ::windowScene
    ) { isSceneInForeground ->
        this.isSceneInForeground = isSceneInForeground
    }

    var windowScene: UIWindowScene? = null
        set(value) {
            field = value
            this.isSceneInForeground = foregroundStateListener.isSceneInForeground
            this.isSceneActive = activeStateListener.isSceneActive
        }

    override fun viewControllerWillDealloc() {
        this.isDisposed = true
        activeStateListener.dispose()
        foregroundStateListener.dispose()
        windowScene = null
    }

    override fun viewControllerWillAppear() {
        this.isViewAppeared = true
    }

    override fun viewControllerDidDisappear() {
        this.isViewAppeared = false
    }

    private fun updateLifecycleState() {
        onLifecycleStateUpdated?.invoke(
            when {
                isDisposed -> Lifecycle.State.DESTROYED
                isViewAppeared && isSceneInForeground && isSceneActive -> Lifecycle.State.RESUMED
                isViewAppeared && isSceneInForeground && !isSceneActive -> Lifecycle.State.STARTED
                else -> Lifecycle.State.CREATED
            }
        )
    }
}
