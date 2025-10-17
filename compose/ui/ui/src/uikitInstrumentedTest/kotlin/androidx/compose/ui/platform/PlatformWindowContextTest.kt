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

import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UISceneDidActivateNotification
import platform.UIKit.UISceneWillDeactivateNotification

class PlatformWindowContextTest {
    @Test
    fun testSceneWindowFocusedWhenSceneActive() = runUIKitInstrumentedTest {
        var isWindowFocused = false

        setContent {
            val windowInfo = LocalWindowInfo.current
            isWindowFocused = windowInfo.isWindowFocused
        }

        assertTrue(isWindowFocused)

        NSNotificationCenter.defaultCenter.postNotificationName(UISceneWillDeactivateNotification, appDelegate.window?.windowScene)
        waitForIdle()

        assertFalse(isWindowFocused)

        NSNotificationCenter.defaultCenter.postNotificationName(UISceneDidActivateNotification, appDelegate.window?.windowScene)
        waitForIdle()

        assertTrue(isWindowFocused)
    }

    @Test
    fun testDialogWindowFocusedWhenSceneActive() = runUIKitInstrumentedTest {
        var isDialogWindowFocused = false

        setContent {
            Dialog({}) {
                val windowInfo = LocalWindowInfo.current
                isDialogWindowFocused = windowInfo.isWindowFocused
            }
        }

        assertTrue(isDialogWindowFocused)

        NSNotificationCenter.defaultCenter.postNotificationName(UISceneWillDeactivateNotification, appDelegate.window?.windowScene)
        waitForIdle()

        assertFalse(isDialogWindowFocused)

        NSNotificationCenter.defaultCenter.postNotificationName(UISceneDidActivateNotification, appDelegate.window?.windowScene)
        waitForIdle()

        assertTrue(isDialogWindowFocused)
    }

    @Test
    fun testWindowContainerSizeIsSet() = runUIKitInstrumentedTest {
        lateinit var windowInfo: WindowInfo
        setContent {
            windowInfo = LocalWindowInfo.current
        }

        val containerSize = windowInfo.containerSize
        assertTrue(containerSize.width > 0)
        assertTrue(containerSize.height > 0)

        val containerDpSize = windowInfo.containerDpSize
        assertTrue(containerDpSize.width > 0.dp)
        assertTrue(containerDpSize.height > 0.dp)
    }
}
