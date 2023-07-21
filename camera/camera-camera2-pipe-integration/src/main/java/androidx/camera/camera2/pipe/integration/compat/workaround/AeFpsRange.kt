/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.util.Range
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.compat.quirk.AeFpsRangeLegacyQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.config.CameraScope
import javax.inject.Inject

/**
 * Sets an AE target FPS range on legacy devices from [AeFpsRangeLegacyQuirk] to maintain good
 * exposure.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

@CameraScope
class AeFpsRange @Inject constructor(cameraQuirks: CameraQuirks) {
    private val aeTargetFpsRange: Range<Int>? by lazy {
        /** Chooses the AE target FPS range on legacy devices.  */
        cameraQuirks.quirks[AeFpsRangeLegacyQuirk::class.java]?.range
    }

    /**
     * Sets the [android.hardware.camera2.CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE]
     * option on legacy device when possible.
     */
    fun getTargetAeFpsRange(): Range<Int>? {
        return aeTargetFpsRange
    }
}