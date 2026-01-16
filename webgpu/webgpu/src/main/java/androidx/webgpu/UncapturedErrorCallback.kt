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

package androidx.webgpu

public fun interface UncapturedErrorCallback {
    /**
     * A callback function invoked when an uncaptured GPU error occurs on a device.
     *
     * @param device The device where the uncaptured error occurred.
     * @param type The type of the uncaptured error.
     * @param message A human-readable message describing the uncaptured error.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onUncapturedError")
    public fun onUncapturedError(device: GPUDevice, @ErrorType type: Int, message: String)
}

internal class UncapturedErrorCallbackRunnable(
    private val callback: UncapturedErrorCallback,
    private val device: GPUDevice,
    private val type: Int,
    private val message: String,
) : Runnable {
    override fun run() {
        callback.onUncapturedError(device, type, message)
    }
}
