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
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.darwin.NSObject

internal class ApplicationActiveStateListener(
    /**
     * [NSNotificationCenter] to listen to, can be customized for tests purposes
     */
    private val notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter,
    /**
     * Callback which will be called with `true` when the app becomes active, and `false` when the app goes background
     */
    private val onApplicationActiveStateChanged: (Boolean) -> Unit
) : NSObject() {
    init {
        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::applicationDidBecomeActive.name),
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null
        )

        notificationCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::applicationWillResignActive.name),
            name = UIApplicationWillResignActiveNotification,
            `object` = null
        )
    }

    @ObjCAction
    fun applicationDidBecomeActive() {
        onApplicationActiveStateChanged(true)
    }

    @ObjCAction
    fun applicationWillResignActive() {
        onApplicationActiveStateChanged(false)
    }

    /**
     * Deregister from [NSNotificationCenter]
     */
    fun dispose() {
        notificationCenter.removeObserver(observer = this, name = UIApplicationDidBecomeActiveNotification, `object` = null)
        notificationCenter.removeObserver(observer = this, name = UIApplicationWillResignActiveNotification, `object` = null)
    }
}