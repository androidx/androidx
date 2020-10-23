/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe

/**
 * Requirement to consider prior to locking auto-exposure, auto-focus and auto-whitebalance.
 */
enum class Lock3ABehavior {
    /**
     * This requirement means that we want to lock the values for 3A immediately.
     *
     * For AE/AWB this is achieved by asking the camera device to lock them immediately by
     * setting [android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK],
     * [android.hardware.camera2.CaptureRequest.CONTROL_AWB_LOCK] to true right away.
     *
     * For AF we immediately ask the camera device to trigger AF by setting the
     * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER] to
     * [android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_START].
     */
    IMMEDIATE,
    /**
     * Lock 3A values after their current scan is finished. If there is no active ongoing scan then
     * the values will be locked to the current values.
     */
    AFTER_CURRENT_SCAN,
    /**
     * Initiate a new scan, and then lock the values once the scan is done.
     */
    AFTER_NEW_SCAN,
}