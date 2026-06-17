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

package androidx.wear.compose.foundation

import android.view.InputDevice
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class HierarchicalFocusInputTest {
    @Test
    fun activeFocusGroup_swallowsPetcDpadEvents() {
        val mockDevice = mock(InputDevice::class.java)
        `when`(mockDevice.name).thenReturn("petc")

        val nativeEvent = mock(AndroidKeyEvent::class.java)
        `when`(nativeEvent.device).thenReturn(mockDevice)
        `when`(nativeEvent.keyCode).thenReturn(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        val keyEvent = KeyEvent(nativeEvent)

        assertTrue(shouldSwallowFocusGroupKeyEvent(active = true, keyEvent = keyEvent))
    }

    @Test
    fun activeFocusGroup_allowsLegitDpadEvents() {
        val mockDevice = mock(InputDevice::class.java)
        `when`(mockDevice.name).thenReturn("Virtual Keyboard")

        val nativeEvent = mock(AndroidKeyEvent::class.java)
        `when`(nativeEvent.device).thenReturn(mockDevice)
        `when`(nativeEvent.keyCode).thenReturn(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        val keyEvent = KeyEvent(nativeEvent)

        assertFalse(shouldSwallowFocusGroupKeyEvent(active = true, keyEvent = keyEvent))
    }

    @Test
    fun inactiveFocusGroup_allowsPetcDpadEvents() {
        val mockDevice = mock(InputDevice::class.java)
        `when`(mockDevice.name).thenReturn("petc")

        val nativeEvent = mock(AndroidKeyEvent::class.java)
        `when`(nativeEvent.device).thenReturn(mockDevice)
        `when`(nativeEvent.keyCode).thenReturn(AndroidKeyEvent.KEYCODE_DPAD_CENTER)

        val keyEvent = KeyEvent(nativeEvent)

        assertFalse(shouldSwallowFocusGroupKeyEvent(active = false, keyEvent = keyEvent))
    }
}
