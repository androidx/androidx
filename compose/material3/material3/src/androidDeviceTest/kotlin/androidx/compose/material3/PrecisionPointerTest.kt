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

package androidx.compose.material3

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // Needed for inline mocking
class PrecisionPointerTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Before
    fun setup() {
        shouldUsePrecisionPointerComponentSizing.value = false
    }

    @Test
    fun precisionPointerUiDisabled_withPhysicalKeyboardAndMouse_noDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = false

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)
        inputManager.addDevice(MockDevices.physicalKeyboard)
        inputManager.addDevice(MockDevices.mouse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }

    @Test
    fun precisionPointerUiEnabled_noDevicesConnected_noDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = true

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)

        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }

    @Test
    fun precisionPointerUiEnabled_withPhysicalKeyboardAndMouse_usesDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = true

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)
        inputManager.addDevice(MockDevices.physicalKeyboard)
        inputManager.addDevice(MockDevices.mouse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_withMouse_physicalKeyboardAddedLater_updatesToUseDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = true

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)

        // Add just mouse, not enough to trigger dense UI
        inputManager.addDevice(MockDevices.mouse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }

        // Add keyboard as well, now we have kb+mouse, so we can trigger dense UI
        inputManager.addDevice(MockDevices.physicalKeyboard)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_updateDeviceToMouse_updatesToUseDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = true

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)

        inputManager.addDevice(MockDevices.physicalKeyboard)
        inputManager.addDevice(MockDevices.touchpadNonMouse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }

        inputManager.updateDevice(
            MockDevices.touchpadNonMouse.copy(
                sources = InputDevice.SOURCE_TOUCHPAD or InputDevice.SOURCE_MOUSE
            )
        )
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }
    }

    @Test
    fun precisionPointerUiEnabled_removePhysicalKeyboard_updatesToRemoveDenseUi() {
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled = true

        val inputManager = FakeInputManager()

        rule.setContent(inputManager.inputManager)

        inputManager.addDevice(MockDevices.physicalKeyboard)
        inputManager.addDevice(MockDevices.mouse)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isTrue() }

        inputManager.removeDevice(MockDevices.physicalKeyboard)
        rule.runOnIdle { assertThat(shouldUsePrecisionPointerComponentSizing.value).isFalse() }
    }
}

private fun ComposeContentTestRule.setContent(inputManager: InputManager) {
    val fakeContext: Context = mock {
        on { getSystemService(InputManager::class.java) } doReturn inputManager
    }

    setContent { CompositionLocalProvider(LocalContext provides fakeContext) { MaterialTheme {} } }
}

object MockDevices {
    private var nextId = 1000

    /** A full physical keyboard. */
    val physicalKeyboard =
        keyboard(isVirtual = false, keyboardType = InputDevice.KEYBOARD_TYPE_ALPHABETIC)

    /** A physical 0-9 numpad. */
    val physicalNumpad =
        keyboard(isVirtual = false, keyboardType = InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC)

    /** A virtual keyboard. */
    val virtualKeyboard =
        keyboard(isVirtual = true, keyboardType = InputDevice.KEYBOARD_TYPE_ALPHABETIC)

    /** A virtual 0-9 numpad. */
    val virtualNumpad =
        keyboard(isVirtual = true, keyboardType = InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC)

    /** A physical mouse. */
    val mouse = device(isVirtual = false, sources = InputDevice.SOURCE_MOUSE)

    /** An external drawing tablet. */
    val drawingTablet =
        device(isVirtual = false, sources = InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_STYLUS)

    /** A touchpad that moves the mouse cursor. */
    val touchpadMouse =
        device(isVirtual = false, sources = InputDevice.SOURCE_MOUSE or InputDevice.SOURCE_TOUCHPAD)

    /** A touchpad that does not move the mouse cursor. */
    val touchpadNonMouse = device(isVirtual = false, sources = InputDevice.SOURCE_TOUCHPAD)

    private fun keyboard(isVirtual: Boolean, keyboardType: Int): InputDevice = mock {
        on { this.id } doReturn nextId++
        on { this.isVirtual } doReturn isVirtual
        on { this.sources } doReturn InputDevice.SOURCE_KEYBOARD
        on { this.keyboardType } doReturn keyboardType
    }

    private fun device(isVirtual: Boolean, sources: Int): InputDevice = mock {
        on { this.id } doReturn nextId++
        on { this.isVirtual } doReturn isVirtual
        on { this.sources } doReturn sources
    }
}

private fun InputDevice.copy(
    id: Int = this.id,
    isVirtual: Boolean = this.isVirtual,
    sources: Int = this.sources,
    keyboardType: Int = this.keyboardType,
): InputDevice = mock {
    on { this.id } doReturn id
    on { this.isVirtual } doReturn isVirtual
    on { this.sources } doReturn sources
    on { this.keyboardType } doReturn keyboardType
}

class FakeInputManager {
    val inputManager: InputManager = mock {
        on { registerInputDeviceListener(any(), anyOrNull()) } doAnswer
            { invocation ->
                val listener = invocation.getArgument<InputManager.InputDeviceListener>(0)
                listeners += listener
            }
        on { unregisterInputDeviceListener(any()) } doAnswer
            { invocation ->
                val listener = invocation.getArgument<InputManager.InputDeviceListener>(0)
                listeners -= listener
            }
        on { getInputDevice(any()) } doAnswer
            { invocation ->
                val id = invocation.getArgument<Int>(0)
                devices[id]
            }
        on { inputDeviceIds } doAnswer { devices.keys.toIntArray() }
    }

    private val listeners = mutableListOf<InputManager.InputDeviceListener>()

    private val devices = mutableMapOf<Int, InputDevice>()

    fun addDevice(inputDevice: InputDevice) {
        devices[inputDevice.id] = inputDevice
        listeners.forEach { it.onInputDeviceAdded(inputDevice.id) }
    }

    fun removeDevice(inputDevice: InputDevice) {
        devices.remove(inputDevice.id)
        listeners.forEach { it.onInputDeviceRemoved(inputDevice.id) }
    }

    fun updateDevice(inputDevice: InputDevice) {
        check(inputDevice.id in devices) {
            "updateDevice(id=${inputDevice.id}) called but this ID doesn't exist. Current IDs: ${devices.keys}"
        }
        devices[inputDevice.id] = inputDevice
        listeners.forEach { it.onInputDeviceChanged(inputDevice.id) }
    }
}
