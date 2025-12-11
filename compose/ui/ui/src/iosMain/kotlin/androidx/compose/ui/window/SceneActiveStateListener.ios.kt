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

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UISceneDidActivateNotification
import platform.UIKit.UISceneWillDeactivateNotification
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

internal class SceneActiveStateListener(
    /**
     * [NSNotificationCenter] to listen to, can be customized for tests purposes
     */
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
    /**
     * Provides [UIWindowScene] as soon as compose container is attached to the scene
     */
    private var getScene: () -> UIWindowScene?,
    /**
     * Callback which will be called with `true` when the app becomes active, and `false` when the app goes background
     */
    private var onSceneActiveStateChanged: (Boolean) -> Unit
) : NSObject() {
    init {
        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::sceneDidActivate.name + ":"),
            name = UISceneDidActivateNotification,
            `object` = null
        )

        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::sceneWillDeactivate.name + ":"),
            name = UISceneWillDeactivateNotification,
            `object` = null
        )
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun sceneDidActivate(notification: NSNotification) {
        if (notification.`object` == getScene()) {
            onSceneActiveStateChanged(true)
        }
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun sceneWillDeactivate(notification: NSNotification) {
        if (notification.`object` == getScene()) {
            onSceneActiveStateChanged(false)
        }
    }

    val isSceneActive: Boolean get() =
        getScene()?.activationState == UISceneActivationStateForegroundActive

    /**
     * Deregister from [NSNotificationCenter]
     */
    fun dispose() {
        onSceneActiveStateChanged = {}
        getScene = { null }
        notificationCenter.removeObserver(observer = this, name = UISceneDidActivateNotification, `object` = null)
        notificationCenter.removeObserver(observer = this, name = UISceneWillDeactivateNotification, `object` = null)
    }
}