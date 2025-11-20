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
package androidx.abbenchmarking

/** Returns a list of connected device serial numbers. */
internal fun getConnectedDevices(): List<String> {
    val output = runCommand(*(listOf("adb", "devices").toTypedArray()))
    return output
        .lines()
        .drop(1) // Drop the "List of devices attached" header
        .mapNotNull { it.split("\\s+".toRegex()).firstOrNull() }
        .filter { it.isNotBlank() }
}
