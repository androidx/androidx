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
package androidx.abbenchmarking.util

/** Returns a list of connected device serial numbers. */
internal fun getConnectedDevices(): List<String> {
    val output = runCommand(*(listOf("adb", "devices").toTypedArray()))
    return output
        .lines()
        .drop(1) // Drop the "List of devices attached" header
        .mapNotNull { it.split("\\s+".toRegex()).firstOrNull() }
        .filter { it.isNotBlank() }
}

/**
 * Determines the target device ID based on connected devices and a provided serial number.
 *
 * If a serial number is provided, the function checks if a device with that serial number is
 * connected. If so, it returns that serial number. If not, it throws a [RuntimeException].
 *
 * If no serial number is provided, the function checks the number of connected devices. If there is
 * only one connected device, it returns the serial number of that device. If there are no connected
 * devices, it throws a [RuntimeException]. If there are multiple connected devices, it throws a
 * [RuntimeException], prompting the user to specify a device with the `--serial` option.
 *
 * @param serial The serial number of the device to target, or null to auto-detect.
 * @return The serial number of the target device.
 * @throws RuntimeException if the specified device is not found, no devices are connected, or
 *   multiple devices are connected without a specific serial.
 */
internal fun getTargetDeviceId(serial: String?): String {
    val connectedDevices = getConnectedDevices()
    return when {
        serial != null -> {
            if (connectedDevices.contains(serial)) {
                serial
            } else {
                throw RuntimeException("Error: Device with ID '$serial' not found.")
            }
        }
        connectedDevices.size == 1 -> connectedDevices[0]
        connectedDevices.isEmpty() -> throw RuntimeException("Error: No devices connected.")
        else -> {
            throw RuntimeException(
                "Error: Multiple devices connected. Please specify one with --serial."
            )
        }
    }
}
