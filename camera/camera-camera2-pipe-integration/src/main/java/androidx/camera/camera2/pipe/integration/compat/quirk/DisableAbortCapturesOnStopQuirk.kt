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
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * Quirk needed on devices where faulty implementations of abortCaptures can lead to undesirable
 * behaviors such as camera HAL crashing.
 *
 * QuirkSummary
 * - Bug Id: 356792947, 431912245
 * - Description: Instructs CameraPipe to not abort captures when stopping.
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class DisableAbortCapturesOnStopQuirk : Quirk {
    public companion object {
        @JvmStatic
        public fun isEnabled(): Boolean {
            return Device.isTecnoDevice() || isSamsungNote10PlusDevice || isPocoX3ProDevice
        }

        private val isSamsungNote10PlusDevice: Boolean =
            Device.isSamsungDevice() && "d2q".equals(Build.DEVICE, true)

        private val isPocoX3ProDevice: Boolean =
            Device.isPocoDevice() && "M2102J20SG".equals(Build.MODEL, true)
    }
}
