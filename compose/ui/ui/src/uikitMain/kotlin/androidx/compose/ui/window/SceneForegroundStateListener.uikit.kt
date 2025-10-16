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
import platform.UIKit.UISceneActivationStateBackground
import platform.UIKit.UISceneActivationStateUnattached
import platform.UIKit.UISceneDidEnterBackgroundNotification
import platform.UIKit.UISceneWillEnterForegroundNotification
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject

internal class SceneForegroundStateListener(
    /**
     * Provides [UIWindowScene] as soon as compose container is attached to the scene
     */
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
    /**
     * Provides [UIWindowScene] as soon as compose container component is attached to the scene
     */
    private var getScene: () -> UIWindowScene?,
    /**
     * Callback which will be called with `true` when the app becomes active, and `false` when the app goes background
     */
    private var onSceneForegroundStateChanged: (Boolean) -> Unit,
) : NSObject() {
    init {
        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::sceneWillEnterForeground.name + ":"),
            name = UISceneWillEnterForegroundNotification,
            `object` = null
        )

        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::sceneDidEnterBackground.name + ":"),
            name = UISceneDidEnterBackgroundNotification,
            `object` = null
        )
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun sceneWillEnterForeground(notification: NSNotification) {
        if (notification.`object` == getScene()) {
            onSceneForegroundStateChanged(true)
        }
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun sceneDidEnterBackground(notification: NSNotification) {
        if (notification.`object` == getScene()) {
            onSceneForegroundStateChanged(false)
        }
    }

    val isSceneInForeground: Boolean
        get() =
            getScene()?.let {
                it.activationState != UISceneActivationStateBackground &&
                    it.activationState != UISceneActivationStateUnattached
            } ?: false

    /**
     * Deregister from [NSNotificationCenter]
     */
    fun dispose() {
        getScene = { null }
        onSceneForegroundStateChanged = {}
        notificationCenter.removeObserver(observer = this, name = UISceneWillEnterForegroundNotification, `object` = null)
        notificationCenter.removeObserver(observer = this, name = UISceneDidEnterBackgroundNotification, `object` = null)
    }
}