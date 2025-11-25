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

import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

internal actual val shouldUsePrecisionPointerComponentSizing: Boolean
    @Composable
    get() {
        val devices = LocalDevices.current
        return devices != null && devices.keyboards.isNotEmpty() && devices.mice.isNotEmpty()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun EnsurePrecisionPointerListenersRegistered(content: @Composable (() -> Unit)) {
    val shouldRegisterListeners =
        ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled &&
            LocalDevices.current == null
    if (shouldRegisterListeners) {
        // Precision pointer UI flag enabled, and LocalDevices is not yet populated in this
        // hierarchy; set up device listeners to update LocalDevices
        val devices by rememberDevicesState()
        CompositionLocalProvider(LocalDevices provides devices, content = content)
    } else {
        // Already initialized within this hierarchy, or the user does not want precision pointer
        // UI, so just render their provided content
        content()
    }
}

@Composable
private fun rememberDevicesState(): State<Devices?> {
    val context = LocalContext.current

    val inputManager =
        context.getSystemService<InputManager>()
            ?: return remember(context) { mutableStateOf(null) }

    val devicesState: MutableState<Devices> =
        remember(context) {
            // Initial state: Cache all current input devices that are keyboards/mice
            val keyboards = MutableIntSet()
            val mice = MutableIntSet()
            for (deviceId in inputManager.inputDeviceIds) {
                val device = inputManager.getInputDevice(deviceId)
                if (device.isKeyboard()) {
                    keyboards.add(deviceId)
                }
                if (device.isMouse()) {
                    mice.add(deviceId)
                }
            }
            mutableStateOf(Devices(keyboards = keyboards, mice = mice))
        }

    DisposableEffect(context) {
        val listener =
            object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    maybeUpdateDevice(deviceId)
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    maybeUpdateDevice(deviceId)
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    maybeUpdateDevice(deviceId)
                }

                private fun maybeUpdateDevice(deviceId: Int) {
                    val device = inputManager.getInputDevice(deviceId)
                    val newDevices =
                        devicesState.value.withUpdateForDevice(
                            deviceId = deviceId,
                            isKeyboard = device.isKeyboard(),
                            isMouse = device.isMouse(),
                        )
                    if (newDevices != null) {
                        devicesState.value = newDevices
                    }
                }
            }
        inputManager.registerInputDeviceListener(listener, /* handler= */ null)
        onDispose { inputManager.unregisterInputDeviceListener(listener) }
    }

    return devicesState
}

private val LocalDevices = compositionLocalOf<Devices?> { null }

@Immutable private data class Devices(val keyboards: IntSet, val mice: IntSet)

/**
 * Returns an updated version of this [Devices] instance, updated accordingly:
 * - [Devices.keyboards] will contain [deviceId] iff [isKeyboard] is true
 * - [Devices.mice] will contain [deviceId] iff [isMouse] is true
 *
 * If the source [Devices] instance is correct already, `null` is returned instead to signify that
 * no update is necessary.
 */
private fun Devices.withUpdateForDevice(
    deviceId: Int,
    isKeyboard: Boolean,
    isMouse: Boolean,
): Devices? {
    val newKeyboards = keyboards.withUpdatedValuePresence(deviceId, shouldBePresent = isKeyboard)
    val newMice = mice.withUpdatedValuePresence(deviceId, shouldBePresent = isMouse)
    return if (newKeyboards == null && newMice == null) {
        // Both the kb + mouse state for this device were already correct; no need to make a new
        // instance of the Devices object as the current one is already correct
        null
    } else {
        // Make a copy of the current Devices object with the kb and/or mouse updates
        copy(keyboards = newKeyboards ?: keyboards, mice = newMice ?: mice)
    }
}

/**
 * Returns an [IntSet] based on the source [IntSet], but with [value] either added to the set if
 * [shouldBePresent] is true, or removed from the set if [shouldBePresent] is false.
 *
 * If the presence of [value] is already correct in the source set (that is, if it is already
 * present and [shouldBePresent] is `true`, or if it already not present and [shouldBePresent] is
 * false), `null` is returned instead to indicate that the source set was already correct.
 */
private fun IntSet.withUpdatedValuePresence(value: Int, shouldBePresent: Boolean): IntSet? {
    val isPresent = value in this
    return if (isPresent && !shouldBePresent) {
        // New set will not contain value
        val newSet = MutableIntSet(initialCapacity = this.size - 1)
        forEach { element -> if (element != value) newSet.add(value) }
        newSet
    } else if (!isPresent && shouldBePresent) {
        // New set will contain value
        val newSet = MutableIntSet(initialCapacity = this.size + 1)
        newSet.addAll(this)
        newSet.add(value)
        newSet
    } else {
        // The value's presence is already correct, return null
        null
    }
}

private fun InputDevice?.isKeyboard(): Boolean =
    this != null &&
        !isVirtual &&
        hasSource(InputDevice.SOURCE_KEYBOARD) &&
        keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC

private fun InputDevice?.isMouse(): Boolean =
    this != null &&
        !isVirtual &&
        hasSource(InputDevice.SOURCE_MOUSE) &&
        // Per SOURCE_MOUSE documentation, a single device that is both SOURCE_MOUSE and
        // SOURCE_STYLUS is not actually a mouse, but an "external drawing tablet".
        !hasSource(InputDevice.SOURCE_STYLUS)

private fun InputDevice.hasSource(source: Int) = (sources and source) == source
