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

package androidx.pdf

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlin.test.assertNotNull

internal object TestUtils {

    fun assertNotNullObjectByText(viewText: String) {
        val selectedObject = selectUiObjectByText(viewText)
        assertNotNull(selectedObject)
    }

    fun selectUiObjectByText(viewText: String): UiObject2? {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        var selectedView: UiObject2?

        // device.wait returns null if the timeout expires
        try {
            selectedView = device.wait(Until.findObject(By.text(viewText)), WAIT_TIMEOUT)
        } catch (e: NullPointerException) {
            return null
        }
        return selectedView
    }

    const val WAIT_TIMEOUT = 500L
}
