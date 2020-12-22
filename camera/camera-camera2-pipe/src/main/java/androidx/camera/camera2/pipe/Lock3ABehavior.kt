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
public enum class Lock3ABehavior {
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

public fun Lock3ABehavior?.shouldUnlockAe(): Boolean =
    this == Lock3ABehavior.AFTER_NEW_SCAN

public fun Lock3ABehavior?.shouldUnlockAf(): Boolean =
    this == Lock3ABehavior.AFTER_NEW_SCAN

public fun Lock3ABehavior?.shouldUnlockAwb(): Boolean =
    this == Lock3ABehavior.AFTER_NEW_SCAN

// For ae and awb if we set the lock = true in the capture request the camera device
// locks them immediately. So when we want to wait for ae to converge we have to explicitly
// wait for it to converge.
public fun Lock3ABehavior?.shouldWaitForAeToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE

public fun Lock3ABehavior?.shouldWaitForAwbToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE

// TODO(sushilnath@): add the optimization to not wait for af to converge before sending the
// trigger for modes other than CONTINUOUS_VIDEO. The paragraph below explains the reasoning.
//
// For af, if the mode is MACRO, AUTO or CONTINUOUS_PICTURE and we send a capture request to
// start an af trigger then camera device starts a new scan(for AUTO mode) or waits for the
// current scan to finish(for CONTINUOUS_PICTURE) and then locks the auto-focus, so if we want
// to wait for af to converge before locking it, we don't have to explicitly wait for
// convergence, we can send the trigger right away, but if the mode is CONTINUOUS_VIDEO then
// sending a request to start a trigger locks the auto focus immediately, so if we want af to
// converge first then we have to explicitly wait for it.
// Ref: https://developer.android.com/reference/android/hardware/camera2/CaptureResult#CONTROL_AF_STATE
public fun Lock3ABehavior?.shouldWaitForAfToConverge(): Boolean =
    this != null && this != Lock3ABehavior.IMMEDIATE