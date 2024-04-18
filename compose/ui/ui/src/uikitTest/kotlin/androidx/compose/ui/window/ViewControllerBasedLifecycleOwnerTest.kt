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

import androidx.lifecycle.Lifecycle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

class ViewControllerBasedLifecycleOwnerTest {
    @Test
    fun allEvents() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = ViewControllerBasedLifecycleOwner(notificationCenter)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleOwner.handleViewWillAppear()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UIApplicationDidEnterBackgroundNotification, UIApplication.sharedApplication)
        assertEquals(Lifecycle.State.STARTED, lifecycleOwner.lifecycle.currentState)

        notificationCenter.postNotificationName(UIApplicationWillEnterForegroundNotification, UIApplication.sharedApplication)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        lifecycleOwner.handleViewDidDisappear()
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleOwner.dispose()
        assertEquals(Lifecycle.State.DESTROYED, lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun foregroundThenViewWillAppear() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = ViewControllerBasedLifecycleOwner(notificationCenter)

        notificationCenter.postNotificationName(UIApplicationWillEnterForegroundNotification, UIApplication.sharedApplication)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        lifecycleOwner.handleViewWillAppear()
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)
    }

    @Test
    fun viewDidDisappearThenBackground() {
        val notificationCenter = NSNotificationCenter()
        val lifecycleOwner = ViewControllerBasedLifecycleOwner(notificationCenter)
        lifecycleOwner.handleViewWillAppear()

        notificationCenter.postNotificationName(UIApplicationWillEnterForegroundNotification, UIApplication.sharedApplication)
        assertEquals(Lifecycle.State.RESUMED, lifecycleOwner.lifecycle.currentState)

        lifecycleOwner.handleViewDidDisappear()
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)

        // this should not happen, but let's protect against it anyway
        notificationCenter.postNotificationName(UIApplicationDidEnterBackgroundNotification, UIApplication.sharedApplication)
        assertEquals(Lifecycle.State.CREATED, lifecycleOwner.lifecycle.currentState)
    }
}