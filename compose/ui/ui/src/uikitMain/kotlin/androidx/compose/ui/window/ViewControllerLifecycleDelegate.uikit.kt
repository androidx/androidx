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

import androidx.compose.ui.platform.UIKitArchitectureComponentsOwner
import androidx.compose.ui.uikit.utils.CMPViewControllerLifecycleDelegateProtocol
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

internal class ViewControllerLifecycleDelegate(
    private val componentsOwner: UIKitArchitectureComponentsOwner,
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
): NSObject(), CMPViewControllerLifecycleDelegateProtocol {
    private val activeStateListener = SceneActiveStateListener(
        notificationCenter = notificationCenter,
        getScene = ::windowScene
    ) { isSceneActive ->
        componentsOwner.isSceneActive = isSceneActive
    }
    private val foregroundStateListener = SceneForegroundStateListener(
        notificationCenter = notificationCenter,
        getScene = ::windowScene
    ) { isSceneInForeground ->
        componentsOwner.isSceneInForeground = isSceneInForeground
    }

    var windowScene: UIWindowScene? = null
        set(value) {
            field = value
            componentsOwner.isSceneInForeground = foregroundStateListener.isSceneInForeground
            componentsOwner.isSceneActive = activeStateListener.isSceneActive
        }

    override fun viewControllerWillDealloc() {
        componentsOwner.dispose()
        activeStateListener.dispose()
        foregroundStateListener.dispose()
        windowScene = null
    }

    override fun viewControllerWillAppear() {
        componentsOwner.isViewAppeared = true
    }

    override fun viewControllerDidDisappear() {
        componentsOwner.isViewAppeared = false
    }
}
