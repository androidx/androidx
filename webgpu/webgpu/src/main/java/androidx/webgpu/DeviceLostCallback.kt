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

public fun interface DeviceLostCallback {
    /**
     * A callback function for notification when a GPU device becomes unusable.
     *
     * @param device The device that was lost.
     * @param reason The reason why the device was lost.
     * @param message A human-readable message explaining the loss.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("onDeviceLost")
    public fun onDeviceLost(device: GPUDevice, @DeviceLostReason reason: Int, message: String)
}

internal class DeviceLostCallbackRunnable(
    private val callback: DeviceLostCallback,
    private val device: GPUDevice,
    private val reason: Int,
    private val message: String,
) : Runnable {
    override fun run() {
        callback.onDeviceLost(device, reason, message)
    }
}
