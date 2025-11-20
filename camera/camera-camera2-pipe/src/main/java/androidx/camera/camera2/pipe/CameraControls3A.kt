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

package androidx.camera.camera2.pipe

import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.RestrictTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred

/**
 * [CameraControls3A] represents the methods to control and change the camera's 3A state via
 * [CameraGraph].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraControls3A {
    /**
     * Applies the given 3A parameters to the camera device.
     *
     * @return A [Deferred] of [Result3A] value which will contain the frame number for which these
     *   parameters were applied. It may be cancelled with a [CancellationException] if a newer
     *   request is submitted before completion.
     */
    public fun update3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
    ): Deferred<Result3A>

    /**
     * Applies the given 3A parameters to the camera device but for only one frame.
     *
     * @return the FrameNumber for which these parameters were applied.
     */
    public fun submit3A(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
    ): Deferred<Result3A>

    /**
     * Turns the torch to ON.
     *
     * This method has a side effect on the currently set AE mode. Ref:
     * https://developer.android.com/reference/android/hardware/camera2/CaptureRequest#FLASH_MODE To
     * use the flash control, AE mode must be set to ON or OFF. So if the AE mode is already not
     * either ON or OFF, we will need to update the AE mode to one of those states, here we will
     * choose ON. It is the responsibility of the application layer above CameraPipe to restore the
     * AE mode after the torch control has been used. The [setTorchOff] or [update3A] method can be
     * used to restore the AE state to a previous value.
     *
     * @return the FrameNumber at which the turn was fully turned on if switch was ON, or the
     *   FrameNumber at which it was completely turned off when the switch was OFF.
     */
    public fun setTorchOn(): Deferred<Result3A>

    /**
     * Turns the torch to OFF.
     *
     * @param aeMode The [AeMode] to set while disabling the torch value. If null which is the
     *   default value, the current AE mode is used.
     * @return the FrameNumber at which the turn was fully turned on if switch was ON, or the
     *   FrameNumber at which it was completely turned off when the switch was OFF.
     */
    public fun setTorchOff(aeMode: AeMode? = null): Deferred<Result3A>
}
