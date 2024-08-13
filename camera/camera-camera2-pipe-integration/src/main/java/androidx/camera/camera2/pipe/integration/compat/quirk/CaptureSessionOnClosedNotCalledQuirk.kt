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
 * A quirk to denote the devices may not receive
 * [android.hardware.camera2.CameraCaptureSession.StateCallback.onClosed] callback.
 *
 * QuirkSummary
 * - Bug Id: 144817309
 * - Description: On Android API 22s and lower,
 *   [android.hardware.camera2.CameraCaptureSession.StateCallback.onClosed] callback will not be
 *   triggered under some circumstances.
 * - Device(s): Devices in Android API version <= 22
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CaptureSessionOnClosedNotCalledQuirk : Quirk {
    public companion object {
        /**
         * The quirk is disabled for CameraPipe, as it intrinsically handles things without the
         * reliance on the onClosed callback. For [androidx.camera.core.impl.DeferrableSurface] that
         * does need this signal for ref-counting, CameraPipe has an extra pipeline that "finalizes"
         * the capture session when a new capture session is created or the camera device is closed.
         */
        public fun isEnabled(): Boolean = false
    }
}
