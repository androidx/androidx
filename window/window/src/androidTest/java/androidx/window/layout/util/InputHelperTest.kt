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

package androidx.window.layout.util

import android.view.InputDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for the [InputHelper] class. */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 30)
class InputHelperTest {

    @Test
    fun testIsPhysicalKeyboardDevice_true() {
        val descriptor =
            InputDeviceInfo(
                id = 1,
                isVirtual = false,
                sources = InputDevice.SOURCE_KEYBOARD,
                keyboardType = InputDevice.KEYBOARD_TYPE_ALPHABETIC,
                isEnabled = true,
            )

        assertThat(descriptor.isPhysicalKeyboard).isTrue()
    }

    @Test
    fun testIsPhysicalKeyboardDevice_virtual_false() {
        val descriptor =
            InputDeviceInfo(
                id = 1,
                isVirtual = true,
                sources = InputDevice.SOURCE_KEYBOARD,
                keyboardType = InputDevice.KEYBOARD_TYPE_ALPHABETIC,
                isEnabled = true,
            )

        assertThat(descriptor.isPhysicalKeyboard).isFalse()
    }

    @Test
    fun testIsMouseDeviceEnabled_true() {
        val descriptor =
            InputDeviceInfo(
                id = 1,
                isVirtual = false,
                sources = InputDevice.SOURCE_MOUSE,
                keyboardType = InputDevice.KEYBOARD_TYPE_NONE,
                isEnabled = true,
            )

        assertThat(descriptor.isMouseEnabled).isTrue()
    }

    @Test
    fun testIsMouseDeviceEnabled_stylus_false() = runTest {
        val descriptor =
            InputDeviceInfo(
                id = 1,
                isVirtual = false,
                sources = InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_STYLUS,
                keyboardType = InputDevice.KEYBOARD_TYPE_NONE,
                isEnabled = true,
            )

        assertThat(descriptor.isMouseEnabled).isFalse()
    }
}
