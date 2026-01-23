/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.keyboard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.runUIKitInstrumentedTest
import kotlin.test.Test
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification

class KeyboardNotificationsHandleTest {
    @Test
    fun testKeyboardNotificationsHandle() = runUIKitInstrumentedTest {
        setContent {
            Text("Compose", modifier = Modifier.fillMaxSize().imePadding())
        }

        NSNotificationCenter.defaultCenter.postNotificationName(
            UIKeyboardWillShowNotification,
            null
        )
        waitForIdle()
        // Should not crash

        NSNotificationCenter.defaultCenter.postNotificationName(
            UIKeyboardWillHideNotification,
            null
        )
        waitForIdle()
        // Should not crash
    }
}