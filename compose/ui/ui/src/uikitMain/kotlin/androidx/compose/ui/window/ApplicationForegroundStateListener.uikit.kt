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

import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.darwin.NSObject

internal class ApplicationForegroundStateListener(
    /**
     * [NSNotificationCenter] to listen to, can be customized for tests purposes
     */
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
    /**
     * Callback which will be called with `true` when the app becomes active, and `false` when the app goes background
     */
    private val onApplicationForegroundStateChanged: (Boolean) -> Unit
) : NSObject() {
    init {
        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::applicationWillEnterForeground.name),
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null
        )

        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::applicationDidEnterBackground.name),
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null
        )
    }

    @ObjCAction
    fun applicationWillEnterForeground() {
        onApplicationForegroundStateChanged(true)
    }

    @ObjCAction
    fun applicationDidEnterBackground() {
        onApplicationForegroundStateChanged(false)
    }

    /**
     * Deregister from [NSNotificationCenter]
     */
    fun dispose() {
        notificationCenter.removeObserver(observer = this, name = UIApplicationWillEnterForegroundNotification, `object` = null)
        notificationCenter.removeObserver(observer = this, name = UIApplicationDidEnterBackgroundNotification, `object` = null)
    }

    companion object {
        val isApplicationForeground: Boolean
            get() = UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateBackground
    }
}