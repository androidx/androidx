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

package androidx.compose.ui.layers

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithLabelOrNull
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController

class PopupAppearanceTest {
    @Test
    fun testPopupAppearanceAfterModalVCPresentation() = runUIKitInstrumentedTest {
        val showPopup = mutableStateOf(true)
        setContent {
            if (showPopup.value) {
                Popup(
                    properties = PopupProperties(dismissOnClickOutside = false, focusable = true)
                ) {
                    Text("Popup")
                }
            }
        }

        // Hide popup
        showPopup.value = false
        waitForIdle()
        val popupExists = findNodeWithLabelOrNull("Popup") != null
        assertFalse(popupExists)

        // Show and hide modal view controller
        var presented = false
        val safari = SFSafariViewController(uRL = NSURL(string = "https://jb.gg"))
        viewController.presentViewController(safari, true) {
            presented = true
        }
        waitUntil { presented }
        viewController.dismissViewControllerAnimated(true) {
            presented = false
        }
        waitUntil { !presented }

        // Show popup
        showPopup.value = true
        waitForIdle()
        assertNotNull(findNodeWithLabel("Popup"))
    }
}