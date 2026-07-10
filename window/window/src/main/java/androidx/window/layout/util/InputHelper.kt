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

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.view.InputDevice

/** Data class representing the properties of an [InputDevice]. */
internal data class InputDeviceInfo(
    val id: Int,
    val isVirtual: Boolean,
    val sources: Int,
    val keyboardType: Int,
    val isEnabled: Boolean,
) {
    /** Returns `true` if the device supports the given [source]. */
    fun supportsSource(source: Int): Boolean = (sources and source) == source

    /** Returns `true` if this device is a physical keyboard. */
    val isPhysicalKeyboard: Boolean
        get() =
            !isVirtual &&
                supportsSource(InputDevice.SOURCE_KEYBOARD) &&
                keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC &&
                isEnabled

    /** Returns `true` if this device is a mouse and is enabled. */
    val isMouseEnabled: Boolean
        get() =
            supportsSource(InputDevice.SOURCE_MOUSE) &&
                isEnabled &&
                !supportsSource(InputDevice.SOURCE_STYLUS)
}

/** Helper class to interact with [InputManager] and [InputDevice]. */
internal interface InputHelper {
    /**
     * Returns the list of input device IDs. This method can be long-running and should not be
     * called from the main thread.
     */
    fun getInputDeviceIds(): IntArray

    /**
     * Returns the [InputDeviceInfo] for the device with the given [deviceId]. This method can be
     * long-running and should not be called from the main thread.
     */
    fun getInputDevice(deviceId: Int): InputDeviceInfo?

    /** Returns `true` if the device with the given [descriptor] is a physical keyboard device. */
    fun isPhysicalKeyboardDevice(descriptor: InputDeviceInfo): Boolean

    /**
     * Returns `true` if the device with the given [descriptor] is a mouse device and is enabled.
     */
    fun isMouseDeviceEnabled(descriptor: InputDeviceInfo): Boolean

    /**
     * Registers an [InputManager.InputDeviceListener] to be notified about changes in input
     * devices.
     *
     * @param listener the [InputManager.InputDeviceListener] to register.
     * @param handler the [Handler] on which the listener should be invoked. If null, the main
     *   thread's looper will be used.
     */
    fun registerInputDeviceListener(
        listener: InputManager.InputDeviceListener,
        handler: Handler? = null,
    )

    /**
     * Unregisters an [InputManager.InputDeviceListener].
     *
     * @param listener the [InputManager.InputDeviceListener] to unregister.
     */
    fun unregisterInputDeviceListener(listener: InputManager.InputDeviceListener)

    companion object {
        fun getInstance(context: Context): InputHelper {
            val inputManager =
                context.applicationContext.getSystemService(Context.INPUT_SERVICE) as InputManager
            return InputHelperImpl(inputManager)
        }
    }
}

/** Implementation of [InputHelper]. */
internal class InputHelperImpl(private val inputManager: InputManager) : InputHelper {
    override fun getInputDeviceIds(): IntArray = inputManager.inputDeviceIds

    override fun getInputDevice(deviceId: Int): InputDeviceInfo? =
        inputManager.getInputDevice(deviceId)?.let { device ->
            val isEnabled =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    device.isEnabled
                } else {
                    true
                }
            InputDeviceInfo(
                id = device.id,
                isVirtual = device.isVirtual,
                sources = device.sources,
                keyboardType = device.keyboardType,
                isEnabled = isEnabled,
            )
        }

    override fun isPhysicalKeyboardDevice(descriptor: InputDeviceInfo) =
        descriptor.isPhysicalKeyboard

    override fun isMouseDeviceEnabled(descriptor: InputDeviceInfo) = descriptor.isMouseEnabled

    override fun registerInputDeviceListener(
        listener: InputManager.InputDeviceListener,
        handler: Handler?,
    ) {
        inputManager.registerInputDeviceListener(listener, handler)
    }

    override fun unregisterInputDeviceListener(listener: InputManager.InputDeviceListener) {
        inputManager.unregisterInputDeviceListener(listener)
    }
}
