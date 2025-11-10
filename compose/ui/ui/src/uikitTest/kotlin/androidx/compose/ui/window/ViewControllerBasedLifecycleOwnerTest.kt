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

import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.lifecycle.Lifecycle
import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UISceneDidActivateNotification
import platform.UIKit.UISceneDidEnterBackgroundNotification
import platform.UIKit.UISceneWillDeactivateNotification
import platform.UIKit.UISceneWillEnterForegroundNotification
import platform.UIKit.UIWindowScene

class ViewControllerBasedLifecycleOwnerTest {
    @Test
    fun allEvents() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = DefaultArchitectureComponentsOwner()
        val lifecycleDelegate = ViewControllerLifecycleDelegate(notificationCenter)
        lifecycleDelegate.onLifecycleStateUpdated = lifecycleOwner::setLifecycleState
        val scene = UIWindowScene()
        lifecycleDelegate.windowScene = scene
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UISceneWillEnterForegroundNotification, scene)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleDelegate.viewControllerWillAppear()
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UISceneDidActivateNotification, scene)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        // app is visible, but not active, e.g. App switcher is shown
        notificationCenter.postNotificationName(UISceneWillDeactivateNotification, scene)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UISceneDidActivateNotification, scene)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        // app in background, e.g. Home button is pressed
        notificationCenter.postNotificationName(UISceneDidEnterBackgroundNotification, scene)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UISceneWillEnterForegroundNotification, scene)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        lifecycleDelegate.viewControllerDidDisappear()
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleDelegate.viewControllerWillDealloc()
        assertEquals(Lifecycle.State.DESTROYED, lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun foregroundThenViewWillAppear() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = DefaultArchitectureComponentsOwner()
        val lifecycleDelegate = ViewControllerLifecycleDelegate(notificationCenter)
        lifecycleDelegate.onLifecycleStateUpdated = lifecycleOwner::setLifecycleState
        val scene = UIWindowScene()
        lifecycleDelegate.windowScene = scene

        notificationCenter.postNotificationName(UISceneDidActivateNotification, scene)
        notificationCenter.postNotificationName(UISceneWillEnterForegroundNotification, scene)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleDelegate.viewControllerWillAppear()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun viewDidDisappearThenBackground() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = DefaultArchitectureComponentsOwner()
        val lifecycleDelegate = ViewControllerLifecycleDelegate(notificationCenter)
        lifecycleDelegate.onLifecycleStateUpdated = lifecycleOwner::setLifecycleState
        val scene = UIWindowScene()
        lifecycleDelegate.windowScene = scene
        lifecycleDelegate.viewControllerWillAppear()

        notificationCenter.postNotificationName(UISceneWillEnterForegroundNotification, scene)
        notificationCenter.postNotificationName(UISceneDidActivateNotification, scene)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        lifecycleDelegate.viewControllerDidDisappear()
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        // this should not happen, but let's protect against it anyway
        notificationCenter.postNotificationName(UISceneDidEnterBackgroundNotification, scene)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)
    }
}