/*
 * Copyright 2024 The Android Open Source Project
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
import android.hardware.camera2.CameraCaptureSession
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isSamsungDevice
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.SessionProcessor

/**
 * A quirk to not abort captures on stop during [SessionProcessor] sessions on certain platforms.
 *
 * QuirkSummary
 * - Bug Id: 325088903
 * - Description: When stopping a capture session, calling [CameraCaptureSession.abortCaptures] may
 *   cause [SessionProcessor.deInitSession] to hang indefinitely. By default, CameraPipe aborts
 *   captures on stop to speed up switching between capture sessions. With this quirk, this behavior
 *   is disabled on devices from selected vendors.
 * - Device(s): Devices on devices from selected vendors.
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
class DisableAbortCapturesOnStopWithSessionProcessorQuirk : Quirk {
    companion object {
        fun isEnabled() = isSamsungDevice()
    }
}
