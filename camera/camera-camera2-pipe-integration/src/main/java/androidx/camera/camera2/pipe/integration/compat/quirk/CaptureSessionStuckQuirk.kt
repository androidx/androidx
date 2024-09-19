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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import androidx.camera.core.impl.Quirk

/**
 * A quirk to denote the [android.hardware.camera2.CameraCaptureSession] cannot successfully be
 * configured if the previous CameraCaptureSession doesn't finish its in-flight capture sequence.
 *
 * QuirkSummary
 * - Bug Id: 146773463
 * - Description: Opening and releasing the capture session quickly and constantly is a problem for
 *   LEGACY devices. It needs to check that all the existing capture sessions have finished the
 *   processing of their capture sequences before opening the next capture session.
 * - Device(s): Devices in LEGACY camera hardware level.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CaptureSessionStuckQuirk : Quirk {
    public companion object {
        /**
         * Always return false as CameraPipe handles this automatically. Please refer to
         * [androidx.camera.camera2.pipe.compat.Camera2Quirks.shouldWaitForRepeatingRequestStartOnDisconnect]
         * for the conditions under which the quirk will be applied.
         */
        public fun isEnabled(): Boolean = false
    }
}
